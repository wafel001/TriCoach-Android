#!/usr/bin/env bash
set -euo pipefail

APK="pixel-empire-v1/app/build/outputs/apk/debug/app-debug.apk"
PKG="com.pixelempire.clicker"
ACT="$PKG/.MainActivity"
LOG="pixel-empire-logcat.txt"
SHOT="pixel-empire-v2-home.png"

adb logcat -c
adb install -r "$APK"
adb shell am force-stop "$PKG" || true
adb shell am start -W -n "$ACT"
sleep 2

# Dismiss the four tutorial pages.
for i in 1 2 3 4; do adb shell input tap 540 1100; sleep 0.25; done

# Tap across the entire world image, not only the central building.
for xy in \
  "110 420" "300 520" "540 460" "790 610" "970 500" \
  "170 820" "390 760" "620 880" "860 820" "1010 940" \
  "90 1180" "280 1260" "520 1160" "760 1320" "990 1220" \
  "160 1540" "370 1470" "590 1580" "830 1500" "1010 1640"; do
  adb shell input tap $xy
  sleep 0.08
done

# Visit every major tab. Coordinates target a 1080x2400 Pixel 6 profile.
adb shell input tap 324 2290; sleep 0.4   # upgrades
adb shell input tap 540 2290; sleep 0.4   # missions
adb shell input tap 756 2290; sleep 0.4   # research
adb shell input tap 972 2290; sleep 0.4   # menu
adb shell input tap 108 2290; sleep 0.5   # world

adb exec-out screencap -p > "$SHOT"

# Verify lifecycle save/restore and a cold-ish relaunch.
adb shell input keyevent KEYCODE_HOME
sleep 1
adb shell am start -W -n "$ACT"
sleep 2

PID="$(adb shell pidof "$PKG" | tr -d '\r' || true)"
if [[ -z "$PID" ]]; then
  echo "Pixel Empire process is not alive after interaction test"
  adb logcat -d -v brief > "$LOG"
  grep -A 80 -B 20 -E "FATAL EXCEPTION|AndroidRuntime|Process: $PKG|$PKG" "$LOG" || true
  exit 1
fi

adb logcat -d -v brief > "$LOG"
if grep -qE "FATAL EXCEPTION|Process: $PKG" "$LOG"; then
  echo "Fatal Android exception detected"
  grep -A 80 -B 20 -E "FATAL EXCEPTION|Process: $PKG" "$LOG" || true
  exit 1
fi

echo "Pixel Empire interactive smoke test passed; PID=$PID"
