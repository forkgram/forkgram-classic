# Forkgram Classic — plan for preserving the old design

Strategy for keeping the pre-`12.2.0` Telegram UI while still tracking upstream.
Companions: `BASELINE.txt`, `DESIGN_FROZEN.txt`, `ALWAYS_MERGE.txt`,
`MODERN_UI_CUTS.md`, and the scripts in `tools/classic/`.

## 0. Goals

Baseline is `12.1.1` (commit `9cbf03332`) — the last release before the
Material 3 / redesign era.

**Keep:** that design; all Forkgram patches; fresh protocol, TL schema, native
code and server-side features from `12.2.0+`. The bar is "builds and runs",
not release-quality polish.

**Drop:** new message bubbles, the new sticker/gif/emoji picker, the new chat
top panel and pinned panel, top panels in general, the redesigned Profile,
Topic tabs. The left slide-out drawer, killed in `12.2.0`, has to come back.

### 0.1. Decisions (locked)

| # | Topic | Decision |
|---|---|---|
| 1 | Stories (`ui/Stories/*`) | **Keep** — pre-redesign feature, not redesign chrome. |
| 2 | Stars / Gifts (`ui/Stars/*`, `ui/Gifts/*`, gift cells and sheets) | **Keep** — pre-redesign. |
| 3 | AI editor (`AIEditorAlert`, `AiButtonDrawable`) | **Keep, gated by toggle** — `dev` already has the "disable AI editor" patch. |
| 4 | `MainTabsActivity` + bottom floating panel | **Opt-in setting.** Default is the classic drawer; both paths must compile and work. |
| 5 | TL features (schema, new server endpoints) | **Take everything**, UI or no UI — otherwise the protocol diverges. |
| 6 | Branch model | **`dev`-only.** `master` stays as an upstream-snapshot reference, nothing is planned around it. |
| 7 | Plan location | Committed as `classic/PLAN.md`. |
| 8 | `ProfileActivity2`, `PollItemMenu`, `PollCreateCheckCell` | **Keep upstream** — not redesign. |
| 9 | New `*TopPanelLayout` files | **Strip**, together with `*FadeView` and `*StatusLayout`: delete the new file, restore the old code path. |

Decision 4 has a consequence: `DialogsActivity` must serve both UI modes, so it
is **not** pinned. It stays at the upstream version with the drawer restored on
top — additive work, and far less merge pain than pinning it at 12.1.1 and
re-porting bottom tabs into it.

## 1. Where the design broke

`12.1.1 → 12.2.0`: 351 files, +33272 / −12332 — one squashed release, not a
series of revertable commits. `12.1.1 → 12.7.3`, half a year later: 212 files
under `org/telegram/ui/**`, ~8500 under `res/**` (mostly emoji assets;
`strings.xml` +1086 lines).

`DESIGN_FROZEN.txt` holds the authoritative pin list. The diff sizes behind it:

- **Chat:** `ChatActivity` +4826/−3596 (effectively rewritten),
  `ChatMessageCell` +3675/−1612 (new bubbles), `ChatActivityEnterView`
  +2243/−784, plus the new `ChatReplyContainer` and
  `ChatActivityTopPanelLayout`.
- **Pickers:** `EmojiView` +1566/−638, `ChatAttachAlert` +1563/−712,
  `ChatAttachAlertPollLayout` +1323/−326, plus the new
  `ChatAttachAlertEmojiLayout`.
- **Dialog list:** `DialogsActivity` +2914/−2361, kept at upstream per decision
  4. `MainTabsActivity` and the `DialogsActivity*` panels are retained but
  reachable only in opt-in tabs mode.
- **Drawer**, deleted upstream and restored from baseline: `DrawerLayoutAdapter`,
  `DrawerActionCell`, `DrawerAddCell`, `DrawerProfileCell`, `DrawerUserCell`,
  `InviteTextCell`, `SideMenultItemAnimator`, `CustomNavigationBar`.
- **Action bar:** `ActionBar` +300, `ActionBarLayout` +825, `BaseFragment` +163,
  and `DrawerLayoutContainer` +745, which owns drawer-slide vs bottom-panel.
- **Profile:** `ProfileActivity` +1485/−1276 pinned; `ProfileActivity2` kept.

Taken from upstream wholesale: `tgnet/**`, `SQLite/**`, `messenger/**` outside
`ui/`, `jni/**`, build files, emoji and animation assets.

