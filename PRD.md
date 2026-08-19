# CampusKart — Product Requirements Document

**Course:** 2CEIT5PE18 — Mobile Application Development (Semester V, CE/IT/CE-AI)
**Faculty:** Prof. Hiten M Sadani, Ganpat University
**Author:** Rutul
**Presentation deadline:** On or before 21/09/2026
**Submission deadline:** 21/09/2026, 8:00 AM (Google Form)

---

## 1. Overview

**One-line pitch:** A marketplace app for university students to buy and sell used academic items — books, drafters, lab coats, calculators, cycles — with direct buyer–seller contact over WhatsApp instead of an in-app chat system.

**Problem:** Every semester, students accumulate used academic items (books, drafters, lab coats, calculators) that have no easy resale channel. Existing options — WhatsApp groups, notice boards, word of mouth — are unorganized, hard to search, and don't scale past a single batch or hostel block.

**Solution:** A lightweight, mobile-first marketplace scoped to students, with category browsing, search, photo listings, and a one-tap WhatsApp deep link to start a conversation — no chat backend required.

---

## 2. Goals

- Ship a working, demoable Android app before the internal deadline
- Solve a real, relatable problem — not a toy CRUD exercise
- Keep the tech stack free end-to-end (no paid APIs, no billing surprises)
- Reuse platform capabilities (implicit intents, on-device ML) instead of building everything from scratch
- Seed the app with real listings before final submission so the demo shows genuine use, not fake data

## 3. Non-Goals (out of scope)

- In-app chat/messaging system — WhatsApp deep link replaces this entirely
- Payments or escrow — this is a discovery/contact tool, not a transaction processor
- Push notifications — not planned for MVP or later layers
- Web or iOS version — Android only, via Android Studio
- Verified/paid seller badges, ratings, or reviews system
- Full ML-based fraud/spam detection (a simplified duplicate-check may appear as a stretch goal only)

---

## 4. Target Users

- University students wanting to sell used academic items before graduating, changing branch, or finishing a semester
- University students looking to buy the same items secondhand instead of new
- No admin/moderator role planned for MVP — any signed-in user can post

## 5. User Characteristics

- Users are college students with basic to intermediate smartphone literacy — no accessibility-specialized design required beyond standard Material3 defaults
- Users have WhatsApp installed (safe assumption for the target demographic)
- Users may or may not want to use their official college email — signup is open to any email address

---

## 6. Key Decisions Log

| Area | Decision | Rationale |
|---|---|---|
| Auth scope | Any email allowed, no college-domain restriction | Some students hesitate to use their official college email |
| WhatsApp number | Mandatory field at signup | Required for the core contact mechanism to work for every listing |
| Image storage | Cloudinary (free tier) | Firebase Storage now requires the paid Blaze plan for new projects |
| Listing permissions | Any signed-in user can create/list | No moderation layer planned for MVP |
| AI/ML feature | Deferred — MVP ships without it first | Keep core flow (auth → post → feed → contact) stable before adding ML Kit categorization |
| Seed data | Added in Week 8, real items from classmates | Demonstrates genuine usage instead of dummy data during presentation |

---

## 7. Tech Stack

- **Language / UI:** Kotlin + Jetpack Compose (Material3) — matches course syllabus
- **Auth:** Firebase Authentication (email/password, no domain restriction)
- **Database:** Cloud Firestore
- **Image hosting:** Cloudinary (free tier)
- **Image loading:** Coil
- **Photo capture:** CameraX / system photo picker
- **AI (planned addon):** Google ML Kit — on-device Image Labeling, for auto-category suggestion
- **IDE:** Android Studio
- **Version control:** GitHub, daily commits

---

## 8. Data Model

### `users` collection
| Field | Type | Notes |
|---|---|---|
| uid | string | Firebase Auth UID |
| name | string | |
| branch | string | Self-declared |
| semester | string | Self-declared |
| whatsappNumber | string | Mandatory at signup |

### `listings` collection
| Field | Type | Notes |
|---|---|---|
| id | string | Document ID |
| title | string | |
| description | string | |
| category | string | Books / Drafter / Lab Coat / Calculator / Cycle / Electronics / Other |
| price | number | |
| condition | string | New / Good / Used |
| photoUrl | string | Cloudinary URL |
| sellerUid | string | References `users.uid` |
| sellerName | string | Denormalized for feed display |
| status | string | available / sold |
| createdAt | timestamp | |

---

## 9. Screens

1. **Login / Signup** — email + password, profile form (name, branch, semester, WhatsApp number)
2. **Home Feed** — listing cards (photo, title, price, seller branch), category chips, search bar
3. **Item Detail** — photos, description, price, condition, seller info, "Chat on WhatsApp" button
4. **Post Item** — photo capture/upload → title, category, price, condition, description → publish
5. **My Listings** — user's own items with Edit / Mark as Sold / Delete
6. **Profile** — basic info + logout

---

## 10. Feature Layers

| Layer | Scope | Status |
|---|---|---|
| **Layer 1 — MVP** | Auth → Post Item → Feed → Detail → WhatsApp deep link | Core, must-ship |
| **Layer 2** | Category filters, search, mark-as-sold, my listings | Core, must-ship |
| **Layer 2.5 — AI addon** | ML Kit on-device image labeling on Post Item screen; auto-suggests category, user can override | Planned after Layer 1–2 are stable |
| **Layer 3 — Polish** | Image compression, empty states, shimmer loading, dark mode | If time permits |
| **Layer 4 — Stretch** | "Request an item" board, price-drop badge, report-listing button, ML Kit duplicate-listing detection | Optional, only if ahead of schedule |

---

## 11. Core Mechanism: WhatsApp Deep Link

Instead of building an in-app chat system, the "Chat" button on Item Detail fires an implicit intent:

```
https://wa.me/91<sellerNumber>?text=<url-encoded prefilled message>
```

This satisfies the assignment's "sharing Android data among others" objective (implicit intents) while avoiding real-time messaging infrastructure entirely.

---

## 12. Success Criteria (for this assignment)

- Functional, bug-free MVP demoable live in class
- Original, uncopied concept — verified unique among classmates' submissions
- Clean UI/UX using Material3 conventions
- Daily/regular GitHub commit history showing real progress
- README.md documenting features, objectives, and challenges faced
- Confident live presentation before 21/09/2026

---

## 13. Open Items / Future Considerations

- Lightweight trust signal for open (non-college-restricted) signup — e.g. self-declared branch/semester shown on listings — may be revisited post-MVP
- ML Kit category-to-label mapping table not yet finalized — needed before Layer 2.5 work starts
- Firestore security rules (who can edit/delete which documents) to be written alongside Layer 1

---

*This document lives at the project root and should be updated as decisions change during development.*
