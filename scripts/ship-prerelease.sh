#!/usr/bin/env bash
# Owner-invoked GitHub build/publish, no local compilation or private key access.
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"
repo='zGabrielR1/onepunch'
command -v gh >/dev/null || { echo 'Install GitHub CLI first: sudo dnf install gh' >&2; exit 1; }
if ! gh auth status --hostname github.com >/dev/null 2>&1; then
  gh auth login --hostname github.com --web
fi
if [[ -n "$(git status --porcelain)" ]]; then
  echo 'Refusing to ship an uncommitted workspace. Review and commit the intended release first.' >&2
  exit 1
fi
source="$(git rev-parse HEAD)"
branch="$(git branch --show-current)"
[[ -n "$branch" ]] || { echo 'A named release branch is required.' >&2; exit 1; }
version="$(python3 - <<'PY'
import sys
sys.path.insert(0, 'scripts')
from release_config import read_release_config
config = read_release_config()
if config['prerelease'] != 'true' or '-' not in config['version']:
    raise SystemExit('This command publishes prereleases only.')
print(config['version'])
PY
)"
tag="v$version"
gh api "repos/$repo" --jq '.full_name' >/dev/null
check="$(mktemp)"
trap 'rm -f "$check"' EXIT
if gh api "repos/$repo/git/ref/tags/$tag" > /dev/null 2> "$check"; then
  echo "Tag $tag already exists; refusing to replace a release." >&2
  exit 1
elif ! python3 - "$check" <<'PY'
import pathlib, sys
raise SystemExit(0 if '(HTTP 404)' in pathlib.Path(sys.argv[1]).read_text() else 1)
PY
then
  cat "$check" >&2
  exit 1
fi
git -c credential.helper= -c 'credential.helper=!gh auth git-credential' \
  push "https://github.com/$repo.git" "HEAD:refs/heads/$branch"
previous_id="$(gh run list --repo "$repo" --workflow candidate.yml --branch "$branch" \
  --commit "$source" --limit 1 --json databaseId --jq '.[0].databaseId // empty')"
gh workflow run candidate.yml --repo "$repo" --ref "$branch" -f publish_prerelease=true
run_id=''
for attempt in {1..30}; do
  run_id="$(gh run list --repo "$repo" --workflow candidate.yml --branch "$branch" \
    --commit "$source" --limit 1 --json databaseId --jq '.[0].databaseId // empty')"
  [[ -n "$run_id" && "$run_id" != "$previous_id" ]] && break
  run_id=''
  sleep 2
done
[[ -n "$run_id" ]] || { echo 'Workflow dispatched; inspect GitHub Actions for its run.' >&2; exit 1; }
gh run watch "$run_id" --repo "$repo" --exit-status
output="$root/build/published-$tag-$run_id"
mkdir -p "$output"
gh release download "$tag" --repo "$repo" --dir "$output"
(cd "$output" && sha256sum -c SHA256SUMS.txt)
python3 - "$output/verification.json" "$source" <<'PY'
import json, sys
value = json.load(open(sys.argv[1]))
if value['commit'] != sys.argv[2] or value['android_variant'] != 'release' or value['prerelease'] is not True:
    raise SystemExit('Published artifact source/variant mismatch')
PY
gh release view "$tag" --repo "$repo" --json url,tagName,isPrerelease,targetCommitish
echo "Downloaded release files verified: $output"