Roughly 95% of the ~250 Forkgram patches in `dev` are design-neutral (inactive-
account skipping, perf, hidden accounts, UnifiedPush, locale, STT, QR login).
The design-sensitive ones touch `DialogCell`, `ForkSettingsActivity`,
`ChatActivity` — and some, like "hided personal information in side bar",
assume the drawer exists and revive on their own once it is back.

## 2. Approach

Pin a list of design-critical files at the baseline, take everything else from
upstream per version, and rebuild by hand the components upstream deleted.

The alternatives lost on cost. Rebasing onto `12.1.1` and hand-porting each
upstream release preserves the design by construction, but costs days to weeks
per release and invites missed protocol and security fixes. Reverting the
redesign on top of `12.7.3` fails outright: it arrived as a single squashed
commit, several of its "reverts" are restorations of long-deleted code, and
half-reverts cascade — 12.7 `ChatActivity` calls methods 12.1.1 never had.

## 3. Phases

### Phase 0 — infrastructure. Done.

`BASELINE.txt` (baseline ref), `DESIGN_FROZEN.txt` (pinned paths),
`ALWAYS_MERGE.txt` (paths taken from upstream), and the three scripts in §5.

### Phase 1 — restore the design domain. Done.

Pinned files rewritten from `9cbf03332`, deleted files restored, redesign-only
files removed — `MODERN_UI_CUTS.md` has the inventory. Compilation went from
383 errors to 0.

Nearly all of that work is shims: overloads, stub methods and visibility
relaxations on the pinned classes so the 12.7.3 core can call into them. Each
carries a `forkgram-classic:` comment naming the upstream call site it serves,
and those comments — not this document — are the record. The patterns that
repeated: upstream's new `scheduleRepeatPeriod` argument (34 call sites in
`ChatActivity` and `ChatActivityEnterView` alone), added `ReplyQuote`
arguments, and drawable and theme-key renames.

Three files listed as semi-pin proved too costly to shim and are taken from
upstream wholesale — `Theme.java`, `RecyclerListView.java` (the new sections
API is a dependency of `UniversalAdapter` and `EmojiTabsStrip`) and
`LaunchActivity.java` (12.1.1 used the since-removed
`com.google.firebase.appindexing`). Worth re-evaluating later whether selective
patches beat the full upstream file.

Runtime smoke-test on a 32-bit Android 11 device — launch, login, dialog list,
opening chats, sending — confirmed 2026-05-26.

#### Phase 1 → 2 backlog

Four appearance regressions turned up on that first run, all fixed and
runtime-verified the same evening. The root causes are worth keeping:

1. **Pinned-message bar transparent.** The Phase 1 hand-merge had swapped
   `R.drawable.blockpanel` for `blockpanel_shadow` at four `ChatActivity` sites
   because upstream dropped the former — but the shadow gradient carries no
   fill. Restored `blockpanel.png` from the baseline and reverted the sites.
2. **Side drawer absent.** Not a cherry-pick: `dev` has *zero* commits touching
   `sideMenu` / `DrawerLayoutAdapter`, and upstream deleted the whole wiring
   from `LaunchActivity`; `UserConfig.mainTabsHiddenFork` only hides the bottom
   tabs. Ported ~200 LoC back as a `setupSideMenu()` helper — the sideMenu
   `RecyclerListView`, adapter, item animator, `setDrawerLayout(...)` and three
   `setAllowOpenDrawer` call sites. Long-press, drag and `updateLayout`
   integration are not ported.
3. **Folder tabs as floating chips.** `FilterTabsView.dispatchDraw` clipped to a
   rounded `clipPath`, inset 9dp with a 16dp radius; gutting the clip path puts
   the tabs back edge-to-edge.
4. **Server suggestions as a pill.** `DialogsActivityTopPanelLayout.dispatchDraw`
   bounded its `BlurredBackgroundDrawable` to `dp(4)` insets with a `dp(24)`
   radius; full-width bounds and zero radius restore the bar.

One lesson from the same session: the redesign attaches `topPanelLayout` with a
`-14dp` top margin, which lifts the hint container into `filterTabsView`.
Setting it to 0 fixed "tabs overlap hint", "archive half-shown" and "active tab
underline clipped" at once. When content looks compressed or overlapped, audit
the wrapper's `createFrame` margins before the widget's own geometry.

