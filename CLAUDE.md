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
Client: id, name, industry, contactName, contactEmail, createdAt.
Content: id, title, type (SOCIAL_POST | BLOG_POST | WEB_PAGE), channel (optional, e.g. Instagram), status (DRAFT | IN_REVIEW | PUBLISHED), dueDate, notes (optional), client (ManyToOne), createdAt, updatedAt.
ActivityLog: id, content, action (CREATED | UPDATED | STATUS_CHANGED), fromStatus, toStatus, createdAt — written by the service layer, powers "Recent activity".

Business rules:
- Allowed status transitions: DRAFT → IN_REVIEW, IN_REVIEW → PUBLISHED, IN_REVIEW → DRAFT (revision). Everything else is rejected with HTTP 409. The rule lives in the domain (enum method) and is enforced in the service.
- "Overdue" is computed, not stored: dueDate < today AND status != PUBLISHED.
- Demo data: a seeder runs on startup only if the DB is empty. Dates are relative to today so overdue / this week always look realistic. 6 fictional hospitality brands (e.g. Turkuaz Bay Bodrum, Peri Cave Hotel, Lara Sands Resort, Olive & Stone Spa, Antalya Travel Co., Medina Health) and ~36 contents. Never use real brand names or logos.

## API (prefix /api)
- GET /clients (with content counts per status), POST /clients, GET /clients/{id}
- GET /contents?status=&clientId=&type=&q=, POST /contents, PUT /contents/{id}
- PATCH /contents/{id}/status  body: { "status": "..." }  → 409 on invalid transition
- GET /dashboard → totals per status, needsAttention { overdue, dueThisWeek, awaitingApproval }, week (7 days with items per day), clients summary, recent activity
Errors: @RestControllerAdvice returning ProblemDetail (RFC 7807). Validation errors → 400 with field messages. CORS origins from env var.

## Backend structure
Package by feature: `client`, `content`, `dashboard`, `activity`, `common` (errors, config). Controller → Service → Repository. Entities never leave the service layer; controllers use record DTOs. Schema via Flyway migrations.
Tests: only a focused unit test for the status transition rule.

## Frontend
Pages: Overview (/), Clients (/clients), Client detail (/clients/:id), Content (/content) with Board | List toggle.
Dialogs: New client, New/Edit content. Status can also be changed inline (list) and by drag & drop (board). On a rejected move (409) the card snaps back and a toast explains why.
UX must-haves: skeleton loading, empty states, toasts (with Undo for status changes), dark mode toggle (persisted), responsive down to mobile, keyboard-friendly dialogs.
No login, no charts, no fake metrics.

## Design system — "Mediterranean, calm"
Font: Inter. Soft borders instead of shadows, radius 8–14px, generous spacing. No gradients, no emoji.
Light: bg #F4F1EB, surface #FFFEFC, border #E5DFD4, subtle #EDE8DF, text #1E2326, muted #666A6C, accent #1D5C67 (on-accent #FFFFFF).
Dark: bg #14181A, surface #1B2023, border #2A3135, subtle #252B2F, text #E7E5E0, muted #A0A6A8, accent #86BAC4 (on-accent #0F1E22).
Status (light): Draft bg #EEF0F2 fg #4B5563 dot #94A3B8 · In review bg #F7EED9 fg #8A5A0B dot #D9A441 · Published bg #E3EFE6 fg #2F6B47 dot #5B9A70 · Overdue text #A3412E.
Status (dark): Draft bg #262A31 fg #B4BBC5 · In review bg #3A3122 fg #E8C07A · Published bg #1F3428 fg #8FD3A6 · Overdue text #F0A08E.
Layout: left sidebar on page background (logo "Harbour" with the subtitle "Client & Content Tracker", nav: Overview, Clients, Content), main content on a rounded surface panel. Use shadcn-vue components and map these colors to its CSS variables.

## Phases
1. Backend skeleton: Spring Boot project, entities, Flyway migration, repositories, demo seeder. Connect to Neon.
2. Backend API: DTOs, services, controllers, validation, ProblemDetail errors, status rule + unit test, Swagger.
3. Dashboard endpoint.
4. Backend deploy: Dockerfile, Render config, env vars, and a docker-compose.yml (app + postgres) so reviewers can run the backend locally with one command.
5. Frontend skeleton: Vite + TS + Tailwind + shadcn-vue, router, layout, theme (light/dark), API client.
6. Overview page.
7. Content page: Board (drag & drop) + List (filters, inline status).
8. Clients list + detail + dialogs.
9. Polish + README (screenshot, architecture, decisions & trade-offs, what's next, AI usage, how to run) + frontend deploy.

## Out of scope (on purpose — mention in README)
Auth, delete, pagination, client editing, multi-module Maven, SSR/Nuxt (internal panel, SPA is enough).
Separate dev/prod databases (one managed Neon Postgres is used for both due to time; in a real project: Docker Compose locally, Testcontainers for tests, managed DB in prod).
