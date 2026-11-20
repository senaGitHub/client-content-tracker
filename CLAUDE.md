# Harbour — Client & Content Tracker (MVP)

## Context
Take-home case study for a Full Stack Developer role at an in-house agency of a hospitality group (hotels in Bodrum, Cappadocia, Antalya).
Users: account managers. They track clients (group brands) and content per client (social posts, blog posts, web pages) through a status pipeline.
Deadline: Friday Nov 20, 2026, 23:59 — deliverable is a live URL + GitHub repo. Reviewers care most about design and UX.

## Working rules (important)
- Talk to me in Turkish. Code, identifiers, comments, commit messages and README in English.
- Work phase by phase (see Phases). Before each phase: show a short plan and wait for my approval. Do not start the next phase on your own.
- Keep changes scoped to the current phase. No unrequested refactors or features.
- Do not add libraries that are not listed in Stack without asking me first.
- After each phase: make sure it builds/runs, tell me how to verify it in 2–3 steps, briefly explain the key decisions (why, not just what — I am learning), and propose a Conventional Commit message.
- Never commit secrets. Use environment variables.

## Stack
Naming: app name "Harbour", repo and Maven artifactId "client-content-tracker", base Java package "io.github.senagithub.tracker" (GitHub user: senaGitHub).
Backend (`/backend`): Java 21, Spring Boot 3.x, Maven wrapper, Spring Web, Spring Data JPA, Bean Validation, PostgreSQL driver, Flyway, springdoc-openapi (Swagger UI). Lombok is NOT used — use Java records for DTOs.
Frontend (`/frontend`): Vue 3 (Composition API, `<script setup>`), TypeScript, Vite, Vue Router, Tailwind CSS, shadcn-vue, lucide-vue-next icons, vue-sonner (toasts), vue-draggable-plus (board drag & drop), date-fns.
Database: PostgreSQL (Neon) for both dev and prod. No local DB install.
Deploy: backend on Render (Docker), frontend on Vercel, DB on Neon.

## Domain
Client: id, name, industry, contactName, contactEmail, coverImageUrl (optional, must be one of the bundled `/images/covers/*.jpg`, listed in `CoverImages`), createdAt.
Content: id, title, type (SOCIAL_POST | BLOG_POST | WEB_PAGE), channel (optional, e.g. Instagram), status (DRAFT | IN_REVIEW | PUBLISHED), dueDate, notes (optional), client (ManyToOne), createdAt, updatedAt.
ActivityLog: id, content, action (CREATED | UPDATED | STATUS_CHANGED), fromStatus, toStatus, createdAt. It is written by the service layer and powers:
- the edit sheet's history,
- "published this week" and "published last week",
- how long an item has been waiting for approval.

Business rules:
- Allowed status transitions: DRAFT → IN_REVIEW, IN_REVIEW → PUBLISHED, IN_REVIEW → DRAFT (revision). Everything else is rejected with HTTP 409. The rule lives in the domain (enum method) and is enforced in the service.
- "Overdue" is computed, not stored: dueDate < today AND status != PUBLISHED.
- Demo data:
  - A seeder runs on startup only if the DB is empty.
  - Dates are relative to today, so overdue items and this week always look realistic.
  - There are 6 fictional hospitality brands (Turkuaz Bay Bodrum, Peri Cave Hotel, Lara Sands Resort, Olive & Stone Spa, Antalya Travel Co., Medina Health), each with a cover, and 36 contents (about 14 Taslak / 10 Onayda / 12 Yayında; 3 of them overdue on the seed day).
  - Titles, notes and industries are in Turkish; brand names are not.
  - Never use real brand names or logos.

