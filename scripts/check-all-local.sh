#!/usr/bin/env bash
# Run from a host terminal with systemd access. Every heavy child shares the cap.
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"
if [[ "${1:-}" == --inside ]]; then
  shift
  bash scripts/local-limited.sh --inside true
elif [[ "${1:-}" != --preflight ]]; then
  exec bash scripts/local-limited.sh bash scripts/check-all-local.sh --inside "$@"
fi
mode="${1:---all}"
case "$mode" in
  --all|--web-only|--preflight) ;;
  *) echo 'Usage: check-all-local.sh [--all|--web-only|--preflight]' >&2; exit 2 ;;
esac

missing=()
for tool in node npm go python3 curl; do
  command -v "$tool" >/dev/null || missing+=("command: $tool")
done
if [[ "$mode" != --web-only ]]; then
  if [[ -z "${JAVA_HOME:-}" ]]; then
    for candidate in /usr/lib/jvm/java-17-openjdk /usr/lib/jvm/java-17-openjdk-amd64 \
      /usr/lib/jvm/temurin-17-jdk /usr/lib/jvm/java-17-temurin-jdk /usr/lib/jvm/java-17-*; do
      if [[ -x "$candidate/bin/java" && -x "$candidate/bin/javac" ]]; then
        version_text="$("$candidate/bin/java" -version 2>&1)"
        if [[ "$version_text" == *'version "17.'* ]]; then
          export JAVA_HOME="$candidate"
          break
        fi
      fi
    done
  fi
  if [[ ! -x "${JAVA_HOME:-}/bin/java" || ! -x "${JAVA_HOME:-}/bin/javac" ]]; then
    missing+=("JDK 17 (install temurin-17-jdk or set JAVA_HOME)")
  else
    version_text="$("$JAVA_HOME/bin/java" -version 2>&1)"
    [[ "$version_text" == *'version "17.'* ]] || missing+=("JAVA_HOME must select JDK 17")
  fi
  export ANDROID_HOME="${ANDROID_HOME:-/var/tmp/onepunch-android-sdk}"
  if [[ ! -d "$ANDROID_HOME/platforms/android-36" || ! -d "$ANDROID_HOME/build-tools/36.0.0" ]]; then
    missing+=("SDK platform 36 and build-tools 36.0.0 under ANDROID_HOME")
  fi
