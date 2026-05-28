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

## Integration hazards — re-verify after each upstream merge

Classic-pinned components meeting modern un-pinned drawing/theming. Each was a real
on-device bug; an upstream re-merge that re-touches the modern side can silently re-break
them (the symptom is usually a no-op, not a compile error). All verified on-device
2026-06-04 (realme C30, low-perf). Companion to `DESIGN_FROZEN.txt`.

- **The screen's top fragment is `MainTabsActivity` (the bottom-tabs wrapper), NOT
  `DialogsActivity`.** `DrawerLayoutContainer` drives the top fragment, so any drawer hook
  implemented in `DialogsActivity` must be FORWARDED from `MainTabsActivity` or it silently
  hits the `BaseFragment` default:
  - `setProgressToDrawerOpened` → drawer-open parallax (#4); without the forward the chat
    list stays static behind the drawer.
  - `isDrawerOpenSwipeEnabled` → folder-vs-drawer swipe guard (#1); without it the drawer
    opens from anywhere instead of only the left edge when folder tabs are shown.
- **Button-only search (#9):** the modern inline `FragmentSearchField` reserves
  `SEARCH_FIELD_HEIGHT` (48dp) in FOUR places; gate each on `animatorSearchVisible` so the
  band collapses at rest (else an empty strip sits above the dialogs / folder tabs and the
  header overlaps the first row): list top padding (`ContentView.onMeasure`),
  `getActionBarFullHeight` (header clip + #8 fill), the `filterTabsView`/`topPanelLayout`
  base in `ContentView.onLayout`, and `getMaxScrollYOffset` (overscroll range).
- **Drawer-open parallax (#4):** upstream perf-gates the slide+scale off on low-perf devices;
  classic ungates it for the drawer-open transition only (keep swipe-back on the upstream
  gate). Also let the list fill the scale's bottom room so the last dialog stays flush
  instead of being cut with empty space under it.
- **Side drawer (#6 + account switch):** modern removed the drawer, so it is not re-themed
  or refreshed automatically. Re-theme in `LaunchActivity.didSetNewTheme` (and reset
  `DrawerProfileCell.switchingTheme`); refresh in `switchToAccount` via
  `drawerLayoutAdapter.notifyDataSetChanged()` or the avatar lags and the selected-account
  checkmark never moves.
- **Dialogs header opacity (#8):** `ContentView.drawBlurRect` is forced to paint a flat
  opaque fill so the header matches the flat folder-tabs strip (no translucent seam).
- **Squared corners (#7):** `popup_fixed_alert4`→`alert2` at the menu call sites; `AlertDialog`
  uses `alert3` with the outline-clip removed. Tracked in `DESIGN_FROZEN.txt`.
- **Edge-to-edge settings rows (#5):** `ThemeActivity` uses `listView.setSections(0, 0, false)`.
- **Attach sheet "Gallery" title (#3):** `ChatAttachAlertPhotoLayout` (modern, un-pinned) sets
  `flp.topMargin = statusBarHeight`, which the pinned 12.1.1 `ChatAttachAlert` action bar
  double-counts → drop it. (Still open in #3: the Liquid-Glass send bar and the 2-tall camera tile.)
