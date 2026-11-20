# Harbour — Client & Content Tracker (MVP)

## Context
Harbour is a full-stack web app I designed and built: an internal tool for the in-house content team of a hospitality group (hotels in Bodrum, Cappadocia, Antalya). All brands and data are fictional.
Users: account managers. They track clients (group brands) and content per client (social posts, blog posts, web pages) through a status pipeline.
Focus: design and UX, with a clean, well-tested API behind it.

## Working rules (important)
- Code, identifiers, comments, commit messages and README are in English. The UI is in Turkish.
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
- Due date can't be set in the past (`DueDateRule`, called by ContentService create/update with `LocalDate.now()`, the same "today" as overdue):
  - New content: due today or later.
  - Edit: an unchanged due date may stay in the past (overdue items stay editable); a changed one must be today or later.
  - Broken → 400 on `dueDate`, "Due date cannot be in the past." (Turkish: "Teslim tarihi bugünden önce olamaz.").
  - The frontend mirrors it in `frontend/src/lib/validation.ts` (`checkDueDate`), on blur and submit; the date input's `min` is today, so earlier days can't be picked, while a saved past date still shows when editing.
- Text validation (Bean Validation on the request records; `\p{L}` = any Unicode letter, so Turkish letters count):
  - Brand name: at least 2 characters and at least one letter (digits are fine, e.g. "Otel 7").
  - Industry: at least 2 characters; letters, spaces and `& - , .` only.
  - Contact name: at least 2 characters; letters, spaces and `' - .` only.
  - Content title: at least one letter.
  - Messages stay English in the API ("... must contain at least one letter.", "... may only contain letters, spaces and ..."); `frontend/src/lib/errors.ts` turns them into Turkish.
  - The frontend mirrors the same rules in `frontend/src/lib/validation.ts`, the one place for them and their Turkish messages. Change both sides together.
  - In every form (New brand, New content, edit sheet), changing a field clears that field's error and `aria-invalid` at once, leaving other fields alone. Blur re-checks a non-empty field; "required" shows on submit.
- Demo data:
  - A seeder runs on startup only if the DB is empty.
  - Dates are relative to today, so overdue items and this week always look realistic.
  - There are 6 fictional hospitality brands (Turkuaz Bay Bodrum, Peri Cave Hotel, Lara Sands Resort, Olive & Stone Spa, Antalya Travel Co., Medina Health), each with a cover, and 36 contents (about 14 Taslak / 10 Onayda / 12 Yayında; 3 of them overdue on the seed day).
  - Titles, notes and industries are in Turkish; brand names are not.
  - Never use real brand names or logos.

