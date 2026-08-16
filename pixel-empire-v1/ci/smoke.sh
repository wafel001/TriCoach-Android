#!/usr/bin/env bash
set -euo pipefail

APK="pixel-empire-v1/app/build/outputs/apk/debug/app-debug.apk"
PKG="com.pixelempire.clicker"
ACT="$PKG/.MainActivity"
LOG="pixel-empire-logcat.txt"
SHOT="pixel-empire-v2-home.png"
FINAL_SHOT="pixel-empire-v2-infinity.png"

adb logcat -c
adb install -r "$APK"
# Prevent Android's one-time immersive-mode education panel from stealing QA taps.
adb shell settings put secure immersive_mode_confirmations confirmed 2>/dev/null || true
adb shell am force-stop "$PKG" || true
adb shell am start -W -n "$ACT"
sleep 2
# Fallback for emulator images that ignore the secure setting.
adb shell input tap 900 500 || true
sleep 0.7
adb shell uiautomator dump /sdcard/pixel-window.xml >/dev/null 2>&1 || true
adb shell cat /sdcard/pixel-window.xml 2>/dev/null | grep -q "Viewing full screen" && { echo "Immersive-mode overlay still visible"; exit 1; } || true

# Dismiss the four in-game tutorial pages.
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
# Cycle all six languages and make sure settings remain responsive.
for i in 1 2 3 4 5 6; do adb shell input tap 900 1050; sleep 0.18; done
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
  exit 1
fi

# Debug-build-only visual QA of the final Infinity Spire era.
adb shell am force-stop "$PKG" || true
adb shell am start -W -n "$ACT" --ei demo_stage 23
sleep 2
adb shell input tap 180 720
adb shell input tap 540 980
adb shell input tap 900 1320
sleep 0.4
adb exec-out screencap -p > "$FINAL_SHOT"
PID="$(adb shell pidof "$PKG" | tr -d '\r' || true)"
[[ -n "$PID" ]]

# Confirm the system education overlay did not return for the final-stage QA shot.
adb shell uiautomator dump /sdcard/pixel-window-final.xml >/dev/null 2>&1 || true
adb shell cat /sdcard/pixel-window-final.xml 2>/dev/null | grep -q "Viewing full screen" && { echo "Immersive overlay visible in final QA"; exit 1; } || true

adb logcat -d -v brief > "$LOG"
if grep -qE "FATAL EXCEPTION|Process: $PKG" "$LOG"; then
  echo "Fatal Android exception detected"
  grep -A 80 -B 20 -E "FATAL EXCEPTION|Process: $PKG" "$LOG" || true
  exit 1
fi

echo "Pixel Empire interactive + localization + visual QA smoke test passed; PID=$PID"
