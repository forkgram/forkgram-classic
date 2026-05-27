---
name: classic-release
description: Cut a new Forkgram Classic release — bump the version inside the existing "Bumped version" commit, force-push the classic branch, create the version tag, and push it (which triggers the automatic F-Droid build/publish). Use when the user asks to release / ship / cut a new version / "сделай релиз" of forkgram-classic.
---

# Forkgram Classic — release

This repo is **exactly 5 commits** layered on upstream. The 5th (HEAD) is `Bumped version.` and
touches only `gradle.properties`. A release does **not** add a 6th commit — it squashes the version
bump *into* that commit, force-pushes, then pushes a tag. F-Droid watches the tag and builds/publishes
the new version automatically (it parses the manifest, builds every ABI, ships) — nothing else needed.

## Bump rules
- `APP_VERSION_NAME` (gradle.properties): increment the **last dotted component** by **1** — e.g. `12.7.6 → 12.7.7`.
- `APP_VERSION_CODE` (gradle.properties): **+1** — e.g. `6753 → 6754`. (Just needs to strictly increase
  for F-Droid; one per release.)
- Release tag (lightweight): `<new APP_VERSION_NAME>.0` — e.g. `12.7.7.0`.

## Procedure

Run everything from the repo. **Steps 1–3 are local and reversible; step 4 (push) is irreversible and
public — confirm before it.**

### 1. Preconditions — abort (don't proceed) if any fail
```bash
cd /home/h/src/forkgram-classic
echo "branch: $(git rev-parse --abbrev-ref HEAD)"          # must be: classic
echo "head:   $(git log -1 --format=%s)"                    # must be: Bumped version.
echo "dirty:  [$(git status --porcelain --untracked-files=no | grep -vE 'TMessagesProj/jni/libvpx' | tr '\n' ';')]"  # must be empty [] (untracked files are ignored; only tracked changes block a release)
echo "commits-on-baseline: $(git rev-list --count 7fa80f16e..HEAD)"  # sanity: small (the classic stack)
```
- If branch ≠ `classic`, HEAD subject ≠ `Bumped version.`, or `dirty` is non-empty (any uncommitted
  tracked change other than the `libvpx` submodule) → **STOP**, tell the user, do nothing.

### 2. Compute the bump, check the tag is free, apply it
```bash
code=$(grep -E '^APP_VERSION_CODE=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')
name=$(grep -E '^APP_VERSION_NAME=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')
newcode=$((code + 1))
newname="${name%.*}.$(( ${name##*.} + 1 ))"
tag="${newname}.0"
echo "NAME: $name -> $newname    CODE: $code -> $newcode    TAG: $tag"
if git rev-parse -q --verify "refs/tags/$tag" >/dev/null; then echo "!! tag $tag ALREADY EXISTS — STOP"; fi
sed -i "s/^APP_VERSION_CODE=.*/APP_VERSION_CODE=${newcode}/; s/^APP_VERSION_NAME=.*/APP_VERSION_NAME=${newname}/" gradle.properties
git --no-pager diff gradle.properties
```
- If the tag already exists → **STOP** (don't re-release the same version).
- The diff must show exactly the two version lines changing.

### 3. Squash into the "Bumped version" commit and tag (local only)
```bash
git add gradle.properties
git commit --amend --no-edit          # keeps subject "Bumped version." — stays 5 commits
git tag "$tag"                         # lightweight, matches existing 12.7.x.0 tags
git log --oneline -1 && echo "tagged $tag"
```

### 4. Confirm, then push  ⚠️ irreversible + public (triggers an F-Droid release build)
Present a one-line summary — `NAME old→new`, `CODE old→new`, `TAG`, and that this will
**force-push `classic` and push tag `$tag`**. **Wait for the user's explicit "yes".** Only then:
```bash
git push --force-with-lease origin classic
git push origin "$tag"
```
- `origin` = `git@github-23rd:forkgram/forkgram-classic.git` (SSH alias `github-23rd` → pushes as `23rd`,
  not the `gh` default account). If a push ever authenticates as the wrong user, the alias is the cause.
- If `--force-with-lease` is rejected (origin advanced unexpectedly) → **STOP and investigate**; do not
  blindly `--force`.

### 5. Label the released issues (after a successful push)
`/classic-fold` appends the folded issue numbers to `.claude/pending-release-issues.txt` (the fold
discards the `[classic] #N:` subjects, so this file is the only record). Run the **`/classic-label`
skill, Mode A**, with version `$newname` and the numbers from that file — it creates the version
label (`$newname`, blue `1d76db`, "Fixed in $newname (F-Droid)") and adds it to each still-OPEN
issue, handling the gh-account switch (writes need `23rd`; restore `superheher` after). Then
truncate the file (`: > .claude/pending-release-issues.txt`).
- Leave the issues **open** — the reporter confirms and closes. No comments unless the user asks.
- If the push was aborted, skip this step and keep the file for the next release.

### 6. Report
State the released version + tag, which issues got the version label, and that F-Droid will pick up
the tag and build/publish; no further action is needed. (Old release tags `12.7.x.0` stay; that's fine.)

## Guardrails
- **Never add a 6th commit** and never change the other 4 commits — only amend `Bumped version.`.
- Do the bump strictly per the rules above; if the user wants a different jump (e.g. an upstream
  rebase changed the major/minor), they'll say so — otherwise last-component +1 / code +1.
- This skill only releases the **current local `classic` state**. If recent fixes aren't folded into
  the 5 commits yet, fold them first (see `classic/PHASE2_BACKLOG.md` and project memory) before releasing.
