# NEGAH Coach — Mission Progress

Last updated: 2026-11-14

## Mission and approval boundary

- Product: نگاه مربی (Android, offline-first, single-ring).
- Repository: https://github.com/KICHIKAN1379/NEGAH
- Integration branch: `main`.
- New implementation work uses short-lived `mission/*` branches and reviewed pull requests.
- PR #1 (coach MVP), PR #5 (group foundation), PR #7 (independent group economy and AK1 ranking), and PR #8 (wheel data/UI foundation) are recorded as merged in GitHub.
- PR #9 (wheel animation) is open; it is not merged.
- Owner instruction: continue routine product, design, implementation and verification work without requiring a separate "next" command at every step; final acceptance remains with the owner.
- Do not merge, publish a release, perform destructive data changes, or silently resolve material product/architecture decisions. Stop at those approval gates.

## Current phase

- Repository audit and product-spec reconciliation: baseline completed; final product acceptance remains with the owner.
- UX reference baseline: `docs/UX_REFERENCE_MAP.md`; primary visual reference is `ux.pdf` and the mapping index is `ux 2.pdf`.
- Technical implementation: the main branch now includes member, session/attendance, reading, group, group-economy and competition work, plus the wheel foundation described below.
- Current focus: reconcile the stale delivery documentation, review the open wheel-graphics PR, and continue with safe outstanding offline-MVP work.
- Architecture baseline: `docs/ARCHITECTURE.md`.
- Product baseline: `docs/PRODUCT_SPEC.md`.
- Delivery plan: `docs/IMPLEMENTATION_PLAN.md`.

## Confirmed repository facts

