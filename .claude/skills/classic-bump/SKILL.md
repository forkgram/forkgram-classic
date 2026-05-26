---
name: classic-bump
description: Port Forkgram Classic onto a NEW upstream Telegram minor version (e.g. 12.7 → 12.8) and the modern Forkgram release built on top of it — the rare "день X" task. Rebase the classic 5-commit stack onto the new modern base, re-pin the design, revert the new redesign (e.g. the chat-header "islands"), re-shim the 12.1.1 UI against the new core until it builds, then device-verify. Use when the user says a new upstream/forkgram release is out and classic must follow ("вышел новый релиз… сделать classic", "обнови classic до 12.X", "день X").
---

# Forkgram Classic — bump to a new upstream minor version

Forkgram Classic preserves the **pre-redesign (`9cbf03332` = 12.1.1) UI** on top of an ever-newer
upstream Telegram core, shipped via F-Droid as `org.forkgram.classic`. Most weeks the work is one issue
(`/classic-issue`). **This skill is the rare, heavy job**: upstream shipped a new *minor* version with a
fresh redesign, the user already cut the modern Forkgram release on it, and classic must cross onto that
new base while undoing the new redesign. (The 12.7→12.8 run undid the chat **title-bar "islands"** —
the solid back+avatar+name bar split into floating capsule chips.)

**The mental model that makes this tractable:** the redesign is almost never a new file to delete — it is
in-place edits to files Classic *already pins to 12.1.1* (`ChatActivity`, `ActionBar`, …). Restoring
those pins **auto-reverts** the redesign. The new redesign was 12.8 "glass mode" (`ActionBar.setupGlass()`
+ three rounded capsule drawables in `dispatchDraw`); pinning `ActionBar`+`ChatActivity` removed all of
it. The only residue is **compile breaks** where *non-pinned* 12.x files call the new methods on the
restored classes — fixed with the established **no-op shim** pattern. So the bulk of the work is not
"finding the redesign" — it is **re-shimming the 12.1.1 UI against the drifted new core** (an incremental
repeat of the original Phase-1 383→0 compile climb).

Work on branch `classic`, **directly in the main tree** (never `isolation: worktree` — a worktree forks
from the merge-base and comes out modern-contaminated). Long builds: run from the **main loop** (durable
background) — a build started by a dispatched Agent is SIGKILLed when the agent's turn ends.

---

## 0. Preconditions
```bash
cd /home/h/src/forkgram-classic
git rev-parse --abbrev-ref HEAD                                   # should be: classic
git status --porcelain --untracked-files=no | grep -vE 'jni/libvpx|jni/tde2e_source'   # should be EMPTY
DEV=$(adb -s 2921295510CA0CVA get-state >/dev/null 2>&1 && echo 2921295510CA0CVA || adb devices | awk 'NR>1&&$2=="device"{print $1;exit}')
adb -s "$DEV" shell getprop ro.product.model                     # want RMX3581 (realme C30, armeabi-v7a, 32-bit)
```
Repos: classic = `/home/h/src/forkgram-classic` (origin `forkgram/forkgram-classic`); modern reference =
`/home/h/src/TelegramAndroid` (origin `forkgram/TelegramAndroid`, `upstream` = DrKLO). They are **separate
repos** — the new base must be *fetched* from the modern repo into the classic repo (§2).

## 1. Locate the THREE inputs (verify before touching anything)
| Role | How to find it |
|---|---|
| **New modern base** (build classic on THIS) | In `/home/h/src/TelegramAndroid`: the newest release tag, `git tag --sort=-creatordate \| head`. It is a `X.Y.Z.0` tag pointing at a `[Release]` commit that **diverges from `upstream/master`** (`git merge-base --is-ancestor <tag> upstream/master` → NO) and carries the fork commits on the new upstream. Confirm: `git log <tag> --oneline \| grep -i "update to"` shows the new upstream snapshot, and the fork features are present. |
| **New upstream snapshot** | the `update to X.Y.Z (build)` commit inside the modern base's history (DrKLO). Reference only. |
| **Design baseline** (unchanged) | `9cbf03332` (12.1.1) — `classic/BASELINE.txt`. The source of every `DESIGN_FROZEN` pin. (For *issue-level* fidelity the truer classic ref is tag `11.9.5.0`; for the bump, restore-frozen uses 9cbf03332.) |

