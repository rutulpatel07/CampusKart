# Software Requirements Specification — CampusKart

**Course:** 2CEIT5PE18 — Mobile Application Development (Semester V, CE/IT/CE-AI)
**Faculty:** Prof. Hiten M Sadani, Ganpat University
**Author:** Rutul
**Version:** 1.0
**Standard followed:** IEEE 830

---

## 1. Introduction

### 1.1 Purpose
This document specifies the software requirements for CampusKart, an Android application that allows university students to buy and sell used academic items within their campus community. It is intended for the developer (self), and for faculty evaluation as part of the Mobile Application Development assignment. It covers functional and non-functional requirements for the MVP and planned feature layers.

### 1.2 Scope
CampusKart is a mobile marketplace app built for Android using Kotlin and Jetpack Compose. It enables students to list used academic items (books, drafters, lab coats, calculators, cycles, electronics) for sale, browse and search listings by category, and contact sellers directly via a WhatsApp deep link — without the app implementing its own messaging system. The goal is to reduce waste and improve affordability by making campus-level resale easy and organized, while keeping the entire tech stack free of cost.

### 1.3 Definitions, Acronyms, and Abbreviations
| Term | Definition |
|---|---|
| MVP | Minimum Viable Product — the smallest fully working version of the app |
| ML Kit | Google's on-device machine learning SDK (used for optional image labeling) |
| Deep link | A link that opens directly into another app (here, WhatsApp) with prefilled content |
| Firestore | Google Cloud Firestore, a NoSQL document database used for app data |
| UID | Unique identifier assigned to each authenticated user by Firebase Auth |

### 1.4 References
- Assignment brief: *2CEIT5PE18: Mobile Application Development — Assignment 1*, Prof. Hiten M Sadani, Ganpat University, AY 2026–2027
- CampusKart PRD.md (project root)
- Firebase Authentication & Firestore documentation
- Google ML Kit documentation (Image Labeling)

### 1.5 Overview
Section 2 describes the product at a high level — its context, functions, users, and constraints. Section 3 lists specific functional and non-functional requirements with unique IDs. Section 4 contains supporting appendices.

---

## 2. Overall Description

### 2.1 Product Perspective
CampusKart is a standalone, self-contained mobile application. It is not part of a larger institutional system and has no dependency on university infrastructure. It communicates with three external services: Firebase Authentication (login), Cloud Firestore (data storage), and Cloudinary (image hosting). It also invokes WhatsApp via an Android implicit intent for buyer–seller contact.

- **System Interfaces:** Firebase Auth, Cloud Firestore, Cloudinary REST API, WhatsApp (via `wa.me` intent)
- **User Interfaces:** Native Android UI built with Jetpack Compose (Material3)
- **Hardware Interfaces:** Device camera (for listing photos), local storage (temporary image cache)
- **Software Interfaces:** Android SDK, Firebase SDK, Coil (image loading), CameraX
- **Communications Interfaces:** HTTPS for all network calls (Firebase, Cloudinary)
- **Memory Constraints:** None beyond standard Android app limits; images compressed before upload to minimize footprint
- **Operations:** Single mode of operation — no offline mode planned for MVP
- **Site Adaptation Requirements:** None; app is generic and requires no per-institution configuration

### 2.2 Product Functions (Summary)
- User registration and login (email-based, no domain restriction)
- Creating, editing, deleting, and marking listings as sold
- Browsing and searching listings by category and keyword
- Viewing listing details and contacting the seller via WhatsApp
- (Planned addon) Auto-suggesting a listing's category from its photo using on-device ML

### 2.3 User Characteristics
- Users are university students with ordinary smartphone literacy — no specialized training required
- Users are assumed to have WhatsApp installed, since it is the sole contact mechanism
- No distinct user roles or permission tiers exist in the MVP — every authenticated user can browse, list, and manage only their own listings

### 2.4 Constraints
- Must be built using Kotlin and Jetpack Compose, per course requirements
- Must use only free-tier services — no paid APIs or billing-dependent infrastructure
- Must be developed and demonstrated within the assignment timeline (submission by 21/09/2026)
- Firebase Cloud Storage is unavailable on the free tier for new projects, so Cloudinary is used instead

### 2.5 Assumptions and Dependencies
- Assumes continued free-tier availability of Firebase Auth, Firestore, and Cloudinary through the project timeline
- Assumes test/demo devices have Google Play Services and WhatsApp installed
- Assumes ML Kit's pretrained image labels can be reasonably mapped to the app's fixed category list (Books, Drafter, Lab Coat, Calculator, Cycle, Electronics, Other) — this mapping was finalized on Day 9 (see `CategorySuggestion.kt`)

---

## 3. Specific Requirements

### 3.1 External Interface Requirements

**User Interfaces:** Six screens — Login/Signup, Home Feed, Item Detail, Post Item, My Listings, Profile — built with Material3 components. Navigation is a persistent bottom navigation bar with four destinations (Feed, Post, My items, Profile), decided during the Day 2 design pass; Item Detail is pushed over the Feed rather than being a fifth tab.