## API (prefix /api)
- GET /clients?q=&page=&size=&sort= (paged, q searches name/industry/contact; with content counts per status + overdue), POST /clients, GET /clients/{id}, PUT /clients/{id} (same validation as POST; 404 if missing)
- GET /contents?status=&clientId=&type=&q=&overdue=&dueThisWeek=&page=&size=&sort=, POST /contents, PUT /contents/{id}
- GET /contents/status-counts?clientId=&type=&q=&overdue=&dueThisWeek= → { draft, inReview, published, overdue, total } for the list's status tabs
- GET /contents/{id}/activity → history, newest first (404 if the content doesn't exist)
- Content responses carry client { id, name, coverImageUrl }; the cover is the content thumbnail where one is shown.
- Paged lists return { items, page (0-based), size, totalElements, totalPages, hasNext }; size defaults to 20, max 100; sort is whitelisted (unknown property → 400). overdue / dueThisWeek use the same specifications as the dashboard.
- GET /contents also accepts `sort=priority` (used on its own): unpublished first by due date ascending (so overdue leads), then published, newest due date first. Used by the Overview feed and its "Tüm içerikler" link.
- PATCH /contents/{id}/status  body: { "status": "..." }  → 409 on invalid transition
- GET /approvals?clientId=&sort=&page=&size= → paged approvals queue (content in IN_REVIEW). `sort` is a whitelist in the lists' `property,direction` form: `waiting,desc` (default, longest-waiting first by `inReviewSince`) or `waiting,asc` (shortest-waiting first); anything else → 400. Ties are broken by content id. The orders live next to the query (`ActivityLogRepository.QueueOrder`, JpaSort on its alias); `ApprovalSort` maps the parameter to them. Items are `ApprovalItem` (see below). Unfiltered, `totalElements` equals `totals.inReview`: both count IN_REVIEW content, and every status change (and the seeder) writes the activity_log row the queue reads. Served by `ApprovalService`, which the dashboard also uses.
- GET /dashboard returns:
  - `totals`: counts per status, plus overdue and total.
  - `metrics`: createdThisWeek, publishedThisWeek, publishedLastWeek, oldestInReviewDays, oldestOverdueDays. The last two are null when there is nothing to measure. "Week" means the last 7 days, rolling.
  - `clients`: every client with its counts and cover.
  - `approvals`: the 4 longest-waiting items in review, longest first: the first page of GET /approvals in the default `waiting,desc` order (same `ApprovalService`, no copied logic). The full count is `totals.inReview`. Each item is a flat content response plus `inReviewSince` (ISO instant: when it last moved into IN_REVIEW, from activity_log). It is the `ApprovalItem` record (`approval.dto`) with the content `@JsonUnwrapped`, so the existing fields stay at the top level.
Errors: @RestControllerAdvice returning ProblemDetail (RFC 7807). Validation errors → 400 with field messages. CORS origins from env var.

## Backend structure
Package by feature: `client`, `content`, `approval`, `dashboard`, `activity`, `common` (errors, config). Controller → Service → Repository. Entities never leave the service layer; controllers use record DTOs. Schema via Flyway migrations.
Tests: focused unit tests only, with no Spring context and no DB: the status transition rule (`ContentStatusTest`), the due date rule (`DueDateRuleTest`, fixed "today"), `Content.update` change detection (`ContentTest`, so an unchanged edit writes no UPDATED entry) and request validation (`ClientRequestValidationTest`, `ContentRequestValidationTest`, plain Bean Validation).

## Frontend
Pages: Overview (/), Clients (/clients), Client detail (/clients/:id), Content (/content) with a List | Board toggle (List is the default; the last choice on the device is remembered), Approvals (/approvals).
Dialogs: New/Edit client (one dialog, two modes), New/Edit content. Status can also be changed from the ⋯ menu (list rows, board cards) and by drag & drop (board). On a rejected move (409) the card snaps back and a toast explains why.
Pagination: List view is paginated; Board loads the first 20 per column with a "Load more" button (Phase 7).
UX must-haves: skeleton loading, empty states, toasts (with Undo for status changes), dark mode toggle (persisted), responsive down to mobile, keyboard-friendly dialogs.
No login, no charts, no fake metrics.

## Design system — "Warm coral" (redesign, Nov 19)
Source of truth: static mockups in `design/` (git-ignored; read `design/README.md` first, then `01`–`07`). Match their layout, spacing, colours and copy. Their numbers, names and dates are sample data, so render real API data only. Never ship `design/images/*`.

UI language: Turkish (all visible copy, dates via the date-fns `tr` locale). Code, identifiers, API messages, commits and README stay English. The frontend maps API errors (409, field errors) to Turkish text.
Labels: statuses Taslak / Onayda / Yayında; types Sosyal Medya / Blog Yazısı / Web Sayfası. Nav: Genel Bakış, Markalar, İçerikler, Onay Süreci (its own page, /approvals, with a count badge).

Font: Inter 400–800. The type scale is defined once, as `@theme` tokens in `frontend/src/assets/main.css` (size, line height, weight and tracking together); use the tokens instead of fixed sizes:
- `text-page-title` 24px / 700 (PageHeader h1, including the Overview greeting), with a -0.02em left offset so its first letter lines up with the description below (measured from Inter's glyph bounds), -0.06em when it starts with a round capital (O, Ö, C, Ç, G, Ğ, Q),
- `text-hero-title` 30px / 700 (the brand name in the brand detail hero: one step above page-title, since it sits on a photo),
- `text-section-title` 16px / 600 (section headings such as "Markalara bakış", "İçerik akışı", "Onay bekleyenler", and the brand detail's "İçerikler"),
- `text-nav` 14px / 500 (sidebar menu; the active item is 600),
- `text-logo` 18px / 800 ("Harbour"),
- `text-kpi` 28px / 700 (KPI numbers, including the brand detail's stat tiles),
- body copy `text-sm` (14px).

A new token goes in both `main.css` and `TYPE_SCALE` in `frontend/src/lib/utils.ts` (so `cn()` keeps it next to text colours). Board column headings and the 404 title keep their own sizes for now.
Radius: inputs and buttons 10px, kanban cards 14px, cards 18px, dialogs 20px, chips and badges full. Very soft shadows on cards (0 2px 10px rgba(60,30,20,.04)) and stronger ones on popovers, sheets and dialogs. No emoji.
Gradients only where the mockups use them: the logo tile and dark scrims on photos so the text on them stays readable.

Colour rules:
- RED (#D92D20) is reserved for overdue only: solid overdue chips, the Geciken tile and overdue dates. The Geciken tile (Overview KPI and brand detail stat) is red only when its value is above 0; at 0 it is a neutral card like the others, icon included. Validation errors and link hovers use the dark coral, never red. The Overview approvals panel uses no red at all, even for overdue items.
- Primary buttons use the logo coral (#E0623F with white text; hover/active darkens slightly to #D85A37), the same in light and dark mode. This is a deliberate design decision: about 3.5:1, below AA's 4.5:1, noted as the one exception in the README's Accessibility item. coral-solid (the Onay Süreci badge and other coral badges) is the same #E0623F + white in both themes, so there is a single coral. Set at token level (`--primary`, `--primary-foreground`, `--primary-hover`); only the default Button variant uses them. Toasts stay dark and the green "Onayla" buttons keep their approve tokens.
- Confirmation dialogs (e.g. unsaved changes): the safe choice ("Düzenlemeye dön") is the primary button and gets focus; the destructive one ("Değişiklikleri sil") is an outline button in accent text, never red. Dialogs with a tinted footer clip it to their rounded corners.
- The accent is coral, used for the active nav item, selected tabs, links, focus and the drop target.
- Content type icons (ContentTypeIcon): Sosyal Medya is lucide `Heart` (platform-independent, still reads as social media), Blog Yazısı `FileText`, Web Sayfası `Globe`. They are plain 16px icons with no tile or background, everywhere (feed, list, board, mobile rows, TypePicker). Each type colours its icon only (pink / purple / cyan, see the table); the label keeps its text colour. These hues stay clear of the status colours, overdue red and the coral accent.
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
| primary / on-primary / primary hover | #E0623F / #FFFFFF / #D85A37 | #E0623F / #FFFFFF / #D85A37 |
| coral solid / on coral solid (Onay Süreci badge) | #E0623F / #FFFFFF | #E0623F / #FFFFFF |
| accent | #E0623F | #F08A6C |
| accent text | #AB4427 | #F5A58C |
| accent soft / nav active | #FDE9E2 / #FBDDD4 | #3A221A / #44271D |
| overdue solid / text / soft bg / soft border | #D92D20 / #B42318 / #FFF5F4 / #F7C9C3 | #D92D20 / #F97066 / #2C1614 / #5A2620 |
| Taslak bg / fg / dot | #EEEAE8 / #3B3331 / #B9AFAB | #2B2523 / #D6CCC8 / #8C817D |
| Onayda bg / fg / dot | #FDEBC8 / #9C5C00 / #F2A93B | #3A2B12 / #F5B95A / #F2A93B |
| Yayında bg / fg / dot | #DDF3E4 / #1E7A44 / #2F8A57 | #173222 / #7FD3A0 / #2F8A57 |
| approve button (soft / solid) | #E7F5EC with text #1E7A44 / #2E8654 | #173222 with text #7FD3A0 / #2E8654 |
| type icon: Sosyal Medya / Blog Yazısı / Web Sayfası | #D6297A / #7C3AED / #0E7490 | #F06AA8 / #B197FC / #22D3EE |
| toast | #1C1716 | #2A2321 |

Layout:
- The sidebar (#FCF3EF) holds:
  - The logo: a coral gradient tile with an anchor icon, "Harbour" with a coral dot, and the subtitle "Brand & Content Workspace".
  - Nav items. Only Onay Süreci has a count: a coral badge, hidden at 0. Markalar and İçerikler have no numbers.
  - A small, plain "Demo veriler" pill at the very bottom. There is no photo in the sidebar.
- The theme toggle is icon-only, with an aria-label and a tooltip. It is not in the sidebar:
  - On desktop it sits in the shared PageHeader, top right, next to the page actions. The brand detail page has no PageHeader, so it sits at the right end of the "← Markalar" row above the hero (never on the photo). The hero has no status chips; the stat tiles below carry those counts. The 404 page has none.
  - On mobile it stays in the top bar, on the right.
- Main content sits directly on bg. On mobile the sidebar becomes a drawer.
- Toasts are dark with an inline "Geri al" action. They sit bottom-right; while the edit sheet is open they move left of it (offset right = sheet width + 16px, only where they fit; full-width sheets on small screens keep the normal offset). They stay clickable while a dialog or sheet is open; a click on a toast never closes the modal (`lib/overlay.ts`).
- Editing content opens a right-side Sheet. New content opens a Dialog.

Photos:
- Use high-quality Unsplash JPGs in `frontend/public/images`: 1600px wide, about 300 KB or less each.
- Each brand has `coverImageUrl`. Where content has a thumbnail (the edit sheet, the approvals panel), it is the brand's cover. The Overview feed, the Onay Süreci page, the content list and board cards have no thumbnails.
- Credit the photographers with links in the README.
- Brands stay the fictional seed names.

Redesign decisions:
- Overview greeting changes with the time of day. It has no name and no avatar, because there is no login. Under it is the full date in small, muted text ("20 Kasım 2026, Cuma"). There is no summary sentence and no separate date box. Greeting and date update once a minute, so a tab left open stays right.
- The Overview header has only "Yeni İçerik" and the theme toggle: no search box and no "Yeni marka" card. New brands are created on the Markalar page.
- KPI tiles: the first row is a 28px icon square + the label, side by side and vertically centred; the second row is the number with the short line next to it. Label and short line wrap rather than overflow in the 2x2 mobile grid. Each tile has one short line, hidden when the value is 0 or null:
  - Toplam içerik: "+N bu hafta",
  - Onay bekleyen: "en eskisi N gün" ("hepsi bugün geldi" for 0 days; the approvals panel chip keeps "N gündür bekliyor"),
  - Yayında: "+N bu hafta" (no last-week comparison in the UI),
  - Geciken: "en eskisi N gün".
- "Markalara bakış" shows 4 brands: most overdue first, then most in review, then most content.
- Each approvals panel item shows how long it has waited in review: a neutral chip "N gündür bekliyor" ("bugün gönderildi" for 0 days), with a small grey "Teslim: 23 Kas" next to it. The line under the title has only the brand name.
- The Overview content feed (İçerik akışı):
  - No row thumbnails. The type icon sits left of the title (tooltip and screen-reader label from ContentTypeIcon), so there is no separate Tür column.
  - Columns: İçerik 44% / Marka 24% / Teslim 16% / Durum 16%.
  - The status menu stays in the feed for inline changes. After a change from the feed, the feed and the rest of the page reload together.
  - Filters: Tüm Markalar, Tüm Türler and the Content page's "Teslim" filter (Tümü / Geciken / Yaklaşan (7 gün), same `DUE_OPTIONS` and `overdue` / `dueThisWeek` parameters). All three use FilterSelect at `size="sm"` (32px, 14px text, as wide as their text) and wrap below the tabs on narrow screens. Tab counts and the "Tüm içerikler →" link carry the selected filters.
- Every Select list (filters and forms alike) opens below its field: the shared SelectContent defaults to popper, bottom, start, `sideOffset` 4, at least as wide as the field (`--reka-select-trigger-width`); collision handling flips it above when there is no room below. Dropdown menus (⋯, status menu) are separate and unchanged.
- Onayla and Revizyon in the approvals queues (the Overview panel and the Onay Süreci page) are both deferred, through one mechanism (`useDeferredMove`, the same one every publish uses): the row stays in place, the pressed button shows a spinner and the other one is hidden (it keeps its place). There is no inline undo: "Geri al" is in the bottom-right toast, "“<title>” yayına alınıyor" for Onayla and "“<title>” taslağa döndürülüyor" for Revizyon. Geri al sends nothing, restores the buttons and moves focus to the row's first button. Otherwise the PATCH (→ PUBLISHED or → DRAFT) goes after about 5 seconds; leaving the page or closing the tab sends it at once. The row's aria-live text announces the move and the buttons keep their aria-labels with the title. The row leaves when the PATCH is done and fresh data is in (list, KPIs and the sidebar badge together). On failure it returns to its buttons, with an error toast.
- While a publish or a queue revision waits, no number on the Overview changes: KPIs, brand cards, feed tab counts and the sidebar badge keep their server values. The feed shows that row's StatusMenu pending state (badge + spinner), not "Yayında". Once the PATCH is done, everything reloads together (refreshAll, awaited), so nothing flickers. The sidebar badge updates the same way everywhere (refreshNavCounts after the publish). The Content page's board and list still move the card to Yayında at once.
- Status badges in StatusMenu (feed, content list, brand detail) are `uniform`: every badge is as wide as the longest label, so the chevrons line up. The feed's "Durum" header is centred over the badge, with its width taken from the same component.
- Publishing cannot be undone on the server, so every move to Yayında is deferred, wherever it starts: "Onayla" in the approvals panel, the edit sheet, board drag & drop, and the list and feed menus. The PATCH is sent after about 5 seconds, and "Geri al" cancels it, so nothing is sent. A pending PATCH is flushed when the user leaves the page or closes the tab. Every other move is sent at once, and its "Geri al" sends the reverse move, except Revizyon in the approvals queues, which is deferred like a publish (see above). The Content page (list, board) and the edit sheet send revisions at once. The status rule stays strict.
- The Content page (İçerikler):
  - It opens on List when the URL has no `view` and nothing was picked on this device; the toggle reads "Liste | Pano". `?view=board` / `?view=list` links still work.
  - One size for the tabs + filters rows (Content page, Overview feed, brand detail): everything is 14px (`text-sm`) and 32px tall. The tabs are the shared `StatusTabs` component; filters are FilterSelect `size="sm"`; the search box is 32px with 14px text. Font weights are unchanged (selected tab semibold, others medium).
  - Status tabs (list only) and the toolbar share one row: tabs left, search + filters right; the toolbar wraps under the tabs (left-aligned) when there is no room. On the board (no tabs) the toolbar spans the row: search left, filters right (`spread`), and the filters wrap under the search, left-aligned, when there is no room. The toolbar is compact like the Overview feed: FilterSelect `size="sm"` and a 32px search box, 220px wide on desktop, full width on mobile.
  - In the list, Durum is read-only: a `uniform` StatusBadge with no chevron. Status changes go through the ⋯ menu (ContentActionsMenu: Düzenle + status moves), on desktop and mobile rows. The ⋯ menu shows the pending spinner.
  - List rows and board cards have no thumbnails. Under the title, a muted 12px line shows the channel (hidden when there is none). The list keeps its Tür column; mobile rows read "type icon · channel · brand · date".
  - On the board, a press on a card's ⋯ trigger, links or menu items never starts a drag (SortableJS `filter`, `preventOnFilter: false`). The stretched title button (`data-card-open`) still drags, so the rest of the card does too.
- The Brands page (Markalar):
  - The header holds only "Yeni Marka" and the theme toggle. The search ("Marka ara...") sits under the header, 40px tall, 260px wide on desktop, full width on mobile.
  - The grid has no dashed "Yeni marka ekle" card. New brands come from the header button, or from the empty state's button when there are no brands.
  - A brand card with nothing overdue and nothing in review shows a small muted 12px "Bekleyen iş yok" in the badge row. The row keeps the badge height, so every card lines up.
  - The New brand dialog's description reads "Markanın adını, sektörünü ve iletişim bilgilerini girin."
  - Brands are edited from the brand detail hero: a secondary "Düzenle" button (pencil icon) next to the contact button opens the same dialog filled in (`useClientDialog().openEdit`), titled "Markayı düzenle" with "Değişiklikleri kaydet". Same validation as creating. After saving, the toast says "Marka güncellendi" and the brand detail, the Brands list, the sidebar counts and the brand pickers reload.
- Kanal is picked from a fixed list (ChannelSelect, a Select that opens below the field), with "Kanal yok" first for no channel. The options are `CHANNEL_SUGGESTIONS`: Instagram, Facebook, LinkedIn, TikTok, YouTube, Web sitesi, Blog, E-bülten (every seeded channel is in it). A saved channel that is not in the list shows at the top, selected, so editing never loses it.
- The edit sheet also has Tür and Kanal fields. Its status buttons apply at once and are separate from "Değişiklikleri kaydet". The PUT body has no status, so saving the form never overwrites a status change.
- "Bu hafta" (KPI lines) means the last 7 days, rolling, and "geçen hafta" the 7 days before that. The due filter's "Yaklaşan (7 gün)" means due today or in the next 6 days (`dueThisWeek`). Code and API names (`createdThisWeek`, `dueThisWeek`, ...) stay as they are.
- The old Overview sections ("This week" strip and "Recent activity") are removed from the UI and from the API.
- Overview layout on xl: the KPI row and the section below share one 4-column grid with a 20px column gap (`gap-5`); the left side (Markalara bakış + İçerik akışı) spans 3 columns and the approvals panel sits under the Geciken tile, edge to edge. The panel's heading is outside its card, on the same line as "Markalara bakış". Sections are 24px apart vertically. A narrow panel (card content under 340px) puts Onayla / Revizyon under the cover and title, side by side. Below xl the page stacks as before.
- The Onay Süreci page (/approvals):
  - An approvals inbox: every item in review. PageHeader "Onay Süreci" with "Onay bekleyen içerikleri inceleyin, yayına alın ya da revizyona gönderin." The header keeps only the theme toggle. Right above the table, on its own right-aligned row with nothing on the left (20px from the header and from the table, like the Content page), are two FilterSelects (`size="sm"`): Marka ("Tüm Markalar" + brands) and Bekleme. Bekleme shows the placeholder "Bekleme Süresi" when nothing is picked (the list is then longest-waiting first, the server default); its options are "En uzun" and "En kısa" (no "all" item: `allOption=false`), shown as "Bekleme: En uzun" / "Bekleme: En kısa"; × clears it. On mobile the two are full width, side by side. Brand, order and page live in the URL (`?clientId=&sort=&page=`); `sort` is written only when the user picked one, even "En uzun". A brand or order change goes back to page 1. No count line and no sortable column headings; the list is an ARIA table (role table/row/columnheader/cell) with plain headings, desktop only.
  - Rows in a card (ApprovalQueueRow): type icon + title + channel, brand, the neutral "N gündür bekliyor" chip, due date + hint (overdue dates red, as everywhere outside the Overview panel), and Onayla / Revizyon. Mobile rows stack, with full-width buttons.
  - Same behaviour as the Overview panel, from shared code: `useApprovalActions` (deferred Onayla and Revizyon, undo in the toast) and `ApprovalActions.vue` (buttons, spinner on the pressed one, focus back after Geri al, aria-live, aria-labels with the title). Each view keeps its own row. After a committed move the list and the sidebar badge reload together.
  - A click on the row or the title opens the edit sheet (the panel's titles do too). Paged with `ListPager` (shared with the content list; hidden on a single page). Skeleton, empty state ("Onay bekleyen içerik yok") and error state with retry.
  - The sidebar's Onay Süreci item, the panel's "Tüm onaylar →" and the Overview's "Onay bekleyen" KPI tile open it.

## Phases
1. Backend skeleton: Spring Boot project, entities, Flyway migration, repositories, demo seeder. Connect to Neon.
2. Backend API: DTOs, services, controllers, validation, ProblemDetail errors, status rule + unit test, Swagger.
3. Dashboard endpoint.
4. Backend deploy: Dockerfile, Render config, env vars, and a docker-compose.yml (app + postgres) so anyone can run the backend locally with one command.
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
10. Overview (Genel Bakış): KPI tiles, brand cards, content feed (status tabs and filters), approvals panel with Onayla / Revizyon and Geri al.
11. Content (İçerikler): restyled Board and List, status tabs with counts, the "Teslim" filter, the edit Sheet (status actions and history) and the redesigned New content Dialog.
12. Brands (Markalar): grid with search and cards, brand detail (hero, stats, status tabs, content table), and a New brand dialog with a cover picker.
13. Polish: responsive and dark mode pass, accessibility, README (screenshot, architecture, decisions & trade-offs, what's next, AI usage, how to run, photo credits), DB reset and final deploy.

## Out of scope (on purpose — mention in README)
Auth, delete and archive (brands and content), multi-module Maven, SSR/Nuxt (internal panel, SPA is enough).
Separate dev/prod databases (one managed Neon Postgres is used for both due to time; in a real project: Docker Compose locally, Testcontainers for tests, managed DB in prod).