> WATCH: the modern repo's `dev` branch may still sit on the OLD upstream — the new release can live **only
> as the tag** (dev not fast-forwarded). Trust the `[Release]` tag, not `dev`. Stash the new modern base SHA
> as `$MODERN` and the old classic base (current `classic` minus the 5 commits) as `$OLDBASE`:
```bash
MODERN_TAG=12.8.2.0   # ← the new release tag, set per run
OLDBASE=$(git -C /home/h/src/forkgram-classic merge-base classic origin/dev)   # the modern commit classic currently sits on
```

## 2. Prep (all reversible)
```bash
git fetch /home/h/src/TelegramAndroid "refs/tags/$MODERN_TAG:refs/heads/modern-$MODERN_TAG"   # bring the new base + history in
git branch backup-classic-pre-bump classic                                                    # safety
tools/classic/categorize-diff.sh "$OLDBASE" "modern-$MODERN_TAG"                               # frozen / merge / unknown worklist
```
Sanity-check the **size of the upstream delta** so you know what you're walking into — and whether the
native cache stays warm (no ffmpeg/voip/rlottie/tde2e churn ⇒ ~5-min rebuilds, no multi-hour cold native build):
```bash
T_OLD=<old upstream snapshot>; T_NEW=<new upstream snapshot>   # the two "update to …" commits
git -C /home/h/src/TelegramAndroid diff --stat $T_OLD..$T_NEW -- 'TMessagesProj/jni/**' | tail -1   # want tiny
git -C /home/h/src/TelegramAndroid diff --diff-filter=A --name-only $T_OLD..$T_NEW -- 'TMessagesProj/src/main/java/org/telegram/ui/**'  # NEW files: feature vs redesign?
```
The real **rebase conflict set** is small — only files the `[classic]` stack touches AND the new base changed:
`comm -12 <(git diff --name-only $OLDBASE classic -- | sort) <(git diff --name-only $OLDBASE modern-$MODERN_TAG | sort)`.

## 3. Move the base — rebase the 5-commit stack
```bash
git rebase --onto "modern-$MODERN_TAG" "$OLDBASE" classic
```
Resolve each conflict **by file class** (this preserves the folded issue-fixes AND the 5-commit shape):

| Class | Files | Resolution |
|---|---|---|
| **DESIGN_FROZEN (pinned)** | `ChatActivity`, `ChatActivityEnterView`, `EmojiView`, `ChatAttachAlert`, `ChatAttachAlertPollLayout`, `ShareAlert`, `ProfileActivity`, `ActionBar`, `ActionBarLayout`, `BaseFragment`, `DrawerLayoutContainer` | **take Classic side** → `git checkout --theirs -- <file>` (theirs = the commit being replayed = classic). This reconstructs the exact content later commits were authored on, so they apply cleanly. **Verify orientation**: the resolved file must have **0** redesign markers (e.g. `grep -c 'setupGlass\|glassDrawable' ChatActivity.java` == 0). |
| **ALWAYS_MERGE + new features** | `tgnet/**`, `messenger/**`, `jni/**`, build, assets, `ui/iv/**` (new IV2.0), WearAuth/Chatbot | flow from the new base — the `[classic]` commits don't touch them; usually no conflict. |
| **Semi-pin / classic-modified** | `DialogCell`, the few settings/feature screens carrying folded `// [classic] #N` fixes (e.g. `ChannelAdminLogActivity`, `ChatAttachAlertPhotoLayout`, `SharedMediaLayout`, `TopicsTabsView`, `FragmentSearchField`, `AudioPlayerAlert`) | **hand-merge per hunk**: keep the new-base code, re-apply the `// [classic] #N` hunk. Most auto-merge; only same-line overlaps conflict. For pure take-classic hunks: `perl -0777 -pi -e 's/^<<<<<<< HEAD\n(.*?)^=======\n(.*?)^>>>>>>> [^\n]*\n/$2/smg' <file>`. |
| **build.gradle (signingConfigs)** | — | take the **classic** form: `if (!fdroid) { debug/release storeFile file("../TMessagesProj/config/release.keystore") … RELEASE_STORE_PASSWORD/… }`, no fdroid block (AGP auto-signs debug). The modern repo's `file("")` fdroid block breaks the F-Droid build. |
| **gradle.properties (version)** | — | set `APP_VERSION_NAME` to the new minor (e.g. `12.8.2`) and `APP_VERSION_CODE` to **base code + 1** (monotonic over the last classic release; keeps the "Bumped version." commit non-empty). Exact release number is `/classic-release`'s call. |

