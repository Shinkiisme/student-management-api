# Campus · Student Management Frontend

React, strict TypeScript, Vite, Tailwind CSS, React Router, Lucide, and native Fetch. Application data comes entirely from the existing Spring Boot API.

## Run locally

Requires Node.js 22.12+ (or a newer supported LTS) and npm. Start your existing backend on `http://localhost:8080` using your usual backend workflow, then:

```bash
cd frontend
npm ci
npm run dev
```

Open **http://localhost:5173** and sign in with an existing backend account. Vite forwards `/api/*`, including `/api/auth/*`, to the backend after stripping `/api`. Development requires no backend CORS change. Port 5173 is strict: stop an existing frontend server before starting another. No accounts or credentials are bundled with the frontend.

## Authentication and roles

- Login posts the entered username/password to `/auth/login`. Passwords stay in the login form only while it is mounted; they are never persisted, logged, or put in URLs.
- The access JWT stays in module memory. Protected requests automatically send `Authorization: Bearer <accessToken>`. Login, refresh, and logout are public calls and never send a Bearer header.
- Only the refresh token is persisted, under `campus.refreshToken` in `sessionStorage`. Reloading the same tab restores through `/auth/refresh` before protected content mounts. Successful refresh synchronously replaces the stored token with the rotated token. Failed restoration clears the session and shows Login.
- Access expiry triggers refresh and temporarily hides protected content. A protected `401` also triggers refresh and at most one retry. Concurrent failures share one refresh; a delayed response using an older access token reuses the already rotated token. `403` displays a permission error without refresh or logout.
- Logout immediately clears local tokens and protected content, then attempts `/auth/logout` using the captured refresh token. Local sign-out succeeds even when that request fails. Session generation checks discard late login/refresh/data responses; discarded rotated tokens are revoked on a best-effort basis.
- **ADMIN** lands at `/` and can use the administrative dashboard, students, courses, and enrollments. **STUDENT** lands at `/courses`, sees only the course navigation item, and can read course lists/details. Add/edit/delete actions are hidden for STUDENT.
- JWT `role`, `sub`, and `exp` are decoded for presentation. This is **not** signature verification or an authorization boundary: Spring Security enforces permissions. `sub` is a **User ID**, not a Student ID. No `/auth/me` endpoint exists, so student profiles and personal enrollments are not exposed in the STUDENT UI until the backend provides that identity mapping.

### Storage trade-offs

`sessionStorage` is tab-scoped and supports reloads without persisting the access token. Refresh coordination is local to one tab; duplicated tabs can copy storage and are not synchronized. Browser-accessible refresh tokens can be stolen by XSS. This SPA follows the current JSON token contract, **not an HttpOnly-cookie model**. A future cookie design requires backend coordination. Use HTTPS in production and protect the application from script injection. Browser storage must be available to sign in.

Logout cannot invalidate an already issued access JWT, which may remain valid on the server until its expiration. Server refresh-token revocation is best effort when offline; local tokens are cleared regardless.

## Features

- Dashboard counts from the actual student/course totals and enrollment array, plus a student directory preview and course enrollment counts. Failed requests show unavailable values and retry controls.
- Students and courses: paginated tables, column sorting, exact age/credits filters, create/edit forms, fetched detail pages, and confirmed deletion. The frontend respects the backend's sorting keys, integer limits, DTO field names, and validation.
- Enrollment registration/cancellation and details remain ADMIN-only. Searchable selectors collect **every** student/course page at size 100 using stable ID ordering; a failed page blocks registration rather than providing partial options. Enrollment pagination is explicitly deferred to Phase 2 (see below).
- Loading, empty, success, and error states. ProblemDetail field errors remain in forms, conflicts retain their backend messages, pending mutations block repeat submissions, and obsolete reads are aborted. Native dialogs support keyboard focus containment and Escape dismissal.
- Responsive sidebar/mobile navigation, scrollable tables, keyboard focus indicators, and reduced-motion support.

## Checks

```bash
npm run typecheck
npm run lint
npm test
npm run build
```

Tests cover the HTTP boundary, authentication lifecycle, rotation/concurrent-401/logout races, cancellation, 403 behavior, expiry, protected routes, and role-aware course controls, alongside the existing directory/validation tests. DOM tests use jsdom with React's native `act`; jsdom is a development-only dependency. Only isolated tests stub Fetch and use synthetic claims; the application contains no fake accounts, unsigned test tokens, or fallback datasets.

Full verification from a clean dependency install:

```bash
npm ci
npm run typecheck
npm run lint
npm test
npm run build
```

Real-account manual verification steps and the results actually executed are recorded in [authentication-verification.md](docs/authentication-verification.md).

## API base and production hosting

The default API base is `/api`. To override it, copy `.env.example` to `.env.local` and set `VITE_API_BASE_URL`. Vite reads this **at build time**; rebuild after changing it. Frontend environment variables are public and must never contain secrets.

For production, serve `dist/` from a web server that:

1. Forwards `/api/*` to the Spring Boot backend with the `/api` prefix removed.
2. Falls back to `index.html` for frontend routes such as `/students/1`.

For example, these blocks belong in an existing Nginx server configuration:

```nginx
root /path/to/frontend/dist;

location /api/ {
    proxy_pass http://127.0.0.1:8080/;
}

location / {
    try_files $uri $uri/ /index.html;
}
```

The trailing slash on `proxy_pass` strips the matching `/api/` prefix. Vite's development proxy is **not** part of the production bundle. `npm run preview` serves the built assets for inspection; configure a real reverse proxy to test production API routing. If `VITE_API_BASE_URL` points to another origin, that existing backend must already permit the frontend's origin. No backend configuration is changed by this project.

## Structure

```text
src/api/          Typed endpoints, Fetch client, API boundary tests
src/auth/         In-memory session store and authentication/routing tests
src/components/   Tables, fields, forms, dialogs, notifications, pagination
src/hooks/        Auth context, abortable reads, complete directories, notifications
src/layouts/      Desktop sidebar and mobile navigation
src/pages/        Dashboard, resources/details, enrollments, not-found route
src/types/        DTOs and paginated response contracts
src/utils/        Resource definitions and validation
docs/             Design, implementation plan, and verification record
```

## Operational limits

The current backend returns paginated objects for `/enrollments` and `/students/{id}/enrollments`, while the existing frontend screens expect arrays. The Enrollment Pagination upgrade is intentionally **not implemented in Phase 1**. A response-shape guard shows an explicit error for the new format, so the dashboard and detail pages stay usable instead of crashing or presenting incomplete counts. Enrollment totals, course enrollment counts, enrollment lists, and personal enrollment lists remain unavailable with that response format until Phase 2. Legacy array responses remain supported. This limitation does not affect course or student CRUD, single-enrollment details, registration, or cancellation endpoints.

Phase 2 should add typed paginated enrollment responses, server pagination/filtering/sorting, and dashboard totals from `totalElements`. Enrollment selectors still load complete student/course directories; very large deployments would benefit from a backend search endpoint. Student self-service needs a separate authenticated User-to-Student identity contract before adding personal profiles/enrollments. Production security and hosting follow the existing backend environment.
