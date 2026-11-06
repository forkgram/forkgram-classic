# Forkgram Classic — Phase 2 backlog: un-ported Forkgram UI features

> Generated 2026-05-31. Living document — update the status column as features land.

## Why this exists

Phase 1 reset every `DESIGN_FROZEN` file to the 12.1.1 baseline (`9cbf03332`), which predates the
entire Forkgram patch set, so any Forkgram modification that lived *inside* a frozen UI file was
discarded — "no Forkgram features at this stage, design only". Phase 2 ports them back by hand. The
~95% of Forkgram patches that live outside the frozen files (`messenger/**`, controllers, DB, push,
locale, settings backend) were never overwritten; this backlog tracks only what the ~15 frozen UI
files lost.

## Method

`master` is the 12.7.3 upstream snapshot and `0d4dabfda` the full Forkgram head sitting directly on
it, so `master..0d4dabfda` is the 250 pure Forkgram commits with no redesign noise. Per frozen file,
`git log --oneline master..0d4dabfda -- <file>` lists what the restore wiped; absence in `classic`
HEAD was then confirmed by signature grep. The modern source at `/home/h/src/TelegramAndroid`
(branch `dev`) is the porting reference.

Legend: ✅ ported · 🔶 partial (backend survived, frozen-file wiring missing) · ❌ missing · ⚙️ perf
only · 🧹 de-Google removal (F-Droid build).

## Audit 2026-05-31 — supersedes the stale ❌ in the tables below

A presence audit found the original backlog **over-counted** misses: several features had already
reached `classic` from `classic-wip` via the squash, and several "missing" verdicts were **false
negatives** from token mismatch. Always read the code before porting.

**Ported in that session:** bare-icon dialog badges + poll-votes mention · Fork Settings entry · Go
to Beginning · Copy without protocol · Copy URL from inline · Copy private link · hide-title ·
delete-all (your msgs / unpinned / all topics) · disable quick-reaction · calendar from floating
date · remove input gift button · disable-video-on-volume guard · hide Send As · disable dialog
thumbs.

**Already present:** copy dialog ID · edit phone/username/bio long-press menu · debug button removed
· proxy "…" text · shift-enter no-send · mention-by-name · ShareAlert Fast Forward ·
hide-bottom-button · GIF auto-caption · disable-bot-link params · drop-screenshot-caption ·
online/last-seen dots.

**Skipped as not clean or not general:** anonymous forward (`4419eaec3`, dead flag — the
`IS_ANONYMOUS_FORWARD` field is only ever set, never read) · auto-dot (`c6691d5cf`, owner-only) ·
disable-online (`40578661b`, debug hack) · de-Google `[TF]` kills (the F-Droid flavor handles them)
· GitHub auto-updater, check-updates row and the fork-update popup (F-Droid is the only update
channel).

### Close-out, session 2

All remaining UI features ported or resolved; verified with
`:TMessagesProj:compileDebugJavaWithJavac -PF_DROID=1` (JDK 21).

- ✅ **Fast Forward** (`80b66bc63` + `9497c3e88`). Everything but the `ChatActivity` wiring had
  survived, so the port added the `forward_anonym` menu id (= 80), `createShareAlert()` +
  `showAnonymShareAlert()`, the action-mode `replaceForward` toggle, the click handler, the `case
  202` context-menu entry in `processSelectedOption`, and the openForward 76→7 inline-`ShareAlert`
  collapse. Kept the classic `!isSavedMessages && != VERIFY` guard that the upstream commit dropped
  — strictly safer. The **group-media floating button was deliberately skipped**: classic's 12.1.1
  `ChatActivity` never wired the `actionsButtonsLayout` subsystem (0 refs), so there is no host, and
  FF core does not need it. On-device verification of both Fast Forward and normal forward is still
  worth doing — high blast radius.
- ✅ **attach text to voice** (`72fecd146`) — backend survived; added the EnterView side
  (`voiceCaption`, `slideText.bringToFront()`, `TimerView.timestamps` with timestamp-to-clipboard,
  `timestamps` init in `start()`).
