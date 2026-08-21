# Digital Discipline — Cloud Infrastructure Cost Model

**Architecture Model**: Local-First Enforcement with Daily Aggregation Sync  
**Benchmark Scales**: 100 | 1,000 | 10,000 | 100,000 | 1,000,000 Active Devices  
**Primary Objective**: Minimum practical recurring infrastructure cost for a 1–3 person team  

---

## 1. Architectural Cost Drivers: Local-First vs Naive Cloud Architecture

### Naive Cloud Architecture (Anti-Pattern)
- Device sends every app launch, intervention start, and unlock to Cloud Functions / Firestore.
- 1 Device = ~250 cloud writes/day + ~500 reads/day.
- **100,000 Users** = **25,000,000 writes/day** $\rightarrow$ **\$4,500+/month** in Firestore costs alone.
- High latency, severe battery drain, breaks completely offline.

### Digital Discipline Local-First Architecture (Approved)
- Real-time blocking, monotonic timers, and event logging execute **100% locally on-device**.
- Cloud writes are strictly limited to **1 daily summary rollup per active device** + infrequent policy changes (<2/week).
- **100,000 Users** = **100,000 writes/day** $\rightarrow$ **~\$54/month** total cloud bill.

---

## 2. Unit Economics & Consumption Assumptions (Per Active Device)

| Metric | Frequency / Volume | Monthly Volume (Per Device) |
| :--- | :--- | :--- |
| **Firestore Writes** | 1 daily summary + 0.3 policy syncs | ~39 writes / month |
| **Firestore Reads** | 1 parent dashboard load + 1 device heartbeat | ~60 reads / month |
| **Firestore Storage** | ~2 KB per device doc + 30-day summaries | ~60 KB / device |
| **Cloud Functions Invocations** | Triggered only on daily summary aggregation | ~30 invocations / month |
| **Firebase Auth** | Token refresh (MAU) | 1 active parent user / 2 devices |
| **Cloud Bandwidth** | JSON summary payloads (~1.5 KB per sync) | ~50 KB / month |

---

## 3. Cost Projections Across User Scales (Monthly USD)

*Pricing based on Google Cloud / Firebase published tier rates (Firestore: \$0.18/100k writes, \$0.06/100k reads, \$0.18/GB storage, Auth: free tier up to 50k MAU).*

| Infrastructure Component | 100 Users | 1,000 Users | 10,000 Users | 100,000 Users | 1,000,000 Users |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **Firebase Authentication** | \$0.00 (Free) | \$0.00 (Free) | \$0.00 (Free) | \$27.50 | \$275.00 |
| **Firestore Writes** | \$0.00 (Free) | \$0.00 (Free) | \$2.10 | \$21.06 | \$210.60 |
| **Firestore Reads** | \$0.00 (Free) | \$0.00 (Free) | \$0.36 | \$3.60 | \$36.00 |
| **Firestore Storage** | \$0.00 (Free) | \$0.00 (Free) | \$0.11 | \$1.08 | \$10.80 |
| **Cloud Functions (Compute)** | \$0.00 (Free) | \$0.00 (Free) | \$0.40 | \$4.00 | \$40.00 |
| **Vercel / Web Dashboard Hosting**| \$0.00 (Free) | \$0.00 (Free) | \$20.00 (Pro) | \$20.00 (Pro) | \$150.00 |
| **Cloud Bandwidth & Egress** | \$0.00 (Free) | \$0.00 (Free) | \$0.05 | \$0.50 | \$5.00 |
| **Total Estimated Cloud Bill** | **\$0.00** | **\$0.00** | **\$23.02** | **\$77.74** | **\$727.40** |
| **Cost Per Active Device / Month**| **\$0.00** | **\$0.00** | **\$0.0023** | **\$0.00078** | **\$0.00073** |

---

## 4. Financial & Operational Takeaways

1. **Sub-\$1,000 / Month at 1 Million Devices**:
   - Because all CPU-intensive window observation, touch interception, and event calculation happen on the child's phone hardware, the cloud operates purely as a lightweight metadata registry.
2. **Infinite Free Tier Margin at Launch**:
   - At 100 to 1,000 active devices, Digital Discipline fits **entirely within Firebase's free tier limits** (50,000 Auth MAU, 20,000 daily Firestore writes, 50,000 daily reads).
3. **High Margin Business Model**:
   - With a \$4.99/month or \$39.99/year family subscription, infrastructure costs represent **< 0.05% of gross revenue**, allowing the company to remain profitable with a 1–3 human operating team.
