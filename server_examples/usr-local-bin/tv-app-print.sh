#!/bin/bash
set -e

TV="192.168.1.164:37375"
JPG="/var/www/html/tvprint/shot.jpg"
PRINTER="Canon_G7000"

test -s "$JPG"

cupsenable "$PRINTER" >/dev/null 2>&1 || true
cupsaccept "$PRINTER" >/dev/null 2>&1 || true

lp \
  -d "$PRINTER" \
  -o PageSize=A4 \
  -o fit-to-page \
  -o ColorModel=RGB \
  "$JPG"

adb connect "$TV" >/dev/null 2>&1 || true
adb -s "$TV" shell input keyevent KEYCODE_MEDIA_PLAY

echo DONE
