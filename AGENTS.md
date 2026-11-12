# NEGAH — Agent Handoff Guide

This file is the entry point for AI coding agents and contributors.

## Read first
1. `docs/OPERATIONS_REPORT.md` — implemented work, gaps, validation status and handoff notes.
2. `docs/PRODUCT_SPEC.md` — product decisions and functional requirements.
3. UX source files referenced by the owner; `ux.pdf` is primary and `ux2` maps screens. If a screen is absent from UX2, use UX as the source of truth.

## Repository workflow
- Repository: `KICHIKAN1379/NEGAH`.
- Active development branch before final promotion: `mission/coach-mvp`.
- `main` is the handoff branch intended to show the complete current project state.
- Fetch the latest branch head before editing. Never overwrite a newer remote commit.
- Use small, focused commits and preserve Room data through explicit migrations.
- Run GitHub Actions CI and report the exact commit and run URL. Never claim a build, APK, device test, or UI test succeeded unless its evidence exists.
- Do not merge or release additional work without owner approval.

## Product invariants
- Product name: نگاه (NEGAH). Current Android app label/package identity may still use Coach naming in places.
- The app is coach-operated. Members have no app-side capabilities: no member login, submissions, requests, approvals, or self-editing.
- All member records, attendance, missions, balances, inventory and progress are controlled by the coach.
- Currency balances are independent: XP, spendable points, diamonds. XP is not spendable.
- Birthday reward: 30 diamonds, not points.
- Attendance statuses: حاضر، غیبت، غیبت موجه، تأخیر، بدون ثبت. A reason is mandatory for غیبت، غیبت موجه and بدون ثبت.
- The calendar is Solar Hijri. Weekly sessions follow the weekdays configured by the coach.
- Home must not show missions, mission titles, member names or member photos in the weekly pulse.
- Missions are coach-managed. Hidden missions are revealed through real-world actions; group puzzle is six pieces in a rectangular frame with a separate image per group.
- Do not invent data, events, metrics, rewards or completion states. If a data source is absent, show unavailable/no data.
- Do not add cloud sync, accounts or backend services unless the owner explicitly changes scope.

## Data safety
- Preserve historical records and transaction ledgers.
- Never delete a session or member history to simplify UI work.
- All schema changes require forward Room migrations and migration tests where practical.
- Economy corrections require a reason, must prevent negative balances, and must be auditable/idempotent.

## Definition of done
- Requirements match product spec and UX.
- Relevant unit/DAO/ViewModel tests pass.
- Latest GitHub Actions build/test passes on the exact commit being reported.
- No fabricated claim of manual Android-device verification.
- Owner retains final approval for release, merge strategy changes, and destructive data operations.
