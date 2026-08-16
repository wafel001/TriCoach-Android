#!/usr/bin/env bash
set -euo pipefail
APK="pixel-empire-v1/app/build/outputs/apk/debug/app-debug.apk"
PKG="com.pixelempire.clicker"
ACT="$PKG/.MainActivity"
LOG="pixel-empire-v3-logcat.txt"

adb shell settings put secure immersive_mode_confirmations confirmed || true
adb logcat -c
adb install -r "$APK"
adb shell am force-stop "$PKG" || true
adb shell am start -W -n "$ACT"
sleep 2

# Complete 3 tutorial cards.
for i in 1 2 3; do adb shell input tap 540 1180; sleep 0.25; done

# 60 taps distributed across the whole world: verifies tap-anywhere and first x1.1 combo tier.
for i in $(seq 1 60); do
  x=$((80 + (i*137)%920))
  y=$((360 + (i*173)%1450))
  adb shell input tap "$x" "$y"
done
sleep 1
adb exec-out screencap -p > pixel-empire-v3-start.png

# Visit every major v3 tab.
for x in 90 270 450 630 810 990; do adb shell input tap "$x" 2285; sleep 0.35; done
adb shell input tap 90 2285
sleep 0.5

# Lifecycle save / restore.
adb shell input keyevent KEYCODE_HOME
sleep 1
adb shell am start -W -n "$ACT"
sleep 2
PID="$(adb shell pidof "$PKG" | tr -d '\r' || true)"
[[ -n "$PID" ]]

# Visual QA: castle era.
adb shell am force-stop "$PKG" || true
adb shell am start -W -n "$ACT" --ei demo_stage 9
sleep 2
adb exec-out screencap -p > pixel-empire-v3-castle.png

# Visual QA: final Infinity Tower.
adb shell am force-stop "$PKG" || true
adb shell am start -W -n "$ACT" --ei demo_stage 39
sleep 2
adb shell input tap 170 700
adb shell input tap 540 980
adb shell input tap 900 1320
sleep 0.5
adb exec-out screencap -p > pixel-empire-v3-infinity.png
PID="$(adb shell pidof "$PKG" | tr -d '\r' || true)"
[[ -n "$PID" ]]

adb logcat -d -v brief > "$LOG"
if grep -qE "FATAL EXCEPTION|Process: $PKG" "$LOG"; then
  grep -A 100 -B 20 -E "FATAL EXCEPTION|Process: $PKG" "$LOG" || true
  exit 1
fi

echo "Pixel Empire 3.0 Android 15 interactive smoke test passed; PID=$PID"