## API (prefix /api)
- GET /clients?q=&page=&size=&sort= (paged, q searches name/industry/contact; with content counts per status + overdue), POST /clients, GET /clients/{id}
- GET /contents?status=&clientId=&type=&q=&overdue=&dueThisWeek=&page=&size=&sort=, POST /contents, PUT /contents/{id}
- GET /contents/status-counts?clientId=&type=&q=&overdue=&dueThisWeek= → { draft, inReview, published, overdue, total } for the list's status tabs
- GET /contents/{id}/activity → history, newest first (404 if the content doesn't exist)
- Content responses carry client { id, name, coverImageUrl }; the cover is the content thumbnail where one is shown.
- Paged lists return { items, page (0-based), size, totalElements, totalPages, hasNext }; size defaults to 20, max 100; sort is whitelisted (unknown property → 400). overdue / dueThisWeek use the same specifications as the dashboard.
- GET /contents also accepts `sort=priority` (used on its own): unpublished first by due date ascending (so overdue leads), then published, newest due date first. Used by the Overview feed and its "Tüm içerikler" link.
- PATCH /contents/{id}/status  body: { "status": "..." }  → 409 on invalid transition
- GET /dashboard returns:
  - `totals`: counts per status, plus overdue and total.
  - `metrics`: createdThisWeek, publishedThisWeek, publishedLastWeek, oldestInReviewDays, oldestOverdueDays. The last two are null when there is nothing to measure. "Week" means the last 7 days, rolling.
  - `clients`: every client with its counts and cover.
  - `approvals`: the 4 longest-waiting items in review, longest first. The full count is `totals.inReview`. Each item is a flat content response plus `inReviewSince` (ISO instant: when it last moved into IN_REVIEW, from activity_log). It is the `ApprovalItem` record with the content `@JsonUnwrapped`, so the existing fields stay at the top level.
Errors: @RestControllerAdvice returning ProblemDetail (RFC 7807). Validation errors → 400 with field messages. CORS origins from env var.

## Backend structure
Package by feature: `client`, `content`, `dashboard`, `activity`, `common` (errors, config). Controller → Service → Repository. Entities never leave the service layer; controllers use record DTOs. Schema via Flyway migrations.
Tests: only a focused unit test for the status transition rule.

## Frontend
Pages: Overview (/), Clients (/clients), Client detail (/clients/:id), Content (/content) with Board | List toggle.
Dialogs: New client, New/Edit content. Status can also be changed inline (list) and by drag & drop (board). On a rejected move (409) the card snaps back and a toast explains why.
Pagination: List view is paginated; Board loads the first 20 per column with a "Load more" button (Phase 7).
UX must-haves: skeleton loading, empty states, toasts (with Undo for status changes), dark mode toggle (persisted), responsive down to mobile, keyboard-friendly dialogs.
No login, no charts, no fake metrics.

## Design system — "Warm coral" (redesign, Nov 19)
Source of truth: static mockups in `design/` (git-ignored; read `design/README.md` first, then `01`–`07`). Match their layout, spacing, colours and copy. Their numbers, names and dates are sample data, so render real API data only. Never ship `design/images/*`.

UI language: Turkish (all visible copy, dates via the date-fns `tr` locale). Code, identifiers, API messages, commits and README stay English. The frontend maps API errors (409, field errors) to Turkish text.
Labels: statuses Taslak / Onayda / Yayında; types Sosyal Medya / Blog Yazısı / Web Sayfası. Nav: Genel Bakış, Markalar, İçerikler, Onay Süreci (a shortcut to the content list filtered to Onayda, with a count badge; not a separate page).

Font: Inter 400–800. The type scale is defined once, as `@theme` tokens in `frontend/src/assets/main.css` (size, line height, weight and tracking together); use the tokens instead of fixed sizes:
- `text-page-title` 24px / 700 (PageHeader h1, including the Overview greeting),
- `text-section-title` 16px / 600 (section headings such as "Markalara bakış", "İçerik akışı", "Onay bekleyenler"),
- `text-nav` 14px / 500 (sidebar menu; the active item is 600),
- `text-logo` 18px / 800 ("Harbour"),
- `text-kpi` 28px / 700 (KPI numbers),
- body copy `text-sm` (14px).

The brand detail hero, its "İçerikler" heading, board column headings and the 404 title keep their own sizes for now.
Radius: inputs and buttons 10px, kanban cards 14px, cards 18px, dialogs 20px, chips and badges full. Very soft shadows on cards (0 2px 10px rgba(60,30,20,.04)) and stronger ones on popovers, sheets and dialogs. No emoji.
Gradients only where the mockups use them: the logo tile and dark scrims on photos so the text on them stays readable.

Colour rules:
- RED (#D92D20) is reserved for overdue only: solid overdue chips, the Geciken tile and overdue dates. Validation errors and link hovers use the dark coral, never red. The Overview approvals panel uses no red at all, even for overdue items.
- Primary buttons are dark (#1C1716 with white text).
- The accent is coral, used for the active nav item, selected tabs, links, focus and the drop target.
- Coral #E0623F is under 4.5:1 on white, so text smaller than 18px uses the dark coral #AB4427 (darkened from #C9502E in Phase 13 so it also passes 4.5:1 on the tinted sidebar, nav-active and coral-soft backgrounds).
- Every colour is a CSS variable mapped to shadcn-vue tokens. Dark mode stays: same roles, values below.

| Token | Light | Dark |
|---|---|---|
| bg | #FBF8F6 | #141110 |
| sidebar | #FCF3EF | #1A1513 |
| surface | #FFFFFF | #1E1917 |
| column bg | #F7F1EE | #191513 |
| border / input border | #F1E8E4 / #ECE2DE | #2E2725 / #3A3230 |
| divider / subtle | #F3ECE9 | #29221F |
| text / secondary / muted | #1C1716 / #3B3331 / #6F6461 | #F3EDEA / #D6CCC8 / #A89D99 |
| primary / on-primary | #1C1716 / #FFFFFF | #F3EDEA / #1C1716 |
| accent | #E0623F | #F08A6C |
| accent text | #AB4427 | #F5A58C |
| accent soft / nav active | #FDE9E2 / #FBDDD4 | #3A221A / #44271D |
| overdue solid / text / soft bg / soft border | #D92D20 / #B42318 / #FFF5F4 / #F7C9C3 | #D92D20 / #F97066 / #2C1614 / #5A2620 |
| Taslak bg / fg / dot | #EEEAE8 / #3B3331 / #B9AFAB | #2B2523 / #D6CCC8 / #8C817D |
| Onayda bg / fg / dot | #FDEBC8 / #9C5C00 / #F2A93B | #3A2B12 / #F5B95A / #F2A93B |
| Yayında bg / fg / dot | #DDF3E4 / #1E7A44 / #2F8A57 | #173222 / #7FD3A0 / #2F8A57 |
| approve button (soft / solid) | #E7F5EC with text #1E7A44 / #2E8654 | #173222 with text #7FD3A0 / #2E8654 |
| Sosyal Medya icon | #D6297A | #F06AA8 |
| toast | #1C1716 | #2A2321 |

Layout:
- The sidebar (#FCF3EF) holds:
  - The logo: a coral gradient tile with an anchor icon, "Harbour" with a coral dot, and the subtitle "Brand & Content Workspace".
  - Nav items. Only Onay Süreci has a count: a coral badge, hidden at 0. Markalar and İçerikler have no numbers.
  - A small, plain "Demo veriler" pill at the very bottom. There is no photo in the sidebar.
- The theme toggle is icon-only, with an aria-label and a tooltip. It is not in the sidebar:
  - On desktop it sits in the shared PageHeader, top right, next to the page actions. The brand detail page has no PageHeader, so it sits in the hero's top-right corner. The 404 page has none.
  - On mobile it stays in the top bar, on the right.
- Main content sits directly on bg. On mobile the sidebar becomes a drawer.
- Toasts are dark with an inline "Geri al" action.
- Editing content opens a right-side Sheet. New content opens a Dialog.

Photos:
- Use high-quality Unsplash JPGs in `frontend/public/images`: 1600px wide, about 300 KB or less each.
- Each brand has `coverImageUrl`. Where content has a thumbnail (board cards, the content list, the edit sheet, the approvals panel), it is the brand's cover. The Overview content feed has no thumbnails.
- Credit the photographers with links in the README.
- Brands stay the fictional seed names.

Redesign decisions:
- Overview greeting changes with the time of day. It has no name and no avatar, because there is no login. Under it is the full date in small, muted text ("20 Kasım 2026, Cuma"). There is no summary sentence and no separate date box. Greeting and date update once a minute, so a tab left open stays right.
- The Overview header has only "Yeni İçerik" and the theme toggle: no search box and no "Yeni marka" card. New brands are created on the Markalar page.
- KPI tiles have one short line each, hidden when the value is 0 or null:
  - Toplam içerik: "+N bu hafta",
  - Onay bekleyen: "en eskisi N gündür" ("hepsi bugün geldi" for 0 days),
  - Yayında: "+N bu hafta" (no last-week comparison in the UI),
  - Geciken: "en eskisi N gün".
- "Markalara bakış" shows 4 brands: most overdue first, then most in review, then most content.
- Each approvals panel item shows how long it has waited in review: a neutral chip "N gündür bekliyor" ("bugün gönderildi" for 0 days), with a small grey "Teslim: 23 Kas" next to it. The line under the title has only the brand name.
- The Overview content feed (İçerik akışı):
  - No row thumbnails. The type icon sits left of the title (tooltip and screen-reader label from ContentTypeIcon), so there is no separate Tür column.
  - Columns: İçerik 44% / Marka 24% / Teslim 16% / Durum 16%.
  - The status menu stays in the feed for inline changes. After a change from the feed, the feed and the rest of the page reload together.
  - Filters: Tüm Markalar, Tüm Türler and the Content page's "Teslim" filter (Tümü / Geciken / Bu hafta, same `DUE_OPTIONS` and `overdue` / `dueThisWeek` parameters). All three use FilterSelect at `size="sm"` (32px, 13px text, as wide as their text) and wrap below the tabs on narrow screens. Tab counts and the "Tüm içerikler →" link carry the selected filters.
- FilterSelect lists open below the field (popper, bottom, start; 4px gap), at least as wide as the field, on the Overview and the Content page alike.
- Approving on the Overview: the approvals panel row stays in place while the publish waits. Its buttons are replaced by StatusMenu's spinner and an inline "Geri al" (the same cancel as the toast's), announced via aria-live. The row leaves when the PATCH is done and fresh data is in. On failure it returns to its buttons.
- While a publish waits, no number on the Overview changes: KPIs, brand cards, feed tab counts and the sidebar badge keep their server values. The feed shows that row's StatusMenu pending state (badge + spinner), not "Yayında". Once the PATCH is done, everything reloads together (refreshAll, awaited), so nothing flickers. The sidebar badge updates the same way everywhere (refreshNavCounts after the publish). The Content page's board and list still move the card to Yayında at once.
- Status badges in StatusMenu (feed, content list, brand detail) are `uniform`: every badge is as wide as the longest label, so the chevrons line up. The feed's "Durum" header is centred over the badge, with its width taken from the same component.
- Publishing cannot be undone on the server, so every move to Yayında is deferred, wherever it starts: "Onayla" in the approvals panel, the edit sheet, board drag & drop, and the list and feed menus. The PATCH is sent after about 5 seconds, and "Geri al" cancels it, so nothing is sent. A pending PATCH is flushed when the user leaves the page or closes the tab. Every other move is sent at once, and its "Geri al" sends the reverse move. The status rule stays strict.
- The edit sheet also has Tür and Kanal fields. Its status buttons apply at once and are separate from "Değişiklikleri kaydet". The PUT body has no status, so saving the form never overwrites a status change.
- "Bu hafta" means the last 7 days, "geçen hafta" the 7 days before that.
- The old Overview sections ("This week" strip and "Recent activity") are removed from the UI and from the API.

## Phases
1. Backend skeleton: Spring Boot project, entities, Flyway migration, repositories, demo seeder. Connect to Neon.
2. Backend API: DTOs, services, controllers, validation, ProblemDetail errors, status rule + unit test, Swagger.
3. Dashboard endpoint.
4. Backend deploy: Dockerfile, Render config, env vars, and a docker-compose.yml (app + postgres) so reviewers can run the backend locally with one command.
5. Frontend skeleton: Vite + TS + Tailwind + shadcn-vue, router, layout, theme (light/dark), API client.
6. Overview page. ✓ (old design)
7. Content page: Board (drag & drop) + List (filters, inline status). ✓ (old design)
— Redesign (Nov 19): phases 8–13 replace the old 8–9 and rework 6–7 to match `design/`. —
8. Backend for the redesign:
   - Flyway V2 adds `clients.cover_image_url`; the seeder fills covers.
   - Unsplash photos downloaded and optimised into `frontend/public/images`.
   - `q` search on GET /clients.
   - GET /contents/status-counts (same filters except status).
   - GET /contents/{id}/activity for the edit sheet's history.
   - Dashboard reshaped to the new Overview (real data only): created this week, published this week and last week (from activity_log), oldest waiting approval in days, oldest overdue in days, overdue and in-review counts per brand, and the approvals queue.
   - Update the Domain and API sections here.
9. Frontend foundation: tokens (light/dark), Turkish copy and date locale, logo, sidebar (counts, photo, demo pill), restyled shared components (buttons, badges, type icons, toasts, menus), Turkish error mapping. First Vercel deploy (SPA rewrite, env, CORS) so a live URL exists early.
10. Overview (Genel Bakış): KPI tiles, brand cards, content feed (status tabs and filters), approvals panel with Onayla / Revizyon plus toast and Geri al.
11. Content (İçerikler): restyled Board and List, status tabs with counts, the "Teslim" filter, the edit Sheet (status actions and history) and the redesigned New content Dialog.
12. Brands (Markalar): grid with search and cards, brand detail (hero, stats, status tabs, content table), and a New brand dialog with a cover picker.
13. Polish: responsive and dark mode pass, accessibility, README (screenshot, architecture, decisions & trade-offs, what's next, AI usage, how to run, photo credits), DB reset and final deploy.

## Out of scope (on purpose — mention in README)
Auth, delete, client editing, multi-module Maven, SSR/Nuxt (internal panel, SPA is enough).
Separate dev/prod databases (one managed Neon Postgres is used for both due to time; in a real project: Docker Compose locally, Testcontainers for tests, managed DB in prod).
