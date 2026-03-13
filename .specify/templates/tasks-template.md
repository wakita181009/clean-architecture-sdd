---

description: "Task list template for feature implementation"
---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tests are MANDATORY. Every user story and every use case must be implemented with TDD. Test tasks are never optional, and every Green task must map to preceding Red task(s).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- This repository is a fixed multi-module Kotlin project
- Production code lives under:
  - `domain/src/main/kotlin/.../domain/`
  - `application/src/main/kotlin/.../application/`
  - `infrastructure/src/main/kotlin/.../infrastructure/`
  - `presentation/src/main/kotlin/.../presentation/rest/`
  - `presentation/src/main/kotlin/.../presentation/graphql/` when GraphQL is introduced
  - `framework/src/main/kotlin/.../framework/`
- Test code lives under the matching module `src/test/kotlin/` tree
- Tasks MUST reference concrete module paths, not generic `src/` or `backend/` placeholders

<!-- 
  ============================================================================
  IMPORTANT: The tasks below are SAMPLE TASKS for illustration purposes only.
  
  The /speckit.tasks command MUST replace these with actual tasks based on:
  - User stories from spec.md (with their priorities P1, P2, P3...)
  - Feature requirements from plan.md
  - Entities from data-model.md
  - Endpoints from contracts/
  
  Tasks MUST be organized by user story so each story can be:
  - Implemented independently
  - Tested independently
  - Delivered as an MVP increment
  
  DO NOT keep these sample tasks in the generated tasks.md file.
  ============================================================================
-->

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [ ] T001 Create project structure per implementation plan
- [ ] T002 Initialize [language] project with [framework] dependencies
- [ ] T003 [P] Configure linting and formatting tools

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

Examples of foundational tasks (adjust based on your project):

- [ ] T004 Setup database schema and migrations framework
- [ ] T005 [P] Implement authentication/authorization framework
- [ ] T006 [P] Setup API routing and middleware structure
- [ ] T007 Create base models/entities that all stories depend on
- [ ] T008 Configure error handling and logging infrastructure
- [ ] T009 Setup environment configuration management

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - [Title] (Priority: P1) 🎯 MVP

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### TDD Cycle for User Story 1

> **MANDATORY**: Red -> Green -> Refactor. Do not create implementation tasks without corresponding failing-test tasks first.

- [ ] T010 [P] [US1] Red: create failing domain/use-case test coverage for [behavior] in [module]/src/test/[path]
- [ ] T011 [P] [US1] Red: create failing contract/integration test coverage for [boundary behavior] in [module]/src/test/[path]
- [ ] T012 [US1] Green: implement the minimum production code in [module]/src/main/[path] to satisfy T010-T011 only after confirming both tests fail for the intended reason
- [ ] T013 [US1] Refactor: clean up production/test code for [behavior] while keeping all US1 tests green
- [ ] T014 [US1] Verify: run targeted tests for US1 and record the exact command/output summary in the task notes (reject `NO-SOURCE`)

### Implementation for User Story 1

- [ ] T015 [P] [US1] Red: create failing test for [Entity1/business rule] in [module]/src/test/[path]
- [ ] T016 [US1] Green: implement [Entity1/business rule] in [module]/src/main/[path] only after T015 fails as expected
- [ ] T017 [US1] Refactor: simplify [Entity1/business rule] code/tests without changing behavior

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - [Title] (Priority: P2)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### TDD Cycle for User Story 2

- [ ] T018 [P] [US2] Red: create failing domain/use-case test coverage for [behavior] in [module]/src/test/[path]
- [ ] T019 [P] [US2] Red: create failing contract/integration test coverage for [boundary behavior] in [module]/src/test/[path]
- [ ] T020 [US2] Green: implement the minimum production code in [module]/src/main/[path] to satisfy T018-T019 only after confirming both tests fail for the intended reason
- [ ] T021 [US2] Refactor: clean up production/test code for [behavior] while keeping all US2 tests green
- [ ] T022 [US2] Verify: run targeted tests for US2 and record the exact command/output summary in the task notes (reject `NO-SOURCE`)

### Implementation for User Story 2

