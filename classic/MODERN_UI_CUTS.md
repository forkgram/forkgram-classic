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

## Upstream 12.8 bump — chat-header "islands" (the `/classic-bump` run, 2026-06-20)

Upstream 12.8 split the chat top title bar (one solid back+avatar+name+menu bar) into three floating
rounded **"island" capsules**. It is **ActionBar "glass mode"**, NOT a new file:
- `ActionBar.setupGlass()` creates `glassDrawable`/`glassDrawableBack`/`glassDrawableMenu` (radius-23 capsules);
  `ActionBar.dispatchDraw` paints them; `ChatAvatarContainer.setGlassMode()` shrinks the title text;
  `ActionBarMenu.glassMode` insets the menu. Activated only from `ChatActivity`/`ChatAttachAlert`/`ChannelAdminLogActivity`.
- **Reverted automatically** by the existing pins: restoring `ChatActivity` + `ActionBar` (+ `ActionBarLayout`)
  to 12.1.1 removes all glass creation and drawing → the solid bar returns. `ChannelAdminLogActivity` (un-pinned)
  had its own `actionBar.setupGlass(...)` line — dropped in the rebase. Device-verified: solid header, no capsules.
- **Residue = no-op shims** for non-pinned 12.8 callers of the now-absent API:
  `ActionBar.checkAvatarContainerWidth(boolean) {}` (called by `ChatAvatarContainer`); `ActionBarLayout`
  `isLayersLayout()/isRightLayout()` (new abstract `INavigationLayout` methods, return false) + `setIsRightLayout() {}`
  (called by `LaunchActivity` — the iPad split "layers" pane stays dormant). `setupGlass`/`setSearchFactor`/etc.
  ended with **0 callers** once the chat pins reverted, so no shims were needed.

Other 12.8 drift handled in the same run (see the `/classic-bump` shim catalog): `ChatMessageCell` swapped to
the 12.8 version (un-pinned; old one imported the removed `FrameTickScheduler`); Instant-View 2.0 relocated the
page-block TL classes (`TLRPC.TL_pageBlockList` → `TL_iv.pageBlockList` — dropped `TL_`, lowercased) in
`ProfileActivity`; drifted call sites fixed in `ChatActivity` (`SearchTagsList`/`ChatSearchTabs` ctors,
`HashtagHistoryView` setter-callbacks, `MotionBackgroundDrawable.updateAnimation()`, `isWebBrowserOpenInApp`);
new IV2.0 hooks shimmed onto pinned classes (`ChatActivity.PROGRESS_FULL_ARTICLE`, `ChatAttachAlert.showSendButtonOnly`/`setLocationPicker`).

**Upstream base advanced 12.7.3 → 12.8.1 (6916); modern release base = tag `12.8.2.0` (`39883086c`).**
jni delta was 2 files / 16 lines (no ffmpeg/voip/rlottie/tde2e) → native cache stayed warm, ~5-min rebuilds.

## Same-minor base move — 12.8.2.0 → 12.8.4.0 (the `/classic-bump` run, 2026-07-01)

**No new redesign.** Both `12.8.2.0` and `12.8.4.0` sit on the **identical upstream snapshot**
`update to 12.8.1 (6916)` (`9b50143d88`) — the modern fork just added patch-level *feature* commits on
the same core. So there was nothing to revert (no glass/islands drift) and **0 reshims** — the 12.8
library shims all still held, library compiled clean first try. Net base delta = 27 files / ~700 lines,
**zero jni churn** (native cache warm).

Conflict resolution (rebased the 5-commit stack `--onto modern-12.8.4.0`):
- **4 pinned files took `--theirs` (classic 12.1.1):** `ChatActivity`, `ChatAttachAlert`, `EmojiView`,
  `ProfileActivity`. `ChatActivityEnterView`/`PhotoViewer` auto-merged (new-base feature hunks kept).
- **2 semi-pins hand-merged:** `DialogsActivity` — new base re-added a "New Message" item + a
  "Show/Hide bottom tabs" toggle to the top-right overflow menu, i.e. exactly what classic **#61**
  deliberately moved to the drawer + Fork Settings → kept classic (dropped the re-additions); the
  benign `MainTabsHiddenHint` toast + `hideInAppHints` hint-cell gate auto-merged and were kept.
  `ForkSettingsActivity` — **union**: kept classic's `updateCheckIntervalRow = -1` (F-Droid = no
  in-app updater) AND the new base's `disableTabletModeRow` (fully wired downstream).
- **Version** set to `12.8.9` / code `6925` — classic runs its **own** patch line (was at `12.8.8`/`6924`,
  already *ahead* of the modern base's `12.8.4`/`6919`), so code = last-classic + 1 for F-Droid
  monotonicity (NOT base + 1, which would regress). Final number is `/classic-release`'s call.

### Feature-parity re-port onto the pins (owner chose FULL parity, 2026-07-01)

`--theirs` on the 4 conflicted pinned files initially dropped the NEW-in-12.8.4 fork features/fixes that
had landed in them. These are pure *logic* (no design), so they were **re-applied on top of the 12.1.1
pins** (same model as porting #33's AI button onto the pinned enter-view) — every anchor still exists in
the 12.1.1 code because these subsystems predate the redesign:
- `disableLinkPreviewByDefault` — ChatActivity search-links gate (1 site).
- `hideSavedMessagesTags` — ChatActivity `savedMessagesTagHint` + `actionBarSearchTags` gates (2 sites).
- **show archived sticker packs** (+81) — EmojiView: 2 fields, `requestArchivedStickerSetIfNeeded`, the
  `showArchivedStickers` block in `updateStickerTabs`, `archivedStickersDidLoad` observer/handler, and a
  placeholder→full-set refresh in the `groupStickersDidLoad` branch. **Adapted**: modern's hunk used
  `stickerSetId`/`stickerSet` locals; classic's `groupStickersDidLoad` carries `id=args[0]`, `set=args[1]`
  (`MediaDataController.getStickerSet` posts `groupStickersDidLoad(set.id, set)`). Toggle already wired in
  Fork Settings (`showArchivedStickersRow` → same `showArchivedStickers` key).
- long-press-to-copy name — ProfileActivity `nameTextView[1]` (`addToClipboard` + `createCopyBulletin`).
- supergroup custom-emoji in captions — ChatAttachAlert `commentTextView`/`topCommentTextView.setChatInfo`
  (both are `EditTextEmoji`, which has `setChatInfo`).

NOT ported (deliberate): `hideAiEditor`-on-media guard — **N/A**, classic's ChatAttachAlert has no
`showAiButton` (that attach-sheet AI button was never ported; #33 only added one to the input bar). The
forward-menu-keyboard `suppressLayout` scroll fix and the report-mode `sortIds` crash guard were **left
out** — the former's anchor is only partial / relies on the modern `suppressLayout` mechanism, and the
latter's selection code path doesn't exist in the 12.1.1-pinned ChatActivity (classic isn't affected).