After the rebase completes: `git log --oneline -6` must show the 4 `[classic]` commits + `Bumped version.`
on top of the new base, and `git grep -l '^<<<<<<< HEAD' -- '*.java'` must be empty.

## 4. Re-shim loop — build the library until 0 errors
Iterate fast on the **library** module (where `ui/**` lives; the redesign/shim surface):
```bash
JAVA_HOME=/usr/lib/jvm/java-21 ANDROID_HOME=/opt/android-sdk \
  ./gradlew :TMessagesProj:compileDebugJavaWithJavac -PF_DROID=1 -Parmeabi-v7a 2>&1 | tail -5
grep -E "\.java:[0-9]+: error:" <log> | sed -E 's#.*/(org/telegram/[^:]+):.*#\1#' | sort | uniq -c | sort -rn
```
**The reference for every "what's the new API?" question is the MODERN file** — the modern `ChatActivity`
has the same feature using the *correct* new-version call. Dump it once: `git show modern-$MODERN_TAG:…/ChatActivity.java > /tmp/modern_ca.java` and grep it.

### Recurring shim/fix catalog (12.7→12.8 made all of these; expect the same shapes)
1. **Redesign "glass/islands" API → no-op shims on the restored pinned class.** Only methods with *live
   callers* need a shim — `grep -rn 'actionBar\.<m>(' ui/` first. 12.8 needed exactly one: `ActionBar.checkAvatarContainerWidth(boolean) {}` (called by the un-pinned `ChatAvatarContainer`). `setupGlass`/`setSearchFactor`/`getActionModeFactor`/`setForcedMenuWidth`/… had **0** callers once the pinned chat files reverted, so no shims. Put them next to the existing `setGlassDrawable`/`setAdaptiveBackground` no-op block in `ActionBar.java`.
