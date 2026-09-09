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
- **Day 2** — Mid-fidelity Compose mockups of all six screens, a fixed green-teal Material3
  theme, and an on-device gallery for reviewing the sketches before any real screen is built.
- **Day 3** — The real navigation graph: a persistent four-tab bottom bar, Item Detail pushed
  over the Feed, and finished Login and Signup screens with branch/semester dropdowns and a
  WhatsApp number field that normalises whatever the user pastes. No Firebase calls yet.
- **Day 4** — Working authentication. Signup creates a Firebase Auth account and writes the
  matching `users` document, login signs an existing user back in, the session survives the app
  being closed, and logging out clears it. Per-field validation, error messages written for a
  student rather than a developer, and 14 JVM unit tests over the form rules.

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

## Screen design

All six screens were sketched in Compose before any of them was wired to Firebase, so the layout
could be reviewed and changed while changing it was still cheap. The sketches live in
`ui/mockups/` and are replaced screen by screen from Day 3; the package is deleted before
submission.

| # | Screen | What it holds |
|---|---|---|
| 1 | Login / Signup | Email + password; signup also collects name, branch, semester and WhatsApp number |
| 2 | Home Feed | Search field, category chips, listing cards (photo, title, price, seller branch) |
| 3 | Item Detail | Photo, description, price, condition, seller, and the "Chat on WhatsApp" button |
| 4 | Post Item | Photo first, then title, description, category, price, condition |
| 5 | My Listings | The user's own items with Edit / Mark sold / Delete on each card |
| 6 | Profile | Name, branch, semester, WhatsApp number, and logout |

Three decisions came out of this pass:

**Bottom navigation, four tabs.** SRS 3.1 left the choice open between a bottom bar and a
top-level graph. CampusKart uses a persistent bottom bar — Feed, Post, My items, Profile — so
every top-level screen is one tap from every other. Item Detail is not a tab: it is pushed over
the feed and hides the bar, because it exists to lead to a single action.

**The WhatsApp button owns the bottom of Item Detail.** It is pinned below the scrolling content
in WhatsApp's own green, rather than sitting inline where it would scroll away. It is the one
thing the whole screen is for.

**A fixed palette instead of Material You.** Dynamic colour repaints the app from the phone's
wallpaper, which would make the development device, the demo device and the README screenshots
three different colours. The app ships a fixed green-teal scheme with a hand-written dark
variant instead.

---

## Navigation

One flat navigation graph holds all seven destinations — Login, Signup, the four tabs, and Item
Detail. Nesting it into separate "auth" and "main" sub-graphs would buy separate back stacks the
app has no use for, and would obscure the one thing that does matter: Login and the Feed sharing
a stack, so that signing in really removes the login form and signing out really clears the
session.

Routes are plain strings rather than Navigation Compose's type-safe `@Serializable` routes. With
seven destinations and exactly one argument between them (`listingId`, on Item Detail), the
type-safe form would add the kotlinx-serialization plugin to the build for no practical gain.

The back stack behaves the way an Android user expects:

| Action | Result |
|---|---|
| Log in / create account | Feed, with the auth screens dropped — back exits the app |
| Tap a tab | Pops back to the Feed rather than stacking tabs; each tab keeps its own state |
| Open a listing | Pushed over the Feed, bottom bar hidden |
| Log out | Firebase session ended, whole stack cleared, back to Login |
| Reopen the app while signed in | Opens on the Feed; Login is never shown |

Until Days 5–8 build them, the four tab destinations render their Day 2 sketches under a banner
that names the day the real screen arrives, so the shell can be walked through as a whole app in
the meantime. The Day 1 service check and the Day 2 mockup gallery are still reachable from a
small **Dev tools** link at the bottom of the Login screen; both go away on Day 10.

---

## Authentication

Firebase Authentication with an email and password, and no email verification step — signup is
open to anyone (PRD Section 6), so there is no college domain to check against.

**Signup writes two things and needs both.** Creating the Auth account and writing the `users`
profile document are separate calls to separate services, and the account is useless without the
profile: `whatsappNumber` lives there, and a seller with no number cannot be contacted at all,
which is the one thing the app exists to do. So the two are treated as a single operation — if
the Firestore write fails, the just-created Auth account is deleted and the session cleared. See
`data/AuthRepository.kt`.

**The session persists.** Firebase stores a signed-in session on disk, so reopening the app goes
straight to the Feed rather than asking for the password again. `Log out` on the Profile screen
is the way back to Login.

