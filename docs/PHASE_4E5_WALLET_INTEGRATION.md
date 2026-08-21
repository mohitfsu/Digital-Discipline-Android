# Phase 4E-5: Earned Time Wallet Authority Integration

## Global Wallet Invariants
- The earned time wallet is global across the user's Self Mode journey.
- Goal lifecycle transitions (Pause, Resume, Complete, Replace, Start Fresh) **NEVER**:
  1. Alter available wallet seconds
  2. Clear transaction ledgers
  3. Create artificial earn/spend transactions
  4. Override Parent Mode restrictions
