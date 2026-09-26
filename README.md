# TV Print

Minimal Android TV app for one-button screenshot printing through the Armbian server.

## Flow

1. Remote shortcut launches **TV Print**.
2. The Activity is visually transparent and calls `capture.php`.
3. Armbian does **SCREENSHOT first, PAUSE second**.
4. When the HTTP response returns, the app shows a native Android TV dialog:
   **Vytlačiť zachytený obrázok?**
   **TLAČIŤ / NIE**
5. TLAČIŤ calls `print.php`; NIE calls `cancel.php`.
6. Both paths resume playback and the app closes.

No browser and no WebView are used.

## Build on GitHub without Android Studio

1. Create an empty GitHub repository.
2. Unzip `TVPrint.zip` on Windows using **Extract All...**. No admin rights are needed.
3. In GitHub choose **Add file -> Upload files**.
4. Upload the contents of the unzipped `TVPrint` folder, preserving the directory structure.
5. Commit.
6. Open **Actions -> Build TV Print APK**.
7. If necessary click **Run workflow**.
8. After the job is green, download artifact **TVPrint-debug**.
9. Extract it; it contains `app-debug.apk`.

## Install

Copy `app-debug.apk` to Armbian and run:

```bash
adb -s 192.168.1.164:37375 install -r app-debug.apk
```

Test launch:

```bash
adb -s 192.168.1.164:37375 shell am start -n sk.michal.tvprint/.MainActivity
```

Package name:

`sk.michal.tvprint`

## Server examples

The `server_examples` folder contains matching Apache/PHP endpoints and three restricted root wrapper scripts.

Install PHP files:

```bash
cp server_examples/apache/tvprint/*.php /var/www/html/tvprint/
```

Install wrappers:

```bash
cp server_examples/usr-local-bin/*.sh /usr/local/bin/
chmod 755 /usr/local/bin/tv-app-*.sh
```

Install sudoers entry:

```bash
cp server_examples/sudoers.d/tvprint /etc/sudoers.d/tvprint
chown root:root /etc/sudoers.d/tvprint
chmod 440 /etc/sudoers.d/tvprint
visudo -c
```

Test:

```bash
curl http://192.168.1.222/tvprint/capture.php
curl http://192.168.1.222/tvprint/cancel.php
```

A successful call returns `DONE`.

## Change Armbian address

Edit:

`app/src/main/java/sk/michal/tvprint/MainActivity.java`

and change:

```java
private static final String BASE_URL = "http://192.168.1.222/tvprint/";
```

Commit; GitHub Actions will build a new APK automatically.

## Safety against duplicate prints

The app uses `singleTask` plus in-process guards. Repeated remote presses while a request/dialog is active do not create multiple simultaneous print actions.
