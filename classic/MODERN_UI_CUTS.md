# Modern UI cuts (Phase 1)

What was removed or rolled back from the modern Telegram-Android 12.7.3
codebase to restore the 12.1.1 design domain. Companion to `PLAN.md` §1.2
and `DESIGN_FROZEN.txt`.

## Files deleted outright (redesign-only — absent at 12.1.1 baseline)

- `ui/Components/ChatActivityTopPanelLayout.java` — new chat top panel with
  animated underlay.
- `ui/Components/ChatReplyContainer.java` — new reply container.
- `ui/Components/ChatAttachAlertEmojiLayout.java` — embedded emoji page
  inside the attach menu.
- `DrawerLayoutAdapter` — holiday-icon branches collapsed (`msg_*_ny`,
  `_14`, `_hw` drawables are gone upstream).

## Files frozen at 12.1.1 (baseline commit `9cbf03332`)

### Chat surface
- `ui/ChatActivity.java` (−8457 LoC vs master)
- `ui/Components/ChatActivityEnterView.java` (−3163)
- `ui/Components/ChatAttachAlert.java` (−2259)
- `ui/Components/ChatAttachAlertPollLayout.java`
- `ui/Components/ShareAlert.java`
- `ui/Components/EmojiView.java`

### Profile
- `ui/ProfileActivity.java` — legacy profile screen.

### Action bar (semi-pin: take fixes, freeze layout logic)
- `ui/ActionBar/ActionBar.java`
- `ui/ActionBar/ActionBarLayout.java`
- `ui/ActionBar/BaseFragment.java`
- `ui/ActionBar/DrawerLayoutContainer.java`

### Dialog list cell (semi-pin)
- `ui/Cells/DialogCell.java` — pinned but routinely takes selective perf
  patches (online-status cache, color computation cache, lazy BoolAnimator).

## Files restored from 12.1.1 (upstream had deleted them)

- Left drawer stack:
  - `ui/Adapters/DrawerLayoutAdapter.java`
  - `ui/Cells/DrawerActionCell.java`
  - `ui/Cells/DrawerAddCell.java`
  - `ui/Cells/DrawerProfileCell.java`
  - `ui/Cells/DrawerUserCell.java`
  - `ui/Cells/InviteTextCell.java`
  - `ui/Components/SideMenultItemAnimator.java`
  - `ui/ActionBar/CustomNavigationBar.java`
- Resources:
  - `bottom_shadow.*`, `menu_shadow.*` (drawer shadows)
  - `blockpanel.{mdpi,hdpi,xhdpi,xxhdpi}.png` (pinned-message bar fill)

## Files initially pinned but rolled back to upstream during Phase 1

Each caused cascading errors deemed too costly to shim. See `PLAN.md`
"Phase 1 deferrals" and `DESIGN_FROZEN.txt` for full notes.

- `ui/ActionBar/Theme.java` — 40+ dependent files referenced new color
  tokens (MessageObject, EmojiTabsStrip, ButtonWithCounterView, …).
- `ui/Components/RecyclerListView.java` — UniversalAdapter + 7 other files
  depend on new public API (`forcedSections`, `TAG_NOT_SECTION`,
  `hasSections()`, `setSections(...)`).
- `ui/LaunchActivity.java` — 12.1.1 imported the now-removed
  `com.google.firebase.appindexing` package; init/loading logic also drifted.
- `ui/Cells/ChatMessageCell.java` — un-pinned 2026-05-27. While pinned at
  12.1.1 the poll v2 display subsystem was unreachable: attached media,
  click-to-open, countdown timer, `hide_results_until_close`,
  `shuffle_answers`, recent voters, and the percent bar respecting media
  padding all relied on upstream code. The file has no significant
  redesign concern that warranted continued freezing.

## Net user-visible effect

What disappears once Phase 1 ships:

- New message-bubble rendering — gone.
- New emoji / sticker / GIF picker — gone.
- New chat top panel with animated subtitle overlays — gone.
- New dialog-list top panel (status / suggestion bubble bar) — opt-in
  only via `UserConfig.mainTabsHiddenFork`.
- Redesigned profile screen — gone (legacy 12.1.1 profile restored).
- Floating bottom panel on the dialog list — opt-in (default UI is the
  classic side drawer).
- New chat reply container — gone (legacy in-input reply preview restored).
- Attach-menu emoji page — gone.
