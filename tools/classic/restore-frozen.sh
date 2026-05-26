#!/usr/bin/env bash
# Restore (or remove) every path in classic/DESIGN_FROZEN.txt from the
# baseline commit (default: classic/BASELINE.txt).
#
# Behavior per path:
#   - exists at baseline  → `git checkout <baseline> -- <path>` (overwrites
#     the worktree copy with the baseline version).
#   - missing at baseline → `git rm` if present in worktree (these are
#     "new redesign files" we don't want).
#   - glob patterns (`**`, `*`) → expanded against the file lists of both
#     the baseline tree and HEAD.
#
# Usage:
#   tools/classic/restore-frozen.sh [baseline-ref]
#
# See classic/PLAN.md §5.2.

set -euo pipefail

repo_root=$(git rev-parse --show-toplevel)
frozen_file=$repo_root/classic/DESIGN_FROZEN.txt
baseline_file=$repo_root/classic/BASELINE.txt

baseline=${1:-}
if [[ -z $baseline && -f $baseline_file ]]; then
  baseline=$(grep -vE '^[[:space:]]*(#|$)' "$baseline_file" | head -n1)
fi
baseline=${baseline:-9cbf03332}

if ! git rev-parse --verify "$baseline" >/dev/null 2>&1; then
  echo "error: baseline ref '$baseline' is not a valid revision" >&2
  exit 1
fi

echo "Restoring DESIGN_FROZEN paths from $baseline"
mapfile -t patterns < <(grep -vE '^[[:space:]]*(#|$)' "$frozen_file" | sed 's/[[:space:]]*$//')
mapfile -t baseline_files < <(git ls-tree -r --name-only "$baseline")
mapfile -t head_files < <(git ls-files)

# Build hash sets for O(1) lookup via sorted arrays + comm.
baseline_sorted=$(mktemp)
head_sorted=$(mktemp)
trap 'rm -f "$baseline_sorted" "$head_sorted"' EXIT
printf '%s\n' "${baseline_files[@]}" | LC_ALL=C sort -u > "$baseline_sorted"
printf '%s\n' "${head_files[@]}"     | LC_ALL=C sort -u > "$head_sorted"

is_in() {
  LC_ALL=C grep -qxF "$1" "$2"
}

restored=0
removed=0
missing=0

for pat in "${patterns[@]}"; do
  if [[ $pat != *\** ]]; then
    if is_in "$pat" "$baseline_sorted"; then
      git checkout "$baseline" -- "$pat"
      restored=$((restored + 1))
    elif is_in "$pat" "$head_sorted"; then
      git rm -- "$pat" >/dev/null
      removed=$((removed + 1))
    else
      missing=$((missing + 1))
      echo "  skip (neither baseline nor HEAD): $pat"
    fi
    continue
  fi
  expanded=${pat//\*\*/\*}
  while IFS= read -r f; do
    # shellcheck disable=SC2053
    if [[ $f == $expanded ]]; then
      git checkout "$baseline" -- "$f"
      restored=$((restored + 1))
    fi
  done < "$baseline_sorted"
  while IFS= read -r f; do
    # shellcheck disable=SC2053
    if [[ $f == $expanded ]] && ! is_in "$f" "$baseline_sorted"; then
      git rm -- "$f" >/dev/null
      removed=$((removed + 1))
    fi
  done < "$head_sorted"
done

printf '\nDone: restored=%d, removed=%d, skipped=%d\n' "$restored" "$removed" "$missing"
echo "Review changes with: git status && git diff --stat"