**Validation happens twice, on purpose.** The form rules in `ui/auth/AuthValidation.kt` run on
the device so a mistake is reported in the field that is wrong, immediately, instead of after a
network round trip returns one generic error. They are not a security boundary — Firebase
re-checks the email and password itself, and the Firestore rules added on Day 8 do the same for
data. The rules are plain Kotlin with no Android or Firebase imports, which is what lets them be
covered by ordinary JVM unit tests:

```
gradlew testDebugUnitTest
```

| Field | Rule |
|---|---|
| Name / branch / semester | Required (FR-AUTH-002) |
| WhatsApp number | Required (FR-AUTH-003), and ten digits starting 6–9 |
| Email | Must look like an address; any domain accepted |
| Password | At least 6 characters — Firebase's own minimum, checked here to save a round trip |

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

`branch` and `semester` are self-declared but chosen from closed lists (CE / IT / CE-AI / Other,
and 1–8). The feed shows a seller's branch on every card, so free text would render "CE", "ce"
and "Computer Engg" as three different branches. `whatsappNumber` is stored as ten digits with no
country code or separators, whatever the user typed.

**`listings`** — `id`, `title`, `description`, `category`, `price`, `condition`, `photoUrl`,
`sellerUid`, `sellerName`, `status`, `createdAt`

See [`PRD.md`](PRD.md) Section 8 for field-level detail.

---

## Project structure

```
app/src/main/java/com/example/campuskart/
├── MainActivity.kt          # entry point — hosts the navigation graph
├── data/                   # Firebase access — the only place the SDKs are touched
│   ├── AuthRepository.kt    # signup, login, logout, and the users document
│   └── UserProfile.kt       # the users collection schema (PRD Section 8)
├── model/
│   └── CampusOptions.kt     # the fixed branch and semester lists
├── setup/                   # temporary: verifies Firebase + Cloudinary connectivity
│   ├── SetupCheck.kt
│   └── SetupStatusScreen.kt
└── ui/
    ├── auth/                # Login and Signup screens, their ViewModels, and form rules
    ├── navigation/          # routes, the bottom bar, and the NavHost
    ├── screens/             # temporary: sketch-backed placeholders for Days 5–8
    ├── mockups/             # temporary: Day 2 sketches of all six screens
    └── theme/               # Material3 theme — fixed green-teal palette
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

**Nesting Scaffolds double-counts everything.** Hosting the Day 2 sketches inside the Day 3
navigation shell put a `Scaffold` inside a `Scaffold`, and both of them wanted to draw the same
things: two bottom navigation bars stacked on top of each other, and the status bar inset applied
twice, which pushed every sketch screen a bar's height down the display. The bars were solved by
giving each sketch a `showTabBar` switch the shell turns off. The insets needed
`Modifier.consumeWindowInsets(innerPadding)` on the `NavHost` — padding a composable by the outer
`Scaffold`'s inner padding positions it correctly but does not *tell* its children the inset has
already been spent, so the nested `TopAppBar` adds it again.

**`popUpTo` silently does nothing when it misses.** The bottom bar's tab-switching pops back to a
fixed destination so tabs never stack up, and every guide writes that as
`popUpTo(graph.findStartDestination().id)`. Here the graph's start destination is Login, which
signing in has deliberately just removed from the back stack — so the pop matched nothing, did
not error, and every tab tap quietly pushed another entry. Popping to the Feed explicitly, which
is the real root of the tabbed area, fixed it.

**Signup is two writes that have to behave like one.** `createUserWithEmailAndPassword` both
creates the account and signs the new user in, so a Firestore failure on the very next line
leaves the app holding a live session for an account with no name and no WhatsApp number — and
retrying signup then fails with "email already in use" against an account the user can never
usefully log into. There is no transaction spanning Auth and Firestore to reach for, so the fix
is a manual rollback: delete the Auth user and sign out, putting the email address back in play.
The awkward case is cancellation — if the user leaves the screen mid-request the coroutine is
cancelled between the two writes, so `CancellationException` has to be caught, rolled back, and
then rethrown rather than swallowed.

**A dependency ahead of the toolchain.** Coil 3.5.0+ is compiled with Kotlin 2.4, whose metadata
the Kotlin 2.2 compiler bundled with AGP 9.3.1 cannot read — the build failed with a wall of
`Class 'kotlin.Unit' was compiled with an incompatible version of Kotlin` errors that pointed at
this project's own source files rather than at the real culprit. Reading the dependency tree
(`gradlew :app:dependencyInsight`) traced it to Coil, and pinning Coil to 3.4.0 resolved it.
