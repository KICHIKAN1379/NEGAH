# NEGAH Coach — Mission Progress

Last updated: 2026-11-10

## Mission and approval boundary

- Product: نگاه مربی (Android, offline-first, single-ring).
- Repository: https://github.com/KICHIKAN1379/NEGAH
- Working branch: `mission/coach-mvp`
- Base branch: `develop`
- Base commit SHA: `8bf6828e1d8f574b60440b5930c6a64261748fb0`
- Owner's latest instruction: continue routine product, design, implementation and verification work without requiring a separate "next" command at every step; final acceptance remains with the owner.
- Do not merge to `main`, publish a release, perform destructive data changes, or silently resolve product/architecture decisions with material impact. Stop and request owner approval for those gates.

## Current phase

- Phase 1 — Repository audit: completed (read-only).
- Phase 2 — Product specification reconciliation: in progress.
- UX source map: available in `docs/UX_REFERENCE_MAP.md`; primary visual reference is `ux.pdf`, navigation/mapping index is `ux 2.pdf`.
- Architecture baseline: `docs/ARCHITECTURE.md`.
- Implementation snapshot: `docs/IMPLEMENTATION_STATUS.md`.

## Confirmed repository facts

- `develop` is 119 commits ahead of `main`; `main` currently contains only `README.md`.
- Audited base: `develop` at `8bf6828e1d8f574b60440b5930c6a64261748fb0`.
- The audited tree contains 80 files, including 60 Kotlin files, Gradle multi-module configuration, Room persistence, Compose UI, and unit/DAO tests.
- No open pull request was present at audit time.
- GitHub combined status returned no status checks for the audited SHA. This is not evidence of a successful build.
- No local build, test, emulator, or device run was performed in this environment.

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

- Home is still a placeholder and does not implement the approved home reference (UX pages 35–36).
- Progress is not yet the full six-destination experience; sessions/attendance, missions, reading, wheel and competition are not implemented as complete flows.
- Settings is currently a minimal entry point to Refinery; most settings and Refinery areas are missing.
- Session and attendance persistence primitives exist, but no complete session/attendance repository + ViewModel + UI workflow was found in the audited tree.
- Encrypted backup/restore, MRT retention of at most two versions, and first-run file-access flow are not implemented in the audited tree.
- WorkManager is mentioned in README architecture text, but no WorkManager implementation/dependency was found in the audited files.
- No migration/DAO tests for attendance or sessions were found in the visible test tree.
- README build prerequisites conflict with actual configuration: README says Gradle 9.3.1 / Android API 37, while wrapper and CI use Gradle 8.13 and app/CI target Android API 36. Reconcile documentation against verified build configuration.
- CI workflow exists, but no successful run/status for the audited commit has been confirmed.

## Product constraints to preserve

- Offline-first; no backend, account system or cloud sync in this version.
- Five main destinations in this exact order: Home, Members, Progress, Shop, Settings.
- XP, spendable points and diamonds remain independent currencies.
- Attendance states: present, absent, excused absence, late, unrecorded.
- Reason is mandatory for absent, excused absence and unrecorded.
- Weekly session calendar must be derived from class days configured in Settings; do not invent a weekly calendar independent of those settings.
- Automatic backups retain at most two versions in the MRT folder.
- First-run file access must use an Android-appropriate user-mediated storage flow.
- Preserve history and data integrity; no destructive migration or permanent deletion without an explicit, reviewable decision.

## Verification record

- Build result: unknown / not verified.
- Unit tests: not run in this host session.
- DAO / integration tests: not run in this host session.
- Manual emulator/device verification: not run.
- CI: no status checks returned for base SHA at audit time.

## Next permitted work

1. Reconcile the approved Coach-only product scope against the product-flow/database document, UX and UX2 references, and existing code.
2. Produce a prioritized implementation matrix and acceptance criteria.
3. Continue implementation in this working branch, in small reviewable slices.
4. Run CI after each meaningful slice and record exact commit-linked results.
5. Keep merge, release, destructive data operations and material unresolved product decisions behind owner approval.

## Current blocker

The available host has GitHub repository read/write integration but no local Android SDK/Gradle execution environment. Build verification must therefore come from GitHub Actions or a compatible build host; until a successful commit-linked run exists, do not claim the app builds.
