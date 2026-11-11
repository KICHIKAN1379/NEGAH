# NEGAH Coach — Implementation Plan

Status: execution plan for the mission branch; routine implementation and verification continue without a separate "next" prompt. Owner retains final acceptance.
Base: develop at 8bf6828e1d8f574b60440b5930c6a64261748fb0.
Working branch: mission/coach-mvp.
Draft PR: https://github.com/KICHIKAN1379/NEGAH/pull/1

## Delivery principles

- Reuse the existing NEGAH codebase; do not restart from scratch or import AK1 source code.
- Keep the Android app offline-first and single-ring.
- Preserve the five-destination navigation order.
- Implement complete vertical slices: domain rule → persistence → ViewModel/state → UI → tests.
- Every code slice must be linked to an exact commit and a real CI result.
- Do not merge, release, perform destructive data changes or invent unresolved product rules without owner approval.
- A feature is not complete merely because a screen or data model exists.

## Phase A — Establish a verifiable baseline

Existing evidence:
- Kotlin/Compose, Hilt, Room and multi-module Gradle scaffold exists.
- Android CI workflow exists.
- CI run #74 succeeded for commit 624aba036390b33be20cf281fc61eed2c8119e52.
- The CI workflow built a debug APK and passed its configured test tasks.
- The workflow's Refinery test task reported NO-SOURCE; no Refinery tests exist in the current tree.
- No emulator/device run or UI test was performed.
- README build prerequisites do not match the actual wrapper/app/CI configuration.
- The repository tree contains gradle/wrapper/gradle-wrapper.properties but no gradlew script, gradlew.bat or gradle-wrapper.jar.

Tasks:
1. Treat CI run #74 as the first verified baseline, not as proof of visual or device correctness.
2. Reconcile README build prerequisites with actual wrapper, AGP, JDK and SDK configuration.
3. Restore the Gradle wrapper files from a compatible Gradle 8.13 host so local builds can use ./gradlew.
4. Confirm all included Gradle modules and current build/test tasks.
5. Add UI/migration/attendance tests as the relevant flows are implemented.
6. Record baseline build and test results before changing application behavior.

Acceptance:
- CI builds the debug APK and executes all configured unit/DAO test tasks.
- The exact commit SHA and artifact are recorded.
- Documentation matches the configuration actually used.
- A usable Gradle wrapper is committed.
- Manual/device verification is reported separately from CI.

## Phase B — Complete the core data and session workflows

Implementation checkpoint (2026-11-10): session creation and editing are implemented in the mission branch. Editing updates the existing session row and preserves its ID, creation timestamp, completion state and attendance references. Domain tests cover update preservation and blank-title rejection; CI run #90 passed. Session deletion remains intentionally unavailable pending the approved retention policy. Week navigation is now implemented in the session list: the coach can move backward/forward by seven days, with the visible day chips derived from configured class weekdays. Monthly attendance reporting is now implemented with month navigation and separate counts for all five statuses; visible attendance-revision history and CSV export remain.

Existing evidence:
- Member, session and attendance entities/DAOs exist.
- Attendance domain enum and reason validation exist.
- Session/attendance repositories and complete UI workflows were not found in the audited tree.

Tasks:
1. Add/complete repository contracts and Room implementations for sessions and attendance.
2. Route all attendance writes through domain validation; do not allow invalid reasonless records through direct UI paths.
3. Add session create/edit/delete, search (title/member/topic/location), filters (all/upcoming/completed), upcoming-first sort and detail state.
4. Store class weekdays selected in Settings (Saturday through Friday); build the weekly session calendar from those settings.
5. Add weekly/monthly attendance range, daily status summary, member search and a separate attendance-page group filter where appropriate.
6. Support the five approved statuses and required reasons.
7. Keep pending attendance edits separate from committed records; save explicitly using the bottom-page action.
8. Add CSV export compatible with Excel.
9. Add unit, DAO and integration tests, including uniqueness and foreign-key behavior.

Acceptance:
- A coach can create a session, select it, mark each member with one of the five statuses and save.
- Missing reasons block save for absent, excused absence and unrecorded.
- Duplicate member/session records cannot be created.
- History remains intact after edits and archive operations.
- Calendar dates follow configured class weekdays and holidays only.
- Session search/filter/sort and CSV export behave as specified.

## Phase C — Complete member management and profile

Existing evidence:
- Member registration/list/profile-related UI and local persistence exist.
- Member archive is represented in the DAO.
- Profile has partial growth, economy and inventory sections.

Tasks:
1. Audit and complete member list against UX pages 1–4: compact rows, name search and sorting by rank/level, spendable points and name.
2. Do not add a group filter to Members.
3. Complete profile sections: identity, Ganjine, received history, Growth Map, six-axis report card and member export.
4. Ganjine must have four tabs in order: avatar, frame, badge, reward. Badges are earned automatically by level; the coach may change the active badge but cannot grant one.
5. Complete received history preview, full list and filters.
6. Complete monthly/yearly report card with six separate metrics: missions, spendable points, Growth Map, reading, level and diamonds.
7. Separate temporary archive, restore and permanent deletion; protect history.
8. Add member export with profile image, active avatar and active frame.
9. Add birthday reminders three days before; implement automatic birthday rewards only after amount/idempotency rules are verified.

Acceptance:
- Member list and profile match the approved information hierarchy.
- Archive does not erase history; restore returns the same member identity.
- Permanent deletion cannot happen through an ordinary archive action.
- Badges cannot be manually granted or purchased.
- Report card metrics remain independent.
- Export contains only selected/authorized member information.

