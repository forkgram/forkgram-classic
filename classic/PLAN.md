# Forkgram Classic — plan for preserving the old design

> Local working document. Not intended for commit to `master`. If you decide to keep it in the repo — add it to `.gitignore`, or store it in a separate branch. This is a working draft and a discussion anchor, not a final architecture.

---

## 0. Context and goals

**What we have:**
- A fork of `DrKLO/Telegram` at `/home/h/src/forkgram-classic` (origin: `github.com/forkgram/forkgram-classic`).
- Working branch — **`dev`**. All Forkgram work lives here.
- Reference branch — `master`. Holds a sequence of upstream snapshots (one commit per upstream version, `update to X.Y.Z (build)`, currently at `12.7.3`). Not part of the strategy going forward — used only as a "what is upstream right now" reference. `dev` is rebased on top of master after each upstream snapshot lands.
- Diff `master..dev` in Java/Kt: 177 files, +10873 / −6368. Compact, intentional patch set of ~250 Forkgram commits.

**What we want:**
1. Old design (as in `12.1.1`, commit `9cbf03332`) — the last release before the Material 3 / redesign era.
2. All Forkgram patches (features and optimizations).
3. Fresh protocol / TL schema / native code / new server-side features from upstream `12.2.0+`.
4. Priority — "builds and runs locally", not release-quality polish.

**What we don't want:**
- New message bubbles, new sticker/gif/emoji picker, new chat top panel, new pinned panel, new top panels in general, redesigned Profile, new Topic tabs.
- Must be restored: the left slide-out drawer menu (classic Drawer) — killed in `12.2.0`.

## 0.1. Decisions log (locked-in answers from the user)

| # | Topic | Decision |
|---|---|---|
| 1 | Stories (`ui/Stories/*`) | **Keep.** Stories existed before the redesign era — they're a feature, not redesign chrome. |
| 2 | Stars / Gifts (`ui/Stars/*`, `ui/Gifts/*`, gift cells, sheets, etc.) | **Keep.** Pre-redesign features. |
| 3 | AI editor (`AIEditorAlert.java`, `AiButtonDrawable.java`) | **Keep, gated by toggle.** `dev` already has the "disable AI editor" patch — that's the model. |
| 4 | `MainTabsActivity` + bottom floating panel | **Optional via setting.** Default UI = classic Drawer. User opts in to bottom tabs. Both code paths must compile and work. |
| 5 | All TL features (schema, server-side new endpoints) | **Take everything.** Even without corresponding UI — otherwise the protocol diverges. |

Round 2 decisions (from follow-up):

