# نگاه مربی — Product Specification (working baseline)

Status: working baseline for implementation; owner retains final acceptance.
Scope: the Android coach application only. This document does not define the separate web product "نگاه راهبر".

## 1. Product purpose and boundaries

نگاه مربی is an offline-first Android application for a coach to manage one تربیتی حلقه, its members, sessions, attendance, growth and in-app reward economy.

In scope:
- Local ring profile and settings.
- Member list, member profiles, history and export.
- Sessions and attendance.
- Progress tools and member growth tracking.
- Shop, inventory and member rewards.
- Refinery/configuration tools.
- Local backup and restore.

Out of scope for this version:
- Backend, cloud accounts, login, synchronization and online services.
- Member-facing application.
- Web administration portal and multi-ring organization management.
- Inter-ring competition.

## 2. Primary navigation

The persistent bottom navigation has exactly five destinations, in this order:
1. خانه
2. اعضا
3. پیشرفت
4. فروشگاه
5. تنظیمات

The Shop is a separate destination; it must not be nested under Home or Progress.

## 3. Home

The Home screen is a concise starting point, not a dashboard containing every operation.

Reference: ux.pdf pages 35–36.
- Ring identity/profile and current ring context.
- Daily quote/message.
- Weekly pulse summary card that opens a separate detail screen.
- Coach Assistant entry/card, using the approved label "دستیار مربی".
- Configurable quick-access shortcuts.
- The weekly-pulse detail may show ring-level activity, recent activity, history, member count and separate XP / spendable points / diamonds summaries.

Do not add a "command center" product concept or duplicate today's sessions/attendance/operations on Home. Do not label the coach assistant "سیگنال".

## 4. Members

Reference: ux.pdf pages 1–4; ux 2.pdf mapping.
- Member list with name search and sorting by name, level and spendable points.
- Do not add a group filter to the member list.
- Add member and open an individual member profile.
- Profile sections include member identity/progress, growth map, report card and received-item/history views.
- Preserve member history and the economy ledger.
- Temporary archive/removal is distinct from permanent deletion; restore and permanent deletion must be deliberate and separately confirmed.
- Member export is a separate flow and must not expose unrelated member data.

## 5. Progress

Progress contains six independent entry points:
1. شناسنامه — XP, spendable points, diamonds and level, shown separately.
2. مأموریت — member missions, special/hidden mission and group puzzle where defined.
3. کتاب‌خوانی.
4. گردونه.
5. رقابت — member and group ranking within the same ring only.
6. جلسات — session list and attendance.

Do not merge Shop into Progress. Do not enable competition between rings. Do not treat badges as shop inventory.

## 6. Sessions and attendance

- A session is a scheduled event; attendance is a separate record of actual participation.
- The weekly session calendar must be generated from the class weekdays selected in Settings. Never invent a fixed weekly calendar independent of those settings.
- Attendance has exactly five states: حاضر، غیبت، غیبت موجه، تأخیر، بدون ثبت.
- A reason is mandatory for غیبت، غیبت موجه and بدون ثبت.
- One member may have at most one attendance record per session.
- Draft changes should remain visibly pending until the coach explicitly saves/commits the attendance for that session.
- Preserve attendance history and make changes auditable.
- Session deletion must not silently cascade into attendance/history.

## 7. Economy, growth and inventory

- XP, spendable points and diamonds are three independent currencies.
- XP is used for progression/level; it is not spendable.
- Avatar and frame purchases use diamonds.
- Reward purchases use spendable points.
- Badges are not sold in the Shop; if granted by progression, they are recorded as earned.
- Every earning, spend, refund or correction must be represented in an append-only, traceable economy ledger.
- Purchases must be atomic: balance deduction and inventory grant either both succeed or both fail.
- Growth map has two independent tracks: توانمندی and خودسازی. Their items and progress must not be merged into one score.
- Growth items can be managed in Refinery; member progress is recorded against configured items.

## 8. Settings and Refinery

Settings areas identified in ux 2.pdf:
- Ring profile.
- Theme/appearance.
- Daily quotes.
- Class weekdays.
- Home shortcuts.
- Import/export.
- Help.
- Factory reset.
- Deleted members.
- Refinery/configuration.

Refinery areas identified in ux 2.pdf:
- Mission configuration.
- Shop reward configuration.
- Avatar/frame catalog configuration.
- Wheel configuration.
- Growth map configuration.

The Growth Map configuration screen has no direct image in the primary UX PDF; use the written specification and design pattern in ux 2.pdf page 8 and docs/UX_REFERENCE_MAP.md. It has separate توانمندی and خودسازی tracks with add, edit, active/hidden controls. Do not invent additional scoring rules.

## 9. Backup, restore and file access

- Backups are local and stored in the user-selected MRT folder.
- Keep no more than two automatic backup versions.
- Support the agreed JSON/ZIP export/import flows with schema/version validation before restore.
- Preserve a pre-restore safety point where appropriate; never overwrite user data silently.
- On first launch, explain why file access is needed and use an Android-appropriate user-mediated folder/file picker. Do not request broad all-files access merely for convenience.
- Backup encryption is a project requirement in the technical baseline. The portable key/recovery design must be specified and tested before implementation; do not invent a key recovery model.

## 10. Visual and interaction baseline

- Primary reference: ux.pdf.
- Navigation and page mapping: ux 2.pdf.
- The initial visual direction is midnight/navy with restrained neon mint, purple and pink accents.
- Support Persian RTL layout and readable typography on small screens.
- Provide empty, loading, validation, error and success states for every data-backed feature.
- Values shown in mockups are examples, not production data.
- Screens not directly represented in ux.pdf should follow the nearest approved pattern and the written mapping/specification.

## 11. Data integrity and privacy

- Keep the app fully usable offline.
- Keep member data, ring data, session data and history scoped to the local ring.
- Never store credentials, access tokens or secrets in exported backups.
- Use explicit transactions for coupled operations and preserve history.
- Database migrations must preserve existing user data.
- Permanent deletion, factory reset and destructive restore require clear confirmation.
- No sensitive operation should be presented as successful until persistence confirms it.

## 12. Open decisions / not to guess

- Backup encryption format and portable recovery-key mechanism.
- Automatic backup cadence and exact trigger conditions.
- Detailed mission, reading, wheel and competition scoring formulas where not already explicit in approved product reference.
- Any product behavior that conflicts between the product guide, UX/UX2 and the latest explicit owner instruction.

Resolve material conflicts with the owner before implementation. Do not silently promote illustrative mockup values or inferred rules to product requirements.
