# Phase 4F-2: Process Death & Activity Recreation Resilience

## State Preservation Guarantee
- Room and DataStore maintain authoritative state.
- In-memory ViewModels are purely reactive projections.
- Process death at any moment (during challenge, review, or spend) leaves database in an uncorrupted, consistent state.
