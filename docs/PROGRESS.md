# NEGAH Coach — Mission Progress

Last updated: 2026-11-10

## Mission and approval boundary

- Product: نگاه مربی (Android, offline-first, single-ring).
- Repository: https://github.com/KICHIKAN1379/NEGAH
- Working branch: mission/coach-mvp
- Base branch: develop
- Base commit SHA: 8bf6828e1d8f574b60440b5930c6a64261748fb0
- Canonical draft PR: https://github.com/KICHIKAN1379/NEGAH/pull/1
- Owner's instruction: continue routine product, design, implementation and verification work without requiring a separate "next" command at every step; final acceptance remains with the owner.
- Do not merge to main, publish a release, perform destructive data changes, or silently resolve product/architecture decisions with material impact. Stop and request owner approval for those gates.

## Current phase

- Phase 1 — Repository audit: completed (read-only).
- Phase 2 — Product specification reconciliation: working baseline drafted and reconciled against the approved product guide, UX and UX2; owner final acceptance remains pending.
- Phase 3 — UX reference baseline: source map exists in docs/UX_REFERENCE_MAP.md; primary visual reference is ux.pdf and navigation/mapping index is ux 2.pdf.
- Phase 4 — Technical implementation: class-days settings and the first sessions/attendance workflow are implemented and CI-verified; remaining product requirements continue below.
- Architecture baseline: docs/ARCHITECTURE.md.
- Prior implementation snapshot: docs/IMPLEMENTATION_STATUS.md.
- Current delivery plan: docs/IMPLEMENTATION_PLAN.md.
- Current product baseline: docs/PRODUCT_SPEC.md.

## Confirmed repository facts

- develop is 119 commits ahead of main; main currently contains only README.md.
- Audited base: develop at 8bf6828e1d8f574b60440b5930c6a64261748fb0.
- The audited tree contains 80 files, including 60 Kotlin files, Gradle multi-module configuration, Room persistence, Compose UI, and unit/DAO tests.
- The mission branch contains the reconciled product documentation plus class-day configuration and a session/attendance vertical slice.
- Draft PR #1 targets develop. It is not merged; main remains untouched.
- No local build/test/emulator/device run was performed in this host; the verified builds were executed by GitHub Actions.

## Existing implementation observed

- Multi-module Android project using Kotlin, Jetpack Compose, Hilt and Room.
- Main navigation shell has five destinations: Home, Members, Progress, Shop and Settings.
- Members feature includes member registration/list/profile-related flows.
- Progress currently shows growth-map progress and now links to Sessions.
- Shop currently exposes a basic member/catalog purchase flow.
- Refinery currently manages growth-map items and catalog items.
- Domain rules include five attendance states and required-reason validation for absent, excused absence and unrecorded.
- Session creation, completion state, weekly class-day calendar, session filtering/search and attendance entry are now connected to Room.
- Attendance revisions are stored transactionally when a previously saved status/reason/confirmation changes.

## Confirmed gaps / risks

- Home is still a placeholder and does not implement the approved Home reference (UX pages 35–36).
- Progress is not yet the full six-entry experience; missions, reading, wheel and competition are not implemented as complete flows.
- Settings is currently a minimal entry point to Refinery and class-day settings; most of the nine approved settings options are missing.
- Sessions/attendance still need session editing, previous/next week and month navigation, date-range reports, group filter when group membership is defined, CSV export and a UI for inspecting attendance revisions.
- The current weekly strip shows the current Saturday–Friday week and only configured class days; it does not yet provide week navigation.
- Member profile has partial growth/economy/inventory sections; four-tab Ganjine, received-history filtering and six-axis report card are incomplete.
- Encrypted backup/restore, MRT retention of at most two versions, and first-run file-access flow are not implemented.
- WorkManager is mentioned in README architecture text, but no WorkManager implementation/dependency was found in the audited files.
- DAO tests now cover class-day persistence and session/attendance persistence, update revisions and deletion restrictions. The 5→6 database migration has not yet been exercised by a dedicated migration test.
- The repository contains gradle/wrapper/gradle-wrapper.properties but no gradlew script, gradlew.bat or gradle-wrapper.jar; local wrapper builds are not currently available.

## Product constraints to preserve