2. **New abstract interface methods → implement on the pinned impl.** 12.8 added `isLayersLayout()`/`isRightLayout()` to `INavigationLayout` and `setIsRightLayout()` (no-arg) called by `LaunchActivity` → add to `ActionBarLayout` (`return false` / no-op; the iPad "layers" split-pane stays dormant).
3. **Un-pinned-wholesale files → swap to the new upstream version.** `ChatMessageCell` is un-pinned (kept upstream for poll v2). The rebase leaves it at the OLD upstream (took --theirs) → it imports classes the new core deleted (`FrameTickScheduler`). Fix: `git checkout modern-$MODERN_TAG -- …/ChatMessageCell.java`. (Any file marked "un-pinned/wholesale upstream" in `DESIGN_FROZEN.txt` gets this.)
4. **TL classes relocate/rename across versions.** 12.8's IV2.0 moved page blocks out of `TLRPC`: `TLRPC.PageBlock`→`TL_iv.PageBlock`, and **renamed** the subtypes `TLRPC.TL_pageBlockList`→`TL_iv.pageBlockList` (dropped `TL_`, lowercased). Find the new name via the modern file (`git show modern:…/ProfileActivity.java | grep pageBlock`), then `perl -pi -e 's/\bTLRPC\.(…)\b/TL_iv.$1/g'` + add the import.
5. **Drifted constructors/methods → fix the call site (classic-owned files ride the rebase; don't patch upstream).** 12.8: `new SearchTagsList(…7-arg…)`→5-arg; `new ChatSearchTabs(ctx, contentView)`→`(ctx)`; `MotionBackgroundDrawable.updateAnimation(bool)`→`()`; `SharedConfig.inappBrowser`→`getMessagesController().isWebBrowserOpenInApp(str)`; a view that moved from overridable `onClick/onScrolled` to setter-callbacks (`setOnHashtagClickListener`/`setOnScrollListener`) → rewrite the construction to setters; a method whose super was removed (`ChatAttachAlert.setAllowDrawContent`) → drop `@Override`+`super`.
6. **Removed lifecycle methods with several call sites → either delete the calls (12.x self-manages via `onAttachedToWindow`) or add a no-op shim.** 12.8 removed `SearchTagsList.attach()/detach()` → deleted the 3 calls; changed `HashtagHistoryView.show()/isShowing()` → added a 2-line bridge shim on it.
7. **New-feature hooks into a pinned class → add the constant/method the feature expects.** New IV2.0 files reference `ChatActivity.PROGRESS_FULL_ARTICLE` (add the `= 7` constant) and `ChatAttachAlert.showSendButtonOnly(boolean,boolean)`/`setLocationPicker()` (add no-op shims — the classic sheet never hosts the rich layout). These only need to **compile** (the feature path is unreachable in classic).

Comment every shim `// forkgram-classic: …` naming the new call site it serves. When the library hits
**BUILD SUCCESSFUL / 0 errors**, do the full APK build.

## 5. Build the APK + device-verify
```bash
JAVA_HOME=/usr/lib/jvm/java-21 ANDROID_HOME=/opt/android-sdk \
  ./gradlew :TMessagesProj_App:assembleAfatDebug -PF_DROID=1 -Parmeabi-v7a 2>&1 | tail -25
```
- **UP-TO-DATE trap** (`reference_gradle_incremental_skip.md`): confirm `compileDebugJavaWithJavac` actually
  *ran*. APK: `TMessagesProj_App/build/outputs/apk/afat/debug/TMessagesProj_App-afat-debug.apk`.
```bash
adb -s "$DEV" install -r <apk>
adb -s "$DEV" shell am force-stop org.forkgram.classic
adb -s "$DEV" shell monkey -p org.forkgram.classic -c android.intent.category.LAUNCHER 1
adb -s "$DEV" exec-out screencap -p > /tmp/dialogs.png        # then tap into a chat, screencap the header
ffmpeg -y -i /tmp/chat.png -vf "crop=720:150:0:55" /tmp/header.png   # crop the header band for a clean look
```
**Headline acceptance:** open a chat → the top bar is **one solid continuous action bar** (back · avatar ·
name/subtitle · call · menu), NOT split into floating capsules. Then smoke: classic dialogs (hamburger
drawer + flat folder tabs, no chips), legacy profile, emoji panel, send. HAZARDS (project memory):
dialog-row/chat-header/tiny-button taps are **flaky** on the RMX3581 (retry; the chat opens on a later
try); a modern test app may co-reside — confirm `org.forkgram.classic` is foreground; screenshots are
720×1600 full-res (coords map 1:1, but eyeballed Y still drifts — prefer `ffmpeg` band-crops).

## 6. Finalize + hand off
- Update `classic/MODERN_UI_CUTS.md` (a section on the new redesign = how it was reverted) and the
  "current upstream snapshot" note; `BASELINE.txt` stays `9cbf03332`.
- The reshim is a temporary **`[classic] … (to fold)`** commit. It belongs in **foundational commit 1**
  ("Set up the Classic design-preservation pipeline" — its job is literally "drive the tree to a clean
  build against the new core"). Run **`/classic-fold`** to fold it (and any `[classic] #N` issue commits)
  back into the four foundational commits, returning the canonical 5-commit shape.
- Then **`/classic-release`** sets the final version + pushes the tag (F-Droid auto-builds). Both are
  user-triggered, outward-facing — **not** part of this skill.
- Save what was non-obvious to project memory (a new redesign's mechanism, a new TL relocation, a new
  shim category).
