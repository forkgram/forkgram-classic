# Forkgram Classic — Phase 2 backlog: un-ported Forkgram UI features

> Generated 2026-05-31. Living document — update the status column as features land.

## Why this exists

Phase 1 of the design-preservation pipeline (`classic/PLAN.md` §3) reset every
`DESIGN_FROZEN` file to the **12.1.1 baseline** (`9cbf03332`) via
`tools/classic/restore-frozen.sh` (`git checkout <baseline> -- <path>`). The
baseline predates the entire Forkgram patch set, so any Forkgram modification
that lived *inside* a frozen UI file was discarded. PLAN §3 Phase 1 says this
explicitly: *"No Forkgram features at this stage — design only."* Phase 2
("Reinstating Forkgram patches") is the work of porting them back by hand on top
of the classic base — and it is barely started.

**Important nuance:** the ~95% design-neutral Forkgram patches that live in
non-frozen files (`messenger/**`, controllers, DB, push, locale, settings
backend) *survived* — they were never overwritten. What this backlog tracks is
only the Forkgram code that lived in the ~15 frozen UI files and was wiped.

## Method

- `master` = upstream snapshot (12.7.3). `0d4dabfda` = full Forkgram head, sits
  directly on top of `master`. So `master..0d4dabfda` = the 250 pure Forkgram
  commits with zero upstream-redesign noise.
- For each frozen file: `git log --oneline master..0d4dabfda -- <file>` lists the
  Forkgram commits that touched it = features lost on restore.
- Verified absent in current `classic` HEAD by signature grep (most return 0).

**Reference for porting:** modern Forkgram source is checked out at
`/home/h/src/TelegramAndroid` (branch `dev`) — every feature below is live there.

## Status legend

- ✅ ported (present in current `classic`)
- 🔶 partial — backend/non-frozen part survived, frozen-file UI wiring missing
- ❌ missing
- ⚙️ perf/optimization (not user-visible)
- 🧹 de-Google removal — relevant to the F-Droid build

---

## Status as of 2026-05-31 (re-audited — supersedes stale ❌ in tables below)

A presence audit (grep distinctive tokens in current `classic`) found the
original backlog **over-counted** misses: several features were already carried
into `classic` from `classic-wip` via the squash.

**Ported this session (committed):** bare-icon dialog badges + poll-votes mention
· Fork Settings entry · Go to Beginning · Copy without protocol · Copy URL from
inline · Copy private link · hide-title · delete-all (your msgs / unpinned / all
topics) · disable quick-reaction · calendar from floating date · remove input
gift button · disable-video-on-volume guard.

**Already present in `classic` (audit — no work needed):** copy dialog ID
(`idRow`) · edit phone/username/bio long-press menu · debug button removed ·
proxy "…" text · shift-enter no-send · mention-by-name · ShareAlert Fast Forward.

**Skipped (not clean/general features):** anonymous forward (`4419eaec3`, dead
flag) · auto-dot (`c6691d5cf`, owner-only) · disable-online (`40578661b`, debug
hack) · slide-to-next-channel disable (`51d415182`, the feature doesn't exist in
the 12.1.1 base) · de-Google `[TF]` kills (handled by the F-Droid flavor).

**Also confirmed already present (precise-token re-audit 2026-05-31):** hide-bottom-button
(now ported), GIF auto-caption (`99f87bdda`), disable-bot-link params (`8741e12a3`),
drop-screenshot-caption (`417e30070`), online/last-seen dots (`38ecf2afa`, `colorOnline`).
The original audit had several **false negatives** (token mismatch) — always read the
code before porting.

**Also ported since (committed):** hide Send As (`953d3a329`), disable dialog
thumbs (`8983aed7b`, minimal early-return).

