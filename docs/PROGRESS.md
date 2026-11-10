# NEGAH Coach — Mission Progress

Last updated: 2026-11-10

## Mission and approval boundary

- Product: نگاه مربی (Android, offline-first, single-ring).
- Repository: https://github.com/KICHIKAN1379/NEGAH
- Working branch: mission/coach-mvp
- Base branch: develop
- Base commit SHA: 8bf6828e1d8f574b60440b5930c6a64261748fb0
- Draft PR: https://github.com/KICHIKAN1379/NEGAH/pull/1
- Owner's latest instruction: continue routine product, design, implementation and verification work without requiring a separate "next" command at every step; final acceptance remains with the owner.
- Do not merge to main, publish a release, perform destructive data changes, or silently resolve product/architecture decisions with material impact. Stop and request owner approval for those gates.

## Current phase

- Phase 1 — Repository audit: completed (read-only).
- Phase 2 — Product specification reconciliation: working baseline drafted and reconciled against the approved product guide, UX and UX2; owner final acceptance remains pending.
- Phase 3 — UX reference baseline: source map exists in docs/UX_REFERENCE_MAP.md; primary visual reference is ux.pdf and navigation/mapping index is ux 2.pdf.
- Phase 4 — Technical implementation: first slice (class-days settings) implemented and CI-verified; session/attendance vertical slice is next.
- Architecture baseline: docs/ARCHITECTURE.md.
- Prior implementation snapshot: docs/IMPLEMENTATION_STATUS.md.
- Current delivery plan: docs/IMPLEMENTATION_PLAN.md.
- Current product baseline: docs/PRODUCT_SPEC.md.

## Confirmed repository facts

- develop is 119 commits ahead of main; main currently contains only README.md.
- Audited base: develop at 8bf6828e1d8f574b60440b5930c6a64261748fb0.
- The audited tree contains 80 files, including 60 Kotlin files, Gradle multi-module configuration, Room persistence, Compose UI, and unit/DAO tests.
- The mission branch contains product/plan/progress documentation plus the first source-code slice: class-day settings (domain contract, Room entity/DAO/repository, migration, settings UI and tests). Session/attendance repositories and UI are not yet implemented.
- Draft PR #1 targets develop. It is not merged; main remains untouched.
- No local build/test/emulator/device run was performed in this host.

## Existing implementation observed

- Multi-module Android project using Kotlin, Jetpack Compose, Hilt and Room.
- Main navigation shell has five destinations: Home, Members, Progress, Shop and Settings.
- Members feature includes member registration/list/profile-related flows.
- Progress currently shows growth-map progress.
- Shop currently exposes a basic member/catalog purchase flow.
- Refinery currently manages growth-map items and catalog items.
- Domain rules include five attendance states and required-reason validation for absent, excused absence and unrecorded.
- Initial data models exist for members, sessions, attendance, commitments, economy, growth map and inventory.

## Confirmed gaps / risks from source inspection

- Home is still a placeholder and does not implement the approved Home reference (UX pages 35–36).
- Progress is not yet the full six-entry experience; sessions/attendance, missions, reading, wheel and competition are not implemented as complete flows.
- Settings is currently a minimal entry point to Refinery; most of the nine approved settings options are missing.
- Session and attendance persistence primitives exist, but no complete session/attendance repository + ViewModel + UI workflow was found in the audited tree.
- Member profile has partial growth/economy/inventory sections; four-tab Ganjine, received-history filtering and six-axis report card are incomplete.
- Encrypted backup/restore, MRT retention of at most two versions, and first-run file-access flow are not implemented in the audited tree.
- WorkManager is mentioned in README architecture text, but no WorkManager implementation/dependency was found in the audited files.
- No migration/DAO tests for attendance or sessions were found in the visible test tree.
- README build prerequisites conflict with actual configuration: README says Gradle 9.3.1 / Android API 37, while wrapper and CI use Gradle 8.13 and app/CI target Android API 36.
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

### Baseline before code changes
- CI run #77: success.
- Verified commit: a68a335ffab48d7d4d507a6a3ba7e15436d287ec.
- Debug APK was generated from the unchanged develop application plus documentation updates.

### Current class-days implementation
- CI workflow run: #78.
- Run URL: https://github.com/KICHIKAN1379/NEGAH/actions/runs/42840105205
- Run conclusion: success.
- Verified commit SHA: 0529bb79e99e7675f63cb574fa4efa615eac8c62.
- Build task: successful; BUILD SUCCESSFUL in 3m 26s.
- Tasks executed:
  - :app:assembleDebug — passed.
  - :domain:core:test — passed, including ClassDayTest.
  - :data:local:testDebugUnitTest — passed, including ClassDaysDaoTest.
  - :feature:members:testDebugUnitTest — passed.
  - :feature:refinery:testDebugUnitTest — NO-SOURCE (no tests exist in that module).
- Debug APK artifact: negah-debug-apk, artifact ID 13776600644.
- Extracted APK size: 12,047,611 bytes.
- APK SHA-256: 8571db4d0a636815bdba4b6b6f1610a11bfd4936483c3f61250fc5fe9f5f050e.
- This APK includes the new class-days settings screen and is still a debug build, not a final release.
- No UI tests were executed.
- Manual emulator/device verification: not run.
- Release signing / Play Store readiness: not checked.

## Verification record

- Latest source commit verified by CI: 0529bb79e99e7675f63cb574fa4efa615eac8c62.
- Build and configured unit/DAO test tasks: successful.
- Refinery tests: NO-SOURCE.
- UI/integration tests: not run.
- Manual device verification: not run.
- The host has no local Android SDK/Gradle execution environment; build verification was performed by GitHub Actions.
- Draft PR #1 remains open and unmerged; main remains untouched.

## Next permitted work

1. Implement the session and attendance vertical slice using the saved class weekdays.
2. Add session/attendance repositories, domain validation, atomic save, five states and required reasons.
3. Complete member profile, Ganjine, received history and archive/restore.
4. Implement Home pages 35–36.
5. Continue through remaining Progress domains, Shop, Refinery and backup/restore.
6. Run CI after each meaningful code slice and record exact commit-linked results.
7. Keep merge, release, destructive data operations and material unresolved product decisions behind owner approval.

## Current blocker

The available host has GitHub repository read/write integration but no local Android SDK/Gradle execution environment. The successful baseline APK was produced by GitHub Actions. Future build verification must use GitHub Actions or another compatible build host; until a successful commit-linked run exists for the latest source commit, do not claim that latest commit builds.
