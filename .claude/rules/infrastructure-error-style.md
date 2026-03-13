---
description: Applies when writing or reviewing infrastructure repository and adapter implementations
globs:
  - "infrastructure/**/*RepositoryImpl.kt"
  - "infrastructure/**/*Adapter.kt"
---

# Infrastructure Error Style Rules

## MANDATORY: RepositoryError subtype required

Every `sealed interface XxxError` used by a Repository MUST include `RepositoryError` for infrastructure failures. Domain logic MUST NOT use `RepositoryError`.

## FORBIDDEN: Placeholder values in error construction

```kotlin
// FORBIDDEN
.mapLeft { XxxError.NotFound(XxxId(0L)) }
// CORRECT
.mapLeft { XxxError.RepositoryError("query failed: ${it.message}") }
```

## MANDATORY: Separate DB exceptions from "not found"

`Either.catch` (infrastructure failure) and `ensureNotNull` (record absence) MUST map to DIFFERENT error types:

```kotlin
override fun findById(id: XxxId): Either<XxxError, Xxx> = either {
    val record = Either.catch {
        dsl.selectFrom(TABLE).where(TABLE.ID.eq(id.value)).fetchOne()
    }.mapLeft { XxxError.RepositoryError("findById failed: ${it.message}") }
        .bind()

    ensureNotNull(record) { XxxError.NotFound(id.value) }
    record.toDomain()
}
```

For full infrastructure patterns, see the `ca-kotlin` skill (`references/layer-rules.md`) and `jooq-ddl` skill.
