---
name: fp-kotlin
description: >
  Functional programming patterns for Kotlin with Arrow-kt. Use when writing immutable domain
  models, designing pure functions, working with compensating transactions, or applying
  sealed interface error hierarchies.
---

# FP in Kotlin with Arrow-kt

## Core Principles

1. **Immutability** — `val` over `var`; entities use `.copy()` for updates; collections are `List`/`Map`
2. **Explicit Errors** — `Either<Error, Value>` instead of exceptions (except presentation layer)
3. **Type-Driven Design** — make illegal states unrepresentable via value objects and sealed state machines
4. **Total Functions** — handle all inputs; no throwing in domain/application/infrastructure

For Either API patterns (`either {}`, `.bind()`, `mapLeft`, `Either.catch`, `.fold()`), see the `arrow-kt` skill.
For state machines, entity patterns, and layer structure, see the `ca-kotlin` skill (`references/layer-rules.md`).

## Entity Method Return Consistency

Entity methods returning `Either<Error, Entity>` MUST always return via `copy()` — even if no fields changed. Never return `this` directly.

```kotlin
// FORBIDDEN — ambiguous intent (forgot to update? or intentional no-op?)
fun processEndOfPeriod(now: Instant): Either<SubscriptionError, Subscription> = either {
    ensure(status == Active) { SubscriptionError.InvalidStatus(status) }
    if (cancelAtPeriodEnd) {
        copy(status = Canceled, canceledAt = now, updatedAt = now)
    } else {
        this@Subscription  // ← bare this is FORBIDDEN
    }
}

// CORRECT — copy() makes intent explicit
fun processEndOfPeriod(now: Instant): Either<SubscriptionError, Subscription> = either {
    ensure(status == Active) { SubscriptionError.InvalidStatus(status) }
    if (cancelAtPeriodEnd) {
        copy(status = Canceled, canceledAt = now, updatedAt = now)
    } else {
        copy(updatedAt = now)  // ← explicit no-op via copy()
    }
}
```

### Why

- `copy()` makes "intentionally unchanged" distinguishable from "forgot to update"
- Consistent pattern across all entity methods simplifies review
- If a timestamp field like `updatedAt` exists, the no-op branch should still update it

## Compensating Transaction Pattern

```kotlin
either {
    val reserved = reserveInventory(items).bind()
    paymentPort.charge(total, method)
        .mapLeft { error ->
            reserved.forEach { (id, qty) ->
                resourceRepo.release(id, qty)  // compensate on failure
            }
            CreateOrderError.PaymentFailed(error)
        }.bind()
}
```

## Extension Functions for Layer Mapping

```kotlin
// Domain → Presentation (in presentation layer)
fun Entity.toResponse() = EntityResponse(
    id = id.value,
    status = status::class.simpleName ?: "unknown",
)

// Request → Application DTO (in presentation layer)
fun CreateXxxRequest.toInput() = CreateXxxInput(name = name)

// DB Record → Domain (in infrastructure layer)
fun XxxRecord.toDomain() = Entity(id = XxxId(id!!), name = Name(name!!))
```
