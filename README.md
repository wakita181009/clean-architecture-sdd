# Clean Architecture SDD

**An LLM-native development scaffold** — this project provides the architectural foundation, specifications, and AI tooling for LLMs (Claude Code) to build a production-grade Kotlin application **from scratch** using **Spec-Driven Development (SDD)** and **Test-Driven Development (TDD)**.

## What This Project Is

This is **not** a traditional application template. It is a **development environment designed for LLMs** to autonomously implement features end-to-end.

The human defines **what** to build (natural language specs). The LLM handles **how** — generating code, tests, and infrastructure across all layers, guided by:

- **SpecKit** (`.specify/`) — A specification-first workflow that turns feature descriptions into structured specs, implementation plans, and task breakdowns
- **Clean Architecture rules** (`.claude/CLAUDE.md`) — Strict layer boundaries, CQRS, and FP conventions that the LLM must follow
- **Skills & Agents** (`.claude/skills/`, `.claude/agents/`) — Specialized knowledge modules (Arrow-kt, jOOQ, TDD, CA patterns) that teach the LLM project-specific idioms
- **Custom Detekt rules** (`detekt-rules/`) — Compile-time enforcement of architecture rules (no cross-layer imports, no `throw`, no `Any`)

### The Development Loop

```
Human: "Add subscription pause/resume feature"
  ↓
LLM: /speckit.specify  →  spec.md (structured specification)
LLM: /speckit.clarify   →  Q&A to resolve ambiguities
LLM: /speckit.cycle     →  plan.md → tasks.md → implementation
                         (resume-aware: continues from the last incomplete stage)
```

Each step produces a reviewable artifact. The human can intervene at any point, or let the LLM run the full pipeline autonomously.

For explicit step-by-step control, the manual sequence remains available:

`/speckit.plan` → `/speckit.tasks` → `/speckit.implement`

## Why This Architecture

The combination of **Clean Architecture + CQRS + FP (Arrow-kt)** is specifically chosen to make LLM-driven development **reliable**:

| Design Choice | Why It Helps LLMs |
|---|---|
| **Strict layer boundaries** | LLM can work on one layer at a time without understanding the whole system |
| **CQRS separation** | Command and query paths are independent — parallelizable task decomposition |
| **`Either<Error, T>` everywhere** | No hidden control flow; error paths are explicit and type-checked |
| **Immutable domain entities** | No mutation bugs; `.copy()` makes state transitions mechanical |
| **1 task = 1 use case** | Each task is self-contained with clear inputs, outputs, and test boundaries |
| **Custom detekt rules** | Architecture violations are caught at compile time, not code review |
| **Value objects with validation** | Domain invariants are enforced by construction, reducing integration bugs |

## Project Structure

```
clean-architecture-sdd/
├── .claude/                     # LLM Development Environment
│   ├── CLAUDE.md                #   Architecture rules & coding conventions
│   ├── agents/                  #   implement-usecase (TDD agent)
│   ├── commands/                #   speckit.* workflow commands
│   ├── skills/                  #   ca-kotlin, fp-kotlin, arrow-kt, jooq-ddl, tdd-kotlin
│   └── rules/                   #   arrow-either-style, test-conventions
│
├── .specify/                    # Spec-Driven Development
│   ├── memory/                  #   constitution.md (project principles), feature-catalog.md
│   ├── specs/                   #   spec0/, spec1/, spec2/, spec3/, spec4/, spec5/ (feature specs)
│   ├── features/                #   Implemented feature docs (post-impl reference)
│   ├── templates/               #   SDD document templates
│   └── scripts/                 #   Automation (create-new-feature, etc.)
│
├── domain/                      # Pure Kotlin + Arrow (no Spring, no framework)
├── application/                 # Use cases: command/ + query/ (CQRS, no Spring)
├── infrastructure/              # jOOQ + JDBC repositories, port adapters
├── presentation/                # REST controllers, request/response DTOs
├── framework/                   # Spring Boot entry point, DI wiring
└── detekt-rules/                # Custom architecture enforcement rules
```

### `.claude/` — LLM Knowledge Base