- Offline-first; no backend, account system or cloud sync in this version.
- Five main destinations in this exact order: Home, Members, Progress, Shop, Settings.
- XP, spendable points and diamonds remain independent currencies.
- Level 0 is 0–19 XP; level 1 is 20–49 XP; each next level adds 30 XP.
- Attendance states: present, absent, excused absence, late, unrecorded.
- Reason is mandatory for absent, excused absence and unrecorded.
- Weekly session calendar must be based on class days configured in Settings (Saturday through Friday).
- Members list has no group filter; attendance may have its own group filter.
- Ganjine has four tabs: avatar, frame, badge, reward. Badges are earned automatically by level; no manual granting or purchase.
- Shop has three categories: avatar, frame and reward. Avatar/frame use diamonds; rewards use spendable points.
- Automatic backups retain at most two versions in the MRT folder; this newer owner decision supersedes older guide snapshots mentioning a larger count.
- First-run file access must use an Android-appropriate user-mediated storage flow.
- Preserve history and data integrity; no destructive migration or permanent deletion without an explicit, reviewable decision.
- Do not add "command center", "signal", separate Home cards for today's operations, or inter-ring competition.

## Baseline and current verification

### Baseline before mission source changes
- CI run #77: success.
- Verified commit: a68a335ffab48d7d4d507a6a3ba7e15436d287ec.

### Class-days settings slice
- CI run #78: success.
- Run URL: https://github.com/KICHIKAN1379/NEGAH/actions/runs/42840105205
- Verified source commit: 0529bb79e99e7675f63cb574fa4efa615eac8c62.
- Added class-day domain contract, Room entity/DAO/repository, version 4→5 migration, Settings screen and tests.

### Sessions and attendance slice
- CI run #82: success.
- Run URL: https://github.com/KICHIKAN1379/NEGAH/actions/runs/42844172165
- Verified source commit: 5cbb3421570adfceb693cf743c675d4dda4479ae.
- Build task: BUILD SUCCESSFUL in 3m 31s.
- Tasks executed:
  - :app:assembleDebug — passed.
  - :domain:core:test — passed, including attendance/session use-case tests and existing domain tests.
  - :data:local:testDebugUnitTest — passed, including class-day DAO and session/attendance revision DAO tests.
  - :feature:members:testDebugUnitTest — passed.
  - :feature:refinery:testDebugUnitTest — NO-SOURCE.
- Debug APK artifact ID: 13777148849.
- Extracted APK size: 12,162,358 bytes.
- APK SHA-256: ce54b1760e745b362266381164f41e314f6ce98af4cdaa8b7ab47538b884d3b4.
- This is a debug build, not a release APK.
- UI/instrumentation tests: not run.
- Manual emulator/device verification: not run.
- Dedicated 5→6 migration test: not run.
- Release signing / Play Store readiness: not checked.

## Verification record

- Latest source commit verified by CI: 5cbb3421570adfceb693cf743c675d4dda4479ae.
- Build and configured unit/DAO test tasks: successful.
- Refinery tests: NO-SOURCE.
- UI/integration tests: not run.
- Manual device verification: not run.
- The host has no local Android SDK/Gradle execution environment; build verification was performed by GitHub Actions.
- Canonical draft PR #1 remains open and unmerged; temporary CI verification PRs #2 and #3 were closed; main remains untouched.

## Next permitted work

1. Complete Sessions: edit sessions, week navigation, month/date-range view, CSV export and visible attendance-revision history.
2. Add group filter when the group data model and UX rules are implemented.
3. Complete member profile, Ganjine, received history and archive/restore.
4. Implement Home pages 35–36.
5. Continue through remaining Progress domains, Shop and the nine Settings options.
6. Implement encrypted backup/restore, MRT two-version retention and first-run storage permission flow.
7. Run CI after each meaningful code slice and record exact commit-linked results.
8. Keep merge, release, destructive data operations and material unresolved product decisions behind owner approval.

## Current blocker

The available host has GitHub repository read/write integration but no local Android SDK/Gradle execution environment. GitHub Actions has verified the current source commit. The repository still lacks the Gradle Wrapper scripts/JAR. UI tests, manual device verification, dedicated migration testing, release signing and Play Store readiness remain outstanding; do not claim these are complete.
