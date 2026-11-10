# NEGAH Coach — Implementation Plan

Status: execution plan for the mission branch; implementation proceeds in small, reviewable slices.
Base: develop at 8bf6828e1d8f574b60440b5930c6a64261748fb0.
Working branch: mission/coach-mvp.

## Delivery principles

- Reuse the existing NEGAH codebase; do not restart from scratch.
- Keep the Android app offline-first and single-ring.
- Preserve the five-destination navigation order.
- Implement complete vertical slices: domain rule → persistence → ViewModel/state → UI → tests.
- Every code slice must be linked to an exact commit and a real CI result.
- Do not merge, release, perform destructive data changes or invent unresolved product rules without owner approval.

## Phase A — Establish a verifiable baseline

Existing evidence:
- Kotlin/Compose, Hilt, Room and multi-module Gradle scaffold exists.
- Android CI workflow exists, but no successful commit-linked result has been confirmed.
- README build prerequisites do not match the wrapper/app/CI configuration.

Tasks:
1. Trigger and observe CI on the current baseline.
2. Reconcile README build prerequisites with the actual wrapper, AGP, JDK and SDK configuration.
3. Confirm all included Gradle modules and current build/test tasks.
4. Record baseline build and test results before changing application behavior.

Acceptance:
- CI builds the debug APK and executes all configured unit/DAO test tasks.
- The exact commit SHA and artifact are recorded.
- Documentation matches the configuration actually used.

## Phase B — Complete the core data and session workflows

Existing evidence:
- Member, session and attendance entities/DAOs exist.
- Attendance domain enum and reason validation exist.
- Session/attendance repositories and complete UI workflows were not found in the audited tree.

Tasks:
1. Add/complete repository contracts and Room implementations for sessions and attendance.
2. Route all attendance writes through domain validation; do not allow invalid reasonless records through direct UI paths.
3. Add session creation/editing and list/detail state.
4. Build the attendance calendar from class weekdays configured in Settings.
5. Support the five approved statuses and required reasons.
6. Keep pending attendance edits separate from committed records; save atomically.
7. Add unit, DAO and integration tests, including uniqueness and foreign-key behavior.

Acceptance:
- A coach can create a session, select it, mark each member with one of the five statuses and save.
- Missing reasons block save for absent, excused absence and unrecorded.
- Duplicate member/session records cannot be created.
- History remains intact after edits and archive operations.
- Calendar dates follow configured class weekdays only.

## Phase C — Complete member management and profile

Existing evidence:
- Member registration/list/profile-related UI and local persistence exist.
- Member archive is represented in the DAO.

Tasks:
1. Audit current member list/profile behavior against UX pages 1–4.
2. Complete search and approved sorting; do not add a group filter.
3. Complete profile sections: identity, growth map, report card, received items and history.
4. Separate temporary archive, restore and permanent deletion; protect history.
5. Add member export with explicit selection and privacy boundaries.
6. Add ViewModel and persistence tests for lifecycle, selection and archive behavior.

Acceptance:
- Member list and profile match the approved navigation and information hierarchy.
- Archive does not erase history; restore returns the same member identity.
- Permanent deletion cannot happen through an ordinary archive action.
- Export contains only selected/authorized member information.

## Phase D — Implement Home from the approved reference

Existing evidence:
- Home is currently a placeholder.
- UX pages 35–36 define the visual and content baseline.

Tasks:
1. Implement ring identity/profile and quote.
2. Implement weekly pulse as a clickable summary and separate detail view.
3. Implement the approved Coach Assistant entry.
4. Implement configurable quick shortcuts.
5. Keep Home concise; do not duplicate today's sessions, attendance or operational lists.

Acceptance:
- Home is data-backed, RTL, responsive and aligned with UX pages 35–36.
- Weekly pulse opens its own detail screen.
- Empty/new-ring states are useful and do not fabricate sample member data.