| Component | Purpose |
|---|---|
| `CLAUDE.md` | Master instruction set — architecture rules, naming conventions, forbidden patterns |
| `skills/ca-kotlin` | Clean Architecture layer rules and patterns with code examples |
| `skills/fp-kotlin` | Functional programming patterns (Either, sealed interfaces, value objects) |
| `skills/arrow-kt` | Arrow-kt DSL patterns (`either {}`, `bind()`, `mapLeft()`) |
| `skills/jooq-ddl` | jOOQ DDL codegen and repository implementation patterns |
| `skills/tdd-kotlin` | TDD workflow with Kotest + MockK + Arrow assertions |
| `agents/implement-usecase` | Autonomous agent that implements a single use case via TDD |
| `commands/speckit.*` | 9 workflow commands covering the full SDD pipeline |
| `rules/` | Inline rules for Either style and test conventions |

### `.specify/` — Specification Pipeline

| Component | Purpose |
|---|---|
| `memory/constitution.md` | Project principles — immutability, no-throw, CQRS, etc. |
| `memory/feature-catalog.md` | Index of implemented features for cross-referencing |
| `specs/spec{N}/` | Phased feature specifications with test cases |
| `templates/` | Templates for plans, tasks, and agent context |
| `scripts/` | Shell scripts for spec pipeline automation |

## Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin 2.3 / JDK 25 |
| Framework | Spring Boot 4.0 + Virtual Threads |
| Error Handling | Arrow-kt `Either<Error, Result>` |
| Database | PostgreSQL (H2 for testing) |
| ORM | jOOQ (DDL-based codegen from Flyway) |
| Testing | Kotest + MockK + kotest-assertions-arrow |
| Linting | ktlint + detekt (custom rules) |
| Coverage | Kover (80% minimum) |

## Architecture

```
framework → presentation → application → domain ← infrastructure
```

### CQRS Pattern

```
Command (write): Controller → Command UseCase → Domain Entity → Domain Repository
Query  (read):   Controller → Query UseCase  → Query Repository → DTO (bypasses domain)
```

- **Domain repositories** (`domain/repository/`): Aggregate persistence and reads (`save`, `delete`, `findById`, `findByXxx`)
- **Query repositories** (`application/query/repository/`): Read-only, returns flat DTOs
- **Ports** (`application/port/`, `application/query/port/`, `application/command/port/`): Non-DB external dependencies only (Clock, transactions, payment gateway, external APIs, brokers)

### Key Constraints

- **No `throw`** in domain, application, infrastructure — everything is `Either`
- **`either {}` DSL** with `.bind()` for composition (imperative `return .left()` banned)
- **Immutable** domain entities (`data class` + `val` only)
- **Virtual Threads** — blocking JDBC, no `suspend`, no reactive
- **No Spring annotations** in domain/application — DI wired in `framework/config/`

## Getting Started

### Prerequisites

- JDK 25+
- PostgreSQL (or Docker)

### Configuration

```bash
export DB_URL=jdbc:postgresql://localhost:5432/subscription_db
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
```

### Build & Run

```bash
./gradlew build           # Build all modules
./gradlew check           # Run all checks (lint + detekt + test + coverage)
./gradlew test            # Run tests only
./gradlew ktlintCheck     # Lint
./gradlew detekt          # Static analysis (custom architecture rules)
./gradlew koverVerify     # Coverage (80% minimum)
./gradlew :framework:bootRun  # Run application
```

### Developing a New Feature with Claude Code

```bash
# 1. Describe what you want
claude "/speckit.specify"
# → Generates .specify/specs/specN/spec.md

# 2. Clarify ambiguities
claude "/speckit.clarify"
# → Interactive Q&A, updates spec.md

# 3. Run one implementation cycle
claude "/speckit.cycle"
# → Generates or reuses plan.md and tasks.md, then continues implementation
# → If work is incomplete, run /speckit.cycle again to resume from the last stop point
```

Manual control is still available when needed:

```bash
claude "/speckit.plan"
claude "/speckit.tasks"
claude "/speckit.implement"
```

## Custom Detekt Rules

| Rule | Enforcement |
|---|---|
| `ForbiddenLayerImport` | Blocks cross-layer imports (whitelist-based) |
| `NoThrowOutsidePresentation` | `throw` only allowed in presentation layer |
| `NoExplicitAny` | Bans explicit `Any`/`Any?` types |

## License

MIT
