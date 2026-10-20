# Authentication foundation · 2026-10-20

**Goal:** Implement the requested Phase 1 authentication directly, changing only frontend/. Keep the Campus UI, native Fetch API, and existing resource pages. No commits or backend changes.

## Design

Use a small in-memory session store, read by React through useSyncExternalStore/context. Expose status, role, User ID, and expiry to presentation; keep the access token outside React state and persist only the refresh token in sessionStorage. Decode JWT role/sub/exp for presentation, with malformed/expired tokens rejected; the backend remains authoritative.

Extend api/http.ts with authenticated request handling and single-flight refresh using the same transport for public auth endpoints. A session generation changes on login/logout/invalidation. Every protected request, login result, and refresh result checks its generation before committing or returning data. Refresh rotates storage synchronously before publishing new credentials. Late tokens from discarded login/refresh responses are revoked on a best-effort basis. An aborted caller stops waiting without aborting the shared refresh. A late 401 from an older access token reuses the already refreshed token rather than rotating again. Retry a protected request at most once; 403 never refreshes or logs out.

AuthProvider restores through refresh before mounting protected content and refreshes at access-token expiry, also checking expiry on window focus/visibility changes. Guards hide content during restoration/expiry and distinguish anonymous login redirects from role denial. ADMIN lands at /; STUDENT lands at /courses and receives read-only course list/details. Never derive Student ID from JWT sub. Session generations also key the protected layout so previous-account page state is discarded.

Login uses existing fields/buttons/error components, required labels, password visibility, and a submission lock. Logout clears local state synchronously before making its public revocation request. No registration or account recovery endpoints are invented.

The backend now returns PageResponseDTO for both enrollment list endpoints. Phase 2 pagination stays deferred. Add only a response-shape guard in existing enrollment API functions, preserving legacy arrays and surfacing a helpful error for paginated responses instead of crashing or displaying incomplete counts.

## Review focus

- Concurrent/delayed 401 responses must share refresh and rotate once.
- Pending refresh/login/protected responses must not revive or leak a previous session after logout/new login.
- Aborted requests must not retry, cancel other refresh waiters, or become generic network errors.
- Expired/malformed/restoring sessions must never mount protected content.
- STUDENT navigation and both course list/detail actions must remain read-only; no User-to-Student ID assumption.

## Execution

- [x] Add session/JWT types and state store; test claim validation and storage policy.
- [x] Extend existing HTTP client with auth headers, refresh, cancellation, generation checks, and retry limits; test races and retain existing HTTP tests.
- [x] Add auth API operations/context, restore/expiry lifecycle, and login/logout flows; test success/failure/restoration/invalidation.
- [x] Add route guards and login/denied pages, role-aware sidebar/logout, read-only courses, and enrollment shape guard; test actual routes/presentation in a DOM environment.
- [x] Update README and manual verification; run npm ci, typecheck, lint, all tests, build, and independent review. Browser authentication success is only claimed if real credentials are available and actually used.
