# Negah Coach — UX Reference Map

Status: implementation baseline derived from `ux.pdf` (36 visual pages) and `ux 2.pdf` (10-page menu/mapping specification).

## Source precedence

1. `ux 2.pdf` is the navigation and screen-mapping index.
2. `ux.pdf` is the primary visual reference for screen composition and interaction.
3. If a screen has no direct visual reference, derive it from the nearest UX pattern and the written specification in UX2.
4. Confirmed product decisions remain constraints when sample UI content conflicts with them. In particular: no weekly calendar, no inter-ring competition, no purchasable badges, and no home dashboard cards for today's sessions, attendance, active mission, or leaderboard. The weekly pulse may open a separate detail view.

## Main navigation

Five persistent destinations: Home, Members, Progress, Shop, Settings.

## Image mapping

| Destination | UX page(s) | Scope |
|---|---:|---|
| Home | 35–36 | Ring identity, banner/quote, weekly pulse, coach assistant, configurable shortcuts; page 36 is weekly-pulse detail |
| Members | 1–4 | Member list, profile, full history, profile export |
| Progress | 22, 25–34 | Progress overview, sessions/attendance, competition, wheel, missions, reading, profile/progress records |
| Shop | 19–24 | Shop overview, reward details, avatar/frame catalogs and item details |
| Settings | 5–11, 16, 18 | Ring profile, appearance, quotes, class days, shortcuts, backup/import-export, reset/help |
| Settings / Refinery | 12–17 | Mission, reward, avatar/frame and wheel configuration |
| Settings / Refinery / Growth map | No direct image | Build from UX2 page 8 specification |

## Growth map configuration screen

The missing screen is located at Settings > Refinery > Growth Map. It manages two independent tracks:
- Capabilities: coach-defined skills such as time management, teamwork and presentation.
- Self-improvement: coach-defined options such as exercise, Quran and prayer.

Each item supports active/hidden state, edit and delete actions. Provide separate add-item actions for each track. The two tracks must not be combined into one score or affect each other. Items configured here connect to the member profile growth map and report card.

## Visual language

- Default visual direction: midnight/navy dark surfaces with neon mint, purple and pink accents.
- UX2 palette reference: mint `#00E5A3`, purple `#8B5CF6`, pink `#EC4899`.
- Maintain high contrast and readable typography on small screens.
- Use rounded surfaces and restrained accent colors; do not turn every component into a neon highlight.
- A light theme may be supported as a separate appearance choice; dark is the initial visual baseline.

## Data integrity and sample content

Numbers, names, avatars, prices and member counts visible in mockups are illustrative unless independently established in product data. Never hard-code them as real ring data. All balances must be sourced from the economy ledger and derived views.

## Implementation sequencing

1. Shared design system and navigation shell.
2. Member list/profile aligned to pages 1–4, using live repository data only.
3. Home and weekly pulse pages 35–36.
4. Progress destinations and their domain/data contracts.
5. Shop and inventory/economy flows.
6. Settings and Refinery, including the newly specified Growth Map configuration.
7. PDF export and backup flows after their data contracts and storage permissions are in place.

Each feature must include domain rules, persistence, ViewModel state, UI states (loading/empty/error/content), and tests before being considered complete.
