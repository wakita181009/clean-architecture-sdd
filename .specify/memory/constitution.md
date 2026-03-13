<!--
Sync Impact Report
- Version change: 1.1.0 -> 1.2.0
- Modified principles: II. CQRS; added "Repository vs Port Boundary" section
- Added sections: "Repository vs Port Boundary (NON-NEGOTIABLE)"
- Removed sections: none
- Key changes:
  - Domain Repository now handles aggregate reads (findById, findByXxx) in addition to writes
  - "Command-side reads via ports" rule removed — aggregate reads go through domain Repository
  - CommandQueryPort pattern FORBIDDEN — use domain Repository instead
  - Port is strictly for non-DB external dependencies (Transaction, Clock, External API, Message Broker)
- Synced files:
  - ✅ .claude/skills/ca-kotlin/SKILL.md
  - ✅ .claude/skills/ca-kotlin/references/layer-rules.md
- Templates requiring updates:
  - ✅ .specify/templates/plan-template.md
  - ✅ .specify/templates/tasks-template.md (no CommandQueryPort references found)
  - ✅ .claude/agents/implement-usecase.md
- Follow-up TODOs: none (no CommandQueryPort code in source)
-->
# Subscription SaaS Platform Constitution

## Core Principles

### I. Clean Architecture — Strict Layer Separation (NON-NEGOTIABLE)

Module dependency direction: `framework → presentation → application → domain ← infrastructure`

- `domain`: Pure Kotlin + Arrow. NO Spring, NO Jakarta, NO framework imports.
- `application`: Depends only on `:domain`. NO Spring annotations (`@Component`, `@Service`).
- `infrastructure`: Implements domain/application interfaces. May use Spring, jOOQ, JDBC.
- `presentation`: Spring REST and GraphQL adapters. NO infrastructure imports. NO domain logic. Controller constructor dependencies on use cases MUST be non-nullable — all use cases are wired as `@Bean` in framework.
- `framework`: Wires everything. Owns `@SpringBootApplication`, `UseCaseConfig`, `DomainServiceConfig`.

Spring DI conventions for this repository:
- repository interfaces and port interfaces in `domain` / `application` have no Spring annotations
- implementations in `infrastructure/**/repository/` use `@Repository`
- implementations in `infrastructure/adapter/` and `infrastructure/**/adapter/` use `@Component`
- use case implementations in `application/**/usecase/` are wired explicitly with `@Bean` in `framework/config/`
- port interface names use `XxxPort`, and their implementations use `XxxAdapter`
- `XxxPortImpl` is forbidden; repository implementations use `XxxRepositoryImpl`

Layer purity enforced by `ForbiddenLayerImport` detekt rule (whitelist-based).

### Naming Rules (NON-NEGOTIABLE)

- Domain entities MUST be named `{Entity}`
- Value objects MUST be named `{ValueObject}`
- Domain errors MUST be named `{Entity}Error`
- Domain services MUST be named `{Xxx}DomainService`
- Repository interfaces MUST be named `{Entity}Repository`
- Repository implementations MUST be named `{Entity}RepositoryImpl`
- Command DTOs MUST be named `{Xxx}Dto`
- Query DTOs MUST be named `{Xxx}QueryDto`
- Command use cases MUST be named `{Entity}{Action}UseCase`
- Query use cases MUST be named `{Entity}{Action}QueryUseCase`
- Port interfaces MUST be named `{Xxx}Port`
- Port implementations in `infrastructure/**/adapter/` MUST be named `{Xxx}Adapter`
- `XxxPortImpl` is forbidden in this repository
- REST controllers MUST be named `{Entity}Controller`
- DTOs under `application/command/dto/` MUST all use the `{Xxx}Dto` pattern
- DTOs under `application/query/dto/` MUST use either `{Xxx}QueryDto` or `PageQueryDto<T>`
- Query errors MUST be named `{Entity}{Action}QueryError`
- Generic or cross-cutting ports MAY use the `{Xxx}Port` pattern
- Standard action names are `FindById`, `FindAll`, `List`, `Create`, `Update`, and `Delete`
- `FindById` means single-item lookup
- `FindAll` means multi-item lookup without paging
- `List` means multi-item lookup with paging

### II. CQRS — Command Query Responsibility Segregation (NON-NEGOTIABLE)

- **Command (write)**: `Presentation → Command UseCase → Domain Entity → Domain Repository`
- **Query (read)**: `Presentation → Query UseCase → Query Repository (bypass domain) → DTO`
- Domain repositories (`domain/repository/`): Save, delete, and aggregate-read methods (`findById`, `findByXxx`). Return domain entities/aggregates.
- Query repositories (`application/query/repository/`): Read methods ONLY. Return flat DTOs, NO domain types.
- Command-side reads that return aggregates MUST use domain repositories, NOT ports.
- Query ports: `application/query/port/`.
- Shared ports (e.g., `ClockPort`, `TransactionPort`): `application/port/`.

