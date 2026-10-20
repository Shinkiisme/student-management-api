# Authentication verification · 2026-10-20

## Scope and implementation

Only frontend/ changes. Existing backend AuthController, AuthService, SecurityConfig, JwtConfig, login/refresh DTOs, ownership checks, and security ProblemDetail handlers were inspected. No credentials, secrets, JWT keys, environment-file contents, backend files, database records, or migrations were changed or exposed. No commits or pushes.

The existing Fetch client handles public auth calls and protected requests; there is no parallel HTTP client. A memory-only access token and cached session snapshot connect to React through AuthProvider/context. sessionStorage holds only the current refresh token. Generation checks prevent late session results or old account data from reaching a replacement session. Shared refresh handles simultaneous and delayed 401s, caller cancellation, and at most one original-request retry. 403 does not alter authentication. Route guards wait for restoration/expiry renewal, and course controls/navigation follow the role claim without treating User ID as Student ID.

Added jsdom as a development-only dependency for actual React/Router DOM tests. No additional production dependencies.

## Automated verification

Executed successfully from frontend/:

```text
npm ci
npm run typecheck
npm run lint
npm test
npm run build
```

The final test suite includes the preserved HTTP, directory, and validation cases plus session storage/claim handling, login/invalid credentials, restoration/no stored token/failed restoration, logout, late login/refresh/previous-account responses, Bearer headers, successful/failed/shared/delayed refresh, retry limits, public auth requests, 403, cancellation including response-body reads, actual role guards/course controls, expiry gating, and login validation/visibility. Fetch and synthetic JWT fixtures are confined to isolated tests, not application behavior.

Final results: npm ci **PASS**; typecheck **PASS**; lint **PASS**; **66 tests in 7 files PASS** with no unhandled errors; production build **PASS**; git diff --check **PASS**. The final code checks were rerun after the review fix. npm ci reported zero vulnerabilities. Browser Login had no uncaught page errors.

Independent read-only review found no Critical or Important issues. It found one Minor cleanup gap: a token response rejected because sessionStorage is unavailable was not revoked. Two regression tests first failed because no logout request was made. Login/refresh now attempt revocation for any rejected token response, including storage failures, before clearing local state. The regression tests pass in the final suite.

## Browser and backend checks actually executed

- The running backend returned HTTP 401 to anonymous GET /courses.
- Visiting /students in an isolated Chromium session redirected to /login, with no protected content shown.
- Empty login submission showed two field errors and stored no session data. Password visibility toggled correctly.
- Login fit a 390×844 viewport with document width 390px.
- axe WCAG A/AA audits reported zero violations/incomplete checks on desktop and mobile Login.
- No verified backend account credentials were available. **Successful real-backend login, refresh rotation, role access, and logout were not tested in the browser.** These flows were exercised by automated tests; use the manual steps below for real accounts.

## Manual real-account verification

1. Run the existing backend on localhost:8080 and `npm run dev` in frontend/. Use existing accounts provisioned through your backend workflow; this frontend creates no users.
2. Open /students without a session. Confirm Login appears before any student data. Submit empty fields, then invalid credentials; confirm field errors or a 401 sign-in error with no refresh loop.
3. Sign in with an ADMIN account. Confirm landing at /, all four navigation links, and student/course mutation controls. Check successful API requests carry Bearer authentication. Do not copy or share token/credential values.
4. Reload. Confirm one POST /auth/refresh, a rotated stored refresh token, and protected content appearing only after restoration. Login and refresh must carry no Authorization header.
5. Allow the 900-second access token to expire. Confirm a single refresh even with concurrent requests. On failure/revocation, confirm Login and empty refresh-token storage. Expired content must not remain mounted while renewal is pending.
6. Log out while requests are pending, then reload. Confirm local state clears immediately and no late response restores the previous account. Repeat with server logout unavailable; local sign-out must still work.
7. Sign in with a STUDENT account. Confirm /courses landing, only course navigation, readable list/details, and no add/edit/delete buttons. Visit /, /students, /students/{User ID}, and /enrollments directly; confirm Access denied. User ID must never be inferred to be Student ID.
8. Trigger a real 403 through an appropriate backend policy case. Confirm a permission message without refresh or sign-out.

## Limits and Phase 2

- sessionStorage is script-accessible and carries XSS risk. Access tokens are not persisted. Refresh is coordinated within one tab, not across duplicated tabs. Server revocation is best effort when offline, and issued access JWTs can remain valid until expiration.
- No /auth/me or User-to-Student identity endpoint exists; STUDENT profiles/personal enrollments intentionally remain hidden.
- Both current enrollment list endpoints already return paginated objects. Phase 1 adds only a response-shape guard, keeping the UI from crashing or displaying incomplete totals. The actual pagination upgrade remains deferred. Phase 2 should update enrollment DTOs, server pagination/filtering/sorting, dashboard totals from totalElements, and their tests.

## Files added and modified

All paths below are relative to frontend/.

Added:

- `src/types/auth.ts`
- `src/auth/session.ts`
- `src/auth/session.test.ts`
- `src/auth/routing.test.tsx`
- `src/api/auth.ts`
- `src/api/auth.test.ts`
- `src/api/http.auth.test.ts`
- `src/hooks/authContext.ts`
- `src/components/AuthProvider.tsx`
- `src/components/ProtectedRoute.tsx`
- `src/pages/LoginPage.tsx`
- `src/pages/AccessDeniedPage.tsx`
- `src/test/authFixtures.ts`
- `docs/authentication-plan.md`
- `docs/authentication-verification.md`

Modified:

- `src/api/http.ts`
- `src/api/http.test.ts` (native Headers assertions and immediate abort rejection)
- `src/api/resources.ts` (response-shape guard only; no enrollment pagination upgrade)
- `src/App.tsx`
- `src/layouts/AppLayout.tsx`
- `src/pages/ResourcePage.tsx`
- `src/pages/ResourceDetailsPage.tsx`
- `package.json`
- `package-lock.json`
- `README.md`
