import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "AutoRestartProxy",
        name = "AutoRestartProxy",
        version = "1.0.0",
        authors = {"me.xFation"}
)
public class main {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();

    private Config config;
    private ScheduledTask restartTask;

    @Inject
    public main(ProxyServer proxyServer, Logger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.config = loadConfig();
        registerCommand();
        logger.info("Plugin został pomyślnie włączony");
    }

    private void registerCommand() {
        CommandManager commandManager = proxyServer.getCommandManager();
        commandManager.register(
                commandManager.metaBuilder("autorestartproxy")
                        .aliases("arp")
                        .plugin(this)
                        .build(),
                new AutorestartCommand()
        );
    }

    private final class AutorestartCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            String permission = config.commandPermission;
            if (!permission.isEmpty() && !invocation.source().hasPermission(permission)) {
                invocation.source().sendMessage(parse(config.noPermissionMessage));
                return;
            }

            String[] args = invocation.arguments();
            if (args.length != 1) {
                invocation.source().sendMessage(parse(config.usageMessage));
                return;
            }

            int seconds;
            try {
                seconds = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                invocation.source().sendMessage(parse(config.invalidSecondsMessage));
                return;
            }

            if (seconds <= 0) {
                invocation.source().sendMessage(parse(config.invalidSecondsMessage));
                return;
            }

            if (restartTask != null) {
                restartTask.cancel();
                restartTask = null;
            }

