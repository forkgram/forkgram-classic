# Modern UI cuts (Phase 1)

What was removed or rolled back from the modern Telegram-Android 12.7.3 codebase to restore the
12.1.1 design domain. Companion to `PLAN.md` §1 and `DESIGN_FROZEN.txt`.

## Files deleted outright (redesign-only — absent at the 12.1.1 baseline)

- `ui/Components/ChatActivityTopPanelLayout.java` — chat top panel with animated underlay.
- `ui/Components/ChatReplyContainer.java` — new reply container.
- `ui/Components/ChatAttachAlertEmojiLayout.java` — emoji page embedded in the attach menu.
- `DrawerLayoutAdapter` — holiday-icon branches collapsed (`msg_*_ny`, `_14`, `_hw` drawables are
  gone upstream).

## Files frozen at 12.1.1 (baseline `9cbf03332`)

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
- `ui/Cells/DialogCell.java` — pinned, but routinely takes selective perf patches (online-status
  cache, color cache, lazy `BoolAnimator`).

## Files restored from 12.1.1 (upstream had deleted them)

- Left drawer: `DrawerLayoutAdapter`, `DrawerActionCell`, `DrawerAddCell`, `DrawerProfileCell`,
  `DrawerUserCell`, `InviteTextCell`, `SideMenultItemAnimator`, `ActionBar/CustomNavigationBar`.
- Resources: `bottom_shadow.*`, `menu_shadow.*` (drawer shadows),
  `blockpanel.{mdpi,hdpi,xhdpi,xxhdpi}.png` (pinned-message bar fill).

## Pinned at first, rolled back to upstream during Phase 1

Each cascaded into more shimming than it was worth:

- `ActionBar/Theme.java` — 40+ files reference new color tokens.
- `Components/RecyclerListView.java` — `UniversalAdapter` and 7 other files need the new API
  (`forcedSections`, `TAG_NOT_SECTION`, `hasSections()`, `setSections(...)`).
- `LaunchActivity.java` — 12.1.1 imports the removed `com.google.firebase.appindexing`, and the init
  path drifted besides.
- `Cells/ChatMessageCell.java` — un-pinned 2026-05-27. The freeze also froze the poll v2 display
  subsystem (attached media, click-to-open, countdown, `hide_results_until_close`,
  `shuffle_answers`, recent voters, media-padding aware percent bar) for no redesign gain.

## Net user-visible effect

Gone: the new message bubbles, the new emoji/sticker/GIF picker, the chat top panel with animated
subtitle overlays, the redesigned profile (legacy 12.1.1 restored), the new reply container (legacy
in-input preview restored), the attach-menu emoji page. Opt-in only, behind
`UserConfig.mainTabsHiddenFork`: the dialog-list top panel and the floating bottom panel — the
default is the classic side drawer.

## Integration hazards — re-verify after each upstream merge

Classic-pinned components meeting modern un-pinned drawing and theming. Each was a real on-device
bug, and an upstream merge that re-touches the modern side can silently re-break them — the symptom
is a no-op, not a compile error. All verified on-device 2026-06-04 (realme C30).

