#!/usr/bin/env bash
set -euo pipefail
APK="pixel-empire-v1/app/build/outputs/apk/debug/app-debug.apk"
PKG="com.pixelempire.clicker"
ACT="$PKG/.MainActivity"
LOG="pixel-empire-v3-api28-logcat.txt"
adb shell settings put secure immersive_mode_confirmations confirmed || true
adb logcat -c
adb install -r "$APK"
adb shell am force-stop "$PKG" || true
adb shell am start -W -n "$ACT"
sleep 2
for i in 1 2 3; do adb shell input tap 540 850; sleep 0.25; done
for i in $(seq 1 20); do x=$((100+(i*113)%850)); y=$((330+(i*149)%1250)); adb shell input tap "$x" "$y"; done
sleep 1
adb shell input keyevent KEYCODE_HOME
sleep 1
adb shell am start -W -n "$ACT"
sleep 2
PID="$(adb shell pidof "$PKG" | tr -d '\r' || true)"
adb logcat -d -v brief > "$LOG"
[[ -n "$PID" ]]
! grep -qE "FATAL EXCEPTION|Process: $PKG" "$LOG"
echo "Pixel Empire 3.0 API 28 compatibility test passed; PID=$PID"
