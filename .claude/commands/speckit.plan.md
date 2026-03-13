---
description: Execute the implementation planning workflow using the plan template to generate design artifacts.
handoffs: 
  - label: Create Tasks
    agent: speckit.tasks
    prompt: Break the plan into tasks
    send: true
  - label: Create Checklist
    agent: speckit.checklist
    prompt: Create a checklist for the following domain...
---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

## Outline

1. **Setup**: Run `.specify/scripts/bash/setup-plan.sh --json` from repo root and parse JSON for FEATURE_SPEC, IMPL_PLAN, SPECS_DIR, BRANCH. For single quotes in args like "I'm Groot", use escape syntax: e.g 'I'\''m Groot' (or double-quote if possible: "I'm Groot").

2. **Load context**: Read FEATURE_SPEC and `.specify/memory/constitution.md`. Load IMPL_PLAN template (already copied).

3. **Execute plan workflow**: Follow the structure in IMPL_PLAN template to:
   - Fill Technical Context (mark unknowns as "NEEDS CLARIFICATION")
   - Fill Constitution Check section from constitution
   - Evaluate gates (ERROR if violations unjustified)
   - Phase 0: Generate research.md (resolve all NEEDS CLARIFICATION)
   - Phase 1: Generate data-model.md, contracts/, quickstart.md
   - Phase 1: Update agent context by running the agent script
   - Re-evaluate Constitution Check post-design

4. **Stop and report**: Command ends after Phase 2 planning. Report branch, IMPL_PLAN path, and generated artifacts.

## Phases

### Phase 0: Outline & Research

1. **Extract unknowns from Technical Context** above:
   - For each NEEDS CLARIFICATION → research task
   - For each dependency → best practices task
   - For each integration → patterns task

2. **Generate and dispatch research agents**:

   ```text
   For each unknown in Technical Context:
     Task: "Research {unknown} for {feature context}"
   For each technology choice:
     Task: "Find best practices for {tech} in {domain}"
   ```

3. **Consolidate findings** in `research.md` using format:
   - Decision: [what was chosen]
   - Rationale: [why chosen]
   - Alternatives considered: [what else evaluated]

**Output**: research.md with all NEEDS CLARIFICATION resolved

### Phase 1: Design & Contracts

**Prerequisites:** `research.md` complete

1. **Extract entities from feature spec** → `data-model.md`:
   - Entity name, fields, relationships
   - Validation rules from requirements
   - State transitions if applicable

2. **Define interface contracts** (if project has external interfaces) → `/contracts/`:
   - Identify what interfaces the project exposes to users or other systems
   - Document the contract format appropriate for the project type
   - Examples: public APIs for libraries, command schemas for CLI tools, endpoints for web services, grammars for parsers, UI contracts for applications
   - Skip if project is purely internal (build scripts, one-off tools, etc.)

3. **Agent context update**:
   - Run `.specify/scripts/bash/update-agent-context.sh claude`
   - These scripts detect which AI agent is in use
   - Update the appropriate agent-specific context file
   - Add only new technology from current plan
   - Preserve manual additions between markers

**Output**: data-model.md, /contracts/*, quickstart.md, agent-specific file

### Phase 2: Scope Alignment Gate

**Prerequisites:** Phase 1 complete (plan.md fully filled, data-model.md, contracts/ generated)

**Purpose**: Verify that the implementation plan preserves the intended delivery slice from spec.md without accidental scope loss. Prevent the LLM from silently dropping acceptance scenarios or required work inside the declared scope.

1. **Extract delivery intent from spec.md**:
   - Parse all user stories and acceptance scenarios
   - Identify which scenarios are included in the current feature slice
   - If the spec or user input explicitly declares phased delivery, incremental rollout, or deferred layers, record that as an allowed scope boundary

2. **Extract declared scope from plan.md**:
   - Read **Summary**
   - Read **Scope**
   - Read **Out of scope**
   - Read **Project Structure**
   - Read **Test Strategy**
   - Read **Feature Doc Planning**

3. **Build scope alignment matrix**:

   For each acceptance scenario in the current feature slice, check:
   - Is the scenario explicitly included in scope?
   - Are all business behaviors in the scenario represented in domain/application design?
   - If the declared scope includes infrastructure/presentation/framework for this slice, are those layers reflected in the plan?
   - If those layers are intentionally deferred, is that deferral stated explicitly in **Out of scope**?

   ```text
   | Scenario | In current slice? | Business behavior planned? | Additional layers required in this slice? | Explicitly deferred? | Covered? |
   |----------|-------------------|----------------------------|-------------------------------------------|----------------------|----------|
   | US1-1    | ✓                 | ✓                          | No                                        | —                    | ✓ PASS   |
   | US2-7    | ✓                 | ✓                          | Infra adapter needed later                | ✓                    | ✓ PASS   |
   | US3-2    | ✓                 | ✗                          | No                                        | —                    | ✗ FAIL   |
   ```

4. **Gate evaluation**:
   - **PASS** if every scenario in the current slice is accounted for by either:
     - planned implementation in this slice, or
     - explicit deferral in `Out of scope`
   - **FAIL** if any scenario in the current slice is silently omitted or partially represented without explicit deferral

5. **Remediation on FAIL**:
   - List omitted scenarios or missing business behaviors
   - Update **Summary/Scope/Out of scope** so the slice is explicit
   - Update **Project Structure** and **Test Strategy** only for layers included in this slice
   - Re-run the matrix until all in-scope scenarios are accounted for

6. **Record evidence in plan.md**:
   - Fill the existing `## Scope Alignment` section in plan.md (immediately after Constitution Check)
   - Include the matrix
   - Include the gate result (PASS or PASS after remediation)

**Output**: Updated plan.md with explicit scope boundaries and `## Scope Alignment` section added

## Key rules

- Use absolute paths
- ERROR on gate failures or unresolved clarifications
- NEVER complete planning with silently omitted acceptance scenarios
- Explicit deferral is allowed, but every deferred scenario, layer, or integration must be named in `Out of scope`
- Do not infer full-stack scope unless the spec or user input requires this slice to deliver those layers
