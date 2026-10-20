# Enrollment pagination and dashboard integration

**Goal:** Implement Phase 2 inside frontend/ while preserving Phase 1 authentication, existing UI, and all backend data. No new dependencies, commits, or backend edits.

**Architecture:** Reuse PageResponse, the existing Fetch client/useAsync, Pagination, and EnrollmentTable. Add typed enrollment queries with only id/studentId/courseId sort keys. Keep table queries server-side and resolve labels through the existing student/course directories.

## API contract

- list(query, signal): GET /enrollments with page/size/sortBy/direction and optional studentId; returns PageResponse<Enrollment>.
- byStudent(studentId, query, signal): GET /students/{studentId}/enrollments with page/size/sortBy/direction only; returns PageResponse<Enrollment>.
- Single-enrollment GET, POST body, DELETE, 204 handling, authentication and AbortSignal behavior stay unchanged.

## UI and data decisions

Main list starts at page 0/size 10/id ascending. Changing filter, sort, or size resets page to zero. Main student filtering uses /enrollments?studentId=...; invalid IDs are rejected before the request. Keep the URL student filter and searchable directory selector. Display content rows and totalElements counts. Creation/deletion refreshes the current query; an empty out-of-range page recovers to the last valid page, including zero after all rows disappear. useAsync cancels/ignores obsolete reads.

Student detail enrollments have independent pagination/sorting using the student-path endpoint, with no redundant studentId query. Show totalElements, preserve labels and manage link, and keep student self-service hidden until an identity contract exists.

Dashboard global totals come from totalElements, using size 1 for the enrollment total request. Replace the course enrollment distribution with real course credit requirements for the first five courses by ID. Do not infer course enrollment counts from one enrollment page or fetch all enrollment pages. Each summary/panel retains its own request state and retry.

## Review focus

- Filters/sorts must reset pagination and only send backend-supported sort keys.
- A deletion or concurrent data change can invalidate the current page; recovery must not loop or display an impossible page range.
- Late/aborted responses must not replace a newer filter/page.
- Dashboard totals can exceed content length; course overview must remain usable when enrollment totals fail.
- STUDENT routes must not call administrative enrollment endpoints; Phase 1 auth implementation must remain unchanged.

## Execution

- [x] Add failing API tests for paginated contract, filtering, sorting, signals/auth, and unchanged mutations; update typed API functions.
- [x] Add sortable enrollment table and server pagination/filtering to main list with page recovery.
- [x] Update student detail enrollment pagination/counts/sorting.
- [x] Replace dashboard array assumptions and misleading distribution with accurate metadata/credits.
- [x] Add DOM tests for pagination, filter/sort resets, mutation refresh/recovery, stale reads, detail errors, and partial dashboard failures. Preserve auth tests, updating only the intentionally changed enrollment assertion/fixture.
- [x] Update current README, record verification/manual steps, run typecheck/lint/all tests/build, and independent code review. No database mutations during verification.