fi
export GOMAXPROCS=2
export CGO_ENABLED=1
command -v cc >/dev/null || missing+=("C compiler (install gcc for Go race tests)")
if (( ${#missing[@]} )); then
  printf 'Missing prerequisites:\n' >&2
  printf '  - %s\n' "${missing[@]}" >&2
  exit 1
fi
if [[ "${1:-}" == --preflight ]]; then
  echo "Prerequisites found: JAVA_HOME=$JAVA_HOME; ANDROID_HOME=$ANDROID_HOME"
  exit 0
fi
mkdir -p build
run_dir="$(mktemp -d "$root/build/verification-XXXXXXXX")"
scratch="$(mktemp -d /var/tmp/onepunch-verification-XXXXXXXX)"
# Transient system services need not have HOME/XDG_CACHE_HOME. Configure tools
# explicitly, and keep compiler/temp data off this host's RAM-backed /tmp.
export GOCACHE="$root/build/local-tools/go-cache"
export GOPATH="$root/build/local-tools/go-path"
export GOMODCACHE="$GOPATH/pkg/mod"
export npm_config_cache="$root/build/local-tools/npm-cache"
export XDG_CACHE_HOME="$root/build/local-tools/cache"
export TMPDIR="$scratch/tmp"
mkdir -p "$GOCACHE" "$GOMODCACHE" "$npm_config_cache" "$XDG_CACHE_HOME" "$TMPDIR"
api_pid=''
cleanup() {
  if [[ -n "$api_pid" ]]; then
    kill "$api_pid" 2>/dev/null || true
    wait "$api_pid" 2>/dev/null || true
  fi
}
finish() {
  local result=$?
  cleanup
  # systemd executes as root; make generated test reports readable by the owner.
  if [[ "$EUID" == 0 ]]; then
    chown -R -- "$(stat -c '%u:%g' "$root")" "$run_dir" || true
  fi
  return "$result"
}
trap finish EXIT
echo "Reports: $run_dir"
printf '%s\n' "$run_dir" > build/latest-verification-path.txt
git -c "safe.directory=$root" rev-parse HEAD > "$run_dir/source-head.txt"
git -c "safe.directory=$root" diff --binary > "$run_dir/source.patch"
# Include untracked test/script sources in the evidence, without signing/runtime data.
python3 - "$run_dir" <<'PY'
import hashlib, json, pathlib, sys
root = pathlib.Path.cwd()
paths = [p for folder in ('android/app/src', 'web/src', 'shared', 'scripts')
         for p in (root / folder).rglob('*') if p.is_file() and '__pycache__' not in p.parts]
pathlib.Path(sys.argv[1], 'source-hashes.json').write_text(json.dumps({
    str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(paths)
}, indent=2) + '\n')
PY

node --max-old-space-size=128 scripts/check-light.mjs | tee "$run_dir/light.txt"
python3 -m unittest discover -s scripts -p 'test_release*.py' 2>&1 | tee "$run_dir/release-tests.txt"
python3 scripts/release_config.py | tee "$run_dir/release-config.txt"
if [[ "$mode" != --web-only ]]; then
  bash scripts/check-android-local.sh 2>&1 | tee "$run_dir/android.txt"
  cp -a android/app/build/test-results/testDebugUnitTest "$run_dir/android-jvm"
  sha256sum android/app/build/outputs/apk/debug/app-debug.apk > "$run_dir/apk-sha256.txt"
fi
# Keep npm's connection recovery within the enclosing 30-minute job deadline.
export npm_config_fetch_retries=3
export npm_config_fetch_retry_mintimeout=10000
export npm_config_fetch_retry_maxtimeout=30000
export npm_config_fetch_timeout=180000
export npm_config_maxsockets=2
npm ci --prefix web --no-audit --no-fund 2>&1 | tee "$run_dir/npm-ci.txt"
npm run build --prefix web 2>&1 | tee "$run_dir/web-build.txt"
(cd server && go test -p 1 -race ./...) 2>&1 | tee "$run_dir/go-race.txt"
(cd server && go build -p 1 -buildvcs=false -o "$run_dir/onepunch-api" .)
npm install --prefix "$scratch/browser" --no-save --no-audit --no-fund playwright@1.56.1
export PLAYWRIGHT_BROWSERS_PATH="$scratch/browsers"
export PLAYWRIGHT_MODULE="$scratch/browser/node_modules/playwright/index.mjs"
node "$scratch/browser/node_modules/playwright/cli.js" install chromium

for suite in check_web check_catalog_ui; do
  port="$(python3 - <<'PY'
import socket
with socket.socket() as s:
    s.bind(('127.0.0.1', 0))
    print(s.getsockname()[1])
PY
)"
  "$run_dir/onepunch-api" -addr "127.0.0.1:$port" -data "$run_dir/$suite-state.json" \
    -catalog shared/catalog.json -web web/dist > "$run_dir/$suite-api.txt" 2>&1 &
  api_pid=$!
  export ONEPUNCH_QA_URL="http://127.0.0.1:$port"
  for attempt in {1..30}; do
    kill -0 "$api_pid" || { cat "$run_dir/$suite-api.txt"; exit 1; }
    if curl --fail --silent "$ONEPUNCH_QA_URL/api/health" >/dev/null; then break; fi
    sleep 1
  done
  kill -0 "$api_pid"
  curl --fail --silent "$ONEPUNCH_QA_URL/api/health" > "$run_dir/$suite-health.json"
  ONEPUNCH_QA_OUT="$run_dir/$suite-screenshots" node "scripts/$suite.mjs" 2>&1 | tee "$run_dir/$suite.txt"
  cleanup
  api_pid=''
done
if [[ "$mode" == --web-only ]]; then
  printf 'Web build, Go race and browser checks passed. Android was not run in this job.\n' | tee "$run_dir/WEB-PASSED.txt"
else
  printf 'All build, JVM, Go race and browser checks passed. Device UI QA is separate.\n' | tee "$run_dir/PASSED.txt"
fi
echo "Reports: $run_dir"
