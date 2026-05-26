#!/usr/bin/env bash
# High-level helper for pulling a new upstream version into the
# classic-wip world. This script does NOT mutate the worktree by itself —
# it categorizes the diff and prints the next-step commands so the user
# can apply them deliberately.
#
# Workflow:
#   1. Categorize the diff between current HEAD and the upstream ref
#      (uses tools/classic/categorize-diff.sh).
#   2. Print a `git checkout` command for the ALWAYS_MERGE delta.
#   3. Print a reminder to re-pin DESIGN_FROZEN paths from baseline.
#   4. Print the `unknown` list — those need manual classification.
#
# Usage:
#   tools/classic/merge-version.sh <upstream-ref>
#
# See classic/PLAN.md §5.3.

set -euo pipefail

if [[ ${1:-} == -h || ${1:-} == --help || $# -lt 1 ]]; then
  sed -n '2,18p' "$0"
  exit 1
fi

upstream=$1
repo_root=$(git rev-parse --show-toplevel)
tools=$repo_root/tools/classic
current=$(git rev-parse HEAD)

git rev-parse --verify "$upstream" >/dev/null

safe_ref=$(printf '%s' "$upstream" | tr '/' '_')
outdir=/tmp/classic-merge/$safe_ref
mkdir -p "$outdir"

echo "Step 1/4: categorize diff $current..$upstream"
"$tools/categorize-diff.sh" "$current" "$upstream" "$outdir"

echo
echo "Step 2/4: apply ALWAYS_MERGE delta (review and commit manually)"
merge_list=$outdir/merge.txt
if [[ -s $merge_list ]]; then
  echo "  $(wc -l < "$merge_list") files would be checked out from $upstream"
  echo "  Review $merge_list, then run:"
  echo "    xargs -a $merge_list git checkout $upstream --"
else
  echo "  (empty)"
fi

echo
echo "Step 3/4: re-pin DESIGN_FROZEN files from baseline"
echo "  Run: $tools/restore-frozen.sh"

echo
echo "Step 4/4: review unknown files"
unknown_list=$outdir/unknown.txt
if [[ -s $unknown_list ]]; then
  total=$(wc -l < "$unknown_list")
  echo "  $total files need manual classification:"
  head -n 20 "$unknown_list" | sed 's/^/    /'
  if (( total > 20 )); then
    echo "    ... ($((total - 20)) more — see $unknown_list)"
  fi
else
  echo "  (empty — all files classified)"
fi
