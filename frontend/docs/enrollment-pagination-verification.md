# Phase 2 verification · 2026-10-20

## Summary and contract

Both enrollment list APIs now return `PageResponse<Enrollment>` with content/page/size/totalElements/totalPages. `list(query, signal)` sends pagination, sorting, and an optional studentId to `/enrollments`; `byStudent(studentId, query, signal)` sends pagination/sorting to `/students/{studentId}/enrollments`, whitelisting the four query keys so studentId is never repeated in the query. The typed sort keys are exactly id/studentId/courseId. POST still sends studentId/courseId, GET by ID still returns one enrollment, and DELETE still accepts 204.

The main table and student detail enrollment table reuse Pagination and EnrollmentTable. Defaults are page 0, size 10, ID ascending. Sort/filter/size changes reset to page zero. Mutations refresh the current query; when its page becomes invalid, recovery requests the last valid page without briefly displaying an impossible record range. Existing AbortSignal cleanup ignores late responses from superseded queries. Names/codes, selection, dialogs, notifications, and authenticated requests are preserved.

Dashboard counts come from totalElements for each resource. The enrollment count fetch requests only page zero with size one. The course panel shows actual credit requirements for the first five courses by ID, using their course DTOs. There is no inferred course enrollment distribution and no all-enrollment download. Failed summary requests remain unavailable, while independent cards/panels stay usable and retain retries.

## Checks actually executed

From frontend/:

```text
npm run typecheck  PASS
npm run lint       PASS
npm test           PASS — 100 tests in 11 files
npm run build     PASS
git diff --check  PASS
```

The original 66 tests passed before changes. They remain present; the routing test's enrollment URL fixture and obsolete Phase 1 warning assertion were updated for the intentional paginated contract. All existing session/login/refresh/logout/header/403/role tests pass. Four API tests first failed against the old enrollment signatures, then passed after the contract change.

Added coverage exercises query encoding and complete page metadata, optional filtering, student path without redundant ID, AbortSignal/Bearer propagation, unchanged mutations, next/previous/size navigation, valid/invalid filters, filter/sort resets, leading-zero ID normalization, empty states, last-row page recovery, registration refresh on the current page, request retry, 401/403, and superseded response cancellation. Student detail tests cover multiple pages, metadata counts, course labels, selected Student ID rather than JWT User ID, sorting/size, empty data, missing student, and 403/404. Dashboard tests use totals larger than content length, verify a size-one enrollment request, true empty datasets, credit labels instead of partial counts, and independent failures/retries for all three resources.

Tests use isolated Fetch fixtures and jsdom; no database records were created, updated, or deleted. jsdom's missing native dialog methods are emulated only in the mutation test environment. No production authentication substitutes or dependencies were added.

Independent read-only review approved the change with no confirmed correctness/security defects. Three optional boundary suggestions were added as passing tests: student-enrollment 401 sign-out after failed refresh, student page recovery to zero after the dataset shrinks, and main-page recovery when cancellation leaves no records. All checks above were rerun after these additions.

An isolated Chromium session visited `/enrollments?studentId=1` anonymously and correctly redirected to `/login`, with no uncaught page errors. This verifies the live frontend's anonymous guard, not authenticated pagination or backend integration.

At verification time, localhost:8080 was unreachable. **No authenticated real-backend ADMIN pagination or mutation verification is claimed.** No credentials were supplied or discovered. Use the read-only manual checks below once the existing backend is running.

## Read-only manual verification

1. Start the backend using your existing workflow, then run `npm run dev` in frontend/. Sign in with an existing ADMIN account. Do not create, modify, or delete records solely for verification.
2. Visit Enrollments. Confirm the request is `/api/enrollments?page=0&size=10&sortBy=id&direction=asc`, the table displays content rows, and the badge/footer use totalElements. Browse Next/Previous if enough records exist and change rows per page.
3. Select a student. Confirm the list request uses `/api/enrollments?...&studentId=...` with page zero, rather than the student-path endpoint. Clear the filter and confirm the parameter is omitted. An unknown positive Student ID can return an empty filtered page; malformed/zero/negative IDs show validation errors.
4. Click Enrollment ID, Student ID, and Course ID headers. Confirm only supported sortBy values are sent, direction toggles, and each new sort starts at page zero. Names/codes remain readable.
5. Open an existing student's detail page. Confirm the selected Student ID appears in `/api/students/{id}/enrollments` and is absent from its query string. Check count, pagination, sorting, and Manage enrollments navigation. Do not substitute JWT sub for the Student ID.
6. Visit Dashboard. Confirm its enrollment request has size one and its count equals totalElements, even when content has fewer rows. Confirm the course panel labels actual credit requirements. Simulate a failed read request using browser network tooling and verify independent cards remain usable with an error/retry for the failed resource.
7. Sign in with an existing STUDENT account. Confirm only permitted course screens are accessible; Dashboard, Students, and Enrollments remain denied. Reload/sign out to check the existing authentication workflow. No backend identity endpoint is invented.
8. Registration/deletion refresh and last-page recovery are verified by isolated automated tests. To check these manually later, use a separately provisioned disposable test database, not existing production/development data.

## Known limits

- No backend aggregate course enrollment endpoint exists; the credit overview intentionally replaces that distribution.
- Statistics are separate reads, not one transactional database snapshot. Server-side concurrent data changes can shift pages; recovery handles an out-of-range page.
- Student/course selection still loads their existing directories across all pages. Very large directories would benefit from backend search endpoints.
- STUDENT self-service remains unavailable until the backend provides an authenticated User-to-Student identity contract. Authentication and role guards are unchanged.

## File inventory

All paths are relative to frontend/.

Added:

- `src/api/enrollments.test.ts`
- `src/pages/EnrollmentsPage.test.tsx`
- `src/pages/StudentEnrollments.test.tsx`
- `src/pages/DashboardPage.test.tsx`
- `src/test/pageHarness.tsx`
- `docs/enrollment-pagination-plan.md`
- `docs/enrollment-pagination-verification.md`

Modified:

- `src/types/api.ts`
- `src/api/resources.ts`
- `src/components/EnrollmentTable.tsx`
- `src/pages/EnrollmentsPage.tsx`
- `src/pages/ResourceDetailsPage.tsx`
- `src/pages/DashboardPage.tsx`
- `src/auth/routing.test.tsx` (only changed enrollment fixture/assertion)
- `README.md`

Only frontend/ is changed. The Phase 1 auth implementation, existing HTTP client, package files/dependencies, backend code/configuration/migrations/tests, and database data are unchanged. No commits or pushes.
