---
name: classic-fold
description: Fold the accumulated temporary "[classic] #N" feature commits down into the four foundational [classic] commits, returning the classic branch to its canonical 5-commit shape (4 foundational + "Bumped version" at HEAD) so it is releasable. Use when the user asks to fold / squash / tidy / reorganize the classic commits, or to prepare for a release.
---

# Forkgram Classic — fold feature commits into the four foundational commits

The `classic` branch is meant to be **exactly 5 commits** layered on upstream Telegram (see the
`/classic-release` skill, which refuses to run otherwise):

1. `[classic] Set up the Classic design-preservation pipeline.`   — **build / infra**
2. `[classic] Restored the classic UI baseline and ported poll v2 (Phase 2).`  — **UI/UX restoration**
3. `[classic] Rebranded the app to Forkgram Classic.`             — **branding / identity**
4. `[classic] Reworked the F-Droid store listing, README and project docs.`  — **F-Droid / docs**
5. `Bumped version.`                                              — version only (HEAD; `/classic-release` amends it)

`/classic-issue` adds temporary `[classic] #N:` feature commits on top. This skill **folds each of them
into the right foundational commit** (`fixup`), drops the separate commits, and leaves `Bumped version.`
back at HEAD — never adding a 6th commit and never re-touching upstream. Non-destructive intent, but it
**rewrites local history** — it stays local (no push); `/classic-release` is what pushes.

## 1. Preconditions
```bash
cd /home/h/src/forkgram-classic
git rev-parse --abbrev-ref HEAD          # must be: classic
git status --porcelain --untracked-files=no | grep -vE 'TMessagesProj/jni/libvpx'   # must be EMPTY (commit/stash first)
git log --oneline --format='%h %s' | sed -n '1,30p'
```
- Working tree must be clean (tracked files; the `libvpx` submodule and untracked files are ignored).
- If HEAD is already `Bumped version.` with only the 4 foundational below it → **nothing to fold, stop.**
- Identify commits by **subject**, not hash (hashes change on every rebase). The four foundational subjects
  and `Bumped version.` are listed above; everything else `[classic] #N:` above `Bumped version.` is a
  feature commit to fold.

