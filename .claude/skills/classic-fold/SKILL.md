---
name: classic-fold
description: Fold the accumulated temporary "[classic] #N:" feature commits into the single umbrella [classic] commit, re-creating the release commit ("Updated version to <name>.") at HEAD, so the classic branch is back to its canonical two-commit shape (umbrella + release). Use when the user asks to fold / squash / tidy / reorganize the classic commits, or to prepare for a release.
---

# Forkgram Classic — fold everything into the single [classic] commit

The `classic` branch is **exactly two commits** above the modern base:

1. `[classic] Added classic design preservation to Forkgram.` — the **only** `[classic]` commit;
   all work (pipeline, UI, rebrand, docs, CI, every issue fix) lives here.
2. `Updated version to <APP_VERSION_NAME>.` — release only, HEAD, version lines only; the next release replaces it.

`/classic-issue` adds temporary `[classic] #N:` feature commits on top. This skill **squashes all of
them (and any other classic-shaped commit) into the umbrella commit**, re-creates the release commit
from the version values at HEAD, and leaves the tree byte-identical. It rewrites local history, so it
stays local (no push); `/classic-release` is what pushes.

## 1. Preconditions
```bash
cd /home/h/src/forkgram-classic
git rev-parse --abbrev-ref HEAD          # must be: classic
git status --porcelain --untracked-files=no | grep -vE 'TMessagesProj/jni/libvpx'   # must be EMPTY
git log --oneline -1
```
- Working tree must be clean (tracked files; the `libvpx` submodule is ignored by convention).
- If the stack is already canonical — the umbrella commit, optionally the release commit at HEAD, and
  nothing else — **nothing to fold, stop.**

## 2. Collect the issue numbers and version pair
```bash
UMB_SUBJECT='[classic] Added classic design preservation to Forkgram.'
U=$(git log --format='%H %s' | awk -v s="$UMB_SUBJECT" 'index($0,s){print $1; exit}')
[ -n "$U" ] || { echo "no umbrella commit — not a foldable classic stack"; exit 1; }
# The fold discards the temporary subjects; keep the issue numbers for /classic-label.
git log --format='%s%n%b' "$U..HEAD" | grep -oE '#[0-9]+' | tr -d '#' >> .claude/pending-release-issues.txt || true
sort -un .claude/pending-release-issues.txt -o .claude/pending-release-issues.txt
# Version pair: the umbrella holds the pre-release values, HEAD the released ones.
umb_code=$(git show "$U:gradle.properties" | sed -n 's/^APP_VERSION_CODE=//p')
umb_name=$(git show "$U:gradle.properties" | sed -n 's/^APP_VERSION_NAME=//p')
head_code=$(sed -n 's/^APP_VERSION_CODE=//p' gradle.properties)
head_name=$(sed -n 's/^APP_VERSION_NAME=//p' gradle.properties)
had_release=$( git log -1 --format=%s | grep -qE '^Updated version to ' && echo yes || echo no )
rel_subject="Updated version to $head_name."
echo "umbrella $umb_name/$umb_code | head $head_name/$head_code | release: $had_release"
```
- If there is no release commit (`had_release=no`), the umbrella and HEAD values are the same and no
  release commit is created.
- Sanity: if `head_name`/`head_code` differ from `umb_name`/`umb_code`, the difference must be exactly
  the release bump (`/classic-release` rules). Anything else means the stack is wrong — stop and report.

## 3. Squash into the umbrella commit
```bash
git tag prefold-backup HEAD
BASE=$(git rev-parse "$U^")
git reset --soft "$BASE"          # HEAD moves to the base; index stays at the old HEAD tree
sed -i "s/^APP_VERSION_CODE=.*/APP_VERSION_CODE=$umb_code/; s/^APP_VERSION_NAME=.*/APP_VERSION_NAME=$umb_name/" gradle.properties
git add gradle.properties
git commit -q -m "$UMB_SUBJECT"
if [ "$had_release" = yes ]; then
  sed -i "s/^APP_VERSION_CODE=.*/APP_VERSION_CODE=$head_code/; s/^APP_VERSION_NAME=.*/APP_VERSION_NAME=$head_name/" gradle.properties
  git add gradle.properties
  git commit -q -m "$rel_subject"
fi
```
- `reset --soft` never touches the index, so the old HEAD tree (minus the version pair we rewrite) is
  committed as-is; the worktree's other differences from the base are not re-examined.
- Both commits are created with a **subject only** — never add `-m <body>`.

## 4. Verify (then stop — do not push)
```bash
git log --oneline -3                  # umbrella, and the release commit at HEAD when released
git diff prefold-backup HEAD --stat   # MUST print nothing — the tree is byte-identical
git log -2 --format='[%s] body=[%b]'  # every body must be empty
python3 ~/kit/projects/forkgram-classic/check_convention.py --repo .
```
- If the tree diff is not empty, undo (`git reset --hard prefold-backup`) and investigate; do not
  force a fold that changes content.
- If the checker is green, delete the safety tag (`git tag -d prefold-backup`) and report the new
  two-commit shape. **Do not push.** Releasing the folded state is `/classic-release`.

## Safety net
Before step 3, note the current tip so a bad fold is trivially undone:
```bash
git tag prefold-backup            # or: git branch backup/pre-fold-<date>
# undo a finished-but-wrong fold:  git reset --hard prefold-backup
```
