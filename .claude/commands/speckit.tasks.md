---
description: Generate an actionable, dependency-ordered tasks.md for the feature based on available design artifacts.
handoffs: 
  - label: Analyze For Consistency
    agent: speckit.analyze
    prompt: Run a project analysis for consistency
    send: true
  - label: Implement Project
    agent: speckit.implement
    prompt: Start the implementation in phases
    send: true
---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

## Pre-Execution Checks

**Check for extension hooks (before tasks generation)**:
- Check if `.specify/extensions.yml` exists in the project root.
- If it exists, read it and look for entries under the `hooks.before_tasks` key
- If the YAML cannot be parsed or is invalid, skip hook checking silently and continue normally
- Filter to only hooks where `enabled: true`
- For each remaining hook, do **not** attempt to interpret or evaluate hook `condition` expressions:
  - If the hook has no `condition` field, or it is null/empty, treat the hook as executable
  - If the hook defines a non-empty `condition`, skip the hook and leave condition evaluation to the HookExecutor implementation
- For each executable hook, output the following based on its `optional` flag:
  - **Optional hook** (`optional: true`):
    ```
    ## Extension Hooks

    **Optional Pre-Hook**: {extension}
    Command: `/{command}`
    Description: {description}

    Prompt: {prompt}
    To execute: `/{command}`
    ```
  - **Mandatory hook** (`optional: false`):
    ```
    ## Extension Hooks

    **Automatic Pre-Hook**: {extension}
    Executing: `/{command}`
    EXECUTE_COMMAND: {command}
    
    Wait for the result of the hook command before proceeding to the Outline.
    ```
- If no hooks are registered or `.specify/extensions.yml` does not exist, skip silently

## Outline

1. **Setup**: Run `.specify/scripts/bash/check-prerequisites.sh --json` from repo root and parse FEATURE_DIR and AVAILABLE_DOCS list. All paths must be absolute. For single quotes in args like "I'm Groot", use escape syntax: e.g 'I'\''m Groot' (or double-quote if possible: "I'm Groot").

2. **Load design documents**: Read from FEATURE_DIR:
   - **Required**: plan.md (tech stack, libraries, structure), spec.md (user stories with priorities)
   - **Optional**: data-model.md (entities), contracts/ (interface contracts), research.md (decisions), quickstart.md (test scenarios)
   - Note: Not all projects have all documents. Generate tasks based on what's available.

3. **Execute task generation workflow**:
   - Load plan.md and extract tech stack, libraries, project structure
   - Load spec.md and extract user stories with their priorities (P1, P2, P3, etc.)
   - If data-model.md exists: Extract entities and map to user stories
   - If contracts/ exists: Map interface contracts to user stories
   - If research.md exists: Extract decisions for setup tasks
   - Generate tasks organized by user story (see Task Generation Rules below)
   - Generate dependency graph showing user story completion order
   - Create parallel execution examples per user story
   - Validate task completeness (each user story has all needed tasks, independently testable)

4. **Generate tasks.md**: Use `.specify/templates/tasks-template.md` as structure, fill with:
   - Correct feature name from plan.md
   - Phase 1: Setup tasks (project initialization)
   - Phase 2: Foundational tasks (blocking prerequisites for all user stories)
   - Phase 3+: One phase per user story (in priority order from spec.md)
   - Each phase includes: story goal, independent test criteria, tests (if requested), implementation tasks
   - Final Phase: Polish & cross-cutting concerns
   - All tasks must follow the strict checklist format (see Task Generation Rules below)
   - Clear file paths for each task
   - Dependencies section showing story completion order
   - Parallel execution examples per story
   - Implementation strategy section (MVP first, incremental delivery)

