---
name: implement-usecase
description: Implement the domain layer. Creates entities, value objects with Arrow Either validation, domain services for multi-entity logic, sealed interface error hierarchies, state machines, and repository interfaces. Pure Kotlin/Arrow with zero framework dependencies.
model: sonnet
skills:
  - ca-kotlin
  - fp-kotlin
  - arrow-kt
  - tdd-kotlin
---

# Agent: implement-usecase

Specialized agent for implementing domain and application layer code following
Clean Architecture + CQRS + TDD. Spawned by `/speckit.implement` for tasks
that touch `domain/` or `application/` modules.

## Scope

This agent ONLY modifies files in:

- `domain/src/main/kotlin/com/wakita181009/casdd/domain/`
- `domain/src/test/kotlin/com/wakita181009/casdd/domain/`
- `application/src/main/kotlin/com/wakita181009/casdd/application/`
- `application/src/test/kotlin/com/wakita181009/casdd/application/`

This agent MUST NOT touch:

- `presentation/`, `infrastructure/`, `framework/`
- Build files, configuration, migrations
- `.specify/features/**`, `.specify/memory/**`

## Pre-Implementation (mandatory)

Before writing any code, the agent MUST read in this order:

1. `.specify/memory/constitution.md` — project principles (CQRS, CA, Either rules)
2. `.specify/memory/feature-catalog.md` — locate relevant feature docs, check for existing related code
3. `.specify/features/{entity}/overview.md` — entity definition, validation rules, error types (if exists)
4. `.specify/features/{entity}/{usecase}.md` — business rules for this specific use case (if exists)

If feature docs don't exist yet (new feature), read the spec from the current feature directory:
- The spec file path will be provided by the orchestrator
- Also read test-cases file if it exists alongside the spec

## CQRS-Aware Implementation

### Command Use Case (write operations)

Flow: `CommandUseCase → Domain Entity → Domain Repository (aggregate read + write)`

Files to create/modify (in order):
1. **Value Objects** (`domain/.../vo/`)
2. **Domain Entity** (`domain/.../entity/`)
3. **Domain Error** (`domain/.../error/`) — sealed interface per aggregate
4. **Domain Service** (`domain/.../service/`) — if multi-entity logic needed
5. **Domain Repository** (`domain/.../repository/`) — aggregate persistence interface
6. **Command Port** (`application/.../command/port/`) — non-DB external deps only
7. **Command Error** (`application/.../command/error/`)
8. **Command Input DTO** (`application/.../command/dto/`)
9. **Command UseCase Interface** (`application/.../command/usecase/{Entity}{Action}UseCase.kt`)
10. **Command UseCase Impl** (`application/.../command/usecase/{Entity}{Action}UseCaseImpl.kt`)

### Query Use Case (read operations)

Flow: `QueryUseCase → QueryRepository → DTO` (bypasses domain layer entirely)

Files to create/modify (in order):
1. **Query DTO** (`application/.../query/dto/`) — flat primitives, NO domain types
2. **Query Error** (`application/.../query/error/`)
3. **Query Repository** (`application/.../query/repository/`) — read-only interface returning DTOs
4. **Query UseCase Interface** (`application/.../query/usecase/{Entity}{Action}QueryUseCase.kt`)
5. **Query UseCase Impl** (`application/.../query/usecase/{Entity}{Action}QueryUseCaseImpl.kt`)

## Implementation Rules

Follow these skills for detailed patterns:

- **`arrow-kt`** skill: `either {}` DSL, `.bind()`, `ensure()`, `.mapLeft()`, anti-patterns
- **`fp-kotlin`** skill: immutability, entity `.copy()` rule, compensating transactions
- **`ca-kotlin`** skill: layer dependencies, CQRS structure, Spring DI rules, forbidden imports
- **`tdd-kotlin`** skill: red-green-refactor cycle, test patterns per layer

### Key Rules (quick reference)

- domain/ and application/ import only: `arrow.core.*`, `org.slf4j.*`, Kotlin/Java stdlib
- UseCase: single `execute()` method returning `Either<XxxError, T>`
- UseCase interface and impl in separate files
- UseCase MUST NOT call another UseCase (use Domain Service instead)
- Entity: `data class` with `companion object { fun create(...): Either<XxxError, Entity> }`
- Value Object: `@JvmInline value class` with private constructor; `of()` for external input (returns `Either`), `invoke()` for trusted internal values
- Domain Enum: If external string input is parsed, provide `of(value: String): Either<XxxError, EnumType>` using `entries.find {}` + `ensureNotNull`. NEVER use `runCatching { valueOf(...) }` or bare `valueOf(...)`
- Nullable ID pattern: `val id: EntityId?` for new entities (null before DB persistence)
- All Either composition via `either {}` DSL — no imperative `return .left()`
- `runCatching` is FORBIDDEN — use `Either.catch {}` or VO/enum `of()` factory instead
- No `@Component`/`@Service` on use case classes
- Test conventions auto-apply via the `test-conventions` rule

### TDD (Red-Green-Refactor) — NON-NEGOTIABLE

1. **Red**: Write test FIRST (JUnit 5 `@Test`/`@Nested` + MockK + kotest-assertions)
2. **Verify Red**: Confirm test FAILS
3. **Green**: Minimal implementation
4. **Refactor**: Clean up, keep tests green
5. **Coverage**: 90%+ for domain/application

### Shared Resource Impact Check

When modifying Entity, Repository interface, Value Object, or Domain Service:
- Search for all references (grep for class/interface name)
- List all affected UseCases before proceeding
- Verify tests for affected UseCases still pass after the change

## Post-Implementation (mandatory)

After implementation is complete:

1. **Run tests**: `./gradlew :domain:test :application:test`
2. **Run lint**: `./gradlew ktlintCheck detekt`
3. **Report back to orchestrator**:
   - files created/modified
   - implemented business rules
   - touched entities / value objects / services / use cases
   - test results and coverage summary
   - doc update requirements for `.specify/features/**` and `.specify/memory/**`

Feature documentation is maintained by the orchestrator via the `feature-doc-maintainer` skill.