- ✅ **reply-quote on voice** (`99e0f811e`) — EnterView now threads `replyingQuote` (was `null`) into
  `MediaController.startRecording` / `setReplyingMessage` / `prepareResumedRecording`.
- ✅ **auto-send text before sticker** (`a99f37155`), **last-seen-dots toggle** (`8e87e5b71`,
  `DialogCell.colorOnline` now honours `enableLastSeenDots`), **proxy hide-sensitive text**
  (`ef3163007`), **Move "Call" to the bottom of the chat menu** (`13adcc961`), **extract media from
  webpage** (`0d4dabfda`), **select-all-between** (`e9cb0a5ff`, device-verified), **copy username on
  tap** (`06b319236`).
- ✅ **video do-not-compress** (`f9ebfc3a5`) — already wired via `ChatAttachAlertDocumentLayout:912`
  → SendMessagesHelper. The 2023 commit's `didSelectPhotos` positional flip maps to today's
  `groupMedia` argument, **not** compression, so it was correctly not applied — it would be a
  grouping regression.
- ✅ **Monet themes** (`119a1eb8d`) — a false negative: the commit is an ancestor of HEAD, and the
  `Theme` hunks, the LaunchActivity register/unregister, `MonetHelper.java` and both `.attheme`
  assets are all in the tree.
- ✅ **Drawer hide-sidebar** (`e82b77879`) — reconstructed, since the commit is empty here and in the
  reference: `DrawerProfileCell` blanks the phone number under `SharedConfig.hideSensitiveData()`,
  name left intact.
- ⛔ **Drawer secret-chat/channel menu items** (`3534d6ccb`) — empty commit; classic deliberately
  comments these out with icon ids = 0, and no drawables or handlers exist.
- ⏸ **multi-select unread count** (`989c2fb1d`) — deferred, low value.
- ⚙️ **DialogCell perf opts** — `81f2aa60a` already present; `a5b03b85f` and `5e0a3a070` skipped as
  non-user-visible tweaks in the hot draw path, no clean port, regression risk without device
  profiling.

### Completeness proof and the 4 dead toggles

`0d4dabfda` is a direct **ancestor** of `classic` (it *is* their merge-base), so a Forkgram feature
can only be lost where a frozen-file reset removed it afterwards. Checked two ways: all **58**
commits in `master..0d4dabfda` touching any of the 23 frozen files map to ported / present / skipped
/ deferred, and every ForkSettings key was checked for a consumer outside `ForkSettingsActivity`.
That sweep found **4 toggles whose backend a frozen reset had eaten** — the row was there and did
nothing:

- ✅ `mentionByName` (`ff64b4813`) — ChatActivity guard re-added: mentioning inserts a name-link
  instead of `@username`.
- ✅ `disableSlideToNextChannel` (`51d415182`) — pull-to-next-channel guard re-added. The feature
  does exist in the 12.1.1 base, contrary to the earlier note.
- ✅ `disableRecentFilesAttachment` (`a676e4e9d`) — the surviving guard in
  `ChatAttachAlertDocumentLayout.loadRecentFiles()` read the **wrong key**
  (`"disableSlideToNextChannel"`), a copy-paste bug **present in upstream Forkgram itself**. Fixing
  it repairs both toggles — the slide toggle no longer wipes recent files.
- ✅ `stickerSize` (`a9c4aaa41`) — the `ChatMessageCell` consumer was lost in the wholesale restore
  (`fc5e14ba6`) and the slider constant had drifted to a hardcoded 20. Re-added `MAX_STICKER_SIZE =
  14.0f` plus the formula and pointed `endStickerSize` back at it.
- ⛔ `hideAiEditor` (`f4df6fb43`) — left as a no-op: the 12.1.1 EnterView has no AI editor to gate.

**No remaining Forkgram UI feature is unaccounted for.** Also moot: Saved Messages on search
long-press (`772d0c6b4`) is tangled with the `mainTabsHiddenFork` bottom-tabs subsystem and is not a
standalone port.

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

