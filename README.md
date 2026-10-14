
# 🔄 AutoRestartProxy

> Lekki i w pełni konfigurowalny plugin dla proxy **Velocity**, który umożliwia bezpieczne, kontrolowane restartowanie sieci z automatycznym powiadamianiem graczy.

---

## 🛠️ Wymagania

- **Silnik proxy:** Velocity (wspierający nowoczesne API Adventure)
- **Wersja Javy:** Java 21 lub nowsza
- **Narzędzie budowania:** Maven

---

## 🚀 Instalacja

1. Pobierz repozytorium i zbilduj je w programie intelliJ idea
2. Wrzuć plik `.jar` do katalogu `plugins/` na swoim serwerze Velocity.
3. Uruchom serwer proxy ponownie – folder `plugins/AutoRestartProxy/` oraz plik `config.yml` wygenerują się automatycznie.

---

## 💻 Komendy i Uprawnienia

Wszystkie komendy posiadają wbudowane aliasy, aby przyspieszyć ich wpisywanie.

| Komenda podstawowa | Alias | Argumenty | Opis | Domyślne uprawnienie |
| :--- | :--- | :--- | :--- | :--- |
| `/autorestartproxy` | `/arp` | `<sekundy>` | Uruchamia odliczanie do restartu sieci. | `autorestartproxy.use` |

*Wpisanie nowej komendy odliczania w trakcie trwania poprzedniego procesu automatycznie anuluje stary licznik i rozpocznie nowy.*

---

## ⚙️ Konfiguracja (`config.yml`)

Plik konfiguracyjny pozwala dostosować każdą wiadomość oraz zachowanie pluginu (np. czas wyświetlania napisów na ekranie czy opóźnienie zamknięcia procesu po kicku):

```yaml
# Uprawnienie wymagane do użycia komendy restartu
command-permission: "autorestartproxy.use"

# Włączanie/wyłączanie poszczególnych powiadomień
send-chat: true
send-title: true

# Wygląd komunikatów ({seconds} podmienia się na aktualny czas)
chat-message: "&dʀᴇꜱᴛᴀʀᴛ ᴄᴀʟᴇᴊ ꜱɪᴇᴄɪ ᴘʀᴏxʏ ᴢᴀ &5{seconds}&d ꜱᴇᴄ"
title-text: "&5&l! &dᴜᴡᴀɢᴀ &5&l!"
subtitle-text: "&dʀᴇꜱᴛᴀʀᴛ ꜱɪᴇᴄɪ ᴘʀᴏxʏ ᴢᴀ &5{seconds} &dꜱᴇᴄ"

# Czas wyświetlania komunikatów Title (w milisekundach)
title-fade-in-millis: 250
title-stay-millis: 1000
title-fade-out-millis: 250

# Wiadomości systemowe i błędy
usage-message: "&dᴡᴘɪꜱᴢ: &5/ᴀᴜᴛᴏʀᴇꜱᴛᴀʀᴛᴘʀᴏxʏ <ɪʟᴏꜱᴄ ꜱᴇᴄ>"
invalid-seconds-message: "&dᴘᴏᴅᴀᴊ &5ᴘᴏᴘʀᴀᴡɴᴀ&d ʟɪᴄᴢʙᴇ ꜱᴇᴋᴜɴᴅ"
no-permission-message: "&5ɴɪᴇ ᴘᴏꜱɪᴀᴅᴀꜱᴢ&d ᴡʏꜱᴛᴀʀᴄᴢᴀᴊᴀᴄʏᴄʜ ᴜᴘʀᴀᴡɴɪᴇɴ ᴅᴏ ᴜᴢʏᴄɪᴀ ᴛᴇᴊ ᴋᴏᴍᴇɴᴅʏ"
command-started-message: "&5ᴘᴏᴍʏꜱʟɴɪᴇ &dᴜʀᴜᴄʜᴏᴍɪᴏɴᴏ ʀᴇꜱᴛᴀʀᴛ ᴘʀᴏxʏ(&5{seconds} &dꜱᴇᴄ)"

# Powód wyrzucenia graczy oraz czas (w sekundach) od wyrzucenia do całkowitego zamknięcia proxy
kick-reason-message: "&dʀᴇꜱᴛᴀʀᴛ ꜱɪᴇᴄɪ ᴘʀᴏxʏ &5ᴘʀᴢᴇᴘʀᴀꜱᴢᴀᴍʏ ᴢᴀ ᴜᴛʀᴜᴅɴɪᴇɴɪᴀ"
restart-delay-after-kick-seconds: 2
```

---

## 🛠️ Kompilacja ze źródeł

Jeśli chcesz samodzielnie skompilować plugin, upewnij się, że masz zainstalowanego Mavena oraz Javę 21, a następnie wykonaj w katalogu głównym projektu:

```bash
mvn clean package
```
Gotowy plik `.jar` znajdziesz w nowo powstałym katalogu `target/`.

---

## 👥 Autorzy

- **xFation** – *Główny twórca i deweloper projektu*
