# Current Status

## Purpose

Tracks implementation status, known gaps, and temporary decisions so agents do not infer completion from specs alone.

## Snapshot

- Architecture skeleton exists for `domain`, `application`, `infrastructure`, `presentation`, `framework`, and `detekt-rules`.
- Core project rules are defined in `.specify/memory/constitution.md`.
- Specs exist for `spec0`, `spec1`, `spec2`, and `spec3`.
- Feature implementation docs under `.specify/features/` have not been populated yet.
- Feature catalog is currently a navigation structure and should be expanded as features are implemented.

## Known Gaps

- Most business use cases are specified but not implemented.
- Search-oriented feature docs are not yet available for existing and future code.
- Cross-cutting areas such as billing calculation, state machines, and error taxonomy need dedicated feature docs once implemented.

## Update Rules

Update this file when:

- A spec phase becomes partially or fully implemented
- A major architectural rule changes
- A temporary workaround or intentional deviation is introduced
- A feature doc or feature catalog is known to be incomplete or stale
