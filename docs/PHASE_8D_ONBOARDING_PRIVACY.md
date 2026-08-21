# Phase 8D — Onboarding Privacy Guarantees

## On-Device By Design

The Phase 8D onboarding collects no data that leaves the device.

## What Is Stored

All onboarding selections are stored in Android DataStore (private to the app process):

| Data | Storage | Purpose |
|------|---------|---------|
| Behaviour pattern selection | DataStore only | Not transmitted |
| Screen time estimate | DataStore only | Not transmitted |
| App selections | DataStore only (as package names) | Used by PolicyEngine locally |
| Intervention categories | DataStore only | Used by InterventionSelector locally |
| Display name (optional) | DataStore only | Shown in TodayScreen header only |

## What Is NOT Collected

- No keystrokes or screen content
- No camera images or video
- No location data
- No contacts
- No clipboard content
- No device identifiers transmitted
- No analytics SDK
- No crash reporting with PII
- No screenshot or screen recording
- No URL or browsing history

## Accessibility Permission Scope

The Accessibility Service (`DigitalDisciplineAccessibilityService`) monitors:
- **Package name only** when an app window comes to the foreground

It does NOT read:
- Screen content (text, images)
- Input fields or passwords
- Clipboard
- Notifications content

This is stated explicitly on Screen 9 permission card:
> "We do not read your messages, keystrokes, screen contents, or personal information."

## Camera Permission (Interventions)

Some interventions (push-ups, squats, wall sit) use the camera for pose detection.
Camera frames are processed **in-memory only** using ML Kit on-device. No frames are stored,
transmitted, or logged. Camera is only active when the user is completing a camera-validated
intervention.

## No Cloud Dependencies Added

Phase 8D introduces zero new network calls, cloud endpoints, Firebase remote config reads,
or analytics tracking. All state is local.