### Phase 2 — reinstate Forkgram patches

The ~250 patches already live on `dev`, so this is a rebase or a cherry-pick,
whichever conflicts less.

- [ ] Take the design-neutral groups first — inactive-account skipping,
      UnifiedPush and push fixes, locale pinning, Cloudflare STT, QR login,
      copy link, jump to message. Hidden accounts need care: they touch the
      drawer and `LaunchActivity`.
- [ ] Then `ForkSettingsActivity`, Fast Forward, multi-select.
- [ ] `DialogCell` perf patches conflict with the pin — port by hand onto the
      classic version.

### Phase 3 — catch up on non-UI upstream

For each version from `12.2.0` to `12.7.3`, walk the delta over the
`ALWAYS_MERGE` paths and apply it:

```bash
git diff <prev>..<v_i> -- $(cat classic/ALWAYS_MERGE.txt) > /tmp/v_i.patch
```

One batch for all six is fine if conflicts stay few.

### Phase 4 — per-version workflow

1. Fetch the upstream snapshot into `upstream-X.Y.Z`.
2. `categorize-diff.sh` splits it three ways: frozen paths (ignore by default,
   or review each touch — bug fix or redesign?), merge paths (apply), new files
   (redesign component by path or name → add to `DESIGN_FROZEN`, else take).
3. Rebase the Forkgram patches onto the refreshed `classic`.

### Phase 5 — acceptance

Build `:TMessagesProj:assembleForkTestDebug`, then: dialog list opens; swiping
right opens the old drawer with accounts and menu items, not the MainTabs
modal; no floating bottom panel; a chat shows the old top panel with no
animated overlays; the emoji panel has the old three-tab layout; the
pinned-message strip keeps its old shape; the AI-editor toggle, hidden accounts
and UnifiedPush notifications all work.

## 4. Boundary

The two lists are files, not prose — see `DESIGN_FROZEN.txt` and
`ALWAYS_MERGE.txt`. What they cannot express:

- `res/values/strings.xml` — take upstream; skip strings that exist only for
  redesign features.
- `res/drawable*` — review; new icons are often new design.
- `res/layout*` — frozen by default. If Java needs a new id, add it to
  `ids.xml` and keep the old layout.
- `AndroidManifest.xml` — take, minus the new activities.

## 5. Tooling

- `categorize-diff.sh <ref1> <ref2>` — splits a diff into frozen / merge /
  unknown, with per-bucket stats.
- `restore-frozen.sh [ref]` — overwrites every frozen path from the baseline.
- `merge-version.sh <ref>` — categorize, apply the merge bucket, restore the
  frozen bucket from baseline, print the unknowns.

Upstream triage is a good fit for an agent: point it at a release diff over the
`ALWAYS_MERGE` paths and ask which changes look design-related (Material 3,
bottom sheet, animation, new layout), with paths, hashes and confidence.

## 6. Risks

| Risk | Mitigation |
|---|---|
| Pinned 12.1.1 files don't compile against the 12.7.3 core | The shim layer. This is the bulk of Phase 1 and it is bounded, one-time work. |
| A `dev` patch is tied to post-redesign API | Cherry-pick one by one; port by hand or postpone on conflict. |
| Upstream `Theme.java` gains colors the merged code needs | Un-pinned: taken from upstream wholesale. |
| `res/layout*` changes drag Java dependencies | Layouts frozen by default; add ids rather than layouts. |
| AI and Star-gift features seep into controller flow | Disable via setting or no-op first, strip only if needed. |
| Selective merge accrues debt | Audit `DESIGN_FROZEN` every 3–6 months: what can be unfrozen, what must be added. |

## 7. Long-term maintenance

Upstream ships every 2–4 weeks; with the tooling, classifying and merging a
version should take hours rather than the working day it costs by hand. A
redesign drop on the scale of `12.2.0` is its own project.

If it gets too heavy, retreat in this order: freeze the base and cherry-pick
only security and protocol fixes; shrink `ALWAYS_MERGE` to `tgnet` + `jni` +
protocol patches in `messenger/`, which still ships "Telegram 12.1.1 with a
current network stack"; failing that, publish as archival with TL schema
updates only. Any of these beats a dead fork.

Stop and rethink if a single upstream release eats more than a day and a half
with tooling, or if the `unknown/` bucket runs past 50 files — that means the
boundary lists have gone stale.
