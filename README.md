# CampusKart

A marketplace Android app for university students to buy and sell used academic items — books,
drafters, lab coats, calculators, cycles — with buyer and seller talking directly over WhatsApp
instead of an in-app chat system.

**Course:** 2CEIT5PE18 — Mobile Application Development (Semester V, CE/IT/CE-AI)
**Faculty:** Prof. Hiten M Sadani, Ganpat University
**Author:** Rutul

---

## The problem

Every semester, students end up with academic items they no longer need — a drafter used for one
subject, a lab coat from a lab they've finished, last year's textbooks. The existing resale
channels are WhatsApp groups, hostel notice boards and word of mouth: unorganised, impossible to
search, and they don't reach past a single batch or block.

CampusKart makes that resale searchable and organised, while keeping the actual conversation
where students already are — WhatsApp.

---

## Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material3) |
| Authentication | Firebase Authentication (email/password) |
| Database | Cloud Firestore |
| Image hosting | Cloudinary (free tier) |
| Image loading | Coil 3 |
| Photo capture | CameraX / system photo picker |
| AI (planned) | Google ML Kit — on-device Image Labeling |
| Min / target SDK | 26 (Android 8.0) / 37 |

---

## Setup

The app talks to two external services you need your own accounts for. Full click-by-click
instructions are in **[SETUP.md](SETUP.md)** — roughly 20 minutes, no credit card, no paid tiers.

The short version:

1. Create a Firebase project, enable **Email/Password** auth and **Cloud Firestore** (test mode).
2. Register an Android app with package name `com.example.campuskart`, download
   `google-services.json`, and put it at `app/google-services.json`.
3. Create a Cloudinary account and an **unsigned** upload preset.
4. Fill in `cloudinary.cloudName` and `cloudinary.uploadPreset` at the bottom of
   `local.properties`.
5. Sync Gradle and run. The temporary setup screen should show five green PASS rows.

### A note on secrets

`app/google-services.json` and `local.properties` are both **gitignored** — this repo never
carries the developer's own Firebase or Cloudinary identifiers. A placeholder
`app/google-services.json.template` is committed so that a fresh clone still compiles before you
have added your own.

---

## Features

### Shipped

- **Day 1** — Firebase Auth, Cloud Firestore, Cloudinary, Coil and CameraX wired into the build,
  with an on-device connectivity check confirming every service is reachable.

### Planned

| Layer | Scope |
|---|---|
| Layer 1 — MVP | Auth → Post Item → Feed → Detail → WhatsApp deep link |
| Layer 2 | Category filters, keyword search, mark-as-sold, My Listings |
| Layer 2.5 — AI | ML Kit on-device image labeling suggests a listing's category from its photo |
| Layer 3 — Polish | Image compression, empty states, loading indicators, dark mode |

Full requirements are specified in [`PRD.md`](PRD.md) and [`SRS.md`](SRS.md); the day-by-day
schedule is in [`BUILD_PLAN.md`](BUILD_PLAN.md).

---

## Core mechanism: the WhatsApp deep link

Rather than building a messaging backend, the "Chat on WhatsApp" button on Item Detail fires an
Android **implicit intent**:

```
https://wa.me/91<sellerNumber>?text=<url-encoded message referencing the listing>
```

This satisfies the assignment objective around *sharing Android data among others* while
sidestepping real-time messaging infrastructure entirely — and it lands buyers in the app they'd
have moved to anyway.

---

## Data model

Two Firestore collections:

**`users`** — `uid`, `name`, `branch`, `semester`, `whatsappNumber`

**`listings`** — `id`, `title`, `description`, `category`, `price`, `condition`, `photoUrl`,
`sellerUid`, `sellerName`, `status`, `createdAt`

See [`PRD.md`](PRD.md) Section 8 for field-level detail.

---

## Project structure

```
app/src/main/java/com/example/campuskart/
├── MainActivity.kt          # entry point (currently the Day 1 setup screen)
├── setup/SetupCheck.kt      # temporary: verifies Firebase + Cloudinary connectivity
└── ui/theme/                # Material3 theme
```

---

## Challenges faced

*Kept as a running log through the build, per the assignment's documentation criterion.*

**Firebase Storage is no longer free.** The original plan was to store listing photos in Firebase
Cloud Storage, keeping everything inside one service. Firebase now requires the paid Blaze plan
to enable Cloud Storage on newly created projects, so photo hosting moved to Cloudinary's free
tier. That split the backend across two providers and meant writing the image upload by hand
against Cloudinary's REST API instead of using a single Firebase SDK call.

**Unsigned uploads are a deliberate compromise.** Cloudinary's *signed* uploads need an API
secret, and anything shipped inside an APK can be extracted from it — so signing would require a
server the project doesn't have and can't pay for. CampusKart therefore uses an *unsigned* upload
preset, which means someone who decompiled the app could upload images to the account. For a
free, serverless class project that was judged the right trade; a production app would put a
small signing endpoint in front of it.

**A dependency ahead of the toolchain.** Coil 3.5.0+ is compiled with Kotlin 2.4, whose metadata
the Kotlin 2.2 compiler bundled with AGP 9.3.1 cannot read — the build failed with a wall of
`Class 'kotlin.Unit' was compiled with an incompatible version of Kotlin` errors that pointed at
this project's own source files rather than at the real culprit. Reading the dependency tree
(`gradlew :app:dependencyInsight`) traced it to Coil, and pinning Coil to 3.4.0 resolved it.
