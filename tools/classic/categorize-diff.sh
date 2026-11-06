#!/usr/bin/env bash
# Categorize the diff between two git refs into three buckets using the
# Forkgram Classic boundary files (classic/DESIGN_FROZEN.txt and
# classic/ALWAYS_MERGE.txt).
#
# Usage:
#   tools/classic/categorize-diff.sh <ref1> <ref2> [outdir]
#
# Output (under outdir, default /tmp/classic-diff/<ref2>):
#   frozen.txt   — files matching DESIGN_FROZEN patterns
#   merge.txt    — files matching ALWAYS_MERGE patterns
#   unknown.txt  — files matching neither (need manual decision)
#
# Patterns support `**` (any subtree) and `*` (single path segment).
#
# See classic/PLAN.md §5.1.

set -euo pipefail

if [[ ${1:-} == -h || ${1:-} == --help || $# -lt 2 ]]; then
  sed -n '2,17p' "$0"
  exit 1
fi

ref1=$1
ref2=$2
repo_root=$(git rev-parse --show-toplevel)
safe_ref=$(printf '%s' "$ref2" | tr '/' '_')
outdir=${3:-/tmp/classic-diff/$safe_ref}
mkdir -p "$outdir"

frozen_file=$repo_root/classic/DESIGN_FROZEN.txt
merge_file=$repo_root/classic/ALWAYS_MERGE.txt

read_patterns() {
  [[ -f $1 ]] || return 0
  grep -vE '^[[:space:]]*(#|$)' "$1" | sed 's/[[:space:]]*$//' || true
}

mapfile -t frozen_pats < <(read_patterns "$frozen_file")
mapfile -t merge_pats  < <(read_patterns "$merge_file")

match_file() {
  local file=$1 patname=$2
  local -n arr="$patname"
  ((${#arr[@]})) || return 1
  local pat p
  for pat in "${arr[@]}"; do
    p=${pat//\*\*/\*}
    # shellcheck disable=SC2053
    [[ $file == $p ]] && return 0
  done
  return 1
}

: > "$outdir/frozen.txt"
: > "$outdir/merge.txt"
: > "$outdir/unknown.txt"

while IFS= read -r file; do
  if match_file "$file" frozen_pats; then
    echo "$file" >> "$outdir/frozen.txt"
  elif match_file "$file" merge_pats; then
    echo "$file" >> "$outdir/merge.txt"
  else
    echo "$file" >> "$outdir/unknown.txt"
  fi
done < <(git diff --name-only "$ref1" "$ref2")

printf '=== Diff categorization: %s..%s ===\n' "$ref1" "$ref2"
printf '  frozen:  %5d files\n' "$(wc -l < "$outdir/frozen.txt")"
printf '  merge:   %5d files\n' "$(wc -l < "$outdir/merge.txt")"
printf '  unknown: %5d files\n' "$(wc -l < "$outdir/unknown.txt")"
printf 'Output: %s\n' "$outdir"