### Repository vs Port Boundary (NON-NEGOTIABLE)

- **Repository** is the abstraction for DB persistence ONLY. It is responsible for saving and reconstructing aggregates.
- All DB read/write operations MUST go through Repository interfaces.
- Even UseCase-specific reads MUST use Repository if the return type is an aggregate.
- **Port** is the abstraction for non-DB external dependencies ONLY: Transaction boundary control (not DB connection itself), Clock, External APIs, Message Brokers.
- `CommandQueryPort` pattern is FORBIDDEN — use domain Repository for aggregate reads.

### III. Functional Error Handling — Arrow-kt Either (NON-NEGOTIABLE)

- ALL fallible operations MUST return `Either<XxxError, T>`.
- `throw` is FORBIDDEN in domain, application, and infrastructure layers.
- Use `either {}` DSL with `.bind()` for composition. Imperative `return .left()` is BANNED.
- Presentation layer: `.fold()` on Either results; may throw `ResponseStatusException` only.
- Error hierarchies: `sealed interface` per layer (`DomainError`, `ApplicationError`, `QueryError`).
- Infrastructure: `Either.catch {}` for DB/external calls. Unknown DB values → `Either.Left`, NEVER silent fallback.

### IV. Domain Purity (NON-NEGOTIABLE)

- domain/ and application/ allowed external imports: `arrow.core.*`, `kotlinx.coroutines.*`, `org.slf4j.*` ONLY.
- Do NOT add library dependencies to `domain/build.gradle.kts` or `application/build.gradle.kts`.
- All time-dependent logic via `ClockPort`. NO direct `Instant.now()` or `LocalDate.now()`.
- Entity: `data class` with `val` only. State changes via `.copy()`.
- Value objects: `@JvmInline value class` with private constructor, `of(...)` for unsafe external input returning `Either`, and `invoke(...)` only for trusted internal values such as validated DB data.
- Domain enums that accept external string input MUST provide `of(value: String): Either<XxxError, EnumType>` using `entries.find {}` + `ensureNotNull`. Do NOT use `runCatching { valueOf(...) }` or bare `valueOf(...)`.
- Nullable ID for new entities (before DB persistence). NO `unsafeOf(0L)` placeholder.

### V. Immutability — FP Principles

- `var` FORBIDDEN in domain and application layers.
- Collections MUST be immutable (`List`, `Map`, NOT `MutableList`).
- Domain entities MUST be `data class` with `val` fields only.
- NO mutable state in domain services.

### VI. Test-First — TDD with JUnit 5 + MockK (NON-NEGOTIABLE)

- Red-Green-Refactor cycle strictly enforced.
- Test runner: JUnit 5 (`@Test`, `@Nested`, `@BeforeEach`). NOT Kotest spec styles.
- Assertions: kotest-assertions + kotest-assertions-arrow (`shouldBe`, `shouldBeRight`, `shouldBeLeft`, `shouldThrow`).
- Mocking: MockK `every {}` / `verify {}`. NOT `coEvery`/`coVerify` (virtual threads, not suspend).
- NO `runTest {}` or `runBlocking {}`.
- NO Kotest `ProjectConfig` or `SpringExtension` — JUnit 5 integrates with Spring natively.
- Coverage: 90%+ for domain and application, 80%+ for infrastructure and presentation (Kover).

## Technical Stack

| Component | Technology                                                                      |
|-----------|---------------------------------------------------------------------------------|
| Language | Kotlin 2.x (JVM)                                                                |
| Framework | Spring Boot 4.x + Virtual Threads (Project Loom)                                |
| Error handling | Arrow-kt (`Either<Error, Result>`)                                              |
| Database | PostgreSQL (H2 for testing)                                                     |
| ORM | jOOQ (DDL-based codegen from Flyway migrations, blocking JDBC)                  |
| Testing | JUnit 5 + MockK + kotest-assertions + kotest-assertions-arrow                     |
| Linting | ktlint + detekt (custom rules)                                                  |
| Coverage | Kover (domain/application 90% minimum, infrastructure/presentation 80% minimum) |
| Concurrency | Virtual Threads — NO reactive, NO suspend at boundaries                         |

## SDD Workflow Rules

### Task Decomposition

- **1 task = 1 use case** (command or query). Each task is independently implementable and testable.
- Each use case task includes: TDD test + domain changes (if command) + application layer code.
- Infrastructure, presentation, and framework wiring are separate tasks.
- Foundational tasks (entity, VO, error hierarchy, migration) come before use case tasks.

### Feature Documentation

