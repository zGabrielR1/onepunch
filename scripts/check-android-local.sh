#!/usr/bin/env bash
# Invoke through local-limited.sh for a hard 3 GiB process-tree memory cap.
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
bash "$root/scripts/local-limited.sh" --inside true
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export ANDROID_HOME="${ANDROID_HOME:-$root/build/local-tools/sdk}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/var/tmp/onepunch-local-gradle}"
# WSL's Windows-mounted filesystem makes Gradle's many small cache reads slow.
# Keep compiler/cache I/O on Linux storage and copy only reviewable outputs back.
if [[ ! -d "$GRADLE_USER_HOME" && -d "$root/build/local-tools/gradle" ]]; then
  cp -a "$root/build/local-tools/gradle" "$GRADLE_USER_HOME"
fi
snapshot="${ONEPUNCH_ANDROID_SNAPSHOT:-$(mktemp -d /var/tmp/onepunch-android-XXXXXX)}"
mkdir -p "$snapshot/android/app" "$snapshot/shared" "$root/build"
cp -a "$root/android/gradle" "$root/android/gradlew" "$root/android/gradle.properties" "$root/android/"*.kts "$snapshot/android/"
cp -a "$root/android/app/src" "$root/android/app/build.gradle.kts" "$snapshot/android/app/"
cp -a "$root/shared/." "$snapshot/shared/"
echo "Native source snapshot: $snapshot"
printf '%s\n' "$snapshot" > "$root/build/local-android-snapshot.txt"
cd "$snapshot/android"
bash ./gradlew --no-daemon --max-workers=1 --no-parallel \
  '-Dorg.gradle.jvmargs=-Xmx1280m -XX:MaxMetaspaceSize=384m -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8' \
  -Pkotlin.compiler.execution.strategy=in-process \
  :app:assembleDebug :app:testDebugUnitTest 2>&1 | tee "$root/build/android-local.log"
mkdir -p "$root/android/app/build/outputs/apk" "$root/android/app/build/test-results" "$root/android/app/build/reports/tests"
cp -a app/build/outputs/apk/debug "$root/android/app/build/outputs/apk/"
cp -a app/build/test-results/testDebugUnitTest "$root/android/app/build/test-results/"
cp -a app/build/reports/tests/testDebugUnitTest "$root/android/app/build/reports/tests/"
