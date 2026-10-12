# Student management frontend

Build a local React/TypeScript university administration dashboard for the existing Spring Boot API. All changes stay inside `frontend/`; backend code, configuration, migrations, tests, and existing data remain unchanged. No commits, branches, deployments, or fabricated application data.

Use Vite, Tailwind CSS, React Router, Lucide icons, and native Fetch. Four routes share a responsive light sidebar layout: Dashboard, Students, Courses, Enrollments. Student/course details have dedicated routes. Reuse tables, pagination, accessible native dialogs, fields, notifications, and request-state handling.

Students and courses use backend pagination, approved sort keys, and exact numeric filters. Student code/course code are nonblank with maximum 20 characters; name is nonblank with maximum 100; age is an integer >= 0; credits are an integer > 0. Java integer inputs cannot exceed 2147483647. Mutations disable duplicate submission, refresh lists, and retain entered data on failure. Deleting the last row on a page moves to the preceding available page.

Enrollments are unpaginated and contain only id/studentId/courseId. Resolve labels from student/course records; keep numeric fallbacks if lookups fail. Selection loads all existing student/course pages with size 100 and id ascending, supports local search, and never silently presents a partial directory. Student filtering calls the dedicated student-enrollments endpoint. Student details also show course enrollments.

Dashboard counts come from paginated totals and the enrollment array; unavailable counts are unknown, never zero. A real-data course distribution may be derived from courses and enrollments; no invented trends, timestamps, user identity, or statistics.

Central HTTP module defaults to /api. Vite strips /api and forwards to localhost:8080. VITE_API_BASE_URL overrides the base at build time. Production requires an /api reverse proxy and SPA fallback, or an already configured cross-origin backend. Parse ProblemDetail including field errors, with sensible fallbacks for 400/404/409/500, non-JSON proxy errors, and network failure. Abort obsolete reads to avoid stale results.

Verification: strict TypeScript, ESLint, production build, focused HTTP/pagination/validation unit tests, real-backend browser navigation and responsive/accessibility checks. Do not alter existing backend data during verification.
