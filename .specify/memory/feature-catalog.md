# Feature Catalog

Central index of all implemented features in the subscription system.
**Before modifying any code, consult this catalog to find relevant feature docs.**

## How to Use

1. **Before implementing**: Search this catalog for related features and read their docs
2. **After implementing**: Add new entries following the format below
3. **Before debugging/fixing**: Find the relevant feature doc for business rules — do NOT read source code first
4. **When docs are partial**: Check `.specify/memory/current-status.md` for gaps and temporary decisions

## Catalog Rules

- Every implemented entity or cross-cutting concern must have an index entry.
- Every implemented use case must include spec traceability, code paths, and change impact hints.
- Use search tags aggressively so agents can locate behavior by domain word, API term, or table name.

## Entities

<!-- Add entries as entities are implemented. Structure:

### [Entity Name]
- **Overview**: `.specify/features/[entity]/overview.md`
- **Status**: Implemented / Partial / Planned
- **Tags**: [domain terms, api terms, db terms]
- **Code Entry Points**:
  - `domain/src/main/kotlin/...`
  - `application/src/main/kotlin/...`
- **Persistence**:
  - Tables: `[table_a]`, `[table_b]`
- **Endpoints**:
  - `[METHOD] /path`
- **Use Cases**:

  | Use Case | Type | Doc | Status | Spec Source | Code Paths | Related Tables | Related Endpoints |
  |----------|------|-----|--------|-------------|------------|----------------|-------------------|
  | [UseCaseName] | Command/Query | `.specify/features/[entity]/[usecase].md` | Implemented/Partial/Planned | `specX/spec.md#...` | `domain/...`, `application/...` | `[table]` | `[METHOD] /path` |

-->

No entities implemented yet.

## Cross-Cutting Concerns

<!-- Add entries for domain services and shared logic. Structure:

### [Topic Name]
- **Doc**: `.specify/features/[topic]/[doc].md`
- **Affects**: [EntityA], [EntityB]
- **Business Rules**: [short summary]
- **Search Tags**: [keyword_a], [keyword_b]
- **Code Entry Points**:
  - `domain/...`
- **Related Tables**:
  - `[table_name]`

-->

No cross-cutting concerns implemented yet.

## Reverse Lookup Index

Populate these sections as features are implemented.

### By Endpoint

<!--
- `[METHOD] /path` → `[entity]/[usecase].md`
-->

None yet.

### By Table

<!--
- `[table_name]` → `[entity]/overview.md`, `[entity]/[usecase].md`
-->

None yet.

### By Domain Concept / Search Tag

<!--
- `[keyword]` → `[topic]/[doc].md`, `[entity]/[usecase].md`
-->

None yet.

## Spec to Feature Traceability

<!-- Track which spec phases have been implemented. Structure:

| Spec | Feature | Status | Notes |
|------|---------|--------|-------|
| `specX/spec.md#...` | `[entity]/[usecase].md` | Implemented/Partial/Planned | [short note] |

-->

No specs implemented yet.
