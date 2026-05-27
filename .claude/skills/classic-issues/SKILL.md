---
name: classic-issues
description: Batch-resolve the open Forkgram Classic GitHub issues — fetch the open issues, build a worklist (skipping ones already fixed or non-actionable), and dispatch ONE agent per pending issue (sequentially) to fix → verify on the adb device → local [classic] #N commit. Use when the user wants to work through / triage / "разобрать" / clear the open issues, or run an unattended issue-clearing sweep.
---

# Forkgram Classic — sweep the open issues (one agent per issue)

This is the **orchestrator** over `/classic-issue` (singular): it turns the open GitHub issues into a
worklist and dispatches a focused agent for each. You (the main loop) keep the list, sequence the agents,
and summarize — each agent does the real fix → build → device-verify → commit for its one issue.

**Run the agents SEQUENTIALLY, never in parallel.** There is one working tree, one connected phone, and a
10–15 min gradle build per issue — parallel agents would clobber each other's edits, race commits on the
same branch, fight over the single device, and thrash the build. Spawn one agent, wait for it to finish
(its commit landed), then spawn the next. (Read-only *investigation* could be parallelized, but the
build/verify/commit cannot — keep it simple and serial.)

## 1. Preconditions
```bash
cd /home/h/src/forkgram-classic
git rev-parse --abbrev-ref HEAD                                  # classic
adb devices | awk 'NR>1 && $2=="device"{print; n++} END{exit !n}' # a phone must be connected/authorized
git status --porcelain --untracked-files=no | grep -vE 'TMessagesProj/jni/libvpx'   # ideally empty
```
- Not on `classic`, no device, or a dirty tree mid-edit → stop and tell the user.

## 2. Build the worklist
```bash
gh issue list -R forkgram/forkgram-classic --state open --json number,title \
  --jq '.[] | "\(.number)\t\(.title)"'
```
Classify each open issue:
- **DONE** — a `[classic] #<N>:` commit already exists (`git log --oneline | grep -E "#<N>:"`). The fix
  shipped; the GitHub issue just wasn't closed. Skip it (optionally tell the user it can be closed —
  closing is outward-facing, only do it if they ask: `gh issue close <N> -R forkgram/forkgram-classic`).
- **NON-ACTIONABLE** — feedback/thanks/questions (e.g. "Thanks?"), not a UI-regression report. Skip, note it.
- **PENDING** — an actionable "screen X still has the new design / is broken" report with no commit yet.

Present the worklist as a table (`# | title | DONE/SKIP/PENDING`). If the user asked for an unattended
sweep, proceed straight to the pending ones; otherwise let them prune the list first.

## 3. Dispatch one agent per PENDING issue, in order
For each pending issue `#N` (lowest number first), fetch its body once and spawn a general-purpose agent;
**wait for it to return before starting the next.** Suggested prompt:

> Resolve Forkgram Classic GitHub issue **#N**: "<title>".
> Issue body / screenshot notes: <paste `gh issue view N` output>.
> Work on branch `classic` in `/home/h/src/forkgram-classic`. **Read `.claude/skills/classic-issue/SKILL.md`
> and follow it exactly** — understand the issue, implement the surgical classic revert (diff against tag
> `11.9.5.0`; do NOT use a worktree), build `assembleAfatDebug -PF_DROID=1` (watch the javac UP-TO-DATE
> trap), install + screenshot-verify on the connected adb device, and only then make the local
> `[classic] #N:` commit. Do NOT push. If you cannot verify the fix on the device, do NOT commit — report
> what's blocking instead. Return: the commit hash (or "no commit"), a one-line result, and any follow-up.

- One agent at a time. Record each outcome: **committed `<hash>`** / **failed (reason)** / **skipped**.
- If an agent reports it couldn't verify or the build broke, leave it uncommitted, note it, and continue
  with the next issue (don't let one bad issue block the sweep). Re-check the tree is clean between agents
  (`git status`) — if an agent left a half-edit, `git stash`/`git checkout --` it before the next, and flag it.
- The device can wedge between long runs (see memory `reference_adb_connect.md`) — if `adb devices` loses
  the phone, recover it (PCI rescan / route) or pause and tell the user, rather than committing unverified.

## 4. Report (do not push, do not fold)
Summarize the sweep as a table: issue → outcome → commit. Then point at the next steps:
- The new commits are temporary `[classic] #N:` commits — run **`/classic-fold`** to fold them into the
  four foundational commits, then **`/classic-release`** to ship. (This skill never folds, pushes, or releases.)
- List any PENDING issues that failed/were skipped and why, plus any DONE issues that can be closed on GitHub.
