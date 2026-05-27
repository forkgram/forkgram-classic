# Poll v2 — full porting plan

> Status: **shipped on `classic-wip` 2026-05-27** — branch tip `fc5e14ba6`.
> First task in Forkgram Classic where the goal was **porting a new upstream
> feature INTO the classic UI**, not preserving the old design. All other
> Phase 2 work has been preservation; this was the opposite.

## Completion log

| Phase | Commits | Notes |
|---|---|---|
| 0 | `e947395a1` | Original 4 toggle rows (revote / shuffle / add answers / hide results). Pre-session. |
| A | `7190d1535` | Channel-vs-group gate for create-side toggles. |
| B1 | `b8659c3db` | `subscribers_only` toggle. |
| B2 | `12fb20f13` `ba3e93c07` | `countries_iso2` toggle + picker, FLAG_12 fix. |
| B3 | `c55b439c2` | Limit-duration toggle + picker dialog. |
| B4 | `6b95124f2` | Question description row (caption travel). |
| C0 | `4db9f6d6e` | Delegate `sendPoll` 5-arg → 6-arg (`+ CharSequence caption`). |
| D1 | `d14cf9d01` | `LAYOUT_TYPE_*` consts + audio/location delegate setters in `ChatAttachAlert`. |
| D2 | `9f1cb75eb` | `enablePollAttachMode(layoutToOpen, allowedLayouts)` gate. |
| D3 | `3fea68da2` | Per-row paperclip → nested `ChatAttachAlert` poll-attach mode + ItemOptions Replace/Remove. |
| D4 | `c4e2171a0` `6aff0ceee` | `PollAttachedMediaPack` threaded through `prepareSendingPoll`; `groupId = Utilities.random.nextLong()` so the upload binds into `inputMediaPoll.attached_media`. |
| E1–E5 + D5 | `9305ababa` `031966a60` `d8172a922` `0b667f43d` `3844ce404` `1accf1454` | Surgical display-side patches **(superseded by `fc5e14ba6`)**. Effect carried forward by the wholesale restore. |
| F1 | `9c510e7c8` | Inline-string poll toggles → `R.string.PollV2*` refs. |
| Display | `fc5e14ba6` | Wholesale `ChatMessageCell.java` from `master:` (12.7.3, ~29k LoC). Drops the pin entry from `classic/DESIGN_FROZEN.txt`. Five supporting-file fixes (`SIDE_MENU_WIDTH`, `setVisiblePart`, `isDraggingdAnyMusicSeekBar`, `PointF` `getMessageSize`, sticker-size max). `ChatActivity#didPressPollMedia` opens `PhotoViewer` for photo media on tap. |

## Verified on device (32-bit Android 11, F-Droid afatDebug)

- Create-side: photo attached to description / answer / explanation via the
  per-row paperclip, sends as a poll with `attached_media` bound to the
  matching `TL_inputPollAnswer` (DB hex-grep for `0xe216eb63` confirms).
- Display-side: description-attached preview renders above answers;
  per-answer thumbnail at the row's right; percent bar stops ~37dp before
  the photo (no overrun). Tap opens `PhotoViewer` for both description- and
  per-answer-attached photos.
- Plain polls and non-poll messages unchanged.

## Known TODOs after this push

- `didPressPollMedia` only handles the photo branch. Geo / sticker / music /
  file branches in upstream's ~200-LoC impl are not ported (silent no-op).
- `getAllowedLayoutsForIndex` in `ChatAttachAlertPollLayout` returns the
  description mask for every slot — upstream tightens per-slot but classic
  has no sticker/emoji layouts to add anyway.

## 0. Why this document exists

Server-side Telegram has been on poll v2 since 12.2.0. Our classic build pins
`ChatAttachAlertPollLayout.java` at baseline 9cbf03332 (12.1.1), which means:

- 4 of the v2 toggle booleans are now wired (commit `e947395a1`) but only as
  inline-string toggle rows over the legacy TextCheckCell layout.
- The other 6+ v2 features (duration picker, country restrict, subscribers-
  only, attach-media-to-option, question description, channel-vs-group
  gating) are completely absent on the **create** side.
- Display-side rendering (`ChatMessageCell`) silently ignores the new flags
  too — shuffle doesn't shuffle, hide-results-until-close still draws zeroed
  bars after voting, restricted polls fail silently.

