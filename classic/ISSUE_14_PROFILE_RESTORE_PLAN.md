# Issue #14 — Restore the classic (pre-12.0) profile header

> Plan for a focused follow-up session. Written 2026-06-07 after shipping the
> safe "buttons-off" partial (commit `d3e33588a`).

## ✅ STATUS: COMPLETED (2026-06-08)

The geometry revert below was implemented in the main tree, built (`assembleAfatDebug
-PF_DROID=1`), installed on the RMX3581, and device-verified. Result: small **42dp
left-aligned** avatar, name/online left-aligned, `dp(88)` header — the classic look.

**What was done (all in `ProfileActivity.java`):**
- `setScaleSize(1f)`, avatar reparented directly into `avatarContainer2` as a 42×42
  frame (avatarGooey kept constructed-but-detached → its `notchInfo` guards short-circuit).
- `getHeaderOnlyExtraHeight()` → `dp(88)`.
- `needLayout()` collapsed branch + `avatarX = -dpf2(47)*diff`, classic `avatarY`,
  left `nameX/onlineX = -21*density*diff`, classic `nameY/onlineY`.
- Full **100dp→42dp reparameterization** of every avatar-sizing site (expand-branch
  scale, `playProfileAnimation==2` entrance, `backwardAnimationLayout`,
  `setAvatarExpandProgress` params + classic bezier `kx==ky==dpf2(8)`,
  nameYEnd `dpf2(38)`, onlineYEnd `dpf2(18)`), plus `calculatePositionsOnFirstLoad`
  (deep-link-expanded) and `getSmallAvatarRoundRadius` (forum stays a rounded square).
- A defensive params reset in the collapsed branch guarantees the 42dp base at rest
  even if an animation left a stale expanded size.

**Device-verified:** static collapsed (own letter avatar + Telegram photo avatar),
scroll-collapse into the action bar, pull/tap-to-fullscreen expand + collapse-back,
standard entrance (recorded video — clean). Two independent code critics confirmed
every geometry site matches 11.9.5.0 with no NPE/regression risk.

The historical plan is kept below for reference.

---

## TL;DR / the key finding

`ProfileActivity.java` is pinned to the **12.1.1 baseline, which IS already the
iOS redesign** — big *centered* collapsing avatar, name/online centered below it.
The fork's own notes are **WRONG** about this: `classic/MODERN_UI_CUTS.md` and
`classic/PLAN.md` claim "Redesigned profile screen — gone (legacy 12.1.1 profile
restored)". Pinning to 12.1.1 *preserved* the redesign, it did not remove it.
That is exactly what issue #14 reports. (The real classic profile is 11.x; the
overhaul landed in 12.0.1, where the file jumped 15,153 → 16,578 lines.)

Diff sizes vs current: 12.1.1.0 = ~212 lines (fork shims only), 11.9.5.0 = ~3,854
lines. So a full file swap is impractical (API drift against the modern 12.7 core).
The fix is a **surgical geometry revert** keeping the file modern-core-compatible.

## Done already (commit `d3e33588a`)

`getActionsExtraHeight()` → `return 0` on all profiles. This removes the iOS
action-buttons row (Message/Mute/Call/Video/Share/Gift): `ProfileActionsView` is
gated on `getActionsExtraHeight() > 0` (~line 3777), so it is never created, and
the screen falls back to the classic floating write button + action-bar
call/video items (the proven own-profile path). Verified via drawer → My Profile.

## Remaining goal

Big **centered 100dp** avatar → small **42dp left-aligned** avatar; name/online
left-aligned; collapsed header height `dp(88)`. (Background gradient `topView` can
stay — out of scope.)

## Reference = source of truth

```
git -C /home/h/src/TelegramAndroid show 11.9.5.0:TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java
```
`11.9.5.0` is the last 11.x = genuine classic. Mirror its formulas.

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

## Current sites to revert (line numbers approximate — re-grep first)

