# Harbour frontend

Vue 3 + TypeScript + Vite SPA. See the root README for setup and architecture.

```bash
npm install
npm run dev                  # http://localhost:5173, talks to http://localhost:8080 by default
```

To point the dev server at another API (e.g. the deployed one), create `.env.development.local`
(git-ignored) based on `.env.example`. It must be `.env.development.local`, not `.env.local`:
Vite gives `.env.development` priority over `.env.local`, so the latter would be ignored.

```bash
cp .env.example .env.development.local   # then edit VITE_API_BASE_URL
```

Restart `npm run dev` after changing env files.
