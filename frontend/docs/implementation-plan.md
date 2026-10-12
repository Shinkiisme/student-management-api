# Student Management Frontend Implementation Plan

**Goal:** Implement the complete frontend requested in the attached brief.

**Architecture:** Separate typed API contracts, request hooks, reusable UI, resource definitions, and route pages. Use native dialogs and Fetch without a UI kit or server-state library.

**Tech stack:** React, TypeScript strict mode, Vite, Tailwind CSS, React Router, Lucide, ESLint, Vitest.

**Spec:** [design.md](design.md)

## Constraints and review focus

All changes stay in frontend/. Never modify backend files or existing data. No commits or deployment. Check field errors, aborted requests, unavailable summaries, multi-page selection, and empty final pages after deletion.

## Execution

- [x] Scaffold package, strict TypeScript, lint, Tailwind, /api proxy, and environment documentation.
- [x] Define DTO types and HTTP client; verify errors, 204 responses, query encoding, and all-page traversal with focused tests.
- [x] Build sidebar layout, notifications, tables, pagination, forms, and dialogs with keyboard accessibility.
- [x] Implement student/course list, exact filters, sorting, create/edit/delete flows, and fetched details.
- [x] Implement enrollment directory selection, registration/cancellation, details, and student-specific enrollments.
- [x] Build real-data dashboard summaries and course enrollment distribution.
- [x] Run lint, typecheck, tests, build, browser checks, and review; record outcomes in README and verification.md.

Implement directly in this session. The user's detailed build request authorizes the work; no extra design approval, commits, worktree, or hosting is required.