**Hardware Interfaces:** Device camera or photo gallery for capturing/selecting listing images.

**Software Interfaces:** Firebase Authentication SDK, Cloud Firestore SDK, Cloudinary upload API, ML Kit Image Labeling SDK (planned addon).

**Communications Interfaces:** All external calls (auth, database reads/writes, image upload) occur over HTTPS.

### 3.2 Functional Requirements

#### Authentication (FR-AUTH)
| ID | Requirement | Priority |
|---|---|---|
| FR-AUTH-001 | The system shall allow a user to register using any valid email address and password. | M |
| FR-AUTH-002 | The system shall require name, branch, semester, and WhatsApp number during signup. | M |
| FR-AUTH-003 | The system shall reject signup if the WhatsApp number field is empty. | M |
| FR-AUTH-004 | The system shall allow a registered user to log in using their email and password. | M |
| FR-AUTH-005 | The system shall allow a logged-in user to log out from the Profile screen. | M |

#### Listings (FR-LIST)
| ID | Requirement | Priority |
|---|---|---|
| FR-LIST-001 | The system shall allow any authenticated user to create a new listing with title, description, category, price, condition, and at least one photo. | M |
| FR-LIST-002 | The system shall upload listing photos to Cloudinary and store the returned URL in Firestore. | M |
| FR-LIST-003 | The system shall display all available listings on the Home Feed, ordered by creation date (most recent first). | M |
| FR-LIST-004 | The system shall allow the user to filter the feed by category. | S |
| FR-LIST-005 | The system shall allow the user to search listings by keyword matching the title. | S |
| FR-LIST-006 | The system shall allow a user to view full details of a listing, including seller name and branch. | M |
| FR-LIST-007 | The system shall allow the listing's owner to edit its title, description, price, condition, or category. | S |
| FR-LIST-008 | The system shall allow the listing's owner to mark their listing as "sold." | M |
| FR-LIST-009 | The system shall allow the listing's owner to delete their own listing. | S |
| FR-LIST-010 | The system shall display a user's own listings on a dedicated "My Listings" screen. | M |

#### Contact (FR-CONTACT)
| ID | Requirement | Priority |
|---|---|---|
| FR-CONTACT-001 | The system shall provide a "Chat on WhatsApp" button on the Item Detail screen. | M |
| FR-CONTACT-002 | Tapping the WhatsApp button shall open WhatsApp via an implicit intent, prefilled with a message referencing the listing title. | M |
| FR-CONTACT-003 | The system shall not store or expose the seller's WhatsApp number in plain view on the listing card or feed — it shall only be used to construct the deep link on tap. | S |

#### AI Addon — Category Suggestion (FR-AI) — *Planned, Layer 2.5*
| ID | Requirement | Priority |
|---|---|---|
| FR-AI-001 | The system shall analyze a listing photo using on-device ML Kit Image Labeling when the user is creating a listing. | C |
| FR-AI-002 | The system shall map the top ML Kit label to one of the app's fixed categories and pre-select it in the category field. | C |
| FR-AI-003 | The system shall allow the user to override the suggested category at any time before publishing. | C |
| FR-AI-004 | If no confident label match is found, the system shall default the category field to "Other" without blocking listing creation. | C |

### 3.3 Performance Requirements
Kept intentionally light for this class project — no strict numeric targets. The feed and item detail screens should load without a noticeable stall on a typical mid-range Android device and a normal campus Wi-Fi/mobile data connection.

### 3.4 Design Constraints
- Must be built in Kotlin using Jetpack Compose and Material3, per the course syllabus
- Must run on Android only (no cross-platform requirement)
- Must avoid any paid or billing-gated service

### 3.5 Software System Attributes
Kept brief per project scope:
- **Security:** Firestore rules shall restrict edit/delete operations on a listing to its original creator (`sellerUid` match). Authentication is required to create or manage listings.
- **Reliability:** No formal uptime target — dependent on Firebase/Cloudinary free-tier availability.
- **Usability:** Standard Material3 conventions are followed so the app feels familiar to any Android user without instructions.

### 3.6 Other Requirements
- **Database:** Cloud Firestore, two collections (`users`, `listings`) as detailed in PRD.md
- **Internationalization:** Not required for this project; English only

---

## 4. Appendices

### Appendix A — Glossary
See Section 1.3 (Definitions, Acronyms, and Abbreviations).

### Appendix B — Analysis Models
A simple data flow: User → Post Item screen → Cloudinary (image) + Firestore (listing metadata) → Home Feed (read) → Item Detail → WhatsApp intent (external app handoff). No formal UML diagrams are included at this stage; can be added later if required for presentation.

### Appendix C — Requirements Traceability (informal)
All functional requirements above trace back to the app's core goal stated in PRD.md Section 2 (Goals) — a working, demoable, free, student-relevant marketplace. AI requirements (FR-AI-xxx) trace to PRD.md Layer 2.5.

---

*This SRS should be updated if scope changes during development.*