| Method / site | Current (12.1.1 redesign) | Change to |
|---|---|---|
| `getHeaderOnlyExtraHeight()` ~5912 | `dp(168)` / `dp(152)` | `dp(88)` |
| avatar creation ~5223–5226 | `avatarGooey.addView(avatarContainer, createFrame(100,100,…,64,0,0,0))` + `avatarContainer2.addView(avatarGooey)` | `avatarContainer2.addView(avatarContainer, createFrame(42,42,…,64,0,0,0))`; keep `avatarGooey` field but out of the view tree |
| `avatarDrawable.setScaleSize(100f/42f)` ~3958 | 100dp letter size | `setScaleSize(1f)` |
| `fixAvatarImageInCenter()` ~6440 | `avatarX = listView.getMeasuredWidth()/2f - …` (centers) | classic left `avatarX = -dpf2(47f)*diff` — neutralize the method or replace its calls and set `avatarX` directly |
| `updateCurrentExpandAnimatorValue` ~6340–6434 | `params.width/height` use `dpf2(100)`; centered bezier `ky = isPulledDown?dpf2(8):dpf2(-24)` | 42dp params; classic bezier (`kx=ky=dpf2(8)`, nameYEnd `dpf2(38)`, onlineYEnd `dpf2(18)`) |
| `needLayout()` ~8442 | `avatarY` uses `avatarGooey.notchInfo`; `avatarScale = lerp(96/42,138/42,…)/100*42`; collapsed branch centers | classic `avatarY`; 42-based expand scale; classic collapsed `avatarScale`/`avatarX`/`nameX` |
| `backwardAnimationLayout()` ~8272 | `avatarScale = lerp(42/100, …)` | `lerp(1.0f, …)`; `extra = dp(42)*avatarScale - dp(42)` |
| entrance `playProfileAnimation==1/2` + `calculatePositionsOnFirstLoad` ~8897 | centered `nameX = listView width/2 …` | classic left |

## Entanglements / risk (why this is ~300–400 lines, not a one-liner)

- **ProfileGooeyView** (`avatarGooey`) wraps the avatar and exposes `notchInfo`
  used by `needLayout` for `avatarY`. Removing it from the hierarchy means every
  `avatarGooey.notchInfo` path must be neutralized; `updateGooey()` should be a
  no-op for the blob.
- **Everything is parameterized around a 100dp base avatar** (`/100`, `*100`,
  `dpf2(100)`); every scale must be reparameterized to 42dp consistently.
- **Highest glitch risk** (verify on device): (a) pull-down-to-fullscreen expand,
  (b) avatar-zoom open entrance (`playProfileAnimation==2`, tapping a chat-header
  avatar), (c) standard open entrance (`==1`) title cross-fade, (d) close/back
  animation, (e) deep-link-into-expanded-photo first load, (f) forum/topic avatar
  round-radius (`getSmallAvatarRoundRadius`).

## Recommended approach — incremental, device-verified

Do **not** one-shot it, and do **not** delegate to a worktree agent: this session
a worktree was created from the **merge-base `9fea72647`**, not `classic` HEAD, so
the agent's `ProfileActivity` came out modern-contaminated (added
`MainTabsActivity.TabFragmentDelegate`, `blur3.*`, `me.vkryl…BoolAnimator`). It
compiled only because the 12.7 core has those classes. Work **directly in the main
tree**, step by step, rebuilding + screenshotting each step:

1. `getHeaderOnlyExtraHeight()→dp(88)` + avatar 42 + reparent + `setScaleSize(1f)`.
   Build, open profile, screenshot. Expect a small avatar, likely mis-positioned.
2. Classic collapsed `needLayout` `avatarX`/`nameX`/`scale`. Build, verify the
   **static collapsed** look = small left avatar + left name. **This is the main win.**
3. `fixAvatarImageInCenter` + `updateCurrentExpandAnimatorValue` → verify pull-to-fullscreen.
4. `backwardAnimationLayout` + entrance animations → verify open/close animation.
5. Forum/topic + expanded-photo edge cases.

Revert anytime (single file): `git checkout HEAD -- TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java`

## Build / verify cheatsheet

```
# Build (afat universal debug, installable):
JAVA_HOME=/usr/lib/jvm/java-21 ANDROID_HOME=/opt/android-sdk \
  ./gradlew :TMessagesProj_App:assembleAfatDebug -PF_DROID=1 --console=plain
# Fast java-only compile check:
  …:TMessagesProj:compileDebugJavaWithJavac -PF_DROID=1
# Install + launch (device RMX3581):
adb -s 192.168.1.48:41281 install -r TMessagesProj_App/build/outputs/apk/afat/debug/TMessagesProj_App-afat-debug.apk
adb -s 192.168.1.48:41281 shell monkey -p org.forkgram.classic -c android.intent.category.LAUNCHER 1
```

Reliable profile nav on the test device: **drawer (tap 40,70) → My Profile (tap
200,423)**. My Profile renders with `actionsView==null` = the same path all
profiles use now. NOTE: dialog-row taps and chat-header taps are **flaky** on this
RMX3581 (they often reopen Saved Messages / don't open the profile) — use the
drawer path, or `adb shell input` into a group/channel header if a non-own profile
is needed.