| # | Topic | Decision |
|---|---|---|
| 6 | Branch model | **`dev`-only.** `master` is no longer part of the strategy. All work happens on `dev`. (`master` may continue to receive upstream-snapshot commits as an external reference, but we don't plan around it.) |
| 7 | Plan location | **Commit to repo, but not at the root.** Moving to `classic/PLAN.md`. |
| 8 | `ProfileActivity2.java`, `PollItemMenu.java`, `PollCreateCheckCell.java` | **Keep upstream versions.** Not classified as design-redesign. |
| 9 | Top panels (new `*TopPanelLayout` files) | **Strip — old design only.** Apply the same "delete the new file, restore the old code path" treatment as `*TopPanelLayout` and `*FadeView` and `*StatusLayout` (these were already in the strip list; reconfirmed). |

All open questions resolved. Plan moves from design-discussion phase to execution.

Implication of #4 (MainTabsActivity optional):
- `DialogsActivity.java` must support **both** UI modes. Pin it at 12.1.1 alone won't work — the new mode wouldn't have anywhere to live. Two viable paths:
  - **(a)** Pin `DialogsActivity` at 12.1.1, then add bottom-tabs back as an alternative path (significant porting work).
  - **(b)** Keep `DialogsActivity` at 12.7.3, restore Drawer code paths as the alternative (and default) mode (additive work, less merge pain over time).
  - Recommendation: lean (b). Cheaper to maintain across upstream merges; the user's existing `dev` patches for the side drawer are simpler to re-apply on top.

---

## 1. Current state — facts, not opinion

### 1.1. Inflection point — `12.2.0` (commit `17b771ef9`)

Delta `12.1.1 → 12.2.0`: 351 files, +33272 / −12332. A single "redesign release".

Delta `12.1.1 → 12.7.3` (half a year of upstream):
- 212 files in `org/telegram/ui/**` (Java only) changed.
- ~8500 files in `res/**` (most are emoji assets; `strings.xml` +1086 lines, `ids.xml` +4).

### 1.2. Files that are candidates for "pinning" (frozen at 12.1.1)

**Chat and messages:**
- `ui/ChatActivity.java` — +4826 / −3596. Effectively rewritten.
- `ui/Cells/ChatMessageCell.java` — +3675 / −1612. New bubble rendering.
- `ui/Components/ChatActivityEnterView.java` — +2243 / −784. Bottom input panel.
- `ui/Components/ChatReplyContainer.java` — new.
- `ui/Components/ChatActivityTopPanelLayout.java` — new, implements the new chat top panel.

**Stickers/GIFs/emoji:**
- `ui/Components/EmojiView.java` — +1566 / −638.
- `ui/Components/ChatAttachAlert.java` — +1563 / −712.
- `ui/Components/ChatAttachAlertEmojiLayout.java` — new.
- `ui/Components/ChatAttachAlertPollLayout.java` — +1323 / −326.

**Dialog list and the bottom floating panel:**
- `ui/DialogsActivity.java` — +2914 / −2361. Per decision §0.1.4 — kept at upstream version, Drawer mode added back as default; bottom-tabs is opt-in.
- `ui/MainTabsActivity.java` — **new**, kept but gated by setting (off by default).
- `ui/Components/DialogsActivityTopPanelLayout.java` — new top panel. Visible only in tabs mode → effectively dead in default config; keep but unused.
- `ui/Components/DialogsActivityTopBubblesFadeView.java` — same.
- `ui/Components/DialogsActivityStatusLayout.java` — same.
- `ui/Cells/DialogCell.java` — partial touch (`dev` already has many custom edits here).

**Left drawer (to restore):**
- Deleted upstream:
  - `ui/Adapters/DrawerLayoutAdapter.java`
  - `ui/Cells/DrawerActionCell.java`
  - `ui/Cells/DrawerAddCell.java`
  - `ui/Cells/DrawerProfileCell.java`
  - `ui/Cells/DrawerUserCell.java`
  - `ui/Cells/InviteTextCell.java`
  - `ui/Components/SideMenultItemAnimator.java`
- Action: **restore these files from `9cbf03332`** + adapt for compatibility with the current API.

**ActionBar and theming:**
- `ui/ActionBar/ActionBar.java` — +300. Semi-pin (important: many new utility methods that may be called from non-design code).
- `ui/ActionBar/ActionBarLayout.java` — +825 / thousands.
- `ui/ActionBar/DrawerLayoutContainer.java` — +745. Holds the logic of drawer-slide vs bottom-panel — critical.
- `ui/ActionBar/CustomNavigationBar.java` — deleted upstream, restore.
- `ui/ActionBar/Theme.java` — +896 / huge. Semi-pin: adding colors/tokens is fine, layout logic is not.
- `ui/ActionBar/BaseFragment.java` — +163. Hook points for new top panels.
- `ui/ActionBar/ActionBarAnimatedSubtitleOverlayContainer.java` — new, part of redesign.

**Other files dragged in by redesign:**
- `ui/ProfileActivity.java` — +1485 / −1276. Pin at 12.1.1 (the legacy profile screen we keep).
- `ui/ProfileActivity2.java` — **new**. Kept (§0.1.8).
- `ui/LaunchActivity.java` — +985 / −976. Affected not only by design (init/loading logic too); requires hand-merge.
- `ui/Components/RecyclerListView.java` — +995 / −95. Base list. Semi-pin: take perf/correctness fixes, skip layout-related changes.

**Kept (per §0.1 decisions):**
- `ui/Stories/**` — all of it. Stories is pre-redesign.
- `ui/Stars/**`, `ui/Gifts/**`, `ui/Cells/ActiveGiftAuctionsHintCell.java` — pre-redesign features.
- `ui/Components/AIEditorAlert.java`, `ui/Components/AiButtonDrawable.java` — gated by toggle (already in `dev`).

### 1.3. What changed upstream but is **not** redesign (can/should take)

- `org/telegram/tgnet/**` — TL schema, MTProto, protocol. Always take.
- `org/telegram/messenger/**` (excluding `ui/`) — controllers, DB, file loaders, networking, push, secret chats, encryption. Take with minimal conflict resolution.
- `org/telegram/SQLite/**`, `org/telegram/messenger/secretmedia/**` — almost no design influence.
- `jni/**` — native code (video decoding, voip, opus, rlottie). Always take.
- `gradle.properties`, `build.gradle`, `Dockerfile`, manifests — take.
- `assets/emoji/**`, `assets/animations/**` — take (new emoji / assets are harmless).
- `org/telegram/tgnet/test/**` — schema tests, take.

### 1.4. Composition of Forkgram patches in `dev` (rough classification)

Looking at the 250 commits, ~95% are **design-neutral**:

| Category | Examples | Design sensitivity |
|---|---|---|
| Inactive-account producer skipping | "Skipped inactive accounts in ..." — dozens of commits | Low |
| Perf optimizations | DialogCell caches, blur gating, batched invalidation, lazy BoolAnimator | Low (but touches DialogCell — potential pin conflict) |
| Hidden accounts / stealth | "Added hidden accounts and stealth mode" | Low |
| UnifiedPush + push fixes | watchdog, foreground type, distributor | Low |
| Locale fixes | currentLocale pinning, FingerprintController | Low |
| Voice/STT | Cloudflare STT, bitrate setting, square crop | Low |
| QR login, copy private link, jump to message | small features | Low |
| Settings: ForkSettingsActivity | dynamic sections, new options | Medium (relies on ListItems style → should be compatible with the theme pin) |
| AI editor toggle | "Added option to disable AI editor" | Low (disables a new AI feature — pro-classic) |
| App ID / app name / icons | branding changes | Low |
| Fast Forward, multi-select, share button | UI features | Medium (may touch ChatActivity) |
| Side-bar features | "Hided personal information in side bar" | High (depends on the Drawer existing!) |

**Important observation:** some `dev` patches **assume the left drawer exists** (e.g. "Hided personal information in side bar"). They will revive naturally once the Drawer is restored.

---

## 2. Strategy options

### Option A. "Rebase onto `12.1.1`" (what you called Option 1)

1. Set `classic-base` = `9cbf03332` (12.1.1).
2. Apply Forkgram patches from `dev` (drop only the irrelevant ones).
3. For each new upstream version: manually select valuable changes (TL schema, native, fixes) and port them.

**Pros:**
- Design preserved by construction.
- Clean boundary "ours / upstream".
- Nothing to cut out — there is nothing to cut out.

**Minuses:**
- Each new upstream release becomes a meticulous cherry-pick. For each 350-file release this is days-to-weeks of solo work.
- Easy to miss a security/network fix or break the TL schema.
- The 6 versions already shipped (12.2.0…12.7.3) have to be hand-walked.

**Cost:** very high per upstream version, relatively safe for design.

### Option B. "Revert redesign on current" (Option 2)

1. Stay on `master = 12.7.3`.
2. Point-revert redesign commits or redesign chunks.

**Pros:**
- All upstream code already present.

**Cons (fatal):**
- The upstream redesign is **not** structured as separate commits. It is "update to 12.2.0 (6289)" — a single giant squash over 351 files.
- Some "reverts" are **restorations of deleted code**. You can't `revert` something that was deleted years ago without an explicit `git checkout 9cbf03332 -- <files>`.
- API logic: `ChatActivity.java` 12.7 calls methods that didn't exist in 12.1.1; `Theme.java` 12.7 has color constants that everything else depends on. Half-reverts cascade into wholesale edits.
- **Doesn't scale to the next upstream**: you'd have to re-revert.

**Cost:** very high one-time + very high per new version = unmaintainable.

### Option C (the one you forgot). "Hybrid: pinned file list + per-version selective merge"

This is the recommended one. Idea:

1. **One-time** design work:
   - Reset "design-critical" files to their `12.1.1` versions (or to an older state if available).
   - Restore the deleted files from `12.1.1`.
   - Fix compatibility (small edits in other files so the pinned ones compile).
2. **Per new upstream version** work via two lists:
   - `ALWAYS_MERGE` (TL, native, controllers, DB, assets) — pick up upstream delta automatically.
   - `DESIGN_FROZEN` (`ChatActivity.java`, `DialogsActivity.java`, `MainTabsActivity.java`, emoji panel, …) — **don't** merge upstream's delta, **or** do a selective hand-merge for genuine bug fixes only.
3. Forkgram patches continue to live as a branch on top.

**Pros:**
- We don't throw away work already done (`master` is at 12.7.3 — we keep it as a snapshot reference).
- Boundary is explicit and formal (file lists), repeatable.
- For each new upstream the cost is proportional to **the number of changed design-critical files**, not the size of the whole release.
- Strategy grows with the project: new redesign file appears → add to `DESIGN_FROZEN`.

**Cons:**
- We first have to build a *compiling* hybrid "12.1.1 UI on top of 12.7.3 core". This is not trivial (some APIs have changed). But it's done once.
- Needs disciplined workflow: file lists and git helpers must be kept up to date.

### Option D (fallback). "Component substitution + shim"

A sub-variant of C: instead of pinning whole files, rewrite the critical UI classes once as "classic" implementations that expose the old API. E.g. `ClassicDrawerLayoutAdapter` always restored; new `ChatActivityTopPanelLayout`/`DialogsActivityTopPanelLayout` are forced to return `null`/no-op while our ActionBar draws the old way.

Makes sense pointwise (where the upstream variation of one file is short) — but as a full strategy heavier than C.

### Recommendation

**Option C** + selectively **D** for the deleted components (Drawer). Rationale:
- Minimum burned hours. Work isn't thrown out.
- The "design / not design" boundary is formalized via lists — diffable and reviewable.
- For each next upstream the list is effectively "what to take" and "what to leave as-is". Can be partly automated (see §5).

---

## 3. Recommended plan — phased

### Phase 0. Infrastructure setup (1–2 days)

- [ ] Create branch `classic-wip` off current `dev` head. Main work lives here. Once stable, fast-forward `dev` to it.
- [ ] Create file `classic/DESIGN_FROZEN.txt` with paths marked as "pinned". Starter list — §1.2 of this document.
- [ ] Create file `classic/ALWAYS_MERGE.txt` (or "everything not in `DESIGN_FROZEN`" by default — then store only exceptions).
- [ ] Helper scripts under `tools/classic/` (see §5).
- [ ] Pin baseline commit: `9cbf03332` (12.1.1).

### Phase 1. Restoring the design domain (1–2 weeks)

This is the most "engineering-recon" phase.

- [x] Rewrite pinned files to the version from `9cbf03332` (via `tools/classic/restore-frozen.sh`).
- [x] Restore deleted files (Drawer family, `CustomNavigationBar.java`, `SideMenultItemAnimator.java`).
- [x] Delete the new redesign files (`*TopPanelLayout`, `ChatAttachAlertEmojiLayout`, `ChatReplyContainer`) + clean up call sites. (`MainTabsActivity` and `Dialogs*` panels are retained per §0.1.4 — opt-in only.)
- [ ] **Iteratively** build (in progress). Each build still breaks:
  - `Theme` signatures changed → **decision:** un-pinned Theme (taken upstream wholesale). Documented in `DESIGN_FROZEN.txt`. PLAN §6 already classed it as semi-pin "allow adding new colors".
  - `RecyclerListView` semi-pin → also un-pinned (upstream `UniversalAdapter`, `EmojiTabsStrip`, etc. depend on its new sections API).
  - `LaunchActivity` semi-pin → un-pinned (12.1.1 used `com.google.firebase.appindexing` which is gone).
  - `BaseFragment` expects new hooks → added `isSupportEdgeToEdge()` shim and adapted `setNavigationBarColor`/`setLightNavigationBar` call sites.
  - `ChatActivity.java`/`ChatActivityEnterView.java`/`ChatAttachAlert.java`/`ChatMessageCell.java` (pinned) call deleted/changed APIs → **partial.** Patterns fixed so far:
    - `ScheduleDatePickerDelegate.didSelectDate(boolean, int)` → `didSelectDate(boolean, int, int)` (added `scheduleRepeatPeriod`).
    - `SendMessagesHelper.SendMessageParams.of(...)` 8/13/16-arg → +1 arg (`scheduleRepeatPeriod`) inserted after `scheduleDate`. 27 call sites in `ChatActivity` + 7 in `ChatActivityEnterView` updated.
    - `MediaController.startRecording/setReplyingMessage/prepareResumedRecording(...)` → +1 `ReplyQuote` arg (null).
    - `ChatAttachAlert.ChatAttachViewDelegate.didPressedButton(...)` → +1 `scheduleRepeatPeriod` arg in interface and all overrides.
    - `ChatActivity.sendMedia(...)/sendButtonPressed(...)/sendSticker(...)/sendGif(...)` → +1 `scheduleRepeatPeriod` arg.
    - `ChatActivity.didSelectDialogs(...)/didSelectFiles(...)` → +1 `scheduleRepeatPeriod` arg.
    - `ChatActivity.fillActionModeMenu(Menu, EncryptedChat, boolean)` → added 4-arg overload.
    - `ChatPullingDownDrawable.showBottomPanel(boolean)` → no-op shim.
    - `ChatMessageCell.setVisiblePart(...)` → added 9-arg overload forwarding to upstream 10-arg.
    - `ChatActivityEnterView.SendButton.setCircleSize(int, int)`/`.newCounterPos`/`.setBlurredBackgroundDrawable(...)` → stubs/no-ops.
    - `EmojiView` 11→12-arg constructor + `customOutline` flag → stubs.
    - `StickerSetNameCell(Context, boolean, ResourcesProvider)`/`ScrollSlidingTabStrip(Context, ResourcesProvider)` → added back compat overloads.
    - `SenderSelectPopup(Context, ChatActivity, MessagesController, TL_chatFull, ArrayList<Peer>, OnSelectCallback)` → added back adapter constructor; `dimView` field restored as no-op placeholder.
    - Drawable name renames (`ic_ab_search`→`msg_search`, `pagedown_shadow`→`floating_shadow`, `attach_shadow`→`header_shadow`).
    - Theme key renames (`key_chat_goDownButtonIcon`→`key_chat_goDownButton`).
    - `BoostRepository.loadGiftOptions(...)` removed upstream → call site stubbed to open `GiftSheet` with empty options.
    - `ActionBarAnimatedSubtitleOverlayContainer` re-included from upstream (Stories dependency).
  - **Compilation now passes — 0 errors** (from 383 → 100 → 70 → 55 → 37 → 12 → 0). Additional shims landed in this push:
    - `BaseFragment`: `getGiftAuctionsController()`, `setTitleOverlayText`, `setTitleOverlayTextIfActionBarAttached`, `checkSystemBarColors`, `drawEdgeNavigationBar`, `onInsets`, `onInsetsInternal`, `getBulletinLayoutContainer`, `getBottomInset`, `AttachedSheet.getBulletinFactory()`. `isFinished` relaxed to public; `getCustomSlideTransition` relaxed to public.
    - `ActionBar`: `createAdditionalSubTitleOverlayContainer`/`getAdditionalSubTitleOverlayContainer`, `setTitleLongClickListener`, `updateColors`, `setGlassDrawable`, additional `setAdaptiveBackground` overloads.
    - `ActionBarLayout`: predictive-back hooks (`onBackInvoked`, `onBackStarted`, `onBackProgress`, `onBackCancelled`), `setIsLayersLayout`, `getLastFragmentIncludeMainTabs`.
    - `DrawerLayoutContainer`: `setInternalNavigationBarColor` no-op.
    - `ChatActivity`: `highlightPollOptionId`, `isShouldHaveLightNavigationBarIcons`, `startFireworks`, `whenFullyVisible`, `getInputIslandHeightTarget`, `openVideoEditor(String, CharSequence)`, `ReplyQuote.fromPollOption`.
    - `ChatActivityEnterView`: `setInAppInsetsController`/`setViewParentForEmoji`/`onChangedIslandTotalHeight` no-ops; `hidePopup(b,b,b)` and `addTopView(View,int)` overloads; `setAnimatedTop`/`getTopViewEnterProgress`/`getTextWithEntities`/`setLiveComment`/`setSideButtonsForAttach`/`areLiveCommentsFree`/`setOnSendButtonLongClick`/`getSenderSelectView` shims; `setHorizontalPadding(4-arg)`, `updateSendAsButton(2-arg)`, `sendMessageInternal(5-arg)` overloads; `needStartRecordVideo` and `onMessageSend` interface sigs grew `scheduleRepeatPeriod`. Several internals (`textFieldContainer`, `sendButtonContainer`, `captionLimitView`, `emojiViewVisible`, `spans`, `sendMessage()`, `checkSendButton`) relaxed to public for PeerStoriesView/DialogsActivity. Delegate interface gained `getDefaultSendAs`, `setDefaultSendAs`, `sendAudio` defaults. `SendButton.setScrimViewBackgroundColor` no-op.
    - `ChatMessageCell`: `getPollIndex`, `getExplanationLayout`/`getExplanationX`/`getExplanationY`, `getPollAddButtonBounds`, `pollButtons`/`firstVisiblePollButton`/`lastVisiblePollButton`/`doNotDrawPollId`/`drawOnlyPollId`/`resultsPollButtonOffset`/`isReplyTaskOrPollOption` fields, `MAX_STICKER_SIZE` constant, `Delegate.isAdmin`/`isOwner` defaults.
    - `ChatAttachAlert`: `isPollAttach`, `allowLivePhotos` fields; `SearchFadeView`, `AttachSearchField` stub classes; `blur3_InvalidateBlur` no-op; `AttachAlertLayout`: `iBlur3Capture`/`iBlur3CaptureView`/`occupyNavigationBar`/`listPaddingBottom`/`occupyStatusBar` fields, `sendSelectedItems(5-arg)` overload routing through 4-arg, `sendAudio(8-arg)` default.
    - `ChatAttachAlertPollLayout`: `getStartLayoutForMedia`/`getAllowedLayoutsForIndex`/`openPollAttachMenu(5-arg)` static stubs; `getAnswersMaxCount` returns 12.1.1 defaults (30/10). Schedule lambdas updated to 3-arg.
    - `DialogCell`: `setCurrentDialogId`/`setTitleOverride` setters + `titleOverride` field.
    - `InstantCameraView`: `cancelBlur`/`invalidateBlur`/`getSwitchButtonView`/`getFlashButtonView` no-ops.
    - `ProfileActivity.SearchAdapter.SearchResult`: `path`/`iconResId`/`url`/`link` relaxed to public, added 1-arg `open(INavigationLayout)`; `loadFaqWebPage`/`faqWebPage` relaxed to public. `hasMainTabs` field added.
    - `SettingsActivity`: local `SearchAdapterStub` replaces `ProfileActivity.SearchAdapter` (non-static restriction).
    - `SideMenultItemAnimator`: `listenToAnimationUpdates(Runnable)` override.
    - `LaunchActivity` calls migrated: `openMessage` 8-arg, `markMessageReactionsAsRead` 3-arg, `closeRenameAlert`/`closeCreationLinkDialog` invoked-arg, etc.
    - Resource restores: `bottom_shadow.*`, `menu_shadow.*` from baseline; added back strings `PollQuestion`, `AnswerOptions`, `QuizInfo`.
    - Many one-off call-site adjustments in `ChatActivity` (`createScheduleDatePickerDialog` 7-arg, `createClearOrDeleteDialogAlert` 11-arg, `sendMessage(ArrayList,…)` 12-arg, `prepareSendingPhoto`/`prepareSendingAudioDocuments`/`prepareSendingVideo` `scheduleRepeatPeriod` slot, `editMessage(7-arg)`, `toggleTodo(5-arg)`, `StarsReactionsSheet(10-arg)`, `ShareAlert(14-arg)`, `BotHelpCell(currentAccount)`/`setText(6-arg)`, `TodoCompletion.completed_by` Peer→long).
- [x] Phase goal — **compile clean.** Pending: `assembleForkTestDebug` (linker/R8) and runtime launch verification.
- [x] Runtime smoke-test on 32-bit Android 11 device: app launches, login works, dialogs list opens, chats open, message sends. **Confirmed 2026-05-26.**
- [ ] No Forkgram features at this stage — design only.

#### Phase 1 → Phase 2 backlog (visual regressions seen on first run)

Observed during the 2026-05-26 smoke-test on a fresh `org.forkgram.messenger`
install. None of these block compile or runtime correctness — they are
appearance issues that the redesigned (upstream-pinned) UI still wears
because the relevant Forkgram patches haven't been cherry-picked yet, or
because the pinned classic file needs a small targeted fix:

1. **Chat: pinned-message bar is transparent.** [FIXED 2026-05-26.] Root
   cause: Phase 1 hand-merge had renamed `R.drawable.blockpanel` →
   `R.drawable.blockpanel_shadow` in 4 `ChatActivity` sites
   (`topChatPanelView`, `topChatPanelView2`, `pinnedMessageView`,
   `alertView`) because upstream had dropped `blockpanel.{png,webp}` and
   the pinned 12.1.1 code did not compile. `blockpanel_shadow` is the
   drop-shadow gradient alone, so the bar lost its solid fill. Fix:
   restored `blockpanel.png` (mdpi, hdpi, xhdpi, xxhdpi) from baseline
   `9cbf03332` and reverted the 4 sites back to `R.drawable.blockpanel`.
   Built clean with `assembleAfatDebug -PF_DROID=1`; runtime verification
   left for the next device install.
2. **Dialogs: side drawer absent.** [NOT a cherry-pick — port required.]
   Default UI is `MainTabsActivity` bottom tabs. PLAN §0.1.4 commits to
   making the drawer default. **Investigation 2026-05-26:** `dev` has
   *zero* commits touching `sideMenu` / `DrawerLayoutAdapter` /
   `DrawerProfileCell` — upstream removed the entire wiring from
   `LaunchActivity` (no `sideMenu` field; no `setSideMenu(...)`; no
   `setAllowOpenDrawer(true, false)` calls anywhere). Toggle
   `UserConfig.mainTabsHiddenFork` (commit `772d0c6b4`) only hides
   bottom tabs; it does not bring the drawer back. To restore the
   classic drawer we need a hand-port of ~200 LoC from
   `9cbf03332:LaunchActivity.java` (lines 546–578 for
   `RecyclerListView`/`DrawerLayoutAdapter`/`setDrawerLayout` plus
   ~20 `setAllowOpenDrawer(true, false)` calls at fragment transitions)
   on top of the upstream-pinned LaunchActivity. Tracking as its own
   Phase 2 task.
3. **Dialogs: folder tabs render as floating "island" chips.** Pre-
   redesign they were a continuous bar. This is `DialogsActivity`
   styling that comes with the upstream redesign; needs a Forkgram
   patch (or a targeted style override) to revert.
4. **Dialogs: server-side suggestions render as a pill.** Before the
   redesign they were a full-width bar. Same class of regression as #3
   — `DialogsActivity` upstream styling.

For Phase 2 the live tickets:
- ~~#1 (pinned bar) — fixed.~~
- ~~#2 (drawer) — compile-clean port landed 2026-05-26.~~ Added imports,
  fields and a `setupSideMenu()` helper to current `LaunchActivity` that
  builds the sideMenu RecyclerListView + DrawerLayoutAdapter +
  SideMenultItemAnimator, wires `drawerLayoutContainer.setDrawerLayout(...)`
  and adds three `setAllowOpenDrawer(true|false, false)` call sites
  (onCreate activated / not-activated paths + switchToAccount). Click
  handler covers profile cell tap, account switch and add-account.
  Long-press / drag / updateLayout integration not ported — minimal first
  pass. Runtime verification still pending — needs APK install and a
  swipe-from-left on the dialogs list to confirm drawer slides out and
  accounts/menu items render.
- ~~#3 (folder tabs as chips) — speculative fix landed 2026-05-26.~~
  Root cause: `FilterTabsView` clipped `dispatchDraw` to a rounded
  `clipPath` inset 9dp/16dp-radius, producing the chip look. Fix gutted
  the clip path build and removed `canvas.clipPath(clipPath)` so tabs
  paint edge-to-edge.
- ~~#4 (server suggestion as pill) — speculative fix landed
  2026-05-26.~~ Root cause: `DialogsActivityTopPanelLayout.dispatchDraw`
  set the `BlurredBackgroundDrawable` bounds to `(dp(4), dp(14),
  w-dp(4), …)` with radius `min(dp(24), bgH/2)`. Fix: full-width
  bounds, zero radius, zero-radius clip path.

All four regressions now ship in the APK and are **runtime-verified on
a 32-bit Android 11 device (2026-05-26 evening)**.

Critical layout bug discovered late in the session: the new redesign added
`contentView.addView(topPanelLayout, createFrame(MATCH, WRAP, TOP, 0, -14,
0, 0))` — a `-14dp` top margin that shifts the hint/context container
14dp UP into the `filterTabsView` area. Changing it to `0` fixed the
"tabs overlap hint", "archive half-shown", and "active tab underline
clipped" issues simultaneously. Lesson for future Phase 2 work: when
content appears compressed/overlapped, audit `addView(..., createFrame(...))`
margins on the wrapper before chasing internal widget geometry.

#### Phase 1 deferrals (consult before merging)

The following files were originally listed as pin/semi-pin but Phase 1 found
them too costly to shim against the 12.7.3 core. They have been taken from
upstream wholesale and noted in `DESIGN_FROZEN.txt`:

- `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/Theme.java`
- `TMessagesProj/src/main/java/org/telegram/ui/Components/RecyclerListView.java`
- `TMessagesProj/src/main/java/org/telegram/ui/LaunchActivity.java`

When Phase 1 reaches compile success and Phase 2/3 begin, re-evaluate
whether selective patches (rather than full upstream) make sense for each.

### Phase 2. Reinstating Forkgram patches (1–2 weeks)

> Note: with `dev`-only model (§0.1.6), the existing 250 Forkgram patches are already on `dev`. After Phase 1 we may rebase or cherry-pick them onto the design-restored base. Plan below describes the cherry-pick path; if rebase-onto-classic works cleanly enough, prefer that.

- [ ] Cherry-pick patches from `dev` in groups by category (§1.4). Start with "low" design sensitivity — those go through almost without conflicts:
  - Inactive accounts skipping
  - UnifiedPush / push fixes
  - Locale pinning
  - Cloudflare STT
  - QR login, copy link, jump to message
  - Hidden accounts (carefully — may touch Drawer and LaunchActivity)
- [ ] Then "medium": ForkSettingsActivity, Fast Forward, multi-select.
- [ ] Side-bar features ("Hided personal information in side bar") — should work naturally once Drawer is restored.
- [ ] DialogCell perf patches — conflict with the pin of pinned files; port by hand on top of the classic version.

### Phase 3. Catching up to upstream's feature height (as needed)

If we want to preserve valuable non-UI novelties from 12.2.0…12.7.3 (TL schema, new server features, fixes):

- [ ] Create the `ALWAYS_MERGE` list (or formalize as "everything except DESIGN_FROZEN").
- [ ] For each version $v_{i}$ in the sequence `12.2.0 → … → 12.7.3` walk the diff over `ALWAYS_MERGE` paths:
  ```bash
  git diff <prev>..<v_i> -- $(cat classic/ALWAYS_MERGE.txt) > /tmp/v_i.patch
  ```
  and apply.
- [ ] Can be done in one batch (all 6 versions) if conflicts are few — peel out specifics afterward.

### Phase 4. Recurring workflow for the next upstream versions

Every time upstream `X.Y.Z` releases:

1. Fetch the upstream snapshot into a new branch `upstream-X.Y.Z`.
2. `tools/classic/diff-by-class.sh <prev>..upstream-X.Y.Z` → produces three lists:
   - Changes inside `DESIGN_FROZEN` → report "here's what upstream changed in our pinned files. Ignore by default, or review each touch — is it a bug fix or redesign?"
   - Changes inside `ALWAYS_MERGE` → automatic merge/cherry-pick (with conflict resolution).
   - Changes in "new files" upstream → if a file is a redesign component (by heuristic on path/name), add to `DESIGN_FROZEN`. Otherwise take.
3. Rebase Forkgram patches onto the refreshed `classic`.

### Phase 5. Testing

Minimum acceptance checklist:
- [ ] Build `./gradlew :TMessagesProj:assembleForkTestDebug` (or your equivalent) succeeds.
- [ ] Launch: LaunchActivity → DialogsActivity → dialog list visible.
- [ ] Swipe right opens the old drawer with accounts and menu items (not the MainTabs modal).
- [ ] No floating bottom panel in the dialog list.
- [ ] Open a chat: top panel is the old one (avatar + name + subtitle), no new animated overlays.
- [ ] Open the emoji/sticker/gif panel in ChatActivityEnterView — old layout with three tabs.
- [ ] Pinned-message strip — old shape.
- [ ] Enable/disable AI editor works.
- [ ] Hidden accounts work.
- [ ] UnifiedPush receives a notification.

---

## 4. "Design / not design" boundary (rough list)

### 4.1. `DESIGN_FROZEN` (pinned at 12.1.1)

```
# Drawer and side menu (to restore)
TMessagesProj/src/main/java/org/telegram/ui/ActionBar/CustomNavigationBar.java
TMessagesProj/src/main/java/org/telegram/ui/ActionBar/DrawerLayoutContainer.java
TMessagesProj/src/main/java/org/telegram/ui/Adapters/DrawerLayoutAdapter.java
TMessagesProj/src/main/java/org/telegram/ui/Cells/DrawerActionCell.java
TMessagesProj/src/main/java/org/telegram/ui/Cells/DrawerAddCell.java
TMessagesProj/src/main/java/org/telegram/ui/Cells/DrawerProfileCell.java
TMessagesProj/src/main/java/org/telegram/ui/Cells/DrawerUserCell.java
TMessagesProj/src/main/java/org/telegram/ui/Cells/InviteTextCell.java
TMessagesProj/src/main/java/org/telegram/ui/Components/SideMenultItemAnimator.java

# Dialog list (default = Drawer mode, opt-in = bottom-tabs mode)
# DialogsActivity is NOT fully pinned — see §0.1.4. Kept at upstream, Drawer behavior
# restored on top via patches. Lean (b) approach.
# These files are RETAINED but only used when the user opts into bottom-tabs mode:
# - TMessagesProj/src/main/java/org/telegram/ui/MainTabsActivity.java
# - TMessagesProj/src/main/java/org/telegram/ui/Components/DialogsActivityTopPanelLayout.java
# - TMessagesProj/src/main/java/org/telegram/ui/Components/DialogsActivityTopBubblesFadeView.java
# - TMessagesProj/src/main/java/org/telegram/ui/Components/DialogsActivityStatusLayout.java

# Chat screen and input panels
TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java
TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java
TMessagesProj/src/main/java/org/telegram/ui/Components/ChatActivityEnterView.java
# New - delete
TMessagesProj/src/main/java/org/telegram/ui/Components/ChatActivityTopPanelLayout.java
TMessagesProj/src/main/java/org/telegram/ui/Components/ChatReplyContainer.java

# Emoji/stickers/GIFs
TMessagesProj/src/main/java/org/telegram/ui/Components/EmojiView.java
TMessagesProj/src/main/java/org/telegram/ui/Components/ChatAttachAlert.java
TMessagesProj/src/main/java/org/telegram/ui/Components/ChatAttachAlertPollLayout.java
# New - delete
TMessagesProj/src/main/java/org/telegram/ui/Components/ChatAttachAlertEmojiLayout.java

# Profile
TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java          # pinned at 12.1.1
# (ProfileActivity2, PollItemMenu, PollCreateCheckCell — KEPT, see §0.1.8)
# (ActiveGiftAuctionsHintCell — KEPT, gifts kept per §0.1.2)

# Action bar
TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBar.java     # semi-pin
TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarLayout.java  # semi-pin
TMessagesProj/src/main/java/org/telegram/ui/ActionBar/BaseFragment.java     # semi-pin
# New — delete
TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarAnimatedSubtitleOverlayContainer.java

# AI editor — KEPT, gated by existing toggle in dev (§0.1.3)
# (no DESIGN_FROZEN entry; AIEditorAlert.java, AiButtonDrawable.java stay as upstream)

# Borderline (to discuss)
TMessagesProj/src/main/java/org/telegram/ui/LaunchActivity.java          # semi-pin
TMessagesProj/src/main/java/org/telegram/ui/ActionBar/Theme.java          # semi-pin: take colors, skip layout
TMessagesProj/src/main/java/org/telegram/ui/Components/RecyclerListView.java  # semi-pin: take fixes
TMessagesProj/src/main/java/org/telegram/ui/Cells/DialogCell.java         # semi-pin: dev already edits this a lot
```

### 4.2. `ALWAYS_MERGE` (take upstream delta without thought)

```
# Network, MTProto, TL
TMessagesProj/src/main/java/org/telegram/tgnet/**
TMessagesProj/src/main/java/org/telegram/SQLite/**

# Controllers / messenger backend (NOT ui/)
TMessagesProj/src/main/java/org/telegram/messenger/**
  - except org/telegram/messenger/voip/** ? — verify, voip code is poked from many places
  - except org/telegram/messenger/web/** ?

# Native code
TMessagesProj/jni/**

# Build
build.gradle
gradle.properties
buildSrc/**
TMessagesProj/build.gradle

# Assets
TMessagesProj/src/main/assets/emoji/**
TMessagesProj/src/main/assets/animations/**
```

### 4.3. Gray zone (handle with care)

- `res/values/strings.xml` — most upstream strings are harmless (TL descriptions, new error texts). Design-only strings (AI editor, Star gifts UI) — skip.
- `res/drawable*` — new icons are often new-design. Review.
- `res/layout*` — XML layouts, **mostly design-critical**. Treat as `DESIGN_FROZEN` by default.
- `AndroidManifest.xml` — take; remove new activities (`MainTabsActivity`).

---

## 5. Tooling

Each script is a sketch, not final code. Names and APIs — suggestions.

### 5.1. `tools/classic/categorize-diff.sh <ref1> <ref2>`
Takes two refs, splits `git diff --name-only` into three buckets:
- `frozen/` — path matches masks from `DESIGN_FROZEN.txt`.
- `merge/` — path matches `ALWAYS_MERGE.txt`.
- `unknown/` — everything else → manual decision required.

Output: three files + brief stats (files per bucket, line counts).

### 5.2. `tools/classic/restore-frozen.sh <baseline-ref>`
Hard-overwrites all `DESIGN_FROZEN` paths from the given ref (default `9cbf03332`):
```bash
xargs -a classic/DESIGN_FROZEN.txt git checkout <baseline-ref> --
```

### 5.3. `tools/classic/merge-version.sh <upstream-ref>`
High-level wrapper:
1. `categorize-diff.sh master..<upstream-ref>` → buckets.
2. Applies patches for `merge/` (with interactive conflict resolution).
3. Restores `frozen/` from baseline (without upstream's delta).
4. Prints the `unknown/` list for manual decision.

### 5.4. Using Claude agents

A big class of automation is upstream commit analysis:

- One-off — point Explore/Plan at each upstream release `12.2.0…12.7.3` to get per-version "what's redesign, what's feature, what's fix".
- On each new upstream — the agent gives the first-pass classification.
- Pattern: "Read all changes in `<ALWAYS_MERGE paths>` between 12.7.3 and 12.8.0. Identify any change that looks design-related (mentions Material 3, bottom sheet, animation, redesign, new layout). Report file paths, commit hashes, and confidence."

### 5.5. CI

- In `.github/` (or a local hook) add a check: "if a PR/commit touches a file in `DESIGN_FROZEN.txt`, require an explicit label". This guards against `dev` patches eroding the boundary.

---

## 6. Risks and mitigations

| Risk | Probability | Mitigation |
|---|---|---|
| Pinned 12.1.1 files don't compile against 12.7.3 core (API gap) | Very high | Shim layer in `Theme.java` and `BaseFragment.java`, adding stub methods. The plan blocks Phase 1 on this work. |
| Some `dev` patch is deeply tied to post-redesign API | Medium | Cherry-pick one by one; on conflict, port by hand or postpone (separate folder `classic/pending-patches/`). |
| 12.1.1 Drawer doesn't understand hidden-accounts because of `UserConfig`/`AccountInstance` changes | Medium | Adapt DrawerLayoutAdapter to the current accounts API by hand. |
| Upstream updates to `Theme.java` bring new colors that the merged code depends on, but the pin lacks them | High | `Theme.java` — semi-pin: allow adding new colors, forbid structural changes. |
| `res/layout*` upstream changes drag Java dependencies | High | All `layout*` defaults to `DESIGN_FROZEN`. If Java code expects a new ID, add the ID to `ids.xml` but keep the old layout. |
| Slow-creep guns: AI features, Star gifts seep into controller flow | Medium | First disable via settings/no-op (the "disable AI editor" patch already exists), then strip out if needed. |
| Tech debt accumulates with selective merge | Medium | Periodic (every 3–6 months) audit of the `DESIGN_FROZEN` list: what can be unfrozen (e.g. upstream itself reverted a redesign), what to add. |

---

## 7. Long-term maintenance

**Cadence:**
- Upstream ships ~1 version every 2–4 weeks. Time per version to classify + merge: estimated 3–8 hours with tooling.
- Without tooling, realistically ~1 working day per version.
- A big redesign drop (like 12.2.0) — a separate story, could eat a week.

**What to do if the work becomes too heavy:**
1. Freeze the base on the current version. Only cherry-pick critical fixes (security, protocol) selectively.
2. Shrink `ALWAYS_MERGE` to the minimum: tgnet + native + jni + protocol patches in `messenger/`. Skip the rest. This is the stable lower bound: "we ship Telegram-12.1.1 with an up-to-date network stack".
3. If everything falls — publish as "archival, only TL schema is updated". Still better than a dead fork.

**Indicators of trouble** (when to stop and rethink):
- One upstream release takes > 1.5 working days even with tooling.
- The `unknown/` bucket in each version is > 50 files (means the boundaries are stale).
- Build is failing for > 30 minutes.

---

## 8. Open questions

All resolved — see decisions log in §0.1.

1. ~~Stories — keep~~ ✓
2. ~~Stars / Gifts — keep~~ ✓
3. ~~AI editor — toggle~~ ✓
4. ~~`MainTabsActivity` — optional via setting (classic Drawer = default)~~ ✓
5. ~~TL schema and server features — take all~~ ✓
6. ~~Branch model — dev-only~~ ✓
7. ~~Plan location — committed at `classic/PLAN.md`~~ ✓
8. ~~`ProfileActivity2.java`, `PollItemMenu.java`, `PollCreateCheckCell.java` — keep upstream~~ ✓
9. ~~Top panels — strip the new `*TopPanelLayout` files~~ ✓

---

## 9. What I propose for the next 2–3 sessions

1. Agree on §8.
2. Based on the answers refine `DESIGN_FROZEN.txt` and `ALWAYS_MERGE.txt`.
3. Run **Phase 0** — script skeletons + `classic-wip` branch.
4. Realistically budget: spend a day on **Phase 1.0** — attempt to build "12.1.1 UI + 12.7.3 core" with a minimum shim layer — and see how wide the API gap is in practice. That is the project's technical "reality check". Without it all the estimates above are expert guesses.

After the 1–2 day pilot spike it becomes clear whether Option C is feasible or we should retreat to a pure rebase-on-12.1.1.
