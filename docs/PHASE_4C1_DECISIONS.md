# Phase 4C-1 — Architecture Decisions & Invariants

## Decision 1: Pure Ephemeral Draft Before Persistence
- **Context**: Users exploring different goals should not pollute the SQLite database with half-configured or abandoned triggers and policies.
- **Decision**: `BehaviourPlanCreator.createDraftPlan` returns a pure, immutable in-memory `BehaviourPlanDraft`. Only when the user explicitly clicks `[ START THIS PLAN ]` in `SelfPlanReviewScreen` does `BehaviourPlanCreator.confirmAndPersistPlan` execute an atomic database commit.

## Decision 2: 100% Deterministic Rule Presets Over LLMs / Cloud
- **Context**: Screen time friction must be instantaneous ($<10\text{ms}$ resolution), offline-first, private, and deterministic.
- **Decision**: Pre-defined domain tables in `GoalTemplateRepository` categorize apps and map goals to appropriate friction levels without any cloud or AI overhead.

## Decision 3: Absolute Parent Precedence Unbroken
- **Context**: Parent mode controls child safety and must never be compromised by Self Mode configurations.
- **Decision**: `BehaviourPolicyResolver` evaluates Parent Mode rules first. Any Parent `BLOCK` or `DELAY` strictly overrides Self Mode templates.
