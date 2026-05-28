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

## Same-minor base move — 12.8.4.0 → 12.8.5.0 (the `/classic-bump` run, 2026-07-14)

**No new redesign, again.** `12.8.4.0` and `12.8.5.0` sit on the **same upstream snapshot**
`update to 12.8.1 (6916)` (`9b50143d88`) — the modern fork only added feature commits on the identical
core. Nothing to revert, **0 reshims**, library compiled clean on the first try. Base delta = 65 files /
~3.2k lines, **zero jni churn** (native cache warm).

Conflict resolution (rebased the 5-commit stack `--onto modern-12.8.5.0`):
- **2 pinned files took `--theirs` (classic 12.1.1):** `ChatActivity`, `ProfileActivity` — then the
  new-in-12.8.5 features they carried were re-ported onto the pins (below), per the FULL-parity rule.
- **`ChatMessageCell`** is un-pinned/wholesale-upstream → `git checkout modern-12.8.5.0 --` it (the rebase
  leaves it at the OLD base; here it was missing the `ForkOfflineTranscribe.isActive()` hook).
- **`ForkSettingsActivity` was REWRITTEN upstream** (row-int `RecyclerListView` → declarative
  `UItem`/`UniversalAdapter` + `ID_*` constants + `highlight()` + a settings-search index). A textual merge
  is meaningless — took the **new screen** and re-applied classic's edits *semantically*: the #61
  "Show bottom tabs" toggle (new `ID_SHOW_BOTTOM_TABS`, backed by the same `UserConfig.mainTabsHiddenFork`),
  the dropped update-check-interval row (F-Droid = no in-app updater), and the "Forkgram Classic" default
  custom title. Design-safe: the new screen builds a plain `UniversalRecyclerView` with **no**
  `setSections(...)`, so it brings no rounded-card insets (cf. #13).
- **CI workflows** `fd.yml` / `tandroid.yml`: modify/delete → stay **deleted** (classic keeps only
  `docker-builder.yml`).
- **Version** `12.8.13` / code `6929` — classic's own patch line, code = last-classic (`12.8.12`/`6928`) + 1.
  Base + 1 (`6921`) would REGRESS below the last release and break F-Droid monotonicity.

**`LocationActivity` left classic's patch set — not a loss:** the modern fork **adopted classic's #73 fix**
(`zoomToBoundingBoxSafe`, the osmdroid live-location hang), so classic's version now equals the base.

### Feature-parity re-port onto the pins (2026-07-14)

- **Configurable formatting menu** (`FormattingMenu`) → `ChatActivity.fillActionModeMenu`. Without this the
  new Fork Settings entry ("Formatting menu" → `FormattingMenuActivity`) would be a **dead setting**: the
  user reorders/hides items and the pinned 12.1.1 menu-builder ignores them. Ported the loop verbatim; the
  "already filled" guard had to become `hasFormattingMenuItems()` (group test) because the old
  `findItem(menu_bold) != null` guard breaks once *bold itself* can be hidden. Two side effects, both
  intended: the fork's **`menu_date`** item now appears in classic (its handler already existed in
  `EditTextCaption`), and `includeLinks` is now **honoured** instead of ignored (only `PeerStoriesView`
  passes it as a variable — live stories legitimately suppress "Create link").
- **Fork settings-search index** (56 entries) → `ProfileActivity.onCreateSearchArray`. Adapted from the
  base's static `f.presentFragment(...)` form to classic's instance form, dropped the
  `ID_UPDATE_CHECK_INTERVAL` entry (that row no longer exists), added a classic-only entry for the #61
  toggle. Ids 1000+ don't collide with classic's (max 914).

NOT ported (deliberate), both `ChatActivity`: `actionBar.setGlassAvatarSquare(...)` — **N/A**, classic's
pinned ActionBar has no glass surface. And the base's own fix for the extract-media button overlapping the
reply text — it is the modern take on **classic's #77**, but implemented against `ChatReplyContainer`, a
redesign class classic *deletes*; classic keeps its own #77 fix.

## Upstream 12.9 bump — Communities, Rich (article) editor, ephemeral bot replies (the `/classic-bump` run, 2026-07-16)

Base move `[Release] 12.8.5.` (`4e3482138`) → `[Release] 12.9.0.` (`f46f7b867`, fetched from the modern
repo's `dev` — **no release tag existed yet**, only the branch head; `backup/dev-pre-rebase-2026-07-16`
confirmed the rebase was same-day fresh). Upstream snapshot moved 12.8.3 → **12.9.0 (6966)**
(`9bcf3d2769`); jni churn was `tgnet/ApiScheme` only (+190 LoC) — native cache stayed warm.
Version: `12.9.0` / code `6967` = base (6966) + 1 — monotonic over classic's last release (6929).

