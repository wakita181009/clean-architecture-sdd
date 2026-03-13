# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]
**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

[Extract from feature spec: primary requirement + technical approach from research]

## Scope

[List the user stories, use cases, layers, and deliverables included in this slice]

## Out of scope

[List the user stories, layers, integrations, or deliverables explicitly deferred from this slice]

## Test Strategy

**TDD Requirement**: This feature MUST be implemented test-first. For every use case, the first code change must be in `src/test` (or module-equivalent test directory), followed by the minimal production change to make the test pass, followed by refactoring with tests still green.

**Required Test Levels**:

- Acceptance / independent verification for each user story:
- Application / use case tests to drive orchestration and error mapping:
- Domain tests for business rules, state machines, and calculations:
- Contract / integration tests required for external boundaries:
- Coverage targets by module:
  - `domain`: 90%+
  - `application`: 90%+
  - `infrastructure`: 80%+ 
  - `presentation`: 80%+

**Completion Gates**:

- No task is complete unless corresponding automated tests exist
- Every Green task must cite the Red test task(s) it satisfies; implementation-first sequencing is invalid
- Every user story must include explicit Red, Green, Refactor, and Verify tasks across all touched layers
- `./gradlew test` must execute real tests; `NO-SOURCE` is a failure, not a pass
- `./gradlew koverVerify` must pass with `domain` and `application` at 90%+ and `infrastructure` and `presentation` at 80%+
- `./gradlew check` must pass before the feature is marked complete
- Coverage / lint / architecture gates from the constitution remain mandatory

**Per-Story Test Mapping**:

- US1:
- US2:
- US3:
- [Add one entry per user story, mapping acceptance scenarios to concrete test files/types]

## Feature Doc Planning

- Planned entities / topics:
- Planned use cases:
- Planned cross-cutting docs:
- Existing features to update:
- Expected reverse-lookup keys:
  - endpoints:
  - tables:
  - search tags:

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: Kotlin 2.x (JVM)
**Framework**: Spring Boot 4.x + Virtual Threads (Project Loom)
**Primary Dependencies**: Arrow-kt (Either), jOOQ (DDL-based codegen), Flyway
**Storage**: PostgreSQL (H2 for testing)
**Testing**: JUnit 5 + MockK + kotest-assertions + kotest-assertions-arrow (TDD mandatory)
**Target Platform**: JVM server (Spring MVC on virtual threads)
**Project Type**: Web service (REST-first, GraphQL optional) — Clean Architecture + CQRS
**Architecture**: 5 Gradle modules: domain, application, infrastructure, presentation, framework
**Concurrency**: Virtual Threads — blocking JDBC, no suspend/reactive
**Constraints**: `domain`/`application` 90%+ coverage, `infrastructure`/`presentation` 80%+ coverage (Kover), detekt custom rules enforced

## Delivery Order

Each user story MUST be delivered in this order:

1. Define independent acceptance test strategy
2. Write failing tests first (Red)
3. Implement minimal code to pass tests (Green)
4. Refactor with tests still passing (Refactor)
5. Run targeted tests plus `./gradlew koverVerify`
6. Run `./gradlew check`

The generated plan must make this ordering explicit; implementation-first sequencing is invalid.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

[Gates determined based on constitution file]

## Scope Alignment

*GATE: Must pass after Phase 1 design. Plan cannot be finalized with silent scope omissions.*

<!--
  Fill this matrix by scanning spec.md acceptance scenarios included in the current delivery slice.
  Every in-scope scenario must be either:
  1) represented by planned work in this slice, or
  2) explicitly deferred in Out of scope.
  Do not mark a scenario as failed only because another layer is deferred intentionally.
-->

| Scenario | In current slice? | Business behavior planned? | Additional layers required in this slice? | Explicitly deferred? | Covered? |
|----------|-------------------|----------------------------|-------------------------------------------|----------------------|----------|
| [US1-1]  | ?                 | ?                          | ?                                         | ?                    | ?        |

**Gate Result**: [PASS / PASS after remediation / FAIL]

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths (e.g., apps/admin, packages/something). The delivered plan must
  not include Option labels.
-->

```text
# Clean Architecture + CQRS (fixed structure)
domain/src/main/kotlin/.../domain/
├── entity/              # Domain entities (data class, val only)
├── vo/                  # Value objects (@JvmInline value class)
├── error/               # Domain errors (sealed interface)
├── service/             # Domain services (multi-entity logic)
└── repository/          # Repository interfaces (write-only: save, delete)

application/src/main/kotlin/.../application/
├── command/
│   ├── usecase/         # Command use cases (interface + impl)
│   ├── error/           # Command-specific errors
│   ├── dto/             # Command DTOs ({Xxx}Dto)
│   └── port/            # Command-side ports (non-DB external deps only)
├── query/
│   ├── usecase/         # Query use cases (interface + impl)
│   ├── error/           # Query-specific errors
│   ├── dto/             # Query DTOs ({Xxx}QueryDto, PageQueryDto)
│   └── repository/      # Query repository interfaces (read-only)
├── port/                # Shared ports (ClockPort, TransactionPort)
└── error/               # Shared ApplicationError base

infrastructure/src/main/kotlin/.../infrastructure/
├── adapter/             # Shared port adapters (TransactionAdapter, ClockAdapter etc.)
├── command/
│   ├── repository/      # Domain repository implementations (jOOQ)
│   └── adapter/         # Command port adapters (non-DB external deps)
├── query/
│   └── repository/      # Query repository implementations (jOOQ)
└── config/              # JooqConfig etc.

presentation/src/main/kotlin/.../presentation/
├── rest/                # REST controllers
└── graphql/             # GraphQL adapters (only if GraphQL is introduced)

framework/src/main/kotlin/.../framework/
├── Application.kt       # @SpringBootApplication
└── config/              # UseCaseConfig, DomainServiceConfig
```

**Structure Decision**: Clean Architecture with CQRS — 5 Gradle modules with strict dependency direction.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |

## Documentation Deliverables

- Feature overview docs to create/update:
- Use case docs to create/update:
- Feature catalog entries to add/update:
- Current status updates required:
