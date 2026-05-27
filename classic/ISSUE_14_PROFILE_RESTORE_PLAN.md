# Issue #14 — Restore the classic (pre-12.0) profile header

## ✅ Completed 2026-06-08

Implemented in the main tree, built (`assembleAfatDebug -PF_DROID=1`), installed on the RMX3581 and
device-verified: a small **42dp left-aligned** avatar, name and online status left-aligned, `dp(88)`
header — the classic look.

All of it in `ProfileActivity.java`: `setScaleSize(1f)`; the avatar reparented directly into
`avatarContainer2` as a 42×42 frame (`avatarGooey` stays constructed but detached, so its
`notchInfo` guards short-circuit); `getHeaderOnlyExtraHeight()` → `dp(88)`; the `needLayout()`
collapsed branch with `avatarX = -dpf2(47)*diff`, classic `avatarY`, and left `nameX/onlineX =
-21*density*diff`; and a full **100dp→42dp reparameterization** of every avatar-sizing site — the
expand-branch scale, the `playProfileAnimation==2` entrance, `backwardAnimationLayout`,
`setAvatarExpandProgress` with the classic bezier (`kx==ky==dpf2(8)`, nameYEnd `dpf2(38)`,
onlineYEnd `dpf2(18)`), `calculatePositionsOnFirstLoad` and `getSmallAvatarRoundRadius` (forum
avatars stay rounded squares). A defensive params reset in the collapsed branch guarantees the 42dp
base at rest even if an animation left a stale expanded size.

Verified static collapsed (letter and photo avatars), scroll-collapse into the action bar,
pull/tap-to-fullscreen expand and collapse-back, and the standard entrance.

The earlier safe partial, commit `d3e33588a`, is what removed the iOS action-buttons row
(Message/Mute/Call/Video/Share/Gift): `getActionsExtraHeight()` returns 0, and since
`ProfileActionsView` is gated on it being > 0 the view is never created, so the screen falls back to
the classic floating write button plus action-bar call/video items.

## The key finding

`ProfileActivity.java` is pinned to the 12.1.1 baseline, **which is already the iOS redesign** — a
big *centered* collapsing avatar with name and online centered below it. `MODERN_UI_CUTS.md` and
`PLAN.md` both claimed "redesigned profile screen — gone (legacy 12.1.1 profile restored)", and both
were **wrong**: pinning to 12.1.1 preserved the redesign rather than removing it, which is exactly
what issue #14 reported. The real classic profile is 11.x; the overhaul landed in 12.0.1, where the
file jumped 15,153 → 16,578 lines.

That is why the fix had to be a surgical geometry revert rather than a file swap: against the
current file, 12.1.1.0 is ~212 lines away (fork shims only) but 11.9.5.0 is ~3,854, and that version
cannot survive the API drift of the modern 12.7 core.

Reference, and the source of truth for every formula below:

```
git -C /home/h/src/TelegramAndroid show 11.9.5.0:TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java
```

### Classic geometry (extracted from 11.9.5.0 needLayout + helpers)
```
extraHeight collapsed = dp(88)
avatar       = createFrame(42, 42, TOP|LEFT, 64,0,0,0), parented DIRECTLY into avatarContainer2 (no ProfileGooeyView wrap)
setScaleSize = 1f                                  (current 100f/42f, for a 100dp avatar)
avatarScale  = (42 + 18*diff) / 42f                (collapsed)
nameScale    = 1.0f + 0.12f*diff
avatarX      = -dpf2(47f) * diff                   (LEFT-aligned; NOT centered)
avatarY      = (occupyStatusBar?statusBarHeight:0) + ActionBar.getCurrentActionBarHeight()/2f*(1+diff) - 21*density + 27*density*diff + actionBar.getTranslationY()
nameX        = -21*density*diff
nameY        = floor(avatarY) + dp(1.3f) + dp(7)*diff + titleAnimationsYDiff*(1-avatarAnimationProgress)
onlineX      = -21*density*diff
onlineY      = floor(avatarY) + dp(24) + floor(11*density)*diff
expand scale = lerp((42+18)/42, (42+42+18)/42, min(1, expandProgress*3))   (42-based, NOT /100)
```

## Why this was ~300–400 lines and not a one-liner

- **`ProfileGooeyView`** (`avatarGooey`) wraps the avatar and exposes the `notchInfo` that
  `needLayout` reads for `avatarY`, so taking it out of the hierarchy means neutralizing every
  `notchInfo` path and making `updateGooey()` a no-op for the blob.
- **Everything is parameterized around a 100dp base avatar** (`/100`, `*100`, `dpf2(100)`), and
  every scale has to be reparameterized to 42dp consistently.
- Highest glitch risk, all device-verified: pull-down-to-fullscreen expand, the avatar-zoom entrance
  (`playProfileAnimation==2`), the standard entrance title cross-fade, the close/back animation,
  deep-link-into-expanded-photo first load, and the forum/topic round-radius.

## Two process lessons

- **Do not delegate this to a worktree agent.** One was created from the merge-base `9fea72647`
  instead of `classic` HEAD, so its `ProfileActivity` came back modern-contaminated
  (`MainTabsActivity.TabFragmentDelegate`, `blur3.*`, a `BoolAnimator` import) and compiled only
  because the 12.7 core happens to carry those classes. Work directly in the main tree, rebuilding
  and screenshotting each step.
- On the RMX3581, dialog-row and chat-header taps are **flaky** — they often reopen Saved Messages
  instead of the profile. Navigate via the drawer (tap 40,70 → My Profile at 200,423), which renders
  with `actionsView == null`, the same path every profile uses now.