### The 12.9 redesign that auto-reverted with the pins
- **"Liquid glass" surfaces** (`iBlur3FactoryLiquidGlass`, blurred top panels, `setDefaultRadiusDp`
  rounded-card top panels in DialogsActivity) — classic keeps flat solid panels.
- `menu.setTranslationX(-dp(5))` + `outline_header_search` on the dialogs action bar (kept classic
  `msg_search`, #66); `ViewPagerFixed.SELECTOR_TYPE_BUBBLE_STYLE` search tabs (kept classic `0` style);
  `popup_fixed_alert4` preview-menu chrome (kept `popup_fixed_alert2`); 12.9 also commented out the
  dialogs `inPreviewMode` avatar header upstream — adopted (dead code either way).
- The 12.9 avatar radius change dp(28)→dp(26) in DialogCell — **rejected**, classic keeps dp(28).

### New no-op shims on the pinned classes (12.9 APIs with live callers)
- `ActionBar.setupGlass(factory, colorProvider)` + `setGlassOnlyBack()` — the new Community screens
  request the glass bar; classic's flat bar ignores them (the screens inherit the classic bar).
- `ActionBar.checkMenuItemsWidth()` (ActionBarMenu re-animates the menu capsule) and
  `ActionBar.setAdditionalTextLeft(int)` (12.9 title offset hint) — no capsule, no offset.
- Removed-core-symbol restorations (reverse shims, marked `forkgram-classic`): `SharedConfig.noStatusBar`,
  `AndroidUtilities.LIGHT/DARK_STATUS_BAR_OVERLAY` + the 3-arg `setLightStatusBar(Window,…)` overload,
  `ChatAvatarContainer.setTitleExpand(boolean)` (12.8 helper the re-shimmed pinned ChatActivity calls),
  `msg_ton`/`menu_my_ton` drawables (12.9 renamed to `outline_gram_24` and deleted the assets; pinned
  screens keep the classic glyphs). `TLRPC.TL_botCommand` list element became abstract `TLRPC.BotCommand`
  (2 pinned call sites patched). `BaseFragment.set/getBulletinDelegate` (12.9 Bulletin stores its delegate
  on the fragment).

### FULL-parity feature re-ports onto the pins
- **Communities** (chat-list aggregation): `DialogCell` — `TL_dialogCommunity` unread counts,
  `formatCommunityDialogNames()` member-chat preview, `insideCommunityList*`/`isHiddenInCommunity` state,
  the eye-off icon in the mute slot (`dialogs_hiddenDrawable`), swipe-to-ungroup, the **community-arrow
  avatar badge** (kept), `setCustomMessageWithoutRebuild`, `isDialogCommunity()`. **Classic design choice:
  the community row's avatar stays a standard circle** — the modern rounded-square avatar over the
  "stacked cards" backdrop (`drawCommunityCardDrawable`) was NOT ported. `ChatActivity` — header-avatar tap
  opens `CommunitySheet` for linked chats, eye-off title icon, `TL_messageActionChangeCommunity` menu gate.
  `ProfileActivity` — a classic **list row** (`CommunityLinkView`, `linkedCommunityRow`) opens the sheet;
  the modern avatar arrow-badge overlay (`communityItem`) was NOT ported (modern-header chrome).
  `DialogsActivity` (classic-owned) kept all merged community machinery; the top-right menu grew the
  12.9 community branch while keeping the #61 classic cuts. The **community-creation entry** lives in
  `ChatEditActivity` (un-pinned; restored after the rebase's whole-file `--theirs` clobbered it — see hazard).
- **Rich (article) editor**: `ChatActivityEnterView` — `richButton` (top-right; the classic AI button moved
  to top-left, matching the modern 12.9 pair), rich-draft preview chip + delete-draft button, draft
  save/load/clear plumbing, `sendRichDraft` via `prepareSendingArticle`, non-premium conversion sheet,
  `SendButton` lock badge + 4-arg ctor. `ChatActivity` — `getDraftThreadId()`, draft-preserving `saveDraft`,
  rich-draft apply, article edit → `RichEditor`, copy-as-HTML (`RichMediaClipboard`), article translate
  (`TranslateAlert2` rich overload), full-article "show more" loader (`loadFullRichMessage`), article-photo
  viewer (`ChatArticlePageBlocksAdapter`/`Provider` with classic clip budget), checkbox-toggle delegate.
  `ChatAttachAlert` — real `setLocationPicker()` (upgraded from a 12.8 no-op: the editor now ships in
  classic), `enablePollAttachMode(int)`, `openAttachLayoutForType`, `get/setTypeButtonsHidden` (classic
  84dp buttons row + `shadow`), public `currentPanTranslationY`, `disableBottomFade()` hook.
  `EmojiView.hideBottomTabContainerBackground()`. `RichEditor.animateFrom` — classic has no input island,
  so the morph-from-island animation degrades to the standard fragment transition (guarded upstream).
- **Ephemeral bot replies**: `ChatActivity` — `MentionsAdapter.EphemeralCommand` click (sends with
  `ephemeralReceiverBotId`) + long-press insert + no-swipe-reply-on-own-ephemeral gate. The enter-view
  **outline send-button morph was NOT ported** (modern island-animator machinery); sending works, the
  special visual is skipped.
- **GIF search (350M)** — server-side; the pinned classic GIF panel queries the new index unchanged.

### Classic re-skin of the new RichEditor screen (the "islands → flat bars" pass)
The 12.9 editor floats rounded capsule buttons (back / undo-redo / emoji / AI / block-type row / attach /
send) over white gradients. Classic pass in `RichEditor.java`: solid `windowBackgroundWhite` top and bottom
bars (8+44+8 dp) with the classic `header_shadow` / `header_shadow_reverse` hairlines, capsule backgrounds
stripped to flat circular `listSelector` ripples, containers transparent, send stays a flat theme-colored
circle (no elevation shadow). `RichEditorToolbar` (18 `key_glass_targetMainTabs` capsules) was left as-is —
its host `ChatAttachAlertRichLayout` is **dormant in 12.9** (only `showEditLatexSheet` is referenced);
re-skin it if upstream ever wires the attach-compose flow.

### Hazards hit (lessons)
- **`git checkout --theirs <file>` during the rebase takes the WHOLE classic file** — for semi-pinned
  files with one comment-only conflict this silently discarded the file's entire 12.9 delta.
  `ChatEditActivity` lost the community-creation entry that way; recovered with a 3-way
  `git merge-file` (classic ⟂ old-base ⟂ modern). FilterTabsView/ForkSettings survived only because their
  12.9 delta WAS classic's own upstreamed #79. Rule: after any wholesale `--theirs`, 3-way-merge the file
  against the two bases before accepting it.
- The rebase's merge engine anchored classic's #61 menu edits INSIDE the new 12.9 community branch of
  `DialogsActivity.showItemOptions` — the method had to be reassembled by hand (community early-return from
  modern + classic #61 body).
- Boxed-Boolean scan (12.8 lesson): **no new** `public Boolean` fields in 12.9 vs 12.8.5 — clean.
- `richEditorAvailable()` is `true` on `DEBUG_VERSION` and server-config-driven in release — same gate as
  the official app, not a #78-style debug trap.

## Modern 12.9.1 bump — fork features only, no new core (the `/classic-bump` run, 2026-07-31)

Base move `[Release] 12.9.0.` (`58a82d253`) → `[Release] 12.9.1.` (`555cb4546`, tag `12.9.1.0`; the
modern repo's `dev` points at the same commit). **The upstream snapshot did not move**: both bases sit
on `9bcf3d2769 update to 12.9.0 (6966)`, so there was **no new redesign to undo** — this bump only
crosses onto the fork's own newer work. Beware the naive commit count: forkgram force-pushes `dev`, so
`58a82d253..modern-12.9.1.0` reads 187 commits; comparing *subject sets* of the two fork stacks shows
the real delta is ~10 commits.

Tree delta 60 files / +3233 -893; jni churn was `tgnet` only (WebSocket transport) — native rebuilt,
~36 min for the APK, no ffmpeg/voip/rlottie churn.

New in the base: account selector + manual account reordering, Share Alert options menu, WebSocket
transport with relay domains, AV1 send fix, audio-player playing-state fix, and its own F-Droid build.

### What the fork picked up FROM classic
`StoriesUtilities` is now **code-identical** to classic's #87 rounded-ring segmentation (only our
explanatory comments differ), and `SharedConfig`'s granular `hideSensitive{Phone,Username,Bio,Id}`
matches classic's #85 API name for name. The modern "Let Hide Sensitive Data choose what it hides"
commit is the same feature squashed. Conflicts there were comment-only — resolved per hunk, never with
a wholesale `--theirs` (that would have dropped the file's whole new-base delta).

### Resolutions worth remembering
- **Fork API renamed PascalCase → camelCase** (`ForkDialogs.CreateDeleteAll*`, `AsCopy.TakeReplyToDraft`
  / `TakeReplyInputToDraft` / `GroupItemsIntoAlbum` / `PerformForwardFromMyName`, `ForkApi.TLRPCMessages`,
  `ForkUtils.HasPhotoOrDocument`, `ForkDialogs.CreateFieldAlert`). Pinned files kept calling the old
  names → 7 compile errors total. Grep for `\b(ForkUtils|ForkDialogs|ForkApi|AsCopy)\.[A-Z]` after a bump.
- `ShareAlert` (frozen) took the classic side wholesale, so the **new share options menu is not adopted**
  — but `UndoView` reads `ShareAlert.UndoInfo.deleted`, so that field was added to the pinned class as a
  compile-only stub (classic never sets it).
- `UserInfoActivity`: union resolution — the base's `listenReorder`/`allowReorder` (account reordering)
  kept **and** classic's flat `setSections(0, 0, false)`.
- `AppUpdater.kt` was refactored under us (`HttpTask` → `httpRequest`, `httpClient` → `connection`,
  `title/desc` → `TITLE/DESC`): took the new structure and re-applied the rebrand strings on top.
- Version: kept classic's own line (`12.9.5` / `6973`). The base carries `12.9.1` / `6967`, which is
  **below** classic's last release — taking it would break F-Droid's monotonic version code.

## Modern 12.9.2 bump — no new redesign, but the native build was restructured (the `/classic-bump` run, 2026-08-03)

Base move `[Release] 12.9.1.` (`555cb4546`, tag `12.9.1.0`) → `[Release] 12.9.2.` (`a2defcfde`, tag
`12.9.2.0`). Unlike the 12.9.1 bump the **upstream snapshot did move**, but only by a patch:
`aac1efcb83 update to 12.9.1 (6976)` → `b7561f0c64 update to 12.9.2 (6991)`. **No new redesign to undo**
— the pins auto-reverted nothing because there was nothing new to revert, and the whole reshim was
**3 fixes** (the smallest of any bump so far). Version: `12.9.9` / code `6992` = base (6991) + 1, next in
classic's own patch line after `12.9.8.0`.

Same force-push caveat as 12.9.1: `12.9.1.0..12.9.2.0` reads **243 commits** because `dev` is rebased.
By *subject set* the real delta is 4 fork commits (satellite networks, `tg://user` links, and the two
third-party ones below) plus the upstream patch.

### The real story: `jni/third_party/` and the merged native build

This bump's weight was in native build infrastructure, not UI:

- The third-party sources **moved into a `third_party/` subtree**: `jni/{dav1d,libvpx}` →
  `jni/third_party/{dav1d,libvpx}`, and a **new** `jni/third_party/ffmpeg` submodule replaced the old
  `jni/ffmpeg` one. Git detects these as renames, so classic's deliberate **downgraded pins survived the
  move intact** (`libvpx d168454e` = v1.15.2, `dav1d b546257f` = 1.5.3 — still below the base's v1.16.0 /
  1.5.4, as pinned since the original pipeline commit).
- The three per-library build scripts (`build_{dav1d,libvpx,ffmpeg}_clang.sh`) and `patch_ffmpeg.sh` were
  **deleted**, replaced by one merged script tracked at
  `jni/ffmpeg/build_ffmpeg_libvpx_dav1d_android_ndk27_merged.sh`, driven by `ANDROID_NDK_HOME` + `ABIS`.
  `jni/ffmpeg` is therefore no longer a submodule — it is a tracked dir whose per-ABI outputs are
  gitignored.
- **FFmpeg jumped n4.4.8 → n8.1.2**, which is why `avresample` disappeared from `jni/CMakeLists.txt`.
  CMake's imported-library paths flattened from `ffmpeg/build/${ABI}/lib/*.a` + `libvpx/build/...` +
  `dav1d/build/...` to a single `ffmpeg/${ABI}/*.a` (ffmpeg **plus** `libvpx.a` and `libdav1d.a`) with
  headers at `ffmpeg/include`. `video.c` was dropped from the native sources.

No manual native work was needed: the fork's own gradle prepare step rebuilt the merged libs from
classic's pinned submodules on the first compile. But the CMakeLists change invalidates the native cache,
so the APK took **~20 min** (full voip/webrtc rebuild), not the usual ~5.

**The F-Droid side needed nothing — by design.** The three deleted scripts *were* patched at build time,
but that patching lives in our own `prebuild_fdroid.sh` (which exists precisely so "a change to the jni
build scripts can never drift away from the sed that fixes them"), and the 12.9.2 base **deleted those
seds from it in the same commit that deleted the scripts** — classic inherited that automatically, since
`prebuild_fdroid.sh` is not a pinned file. What remains there (`build_boringssl.sh`, `prepare.py`) still
exists. The live fdroiddata recipe is likewise unaffected: since the `12.9.4.0` block its `prebuild:` has
been the one-liner `../prebuild_fdroid.sh 0 <id> <hash>`, and `AutoUpdateMode: Version` clones that shape
for each new tag.

> Note: `classic/fdroiddata/org.forkgram.classic.yml` in this repo is a **stale 12.7.3.0-era snapshot** of
> the recipe — it still carries the old inline `sed` list. It is a reference copy only; the real recipe
> (`fdroiddata/metadata/org.forkgram.classic.yml`, auto-updated from tags) is what builds. Don't diagnose
> F-Droid problems from the local copy — fetch the live one.

### The 3 reshims

1. `Utilities.blurBitmap(bitmap, radius, unpin, w, h, stride)` collapsed to `blurBitmap(bitmap, radius)`
   (the native side derives the rest). Pinned `ChatActivity` was the **only** stale call site — every
   other one in the tree already used the 2-arg form.
2. `BaseFragment.getEdgeToEdgeSupportMode()` — 12.9.2 replaced the boolean `isSupportEdgeToEdge()` with an
   `EdgeToEdgeSupportMode` enum and moved upstream's readers to it; `MainTabsActivity`,
   `PremiumPreviewFragment` and `QrActivity` override it. Added to the pinned class returning `NONE`.
   **Deliberately not cross-delegated** with `isSupportEdgeToEdge()`: 55 classes override the boolean and
   classic's only readers (`BottomSheet`, `StoryViewer`) read the boolean, so deriving one from the other
   would flip `MainTabsActivity` — which overrides *only* the enum, and overrode neither before the bump —
   to edge-to-edge and change classic's bottom-tabs layout. (Deriving in the other direction is the
   `StackOverflow` shape from the shim-delegation lesson.)
3. `DrawerLayoutContainer.setActionBarLayout(ActionBarLayout)` — no-op. `LaunchActivity` now calls it *in
   addition to* `setParentActionBarLayout()` (which it still calls, and which classic implements). Upstream
   caches the layout to route `parentDraw()`/`storyViewerAttached()`; classic's pinned `drawChild()` finds
   the `ActionBarLayout` among its own children, so there is nothing to store.

### Conflict resolutions

- 8 conflicts on the pipeline commit, all DESIGN_FROZEN: `ActionBar`, `ActionBarLayout`, `BaseFragment`,
  `DrawerLayoutContainer`, `ChatActivity`, `ProfileActivity` took the classic side; the redesign file
  `ChatActivityTopPanelLayout` stayed deleted. The `ActionBarLayout` hunks were all new-base-only
  machinery the 12.1.1 pin never had (`EdgeToEdgeSupportMode`, layers-layout rounded clip, predictive-back
  scaling).
- **`DialogCell` was NOT resolved with `--theirs`** (it is the documented semi-pin that takes upstream
  fixes). The base added `&& draftMessage.rich_message == null` to the empty-draft guard; without it the
  `formatRichMessage(...)` branch a few lines below is unreachable. Took the base hunk on the pipeline
  commit, then let Phase 2's own richer classic handling supersede it — classic renders rich drafts as
  "Draft: <text>" via its own `// forkgram-classic: 12.9 rich (article) drafts` branch.
- **`ForkSettingsActivity` needed a real hand-merge, not a side pick.** The base added the new
  `ID_SATELLITE_DATA_SAVING` row in the *same hunk* where classic drops the update-check-interval row.
  Its click handler had already auto-merged in, so taking the classic side wholesale would have left a
  **dead setting** (handler present, row unreachable). Kept the satellite row, dropped the interval row.
- Contamination audit (pinned files, pre-bump classic → new classic) came back to just 3 files, all
  benign: `ChatMessageCell` (un-pinned/wholesale-upstream by design), `DialogCell` (the rich-draft delta
  above), and one upstream `invalidate()` added to an `EmojiView` draw path.
- `README.md` kept the classic rewrite; `.github/workflows/tandroid.yml` stayed deleted.

Device-verified on the RMX3581 (v7a, SDK 30): solid continuous chat action bar, hamburger drawer + flat
folder tabs, legacy profile, classic emoji panel (bottom Emoji/GIFs/Stickers tabs), send works.
