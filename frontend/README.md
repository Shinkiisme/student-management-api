# Campus · Student Management Frontend

React, strict TypeScript, Vite, Tailwind CSS, React Router, Lucide, and native Fetch. Application data comes entirely from the existing Spring Boot API.

## Run locally

Requires Node.js 22.12+ (or a newer supported LTS) and npm. Start your existing backend on `http://localhost:8080` using your usual backend workflow, then:

```bash
cd frontend
npm ci
npm run dev
```

Open **http://localhost:5173**. Vite forwards `/api/students`, `/api/courses`, and `/api/enrollments` to the backend after stripping `/api`. Development requires no backend CORS change. Port 5173 is strict: stop an existing frontend server before starting another.

## Features

- Dashboard counts from the actual student/course totals and enrollment array, plus a student directory preview and course enrollment counts. Failed requests show unavailable values and retry controls.
- Students and courses: paginated tables, column sorting, exact age/credits filters, create/edit forms, fetched detail pages, and confirmed deletion. The frontend respects the backend's sorting keys, integer limits, DTO field names, and validation.
- Enrollments: all returned registrations, readable names/codes with ID fallbacks, details, registration, cancellation, and per-student filtering. Student detail pages show their enrollments. Searchable selectors collect **every** student/course page at size 100 using stable ID ordering; a failed page blocks registration rather than providing partial options.
- Loading, empty, success, and error states. ProblemDetail field errors remain in forms, conflicts retain their backend messages, pending mutations block repeat submissions, and obsolete reads are aborted. Native dialogs support keyboard focus containment and Escape dismissal.
- Responsive sidebar/mobile navigation, scrollable tables, keyboard focus indicators, and reduced-motion support.

## Checks

```bash
npm run typecheck
npm run lint
npm test
npm run build
```

Unit tests cover the HTTP boundary (including validation, conflicts, network errors, aborts, and 204 responses), multi-page selection, and DTO-aligned validation. Only isolated tests stub Fetch; the application contains no mock records or fallback datasets.

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
src/components/   Tables, fields, forms, dialogs, notifications, pagination
src/hooks/        Abortable reads, complete directories, notification context
src/layouts/      Desktop sidebar and mobile navigation
src/pages/        Dashboard, resources/details, enrollments, not-found route
src/types/        DTOs and paginated response contracts
src/utils/        Resource definitions and validation
docs/             Design, implementation plan, and verification record
```

## Operational limits

Enrollment lists are intentionally unpaginated because the backend exposes an array. Enrollment selectors load complete directories; very large deployments would benefit from a backend search endpoint. Dashboard course counts cover the first five courses by ID and make no claims about trends or historical activity. The backend exposes no authentication/user endpoint, so this frontend does not invent login or user permissions. Production security and hosting follow the existing backend environment.
