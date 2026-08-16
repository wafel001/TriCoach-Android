#!/usr/bin/env bash
set -euo pipefail
APK="pixel-empire-v1/app/build/outputs/apk/debug/app-debug.apk"
PKG="com.pixelempire.clicker"
ACT="$PKG/.MainActivity"
LOG="pixel-empire-release-logcat.txt"

adb shell settings put secure immersive_mode_confirmations confirmed || true
adb logcat -c
adb install -r "$APK"
adb shell am force-stop "$PKG" || true
adb shell am start -W -n "$ACT"
sleep 2

# Complete the four release tutorial pages.
for i in 1 2 3 4; do adb shell input tap 540 1180; sleep 0.2; done

# 120 fast taps distributed across the world scene. This crosses combo x1.1 and x1.2.
for i in $(seq 1 120); do
  x=$((160 + (i*137)%760))
  y=$((320 + (i*83)%1200))
  adb shell input tap "$x" "$y"
done
sleep 1
adb exec-out screencap -p > pixel-empire-release-start.png

# Visit all seven release tabs.
for x in 75 230 390 540 690 850 1005; do adb shell input tap "$x" 2290; sleep 0.3; done

# Exercise build screen buy mode and first card.
adb shell input tap 230 2290
sleep 0.4
adb shell input tap 100 310
adb shell input tap 850 500
sleep 0.3

# Research and missions navigation.
adb shell input tap 540 2290
sleep 0.3
adb shell input tap 690 2290
sleep 0.3

# Lifecycle save and restore.
adb shell input keyevent KEYCODE_HOME
sleep 1
adb shell am start -W -n "$ACT"
sleep 2
PID="$(adb shell pidof "$PKG" | tr -d '\r' || true)"
[[ -n "$PID" ]]

# Visual QA for castle era.
adb shell am force-stop "$PKG" || true
adb shell am start -W -n "$ACT" --ei demo_stage 12
sleep 2
adb exec-out screencap -p > pixel-empire-release-castle.png

# Visual QA for Infinity Tower endgame.
adb shell am force-stop "$PKG" || true
adb shell am start -W -n "$ACT" --ei demo_stage 39
sleep 2
adb exec-out screencap -p > pixel-empire-release-infinity.png
PID="$(adb shell pidof "$PKG" | tr -d '\r' || true)"
[[ -n "$PID" ]]

adb logcat -d -v brief > "$LOG"
if grep -qE "FATAL EXCEPTION|Process: $PKG" "$LOG"; then
  grep -A 120 -B 30 -E "FATAL EXCEPTION|Process: $PKG" "$LOG" || true
  exit 1
fi

echo "Pixel Empire release Android 15 interactive QA passed; PID=$PID"
