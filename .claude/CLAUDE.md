# Clean Architecture Kotlin Project

## Purpose

Subscription SaaS platform built with Clean Architecture + CQRS + Arrow-kt in Kotlin, using Spec-Driven Development.

## Read This First

Before planning, implementing, or reviewing changes, load context in this order:

1. `.specify/memory/constitution.md`
2. `.specify/memory/current-status.md`
3. `.specify/memory/feature-catalog.md`
4. Relevant `.specify/features/**` documents

Use `.specify/features/` and `.specify/memory/feature-catalog.md` as the primary navigation surface for existing behavior, invariants, and change impact. Do not start from source code unless those docs are missing or stale.

## Working Rules

- `constitution.md` is the normative source for architecture, quality gates, documentation rules, and path conventions.
- `current-status.md` is the source for implementation gaps, partial work, and temporary deviations.
- `feature-catalog.md` is the search index for implemented behavior.
- `implement-usecase` is for `domain/` and `application/` implementation only.
- The orchestrator owns `.specify/**` updates and may use the `feature-doc-maintainer` skill.
- Default implementation flow is `/speckit.cycle` after `/speckit.specify` and `/speckit.clarify`.
- `/speckit.cycle` is resume-aware: it must reuse existing `plan.md`, `tasks.md`, checklist state, and task completion state, then continue from the last incomplete stage.
- Use `/speckit.plan`, `/speckit.tasks`, and `/speckit.implement` directly only when explicit step-by-step control is needed.

## Skills

Use the project skills instead of restating their guidance here:

- `ca-kotlin`: Clean Architecture and CQRS implementation patterns
- `fp-kotlin`: functional programming patterns
- `arrow-kt`: `Either`, `either {}`, `.bind()`, error-mapping style
- `tdd-kotlin`: JUnit 5 + MockK + kotest-assertions workflow
- `jooq-ddl`: jOOQ and DDL-based persistence work
- `feature-doc-maintainer`: post-implementation updates to `.specify/features/**` and `.specify/memory/**`

## Fast References

- Specs: `.specify/specs/`
- Implemented feature docs: `.specify/features/`
- Search index: `.specify/memory/feature-catalog.md`
- Current status: `.specify/memory/current-status.md`
- Commands: `.claude/commands/`
- Agents: `.claude/agents/`
- Skills: `.claude/skills/`
- Rules: `.claude/rules/`

## Commands

- `./gradlew detekt`
- `./gradlew ktlintCheck`
- `./gradlew test`
- `./gradlew koverVerify`
- `./gradlew check`
