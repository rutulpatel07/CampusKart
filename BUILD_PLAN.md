# CampusKart — 10-Day Build Plan for Claude Code

**Context for Claude Code:** This is a solo Android project (Kotlin + Jetpack Compose) for a university assignment. Full project context lives in `PRD.md` and `SRS.md` at the project root — read both before starting Day 1. The developer (Rutul) is new to Android and reviews/runs the app for 1–2 hours per day, so each day's output must be small enough to review in that window and must end in a real, working commit — not a half-broken state.

**Ground rules for every day:**
- Read `PRD.md` and `SRS.md` first if not already in context.
- End every day with a **working build** (app compiles and runs, even if a feature is incomplete) and a **git commit** with a clear, specific message — never a vague "progress" commit.
- Do not skip ahead to a later day's scope even if it seems fast — small reviewable chunks matter more than speed here.
- If a decision isn't covered in PRD.md/SRS.md, stop and ask Rutul rather than assuming.
- Update `README.md` incrementally as features land (per PRD Section 12 — README documentation is 10% of the grade).

---

## Day 1 — Environment Setup + Firebase/Cloudinary Guidance

**Goal:** Get all external services connected and the project able to talk to them, with zero UI yet.

1. Guide Rutul step-by-step (he's new to this) through:
   - Creating a Firebase project on the Firebase Console
   - Enabling **Authentication** (Email/Password provider)
   - Creating a **Cloud Firestore** database (start in test mode, tighten rules later per SRS 3.5)
   - Downloading `google-services.json` and placing it in the `app/` folder
   - Creating a free **Cloudinary** account and retrieving the cloud name + unsigned upload preset
2. Add Gradle dependencies: Firebase Auth, Firebase Firestore, Coil, CameraX, Cloudinary Android SDK (or plain HTTP upload if the SDK is heavy — Claude Code should pick the simpler reliable option).
3. Add the Google Services Gradle plugin and sync.
4. Confirm the app still builds and runs (even though it still just shows the default screen).
5. Create `.gitignore` entries for `google-services.json` if Rutul doesn't want it public, or note in README that it's required and gitignored (Claude Code should ask Rutul which he prefers).
6. Initialize the GitHub repo (name: `CampusKart`), push first commit.

**Commit message example:** `chore: project setup, Firebase/Cloudinary integration, base dependencies`

---

## Day 2 — UI Mockups / Screen Planning (No code yet)

**Goal:** Plan all 6 screens visually before writing Compose code, per Rutul's preference for design-first.

1. Since there's no separate design tool in this workflow, Claude Code should create **low-fidelity mockups as Compose Preview sketches** — simple placeholder Composables with boxes, labels, and rough layout for all 6 screens (Login/Signup, Home Feed, Item Detail, Post Item, My Listings, Profile), viewable via `@Preview` without needing real data or navigation wired up.
2. Include rough spacing, where buttons/fields go, and how the WhatsApp button looks on Item Detail.
3. Get Rutul's sign-off in this review window before Day 3 builds real screens — Claude Code should explicitly note in its output "review these 6 mockups before I proceed to real implementation tomorrow."
4. Commit these as a `mockups` or `ui-preview` package that will be replaced/refined starting Day 3.

**Commit message example:** `docs(ui): add low-fidelity screen mockups for all 6 screens`

---

## Day 3 — Navigation Shell + Auth Screens (UI only, no Firebase logic yet)

**Goal:** Real navigation graph + Login/Signup screens built in Compose, matching Day 2 mockups, but not yet connected to Firebase.

1. Set up Jetpack Compose Navigation with routes for all 6 screens (placeholder content where not yet built).
2. Build the real Login screen UI (email, password fields, login button, "create account" link).
3. Build the real Signup screen UI (name, branch, semester, WhatsApp number, email, password fields) per the data model in PRD.md Section 8.
4. No Firebase calls yet — buttons can just navigate forward for now.

**Commit message example:** `feat(ui): navigation graph + login/signup screens`

---

## Day 4 — Firebase Auth Wiring

**Goal:** Make Login/Signup actually work end-to-end.

1. Wire Signup screen to Firebase Auth (create user with email/password).
2. On successful signup, write the user's profile (name, branch, semester, whatsappNumber) to the `users` Firestore collection per PRD Section 8 schema.
3. Wire Login screen to Firebase Auth sign-in.
4. Add basic validation (per SRS FR-AUTH-003: reject signup if WhatsApp number is empty).
5. Add logout functionality (can be a temporary button anywhere for now — full Profile screen comes later).
6. Test full signup → login → logout loop actually works before committing.

**Commit message example:** `feat(auth): Firebase email/password signup and login wired to Firestore`

---

## Day 5 — Post Item Screen (Core Flow Part 1)

**Goal:** A user can create a listing with a photo.

1. Build the real Post Item screen UI (title, description, category dropdown/chips, price, condition, photo picker).
2. Wire photo capture/selection using CameraX or system photo picker.
3. Upload the selected photo to Cloudinary, get back the URL.
4. On publish, write the listing document to Firestore per the `listings` schema in PRD.md Section 8 (including `sellerUid`, `sellerName`, `status: available`, `createdAt`).
5. Show a success state (e.g. navigate back to a temporary "posted!" confirmation).

**Commit message example:** `feat(listings): Post Item screen with Cloudinary upload and Firestore write`

---

## Day 6 — Home Feed + Item Detail (Core Flow Part 2)

**Goal:** Listings are visible and viewable — the app now feels like a real marketplace.

1. Build Home Feed screen: fetch all listings with `status: available` from Firestore, display as cards (photo, title, price, seller branch) in a LazyColumn.
2. Build Item Detail screen: show full listing info (photos, description, price, condition, seller name).
3. Implement the WhatsApp deep link button (per SRS FR-CONTACT-001/002) — tapping it opens WhatsApp with a prefilled message referencing the listing title.
4. Confirm the full user journey now works: sign up → post an item → see it in the feed → open detail → tap WhatsApp button → WhatsApp opens correctly.

**Commit message example:** `feat(feed): home feed, item detail screen, WhatsApp deep link contact`

---

## Day 7 — My Listings + Category Filters + Search

**Goal:** Layer 2 features — users can manage their own listings and narrow the feed.

1. Build My Listings screen: query Firestore for listings where `sellerUid` matches the current user, show as a list.
2. Add Edit, Mark as Sold, and Delete actions on the user's own listings (per SRS FR-LIST-007/008/009).
3. Add category filter chips on the Home Feed (per FR-LIST-004).
4. Add a search bar on Home Feed that filters listings by title match client-side (per FR-LIST-005 — note Firestore doesn't support full-text search natively, so this is a client-side filter on already-fetched data, as flagged in PRD.md README notes).

**Commit message example:** `feat(listings): my listings management, category filters, search`

---

## Day 8 — Profile Screen + Firestore Security Rules + Polish Pass

**Goal:** Wrap up remaining MVP/Layer 2 gaps and harden the backend.

1. Build the real Profile screen (show name, branch, semester, WhatsApp number; edit option if time allows; logout button moves here properly).
2. Write and deploy actual Firestore security rules (per SRS 3.5): only the listing's `sellerUid` can edit/delete/mark-sold that listing; only authenticated users can create listings; anyone authenticated can read listings.
3. Polish pass: empty states (e.g. "No listings yet" on My Listings when empty), basic loading indicators on Feed/Detail, and confirm image compression before Cloudinary upload isn't producing huge uploads.
4. Fix any bugs surfaced from Days 3–7 usage.

**Commit message example:** `feat(profile): profile screen, Firestore security rules, empty states and polish`

---

## Day 9 — ML Kit Category Suggestion (Layer 2.5 — AI addon)

**Goal:** Add the on-device AI feature to the Post Item flow.

1. Add ML Kit Image Labeling dependency.
2. On photo selection in Post Item, run the image through ML Kit's on-device labeler.
3. Build (and finalize, since this was flagged as an open item) a mapping table from ML Kit's generic labels to the app's fixed categories (Books, Drafter, Lab Coat, Calculator, Cycle, Electronics, Other) per SRS FR-AI-002.
4. Pre-select the suggested category in the Post Item form; allow full override (FR-AI-003).
5. Default to "Other" when no confident label match exists (FR-AI-004) — do not block posting.
6. Update README with this feature and honestly note ML Kit's label limitations as a "challenges faced" point (this is graded material per PRD Section 12).

**Commit message example:** `feat(ai): ML Kit on-device category suggestion on Post Item screen`

---

## Day 10 — Final Polish, README Completion, Presentation Prep

**Goal:** Everything is stable, documented, and demo-ready.

1. Full end-to-end regression pass: signup, login, post item (with AI suggestion), browse/filter/search feed, view detail, WhatsApp contact, edit/mark-sold/delete own listing, logout.
2. Finalize `README.md`: features list, objectives, tech stack, screenshots of each screen, and an honest "challenges faced" section (Cloudinary migration reason, Firestore search limitation, ML Kit accuracy limits, open-signup trust tradeoff — all already noted in PRD.md).
3. Clean up any leftover placeholder/mockup code from Day 2 that wasn't replaced.
4. Prepare a short demo script (this can mirror the "bring a real item, post it live" approach already noted in earlier planning).
5. Final commit and tag (e.g. `v1.0-submission`).

**Commit message example:** `docs: finalize README, demo prep, final polish for submission`

---

## Notes on Seed Data (from PRD.md)

Real seed listings (15–20 items from classmates) are planned for closer to submission, **after** this 10-day core build — not part of these 10 days. Revisit once Day 10 is stable and there's runway before the actual 21/09/2026 deadline.

## What to hand Claude Code

Give Claude Code this file plus `PRD.md` and `SRS.md` together at the start of each session, and tell it explicitly which Day you're on. Review each day's output within your 1–2 hour window before greenlighting the next day — don't let it batch multiple days into one commit, since that breaks both the review cadence and the "daily progress" grading criterion.