- After implementing or modifying a use case, the implementation workflow MUST update `.specify/features/{entity}/{usecase}.md`.
- After implementing or modifying an entity, the implementation workflow MUST update `.specify/features/{entity}/overview.md`.
- For cross-cutting logic that spans multiple entities (billing, pricing, state machines, error mapping), create a dedicated doc under `.specify/features/{topic}/`.
- Update `.specify/memory/feature-catalog.md` with every new entry.
- Update `.specify/memory/current-status.md` when implementation is partial, deferred, or intentionally divergent from the spec.
- Before modifying existing code, consult feature docs first — NOT source code.
- The orchestrator owns documentation updates and may use a dedicated documentation skill to maintain `.specify/**`.

### Feature Doc Completeness (NON-NEGOTIABLE)

Every implemented feature document MUST be optimized for future search and safe modification.

- `overview.md` MUST include:
  - Purpose and scope
  - Business invariants
  - State machine or lifecycle rules, if applicable
  - Value objects and validation rules
  - Repository and persistence touchpoints
  - Entry-point code paths
  - Related use cases
  - Related tests
- `{usecase}.md` MUST include:
  - Business rules
  - Inputs and outputs
  - Error mapping
  - Touched files
  - Endpoints, ports, tables, and external dependencies
  - Related features
  - Change impact

### Searchability & Traceability

- `.specify/memory/feature-catalog.md` MUST support reverse lookup by entity, use case, endpoint, table, and cross-cutting concern.
- Each implemented use case MUST trace back to its spec source.
- If code changes invalidate an existing feature doc, the doc MUST be updated in the same change set.

### Path Conventions

| Artifact | Path |
|----------|------|
| Domain Entity | `domain/src/main/kotlin/.../domain/entity/{Entity}.kt` |
| Value Object | `domain/src/main/kotlin/.../domain/vo/{ValueObject}.kt` |
| Domain Error | `domain/src/main/kotlin/.../domain/error/{Entity}Error.kt` |
| Domain Service | `domain/src/main/kotlin/.../domain/service/{Xxx}DomainService.kt` |
| Domain Repository | `domain/src/main/kotlin/.../domain/repository/{Entity}Repository.kt` |
| Command DTO | `application/src/main/kotlin/.../application/command/dto/{Xxx}Dto.kt` |
| Command UseCase Interface | `application/src/main/kotlin/.../application/command/usecase/{Entity}{Action}UseCase.kt` |
| Command UseCase Impl | `application/src/main/kotlin/.../application/command/usecase/{Entity}{Action}UseCaseImpl.kt` |
| Command Error | `application/src/main/kotlin/.../application/command/error/{Entity}{Action}Error.kt` |
| Command Port | `application/src/main/kotlin/.../application/command/port/{Xxx}Port.kt` |
| Query UseCase Interface | `application/src/main/kotlin/.../application/query/usecase/{Entity}{Action}QueryUseCase.kt` |
| Query UseCase Impl | `application/src/main/kotlin/.../application/query/usecase/{Entity}{Action}QueryUseCaseImpl.kt` |
| Query Error | `application/src/main/kotlin/.../application/query/error/{Entity}{Action}QueryError.kt` |
| Query Repository | `application/src/main/kotlin/.../application/query/repository/{Entity}QueryRepository.kt` |
| Query DTO | `application/src/main/kotlin/.../application/query/dto/{Xxx}QueryDto.kt` |
| Page Query DTO | `application/src/main/kotlin/.../application/query/dto/PageQueryDto.kt` |
| Query Port | `application/src/main/kotlin/.../application/query/port/{Xxx}Port.kt` |
| Shared Port | `application/src/main/kotlin/.../application/port/{Xxx}Port.kt` |
| Infra Repository | `infrastructure/src/main/kotlin/.../infrastructure/command/repository/{Entity}RepositoryImpl.kt` |
| Infra Query Repo | `infrastructure/src/main/kotlin/.../infrastructure/query/repository/{Entity}QueryRepositoryImpl.kt` |
| Command Adapter | `infrastructure/src/main/kotlin/.../infrastructure/command/adapter/{Xxx}Adapter.kt` |
| Query Adapter | `infrastructure/src/main/kotlin/.../infrastructure/query/adapter/{Xxx}Adapter.kt` |
| Shared Adapter | `infrastructure/src/main/kotlin/.../infrastructure/adapter/{Xxx}Adapter.kt` |
| REST Controller | `presentation/src/main/kotlin/.../presentation/rest/{Entity}Controller.kt` |
| DI Config | `framework/src/main/kotlin/.../framework/config/UseCaseConfig.kt` |
| Migration | `infrastructure/src/main/resources/db/migration/V{NNN}__{description}.sql` |

**Base package**: `com.wakita181009.casdd`

## Governance

- Constitution supersedes all other practices.
- Amendments require documentation, version bump, and migration plan.
- All implementations MUST verify compliance with these principles.
- Violations detected by detekt custom rules are build failures.

**Version**: 1.3.0 | **Ratified**: 2026-03-11 | **Last Amended**: 2026-03-12
