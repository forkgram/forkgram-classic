# F-Droid release checklist — Forkgram Classic

> **Deployment model (2026-05-28).** Forkgram Classic is a **separate app** —
> `applicationId = org.forkgram.classic`, display name "Forkgram Classic" — installed
> **alongside** regular Forkgram, not replacing it, and published to the official F-Droid
> repo as a **new package** with its own fdroiddata recipe. Not an in-place update of
> `org.forkgram.messenger`.
>
> Little extra infrastructure is needed: the original Forkgram has built on F-Droid for
> years, and F-Droid builds from source on its own servers, so the GitHub CI workflows
> (`fd.yml` auto-MR, `tandroid.yml` release/attestation) were removed and only
> `docker-builder.yml` is kept for local reproducible-build dry runs. The work is a recipe
> plus a clean tagged commit on `classic`, the main branch.

## Before tagging

- [ ] Cold-built APK installs and launches; 5-minute smoke test of the classic UI (drawer, folder
  tabs, pinned-message bar, audio mini-player, poll v2, ShareAlert / Fast Forward, dialog color
  dots).
- [ ] No `TODO` / `WIP` / `broken` markers in the commit chain about to ship.
- [ ] `TMessagesProj/jni/tde2e_source` submodule clean — the most common silent reproducibility
  breaker. `git submodule status` matches `.gitmodules` (incl. `libvpx`), and `git status --short`
  shows only intended state.
- [ ] Version code bumped by 1. APK version is `(APP_VERSION_CODE*10 + ADDITIONAL_BUILD_NUMBER)*10 +
  abiVersionCode`.

## The first release (12.7.3.0, done 2026-05-28)

- `APP_VERSION_CODE` stayed at 6750 — nothing published to exceed. versionName 12.7.3.0, versionCode
  675007 (v7a) / 675008 (v8a) / 675009 (afat), confirmed via aapt2.
- Built with **JDK 21** (`JAVA_HOME=/usr/lib/jvm/java-21`; the default `java` is 25 — avoid): `afat`
  debug and `afatFd_v8a` release both succeeded with `-PF_DROID=1` on the tagged commit `2ae4bb8b3`.
  That local Gradle build does **not** run the fdroiddata `prebuild` seds — only `fdroid build` in
  MR CI proves the real thing.
- Recipe assembled at `classic/fdroiddata/org.forkgram.classic.yml`, modeled on the **live**
  `org.forkgram.messenger` entry rather than the stale `metadata/_/` fragments (r21e ndk, simple
  prebuild): `subdir: TMessagesProj_App`, gradle `afatFd_v7a`/`afatFd_v8a`, 10-step prebuild,
  `commit: 12.7.3.0`. Auto-update on (`UpdateCheckMode: Tags`, `VercodeOperation: 100*%c+{7,8}`);
  F-Droid-signed, not reproducible (no dev-side signing).
- Submitted as **MR !39243** ("New App: Forkgram Classic"), CI passed, author info added on
  maintainer request, **merged 2026-05-28**.

## After a release

- [ ] Verify it installs from the F-Droid client and appears at
  `https://f-droid.org/app/org.forkgram.classic`.
- [ ] Update the `reference-fdroid-checklist` / `project-phase2-progress` memories.

## Pitfalls

- versionCode is append-only: a broken build needs a *new* version, never a re-tag.
- Never tag or push with a dirty submodule — F-Droid builds the tagged tree exactly.
