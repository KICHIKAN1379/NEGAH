# نگاه مربی — Product Specification (working baseline)

Status: working implementation baseline; owner retains final acceptance.
Scope: the Android coach application only. The AK1 guide is a product-behavior reference, not a source-code dependency. This document does not define the separate web product "نگاه راهبر".

## 1. Product purpose and boundaries

نگاه مربی is an offline-first Android application for a coach to manage one تربیتی حلقه, its members, sessions, attendance, growth and in-app reward economy.

In scope:
- Local ring profile, settings and home identity.
- Member list, member profiles, inventory, history and member export.
- Sessions and attendance.
- Progress tools: member identity, missions, reading, wheel, competition and sessions.
- Shop, inventory and member rewards.
- Refinery/configuration tools.
- Local automatic backup and manual export/import.

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

The Home screen is concise, visually energetic and easy to scan; it is not a dashboard containing every operation.

Reference: ux.pdf pages 35–36 and the latest product-guide decisions.
- Ring identity/profile, including the real ring name, image and home banner; do not hard-code sample names or counts.
- Daily quote/message.
- A clickable weekly-pulse summary that opens a separate detail screen.
- Configurable quick shortcuts, with no more than four shown at once.
- "دستیار مربی" is available as a quick shortcut; it is not a separate dashboard card. Do not use "سیگنال" as its name.
- Weekly-pulse details may include today's active mission, activity trail, history, member count and avatars, separate XP / spendable-points / diamonds summaries, seven-day attendance trend, active-mission count and recent events (up to the documented 20-event snapshot).

Do not add:
- "مرکز فرماندهی".
- Separate cards for today's operations, today's sessions, today's attendance or the active mission.
- A long standalone activity feed on Home.
- A duplicate Shop card.
- Separate top-ranked/member-count cards that were removed in the latest Home revision.

## 4. Members and member profile

Reference: ux.pdf pages 1–4; ux 2.pdf mapping.
- Compact row-based member list, not oversized cards.
- Search by member name.
- Sorting options in this order: rank/level, spendable points, name.
- Do not add a group filter to the Members list. Attendance may have its own group filter if needed; that decision does not apply to Members.
- Add a member and open an individual member profile by tapping the row.
- Profile sections: identity, Ganjine (inventory), received history, Growth Map, report card and member export/summary.
- Preserve member history and the economy ledger.
- Temporary archive/removal is distinct from permanent deletion. Deleted members are managed through the import/export and deleted-members flow; restore and permanent deletion must be deliberate and separately confirmed.
- Member export must include only that member's approved data and may include the profile image, active avatar and active frame.

### Ganjine (member inventory)

Ganjine has exactly four tabs, in this order:
1. آواتار
2. قاب
3. نشان
4. جایزه

Each tab shows items the member has received of that type. The coach may change the active item where applicable.
- Badges (نشان) are earned automatically when the member reaches the required level. The 21 progression tiers follow the approved IRGC rank order; level 1 unlocks the first tier, each subsequent level unlocks the next tier, and level 21 unlocks the final tier. Higher levels do not create additional rank badges.
- These rank names are used only as symbolic in-app progress badges; they do not represent real-world rank, authority, affiliation, or promotion.
- The coach may change which earned badge is active, but cannot manually grant a badge.
- Badges are not Shop products and are never purchased.

### Received history

- A separate preview appears near the bottom of the member profile.
- "همه" opens the complete received-history list.
- The full list supports filtering.
- Record every item the member can receive, including the source/reason.
- This is not the same as the general ring activity feed.

### Member report card

- The coach selects a monthly or yearly range.
- Show percentage progress and growth trends.
- Maintain six independent metrics and charts; do not collapse the report into XP alone:
  1. مأموریت
  2. امتیاز قابل‌خرج
  3. نقشه کمال
  4. کتاب‌خوانی
  5. سطح
  6. الماس
- Each metric has its own line/progress-percentage view.
- Member export/summary includes the profile image, active avatar and active frame.

### Birthdays

- Notify the coach three days before a member's birthday.
- A birthday reward is automatic and equals 30 spendable credits (امتیاز قابل‌خرج); it is recorded in the append-only economy ledger.
- The reward is not a manual repeatable claim. The same member must not receive duplicate rewards for the same birthday occurrence, including after backup restore.

## 5. Progress

