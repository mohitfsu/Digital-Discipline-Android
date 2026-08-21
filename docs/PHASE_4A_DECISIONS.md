# Phase 4A — Architectural Decisions & Trade-Offs

## ADR 1: Unified Domain Model for Parent and Self Modes
- **Context**: The product is evolving from a pure parental control utility to a dual-mode behavioral change platform (Parent Mode and Self Mode).
- **Decision**: Use a single, unified domain model (`GoalEntity`, `TriggerEntity`, `ReplacementBehaviourEntity`, `BehaviourPolicyEntity`) and common enforcement engine (`PolicyEngine`, `AccessibilityService`, `OverlayManager`) rather than creating a separate fork or engine.
- **Rationale**: Keeps the codebase compact, minimizes maintenance overhead for a small team, and ensures that all hardening, battery optimizations, and reliability fixes automatically benefit both modes.

---

## ADR 2: Absolute Precedence of Parent Mode Policies
- **Context**: When a device is configured under Parent Mode, or when an explicit parent rule is set for an app, a child should not be able to bypass it by creating a Self Mode policy.
- **Decision**: In `BehaviourPolicyResolver`, Parent Mode rules are checked first. If a parent restriction exists and is enabled, it immediately takes precedence over any user-defined Self Mode behaviour policy.
- **Rationale**: Protects parental trust and guarantees child safety.

---

## ADR 3: Zero-Latency Local Policy Resolution
- **Context**: Real-time app interception occurs when the user launches a target app (e.g. Instagram).
- **Decision**: `BehaviourPolicyResolver` executes 100% locally from in-memory caches and indexed SQLite Room tables without any cloud, network, or AI calls in the execution path.
- **Rationale**: Real-time enforcement must remain responsive ($<50\text{ ms}$), work in airplane mode, and never incur per-app-launch cloud costs.

---

## ADR 4: Decoupled AI Interface for Phase 4B+
- **Context**: Future Gemini advisory features may suggest optimal replacement behaviors.
- **Decision**: Keep AI entirely out of the real-time blocking loop. Future AI modules will consume aggregated weekly summaries out-of-band and submit proposed `BehaviourPolicyDto` objects for user/parent approval.
- **Rationale**: Prevents cloud downtime from blocking app launches, ensures predictable behavior, and avoids cloud API expenses.
