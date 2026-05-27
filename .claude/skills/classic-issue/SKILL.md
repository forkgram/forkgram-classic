---
name: classic-issue
description: Resolve a Forkgram Classic GitHub issue end-to-end — understand it, implement the classic-restoration fix, build the F-Droid debug APK, install + verify on the USB-connected device via adb, and make a local [classic] #N commit. Use when the user asks to fix / work on / "сделай" / "поработай над" an issue (by number, or "the next open one") of forkgram-classic.
---

# Forkgram Classic — resolve one issue (fix → verify on device → commit)

Forkgram Classic is a Telegram fork whose job is to **undo the modern (12.x "iOS redesign") UI
and restore the classic pre‑12.0 look**, shipped only via F‑Droid as `org.forkgram.classic`.
Issues live on GitHub at `forkgram/forkgram-classic` and are referenced as `#N` in commits.
This skill does **one** issue per run (fix → device‑verify → local commit). Loop it for several.

Work on branch `classic`, **directly in the main tree** (do not use `isolation: worktree` — a worktree
gets created from the merge-base, not `classic` HEAD, and comes out modern-contaminated). Keep changes
**surgical**: a full-file swap from the classic reference drifts against the modern 12.7 core.

## 0. Preconditions
```bash
cd /home/h/src/forkgram-classic
git rev-parse --abbrev-ref HEAD          # should be: classic
DEV=$(adb devices | awk 'NR>1 && $2=="device"{print $1; exit}'); echo "device: $DEV"   # the connected phone (RMX3581)
```
- If not on `classic`, stop and tell the user. If no `device`, ask them to connect/authorize the phone
  (USB or `adb connect`). See project memory `reference_adb_connect.md` for the T2-MacBook reconnect quirks.

## 1. Pick & understand the issue
```bash
gh issue list -R forkgram/forkgram-classic --state open
gh issue view <N> -R forkgram/forkgram-classic            # the report (often a screenshot + a one-line "still has the new design")
```
- The arg may be an issue number/URL, or "the next open one" → take the lowest open number not already done
  (cross-check `git log --oneline | grep "#<N>"`).
- **Reference = source of truth.** Most issues are "screen X still uses the redesign; restore classic":
  - Classic look: tag `11.9.5.0` in the modern repo — `git -C /home/h/src/TelegramAndroid show 11.9.5.0:<path>`.
    (`11.9.5.0` is the last 11.x = genuine classic; the overhaul landed in 12.0.1.)
  - Modern/redesign + full feature set: `/home/h/src/TelegramAndroid` branch `dev` (the port source).
  - Local context: `classic/PHASE2_BACKLOG.md`, `classic/MODERN_UI_CUTS.md`, `classic/PLAN.md`, and project
    memory. **GOTCHA:** the fork docs are sometimes wrong (e.g. they claimed 12.1.1 ProfileActivity was
    "classic" — it is actually the redesign). Trust the 11.9.5.0 diff over the docs.

## 2. Implement the fix
- Locate the redesigned widget/screen; diff the relevant methods against `11.9.5.0` (and 11.7 where the
  classic body differs), revert the geometry/layout/colors surgically. Watch for these recurring hazards
  (all in project memory): animated-bg must route through `dispatchDraw`/`clipRectF` (never `setBackgroundColor`
  on an `AnimatedLinearLayout`); 12.4.x top panels assume a `topPanelLayout` absent in 12.1.1; restoration
  sometimes *deleted* required base code (diff full method bodies, scan removed `-` lines); the classic
  emoji panel is incompatible with the modern input-island.
- Match surrounding code style; tag new/changed lines with a `// [classic] #<N>:` comment.

## 3. Build the installable APK
```bash
JAVA_HOME=/usr/lib/jvm/java-21 ANDROID_HOME=/opt/android-sdk \
  ./gradlew :TMessagesProj_App:assembleAfatDebug -PF_DROID=1 --console=plain 2>&1 | tail -25
```
- **TRAP (project memory `reference_gradle_incremental_skip.md`):** gradle sometimes prints
  `compileAfatDebugJavaWithJavac UP-TO-DATE` after a real `.java` edit → a stale APK ("the fix did nothing").
  `touch` does not help. After every build, confirm the line says `compileAfatDebugJavaWithJavac` (ran),
  not `UP-TO-DATE`. If skipped: `./gradlew --stop` + `rm -rf TMessagesProj_App/build/intermediates/javac/afatDebug`
  and rebuild. APK: `TMessagesProj_App/build/outputs/apk/afat/debug/TMessagesProj_App-afat-debug.apk`.

## 4. Install, launch, verify on the device
```bash
adb -s "$DEV" install -r TMessagesProj_App/build/outputs/apk/afat/debug/TMessagesProj_App-afat-debug.apk
adb -s "$DEV" shell am force-stop org.forkgram.classic        # force-stop so the NEW code runs (install -r alone keeps the old process)
adb -s "$DEV" shell monkey -p org.forkgram.classic -c android.intent.category.LAUNCHER 1
# wait for the window, then navigate to the affected screen and screencap:
adb -s "$DEV" exec-out screencap -p > /tmp/after.png
```
- Read the screenshot. **Compare before/after at IDENTICAL crop coords** (a zoomed crop via
  `ffmpeg -i x.png -vf crop=W:H:X:Y,scale=... out.png` makes small color/position regressions obvious).
- Reliable own-profile nav on this RMX3581: drawer (`input tap 40 70`) → My Profile (`input tap 200 423`).
  **Dialog-row and chat-header taps are flaky here** (often reopen Saved Messages / miss) — retry, or use a
  recorded `screenrecord` + `ffmpeg` frame extract for animations/transients. Foreground `sleep` is blocked;
  put waits inside `run_in_background` commands or `until <cond>; do sleep …; done` loops.
- For non-trivial visual/geometry work, spawn a critic agent (general-purpose) to diff the change against
  `11.9.5.0` and check for NPE/regressions before committing.

## 5. Commit locally (do NOT push)
Only after the device shows the classic look with no regression:
```bash
git add <changed files>            # stage only what you changed; never the libvpx submodule or .claude/
git commit -m "[classic] #<N>: <imperative one-line summary>." -m "<body: what redesign was removed and how the classic look is restored; reference 11.9.5.0>" -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
gh issue comment <N> -R forkgram/forkgram-classic --body "..."   # OPTIONAL and only if the user asks — outward-facing
```
- Push/classic-release is **not** part of this skill (outward + irreversible). The commit is a temporary
  `[classic] #N:` feature commit; before a release it must be folded into the four foundational `[classic]`
  commits — that's the **`/classic-fold`** skill, and then **`/classic-release`**. Mention this in your wrap-up.
- Update project memory if you learned something non-obvious (a new hazard, a corrected fork-doc claim).