Progress contains six independent entry points:
1. شناسنامه — XP, spendable points, diamonds, level and rank.
2. مأموریت — one destination with three tabs: مأموریت، مأموریت ویژه (مخفی)، پازل گروهی.
3. کتاب‌خوانی.
4. گردونه.
5. رقابت — member and group rankings within the same ring only.
6. جلسات — session list and attendance.

Do not merge Shop into Progress. Do not enable competition between rings. Do not treat badges as Shop inventory. Do not restore the removed "مرکز مأموریت‌ها و دستاوردها" concept.

### Missions

- Track mission requests/progress for each member.
- A member request may require coach review/approval.
- Hidden/special missions remain hidden according to their own access rules.
- Mission notifications are distinguishable from general notifications and correctly update read/unread state.
- Restoring a backup must not grant the same mission reward twice.
- Mission lifecycle, completion conditions, cancellation/rejection and reward amounts must follow approved domain rules; do not invent formulas.

### Reading

- Reading is a separate Progress destination.
- Preserve member reading records and participation history.
- Reading has its own report-card metric.
- Scoring units, reward amounts and calculation rules remain open unless explicitly defined in the approved product source.

### Wheel

- Wheel is a separate Progress destination; its configuration is in Refinery.
- Record each result in the economy ledger and received/activity history as appropriate.
- Randomness, probability weights, duplicate prevention and restore behavior require tests.
- Do not guess probability weights or reward distributions.

### Competition

- Competition is restricted to members and groups within the same ring.
- XP is the basis for permanent progression/level/rank.
- Spendable points must not replace XP as the rank basis.
- Group power metrics, ranking formulas and trend calculations are not final unless explicitly approved.

## 6. Sessions and attendance

A session is a scheduled event; attendance is a separate record of actual participation.

### Sessions
- Dashboard summary: total, upcoming and completed sessions.
- Create, edit and delete sessions with history/data-integrity safeguards.
- Search by title, member, topic and location.
- Filters: all, upcoming and completed.
- Sort with upcoming sessions prioritized.
- Show attendance state in the session card or detail view.
- Session deletion must not silently cascade into attendance/history.

### Attendance
- Support weekly statistics, member search and a group filter where appropriate to the attendance page.
- Support quick attendance entry and a weekly/monthly date-range view.
- Show a clear daily status summary and a final Save/Register action at the bottom of the page.
- Changes remain pending until the coach explicitly presses the final save action.
- Attendance has exactly five states: حاضر، غیبت، غیبت موجه، تأخیر، بدون ثبت.
- A reason is mandatory for غیبت، غیبت موجه and بدون ثبت.
- One member may have at most one attendance record per session.
- Do not show a separate weekly calendar on Home; the session calendar belongs in Sessions.
- The weekly session calendar must be generated from the class weekdays selected in Settings (Saturday through Friday). Never invent a fixed weekly calendar independent of those settings.
- Support holidays/removed days in the calendar.
- Provide CSV export compatible with Excel.
- Preserve attendance history and make changes auditable.

## 7. Economy, growth and inventory

- XP, spendable points and diamonds are three independent currencies.
- XP is used for permanent progression, level and rank; it is not spendable.
- Spendable points are used for Shop rewards.
- Diamonds are used for avatar and frame purchases.
- Every earning, spend, refund or correction must be represented in an append-only, traceable economy ledger.
- Purchases must be atomic: balance deduction and inventory grant either both succeed or both fail.
- Badges are earned automatically by level; no manual badge grant and no badge sale.
- Growth Map has two independent tracks: توانمندی and خودسازی. Their items and progress must not be merged into one score.
- Growth items support add, edit, delete and active/hidden state in Refinery. Hidden/inactive items are excluded from the member-facing Growth Map.
- Growth progress is recorded against configured items.

### Level progression

The approved level thresholds are:
- Level 0: 0–19 XP.
- Level 1: 20–49 XP.
- Each next level requires 30 additional XP beyond the previous threshold.

The current domain implementation has matching boundary tests. Changes to this formula require a separate product decision and updated tests.

## 8. Settings and Refinery

Settings has exactly nine main options, in this order:
1. پالایشگاه
2. ویرایش پروفایل حلقه
3. صدا، پس‌زمینه و تم
4. جمله‌های روز
5. روزهای کلاس
6. میانبرهای خانه
7. ورودی و خروجی اطلاعات
8. بازگشت به تنظیمات کارخانه
9. راهنما

