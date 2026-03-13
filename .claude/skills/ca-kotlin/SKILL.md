---
name: ca-kotlin
description: >
  Clean Architecture rules and patterns for Kotlin + Spring Boot + Arrow-kt projects.
  Use when implementing or reviewing any CA layer: domain modeling, use case design,
  repository patterns, infrastructure implementation, or controller wiring.
  Triggers on: domain entity design, value object creation, repository interface definition,
  use case implementation, jOOQ repository, REST controller, DI wiring.
---

# Clean Architecture — Kotlin

## Layer Dependency Direction

```
framework → presentation → application → domain ← infrastructure
```

## CQRS Pattern

The application layer is split into **command/** (writes) and **query/** (reads):

- **Command side**: `application/command/` → uses domain repository (in `domain/`) → returns domain entities
- **Query side**: `application/query/` → uses query repository (in `application/query/repository/`) → returns flat DTOs

## Key Patterns

| Concept | Pattern |
|---------|---------|
| Entity | `data class Entity(val id: EntityId, ...)` — `val` only, `.copy()` for updates |
| Value Object | `@JvmInline value class VO private constructor(val value: T)` with `of()` returning `Either` |
| State Machine | `sealed interface Status` with typed transitions as methods on each state |
| Domain Error | `sealed interface XxxError : DomainError` |
| Domain Service | `XxxDomainService` in `domain/service/` — stateless, multi-entity domain logic, returns `Either` |
| Domain Repo Interface | In `domain/`, aggregate persistence (`findById`, `save`, `delete`), returns `Either<XxxError, T>` |
| Command Use Case Interface | `interface CreateXxxUseCase` in `application/command/usecase/CreateXxxUseCase.kt` |
| Command Use Case Impl | `class CreateXxxUseCaseImpl` in `application/command/usecase/CreateXxxUseCaseImpl.kt` — uses domain repository for aggregate read/write |
| Query Repo Interface | In `application/query/repository/`, read methods, returns `Either<QueryError, QueryDto>` |
| Query Use Case Interface | `interface XxxListQueryUseCase` in `application/query/usecase/XxxListQueryUseCase.kt` |
| Query Use Case Impl | `class XxxListQueryUseCaseImpl` in `application/query/usecase/XxxListQueryUseCaseImpl.kt` — uses query repository |
| Query DTO | `data class XxxQueryDto(val id: Long, ...)` — flat primitives, no domain types |
| Infra Domain Repo | `@Repository class XxxRepositoryImpl(dsl: DSLContext)` in `infrastructure/command/repository/` (blocking JDBC) |
| Infra Query Repo | `@Repository class XxxQueryRepositoryImpl(dsl: DSLContext)` in `infrastructure/query/repository/` (blocking JDBC) |
| Shared Port | `ClockPort`, `TransactionPort` in `application/port/` — non-DB external deps |
| Command Port | `PaymentPort` in `application/command/port/` — non-DB external deps only (NOT for aggregate reads) |
| Query Port | `XxxQueryPort` in `application/query/port/` — non-DB external deps for query side |
| Shared Adapter | `@Component class XxxAdapter(...)` in `infrastructure/adapter/` — implements shared ports |
| Command Adapter | `@Component class XxxAdapter(...)` in `infrastructure/command/adapter/` — implements command ports |
| Query Adapter | `@Component class XxxAdapter(...)` in `infrastructure/query/adapter/` — implements query ports |
| REST Controller | `@RestController` injecting both command and query use cases |
| DI Wiring | `@Bean` for use case implementations in `framework/config/`; infrastructure repository/adapter implementations should use Spring stereotypes directly |

## Spring DI Rules

- Port interfaces in `application/**/port/` and `application/port/` MUST have no Spring annotations
- Domain repository interfaces in `domain/repository/` MUST have no Spring annotations
- Implementations in `infrastructure/**/repository/` MUST use `@Repository`
- Implementations in `infrastructure/**/adapter/` MUST use `@Component` by default
- Use case implementations in `application/**/usecase/` MUST NOT use `@Component` / `@Service`; define them as beans in `framework/config/`
- Use `@Bean` for infrastructure only when constructor logic is conditional, environment-specific, or otherwise more complex than standard component scanning

## Forbidden Imports by Layer

| Layer | Forbidden |
|-------|-----------|
| `domain` | `org.springframework.*`, `jakarta.*`, `*.application.*`, `*.infrastructure.*`, jOOQ |
| `application` | Same as domain + no infra/presentation imports |
| `presentation` | `*.infrastructure.*` only (may import `*.domain.*` for DTO mapping) |

## Strict Rules

- NO `throw` in domain/application/infrastructure — use `Either`
- NO `@Component`/`@Service` on use case classes — wire manually via `@Bean`
- DO use Spring stereotypes in infrastructure: `@Repository` for repositories, `@Component` for adapters
- NO new library deps in `domain` or `application` `build.gradle.kts`
- NO `var` in domain or application — immutability enforced
- NO `Instant.now()` / `LocalDate.now()` — use `ClockPort`

**This project uses jOOQ + JDBC with virtual threads (NOT JPA, NOT R2DBC).** All interface methods (repos, use cases, ports, controllers) are regular functions (not `suspend`). Blocking JDBC calls are safe on virtual threads.

## Cross-References

- [references/layer-rules.md](references/layer-rules.md) — detailed layer implementation examples
- `arrow-kt` skill — Either API patterns (`either{}`, `.bind()`, `mapLeft`, `Either.catch`, `.fold()`)
- `fp-kotlin` skill — immutability, entity `.copy()` rule, compensating transactions
- `jooq-ddl` skill — jOOQ codegen, migration rules, repository JDBC patterns
- `infrastructure-error-style` rule — RepositoryError patterns
- `test-conventions` rule — test framework rules (auto-applies to `*Test.kt`)
