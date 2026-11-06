---
name: classic-release
description: Cut a new Forkgram Classic release — replace the "Updated version to <name>." release commit with a fresh one carrying the new version, force-push the classic branch, create the version tag, and push it (which triggers the automatic F-Droid build/publish). Use when the user asks to release / ship / cut a new version / "сделай релиз" of forkgram-classic.
---

# Forkgram Classic — release

This repo is **exactly 2 commits** above the modern base: the single
`[classic] Added classic design preservation to Forkgram.` umbrella commit, and `Updated version to <APP_VERSION_NAME>.` at
HEAD touching only the two version lines of `gradle.properties`. A release does **not** add a commit —
it **replaces** the release commit with a fresh `Updated version to <name>.` carrying the new version, then
force-pushes the branch and pushes a tag. F-Droid watches the tag and builds/publishes the new version
automatically (it parses the manifest, builds every ABI, ships) — nothing else needed.

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
echo "head:   $(git log -1 --format=%s)"                    # must match: ^Updated version to 
echo "stack:  $(git log -1 --format=%s HEAD~1)"             # must be the umbrella [classic] commit
echo "count:  $(git rev-list --count HEAD~2..HEAD)"         # must be: 2
echo "dirty:  [$(git status --porcelain --untracked-files=no | grep -vE 'TMessagesProj/jni/libvpx' | tr '\n' ';')]"  # must be empty []
python3 ~/kit/projects/forkgram-classic/check_convention.py --repo .
```
- If branch ≠ `classic`, HEAD subject does not match `^Updated version to `, the count isn't 2, the checker is red, or
  `dirty` is non-empty (any uncommitted tracked change other than the `libvpx` submodule) →
  **STOP**, tell the user, do nothing.
- Unfolded temporary commits above the release commit mean `/classic-fold` has not run — fold
  first, then release.

### 2. Compute the bump, check the tag is free
```bash
code=$(grep -E '^APP_VERSION_CODE=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')
name=$(grep -E '^APP_VERSION_NAME=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')
newcode=$((code + 1))
newname="${name%.*}.$(( ${name##*.} + 1 ))"
tag="${newname}.0"
echo "NAME: $name -> $newname    CODE: $code -> $newcode    TAG: $tag"
if git rev-parse -q --verify "refs/tags/$tag" >/dev/null; then echo "!! tag $tag ALREADY EXISTS — STOP"; fi
```
- If the tag already exists → **STOP** (don't re-release the same version).

### 3. Replace the release commit with the new version and tag (local only)
```bash
git reset --soft HEAD~1               # drop the old release commit; index = umbrella tree
sed -i "s/^APP_VERSION_CODE=.*/APP_VERSION_CODE=${newcode}/; s/^APP_VERSION_NAME=.*/APP_VERSION_NAME=${newname}/" gradle.properties
git add gradle.properties
git --no-pager diff --cached --stat   # must show only gradle.properties, 2 lines
git commit -q -m "Updated version to ${newname}."   # fresh release commit — still exactly 2 commits
git tag "$tag"                        # lightweight, matches existing X.Y.Z.0 tags
git log --oneline -2 && echo "tagged $tag"
```
- The umbrella commit is untouched: the new release commit is the only difference and it carries only
  the two version lines. Never reword or amend the umbrella commit during a release.
- If `git diff --cached --stat` shows anything besides `gradle.properties`, **STOP** — the stack is
  wrong (fold first).

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
discards temporary subjects, so this file is the only record). Run the **`/classic-label`
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
- **Never add a third commit** and never modify the umbrella commit — only replace the release commit.
- Do the bump strictly per the rules above; if the user wants a different jump (e.g. an upstream
  rebase changed the major/minor), they'll say so — otherwise last-component +1 / code +1.
- This skill only releases the **current local `classic` state**. If recent fixes aren't folded into
  the umbrella commit yet, fold them first (`/classic-fold`) before releasing.