## Phase E — Complete Progress domains

Existing evidence:
- Progress currently displays growth-map progress.
- Domain models exist for economy and growth map; missions, reading, wheel and competition are not complete vertical slices in the audited tree.

Tasks:
1. Complete member identity/progression and independent XP, points and diamonds.
2. Complete mission flows using only approved mission rules.
3. Complete reading records and rankings where specified.
4. Complete wheel configuration, cost and winner history where specified.
5. Complete within-ring member/group competition; no inter-ring competition.
6. Complete session list and attendance entry points.
7. Add tests for all score/economy calculations and state transitions.

Acceptance:
- Progress has the six specified entry points.
- XP, spendable points and diamonds remain independent.
- Every reward/score change is traceable.
- Unspecified scoring formulas remain blocked for owner decision rather than guessed.

## Phase F — Complete Shop and Refinery

Existing evidence:
- Basic Shop list/purchase flow exists.
- Refinery currently manages growth-map items and catalog items.

Tasks:
1. Audit and complete Shop catalog, item details, balances, purchase feedback and inventory activation.
2. Keep avatar/frame purchases in diamonds and rewards in spendable points.
3. Keep badges out of the Shop.
4. Complete Refinery configuration for missions, rewards, avatars/frames, wheel and growth map.
5. Add edit/disable/delete behavior only where deletion semantics are safe and specified.

Acceptance:
- Purchase is atomic and idempotent.
- Insufficient balance, unavailable item and already-owned states are handled.
- Every successful purchase updates inventory and ledger together.
- Configuration changes affect only the intended ring-local catalog/rules.

## Phase G — Settings, backup and restore

Existing evidence:
- Settings is currently a minimal entry point to Refinery.
- Backup/restore and MRT retention are not implemented in the audited tree.

Tasks:
1. Implement ring profile, theme, quotes, class weekdays, shortcuts and help.
2. Implement deleted-member recovery and factory reset behind explicit confirmation.
3. Define portable backup encryption and recovery before writing the final backup flow.
4. Implement user-selected MRT folder access using Android's system picker.
5. Implement JSON/ZIP export/import with schema/version validation.
6. Keep at most two automatic backups; verify rotation, restore and failure recovery.
7. Add WorkManager only after backup cadence/trigger policy is approved and documented.

Acceptance:
- All settings persist across app restarts.
- Backup/restore round-trips the approved data without leaking credentials or secrets.
- Invalid/incompatible backups are rejected without modifying live data.
- Automatic backup rotation never retains more than two versions.
- Restore and factory reset have explicit confirmation and safe failure behavior.

## Phase H — Release readiness

Tasks:
1. Run unit, DAO, integration and UI tests.
2. Run migration and backup/restore tests against representative prior schemas.
3. Compare screenshots with approved UX references on small and large screens.
4. Run the app on an emulator or physical Android device.
5. Verify offline behavior, RTL, accessibility, empty/error states and data retention.
6. Produce a debug APK for owner review, then a release candidate only after final approval.
7. Document known issues, installation steps and remaining scope.

Acceptance:
- Build, unit tests, integration/UI tests, manual device verification and visual review are reported separately.
- APK is tied to the exact reviewed commit.
- No merge to main or public release occurs without owner approval.

## Current execution order

1. Establish a reliable CI baseline.
2. Complete Settings class weekdays and session/attendance vertical slice.
3. Complete member profile and archive/restore behavior.
4. Implement Home pages 35–36.
5. Complete remaining Progress domains, Shop and Refinery.
6. Complete backup/restore and release QA.

## Explicit blockers

- CI status for the current baseline has not been confirmed.
- Local Android SDK/Gradle execution is unavailable in the current host; use GitHub Actions or another compatible build host.
- Backup encryption/recovery and cadence require a concrete design before implementation.
- Any unrecorded mission/reading/wheel/competition scoring rule must be reconciled against approved product sources or referred to the owner.
