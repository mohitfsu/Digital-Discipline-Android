# PHASE 2C IMPLEMENTATION — PARENT WEB CONTROL CENTER

## 1. Overview
Phase 2C delivers the official web-based Parent Control Center for Digital Discipline built with Next.js 15, TypeScript, Tailwind CSS, Firebase Authentication, and Cloud Firestore. The web application interacts directly with Firestore through the client Web SDK without requiring an intermediate backend or custom Node API server.

---

## 2. Technology Stack & Architectural Decisions
- **Framework**: Next.js 15 (App Router, Standalone output)
- **Language**: TypeScript (Strict Mode)
- **Styling**: Tailwind CSS (Dark theme slate design tokens)
- **Authentication**: Firebase Authentication (Email/Password + Resilient Local Dev Fallback)
- **Database**: Cloud Firestore (`digital-discipline-2026`)
- **Icons**: Lucide React

---

## 3. Directory Structure
```
d:/Zidd/web/
├── package.json
├── tsconfig.json
├── next.config.ts
├── tailwind.config.ts
├── postcss.config.mjs
├── src/
│   ├── app/
│   │   ├── layout.tsx
│   │   ├── globals.css
│   │   ├── LayoutClient.tsx
│   │   ├── page.tsx               (1. Home Dashboard)
│   │   ├── login/page.tsx         (2. Login & Registration)
│   │   ├── child/[childId]/page.tsx (3. Child Profile View)
│   │   ├── apps/page.tsx          (4. App Rules & Policy Editor)
│   │   ├── schedules/page.tsx     (5. Recurring Schedules)
│   │   ├── interventions/page.tsx (6. Challenge Timer Settings)
│   │   ├── activity/page.tsx      (7. Aggregated Activity Trends)
│   │   └── settings/page.tsx      (8. Family & Device Pairing)
│   ├── components/
│   │   ├── Navbar.tsx
│   │   ├── Sidebar.tsx
│   │   ├── ChildSelector.tsx
│   │   ├── DeviceStatusBadge.tsx
│   │   ├── ActivityBarChart.tsx
│   │   └── PairingModal.tsx
│   ├── context/
│   │   ├── AuthContext.tsx
│   │   └── FamilyContext.tsx
│   ├── lib/
│   │   └── firebase.ts
│   └── types/
│       └── index.ts
```

---

## 4. End-to-End Synchronization Lifecycle

```
[Parent Web Control Center]
          │
          │ (1. Parent adjusts rule: Instagram -> BLOCK, pushes v2)
          ▼
   [Cloud Firestore]
   /families/{familyId}/children/{childId}/policy/current
          │
          │ (2. SyncManager / WorkManager executes 0ms or periodic pull)
          ▼
   [Android Sync Engine]
   PolicySyncWorker.kt -> Room DB (atomic transactional update)
          │
          │ (3. PolicyEngine evaluates Room AppRuleEntity in ~58ms)
          ▼
   [Overlay & Interventions]
   WindowManager overlay enforcers launch Strict Block / Mindful Pause
```
