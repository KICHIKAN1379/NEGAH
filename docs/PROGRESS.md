# NEGAH Coach — Mission Progress

Last updated: 2026-11-11

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
- Phase 4 — Technical implementation: class-days settings, session creation/editing and the first attendance workflow are implemented and CI-verified; remaining product requirements continue below.
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
- Session creation and editing, completion state, previous/next week navigation, configured-class-day calendar, session filtering/search and attendance entry are now connected to Room. Session edits preserve the session ID, creation timestamp, completion state and attendance references; a separate audit log for session-field edits is still absent.
- Attendance revisions are stored transactionally when a previously saved status/reason/confirmation changes.
- Session deletion is intentionally not exposed in the UI pending an explicit retention policy; the DAO-level foreign-key constraint prevents deletion when attendance exists.

## Confirmed gaps / risks

- Home now has a data-backed weekly pulse, seven-day attendance summary, recent sessions and configurable shortcuts. Ring identity/profile, ring image, Home banner and daily quote are still missing, so the approved Home reference (UX pages 35–36) is not complete.
- Progress is not yet the full six-entry experience; missions, reading, wheel and competition are not implemented as complete flows.
- Settings is currently a minimal entry point to Refinery and class-day settings; most of the nine approved settings options are missing.
- Sessions/attendance now include a month-navigable report with separate counts for all five attendance states, selected-month CSV export, and a live revision-history view; a group filter waits for the group data model.
- The Sessions screen now supports previous/next week navigation; its seven-day window starts on Saturday and day chips remain limited to configured class days.
- Member profile now includes growth/economy/inventory sections, four Ganjine tabs, automatic level badges, received-history filters, a six-axis report card, and active/archive switching with restore. Member profile avatar selection and display now use app-private image storage; catalog and ring image flows and frame rendering remain unsupported.
- Backup/restore, MRT retention of at most two versions, and first-run file-access flow are not implemented. Backup encryption and portable key recovery are explicitly deferred until online-service work.
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
- Members list has no group filter; attendance may have its own group filter. Members can be archived without deletion and restored from a separate archived list.
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
- Canonical draft PR #1 remains open and unmerged; temporary CI verification PRs #2 and #3 were closed; main remains untouched.

## Next permitted work

1. Re-check CI for the birthday profile-edit change and keep the exact commit/result linked.
2. Add durable image-asset storage and image selection/display for member, ring and catalog assets; later include actual bytes in portable backup/restore.
3. Complete ring profile and Home identity/banner/daily-quote flows.
4. Complete the remaining Settings options and the missing Progress domains (missions, reading, wheel and competition) without inventing unresolved scoring rules.
5. Add group filter only when the group data model and UX rules are implemented.
6. Implement the local backup/restore flow when scheduled in the offline MVP plan; keep encryption and portable key recovery deferred until online-service work, with the agreed MRT two-version retention and background trigger.
7. Run CI after each meaningful code slice and record exact commit-linked results.
8. Keep merge, release, destructive data operations and material unresolved product decisions behind owner approval.

## Current blocker

The available host has GitHub repository read/write integration but no local Android SDK/Gradle execution environment. GitHub Actions has verified the current source commit ee0d4906e7ecab62ace832b822822dee798203d6. The repository still lacks the Gradle Wrapper scripts/JAR. UI tests, manual device verification, dedicated migration testing, release signing and Play Store readiness remain outstanding; do not claim these are complete.


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