The user wants the full v2 feature surface, in any shape that works. Classic
preservation policy is **relaxed** for this file pair only.

## 1. Audit findings (2026-05-27)

Audit ran 4 parallel agents:

| Agent | Scope | Result |
|---|---|---|
| 1 | Create-side feature inventory upstream vs baseline | 8 distinct features, sized TRIVIAL→LARGE |
| 2 | `ChatAttachAlert` API gaps for hosting upstream PollLayout | 10 missing symbols + 1 signature drift |
| 3 | Display-side (chat-cell + ChatActivity) v2 gaps | 5 fields silently ignored, ~1 MEDIUM + 4 SMALL fixes |
| 4 | Resources (strings / drawables / raw lottie) | **Zero missing.** Classic res tree is already a superset. |

### Resource audit shortcut

The resource agent confirmed that every `R.string.PollV2*`, every `filled_poll_*_24`
drawable, every `Todo*` string, and every poll-related Lottie file is already
present in `TMessagesProj/src/main/res/`. The Phase 1 res cleanup never
stripped poll assets. **No res/ commit needed at any phase below.**

### Adjacent code already in tree

`TMessagesProj/src/main/java/org/telegram/ui/Components/poll/` contains the
full upstream supporting package: `PollAttachedMediaPack`,
`PollAttachedMedia{File,Gallery,Location,Music,Sticker}`,
`PollAttachedMediaFile.createMessagePreviewDrawable`,
`sheets/CountrySelectBottomSheet`, `PollCreateCheckCell`, `PollUtils`,
`TlUtils.calculateAnswerShuffleHash`. We don't need to write any of these —
they ship with the upstream snapshot we merge.

## 2. Strategy: hybrid surgical port

Two paths considered and rejected:

- **Wholesale upstream PollLayout** (tried 2026-05-27, reverted): 26 compile
  errors against pinned `ChatAttachAlert`. Path requires (a) porting the
  `LAYOUT_TYPE_*` constants + 4 delegate setters into ChatAttachAlert
  (mostly trivial) **plus** (b) porting the `enablePollAttachMode`
  layout-switching state machine (HARD; ~200 LoC subsystem + lifecycle
  glue). Cost: ≥1 day for compile-clean, more for runtime stability.
- **Pure manual port** (current course since `e947395a1`): too slow for
  large features like attach-media-to-option, which by itself is ~600 LoC
  upstream.

Adopted strategy: **port feature-by-feature in priority order, copy upstream
code blocks into the pinned file where the dependency surface is small,
stub or hand-write supporting glue where the upstream dependency is heavy.**
Each feature ships as its own commit. The pinned file gradually grows in
size — `DESIGN_FROZEN.txt` keeps it listed, but the "frozen at baseline"
invariant becomes "frozen at baseline + explicit poll v2 deltas".

## 3. Work breakdown

Effort scale: TRIVIAL <30 min, SMALL ~1 h, MEDIUM 2-4 h, LARGE ≥1 d.
Each row is one commit.

### Phase A — Foundation (must come first)

| # | Title | Effort | Notes |
|---|---|---|---|
| A1 | **Channel-vs-group gating** for poll-create | TRIVIAL | Sets `isChannel = ChatObject.isChannel(chat) && !chat.megagroup`; forces anonymous=true and hides allowAdding for channels. Prerequisite for A3/A4. |
| A2 | **Sender-side bind already present** (sanity test) | — | Verify `e947395a1` toggles end-to-end with another client. Smoke test only. |

### Phase B — Send-side rows (additive, each commit independent)

| # | Title | Effort | Notes |
|---|---|---|---|
| B1 | `subscribers_only` toggle | TRIVIAL | One boolean + one row + one TL bind. Channel-only. |
| B2 | Country restriction (`countries_iso2`) | SMALL | `ArrayList<String>` field; one toggle row + one detail row (opens `CountrySelectBottomSheet` — already in tree). Channel-only. Plurals string exists. |
| B3 | Poll duration / close-date picker | MEDIUM | Toggle row + value row (opens preset menu via `ItemOptions` or custom via `AlertsCreator.createPollCloseDatePickerDialog`). Sets `close_period` or `close_date`. ~80 LoC. |
| B4 | Question description row | MEDIUM | New `PollEditTextCell` under question; caption travels via separate `CharSequence` arg. **Requires delegate signature change (5-arg → 7-arg)** which propagates into `ChatAttachAlert.java` (3 call sites at upstream master:2570-2595) and `ChatActivity.sendPoll`. Bundle this with C0. ~100 LoC. |