5. **Plan-to-Tasks Alignment Gate**: After tasks.md is generated, verify it covers ALL implementation work declared for this slice in plan.md. This gate prevents scope gaps where plan.md commits to behavior or components but tasks.md omits them.

   **5.1. Extract declared implementation items from plan.md**:
   - From **Scope**: user stories and use cases included in this slice
   - From **Out of scope**: excluded stories/layers/integrations that must NOT be required in tasks.md
   - From **Project Structure**: only file paths or components explicitly described as new or changed in this slice
   - From **Test Strategy**: required test obligations for touched layers in this slice
   - From **Feature Doc Planning**: docs explicitly planned for this slice
   - From **Scope Alignment** section (if present): scenarios marked in current slice and not deferred

   **5.2. Extract task coverage from tasks.md**:
   - Map each task to:
     - user story
     - layer
     - file path or component
     - task type (`Red`, `Green`, `Refactor`, `Verify`, `Docs`)
   - Build a coverage set for declared implementation items in this slice

   **5.3. Build coverage matrix**:

   | Declared item (from plan.md) | Story | Layer | Has task? | Task ID(s) |
   |------------------------------|-------|-------|-----------|------------|
   | `SubscriptionCreateUseCaseImpl` | US1 | application | ✓ | T025, T026, T028 |
   | `SubscriptionController`        | US1 | presentation | N/A deferred | — |
   | `subscription/create.md`        | US1 | docs | ✓ | T048 |

   **5.4. Gate evaluation**:
   - **PASS** if every declared in-scope item has matching tasks
   - **PASS** if an item is absent from tasks.md because it is explicitly deferred in `Out of scope`
   - **FAIL** if an in-scope item has no corresponding task
   - **FAIL** if tasks.md introduces substantial out-of-scope work not declared in the plan

   **5.5. Remediation on FAIL**:
   - Add missing tasks to the relevant user story phase whenever possible
   - Preserve story-first organization
   - Only use a cross-cutting/foundational/polish phase when the item is truly shared
   - Ensure every touched layer in scope still has Red → Green → Refactor → Verify coverage
   - Re-run the matrix until all in-scope items are covered

   **5.6. Record evidence**:
   - Output the coverage matrix as part of the report
   - Note tasks added during remediation

6. **Report**: Output path to generated tasks.md and summary:
   - Total task count
   - Task count per user story
   - Task count per layer (domain, application, infrastructure, presentation, framework)
   - Parallel opportunities identified
   - Independent test criteria for each story
   - Suggested MVP scope (typically just User Story 1)
   - Format validation: Confirm ALL tasks follow the checklist format (checkbox, ID, labels, file paths)
   - Plan-to-Tasks Alignment Gate result: PASS or PASS after remediation (with list of added tasks)

7. **Check for extension hooks**: After tasks.md is generated, check if `.specify/extensions.yml` exists in the project root.
   - If it exists, read it and look for entries under the `hooks.after_tasks` key
   - If the YAML cannot be parsed or is invalid, skip hook checking silently and continue normally
   - Filter to only hooks where `enabled: true`
   - For each remaining hook, do **not** attempt to interpret or evaluate hook `condition` expressions:
     - If the hook has no `condition` field, or it is null/empty, treat the hook as executable
     - If the hook defines a non-empty `condition`, skip the hook and leave condition evaluation to the HookExecutor implementation
   - For each executable hook, output the following based on its `optional` flag:
     - **Optional hook** (`optional: true`):
       ```
       ## Extension Hooks

       **Optional Hook**: {extension}
       Command: `/{command}`
       Description: {description}

       Prompt: {prompt}
       To execute: `/{command}`
       ```
     - **Mandatory hook** (`optional: false`):
       ```
       ## Extension Hooks

       **Automatic Hook**: {extension}
       Executing: `/{command}`
       EXECUTE_COMMAND: {command}
       ```
   - If no hooks are registered or `.specify/extensions.yml` does not exist, skip silently

Context for task generation: $ARGUMENTS

The tasks.md should be immediately executable - each task must be specific enough that an LLM can complete it without additional context.

## Task Generation Rules

**CRITICAL**: Tasks MUST be organized by user story to enable independent implementation and testing.

**Tests are REQUIRED**: Always generate test tasks. This repository is TDD-mandatory, so every user story and every touched layer must include Red, Green, Refactor, and Verify tasks.

### Clean Architecture CQRS Task Rule (PROJECT-SPECIFIC)

**1 Task = 1 Use Case**: Each use case (command or query) MUST be a separate task. This is NON-NEGOTIABLE.

