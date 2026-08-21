# Digital Discipline — Phase 2B Cloud Cost Validation Report

**Classification**: Cloud Infrastructure Cost & Capacity Model  
**Target Milestone**: Phase 2B Minimal Cloud Control Plane  
**Operating Tier Target**: $0/month on Firebase Spark Free Tier  

---

## 1. Firebase Free Tier (Spark Plan) Monthly Allowances

- **Document Reads**: 50,000 / day (1,500,000 / month)
- **Document Writes**: 20,000 / day (600,000 / month)
- **Document Deletes**: 20,000 / day (600,000 / month)
- **Stored Data**: 1 GiB total
- **Network Egress**: 10 GiB / month

---

## 2. Digital Discipline Daily Cloud Usage Per Child Device

| Action | Frequency | Reads / Day | Writes / Day | Bandwidth / Day |
| :--- | :--- | :---: | :---: | :---: |
| **Policy Sync (WorkManager)** | 12 times / day (Every 2h) | 12 | 0 | ~15 KB |
| **Device Heartbeat Ping** | 2 times / day | 0 | 2 | ~2 KB |
| **Daily Analytics Rollup** | 1 time / day (Nightly) | 0 | 1 | ~1 KB |
| **Parent Policy Change** | ~2 times / week | 0 | ~0.3 | ~1 KB |
| **TOTAL PER DEVICE / DAY** | — | **12 reads** | **3.3 writes** | **~19 KB** |

---

## 3. Scale Capacity on $0/month Spark Free Tier

$$\text{Max Supported Active Devices} = \min\left(\frac{50,000 \text{ reads/day}}{12 \text{ reads/device/day}}, \frac{20,000 \text{ writes/day}}{3.3 \text{ writes/device/day}}\right) = \min(4,166, 6,060) \approx \mathbf{4,100 \text{ Active Devices}}$$

> [!NOTE]
> Under this architecture, **up to 4,000+ active child devices can run concurrently at $0.00/month recurring cloud cost**.
> When scaling past 4,000 devices, Firestore pay-as-you-go costs are approximately **$0.0006 per device per month** ($0.60 per 1,000 active devices).

---

## 4. Why This Architecture Achieves Zero-Cost Scalability

1. **No Real-Time Cloud Listeners on Child Device**: Periodic WorkManager polling avoids keeping hundreds of thousands of concurrent WebSocket connections open.
2. **Aggregated Single-Write Daily Summaries**: App launches (~300/day) and intervention triggers (~50/day) are processed 100% locally in SQLite/Room, eliminating 350 writes/device/day.
3. **Delta Sync Checks**: Devices only write updates when policy versions change.
