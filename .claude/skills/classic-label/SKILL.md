---
name: classic-label
description: Label open Forkgram Classic GitHub issues with the release version that ships their fix (e.g. "12.7.9"). Use after a release, for retro audits ("which release fixed #N?"), or to move/correct version labels. Args; "<version> <N> [<N>...]" applies one version to given issues; no args = full reconcile of all open issues against release tags.
---

# Forkgram Classic — version-label released issues

Open issues whose fix has shipped get a **version label** = bare release name (e.g. `12.7.9`),
so the reporter knows which F-Droid build to verify. The issue **stays open** — the reporter
confirms and closes. Labels are the agreed signal; don't post comments unless the user asks.

## Conventions
- Label name: bare `APP_VERSION_NAME` (`12.7.9`), **not** the tag (`12.7.9.0`).
- Color `1d76db` (blue), description `Fixed in <version> (F-Droid)`.
- One issue can end up with several version labels over time (fix improved in a later release);
  when the user says "поменяй X на Y" use `--remove-label X --add-label Y` and delete the old
  label from the repo if nothing else uses it (`gh label delete`).

## Auth gotcha (always)
`gh` write operations on `forkgram/*` need the **23rd** account — the default `superheher`
gets HTTP 404 on label/issue edits. Wrap every write block:
```bash
gh auth switch -u 23rd
# ... writes ...
gh auth switch -u superheher     # ALWAYS restore, even on failure
```

## Mode A — apply one version to known issues (args: `<version> <N> [<N>...]`)
Also the path `/classic-release` step 5 uses (issues from `.claude/pending-release-issues.txt`):
```bash
ver=12.7.9                       # from args
gh auth switch -u 23rd
gh label create "$ver" -R forkgram/forkgram-classic --color 1d76db --description "Fixed in $ver (F-Droid)" 2>/dev/null || true
for n in 23 14; do               # from args / pending file
  state=$(gh issue view "$n" -R forkgram/forkgram-classic --json state --jq .state 2>/dev/null)
  [ "$state" = "OPEN" ] && gh issue edit "$n" -R forkgram/forkgram-classic --add-label "$ver"
done
gh auth switch -u superheher
```

## Mode B — full reconcile (no args)
Map every open issue to the **first release tag whose tree contains its fix marker** (fix commits
tag changed lines with `// [classic] #N:`; the marker is the only durable issue↔code link because
`/classic-fold` melts the per-issue commits).
```bash
cd /home/h/src/forkgram-classic
git fetch origin 'refs/tags/12.*:refs/tags/12.*' 2>/dev/null   # make sure all release tags are local
# GOTCHA (zsh): `for t in $tags` does NOT line-split a variable — iterate the command substitution directly.
# GOTCHA (git grep): a bare `**` pathspec matches nothing — use the plain directory prefix.
gh issue list -R forkgram/forkgram-classic --state open --json number --jq '.[].number' | while read -r n; do
  first=""
  for t in $(git tag --list '12.*.0' | sort -V); do
    if git grep -q "\[classic\] #${n}[:. ]" "$t" -- 'TMessagesProj/src/main/java' 2>/dev/null; then first="$t"; break; fi
  done
  echo "#${n}: ${first:-no marker — not shipped (or fixed without a marker)}"
done
```
- Strip the trailing `.0` from the tag for the label name.
- **Show the resulting table to the user and confirm before applying** — a marker proves code
  shipped, not that the fix is complete (e.g. the user may prefer pointing at a later version where
  the area was finished: on 2026-06-11 they moved #3/#5/#8 from first-shipped 12.7.6 to 12.7.8).
- Skip non-bug issues (e.g. #22 "Thanks?") and issues already carrying the right label.
- Apply per Mode A (create label once per version, batch the `gh issue edit`s).

## Relationship to the pipeline
`/classic-issue` writes the `// [classic] #N:` markers → `/classic-fold` saves folded numbers to
`.claude/pending-release-issues.txt` → `/classic-release` step 5 calls **Mode A** with the new version →
this skill standalone covers retro audits and corrections.
