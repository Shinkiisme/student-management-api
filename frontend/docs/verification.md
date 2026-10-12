# Verification · 2026-10-12

## Automated checks

- `npm run typecheck`: passed with TypeScript strict mode.
- `npm run lint`: passed with zero warnings/errors.
- `npm test`: 3 files, 26 tests passed. Covers configurable API bases, request payloads, ProblemDetail field errors/conflicts, non-JSON HTTP failures, network failures, aborted reads, 204 deletion, age zero, positive credits, integer/text limits, full directory pagination, and failure/abort on a later page.
- `npm run build`: passed; outputs `dist/`.
- Dependency installation reported zero vulnerabilities.

## Browser checks against the real backend

The existing backend was available at localhost:8080; all browser API requests used the Vite /api proxy. No backend source, migration, configuration, or test files were edited.

- Dashboard initially showed 8 students, 5 courses, and 12 enrollments, matching backend responses.
- Age 20 filtering returned the three matching students. Five-row student pagination showed IDs 6, 7, 8 on page two.
- Student-name sorting toggled between ascending and descending and returned the actual backend order.
- Empty student submission showed three field errors. A temporary student was created with age zero, loaded through its details endpoint, and edited successfully.
- Zero course credits were rejected. A temporary course was created, loaded through its details endpoint, and edited to five credits. Filtering by five credits returned that course.
- Enrollment registration used the complete real student/course directories. Student filtering used /students/{id}/enrollments. Enrollment details loaded the single-record endpoint and showed readable names.
- Duplicate enrollment returned 409, retained both selections, and displayed the backend detail. Deletion of the enrolled temporary course returned the resource-in-use 409 without removing it.
- Enrollment cancellation succeeded through confirmation. Deleting the only course on page two returned the list to page one. Student deletion succeeded and navigated back to the list.
- The test created one student, one course, and one enrollment. All three temporary records were removed through the UI. A JSON comparison against the pre-test snapshot confirmed all original student, course, and enrollment records were unchanged.
- Invalid IDs, unknown students, missing resources, and unknown routes showed useful error/empty states. An unknown student in the URL initialized registration with an empty selection.
- At 390×844, the document remained 390px wide; tables scrolled within their container. The student dialog was 358px wide with no internal horizontal overflow. Mobile navigation opened, trapped focus, closed with Escape, and restored focus.
- A browser-only 100-character unbroken title exposed mobile overflow. Title/detail text now wraps; the same title kept the document at 390px wide. No backend record was changed for this layout check.
- WCAG A/AA axe checks reported zero violations on the student list, student detail, enrollment dialog, mobile navigation, mobile student form, and dashboard. Automated contrast checks were inconclusive for dashboard gradient text and one mobile dialog description; screenshot inspection confirmed the description did not overlap and the surfaces/text were legible. This is a targeted check, not a full accessibility certification.

## Independent review and fixes

The requesting-code-review skill's reviewer found no Critical issue and one Important keyboard issue: Enter in a directory search input implicitly submitted the enrollment form with the existing selections. The behavior was reproduced using the temporary student/course, then corrected. Repeating the same keyboard interaction kept the dialog open with no submission or error; deliberate submission still produced the expected duplicate 409.

Two smaller findings were also fixed: directory matching now uses Set membership instead of quadratic includes checks, and an unknown initial student ID becomes an empty selection. Browser checking additionally found focus returning to the document body after dialog unmount; explicit restoration now returns focus to the opening control.

Backend authentication policy and concurrent data changes remain responsibilities of the existing backend. Complete directory pagination follows stable ID order but is not a database snapshot. Enrollment pagination/search cannot be added without backend endpoints, so the documented full-directory/unpaginated behavior is intentional.

## Repository scope

Only `frontend/` is new. No tracked backend files were modified. No commits, branch changes, or deployments were performed. Generated dependencies, build output, and TypeScript build metadata are ignored by frontend/.gitignore. Run/setup and production reverse-proxy instructions are in ../README.md.