- **Command UseCase task**: Includes domain entity/VO changes + command use case interface/impl + TDD test
- **Query UseCase task**: Includes query DTO + query repository interface + query use case interface/impl + TDD test
- **Foundational tasks** (before use cases): Entity creation, value objects, error hierarchies, domain services, Flyway migration
- **Infrastructure tasks** (after use cases): jOOQ repository impl, port adapters, DI wiring
- **Presentation tasks** (after infrastructure): REST controllers, request/response DTOs
- **Every touched layer must have test tasks first**: domain/application unit tests, infrastructure integration tests, presentation controller/contract tests
- **domain/application tasks** → delegated to `implement-usecase` agent
- **infrastructure/presentation/framework tasks** → handled by orchestrator directly

Task naming convention:
- `T0XX [US?] Create {Entity} entity with value objects in domain/entity/`
- `T0XX [US?] Implement {Entity}{Action}UseCase (command) in application/command/usecase/`
- `T0XX [US?] Implement {Entity}{Action}QueryUseCase (query) in application/query/usecase/`
- `T0XX [US?] Implement {Entity}RepositoryImpl in infrastructure/command/repository/`
- `T0XX [US?] Create {Entity}Controller in presentation/rest/`
- `T0XX [US?] Wire {Entity} use cases in framework/config/UseCaseConfig.kt`

### Checklist Format (REQUIRED)

Every task MUST strictly follow this format:

```text
- [ ] [TaskID] [P?] [Story?] Description with file path
```

**Format Components**:

1. **Checkbox**: ALWAYS start with `- [ ]` (markdown checkbox)
2. **Task ID**: Sequential number (T001, T002, T003...) in execution order
3. **[P] marker**: Include ONLY if task is parallelizable (different files, no dependencies on incomplete tasks)
4. **[Story] label**: REQUIRED for user story phase tasks only
   - Format: [US1], [US2], [US3], etc. (maps to user stories from spec.md)
   - Setup phase: NO story label
   - Foundational phase: NO story label  
   - User Story phases: MUST have story label
   - Polish phase: NO story label
5. **Description**: Clear action with exact file path

**Examples**:

- ✅ CORRECT: `- [ ] T001 Create project structure per implementation plan`
- ✅ CORRECT: `- [ ] T005 [P] Implement authentication middleware in src/middleware/auth.py`
- ✅ CORRECT: `- [ ] T012 [P] [US1] Create User model in src/models/user.py`
- ✅ CORRECT: `- [ ] T014 [US1] Implement UserService in src/services/user_service.py`
- ❌ WRONG: `- [ ] Create User model` (missing ID and Story label)
- ❌ WRONG: `T001 [US1] Create model` (missing checkbox)
- ❌ WRONG: `- [ ] [US1] Create User model` (missing Task ID)
- ❌ WRONG: `- [ ] T001 [US1] Create model` (missing file path)

### Task Organization

1. **From User Stories (spec.md)** - PRIMARY ORGANIZATION:
   - Each user story (P1, P2, P3...) gets its own phase
   - Map all related components to their story:
     - Models needed for that story
     - Services needed for that story
     - Interfaces/UI needed for that story
     - If tests requested: Tests specific to that story
   - Mark story dependencies (most stories should be independent)

2. **From Contracts**:
   - Map each interface contract → to the user story it serves
   - If tests requested: Each interface contract → contract test task [P] before implementation in that story's phase

3. **From Data Model**:
   - Map each entity to the user story(ies) that need it
   - If entity serves multiple stories: Put in earliest story or Setup phase
   - Relationships → service layer tasks in appropriate story phase

4. **From Setup/Infrastructure**:
   - Shared infrastructure → Setup phase (Phase 1)
   - Foundational/blocking tasks → Foundational phase (Phase 2)
   - Story-specific setup → within that story's phase

### Phase Structure

- **Phase 1**: Setup (project initialization)
- **Phase 2**: Foundational (blocking prerequisites - MUST complete before user stories)
- **Phase 3+**: User Stories in priority order (P1, P2, P3...)
   - Within each story: Red tests → Green implementation → Refactor → Verify
   - Each phase should be a complete, independently testable increment
- **Final Phase**: Polish & Cross-Cutting Concerns

### TDD Enforcement Rules

- Every Green task MUST reference the Red task IDs it satisfies.
- Never emit an implementation task without at least one preceding failing-test task in the same story.
- Every story MUST include verification tasks that run real tests and reject `NO-SOURCE`.
- Generated tasks MUST include a coverage verification task that enforces `domain`/`application` 90%+ and `presentation` 80%+.