### Phase C — Delegate signature migration

| # | Title | Effort | Notes |
|---|---|---|---|
| C0 | **`PollCreateActivityDelegate.sendPoll` 5-arg → 7-arg** | MEDIUM | Adds `CharSequence caption`, `PollAttachedMediaPack media`, `ArrayList<Integer> correctAnswers`. Pass null for `media` / nullable for caption until B4 / D-series light up. Touches 3 callers in ChatAttachAlert.java (`:2572`, `:2590`) + ChatActivity.sendPoll. ~30 LoC, mechanical. **Do this before B4 and Phase D.** |

### Phase D — Attach media to poll items (the big one)

| # | Title | Effort | Notes |
|---|---|---|---|
| D1 | Add 6 `LAYOUT_TYPE_*` constants + 3 trivial delegate setters to `ChatAttachAlert.java` | SMALL | Constants are 6 lines. Three setters store the delegate + propagate to the existing sub-layout (`locationLayout.setDelegate(...)`, `audioLayout.setDelegate(...)`, `stickersLayout.setDelegate(...)`). |
| D2 | Implement minimal `enablePollAttachMode(layoutToOpen, allowedLayouts)` | MEDIUM | Stores `isPollAttach`, `layoutToOpen`, `pollAllowedLayouts` fields. Suppresses chips/options when set. Switches active sub-layout to `layoutToOpen` when shown. The classic ChatAttachAlert already has multi-layout support (photo/document/audio/location/contact) — wire the gate, don't add new layouts. ~80 LoC. |
| D3 | `PollEditTextCell` attach-icon + tap-to-open-attach-menu | MEDIUM | Adds clip-icon to question / answer / explanation rows. Tap calls `openAttachOrReplaceMenuForOptions(index)` → spawns nested ChatAttachAlert with `enablePollAttachMode(...)`. Uses `PollAttachedMediaPack.set(index, media)`. Reorder logic must swap attachedMedia indices too. ~150 LoC. |
| D4 | `PollAttachedMediaPack` plumbing through `delegate.sendPoll` → `ChatActivity.sendPoll` → `SendMessagesHelper` | MEDIUM | Send side: for each filled slot, send the media in the same album as the poll, with the poll itself attached as a caption-message. Server expects a media group, not a TL_poll field. Existing send-helpers handle albums; just need to assemble. ~100 LoC. |

**Phase D total: ~400 LoC over 4 commits.** Largest single effort in the plan.
If time is tight, ship A+B+C and stop — that gets 90% of the textual feature
surface working without attach-media.

### Phase E — Display-side fixes (`ChatMessageCell.java` + `ChatActivity.java`)

| # | Title | Effort | Notes |
|---|---|---|---|
| E1 | `shuffle_answers` honored on display | SMALL | Port the 2-line answers-array swap from upstream `ChatMessageCell:11440-11441`; pre-compute via `TlUtils.calculateAnswerShuffleHash`. Per-viewer order. |
| E2 | `hide_results_until_close` rendering | MEDIUM | After voting, `MessageObject` already clears the results array, but the cell still draws "0%" bars. Suppress the bar/percent draw when `media.poll.hide_results_until_close && !media.poll.closed`. Show an "Awaiting results" hint instead. Bind `ChatActivity` to re-bind cell when poll closes (server push of `updatePoll`). |
| E3 | `revoting_disabled` user feedback | SMALL | Show a `BulletinFactory` toast when user taps a vote on a poll with the flag and has already voted. Pattern matches upstream `ChatActivity:39575-39577`. |
| E4 | `subscribers_only` + `countries_iso2` vote-restriction toasts | SMALL | Bundle: `PollUtils.getVoteRestrictedFlags` exists; we never call it. Add the call in the vote handler and emit the appropriate bulletin. Pattern: upstream `ChatActivity:30707-30714`. |
| E5 | `open_answers` "Anonymous + Open Answers" subtitle | TRIVIAL | Subtitle line under poll header — append "/ Open" when `open_answers` set. |

**Phase E total: ~80 LoC across 5 commits.**

