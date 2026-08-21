# Phase 4F-5: Complete Self Mode E2E Journey Verification

## Complete 20-Step End-to-End User Flow
1. **Mode Selection**: User chooses Self Mode.
2. **Goal Selection**: User picks "Study / Focus" goal.
3. **Plan Creation & Review**: AdaptivePlanEngine generates 7-day habit plan.
4. **Plan Activation**: Goal saved to Room as active primary goal.
5. **Permission Request**: Clear explanation screen directs to Accessibility Settings.
6. **Permission Grant**: Service connects and loads policies synchronously.
7. **Protection ON**: Honest banner displayed on TodayScreen.
8. **Distraction Trigger**: Launching target distraction app intercepts cleanly.
9. **Intentional Pause**: Mindful countdown initiates positive friction.
10. **Challenge Completion**: Physical/mental challenge marked completed.
11. **Earned Time**: 15 minutes awarded to Earned Time Wallet.
12. **USE MY TIME**: User starts active unlock session.
13. **Monotonic Session**: `SystemClock.elapsedRealtime()` protects unlock duration.
14. **Session Expiry**: Overlay returns immediately upon session expiry fail-closed.
15. **SAVE FOR LATER**: Excess earned minutes preserved in wallet ledger.
16. **TodayScreen & First Win**: First Win banner surfaces and records milestone.
17. **Habit Momentum**: Daily Actions tracked across 7-day momentum cycle.
18. **Plan Continuity & Weekly Review**: Weekly reflection allows user-guided plan refinement.
19. **Goal Completion**: Goal completed and moved to immutable Goal History archive.
20. **MY JOURNEY**: BehaviourJourneyEngine synthesizes chronological timeline in $< 0.5\text{ms}$.