## 2. Route each feature commit to a foundational commit
For every feature commit, inspect what it touches and pick its target:
```bash
git show --stat --format='%h %s' <feat>      # files decide the target
```
| Feature touches… | Fold into |
|---|---|
| `TMessagesProj/src/main/java/org/telegram/ui/**`, `res/**`, drawables/layouts/colors (the usual issue fix) | **#2 UI baseline** |
| `*.gradle`, `gradle.properties` (non-version), `jni/**`, `.github/**`, build flavors / `F_DROID` plumbing | **#1 pipeline** |
| app name / `applicationId` / package, launcher `mipmap-*`, "Forkgram"/store-name strings | **#3 rebrand** |
| `fastlane/**`, `metadata/**`, `README*`, `classic/*.md`, `fdroiddata/**`, F-Droid listing | **#4 F-Droid/docs** |
| pure version bump (`APP_VERSION_*`) | leave for `/classic-release` (don't fold) |

- **Default:** a `[classic] #N:` issue fix is almost always a UI restoration → **#2 UI baseline**. When a
  commit spans categories, route by its *dominant* change; if it's a tiny cross-cutting bit, #2 is the safe default.
- Print the routing plan (`<feat> → #k`) and confirm it with the user before rewriting history.

## 3. Fold via one non-interactive rebase
`git rebase -i` works here **only with the editors stubbed out** (interactive editing is unavailable):
generate the todo yourself and feed it in. Build `/tmp/fold_todo.txt` as the full pick-list from the
foundational base, with each foundational commit kept as `pick` and **each feature `fixup`-ed in directly
after its target foundational commit, preserving the features' original relative order**; `Bumped version.`
stays the last `pick`.

Resolve the five foundational hashes by subject, then emit the todo **one line per feature** — a
`for f in $LIST` word-splits and silently drops the `fixup ` prefix on all but the first, so use a
`while read` loop. The common case is **all features → F2**:
```bash
H(){ git log --format='%H %s' | awk -v s="$1" 'index($0,s){print $1}' | tail -1; }
F1=$(H "Set up the Classic design-preservation pipeline.")
F2=$(H "Restored the classic UI baseline and ported poll v2")
F3=$(H "Rebranded the app to Forkgram Classic.")
F4=$(H "Reworked the F-Droid store listing"); BV=$(H "Bumped version.")
{
  printf 'pick %s\n' "$F1"
  printf 'pick %s\n' "$F2"
  git log --reverse --format='%H' "${BV}..HEAD" | while IFS= read -r f; do printf 'fixup %s\n' "$f"; done
  printf 'pick %s\n' "$F3"
  printf 'pick %s\n' "$F4"
  printf 'pick %s\n' "$BV"
} > /tmp/fold_todo.txt
awk '{print NR": "$1}' /tmp/fold_todo.txt            # eyeball: pick,pick,fixup×N,pick,pick,pick
# Folding DISCARDS the "[classic] #N:" subjects — capture the issue numbers first, so
# /classic-release can label those issues with the released version (see the /classic-release skill):
git log --format='%s' "${BV}..HEAD" | grep -oE '#[0-9]+' | tr -d '#' >> .claude/pending-release-issues.txt 2>/dev/null || true
sort -un .claude/pending-release-issues.txt -o .claude/pending-release-issues.txt 2>/dev/null || true
git tag prefold-backup HEAD                           # safety net
GIT_SEQUENCE_EDITOR='cp /tmp/fold_todo.txt' GIT_EDITOR=true git rebase -i "${F1}^"
```
- `fixup` melts a commit into the one above and **discards its message** (the foundational commit's is
  kept). `${F1}^` (parent of foundational #1) is the rebase base, so all four foundational commits are in
  the todo and re-applied. When some features go to **different** targets, interleave their `fixup` lines
  after the matching `pick` instead of lumping them under F2.
- **Order caveat:** if a file is *created* by a foundational commit and *modified* by a feature you route
  to an *earlier* foundational commit, the fixup lands before the file exists → conflict (e.g. an issue's
  planning doc created in #4 but the same doc tweaked by a UI commit routed to #2). Keep order monotonic:
  route such a feature to the **same or later** target — e.g. keep an issue's planning doc with its UI work
  in #2, or just drop throwaway scaffolding docs. (Validated: routing all current UI features into F2
  rebases with **zero conflicts** and a byte-identical tree.)
- **Conflicts:** resolve to the **union** intent (the foundational change *and* the folded feature change),
  `git add`, `GIT_EDITOR=true git rebase --continue`. Bail anytime: `git rebase --abort` (restores the
  branch exactly), or after a finished-but-wrong fold `git reset --hard prefold-backup`.

## 4. Verify (then stop — do not push)
```bash
git log --oneline                         # EXACTLY 5: the 4 foundational + "Bumped version." at HEAD
git log -1 --format=%s                    # must be: Bumped version.
git rev-list --count <BASE_subjects_parent>..HEAD   # sanity: 5
# build once to prove the folded tree still compiles:
JAVA_HOME=/usr/lib/jvm/java-21 ANDROID_HOME=/opt/android-sdk ./gradlew :TMessagesProj_App:assembleAfatDebug -PF_DROID=1 --console=plain 2>&1 | grep -E "BUILD (SUCCESSFUL|FAILED)|error:"
```
- HEAD must be `Bumped version.` and the count must be exactly 5. If a foundational commit's *content* must
  also bump (rare), that's still its own commit — never collapse the four into fewer.
- **Do not push.** Releasing the folded state is the `/classic-release` skill (it amends `Bumped version.`,
  force-pushes `classic`, pushes the tag). Report the new 5-commit shape and that it's ready to release.

## Safety net
Before step 3, note the current tip so a bad fold is trivially undone:
```bash
git tag prefold-backup            # or: git branch prefold-backup
# undo a finished-but-wrong fold:  git reset --hard prefold-backup
```
Delete the backup tag once the fold is verified good.