- **The top fragment is `MainTabsActivity`, not `DialogsActivity`.** `DrawerLayoutContainer` drives
  the top fragment, so a drawer hook implemented in `DialogsActivity` must be forwarded from
  `MainTabsActivity` or it hits the `BaseFragment` default: `setProgressToDrawerOpened` (drawer-open
  parallax,
  #4 — without it the chat list stays static behind the drawer) and
  `isDrawerOpenSwipeEnabled` (folder-vs-drawer swipe guard, #1 — without it the drawer opens from
  anywhere instead of the left edge only).
- **Button-only search (#9):** the modern inline `FragmentSearchField` reserves
  `SEARCH_FIELD_HEIGHT` (48dp) in four places, each of which must be gated on
  `animatorSearchVisible` or an empty strip sits above the dialogs and the header overlaps the first
  row — list top padding in `ContentView.onMeasure`, `getActionBarFullHeight` (header clip and the
  #8 fill), the `filterTabsView`/`topPanelLayout` base in `ContentView.onLayout`, and
  `getMaxScrollYOffset`.
- **Drawer-open parallax (#4):** upstream perf-gates the slide+scale off on low-perf devices;
  classic ungates it for the drawer-open transition only (swipe-back keeps the upstream gate). The
  list must also fill the scale's bottom room, or the last dialog is cut with empty space under it.
- **Side drawer (#6, account switch):** modern deleted the drawer, so nothing re-themes or refreshes
  it. Re-theme in `LaunchActivity.didSetNewTheme` (and reset `DrawerProfileCell.switchingTheme`);
  refresh in `switchToAccount` via `drawerLayoutAdapter.notifyDataSetChanged()`, or the avatar lags
  and the selected-account checkmark never moves.
- **Dialogs header opacity (#8):** `ContentView.drawBlurRect` is forced to a flat opaque fill,
  matching the flat folder-tabs strip with no translucent seam.
- **Squared corners (#7):** `popup_fixed_alert4` → `alert2` at the menu call sites; `AlertDialog`
  uses `alert3` with the outline clip removed.
- **Edge-to-edge settings rows (#5):** `ThemeActivity` uses `listView.setSections(0, 0, false)`.
- **Attach sheet "Gallery" title (#3):** modern `ChatAttachAlertPhotoLayout` sets `flp.topMargin =
  statusBarHeight`, which the pinned 12.1.1 `ChatAttachAlert` action bar double-counts — drop it.
  Still open in #3: the Liquid-Glass send bar and the 2-tall camera tile.

## 12.8 bump — chat-header "islands" (`/classic-bump`, 2026-06-20)

Upstream 12.8 split the chat title bar into three floating rounded capsules. It is **ActionBar
"glass mode"**, not a new file: `ActionBar.setupGlass()` builds
`glassDrawable`/`glassDrawableBack`/`glassDrawableMenu` (radius-23 capsules),
`ActionBar.dispatchDraw` paints them, `ChatAvatarContainer .setGlassMode()` shrinks the title,
`ActionBarMenu.glassMode` insets the menu — activated only from `ChatActivity`, `ChatAttachAlert`
and `ChannelAdminLogActivity`.

The existing pins **revert it for free**: restoring `ChatActivity`, `ActionBar` and
`ActionBarLayout` to 12.1.1 removes every glass call. Un-pinned `ChannelAdminLogActivity` had its
own `setupGlass(...)` line, dropped in the rebase. Residue is no-op shims for the non-pinned 12.8
callers of the now-absent API: `ActionBar.checkAvatarContainerWidth(boolean)` (from
`ChatAvatarContainer`) and `ActionBarLayout.isLayersLayout()`/`isRightLayout()` (new abstract
`INavigationLayout` methods, return false) plus `setIsRightLayout() {}` from `LaunchActivity` — the
iPad split "layers" pane stays dormant. `setupGlass`/`setSearchFactor` ended with **0 callers**, so
they needed no shim at all.

Other 12.8 drift: `ChatMessageCell` swapped to the 12.8 version (the old one imported the removed
`FrameTickScheduler`); Instant-View 2.0 relocated the page-block TL classes
(`TLRPC.TL_pageBlockList` → `TL_iv.pageBlockList` — `TL_` dropped, lowercased) in `ProfileActivity`;
`ChatActivity` call sites fixed (`SearchTagsList`/`ChatSearchTabs` ctors, `HashtagHistoryView`
callbacks, `MotionBackgroundDrawable.updateAnimation()`, `isWebBrowserOpenInApp`); new IV2.0 hooks
shimmed onto the pins (`ChatActivity.PROGRESS_FULL_ARTICLE`,
`ChatAttachAlert.showSendButtonOnly`/`setLocationPicker`).

Base: upstream 12.7.3 → 12.8.1 (6916), modern release tag `12.8.2.0` (`39883086c`). jni delta 2
files / 16 lines — native cache warm, ~5-min builds.

## 12.8.2.0 → 12.8.4.0 (`/classic-bump`, 2026-07-01)

**No new redesign.** Both tags sit on the identical upstream snapshot `update to 12.8.1 (6916)`
(`9b50143d88`); the modern fork only added feature commits on the same core. Nothing to revert, **0
reshims**, library clean on the first try. Base delta 27 files / ~700 lines, zero jni churn.

Conflicts (stack rebased `--onto modern-12.8.4.0`):

- **4 pinned files took `--theirs`:** `ChatActivity`, `ChatAttachAlert`, `EmojiView`,
  `ProfileActivity`. `ChatActivityEnterView` and `PhotoViewer` auto-merged with the new base's
  feature hunks kept.
- **`DialogsActivity`** — the new base re-added a "New Message" item and a "Show/Hide bottom tabs"
  toggle to the overflow menu, exactly what classic **#61** moved to the drawer and Fork Settings;
  kept classic. The benign `MainTabsHiddenHint` toast and `hideInAppHints` gate auto-merged, kept.
- **`ForkSettingsActivity`** — union: classic's `updateCheckIntervalRow = -1` (F-Droid ships no
  in-app updater) **and** the base's `disableTabletModeRow`.
- **Version `12.8.9` / code `6925`** — classic runs its own patch line and was already ahead of the
  base (`12.8.4`/`6919`), so code = last classic + 1. Base + 1 would regress and break F-Droid
  monotonicity.

### Feature-parity re-port onto the pins (owner chose FULL parity)

`--theirs` on the four pinned files dropped the new-in-12.8.4 fork features that had landed inside
them. They are pure logic, so they were re-applied on top of the 12.1.1 pins — every anchor still
exists, because these subsystems predate the redesign:

- `disableLinkPreviewByDefault` — `ChatActivity` search-links gate (1 site).
- `hideSavedMessagesTags` — `savedMessagesTagHint` + `actionBarSearchTags` (2 sites).
- **Show archived sticker packs** (+81) — `EmojiView`: 2 fields,
  `requestArchivedStickerSetIfNeeded`, the `showArchivedStickers` block in `updateStickerTabs`, the
  `archivedStickersDidLoad` observer, and a placeholder→full-set refresh in the
  `groupStickersDidLoad` branch. **Adapted:** modern used `stickerSetId`/`stickerSet` locals,
  classic's `groupStickersDidLoad` carries `id=args[0]`, `set=args[1]`.
- Long-press-to-copy name — `ProfileActivity.nameTextView[1]`.
- Supergroup custom emoji in captions — `ChatAttachAlert`
  `commentTextView`/`topCommentTextView.setChatInfo` (both `EditTextEmoji`).

Deliberately not ported: the `hideAiEditor`-on-media guard (**N/A** — classic's attach sheet has no
AI button; #33 added one only to the input bar); the forward-menu-keyboard `suppressLayout` scroll
fix (anchor only partial, relies on the modern mechanism); the report-mode `sortIds` crash guard
(that selection path does not exist in the pinned `ChatActivity`).

## 12.8.4.0 → 12.8.5.0 (`/classic-bump`, 2026-07-14)

**No new redesign again** — same upstream snapshot `update to 12.8.1 (6916)`, **0 reshims**, clean
first try. Base delta 65 files / ~3.2k lines, zero jni churn.

- **2 pinned files took `--theirs`:** `ChatActivity`, `ProfileActivity`, with their new-in-12.8.5
  features re-ported below.
- **`ChatMessageCell`** is un-pinned, so `git checkout modern-12.8.5.0 --` it: the rebase leaves it
  at the old base, here missing the `ForkOfflineTranscribe.isActive()` hook.
- **`ForkSettingsActivity` was rewritten upstream** (row-int `RecyclerListView` → declarative
  `UItem`/`UniversalAdapter` with `ID_*` constants, `highlight()` and a settings-search index). A
  textual merge is meaningless: took the new screen and re-applied classic's edits semantically —
  the #61 "Show bottom tabs" toggle as `ID_SHOW_BOTTOM_TABS` over the same
  `UserConfig.mainTabsHiddenFork`, the dropped update-check-interval row, and the "Forkgram Classic"
  default custom title. Design-safe: the new screen builds a plain `UniversalRecyclerView` with no
  `setSections(...)`, so no rounded-card insets (cf. #13).
- **CI** `fd.yml` / `tandroid.yml`: modify/delete → stay deleted.
- **Version `12.8.13` / code `6929`** = last classic + 1. Base + 1 (`6921`) would regress below the
  last release.
- **`LocationActivity` lost classic's patch set and that is not a loss** — the modern fork adopted
  classic's #73 fix (`zoomToBoundingBoxSafe`, the osmdroid live-location hang), so classic's version
  now equals the base.

### Feature-parity re-port onto the pins

- **Configurable formatting menu** (`FormattingMenu`) → `ChatActivity.fillActionModeMenu`. Without
  it the new Fork Settings entry is a **dead setting**: the user reorders items and the pinned
  12.1.1 builder ignores them. The "already filled" guard had to become `hasFormattingMenuItems()` —
  the old `findItem(menu_bold) != null` breaks once bold itself can be hidden. Two intended side
  effects: the fork's `menu_date` item now appears in classic (its handler already lived in
  `EditTextCaption`), and `includeLinks` is honoured instead of ignored (only `PeerStoriesView`
  passes it as a variable — live stories legitimately suppress "Create link").
- **Fork settings-search index** (56 entries) → `ProfileActivity.onCreateSearchArray`, adapted from
  the base's static `f.presentFragment(...)` form to classic's instance form, minus the
  `ID_UPDATE_CHECK_INTERVAL` entry, plus a classic-only entry for #61. Ids 1000+ do not collide with
  classic's (max 914).

Not ported, both `ChatActivity`: `actionBar.setGlassAvatarSquare(...)` (**N/A**, no glass surface)
and the base's fix for the extract-media button overlapping the reply text — that is the modern take
on classic's **#77**, written against `ChatReplyContainer`, which classic deletes; classic keeps its
own fix.

## 12.9 bump — Communities, rich (article) editor, ephemeral bot replies (`/classic-bump`, 2026-07-16)

Base `[Release] 12.8.5.` (`4e3482138`) → `[Release] 12.9.0.` (`f46f7b867`, taken from the modern
repo's `dev` — **no release tag existed yet**). Upstream snapshot 12.8.3 → **12.9.0 (6966)**
(`9bcf3d2769`); jni churn was `tgnet/ApiScheme` only (+190 LoC). Version `12.9.0` / code `6967` =
base + 1.

### Redesign that auto-reverted with the pins

- **Liquid-glass surfaces** (`iBlur3FactoryLiquidGlass`, blurred top panels, `setDefaultRadiusDp`
  rounded-card panels in `DialogsActivity`) — classic keeps flat solid panels.
- `menu.setTranslationX(-dp(5))` + `outline_header_search` on the dialogs action bar (classic keeps
  `msg_search`, #66); `ViewPagerFixed.SELECTOR_TYPE_BUBBLE_STYLE` search tabs (classic keeps style
  `0`); `popup_fixed_alert4` preview-menu chrome (classic keeps `alert2`). 12.9 also commented out
  the dialogs `inPreviewMode` avatar header — adopted, dead code either way.
- The 12.9 `DialogCell` avatar radius dp(28) → dp(26) — **rejected**, classic keeps dp(28).

### New no-op shims on the pinned classes

- `ActionBar.setupGlass(factory, colorProvider)` and `setGlassOnlyBack()` — the new Community
  screens request a glass bar and inherit the flat one.
- `ActionBar.checkMenuItemsWidth()` and `setAdditionalTextLeft(int)` — no capsule, no offset.
- Reverse shims for symbols the core removed: `SharedConfig.noStatusBar`,
  `AndroidUtilities.LIGHT/DARK_STATUS_BAR_OVERLAY` and the 3-arg `setLightStatusBar(Window,…)`,
  `ChatAvatarContainer.setTitleExpand(boolean)`, and the `msg_ton`/`menu_my_ton` drawables (12.9
  renamed them to `outline_gram_24` and deleted the assets; pinned screens keep the classic glyphs).
  `TLRPC.TL_botCommand` became abstract `TLRPC.BotCommand` (2 pinned call sites).
  `BaseFragment.set/getBulletinDelegate` — 12.9 Bulletin stores its delegate on the fragment.

### FULL-parity re-ports onto the pins

- **Communities** — `DialogCell`: `TL_dialogCommunity` unread counts, `formatCommunityDialogNames()`
  previews, `insideCommunityList*` / `isHiddenInCommunity` state, the eye-off icon in the mute slot
  (`dialogs_hiddenDrawable`), swipe-to-ungroup, the community-arrow avatar badge,
  `setCustomMessageWithoutRebuild`, `isDialogCommunity()`. **Classic design choice: the community
  row keeps a standard circular avatar** — the modern rounded-square over the stacked-cards backdrop
  (`drawCommunityCardDrawable`) was not ported. `ChatActivity`: header-avatar tap opens
  `CommunitySheet` for linked chats, eye-off title icon, `TL_messageActionChangeCommunity` menu
  gate. `ProfileActivity`: a classic **list row** (`CommunityLinkView`, `linkedCommunityRow`) opens
  the sheet; the modern avatar arrow-badge overlay was not ported. `DialogsActivity` kept all merged
  community machinery and took the 12.9 community branch in its top-right menu while keeping the #61
  cuts. The community-creation entry lives in un-pinned `ChatEditActivity`, restored after the
  rebase clobbered it (see hazards).
- **Rich (article) editor** — `ChatActivityEnterView`: `richButton` top-right (the classic AI button
  moved top-left, matching the modern pair), rich-draft preview chip and delete button, draft
  save/load/clear, `sendRichDraft` via `prepareSendingArticle`, non-premium conversion sheet,
  `SendButton` lock badge and 4-arg ctor. `ChatActivity`: `getDraftThreadId()`, draft-preserving
  `saveDraft`, rich-draft apply, article edit → `RichEditor`, copy-as-HTML (`RichMediaClipboard`),
  article translate (`TranslateAlert2` rich overload), full-article loader (`loadFullRichMessage`),
  article-photo viewer (`ChatArticlePageBlocksAdapter`/`Provider` with the classic clip budget),
  checkbox-toggle delegate. `ChatAttachAlert`: real `setLocationPicker()` (upgraded from the 12.8
  no-op), `enablePollAttachMode(int)`, `openAttachLayoutForType`, `get/setTypeButtonsHidden`
  (classic 84dp button row + `shadow`), public `currentPanTranslationY`, `disableBottomFade()`.
  `EmojiView.hideBottomTabContainerBackground()`. `RichEditor.animateFrom` degrades to a standard
  fragment transition — classic has no input island.
- **Ephemeral bot replies** — `ChatActivity`: `MentionsAdapter.EphemeralCommand` click (sends with
  `ephemeralReceiverBotId`), long-press insert, and the no-swipe-reply-on-own-ephemeral gate. The
  enter-view outline send-button morph was **not** ported (island-animator machinery); sending
  works, the visual is skipped.
- **GIF search (350M)** is server-side — the pinned GIF panel queries the new index unchanged.

### Classic re-skin of the new RichEditor screen

The 12.9 editor floats capsule buttons (back, undo-redo, emoji, AI, block-type row, attach, send)
over white gradients. `RichEditor.java` now gets solid `windowBackgroundWhite` top and bottom bars
(8+44+8 dp) with the classic `header_shadow` / `header_shadow_reverse` hairlines, capsule
backgrounds stripped to flat circular `listSelector` ripples, transparent containers, and a flat
theme-colored send circle with no elevation. `RichEditorToolbar` (18 `key_glass_targetMainTabs`
capsules) was left alone: its host `ChatAttachAlertRichLayout` is dormant in 12.9 (only
`showEditLatexSheet` is referenced). Re-skin it if upstream ever wires the attach-compose flow.

### Hazards hit

- **`git checkout --theirs <file>` during a rebase takes the whole classic file.** For a semi-pinned
  file with one comment-only conflict this silently discards the file's entire new-base delta —
  `ChatEditActivity` lost the community-creation entry that way, recovered with a 3-way `git
  merge-file` (classic ⟂ old base ⟂ modern). `FilterTabsView` and `ForkSettingsActivity` survived
  only because their 12.9 delta *was* classic's own upstreamed #79. Rule: after any wholesale
  `--theirs`, 3-way-merge the file against both bases before accepting it.
- The merge engine anchored classic's #61 menu edits **inside** the new community branch of
  `DialogsActivity.showItemOptions`; the method had to be reassembled by hand.
- Boxed-`Boolean` scan (the 12.8 lesson): no new `public Boolean` fields in 12.9 — clean.
- `richEditorAvailable()` is `true` on `DEBUG_VERSION` and server-driven in release — the same gate
  as the official app, not a #78-style debug trap.

## Modern 12.9.1 bump — fork features only, no new core (`/classic-bump`, 2026-07-31)

Base `[Release] 12.9.0.` (`58a82d253`) → `[Release] 12.9.1.` (`555cb4546`, tag `12.9.1.0`). **The
upstream snapshot did not move** — both sit on `9bcf3d2769 update to 12.9.0 (6966)`, so there was no
new redesign to undo. Beware the naive commit count: forkgram force-pushes `dev`, so the range reads
187 commits while the *subject sets* differ by ~10. Tree delta 60 files / +3233 −893; jni churn was
`tgnet` only (WebSocket transport), ~36 min for the APK.

New in the base: account selector and manual reordering, Share Alert options menu, WebSocket
transport with relay domains, an AV1 send fix, an audio-player playing-state fix, and its own
F-Droid build.

### What the fork picked up from classic

`StoriesUtilities` is now code-identical to classic's #87 rounded-ring segmentation (only the
comments differ), and `SharedConfig`'s granular `hideSensitive{Phone,Username,Bio,Id}` matches
classic's #85 — the modern "let Hide Sensitive Data choose what it hides" commit is the same feature
squashed. Conflicts there were comment-only, resolved per hunk, never wholesale.

### Resolutions worth remembering

- **The fork API was renamed PascalCase → camelCase** (`ForkDialogs.CreateDeleteAll*`,
  `AsCopy.TakeReplyToDraft` / `TakeReplyInputToDraft` / `GroupItemsIntoAlbum` /
  `PerformForwardFromMyName`, `ForkApi.TLRPCMessages`, `ForkUtils.HasPhotoOrDocument`,
  `ForkDialogs.CreateFieldAlert`). Pinned files kept calling the old names — 7 compile errors. After
  a bump, grep `\b(ForkUtils|ForkDialogs|ForkApi|AsCopy)\.[A-Z]`.
- `ShareAlert` (frozen) took the classic side wholesale, so the new share options menu is not
  adopted — but `UndoView` reads `ShareAlert.UndoInfo.deleted`, so that field was added to the
  pinned class as a compile-only stub.
- `UserInfoActivity`: union — the base's `listenReorder`/`allowReorder` **and** classic's flat
  `setSections(0, 0, false)`.
- `AppUpdater.kt` was refactored under us (`HttpTask` → `httpRequest`, `httpClient` → `connection`,
  `title/desc` → `TITLE/DESC`): took the new structure, re-applied the rebrand strings.
- Version stays on classic's line (`12.9.5` / `6973`); the base's `12.9.1` / `6967` is below
  classic's last release.

## Modern 12.9.2 bump — no new redesign, but the native build was restructured (`/classic-bump`, 2026-08-03)

Base `12.9.1.0` (`555cb4546`) → `12.9.2.0` (`a2defcfde`). The upstream snapshot moved by a patch,
`aac1efcb83 update to 12.9.1 (6976)` → `b7561f0c64 update to 12.9.2 (6991)`, with nothing new to
revert and the smallest reshim of any bump so far — **3 fixes**. Version `12.9.9` / code `6992` =
base + 1. Same force-push caveat as 12.9.1: the range reads 243 commits, the real delta by subject
set is 4 fork commits plus the upstream patch.

### The weight was in `jni/third_party/`

- The third-party sources **moved into a `third_party/` subtree**: `jni/{dav1d,libvpx}` →
  `jni/third_party/{dav1d,libvpx}`, and a new `jni/third_party/ffmpeg` submodule replaced
  `jni/ffmpeg`. Git sees renames, so classic's deliberate **downgraded pins survived intact**
  (`libvpx d168454e` = v1.15.2, `dav1d b546257f` = 1.5.3, still below the base's v1.16.0 / 1.5.4).
- The three per-library scripts (`build_{dav1d,libvpx,ffmpeg}_clang.sh`) and `patch_ffmpeg.sh` were
  deleted for one merged `jni/ffmpeg/build_ffmpeg_libvpx_dav1d_android_ndk27_merged.sh` driven by
  `ANDROID_NDK_HOME` + `ABIS`. `jni/ffmpeg` is no longer a submodule but a tracked dir with
  gitignored per-ABI outputs.
- **FFmpeg jumped n4.4.8 → n8.1.2**, which is why `avresample` left `jni/CMakeLists.txt`. The
  imported-library paths flattened to a single `ffmpeg/${ABI}/*.a` (ffmpeg plus `libvpx.a` and
  `libdav1d.a`) with headers at `ffmpeg/include`; `video.c` was dropped from the native sources.

No manual native work was needed — the gradle prepare step rebuilt the merged libs from classic's
pinned submodules on the first compile — but the CMakeLists change invalidates the native cache, so
the APK took ~20 min instead of ~5.

**F-Droid needed nothing, by design.** The three deleted scripts were patched at build time, but
that patching lives in our own `prebuild_fdroid.sh`, and the 12.9.2 base deleted those seds in the
same commit that deleted the scripts — classic inherited it, since the file is not pinned. The live
recipe is unaffected: since the `12.9.4.0` block its `prebuild:` is the one-liner
`../prebuild_fdroid.sh 0 <id> <hash>`, and `AutoUpdateMode: Version` clones that shape per tag.

> `classic/fdroiddata/org.forkgram.classic.yml` in this repo is a **stale
> 12.7.3.0-era snapshot** carrying the old inline `sed` list. It is a reference
> copy; the real recipe (`fdroiddata/metadata/org.forkgram.classic.yml`,
> auto-updated from tags) is what builds. Never diagnose F-Droid from the local
> copy — fetch the live one.

### The 3 reshims

1. `Utilities.blurBitmap(bitmap, radius, unpin, w, h, stride)` collapsed to `blurBitmap(bitmap,
   radius)`. Pinned `ChatActivity` was the only stale call site.
2. `BaseFragment.getEdgeToEdgeSupportMode()` — 12.9.2 replaced the boolean `isSupportEdgeToEdge()`
   with an enum and moved upstream's readers to it; `MainTabsActivity`, `PremiumPreviewFragment` and
   `QrActivity` override it. Added to the pinned class returning `NONE`, and **deliberately not
   cross-delegated** with the boolean: 55 classes override the boolean, and classic's only readers
   (`BottomSheet`, `StoryViewer`) read it, so deriving one from the other would flip
   `MainTabsActivity` — which overrides only the enum, and overrode neither before the bump — to
   edge-to-edge and change the bottom-tabs layout. (The other direction is the `StackOverflow` shape
   from the shim-delegation lesson.)
3. `DrawerLayoutContainer.setActionBarLayout(ActionBarLayout)` — no-op. `LaunchActivity` now calls
   it in addition to `setParentActionBarLayout()`. Upstream caches the layout to route
   `parentDraw()`/`storyViewerAttached()`; classic's pinned `drawChild()` finds the
   `ActionBarLayout` among its own children, so there is nothing to store.

### Conflict resolutions

- 8 conflicts on the pipeline commit, all DESIGN_FROZEN: `ActionBar`, `ActionBarLayout`,
  `BaseFragment`, `DrawerLayoutContainer`, `ChatActivity`, `ProfileActivity` took the classic side;
  `ChatActivityTopPanelLayout` stayed deleted. The `ActionBarLayout` hunks were all new-base
  machinery the pin never had (`EdgeToEdgeSupportMode`, layers-layout rounded clip, predictive-back
  scaling).
- **`DialogCell` was not resolved with `--theirs`** — it is the documented semi-pin that takes
  upstream fixes. The base added `&& draftMessage.rich_message == null` to the empty-draft guard;
  without it the `formatRichMessage(...)` branch below is unreachable. Took the base hunk, then let
  Phase 2's richer classic handling supersede it.
- **`ForkSettingsActivity` needed a real hand-merge.** The base added `ID_SATELLITE_DATA_SAVING` in
  the *same hunk* where classic drops the update-check-interval row, and its click handler had
  already auto-merged — taking the classic side wholesale would have left a **dead setting**. Kept
  the satellite row, dropped the interval row.
- Contamination audit came back to 3 benign files: `ChatMessageCell` (un-pinned by design),
  `DialogCell` (the rich-draft delta) and one upstream `invalidate()` in an `EmojiView` draw path.

Device-verified on RMX3581 (v7a, SDK 30).

## Modern `dev` re-base — no new release, one fork commit (`/classic-bump`, 2026-08-06)

Not a version bump: the newest modern tag was still `12.9.2.0`, so this moved classic from the tag
commit (`a2defcfde`) onto the `dev` tip `2b2c5260b` ("Bound no-frame decode path so dead decoder
cannot pin core."). The upstream snapshot did not move — nothing to revert.

Measure such a delta by **subject sets**, never by commit count: both sides carry 101 commits after
the merge-base `777b9dc5ef`, and the sets differ by exactly two entries — `dev` gained the
decode-path commit and lacks `[Release] 12.9.2.`, which is an **empty marker commit** (0 files).

The other 16 changed files are not new commits. The force-push **amended** several existing fork
commits (UnifiedPush, hidden accounts and stealth mode, WebSocket transport, satellite networks),
which is why a subject-identical history still yields a real content delta: `AppStartReceiver`,
`PushListenerController`, `SharedConfig`, `UnifiedPushService`, `Dialogs.kt`,
`ForkSettingsActivity`, `NotificationsSettingsActivity`, `ProfileActivity` and strings in 10
locales. `AnimatedFileDrawable`'s decode-path guard merged silently — classic already carried the
byte-identical patch.

- **The one conflict: pinned `ProfileActivity`.** The settings-search array collided — the base's
  62-line block in `f.presentFragment(…)` style against classic's 3-line 12.1.1 form of the
  402/403/404 help entries. Resolved `--theirs` per the DESIGN_FROZEN rule; the fork search block is
  re-added by the Phase-2 commit in classic's own style and applied cleanly afterwards.
- **The one reshim: the UnifiedPush setting was split in two.**
  `ForkSettingsActivity.ID_DISABLE_UNIFIED_PUSH` became `ID_UNIFIED_PUSH` (the distributor/gateway
  screen) while the on/off switch moved to `NotificationsSettingsActivity#disableUnifiedPushRow`,
  reachable via a new chainable `highlightUnifiedPush()`. Both screens are un-pinned and rode the
  base in, but the **pinned** `ProfileActivity` is the reader of both — the classic dead-setting
  shape. Ported the base's hunk in classic's call style: entry 1051 retargeted to
  `ID_UNIFIED_PUSH`/`R.string.UnifiedPush`, plus a new entry 1057 pointing at the notifications row.

Library compiled 0 errors after that single fix; the contamination audit came back to exactly the 17
files the base changed.

## Modern 12.9.7 bump — a real upstream minor (12.10), no new chrome (`/classic-bump`, 2026-08-23)

Base: the `dev` tip `2b2c5260b` → `[Release] 12.9.7.` (`3367cea91`, tag `12.9.7.0`). The **upstream
snapshot jumped a whole minor**: `b7561f0c6 update to 12.9.2 (6991)` → `4e1a61eca update to 12.10.0
(7031)`, 326 java files, +18757/−19022. Version `12.10.0` / code `7032` = base + 1.

### Measure before hunting for a redesign

The 12.8 run's headline was reverting glass mode; this one has no equivalent. The upstream delta on
the **pinned chrome** is small and behavioural — `ActionBar` +29/−3, `ActionBarLayout` +14/−14,
`EmojiView` +1/−1, `ShareAlert` and `DrawerLayoutContainer` unchanged. Where 12.10 changed pixels is
the **message-bubble layer**: new `ui/ActionBar/MessageDrawable` (extracted from
`Theme.MessageDrawable`), `Components/TornEdge`, `Components/QuoteCollapseButton`,
`Components/UnsupportedBlockDrawable`, `Cells/ChatMessageUnsupportedCell`, and `ChatMessageCell`
itself (+315/−79). **Classic keeps `ChatMessageCell` un-pinned** for poll v2, so it *adopts* that
layer rather than reverting it — device-checked, the bubbles still read as classic. The other new
surface is a feature, not a redesign: `Gifts/GiftMessageBottomSheet` with
`GiftMessageDrawable`/`GiftMessageView`/ `StarGiftUniqueActionView`, plus more `iv/Rich*` files. So
the whole run was re-shimming the pins against a drifted core.

### The reshims

1. **`SharedConfig.noStatusBar` deleted upstream** → re-added as `public static final boolean =
   true`; pinned `ActionBar`, `ActionBarLayout` and `ProfileActivity` still read it.
2. **Classes moved out of `androidx.recyclerview.widget` into `org.telegram.ui.recyclerview`** —
   `LinearSmoothScrollerCustom` (pinned `EmojiView`, `ProfileActivity`) and `ChatListItemAnimator`
   (pinned `ChatActivityEnterView`). Import-only, but it must flow into the pins.
3. **`Theme.MessageDrawable` → top-level `ui/ActionBar/MessageDrawable`** — `ActionBarLayout`'s two
   crossfade fields and two `INavigationLayout` overrides retyped.
4. **Send parameters consolidated** (the big one). 12.10 replaced the `quickReplyShortcut` +
   `getQuickReplyId()` argument pair threaded through every send call with a single
   `SendMessageChatArguments`. Added `ChatActivity.getMessageChatSendParams()` mirroring the 12.10
   implementation **minus its `MODE_WELCOME_MESSAGES` branch** (the pinned chat screen never hosts
   that mode), and rewrote every pinned call site in `ChatActivity`/`ChatActivityEnterView` to pass
   it — including `MediaController.startRecording`/`prepareResumedRecording` and
   `SendMessagesHelper.SendMessageParams.sendMessageChatArguments`.
5. **`prepareSendingPhoto`/`prepareSendingVideo` grew a `scheduleRepeatPeriod`** between
   `scheduleDate` and `forceDocument`/`mode`. Padded with `0` at the six pinned call sites — safe
   here because classic ships no repeat-schedule UI, but this is exactly the
   widened-signature/padded-call shape: check what the literal means before padding.
6. **`MODE_WELCOME_MESSAGES = 9`** added to the pinned `ChatActivity`. Classic never enters the
   mode; the constant exists because un-pinned
   `MessagesController`/`MessagesStorage`/`MessageObject`/`ChatMessageCell`/ `ChatAvatarContainer`
   read it off `ChatActivity`.
7. **TL keyboard schema rewritten**: `TLRPC.KeyboardButton` → `TL_keyboard.KeyboardButtonProto`, the
   button *kind* moved from the subclass into a `type` field queried via
   `TLKeyboardHelper.getType/isType`. `ChatActivityEnterView.didPressedBotButton` was ported from
   the 12.10 implementation — behaviour, not layout, so the design pin does not apply. One more TL
   relocation to remember, alongside 12.8's `TLRPC.PageBlock` → `TL_iv`.
8. **`BaseFragment.createView` demoted to `protected`** behind a new `performCreateView(Context)`
   trace wrapper; the pinned file takes the new form and needs the `BuildConfig` import.
9. **Two `GiftMessageBottomSheet` helpers** on the pinned `ChatActivityEnterView`:
   `setCustomWindowView(View)` (anchor popups to the sheet, not the window) and `static
   disableNewLines(EditText)`.

### Audit notes

- **The `[classic] #` marker count fell 641 → 640 and nothing was lost.** The missing one is
  `SharedConfig`'s #109 UnifiedPush-gateway comment: the fork upstreamed classic's fix, so
  `unifiedPushGatewayMigrated` now ships in the base with the tag stripped. Same shape as 12.9's
  #79. Diff the *logic*, not the marker, before calling a rebase lossy.
- The contamination audit came back to exactly the reshims above; `ChatAttachAlert`,
  `ChatAttachAlertPollLayout`, `ShareAlert`, `ActionBar` and `DrawerLayoutContainer` are
  byte-identical to pre-bump.
- Boxed-`Boolean` scan: no new `public Boolean` fields in 12.10 — clean.
- **Native:** `jni/**` reads 816 files / −298138 lines because upstream slimmed the vendored
  `opus`/`mozjpeg`/`rlottie`/`exoplayer` sources. `TMessagesProj/jni/dav1d` (20M) and `jni/libvpx`
  (39M) are untracked build-time leftovers in the tree — expected.
- Build: library `compileDebugJavaWithJavac` 0 errors; `:TMessagesProj_App:assembleAfatDebug
  -PF_DROID=1 -Parmeabi-v7a` succeeded in 12m09s, native cache warm despite the jni churn.
- Device-verified on RMX3581 (`12.10.0.0`, code `703209`), including the Emoji/GIFs/Stickers panel
  scrolling without the #74 vanish.

## Modern 12.10.3 bump — no new redesign, zero reshims (`/classic-bump`, 2026-09-10)

Base `[Release] 12.9.7.` (`3367cea91`) → `[F-Droid] 12.10.3.` (`d37ded1ba`, tag `12.10.3.0`).
Upstream snapshot moved to **12.10.1 (7038)**, a patch-level step with no new chrome to undo.
Version `12.10.3` / code `7041` = base + 1.

**The reshim climb did not happen.** `:TMessagesProj:compileDebugJavaWithJavac` came back **0 errors
on the first attempt** — the first bump in the series to need no shim. The glass API 12.8 introduced
still has live callers (`ChatAvatarContainer` → `checkAvatarContainerWidth`, the two community
activities → `setupGlass`), but the earlier no-op shims already cover them.

### Conflict resolutions (5 commits, 11 files)

- **Pinned, take classic** — `ActionBar` (5 hunks:
  `setupGlass`/`glassDrawable*`/`searchFactor`/`checkAvatarContainerWidth`), `ChatActivity` (4
  hunks: `setupGlass` wiring, input-island containers, `topPanelLayout`, `didPressStreamingStop`),
  `ChatActivityEnterView` (send-button slide, `sendButtonBlockedByTypingView`), `DialogCell`.
- **`DialogCell` looked like a genuine feature and is not.** The base's hunk is community-aware
  swipe-mute (`isDialogCommunity() ? isCommunityMuted(...) : dialogMuted`), and taking it does not
  compile: the pinned `DialogCell` carries none of the community fields (12 occurrences in the base,
  0 in the pin). Verify the symbols exist before "rescuing" a hunk.
- **`prebuild_fdroid.sh` — take the BASE, not classic.** Both sides carry the same #113
  Rust-toolchain fix, but the jni restructure renamed the script: classic's `./tlottie_lib/build.sh`
  is now `./prebuild/build_tlottie.sh` (and the `git checkout --` guard moved with it). Keeping the
  classic side would have re-armed #113 — the guard exits 1 and the F-Droid build dies, exactly the
  12.10.1 bad release. Took the base's paths and re-added the `# [classic] #113:` tag.
- **`NotificationsService` — base's code, classic's hunk re-applied.** The base replaced the inline
  foreground block with `moveToForeground()`, upstream's fix for the `RemoteServiceException:
  Context.startForegroundService() did not then call Service.startForeground()` crash — worth having
  on low-RAM devices. But its `getNotification()` is back on the hardcoded
  `R.drawable.notification`, so #54 (icon follows the selected app icon) had to be re-applied: base
  + the `LauncherIconController` import + the #54 line + two rebranded log tags.
- **`ApplicationLoader`** — base's `stopPushService()` helper, classic's rebranded log tag.
  **`FilterTabsView`** — base's declaration form, classic's
  #79 marker re-attached.
- **`.github/workflows/tandroid.yml`** — modify/delete; the deletion stands.

### Native

`jni/**` reads 383 files / −241719 lines: upstream moved the vendored `third_party` sources to
submodules, added `third_party/openh264` and `third_party/libyuv`, and renamed `jni/tlottie_lib` →
`jni/prebuild`. All submodules were already checked out, so the cache stayed warm
(`assembleAfatDebug` succeeded in 4m56s). `jni/dav1d`, `jni/libvpx` and now `jni/tlottie_lib` sit in
the tree as untracked leftovers of the old layout; nothing reads them any more (`prepare.py:420`
points at `./prebuild/build_tlottie.sh`).

### Audit notes

- **The `[classic] #` marker count fell 647 → 643 and nothing was lost.** All four lines are #115 in
  `OfflineLanguageDetector` / `TranslateController`: the fork took classic's fix upstream (it ships
  inside `[TF][KILL] Google Translate without MLKit`) and its repo bans comments, so the tags are
  stripped. The logic is verified present in the base — `containsWord` / `isWordCharacter` /
  `NON_SPACING_MARK`, the Nepali and Marathi markers, and a live `checkLanguage` — so the classic
  #115 commit was dropped during the rebase (`git rebase --skip`) rather than re-applied. Third time
  this shape appears, after 12.9's #79 and 12.10's #109.
- The uncommitted #97 work found in the tree was committed **before** the rebase, so it would ride
  the stack rather than be stranded in a stash against a pinned file the rebase rewrites.
- Device-verified on RMX3581 (`12.10.3.0`, code `7041`): no crashes across dialogs, chat, drawer and
  profile.
- **Not caused by this bump:** the optional bottom tab bar draws as a floating rounded capsule.
  `MainTabsLayout.java` is byte-identical pre- and post-bump and the base never touched it —
  pre-existing classic-fidelity debt, same family as #59 / #61.