**BACKLOG CLOSE-OUT — 2026-05-31 (session 2). All remaining UI features ported or resolved; verified by `:TMessagesProj:compileDebugJavaWithJavac -PF_DROID=1` (JDK 21).**
- ✅ **Fast Forward** (`80b66bc63`+`9497c3e88`) — **ported**. Everything except ChatActivity
  SURVIVED (ShareAlert FF via `UndoInfo`, UndoView `ACTION_FWD_MESSAGES`, `AsCopy`/`ForkApi`
  helpers, `replaceForward` toggle, `anon_forward`/`ic_ab_forward_anonym` drawables, all 4
  strings, `ChatActivityActionsButtonsLayout` group-media methods). Ported the missing
  ChatActivity wiring: `forward_anonym` menu-id (= 80), `createShareAlert()` +
  `showAnonymShareAlert()`, action-mode `replaceForward` toggle (kept the classic
  `!isSavedMessages && != VERIFY` guard that the upstream commit *dropped* — strictly safer),
  the `forward_anonym` click handler, the `case 202` context-menu **Fast Forward** entry (in
  `processSelectedOption`), and the openForward 76→7 inline-`ShareAlert` collapse. **Group-media
  floating button (hunks 5–8) deliberately skipped**: classic's 12.1.1 ChatActivity never wired
  the `actionsButtonsLayout` reply/forward/group subsystem (0 refs) — no host exists; FF core
  doesn't depend on it. Compile-clean; on-device verification of Fast Forward AND normal forward
  still recommended (high blast-radius core flow).
- ✅ **extract media from webpage** (`0d4dabfda`) — ported 2026-05-31 (unchanged).
- ✅ **attach text to voice** (`72fecd146`) — **ported**: backend `ForkDialogs.CreateVoiceCaptionAlert`
  survived; added the EnterView integration (`voiceCaption` field, `slideText.bringToFront()`,
  `TimerView.timestamps` + timestamp-to-clipboard onClick, `timestamps` init in `start()`).
- ✅ **reply-quote on voice** (`99e0f811e`) — **ported**: EnterView now threads `replyingQuote`
  (was `null`) into MediaController.startRecording / setReplyingMessage / prepareResumedRecording
  (MediaController side already survived).