The drawer *files themselves* were restored from baseline (the drawer exists again), but the
Forkgram patches layered on them are missing:

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

---

## Files with **no** Forkgram delta (nothing lost)

`EmojiView.java`, `ChatAttachAlert.java`, `ChatAttachAlertPollLayout.java`, `RecyclerListView.java`,
`ActionBarLayout.java`, `BaseFragment.java`, `CustomNavigationBar.java`,
`DrawerLayoutContainer.java` and the remaining Drawer cells — Forkgram never modified these, so
restoring them to baseline cost no features.

## Caveats

- Analysis is against `classic` HEAD; a few items may already be ported on `classic-wip`. "Fixed
  build" commits (`b42e168e3`, `b0a141c81`) are omitted — not features.
- `ChatMessageCell.java` is **un-pinned**, so poll v2 and its other edits are already present and
  out of scope here. One exception, ported 2026-06-05: the **forwarded-message date** (`751f51692`)
  was absent. Classic renders it on its **own third line** under the forward name rather than
  appending it to the "Forwarded from" label as modern does, which truncates on narrow or media
  bubbles. Implemented as a separate `forwardedDateLayout` one line below at the same per-line step,
  with reserved height (`namesOffset` ×2→×3, the forward term in `replyStartY`/`summaryStartY`) and
  a lazy `ensureForwardDateTime` / `ForkApi.TLRPCMessages` fetch when `fwd_from.date == 0`.
- **Classic nests `DialogsActivity` inside `MainTabsActivity`.** Only `MainTabsActivity` is in
  `actionBarLayout`'s fragment stack, so anything pushed there never reaches the dialogs `ActionBar`
  that draws the visible title. It bit us on 2026-06-05: the connection-state overlay ("Connecting"
  / "Updating" / "Connecting to proxy", set by `LaunchActivity.updateCurrentConnectionState`) went
  to the invisible `MainTabsActivity` while the dialogs title stayed stuck on the custom "Forkgram
  Classic". Fixed by also forwarding it to `((MainTabsActivity) actionBarLayout.getLastFragment())
  .getDialogsActivity().getActionBar()`. **Rule:** anything aimed at the dialogs `ActionBar` must be
  forwarded explicitly into the nested `DialogsActivity`.
- **Status-bar icon colour resets on resume** (2026-06-05). `onCreate` sets a baseline decor
  `systemUiVisibility` of `0x700` with no `LIGHT_STATUS_BAR`, and on resume the decor view reverts
  to it *after* `checkSystemBarColors` has set the light flag — so on a light-theme dialogs list the
  icons go white-on-white. Cold start is fine and a chat round-trip cleared it, which is the tell.
  Fixed by re-applying in `onWindowFocusChanged(hasFocus)` → `checkSystemBarColors(false, true,
  false)`, which fires after the revert.
- **First-login side-menu header glitches** (2026-06-05). The hand-ported drawer is built once at
  startup, before any account is activated, and was only refreshed on account switch:
  - *Empty menu items* — `DrawerLayoutAdapter.resetItems()` early-returns when the selected account
    is not `isClientActivated()`. Fixed by rebuilding the adapter every time the drawer opens
    (`DrawerLayoutContainer.openDrawer()` → `notifyDataSetChanged()`), which is timing-independent.
  - *White avatar for a photo-less account on the light theme* — `setUser` overrode the avatar
    colour with `key_avatar_backgroundInProfileBlue`, whose default is white and which the light
    theme never redefines (the dark one does, so it showed there). Fixed by dropping the override,
    leaving `new AvatarDrawable(user)`'s per-user gradient.
  - *Name and phone drawn shifted down* — on the cold-login first layout the text height is not
    measured yet, so `SimpleTextView`'s centred vertical offset draws low, and the pinned header is
    not re-laid-out until a restart. View bounds were correct (`top=253`); the shift is purely the
    draw offset. Fixed by forcing one more layout pass after `setUser`.
- **`updateCheckIntervalRow` removed** from `ForkSettingsActivity` (set to `-1`, not allocated in
  `rowCount`) — F-Droid is the only update channel (2026-06-05).
