# F-Droid release checklist — Forkgram Classic

> **Deployment model (updated 2026-05-28).** Forkgram Classic is a **separate
> app** — `applicationId = org.forkgram.classic`, display name "Forkgram
> Classic" — installed **alongside** regular Forkgram, not replacing it. It is
> published to the **official F-Droid repo as a new package** (a fresh
> fdroiddata recipe keyed to `org.forkgram.classic`), NOT an in-place update of
> `org.forkgram.messenger`.
>
> **Why this repo needs little extra:** the original Forkgram has built on
> F-Droid for years. That infrastructure — the F-Droid build server,
> reproducible-build setup, the `metadata/_/` recipe fragments — already works.
> F-Droid builds from source on its own servers, so the GitHub CI workflows
> (`fd.yml` auto-MR, `tandroid.yml` release/attestation) were removed; only
> `docker-builder.yml` is kept for local reproducible-build dry runs. The
> remaining work is mostly: a new fdroiddata recipe + a clean tagged commit.
>
> **Branch:** `classic` is the main branch (renamed from `dev`, pushed to
> `origin/classic`). Do all work on `classic` and push it to `origin`.

---

## 1. App readiness (smoke test on device)

- [ ] Cold-built APK installs and launches; 5-minute smoke test of the classic
      UI (drawer, folder tabs, pinned-message bar, audio mini-player, poll v2,
      ShareAlert / Fast Forward, dialog color dots).
- [ ] No `TODO` / `WIP` / `broken` markers in the commit chain about to ship.

## 2. Working tree hygiene

- [ ] `TMessagesProj/jni/tde2e_source` submodule clean (most common silent
      reproducibility breaker — `git -C TMessagesProj/jni/tde2e_source status`).
- [ ] `git submodule status` matches `.gitmodules` pins (incl. `libvpx`).
- [ ] `git status --short` shows only intended state.

## 3. Version

- [x] **First F-Droid release: keep `APP_VERSION_CODE` (6750)** — there is no
      prior published version to exceed, so no bump is needed. APK version =
      `(APP_VERSION_CODE*10 + ADDITIONAL_BUILD_NUMBER)*10 + abiVersionCode`
      (ADDITIONAL_BUILD_NUMBER=0) → versionName 12.7.3.0, versionCode 675007
      (v7a) / 675008 (v8a); confirmed via aapt2 (afat build = 675009). Bump by 1
      only for *subsequent* updates.

## 4. Reproducible build

- [x] Built with **JDK 21** (`JAVA_HOME=/usr/lib/jvm/java-21`; default `java` is
      25 — avoid). On the tagged release commit `2ae4bb8b3` (= tag `12.7.3.0`):
      `afat` debug + `afatFd_v8a` **release** (unsigned) both `BUILD SUCCESSFUL`
      `-PF_DROID=1` (re-verified 2026-05-28 after the fix — 23 tasks executed,
      R8 ran). NB: Docker not installed, so this is the local Gradle proxy — it
      does NOT run the fdroiddata `prebuild` seds; full end-to-end proof is
      `fdroid build` (§6 / MR CI).
- [x] aapt2 on both APKs (commit `2ae4bb8b3`): package `org.forkgram.classic`,
      label "Forkgram Classic", versionName 12.7.3.0, versionCode 675009 (afat) /
      675008 (afatFd_v8a). On-device smoke test still pending.

## 5. Land on dev + push

- [x] `classic` contains all Classic work (consolidated; 4 squashed commits).
- [x] `git push origin classic` + annotated tag **12.7.3.0** (done 2026-05-28).
      Force-updated 2026-05-28 after a fix — branch and tag now at `2ae4bb8b3`
      (was `e79f6ecf7`).

## 6. fdroiddata submission (NEW package)

- [x] Recipe assembled at `classic/fdroiddata/org.forkgram.classic.yml`.
      NB: the `metadata/_/` fragments are **stale** (r21e ndk, simple prebuild) —
      modeled instead on the **live `org.forkgram.messenger` 12.7.3.0 entry**.
      `subdir: TMessagesProj_App`, gradle `afatFd_v7a`/`afatFd_v8a`, 10-step
      prebuild, versionCode 675007/675008, `commit: 12.7.3.0`. YAML valid.
      Decisions (2026-05-28): author = self (23rd); **auto-update ON**
      (`UpdateCheckMode: Tags`, `VercodeOperation: 100*%c+{7,8}`); **F-Droid-
      signed, NOT reproducible** (no dev-side signing); `fdroid build` left to MR CI.
- [x] Localized store text + images already under `metadata/<locale>/` (31
      languages, `images/icon.png`, screenshots).
- [x] Submitted as **MR !39243** (https://gitlab.com/fdroid/fdroiddata/-/merge_requests/39243)
      — "New App: Forkgram Classic". CI pipeline (`fdroid build`) passed;
      maintainer linsui asked for author info → added `AuthorName/AuthorEmail/AuthorWebSite`
      (23rd) to both classic and `org.forkgram.messenger`. **MERGED 2026-05-28.**

## 7. Post-release

- [ ] Verify installable from the F-Droid client; appears at
      `https://f-droid.org/app/org.forkgram.classic`.
- [ ] Update the `reference-fdroid-checklist` / `project-phase2-progress`
      memories to ground truth.

---

### Pitfalls
- versionCode is append-only — a broken build needs a *new* version, not a
  re-tag.
- Don't tag/push with a dirty submodule — F-Droid builds the tagged tree exactly.
- This is a brand-new F-Droid listing, not an update: there are no existing
  users to auto-update, and the first build may take a full review + build cycle.
