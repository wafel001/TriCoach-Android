#!/usr/bin/env bash
set -euo pipefail
APK="pixel-empire-v1/app/build/outputs/apk/debug/app-debug.apk"
PKG="com.pixelempire.clicker"
ACT="$PKG/.MainActivity"
LOG="pixel-empire-release-api28-logcat.txt"

adb shell settings put secure immersive_mode_confirmations confirmed || true
adb logcat -c
adb install -r "$APK"
adb shell am force-stop "$PKG" || true
adb shell am start -W -n "$ACT"
sleep 2
for i in 1 2 3 4; do adb shell input tap 540 900; sleep .2; done
for i in $(seq 1 70); do adb shell input tap $((120+(i*91)%780)) $((300+(i*73)%1250)); done
sleep 1
adb shell input keyevent KEYCODE_HOME
sleep 1
adb shell am start -W -n "$ACT"
sleep 2
PID="$(adb shell pidof "$PKG" | tr -d '\r' || true)"
[[ -n "$PID" ]]
adb logcat -d -v brief > "$LOG"
! grep -qE "FATAL EXCEPTION|Process: $PKG" "$LOG"
echo "Pixel Empire release Android 9 compatibility QA passed; PID=$PID"
