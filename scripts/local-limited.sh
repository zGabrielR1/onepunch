#!/usr/bin/env bash
# Run locally in WSL/Linux as root. The whole process tree shares a hard cap.
set -euo pipefail
if [[ "${1:-}" == --inside ]]; then
  shift
  group="$(awk -F: '$1 == "0" {print $3}' /proc/self/cgroup)"
  memory="$(cat "/sys/fs/cgroup$group/memory.max")"
  swap="$(cat "/sys/fs/cgroup$group/memory.swap.max")"
  echo "Verified process-tree limits: memory.max=$memory bytes; memory.swap.max=$swap bytes"
  [[ "$memory" == 3221225472 && "$swap" == 0 ]] || exit 1
  exec "$@"
fi
if [[ $# -eq 0 ]]; then
  echo 'Usage: bash scripts/local-limited.sh command [args...]' >&2
  exit 2
fi
unit="onepunch-local-$(date +%s)-$$"
exec systemd-run --wait --pipe --collect --unit="$unit" \
  -p MemoryMax=3G -p MemorySwapMax=0 -p CPUQuota=200% \
  -p TasksMax=256 -p RuntimeMaxSec=1800 -p KillMode=control-group \
  --working-directory="$PWD" /bin/bash "$PWD/scripts/local-limited.sh" --inside "$@"