### Phase F — Polish (optional)

| # | Title | Effort | Notes |
|---|---|---|---|
| F1 | Replace English-inline strings on the 4 already-wired toggles with proper `R.string.PollV2*` references | TRIVIAL | All strings already in `strings.xml` per resource audit. Cosmetic. |
| F2 | Adopt `PollCreateCheckCell` (icon-tile rows) for the v2 toggles | SMALL | Pure look change — keeps classic preservation philosophy if rejected, modernizes if accepted. **Default: skip.** |
| F3 | Quiz "Hide results until close" interaction tweak | TRIVIAL | Currently we hide the row when quizPoll=true; verify nothing surprising in quiz flow with v2 fields. |

## 4. Total estimate

| Phase | Time |
|---|---|
| A | 30 min |
| B | 4-6 h |
| C | 1-2 h |
| D | 6-8 h |
| E | 2-3 h |
| F | 1 h optional |
| **Total core (A+B+C+E)** | **8-12 h** |
| **Total with D (attach media)** | **14-20 h** |

That's a full sprint for one developer if uninterrupted. Recommend splitting:
land A+B+C first (call it "poll v2 settings rows ship"), evaluate, then
schedule D as its own milestone.

## 5. Risk register

| Risk | Mitigation |
|---|---|
| Signature change in C0 misses a call site → runtime NPE | grep for every `sendPoll(` callsite + every `PollCreateActivityDelegate` implementor before commit. |
| Adopting `PollAttachedMediaPack` reveals more missing supporting classes in `Components/poll/attached/` | Resource agent confirmed all classes present in tree. Re-verify before D3. |
| D2 layout-switching breaks the normal attach-flow (paperclip from chat) | Smoke-test chat attach (gallery/file/location/audio) after every D commit. |
| E2 needs `MessagesController` push handler — server tells us "poll just closed", we need to re-bind cell | Look at upstream's existing notification listener for `pollResults` updates; classic already handles vote updates via `didReceivedNotification(pollResults)`, only need to widen what re-draw triggers on. |
| Phase F2 (cell visual refresh) clashes with classic preservation policy | Strict opt-in. Default decision: do not ship F2 unless user specifically asks. |

## 6. Rollback plan

Each commit is independent and atomic. To revert:

- A+B+C only touches `ChatAttachAlertPollLayout.java` + (for C0) `ChatAttachAlert.java` + `ChatActivity.java` 3-arg signature spot.
- E only touches `ChatMessageCell.java` (pinned but already deviated for blockpanel; another small delta is acceptable) + `ChatActivity.java`.
- D adds new fields to `ChatAttachAlert.java`; reverting D2/D3/D4 cleanly is feasible since the surface stays additive.

Keep the existing inline-string commit `e947395a1` as the baseline checkpoint
to revert past in worst case. After F1 lands, that commit becomes a no-op
diff and can be amended away with `git rebase -i` (or kept for history).

## 7. Open questions

- Should the duration picker include the preset menu (3h/8h/24h/72h + custom)
  or only custom? Upstream defaults to preset; matches user-facing behaviour
  of other Telegram clients. **Default: preset + custom.**
- Should A1 (channel gating) force `hideResults=true` for channel polls as
  upstream does? Investigate during A1 — minor UX nuance.
- D4: send poll-with-attachments as media group or as individual messages?
  Upstream sends as group. Match.

## 8. Related files (read before starting)

Pinned (in `classic/DESIGN_FROZEN.txt`):
- `TMessagesProj/src/main/java/org/telegram/ui/Components/ChatAttachAlertPollLayout.java`
- `TMessagesProj/src/main/java/org/telegram/ui/Components/ChatAttachAlert.java`
- `TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java`

Not pinned:
- `TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java`
- `TMessagesProj/src/main/java/org/telegram/ui/Components/poll/**`
- `TMessagesProj/src/main/java/org/telegram/messenger/SendMessagesHelper.java`
- `TMessagesProj/src/main/java/org/telegram/tgnet/TLRPC.java` (TL schema)

Cached upstream snapshots used during audit:
- `/tmp/upstream-poll.java` — `master:ChatAttachAlertPollLayout.java`
- `/tmp/baseline-poll.java` — `9cbf03332:ChatAttachAlertPollLayout.java`
- `/tmp/poll-diff.patch` — 2328-line unified diff