Rules:
- Ring profile editing is independent and includes ring name, ring username where defined, ring image and Home banner; never hard-code example values.
- Sound/music, background and theme form one settings group.
- Class days are selected as weekdays from Saturday through Friday, not as a date range.
- Home shortcuts have their own settings screen and allow at most four simultaneous shortcuts.
- Import and export share one destination. The deleted-members flow (temporary archive, restore, permanent deletion) belongs within the data import/export area, not as a tenth main setting.
- Factory reset requires clear final confirmation.
- Do not add an undefined "personalization" option.

Refinery areas:
- Mission configuration/editing.
- Shop rewards.
- Avatar and frame catalogs.
- Wheel configuration.
- Growth Map configuration.

The Growth Map configuration screen has no direct image in the primary UX PDF; use the written specification and pattern in ux 2.pdf page 8 and docs/UX_REFERENCE_MAP.md. It has separate توانمندی and خودسازی tracks with add, edit, delete and active/hidden controls. Do not invent additional scoring rules. The group puzzle belongs in the Missions destination, not as a separate main Refinery area.

## 9. Backup, restore and file access

- Automatic local backup is always enabled because the app is offline-first.
- Store backups in the dedicated MRT folder.
- Keep no more than two automatic backup versions. This newer explicit owner decision supersedes older product-guide snapshots that mention a larger retention count.
- Automatic backup and manual export are complementary, separate flows.
- Support the agreed JSON/ZIP manual export/import flows with schema/version validation before restore.
- Full-ring export must preserve the data required to restore the ring's state; member export is a separate scoped export.
- Never overwrite live data silently. Validate before restore and create a safety point where appropriate.
- On first launch, explain why file access is needed and use an Android-appropriate user-mediated folder/file picker. Do not request broad all-files access merely for convenience.
- Backup encryption is a project requirement in the technical baseline. The portable key/recovery design must be specified and tested before implementation; do not invent a key recovery model.
- Automatic backup cadence/trigger remains an open technical/product detail; an older snapshot mentions Activity exit, but that trigger must not be assumed final without reconciliation.
- Never include passwords, tokens, invitation codes or secrets in public backup files.

## 10. Visual and interaction baseline

- Primary reference: ux.pdf.
- Navigation and page mapping: ux 2.pdf.
- Approved art direction: Hybrid visual language with Bold intensity; modern, game-oriented, energetic and distinctive, not a generic/classic admin panel.
- Use the midnight/navy base and neon mint (#00E5A3), purple (#8B5CF6) and pink (#EC4899) accents specified by UX2.
- The product guide also records purple as #6B5CF5; this conflicts with UX2 and the current theme token. Keep #8B5CF6 pending an explicit owner decision.
- Effects must never reduce text/control readability; translucent surfaces need sufficient opacity.
- Support Persian RTL layout and readable typography on small screens.
- Provide empty, loading, validation, error and success states for every data-backed feature.
- Values shown in mockups are examples, not production data.
- Screens not directly represented in ux.pdf should follow the nearest approved pattern and written mapping/specification.

## 11. Data integrity and privacy

- Keep the app fully usable offline.
- Keep member data, ring data, session data and history scoped to the local ring.
- Never store credentials, access tokens or secrets in exported backups.
- Use explicit transactions for coupled operations and preserve history.
- Database migrations must preserve existing user data.
- Permanent deletion, factory reset and destructive restore require clear confirmation.
- No sensitive operation should be presented as successful until persistence confirms it.

## 12. Explicit prohibitions

- No "مرکز فرماندهی" or "سیگنال" terminology.
- No Shop nested under Home or Progress.
- No group filter in the Members list.
- No separate Home cards for today's sessions, attendance, operations or active mission.
- No mixing XP, spendable points and diamonds.
- No manual badge granting or badge sales.
- No inter-ring competition in this version.
- No online sync/backend activation without a separate decision.

## 13. Open decisions / not to guess

- Backup encryption format and portable recovery-key mechanism.
- Automatic backup cadence and exact trigger conditions.
- Detailed mission lifecycle/reward formulas where not explicit in approved product rules.
- Reading scoring units/rewards.
- Wheel probability weights and reward distribution.
- Group competition formulas and metrics.
- Birthday date capture/display convention and notification scheduling details.
- Purple accent conflict: UX2/current theme uses #8B5CF6, while the product guide lists #6B5CF5.
- Any behavior that conflicts between the product guide, UX/UX2 and the latest explicit owner instruction.

Resolve material conflicts with the owner before implementation. Do not silently promote illustrative mockup values or inferred rules to product requirements.