- ✅ **auto-send text before sticker** (`a99f37155`) — **ported** in EnterView (sends pending
  typed text before `sendSticker`, clearing it if it's a bare valid emoji).
- ✅ **last-seen-dots toggle** (`8e87e5b71`) — **ported**: DialogCell `colorOnline` now honours
  the `enableLastSeenDots` ForkSettings toggle (toggle row already existed).
- ✅ **proxy hide-sensitive text** (`ef3163007` gap) — **ported**: ProfileActivity
  `ConnectingToProxy` text wrapped in the `SharedConfig.hideSensitiveData() ? "..." : …` ternary
  (matches ChatAvatarContainer).
- ✅ **Move 'Call' to bottom of chat menu** (`13adcc961`) — **ported** (relocated the
  call/video_call block to after the fee block).
- ✅ **video do-not-compress** (`f9ebfc3a5`) — **already present**: `doNotCompress` is wired via
  `ChatAttachAlertDocumentLayout:912` → SendMessagesHelper (10421/10510). The 2023 commit's
  didSelectPhotos positional flip maps to today's `groupMedia` arg (NOT compression) — correctly
  NOT applied (would be a grouping regression).
- ✅ **Monet themes** (`119a1eb8d`) — **already present** (backlog false-negative): the commit is an
  ancestor of HEAD; Theme.java hunks, LaunchActivity register/unregister, `MonetHelper.java`
  (OKLCH dep satisfied) and both `.attheme` assets are all in the tree.
- ✅ **select-all-between** (`e9cb0a5ff`) — ported + device-verified 2026-05-31 (unchanged).
- ✅ **copy username on tap** (`06b319236`) — already present (unchanged).
- ✅ **Drawer hide-sidebar** (`e82b77879`) — **ported (reconstructed)**: commit is empty here AND
  in the reference, so reconstructed minimally — `DrawerProfileCell` blanks the phone number when
  `SharedConfig.hideSensitiveData()` (consistent with ProfileActivity; name left intact).
- ⛔ **Drawer secret-chat/channel menu items** (`3534d6ccb`) — **SKIPPED**: empty commit; classic
  intentionally comments these out with icon ids = 0 (upstream removed them) — no drawables or
  click handlers exist to restore. Not a clean port.
- ⏸ **multi-select unread count** (`989c2fb1d`) — DEFERRED (low value, unchanged).
- ⚙️ **DialogCell perf opts** (`a5b03b85f`/`5e0a3a070`/`81f2aa60a`) — `81f2aa60a` already present;
  the other two **SKIPPED**: non-user-visible perf tweaks in the hot draw/animation path, no clean
  port, regression risk without device profiling. Not user-facing features.

### Deeper re-audit (session 2): completeness proof + 4 dead toggles fixed

Because `0d4dabfda` (full Forkgram head) is a direct **ancestor** of `classic` (their merge-base
IS `0d4dabfda`), a Forkgram feature can only be lost where a frozen-file reset removed it after that
point. Verified two complementary ways:

1. **Full frozen cross-check.** All **58** Forkgram commits (`master..0d4dabfda`) that touch any of
   the 23 frozen files map to ported / already-present / skipped / deferred — none unaccounted.
2. **Dead-toggle sweep.** Every ForkSettings setting key was checked for a consumer *outside*
   ForkSettingsActivity. Found **4 toggles whose backend was lost in a frozen reset** (the UI row
   existed but did nothing) — now fixed (compile-verified):
   - ✅ `mentionByName` (`ff64b4813`) — re-added the ChatActivity guard: mentioning inserts a
     name-link instead of `@username` when the toggle is on.
   - ✅ `disableSlideToNextChannel` (`51d415182`) — re-added the ChatActivity pull-to-next-channel
     guard (the feature exists in the 12.1.1 base, contrary to the earlier "doesn't exist" note).
   - ✅ `disableRecentFilesAttachment` (`a676e4e9d`) — the surviving guard in
     `ChatAttachAlertDocumentLayout.loadRecentFiles()` read the WRONG key `"disableSlideToNextChannel"`
     — a copy-paste bug **present in upstream Forkgram itself**. Corrected to the right key, which
     fixes BOTH toggles (the slide toggle no longer wrongly wipes recent files).
   - ✅ `stickerSize` (`a9c4aaa41`) — the ChatMessageCell consumer was lost in the wholesale
     ChatMessageCell restore (`fc5e14ba6`) and the slider constant had drifted to a hardcoded 20.
     Re-added `ChatMessageCell.MAX_STICKER_SIZE = 14.0f` + the sticker-size formula and pointed
     `endStickerSize` back at the constant (matches the modern reference; default = normal size).
   - ⛔ `hideAiEditor` (`f4df6fb43`) — left as a no-op toggle: the 12.1.1 baseline EnterView has no
     AI editor to gate (MOOT).

**Conclusion: no remaining Forkgram UI feature is unaccounted-for** — every one is ported, already
present, deliberately skipped (with rationale), or a deferred niche item.

**Newly skipped (verified moot / out-of-scope):**
- disable AI editor (`f4df6fb43`) — **moot**: 12.1.1 baseline EnterView has no
  `showAiButton`/AI editor at all, so there's nothing to disable (already pro-classic).
- Saved Messages on search long-press (`772d0c6b4`) — tangled with the
  `mainTabsHiddenFork` bottom-tabs subsystem (UserConfig + DialogsActivity +
  MainTabsActivity); not a standalone port.

**Also skipped (F-Droid policy — no in-app updaters):** GitHub auto-updater
(`67113d7a2`), check-updates row (`962c912c7`), channel fork-update popup (`2996e3195`).

---

## Dialog list — `DialogCell.java` (8 commits)

| Status | Commit | Feature |
|---|---|---|
| ✅ | `171889dc0` | Poll-votes mention badge (bare-icon, no circle) — **ported this session** |
| ❌ | `8e87e5b71` | Option for last-seen dots |
| ❌ | `38ecf2afa` | Experimental additional online statuses |
| ❌ | `8983aed7b` | Option to disable thumbs in dialog list |
| ❌ | `989c2fb1d` | Don't hide unread count when multi-selecting dialogs |
| ⚙️❌ | `a5b03b85f` | Cache online status & color outside draw |
| ⚙️❌ | `5e0a3a070` | Batch typing-status invalidation / fewer waiting redraws |
| ⚙️❌ | `81f2aa60a` | `needInvalidate` instead of direct invalidate in spoiler draw |

> Note: the bare-icon badge restyle (mention/reaction without the count pill) was
> also part of the original DialogCell look and is restored alongside the badge work.

## Chat screen — `ChatActivity.java` (26 commits)

| Status | Commit | Feature |
|---|---|---|
| ❌ | `0d4dabfda` | "Extract media from webpage preview" toggle |
| ✅ | `1114df66b` | Copy private message link — **ported 2026-05-31** (compile-verified): `canCopyPrivateLink`/`copyPrivateLink` extend the message-menu Copy Link to non-channel chats |
| ❌ | `0cd038288` | Disable "trigger video by volume change" |
| ❌ | `4e470f740` | Delete all of a user's messages across all forum topics |
| ❌ | `51d415182` | Disable slide-to-next-channel |
| ❌ | `f9ebfc3a5` | Do not compress videos |
| ❌ | `3f8f19c81` | Delete all unpinned messages in chat |
| ❌ | `c151edcd8` | Disable quick reaction on double-tap |
| ❌ | `953d3a329` | Hide "Send As" button |
| ❌ | `755698d38` | Delete all your messages in a group |
| ✅ | `d8b138223` | Copy links without protocol — **ported 2026-05-31** (link long-press → ItemOptions) |
| ❌ | `15c510fc4` | Hide bottom button |
| ❌ | `ff64b4813` | Mention users by name |
| ❌ | `e9cb0a5ff` | Select all messages between two selections |
| ❌ | `80b66bc63` + `9497c3e88` | **Fast Forward** (replace original Forward) |
| ❌ | `d05848ccc` | Open calendar from long-click on floating date |
| ✅ | `220422c9a` | Copy URL from inline button — **ported 2026-05-31** (Copy btn on open-link alert) |
| ✅ | `7d94e9751` | "Go to First Message" (⋮ menu → "Go to Beginning") — **ported + device-verified 2026-05-31** |
| ⛔ | `4419eaec3` | Anonymous Forward — **SKIPPED (dead feature)**: `IS_ANONYMOUS_FORWARD` is only ever set, never read (no anonymization logic), and its handler is commented out at the forkgram head (`0d4dabfda`). Porting it would add a button identical to normal Forward. |
| ❌ | `2d03b8f4b` | Hide title & avatar in dialog |
| ❌ | `13adcc961` | Move "Call" item to bottom of dialog menu |
| ❌ | `ec40baf4f` | Remove Dump Canvas debug menu entries |
| 🧹❌ | `3ca5fe151` | `[TF][KILL]` Huawei |
| 🧹❌ | `2baf3aae8` | `[TF][KILL]` Google Translate without MLKit |

## Input panel — `ChatActivityEnterView.java` (10 commits)

| Status | Commit | Feature |
|---|---|---|
| ❌ | `f4df6fb43` | Option to disable AI editor |
| ❌ | `72fecd146` | Attach text to a voice message before sending |
| ❌ | `99e0f811e` | Keep reply quote intact when sending a recorded voice message |
| ❌ | `701d60a8b` | Remove gift button from input field |
| ❌ | `13a198a6a` | Don't send message on Shift+Enter |
| ❌ | `99f87bdda` | Auto-insert caption into cloud GIF when EditField has text |
| ❌ | `a99f37155` | Auto-send pending message before sending a sticker |
| ❌ | `c6691d5cf` | Auto-insert trailing dot at end of message (owner-only) |
| 🧹❌ | `1a46b5c63` | Remove Android Billing (attempt) |

## Profile / Settings — `ProfileActivity.java` (14 commits)

| Status | Commit | Feature |
|---|---|---|
| ✅ | `92f343c47` | New **Fork Settings** section — **ported 2026-05-31**: `forkHeaderRow`/`forkRow`/`forkSectionCell` re-added to the 12.1.1 `ProfileActivity` (drawer → Settings host); opens existing `ForkSettingsActivity`. Verified on device. |
| ❌ | `865bafb80` | Setting to hide/show personal information |
| ❌ | `ef3163007` | `hideSensitiveData` utils + conditional proxy text |
| ❌ | `160d2700f` | Edit phone/username/bio from long-tap dots menu |
| ❌ | `40578661b` | Option to disable constant "online" status updates |
| ❌ | `962c912c7` | "Check updates" button |
| ❌ | `06b319236` | Copy username without long-press |
| ❌ | `83fec50d1` | Copy ID of dialogs |
| ❌ | `bb972752f` | Force profile info into one card (open-animation flicker fix) |
| ❌ | `961911418` | Remove debug button from settings |
| ❌ | `3ef2f5f86` | "Connecting to proxy" → "…" |
| 🧹❌ | `8179473aa` | `[TF]` kill more billing |

## Share sheet — `ShareAlert.java` (1 commit)

| Status | Commit | Feature |
|---|---|---|
| ❌ | `9497c3e88` | Fast Forward implementation. **`DESIGN_FROZEN.txt` claims this was re-applied 2026-05-27, but grep on `classic` HEAD returns 0 — likely only on `classic-wip`. Verify.** |

## Left drawer — `DrawerLayoutAdapter` / `DrawerProfileCell` / `DrawerUserCell` (2 commits)

The drawer *files themselves* were restored from baseline (the drawer exists
again), but the Forkgram patches layered on them are missing:

| Status | Commit | Feature |
|---|---|---|
| ❌ | `e82b77879` | Hide personal information in side bar |
| ❌ | `3534d6ccb` | Return missing menu items for secret chats & channels |

## Action bar — `ActionBar.java` (2 commits)

| Status | Commit | Feature |
|---|---|---|
| ❌ | `772d0c6b4` | Open Saved Messages on long-press of the search top button |
| ❌ | `3ef2f5f86` | "Connecting to proxy" → "…" (shared with Profile) |

## Theme — `Theme.java` (taken from upstream wholesale, 1 commit)

| Status | Commit | Feature |
|---|---|---|
| ❌ | `119a1eb8d` | Monet themes (PR #307/#375) |

## LaunchActivity — `LaunchActivity.java` (10 commits)

| Status | Commit | Feature |
|---|---|---|
| 🔶 | `1e02a460d` | Hidden accounts & stealth mode — backend survived (non-frozen); LaunchActivity wiring present (12 refs) but verify completeness |
| ❌ | `67113d7a2` | Own auto-updater via GitHub Releases |
| ❌ | `2996e3195` | Channel fork-update → `UpdateAppAlertDialog` popup |
| ❌ | `8741e12a3` | Option to disable parameters from bot links |
| ❌ | `417e30070` | Drop auto-generated Screenshot caption from share intent |
| ❌ | `944de10a5` | Fix web-links sharing |
| ❌ | `119a1eb8d` | Monet themes (shared with Theme) |
| 🧹❌ | `042efd62b` | `[TF][MAPS]` support `geo:` intents |
| 🧹❌ | `dcf33b820` | `[TF][KILL]` remove fusedlocationprovider |

---

## Files with **no** Forkgram delta (nothing lost)

`EmojiView.java`, `ChatAttachAlert.java`, `ChatAttachAlertPollLayout.java`,
`RecyclerListView.java`, `ActionBarLayout.java`, `BaseFragment.java`,
`CustomNavigationBar.java`, `DrawerLayoutContainer.java` and the other Drawer
cells — Forkgram never modified these, so restoring them to baseline cost no
features.

## Caveats

- Analysis is against current **`classic` HEAD**. A few items may already be
  ported on `classic-wip` (Phase-2 work happens there per project memory) —
  cross-check before re-doing (esp. ShareAlert Fast Forward).
- `ChatMessageCell.java` was **un-pinned** (kept at the Forkgram/upstream
  version), so poll v2 and its other edits are already present — not in this list.
- "Fixed build" commits (`b42e168e3`, `b0a141c81`) are omitted — they're not
  features.

## Suggested porting order

1. **Dialog list** finish (`DialogCell` — last-seen dots, online statuses,
   disable thumbs, multi-select unread) — same file we're already in.
2. **Fork Settings section** (`92f343c47`) — it's the umbrella host for most of
   the toggles above; porting it first gives the others a home.
3. **Chat context-menu features** (Fast Forward, Go to First Message, copy
   link/ID, delete-all variants) — high user value, self-contained.
4. **Input-panel toggles** (disable AI editor, text-to-voice, Shift-Enter).
5. **de-Google `[TF]` removals** — matter for the F-Droid build; verify whether
   the F-Droid flavor already strips these another way.