- Latest observed main commit: `5f7373bea9117b769c692dc35540936f3af1f098` (merge commit for PR #8).
- PR #7 merged as `176df46e9bc75f909311331120c3435abb33c70d`; it adds independent group economy balances and AK1-compatible group ranking.
- PR #8 is reported merged by GitHub. Its tested head was `9c96883cbff215db4e7aeffec7ca81edfe2be2f0`; Android CI run [#204](https://github.com/KICHIKAN1379/NEGAH/actions/runs/43530430679) completed successfully, including build/test and debug APK upload. An earlier run #203 failed before the successful run.
- PR #9 head is `00f6baf32d2db585e1471e3b31c45dd294a13ffd`; Android CI run [#205](https://github.com/KICHIKAN1379/NEGAH/actions/runs/43546451712) completed successfully, including build/test and debug APK upload. PR #9 remains open.
- The successful CI runs are evidence for their exact PR heads, not for manual device/UI verification.
- No local Android SDK/Gradle, emulator or physical-device run was performed in this host.

## Existing implementation observed

- Multi-module Android project using Kotlin, Jetpack Compose, Hilt and Room.
- Five primary destinations: Home, Members, Progress, Shop and Settings.
- Member profiles include separate XP/points/diamonds balances, ledger history, archive/restore, Ganjine, growth map, reports, reading records and media assets.
- Sessions and attendance include configured class days, editing, five attendance states, reason validation, revision history, monthly reporting and CSV export.
- Progress includes the six-entry hub, reading workflow, member competition and group competition.
- Group economy uses group-owned balances and an append-only ledger; group ranking follows the approved AK1 rule (active groups ordered by their own spendable points; diamonds are informational).
- Ring profile, member avatar and catalog image assets use app-private storage; their binary contents are not yet portable through backup.
- PR #8 added a weighted prize-wheel implementation with a configurable spendable-point spin cost, prize ledger/history and coach settings. Treat this as a product/safety hold, not as an approved pattern to extend.
- PR #9 proposes a visual wheel animation and changes the outcome selection to use the current timestamp modulo total prize weight; see the current review note below.

## Confirmed gaps / risks

- **Wheel hold:** the current design exchanges spendable points for a chance at weighted random prizes. Do not extend or land this paid chance-based mechanic. Any future wheel work must first be redesigned as a free, non-wagering and transparent activity; product changes still require owner review.
- **PR #9 review finding:** its `WheelViewModel.spin` derives the selected weighted outcome from `Math.floorMod(now, total)`, where `now` is the wall-clock timestamp. This is deterministic and predictable rather than a proper random draw. CI success does not validate fairness or product safety.
- Local backup/restore, portable media packaging, MRT automatic retention of at most two versions, and the first-run storage flow remain incomplete. Encryption and portable key recovery remain deferred until online-service work.
- Hidden-mission and group-puzzle mechanics still need explicit product rules; do not infer them.
- Most of the nine approved Settings destinations and final UX alignment remain incomplete.
- UI/instrumentation tests, manual emulator/device verification, dedicated migration coverage, release signing and Play Store readiness remain outstanding.
- The repository has Gradle wrapper properties, but the local wrapper scripts/JAR and a local Android SDK are unavailable in this host; use GitHub Actions for build evidence.

## Product constraints to preserve

- Offline-first; no backend, account system or cloud sync in this version.
- Five main destinations remain Home, Members, Progress, Shop and Settings, in the approved order.
- XP, spendable points and diamonds remain independent; XP is never spendable.
- Attendance has exactly five states; absent, excused absence and unrecorded require a reason.
- Weekly sessions follow the configured class weekdays (Saturday through Friday).
- Members list has no group filter; archive/restore preserves history.
- Badges are earned automatically by level and are never manually granted or purchased.
- Automatic backup retention is at most two versions in MRT; backup encryption/key recovery remain deferred.
- Preserve history and data integrity; no destructive migration or permanent deletion without an explicit, reviewable decision.
- Do not add a command center, Home operation cards, or inter-ring competition.

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

### Session editing slice
- CI run #90: success — https://github.com/KICHIKAN1379/NEGAH/actions/runs/42867008992
- Verified source commit: ee0d4906e7ecab62ace832b822822dee798203d6.
- Added session editing for title, topic, location, notes and date while preserving session ID, created timestamp, completion state and attendance references.
- Added domain tests for preserving identity/state during edits and rejecting blank edit titles.
- :app:assembleDebug, :domain:core:test, :data:local:testDebugUnitTest, :feature:members:testDebugUnitTest and :feature:refinery:testDebugUnitTest all completed successfully; Refinery remains NO-SOURCE.
- Debug APK artifact ID: 13786474856; extracted APK size: 12,162,358 bytes; SHA-256: 96b4cc7a0ff6454edbb810c8ada0a0d3c2c59e2cd3ac3651638ba691385a4bb8.
- This is a debug build, not a release build. UI/instrumentation tests and manual device verification remain unperformed.
- TDD RED was not observed before implementation because this host has no local Gradle/Android SDK; the added tests were run in the post-change CI verification. Do not represent this as a full red-green cycle.

## Monthly attendance reporting slice
- Added a month selector with previous/next month navigation to the Sessions screen.
- The report counts sessions and persisted attendance records within the selected calendar month.
- All five attendance states are shown separately; no attendance percentage or scoring formula is inferred.
- Added domain tests for inclusive date boundaries, missing session references and inverted-range rejection.
- CI verification is pending for this change.

## In-progress session navigation slice
- Added previous/next week controls to the session list.
- The seven-day window starts on Saturday and day chips remain limited to configured class days.
- Moving between weeks clears a stale single-day selection so the list does not appear empty due to a date from another week.
- CI verification is pending for this change.

## Verification record

- Latest source commit verified by CI: ee0d4906e7ecab62ace832b822822dee798203d6.
- Build and configured unit/DAO test tasks: successful.
- Refinery tests: NO-SOURCE.
- UI/integration tests: not run.
- Manual device verification: not run.
- The host has no local Android SDK/Gradle execution environment; build verification was performed by GitHub Actions.
- PR #1 is merged. PR #5, #7 and #8 are also reported merged; PR #9 remains open. The current integration branch is main.

## Next permitted work

1. Keep PR #9 unmerged while its paid chance-based wheel dependency remains unresolved; do not improve or land the paid random-reward mechanic.
2. Prioritize local backup/restore design and implementation: preserve all Room data and media assets, support portable export/import, retain at most two automatic MRT versions, and use a user-mediated document flow. Keep encryption/key recovery deferred.
3. Complete approved mission tabs only after the hidden-mission and group-puzzle rules are explicitly decided.
4. Finish the remaining Settings destinations and verify the existing Home/ring profile flows against UX.
5. Add missing migration, DAO, ViewModel and UI tests; verify every meaningful change on its exact PR head in CI.
6. Keep merge, release, destructive data operations and material unresolved product decisions behind owner approval.

## Current blocker

The GitHub integration can inspect and update repository files, but this host has no local Android SDK/Gradle or device runtime. CI has verified PR #8 head `9c96883cbff215db4e7aeffec7ca81edfe2be2f0` (run #204) and PR #9 head `00f6baf32d2db585e1471e3b31c45dd294a13ffd` (run #205). These results do not establish device/UI correctness. The paid chance-based wheel design and the predictable timestamp-based result in PR #9 are separate review blockers; do not treat green CI as approval of either.

## Latest continuation — attendance revision history
- Added a live revision-history section to the attendance screen, showing the member, previous status, previous reason (when present), and change timestamp.
- Revision history is observed from the existing Room-backed repository; no schema change or data migration was needed.
- CSV export remains the next Sessions task.


## Latest continuation — monthly attendance CSV export
- Added an Android system document picker action to export the selected month's persisted attendance records as CSV.
- Export columns: date, session, member, status and reason. Values are escaped according to CSV quoting rules, and a UTF-8 BOM is written for spreadsheet compatibility.
- The export is user-initiated and writes only to the destination selected in the Android document picker; no storage permission or backend is required.
- CI verification is pending for this commit.


## Latest continuation — member archive and restore
- Added a separate archived-members view with search/sort reuse; the active members list remains free of group filters.
- Added confirmation before archiving. Archive changes only the member archived flag and timestamp; it does not delete the member or history.
- Added restore from the archived list, preserving the original member ID and stored balances/history.
- CI verification is pending for this commit.


## Latest continuation — received-history filters
- Added independent history filters for all transactions, XP, spendable points and diamonds in the member profile.
- Filters operate on the existing ledger stream only; no balance or ledger data is modified.
- Added ViewModel coverage for currency filtering and returning to the full history.
- CI verification is pending for this commit.

## Latest continuation — birthday profile edit and image-asset audit (2026-11-11)
- Optional Jalali birthday editing is now wired in the member profile; clearing the field persists null. The latest profile-edit test commit is `2f9dfdd864edec46d3f9ad630095b71e95135fa8`.
- CI run #123 for that exact commit was still pending at the latest check; the workflow returned no jobs yet. Do not treat it as passed.
- Birthday reward logic currently checks active-member stream emissions and writes an idempotent 30-point ledger entry for the Jalali year. A scheduled birthday reminder (three days before) is not implemented, and the reward check is not a durable midnight scheduler.
- Media audit: member avatar bytes are now associated through `media_assets` using MEMBER/AVATAR ownership (without changing the existing member model or its avatar_id/catalog semantics). Catalog items still have no image association, and persisted ring profile image/banner flows remain unimplemented. Portable image backup is not implemented.
- Required next media slice: define an app-private asset store with stable logical IDs and explicit owner/category metadata; copy selected content into managed storage rather than retaining temporary picker URIs; include binary assets in full-ring backup/export and remap local paths on restore. Keep encryption/recovery deferred until the online-service phase, per the owner decision.
- Owner backup decisions now recorded: portable to a new phone; attempt automatic backup when the app moves to the background; retain at most two automatic versions in MRT. Do not claim guaranteed execution after force-stop, crash, process kill or shutdown.


## Latest continuation — durable image asset foundation
- Added Room `media_assets` metadata with a unique owner-type/owner-ID/role association and a version 8→9 migration.
- Added app-private binary storage for JPEG, PNG and WebP imports. The storage layer copies bytes out of temporary picker URIs, uses generated filenames (not user-supplied paths), records byte size and SHA-256, and validates paths before opening/deleting.
- Added an injectable manager to import, replace, open and remove an asset associated with a member, ring or catalog item.
- Added JVM tests for byte round-trip, metadata, deletion, empty input, unsupported MIME types and path traversal rejection.
- The member profile now invokes this storage for avatar selection and display through a domain repository boundary. Catalog and ring image screens are not yet connected, and portable backup/restore is not implemented.
- CI must verify Room schema generation, migration wiring, Hilt construction and tests before this slice is considered validated.


## Latest continuation — media database wiring and backup scope (2026-11-12)
- Corrected the image-storage foundation so it is actually registered with Room: schema version 9 includes `media_assets`, its unique owner/role index, and the 8→9 migration.
- Registered `MediaAssetDao` with the database and Hilt provider, and added the 8→9 migration to the production Room builder. Without these registrations, the previously added entity/DAO were not part of the live database and the manager could not be constructed.
- Owner decision: backup encryption and portable key/recovery design are deferred until online-service work. No encryption implementation is in scope for the offline MVP.
- Member-profile avatar selection/display is now connected through the domain repository boundary. Catalog/ring image screens and portable backup remain unimplemented. CI verification is required before considering this slice validated.


## Latest continuation — member profile avatar flow (2026-11-12)
- Added a domain-level `MemberAvatarRepository` contract so the Members feature does not depend directly on the data/local implementation.
- Bound the Room-backed media manager through Hilt. The profile's Android content picker copies the selected image into app-private storage; it does not retain the temporary picker URI.
- The profile loads and displays the saved avatar and supports replacing it. The UI decodes a downsampled preview to limit display memory use.
- Added ViewModel tests for loading an existing avatar and saving/reloading a newly selected avatar. CI has not yet completed for this change.
- Ring image/banner and catalog image selection/display remain future work. No image bytes are included in backup yet.


## Avatar-flow follow-up
- Added a regression test ensuring switching from a member with a saved avatar to a member without one does not display the previous member's image.


## Documentation alignment — backup encryption deferral
- Reconciled README, architecture, implementation plan/status and progress notes with the owner's decision: encryption and portable key recovery are deferred until online-service work.
- Local backup/restore and the two-version MRT retention requirement remain product work; this documentation change does not implement backup.


## Dependency-injection verification follow-up — member avatar (2026-11-12)
- Added the missing Hilt binding from the domain-level `MemberAvatarRepository` contract to the Room/private-file-backed `MediaAssetManager`.
- This closes the runtime graph gap between the MembersViewModel and local media persistence. The feature module remains dependent only on the domain contract.
- Avatar picker, private-file persistence, profile preview, and ViewModel regression tests are present; CI is still queued, so compilation and test execution are not yet confirmed.


## Member avatar management completion (2026-11-12)
- Added an explicit remove-avatar operation to the domain contract and local media manager.
- Added a profile action to remove the currently displayed avatar, clear the persisted media asset, and immediately clear the in-memory preview.
- Added a ViewModel regression test for persisted and visible avatar removal.
- Replacing, loading, and removing a member avatar now have explicit profile-level flows; ring and catalog images remain separate future integrations.


## Ring profile settings — persistent metadata slice (2026-11-12)
- Added a domain contract and Room-backed persistence for the ring's name and optional display username.
- Added schema migration 9→10 and Hilt bindings for the repository.
- Added a dedicated Settings destination and editable screen; the profile is stored locally and no example ring identity is prefilled.
- Ring image and Home banner selection/display are not yet connected; they remain the next part of this profile slice.
- CI is queued and has not validated the migration or Android build yet.

- Added Room/Robolectric tests for an empty first-run ring profile, persistence after repository recreation, and the optional username remaining null when unset.


## Ring profile media integration (2026-11-12)
- Connected ring image and banner roles to a domain media contract and the private-file/Room media manager.
- Added image/banner selection, persisted previews, and removal controls to ring profile settings.
- Kept text metadata in its existing repository; image bytes remain separate assets owned by the stable ring profile key.
- Added a Robolectric test for independent image/banner persistence and removal.
- Backup packaging of media files and remapping on restore remain pending; selecting images does not yet make backups portable.
- CI must validate Hilt, Compose, Room, and test integration.

- Added the missing Hilt binding for the new `RingProfileMediaRepository` contract to the existing local media manager. The feature screen can now receive the media contract without depending on the data module.


## Catalog image integration (2026-11-12)
- Added a domain media contract and local asset-manager implementation for catalog item images, keyed by catalog item ID.
- Added image selection/removal to the Refinery catalog editor and image previews in the Shop.
- Catalog image bytes remain separate from catalog metadata and are stored in the private media directory with Room ownership metadata.
- Added a persistence-isolation test for two catalog item images.
- Backup inclusion of media files remains pending; this is not yet a portable backup solution.


## Mission scope clarification — 2026-11-12
- Owner confirmed that members have no capabilities in this application; all member-related data and operations are controlled by the coach.
- Owner confirmed that the Missions icon opens one destination containing three tabs: مأموریت، مأموریت ویژه (مخفی)، پازل گروهی.
- Regular mission workflow is coach-defined and coach-recorded: create, assign, record progress, approve completion, and award a preconfigured reward once.
- Reward currency and amount are explicitly selected by the coach; no guessed formulas.
- Hidden and group-puzzle tabs remain part of the destination. Their unique mechanics are not inferred; they require their own approved rules before specialized behavior is added.
- No member login, submission, request, or self-service flow is permitted.

## Members completion slice — 2026-11-13
- Branch mission/members-complete (draft PR #4): Solar Hijri birthday reminder (3 days before, WorkManager + notification permission at first launch), book-reading model with Room migration 13→14, immutable growth-progress history, reading axis wired into member report/PDF, monthly trend charts in the member PDF.
- CI evidence: run 43343026664 succeeded on aeafcae3ef1f8d1c7816f60f58912cdcdd4bcb48 (build + domain/data/members/refinery unit tests). No device or UI test performed.
- Next: Progress menu (identity, missions, reading entry form/ranking, wheel, competition, sessions) per mrt-menu.md and ux_2.pdf.

## Progress hub slice — 2026-11-13
- Progress now opens a six-entry hub (identity, missions, reading, wheel, competition, sessions) with back handling; reading has ranking + entry form + recent list; competition ranks members by XP inside the ring (group ranking unavailable: no group model); wheel is an explicit not-implemented screen.
- CI evidence: run 43343891178 succeeded on 4c1b0c2e0cfd13be803c26b7f1e3162c5bee0864. No device/UI test performed.
- Next: wheel (Room model, refinery settings, cost in spendable points, spin ledger), mission tabs (hidden mission, six-piece group puzzle), identity page per UX, shop details.
