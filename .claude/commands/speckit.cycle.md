---
description: Run one implementation cycle by orchestrating plan, tasks, and implement, resuming from existing artifacts when prior work is incomplete.
---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

## Goal

Execute one end-to-end development cycle for the active feature by chaining:

1. `/speckit.plan`
2. `/speckit.tasks`
3. `/speckit.implement`

This command is **resume-aware**. If prior work is incomplete, do not restart from scratch. Reuse the active feature branch and existing artifacts, then continue from the furthest valid stage.

## Resume Rules

Determine the current state of the active feature before doing any generation:

1. Run `.specify/scripts/bash/check-prerequisites.sh --json --paths-only` from repo root and parse:
   - `FEATURE_DIR`
   - `FEATURE_SPEC`
   - `IMPL_PLAN`
   - `TASKS`
   - `BRANCH`

2. Inspect the active feature directory and decide the entry point:
   - If `FEATURE_SPEC` does not exist: STOP and instruct the user to run `/speckit.specify` first.
   - If `plan.md` does not exist: start from `/speckit.plan`
   - If `plan.md` exists but `tasks.md` does not exist: start from `/speckit.tasks`
   - If `tasks.md` exists:
     - Scan checklist files under `FEATURE_DIR/checklists/` if present
     - Scan `tasks.md` for incomplete tasks (`- [ ]`)
     - If any checklist item is incomplete, log the incomplete items and proceed with implementation anyway
     - If any task is incomplete, continue with `/speckit.implement`
     - If all tasks are complete, report cycle complete and do not regenerate plan/tasks unless the user explicitly asked to refresh them

3. Never overwrite finished artifacts just to start a new cycle. Only refresh an artifact if:
   - it is missing, or
   - the user explicitly asked to regenerate it, or
   - a prior step changed upstream inputs and the downstream artifact is now stale

## Execution Flow

### Stage 1: Planning

If planning is required:

1. Run `/speckit.plan`
2. Confirm `plan.md` was created or updated successfully
3. If planning stops because of unresolved clarification or constitution gate failure, STOP and report the blocker

### Stage 2: Task Generation

If task generation is required:

1. Run `/speckit.tasks`
2. Confirm `tasks.md` was created or updated successfully
3. If task generation fails or produces invalid/incomplete output, STOP and report the blocker

### Stage 3: Implementation

If implementation is allowed:

1. Run `/speckit.implement`
2. If implementation halts because checklists are incomplete, STOP and report that the cycle should be resumed with `/speckit.cycle` after checklist completion
3. If implementation halts because some tasks remain incomplete, STOP and report that the cycle should be resumed with `/speckit.cycle`
4. If all tasks complete successfully, report cycle complete

## Incomplete Work Policy

This command is intentionally conservative:

- Incomplete checklist items are logged but do not block implementation
- Incomplete implementation tasks are **not** a failure; they define the next resume point
- The next invocation of `/speckit.cycle` MUST continue from existing `plan.md`, `tasks.md`, checklist state, and task completion state
- Do not create a new feature branch or a new spec directory when resuming the same active feature

## Reporting

At the end, report:

- active branch
- feature directory
- detected resume stage
- artifacts reused vs generated in this run
- whether the cycle completed or paused
- exact next command, which should normally be `/speckit.cycle`

## Notes

- This command assumes `/speckit.specify` has already created the active feature
- Prefer continuation over regeneration
- Treat `tasks.md` as the source of truth for execution progress
- Treat checklist completion as the gate for entering implementation
