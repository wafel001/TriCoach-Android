#!/usr/bin/env bash
set -euo pipefail
APK="pixel-empire-v1/app/build/outputs/apk/debug/app-debug.apk"
PKG="com.pixelempire.clicker"
ACT="$PKG/.MainActivity"
LOG="pixel-empire-api28-logcat.txt"
adb logcat -c
adb install -r "$APK"
adb shell settings put secure immersive_mode_confirmations confirmed 2>/dev/null || true
adb shell am force-stop "$PKG" || true
adb shell am start -W -n "$ACT"
sleep 2
# Fallback: dismiss Android's immersive-mode education UI if this old image shows it.
adb shell input tap 900 500 || true
sleep 0.5
for i in 1 2 3 4; do adb shell input tap 540 850; sleep 0.2; done
for xy in "100 350" "300 500" "520 650" "780 720" "980 900" "200 1150" "600 1300" "900 1450"; do adb shell input tap $xy; done
sleep 1
adb shell input keyevent KEYCODE_HOME
sleep 1
adb shell am start -W -n "$ACT"
sleep 2
PID="$(adb shell pidof "$PKG" | tr -d '\r' || true)"
adb logcat -d -v brief > "$LOG"
[[ -n "$PID" ]]
! grep -qE "FATAL EXCEPTION|Process: $PKG" "$LOG"
echo "API 28 compatibility test passed; PID=$PID"
