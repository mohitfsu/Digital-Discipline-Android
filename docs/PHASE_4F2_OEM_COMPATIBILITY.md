# Phase 4F-2: OEM-Specific Battery & Service Restriction Analysis

## OEM Compatibility Profiles

### 1. Google Pixel (Stock Android)
- **Status**: **PHYSICALLY VERIFIED (`9645561501002LC`)**
- **Risks**: None. Clean Accessibility and notification behavior.

### 2. Samsung (OneUI)
- **Status**: **STATIC ANALYSIS VERIFIED**
- **Behavior**: Aggressive memory manager may flag background apps.
- **Guidance**: User can exclude Digital Discipline from "Sleeping Apps" in device settings.

### 3. Xiaomi / Redmi (HyperOS / MIUI)
- **Status**: **STATIC ANALYSIS VERIFIED**
- **Behavior**: Requires explicit "Autostart" and "Display pop-up windows" permission.
- **Guidance**: Step-by-step permission guidance provided in onboarding.

### 4. OnePlus / Oppo / Realme (OxygenOS / ColorOS)
- **Status**: **STATIC ANALYSIS VERIFIED**
- **Behavior**: Aggressive battery saver. Setting battery to "Don't Optimize" ensures uninterrupted protection.
