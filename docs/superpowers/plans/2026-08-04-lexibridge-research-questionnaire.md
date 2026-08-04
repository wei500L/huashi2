# Lexi-Bridge Research Questionnaire Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the existing assessment loop with versioned research questionnaire metadata, public-code delivery, scoring metrics, and a first Lexi-Bridge seed package while preserving current class assessments.

**Architecture:** Keep `assessment_paper` as the immutable published snapshot and add additive research tables/entities around it. Introduce a public-code service/controller that reuses existing paper/attempt/scoring primitives, then expose typed frontend contracts and a `/research/:releaseCode` flow. Seed import is represented as a versioned JSON package plus preflight/commit services, with publication blocked by unresolved review findings.

**Tech Stack:** Spring Boot 4, MyBatis-Plus, MySQL/H2 SQL snapshots, React 19 + TypeScript, Vitest/JUnit.

---

### Task 1: Shared questionnaire enums and additive schema

**Files:** `shared-kernel/src/main/java/com/huashi/eftransfer/shared/enums/*`; `app-server/src/main/resources/schema.sql`; `app-server/src/test/resources/schema-h2.sql`

- [ ] Add delivery, transfer, context, construct, AI/import status, and quality flag enums with tolerant `fromCode` parsing.
- [ ] Add additive questionnaire/participant/code/import/review/timing/metric/AI tables and indexes to both SQL snapshots without changing existing assessment tables.
- [ ] Run app-server schema/bootstrap tests.

### Task 2: Research question metadata and public-code delivery

**Files:** `app-server/src/main/java/com/huashi/eftransfer/app/modules/assessment/{entity,mapper,dto,vo,service,controller}`

- [ ] Add question metadata fields and `justificationText` answer support.
- [ ] Extend publish DTO/service for `CLASS` and `PUBLIC_CODE`, generate one-time `XXXX-XXXX-XXXX` codes stored as HMAC digests, and make repeated verification/start/submit idempotent.
- [ ] Add unauthenticated `/api/public/assessments/**` endpoints with cookie-backed participant sessions and bounded timing updates.
- [ ] Add focused integration tests for code isolation, idempotency, and F-justification validation.

### Task 3: SCORING_V1 metrics and AI analysis contract

**Files:** `app-server/src/main/java/com/huashi/eftransfer/app/modules/assessment/service/*`; `shared-kernel/...`; `src/lib/contracts.ts`; `src/lib/services.ts`

- [ ] Implement weighted scoring, multi-select exact matching, dimension accuracy, three percentage-point deltas, reaction-time quartiles, and quality flags.
- [ ] Persist metric snapshots and idempotent AI analysis events with schema-validated `assessment-analysis/v1` output and rule-summary fallback.
- [ ] Extend result DTOs/contracts and add unit tests for scoring edge cases.

### Task 4: Import template and LEXIBRIDGE_RESEARCH_V1 seed

**Files:** `docs/lexibridge/*`; `app-server/.../assessment/import/*`

- [ ] Provide Questionnaire/Sections/Items/Options XLSX template and equivalent JSON schema/package.
- [ ] Add preflight diff/error reporting, review-required blocking, transactional commit, and seed content with 7 sections/60 scored items.
- [ ] Verify counts, answers, explanations, and research labels.

### Task 5: Teacher and public student UI

**Files:** `src/pages/teacher/*`; `src/pages/research/*`; `src/App.tsx`; `src/lib/contracts.ts`; `src/lib/services.ts`

- [ ] Add teacher questionnaire bank/version/import/release/data tabs and code export affordance.
- [ ] Add login-free `/research/:releaseCode` participant flow with autosave, resume, timing pause, submit, and result/AI panels.
- [ ] Run lint, typecheck, build, frontend tests, and app-server tests.

---
