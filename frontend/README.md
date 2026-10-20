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

- Dashboard global student, course, and enrollment counts come from each paginated response's `totalElements`. The enrollment count requests only page 0 with size 1. The course snapshot shows actual credit requirements for the first five courses by ID; it does not infer course enrollment counts from one page or download all enrollments. Failed requests show unavailable values and independent retry controls.
- Students and courses: paginated tables, column sorting, exact age/credits filters, create/edit forms, fetched detail pages, and confirmed deletion. The frontend respects the backend's sorting keys, integer limits, DTO field names, and validation.
- ADMIN enrollment management includes server pagination, sorting by enrollment/student/course ID, and server-side student filtering. Student details show a separately paginated enrollment list with the correct student total. Registration/cancellation, details, names/codes, notifications, and confirmation dialogs are preserved. Searchable selectors collect **every** student/course page at size 100 using stable ID ordering; a failed page blocks registration rather than providing partial options.
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

Phase 2 pagination/dashboard tests and read-only manual checks are described in [enrollment-pagination-verification.md](docs/enrollment-pagination-verification.md). Mutation refresh/recovery tests use isolated in-memory fixtures and do not modify a database.

## Enrollment API and paging

The frontend uses the existing authenticated HTTP client:

```typescript
enrollmentsApi.list(query, signal)                  // Promise<PageResponse<Enrollment>>
enrollmentsApi.byStudent(studentId, query, signal)  // Promise<PageResponse<Enrollment>>
enrollmentsApi.get(id, signal)                     // Promise<Enrollment>
```

`list` sends `GET /enrollments?page=0&size=10&sortBy=id&direction=asc` and adds `studentId` only when filtering. `byStudent` sends `GET /students/{studentId}/enrollments` with the four pagination/sorting parameters; identity appears only in the path. Both responses contain `content`, `page`, `size`, `totalElements`, and `totalPages`.

- Page indices start at zero; default size is 10. Supported page sizes are 1–100; the UI offers 5, 10, 25, 50, and 100.
- Allowed enrollment sort keys are exactly `id`, `studentId`, and `courseId`, with `asc` or `desc` direction. Student/course column sorting uses their IDs while retaining readable names and codes.
- Changing student filter, sort, or page size resets to page zero. The selected student filter remains in the URL; clearing it removes the server filter. Invalid IDs show an error before an enrollment request is sent.
- Counts and navigation use server metadata, while table rows use `content`. Registration and cancellation refresh the current page. If deletion or a concurrent change makes it invalid, the list moves to the last valid page. Obsolete reads are aborted/ignored through the existing request hook.
- POST `/enrollments` still sends `{ studentId, courseId }`; DELETE `/enrollments/{id}` still accepts HTTP 204. JWT injection, refresh rotation, session cleanup, and 401/403 handling are unchanged.

Enrollment endpoints remain ADMIN-only in the frontend. Student detail uses the selected resource's Student ID; JWT `sub` remains a User ID and is never substituted for it.

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

The backend does not expose aggregate course enrollment counts, so the dashboard deliberately shows course credit requirements instead of a global enrollment distribution. Summaries are independent requests, not a transactional snapshot.

Enrollment selectors still load complete student/course directories; very large deployments would benefit from backend search endpoints. Student self-service needs a separate authenticated User-to-Student identity contract before adding personal profiles/enrollments. Production security and hosting follow the existing backend environment. Earlier documents under docs/ record previous phases; the Phase 2 verification document describes current enrollment behavior.