## Phase D — Implement Home from the approved reference

Existing evidence:
- Home is currently a placeholder.
- UX pages 35–36 define the visual and content baseline.

Tasks:
1. Implement ring identity/profile, home banner and daily quote.
2. Implement weekly pulse as a clickable summary and separate detail view.
3. Include "دستیار مربی" as a configurable quick shortcut, not a separate dashboard card.
4. Implement configurable quick shortcuts, maximum four visible at once.
5. Keep Home concise; do not duplicate today's sessions, attendance or operational lists.

Acceptance:
- Home is data-backed, RTL, responsive and aligned with UX pages 35–36.
- Weekly pulse opens its own detail screen and shows the approved ring-level summaries.
- Empty/new-ring states are useful and do not fabricate sample member data.
- No forbidden "command center", "signal" or today's-operation cards appear.

## Phase E — Complete Progress domains

Existing evidence:
- Progress currently displays growth-map progress.
- Domain models exist for economy and growth map; missions, reading, wheel and competition are not complete vertical slices in the audited tree.

Tasks:
1. Complete member identity/progression and independent XP, points and diamonds.
2. Preserve the level formula: level 0 = 0–19 XP, level 1 = 20–49 XP, each next level adds 30 XP.
3. Complete mission flows with three tabs (mission, hidden/special mission, group puzzle), coach review/approval and notification state.
4. Complete reading records and its independent report-card metric.
5. Complete wheel configuration, result history and idempotent reward posting; do not invent probability weights.
6. Complete within-ring member/group competition; no inter-ring competition.
7. Complete session list and attendance entry points.
8. Add tests for all score/economy calculations and state transitions.

Acceptance:
- Progress has the six specified entry points.
- XP, spendable points and diamonds remain independent.
- Every reward/score change is traceable.
- Unspecified scoring formulas remain blocked for owner decision rather than guessed.
- Backup restore cannot duplicate mission or wheel rewards.

## Phase F — Complete Shop and Refinery

Existing evidence:
- Basic Shop list/purchase flow exists.
- Refinery currently manages growth-map items and catalog items.

Tasks:
1. Audit and complete Shop catalog, item details, balances, purchase feedback and inventory activation.
2. Keep Shop categories to avatar, frame and reward.
3. Keep avatar/frame purchases in diamonds and rewards in spendable points.
4. Keep badges out of the Shop.
5. Complete Refinery configuration for missions, rewards, avatars/frames, wheel and Growth Map.
6. Add edit/disable/delete behavior only where deletion semantics are safe and specified.

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
1. Implement the nine Settings options in the approved order: Refinery, ring profile, sound/background/theme, daily quotes, class weekdays, Home shortcuts, import/export, factory reset, help.
2. Implement deleted-member recovery inside the import/export/data-management area.
3. Implement ring profile, theme, quotes, class weekdays and shortcuts (maximum four).
4. Defer backup encryption and portable recovery-key design until online-service implementation, per owner decision (2026-11-12).
5. Implement user-selected MRT folder access using Android's system picker.
6. Implement JSON/ZIP manual export/import with schema/version validation.
7. Keep automatic backups always enabled and retain at most two versions; verify rotation, restore and failure recovery.
8. Keep automatic backup and manual export as separate complementary flows.
9. Add WorkManager only after backup cadence/trigger policy is approved and documented.

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

## Two-day delivery priority

The previously stated deadline is extremely tight for the full product scope. Prioritize a complete, testable coach workflow first; do not silently delete or mislabel deferred scope.

P0 (first reviewable app):
1. Reliable build/CI baseline.
2. Ring profile and class weekdays.
3. Member list/add/profile.
4. Session creation and five-state attendance with required reasons and explicit save.
5. Home identity, banner, quote, weekly pulse and shortcuts.
6. Local data persistence and backup/export flows; keep encryption and portable key recovery deferred until online-service work.

P1 (complete core experience):
1. Economy/level integrity and Growth Map.
2. Shop and Ganjine.
3. Settings and Refinery.
4. Session/attendance export and member export.

P2 (remaining product completeness):
1. Missions, hidden missions and group puzzle.
2. Reading and six-axis report card.
3. Wheel and within-ring competition.
4. Birthday automation, notifications and remaining refinements.
5. Local automatic backup/restore with MRT two-version retention; backup encryption and portable key recovery remain deferred until online-service work.

This is a delivery order, not a permanent scope reduction. Any unfinished item must remain explicitly marked as incomplete.

## Current execution order

1. Use the successful baseline CI and debug APK as the starting point.
2. Correct product/UX documentation against the approved guide and UX/UX2.
3. Complete Sessions/attendance: creation, editing and previous/next week navigation are implemented; CSV export for the selected month is now implemented; monthly reporting and visible attendance-revision history are also implemented.
4. Complete member profile, Ganjine and archive/restore behavior.
5. Implement Home pages 35–36.
6. Complete remaining Progress domains, Shop and Refinery.
7. Complete backup/restore and release QA.

## Explicit blockers

- No local Android SDK/Gradle execution is available in the current host; use GitHub Actions or another compatible build host.
- The repository does not currently contain Gradle wrapper scripts/JAR; local wrapper use is unavailable until restored.
- README build prerequisites conflict with the actual project configuration.
- Backup encryption/recovery are explicitly deferred until online-service work. The automatic trigger (app backgrounding) and two-version MRT retention are already owner decisions.
- Any unrecorded mission/reading/wheel/competition scoring rule must be reconciled against approved product sources or referred to the owner.