            startRestartCountdown(seconds);
            invocation.source().sendMessage(parse(format(config.commandStartedMessage, seconds)));
        }
    }

    private void startRestartCountdown(int startSeconds) {
        final int[] secondsLeft = {startSeconds};
        restartTask = proxyServer.getScheduler().buildTask(this, () -> {
            int now = secondsLeft[0];
            sendCountdown(now);
            if (now <= 0) {
                if (restartTask != null) {
                    restartTask.cancel();
                    restartTask = null;
                }
                performRestartSequence();
                return;
            }
            secondsLeft[0] = now - 1;
        }).repeat(1, TimeUnit.SECONDS).schedule();
    }

    private void performRestartSequence() {
        Component kickReason = parse(config.kickReasonMessage);
        for (Player player : proxyServer.getAllPlayers()) {
            player.disconnect(kickReason);
        }

        logger.info("Wszyscy gracze zostali wyrzuceni!");
        proxyServer.getScheduler().buildTask(this, () -> {
            proxyServer.shutdown();
        }).delay(config.restartDelayAfterKickSeconds, TimeUnit.SECONDS).schedule();
    }

    private void sendCountdown(int secondsLeft) {
        Component chatComponent = parse(format(config.chatMessage, secondsLeft));
        Component titleComponent = parse(format(config.titleText, secondsLeft));
        Component subtitleComponent = parse(format(config.subtitleText, secondsLeft));

        Title.Times times = Title.Times.times(
                Duration.ofMillis(config.titleFadeInMillis),
                Duration.ofMillis(config.titleStayMillis),
                Duration.ofMillis(config.titleFadeOutMillis)
        );
        Title title = Title.title(titleComponent, subtitleComponent, times);

        for (Player player : proxyServer.getAllPlayers()) {
            if (config.sendChat) {
                player.sendMessage(chatComponent);
            }
            if (config.sendTitle) {
                player.showTitle(title);
            }
        }
    }

    private Component parse(String text) {
        return legacySerializer.deserialize(text);
    }

    private String format(String raw, int secondsLeft) {
        return raw.replace("{seconds}", Integer.toString(secondsLeft));
    }

    private Config loadConfig() {
        Path pluginFolder = Paths.get("plugins", "AutoRestartProxy");
        Path configPath = pluginFolder.resolve("config.yml");
        try {
            Files.createDirectories(pluginFolder);
            if (Files.notExists(configPath)) {
                try (InputStream resource = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                    if (resource == null) {
                        throw new IllegalStateException("Brak pliku config.yml w zasobach pluginu.");
                    }
                    Files.copy(resource, configPath, StandardCopyOption.REPLACE_EXISTING);
                    logger.info("Pomyślnie utworzono pliki konfiguracyjne pluginu!");
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Nie mozna utworzyc plików konfiguracyjnych pluginu! ", e);
        }

        Yaml yaml = new Yaml();
        Map<String, Object> loaded = new LinkedHashMap<>();
        try (InputStream input = Files.newInputStream(configPath)) {
            Object obj = yaml.load(input);
            if (obj instanceof Map<?, ?> rawMap) {
                loaded = convertMap(rawMap);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Nie mozna odczytac plików konfiguracyjnych pluginu! ", e);
        }
        return Config.fromMap(loaded);
    }

    private Map<String, Object> convertMap(Map<?, ?> raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() != null) {
                result.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return result;
    }

    private static final class Config {
        private final String commandPermission;
        private final boolean sendChat;
        private final boolean sendTitle;
        private final String chatMessage;
        private final String titleText;
        private final String subtitleText;
        private final long titleFadeInMillis;
        private final long titleStayMillis;
        private final long titleFadeOutMillis;
        private final String usageMessage;
        private final String invalidSecondsMessage;
        private final String noPermissionMessage;
        private final String commandStartedMessage;
        private final String kickReasonMessage;
        private final long restartDelayAfterKickSeconds;

        private Config(
                String commandPermission,
                boolean sendChat,
                boolean sendTitle,
                String chatMessage,
                String titleText,
                String subtitleText,
                long titleFadeInMillis,
                long titleStayMillis,
                long titleFadeOutMillis,
                String usageMessage,
                String invalidSecondsMessage,
                String noPermissionMessage,
                String commandStartedMessage,
                String kickReasonMessage,
                long restartDelayAfterKickSeconds
        ) {
            this.commandPermission = commandPermission;
            this.sendChat = sendChat;
            this.sendTitle = sendTitle;
            this.chatMessage = chatMessage;
            this.titleText = titleText;
            this.subtitleText = subtitleText;
            this.titleFadeInMillis = titleFadeInMillis;
            this.titleStayMillis = titleStayMillis;
            this.titleFadeOutMillis = titleFadeOutMillis;
            this.usageMessage = usageMessage;
            this.invalidSecondsMessage = invalidSecondsMessage;
            this.noPermissionMessage = noPermissionMessage;
            this.commandStartedMessage = commandStartedMessage;
            this.kickReasonMessage = kickReasonMessage;
            this.restartDelayAfterKickSeconds = restartDelayAfterKickSeconds;
        }

        private static Config fromMap(Map<String, Object> map) {
            return new Config(
                    getString(map, "command-permission", "autorestartproxy.use"),
                    getBoolean(map, "send-chat", true),
                    getBoolean(map, "send-title", true),
                    getString(map, "chat-message", "&dʀᴇꜱᴛᴀʀᴛ ᴄᴀʟᴇᴊ ꜱɪᴇᴄɪ ᴘʀᴏxʏ ᴢᴀ &5{seconds}&d ꜱᴇᴄ"),
                    getString(map, "title-text", "&5&l! &dᴜᴡᴀɢᴀ &5&l!"),
                    getString(map, "subtitle-text", "&dʀᴇꜱᴛᴀʀᴛ ꜱɪᴇᴄɪ ᴘʀᴏxʏ ᴢᴀ &5{seconds} &dꜱᴇᴄ"),
                    getLong(map, "title-fade-in-millis", 250L),
                    getLong(map, "title-stay-millis", 1000L),
                    getLong(map, "title-fade-out-millis", 250L),
                    getString(map, "usage-message", "&dᴡᴘɪꜱᴢ: &5/ᴀᴜᴛᴏʀᴇꜱᴛᴀʀᴛᴘʀᴏxʏ <ɪʟᴏꜱᴄ ꜱᴇᴄ>"),
                    getString(map, "invalid-seconds-message", "&dᴘᴏᴅᴀᴊ &5ᴘᴏᴘʀᴀᴡɴᴀ&d ʟɪᴄᴢʙᴇ ꜱᴇᴋᴜɴᴅ"),
                    getString(map, "no-permission-message", "&5ɴɪᴇ ᴘᴏꜱɪᴀᴅᴀꜱᴢ&d ᴡʏꜱᴛᴀʀᴄᴢᴀᴊᴀᴄʏᴄʜ ᴜᴘʀᴀᴡɴɪᴇɴ ᴅᴏ ᴜᴢʏᴄɪᴀ ᴛᴇᴊ ᴋᴏᴍᴇɴᴅʏ"),
                    getString(map, "command-started-message", "&5ᴘᴏᴍʏꜱʟɴɪᴇ &dᴜʀᴜᴄʜᴏᴍɪᴏɴᴏ ʀᴇꜱᴛᴀʀᴛ ᴘʀᴏxʏ(&5{seconds} &dꜱᴇᴄ)"),
                    getString(map, "kick-reason-message", "&dʀᴇꜱᴛᴀʀᴛ ꜱɪᴇᴄɪ ᴘʀᴏxʏ &5ᴘʀᴢᴇᴘʀᴀꜱᴢᴀᴍʏ ᴢᴀ ᴜᴛʀᴜᴅɴɪᴇɴɪᴀ"),
                    getLong(map, "restart-delay-after-kick-seconds", 2L)
            );
        }

        private static String getString(Map<String, Object> map, String key, String def) {
            return Optional.ofNullable(map.get(key)).map(Objects::toString).orElse(def);
        }

        private static boolean getBoolean(Map<String, Object> map, String key, boolean def) {
            Object value = map.get(key);
            if (value instanceof Boolean b) {
                return b;
            }
            if (value instanceof String s) {
                return "true".equalsIgnoreCase(s.trim());
            }
            return def;
        }

        private static long getLong(Map<String, Object> map, String key, long def) {
            Object value = map.get(key);
            if (value instanceof Number n) {
                return n.longValue();
            }
            if (value instanceof String s) {
                try {
                    return Long.parseLong(s.trim());
                } catch (NumberFormatException ignored) {
                    return def;
                }
            }
            return def;
        }
    }
}
