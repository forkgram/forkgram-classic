# Poll v2 — porting log

> **Shipped on `classic-wip` 2026-05-27**, branch tip `fc5e14ba6`. The first task in
> Forkgram Classic whose goal was porting a new upstream feature **into** the classic UI
> rather than preserving the old design — classic preservation policy was relaxed for this
> file pair only.

## Why it was needed

Telegram has been on poll v2 server-side since 12.2.0, but classic pins
`ChatAttachAlertPollLayout.java` at the 12.1.1 baseline. Four v2 toggles existed as inline-string
rows over the legacy `TextCheckCell` layout; duration, country restriction, subscribers-only,
attach-media-to-option, question description and channel-vs-group gating were absent from the create
side entirely, and the display side ignored the new flags — shuffle did not shuffle,
hide-results-until-close still drew zeroed bars after voting, and restricted polls failed silently.

Two things made the port cheap. The resource audit found **nothing missing**: every
`R.string.PollV2*`, `filled_poll_*_24` drawable and poll Lottie file was already in the res tree, so
no resource commit was needed at any stage. And `ui/Components/poll/` already ships the whole
upstream supporting package — `PollAttachedMediaPack`,
`PollAttachedMedia{File,Gallery,Location,Music,Sticker}`, `sheets/CountrySelectBottomSheet`,
`PollCreateCheckCell`, `PollUtils`, `TlUtils.calculateAnswerShuffleHash` — none of which had to be
written.

A wholesale swap to the upstream `PollLayout` was tried first and reverted: 26 compile errors
against the pinned `ChatAttachAlert`, because it needs both the `LAYOUT_TYPE_*` constants with their
delegate setters and the ~200-LoC `enablePollAttachMode` layout-switching state machine. The port
went feature by feature instead, copying upstream blocks where the dependency surface was small and
hand-writing the glue where it was not.

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

- Create side: a photo attached to the description, an answer or the explanation via the per-row
  paperclip sends as a poll with `attached_media` bound to the matching `TL_inputPollAnswer` (DB
  hex-grep for `0xe216eb63` confirms).
- Display side: the description-attached preview renders above the answers, the per-answer thumbnail
  sits at the row's right, and the percent bar stops ~37dp before the photo. Tap opens `PhotoViewer`
  for both description- and answer-attached photos.
- Plain polls and non-poll messages unchanged.

## Known gaps

- `didPressPollMedia` handles only the photo branch; geo, sticker, music and file are a silent no-op
  (upstream's implementation is ~200 LoC).
- `getAllowedLayoutsForIndex` returns the description mask for every slot. Upstream tightens it per
  slot, but classic has no sticker or emoji layouts to exclude anyway.

## Files involved

Pinned (listed in `classic/DESIGN_FROZEN.txt`): `ChatAttachAlertPollLayout.java`,
`ChatAttachAlert.java`, `Cells/ChatMessageCell.java` — the last of which this port un-pinned. Not
pinned: `ChatActivity.java`, `ui/Components/poll/**`, `SendMessagesHelper.java`, `tgnet/TLRPC.java`.
