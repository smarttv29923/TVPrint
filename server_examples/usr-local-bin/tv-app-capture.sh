#!/bin/bash
set -e

TV="192.168.1.164:37375"
DIR="/var/www/html/tvprint"
PNG="$DIR/shot.png"
JPG="$DIR/shot.jpg"
TMP="$PNG.tmp"

adb connect "$TV" >/dev/null 2>&1 || true

# Capture first, pause second, so the pause bar is not printed.
adb -s "$TV" exec-out screencap -p > "$TMP"
test -s "$TMP"
mv "$TMP" "$PNG"

adb -s "$TV" shell input keyevent KEYCODE_MEDIA_PAUSE
sleep 0.15

convert "$PNG" -resize '1920x1080>' -quality 90 "$JPG"
chmod 644 "$PNG" "$JPG"

echo DONE
