#!/bin/bash
set -e

TV="192.168.1.164:37375"

adb connect "$TV" >/dev/null 2>&1 || true
adb -s "$TV" shell input keyevent KEYCODE_MEDIA_PLAY

echo DONE
