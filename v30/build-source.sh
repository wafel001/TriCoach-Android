set -euo pipefail
cat \
  v30/data/blob00.txt \
  v30/data/b01_00.txt v30/data/b01_01.txt v30/data/b01_02.txt v30/data/b01_03.txt \
  v30/data/blob02.txt \
  v30/data/b03_00.txt v30/data/b03_01.txt v30/data/b03_02a.txt v30/data/b03_02b.txt \
  v30/data/b03_03a.txt v30/data/b03_03b0.txt v30/data/b03_03b1a.txt v30/data/b03_03b1b.txt \
  | base64 --decode > tricoach-v30-truncated.zip
test "$(stat -c %s tricoach-v30-truncated.zip)" = "48000"
printf 'y\n' | zip -FF tricoach-v30-truncated.zip --out TriCoach-v3.0-source-repaired.zip
unzip -t TriCoach-v3.0-source-repaired.zip
rm -rf buildsrc
mkdir buildsrc
unzip -q TriCoach-v3.0-source-repaired.zip -d buildsrc
test -f buildsrc/settings.gradle
test -f buildsrc/app/build.gradle
cd buildsrc
gradle --no-daemon :app:assembleDebug --stacktrace
cd ..
cp buildsrc/app/build/outputs/apk/debug/app-debug.apk TriCoach-v3.0-test.apk
test -s TriCoach-v3.0-test.apk
unzip -t TriCoach-v3.0-test.apk >/dev/null