- [ ] T023 [P] [US2] Red: create failing test for [Entity/business rule] in [module]/src/test/[path]
- [ ] T024 [US2] Green: implement [Entity/business rule] in [module]/src/main/[path] only after T023 fails as expected
- [ ] T025 [US2] Refactor: simplify [Entity/business rule] code/tests without changing behavior

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - [Title] (Priority: P3)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### TDD Cycle for User Story 3

- [ ] T026 [P] [US3] Red: create failing domain/use-case test coverage for [behavior] in [module]/src/test/[path]
- [ ] T027 [P] [US3] Red: create failing contract/integration test coverage for [boundary behavior] in [module]/src/test/[path]
- [ ] T028 [US3] Green: implement the minimum production code in [module]/src/main/[path] to satisfy T026-T027 only after confirming both tests fail for the intended reason
- [ ] T029 [US3] Refactor: clean up production/test code for [behavior] while keeping all US3 tests green
- [ ] T030 [US3] Verify: run targeted tests for US3 and record the exact command/output summary in the task notes (reject `NO-SOURCE`)

### Implementation for User Story 3

- [ ] T031 [P] [US3] Red: create failing test for [Entity/business rule] in [module]/src/test/[path]
- [ ] T032 [US3] Green: implement [Entity/business rule] in [module]/src/main/[path] only after T031 fails as expected
- [ ] T033 [US3] Refactor: simplify [Entity/business rule] code/tests without changing behavior

**Checkpoint**: All user stories should now be independently functional

---

[Add more user story phases as needed, following the same pattern]

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] TXXX Update `.specify/features/**` docs for implemented behavior
- [ ] TXXX Update `.specify/memory/feature-catalog.md` reverse-lookup indexes
- [ ] TXXX Update `.specify/memory/current-status.md` for remaining gaps or partial work
- [ ] TXXX Run full `./gradlew check` and capture whether tests actually executed (reject `NO-SOURCE`)
- [ ] TXXX Run `./gradlew koverVerify` and capture module-level coverage; fail if `domain` or `application` is below 90% or if `infrastructure` or `presentation` is below 80%
- [ ] TXXX Verify coverage and architecture gates pass before any task is marked complete
- [ ] TXXX Performance optimization across all stories
- [ ] TXXX Security hardening
- [ ] TXXX Run quickstart.md validation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - May integrate with US1 but should be independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - May integrate with US1/US2 but should be independently testable

### Within Each User Story

- Red tasks MUST be completed before any Green task
- Red tasks MUST be executed and shown to fail for the intended reason before any Green task is marked in progress
- Green tasks MUST implement only the minimum code needed to pass the failing tests
- Refactor tasks MUST not change behavior and MUST leave all related tests green
- Verify tasks MUST run real tests; `NO-SOURCE` is a failure
- Story completion requires matching Red/Green/Refactor/Verify evidence for every touched layer
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- Independent Red tasks for a user story can run in parallel
- Independent Refactor tasks for separate files can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch Red tasks for User Story 1 together:
Task: "Red: create failing domain/use-case test coverage for [behavior]"
Task: "Red: create failing contract/integration test coverage for [boundary behavior]"

# Then run the matching Green task:
Task: "Green: implement the minimum production code for [behavior]"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 using Red -> Green -> Refactor
4. **STOP and VALIDATE**: Run User Story 1 tests independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Red/Green/Refactor → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Red/Green/Refactor → Test independently → Deploy/Demo
4. Add User Story 3 → Red/Green/Refactor → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently

---

## Alignment Check

*Verified by /speckit.tasks. This section records only the final gate outcome for this slice.*

**Plan-to-Tasks Alignment**: [PASS / PASS after remediation]

**Explicitly deferred**:
- [None or deferred items from plan.md Out of scope]

**Tasks added during remediation**:
- [None or task IDs]

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Never mark a task done unless the corresponding test file path and verification command are known
- Never mark a Green task done without a recorded failing-test step and a passing verification step
- Documentation updates are part of completion, not optional follow-up
- Documentation update tasks are executed by the orchestrator using the `feature-doc-maintainer` skill
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
