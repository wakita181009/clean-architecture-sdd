---
description: Applies when writing or reviewing Kotlin code that returns Either
globs:
  - "**/*.kt"
---

# Arrow Either Style Rules

## MANDATORY: Use `either {}` DSL for ALL functions returning Either

Every function that returns `Either` MUST use the `either {}` DSL. No exceptions.

## FORBIDDEN patterns

- `value.left()` / `value.right()` as bare expressions
- `.fold({ return it.left() }) { it }` imperative early-return
- `return SomeError.left()`
- `.getOrNull() ?: return ...`
- `runCatching { ... }` — use `Either.catch {}` or a VO `of()` factory instead

## Required alternatives

- `either {}` + `.bind()` for chaining
- `ensure(cond) { error }` for boolean checks
- `ensureNotNull(value) { error }` for null checks
- `.mapLeft()` + `.bind()` at layer boundaries
- `.fold(ifLeft, ifRight)` for consuming in presentation only

For comprehensive examples and rationale, see the `arrow-kt` skill.
