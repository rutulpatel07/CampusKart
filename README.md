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
| Photo capture | System photo picker + camera intent |
| AI | Google ML Kit — on-device Image Labeling (bundled model) |
| Min / target SDK | 26 (Android 8.0) / 37 |

---

## Setup

The app talks to two external services you need your own accounts for. Full click-by-click
instructions are in **[SETUP.md](SETUP.md)** — roughly 20 minutes, no credit card, no paid tiers.

The short version:

1. Create a Firebase project, enable **Email/Password** auth and **Cloud Firestore** (test mode
   is fine while setting up).
2. Register an Android app with package name `com.example.campuskart`, download
   `google-services.json`, and put it at `app/google-services.json`.
3. Create a Cloudinary account and an **unsigned** upload preset.
4. Fill in `cloudinary.cloudName` and `cloudinary.uploadPreset` at the bottom of
   `local.properties`.
5. Sync Gradle and run. You should see the Login screen.
6. **Deploy the Firestore rules and indexes** before sharing the app beyond your own test
   account — see below.

### Deploy Firestore rules and indexes

The committed [`firestore.rules`](firestore.rules) makes the marketplace private to signed-in
students, allows a listing to be created only under the current account's UID, and permits only
that seller to edit, mark sold, or delete it. It also permits authenticated buyers to read a
seller profile only so Item Detail can construct the private WhatsApp handoff.

Install and authenticate the Firebase CLI once, then deploy both committed files against your
Firebase project (replace `YOUR_PROJECT_ID` with its **Project ID**, not its display name):

```powershell
npm install -g firebase-tools
firebase login
firebase deploy --only firestore --project YOUR_PROJECT_ID
```

The deploy uses [`firebase.json`](firebase.json), which points to both the rules and index
definitions. It replaces Firestore's temporary test-mode rules, so do this only after Email /
Password signup has been enabled and you have tested with a real signed-in account.

### Firestore needs two composite indexes

Two screens ask Firestore for a filtered list in date order:

| Screen | Query |
|---|---|
| Home Feed | `status == available`, newest first |
| My Listings | `sellerUid == me`, newest first |

Each pairs an equality filter on one field with an ordering on *another*, and Firestore can only
serve that combination from a **composite index** — which it does not create by itself. Until an
index exists, that screen shows "Firestore needs a one-time index for this query" instead of
listings.

Two ways to create them, either is fine:

- **From Logcat (easiest).** Open the screen once, find the `FAILED_PRECONDITION` line from
  Firestore in Logcat, and tap the `https://console.firebase.google.com/...` link in it. The
  console opens with the index already filled in — press Create and wait a minute or two. Repeat
  for the other screen.
- **From the repo.** [`firestore.indexes.json`](firestore.indexes.json) holds both indexes
  written down. With the Firebase CLI: `firebase deploy --only firestore:indexes`, and neither
  screen ever shows the error.

The Home Feed's category filter and search need no index at all — they narrow a list already in
memory rather than re-querying, so Firestore never sees them.

### A note on secrets

`app/google-services.json` and `local.properties` are both **gitignored** — this repo never
carries the developer's own Firebase or Cloudinary identifiers. A placeholder
`app/google-services.json.template` is committed so that a fresh clone still compiles before you
have added your own.

---

## Features

### Shipped

- **Day 1** — Firebase Auth, Cloud Firestore, Cloudinary and Coil wired into the build,
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

- **Day 5** — Posting works end to end. The Post Item screen takes a photo from the camera or
  the system picker, compresses and rotates it, uploads it to Cloudinary, and writes the
  `listings` document to Firestore with the seller stamped on it. A success card loads the photo
  back from its Cloudinary URL, which confirms the whole path on the device. 11 more JVM unit
  tests over the listing form rules.

- **Day 6** — The marketplace is real. The Home Feed lists every available listing newest first,
  with the photo loaded back from Cloudinary; tapping a card opens Item Detail, which shows the
  full listing alongside the seller's live profile; and the "Chat on WhatsApp" button opens
  WhatsApp with a message already naming the item. The core flow now runs end to end: sign up →
  post an item → see it in the feed → open it → message the seller. 8 more JVM unit tests over
  the deep-link construction.

- **Day 7** — Layer 2 lands. My Listings shows everything the signed-in user has posted, with
  Edit, Mark sold and Delete on each card; marking an item sold takes it off the feed and can
  be undone; deleting asks first. Editing reuses the Post Item form and its rules on a screen
  of its own. The Home Feed gained a search box and a row of category chips, both filtering
  instantly over what is already loaded. 14 more JVM unit tests over the filter rules.

- **Day 8** — The real Profile tab now shows the signed-in student's name, email, branch,
  semester, and WhatsApp number, with logout in its permanent location. Firestore rules are
  versioned in the repository and ready to deploy, locking listing changes to their original
  seller. Feed, detail, and My Listings already include loading, empty, and retry states; photo
  uploads are capped at a 1600 px longest edge and JPEG quality 80 before Cloudinary upload.

- **Day 9** — On-device AI category suggestion. When a seller picks a photo on the Post Item
  screen, Google ML Kit's bundled Image Labeling model scans it locally (no network needed) and
  maps recognised labels to the app's fixed categories (Books, Calculator, Cycle, Lab Coat,
  Drafter, Electronics). The suggestion pre-selects the category chips; the seller can override
  it freely or ignore it. A 70% confidence threshold keeps weak guesses out, and unrecognised
  images default to Other. The mapper and its tests are pure Kotlin with no Android imports. 3
  more JVM unit tests over the label-to-category mapping.

- **Day 10** — Final polish. Removed the Day 1 service-check screen and Day 2 mockup gallery
  (both were scaffolding that stayed accessible via a Dev tools link on the Login screen). Full
  end-to-end regression pass, README finalised with demo script, and the codebase cleaned up for
  submission.

### Layer summary

| Layer | Scope | Status |
|---|---|---|
| Layer 1 — MVP | Auth → Post Item → Feed → Detail → WhatsApp deep link | Shipped (Days 4–6) |
| Layer 2 | Category filters, keyword search, mark-as-sold, My Listings | Shipped (Day 7) |
| Layer 2.5 — AI | ML Kit on-device image labeling suggests a listing's category from its photo | Shipped (Day 9) |
| Layer 3 — Polish | Image compression, empty states, loading indicators, Firestore security rules, profile | Shipped (Day 8) |

Full requirements are specified in [`PRD.md`](PRD.md) and [`SRS.md`](SRS.md); the day-by-day
schedule is in [`BUILD_PLAN.md`](BUILD_PLAN.md).

---

## Screen design

All six screens were sketched in Compose before any of them was wired to Firebase, so the layout
could be reviewed and changed while changing it was still cheap. The sketches were replaced
screen by screen from Day 3 onwards, and the mockup package was removed on Day 10.

| # | Screen | What it holds |
|---|---|---|
| 1 | Login / Signup | Email + password; signup also collects name, branch, semester and WhatsApp number |
| 2 | Home Feed | Search field, category chips, then listing cards (photo, title, price, seller and branch) |
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

### Screenshots

<div align="center">
  <div style="display: flex; overflow-x: auto; gap: 10px; padding: 10px; width: 100%; max-width: 900px; scroll-behavior: smooth; -webkit-overflow-scrolling: touch;">
    <img src="https://github.com/user-attachments/assets/ad4bc493-c6ec-46b6-acf0-8f457c474313" alt="Login" width="220" style="border-radius: 10px; flex-shrink: 0; box-shadow: 0 4px 8px rgba(0,0,0,0.2);">
    <img src="https://github.com/user-attachments/assets/7080b39b-ee4c-489d-8ed9-cf0fa3b8614f" alt="Create Account" width="220" style="border-radius: 10px; flex-shrink: 0; box-shadow: 0 4px 8px rgba(0,0,0,0.2);">
    <img src="https://github.com/user-attachments/assets/8900876d-51bd-4dd2-87d4-a28463d521ad" alt="Home Feed" width="220" style="border-radius: 10px; flex-shrink: 0; box-shadow: 0 4px 8px rgba(0,0,0,0.2);">
    <img src="https://github.com/user-attachments/assets/2e7437f8-7ffc-4dbe-a39c-f85f06406289" alt="Post Item – Empty" width="220" style="border-radius: 10px; flex-shrink: 0; box-shadow: 0 4px 8px rgba(0,0,0,0.2);">
    <img src="https://github.com/user-attachments/assets/42c1795e-4ed9-4c68-beb5-a243938a9ac9" alt="Post Item – Filled" width="220" style="border-radius: 10px; flex-shrink: 0; box-shadow: 0 4px 8px rgba(0,0,0,0.2);">
    <img src="https://github.com/user-attachments/assets/fd82440c-e90d-4a85-bde4-2e41623b7de9" alt="Listing Published" width="220" style="border-radius: 10px; flex-shrink: 0; box-shadow: 0 4px 8px rgba(0,0,0,0.2);">
    <img src="https://github.com/user-attachments/assets/0e11d3f5-49b6-4396-a957-fa85c7678fb9" alt="Updated Feed" width="220" style="border-radius: 10px; flex-shrink: 0; box-shadow: 0 4px 8px rgba(0,0,0,0.2);">
    <img src="https://github.com/user-attachments/assets/8edba680-39de-4391-9c84-8acd8f831bde" alt="My Listings" width="220" style="border-radius: 10px; flex-shrink: 0; box-shadow: 0 4px 8px rgba(0,0,0,0.2);">
    <img src="https://github.com/user-attachments/assets/9d4ec125-c259-4415-a669-c12d27709e65" alt="Profile" width="220" style="border-radius: 10px; flex-shrink: 0; box-shadow: 0 4px 8px rgba(0,0,0,0.2);">
  </div>
  <sub>Login · Create Account · Home Feed · Post Item (empty) · Post Item (filled) · Published · Updated Feed · My Listings · Profile</sub>
</div>

---

## Navigation

One flat navigation graph holds all eight destinations — Login, Signup, the four tabs, Item
Detail and Edit Listing. Nesting it into separate "auth" and "main" sub-graphs would buy
separate back stacks the app has no use for, and would obscure the one thing that does matter:
Login and the Feed sharing a stack, so that signing in really removes the login form and signing
out really clears the session.

Routes are plain strings rather than Navigation Compose's type-safe `@Serializable` routes. With
eight destinations and one argument between them — `listingId`, shared by Item Detail and Edit
Listing, which never appear together — the type-safe form would add the kotlinx-serialization
plugin to the build for no practical gain.

The back stack behaves the way an Android user expects:

| Action | Result |
|---|---|
| Log in / create account | Feed, with the auth screens dropped — back exits the app |
| Tap a tab | Pops back to the Feed rather than stacking tabs; each tab keeps its own state |
| Open a listing | Pushed over the Feed (or over My items), bottom bar hidden |
| Edit a listing | Pushed over My items; saving pops straight back, and the list reloads as it resumes |
| Log out | Firebase session ended, whole stack cleared, back to Login |
| Reopen the app while signed in | Opens on the Feed; Login is never shown |

Post Item was the first Day 2 sketch replaced by a real screen, on Day 5; the Feed and Item
Detail followed on Day 6, My items on Day 7, and Profile on Day 8. The Day 1 service check and
the Day 2 mockup gallery were removed on Day 10.

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

## Posting a listing

Tapping **Publish** runs three steps, and the button names whichever one it is on rather than
showing a single anonymous spinner — they take noticeably different amounts of time and fail for
entirely different reasons.

| Step | What happens | Where |
|---|---|---|
| Preparing the photo | Decode, rotate upright, scale to 1600 px, encode as JPEG at quality 80 | `data/ListingPhoto.kt` |
| Uploading the photo | Unsigned multipart POST to Cloudinary; returns a `secure_url` | `data/CloudinaryUploader.kt` |
| Saving the listing | Write the `listings` document, stamped with `sellerUid` and `status` | `data/ListingRepository.kt` |

**The photo is uploaded before the document is written**, so a failure leaves at worst an orphaned
image in Cloudinary — invisible to the app and costing nothing but free-tier storage. The other
order would put a listing on the feed pointing at a photo that does not exist, which every screen
rendering it would then have to handle.

**Neither way of getting a photo asks for a permission.** *Gallery* opens Android's system photo
picker, which runs in its own process and returns a URI for exactly the one image chosen — the app
never gets access to the gallery. *Camera* fires an implicit intent at whatever camera app is
installed; `CAMERA` permission is only demanded of an app that declares it in its manifest, and
this one deliberately does not, because it never touches the camera itself. It is the same
implicit-intent mechanism as the WhatsApp hand-off below. The camera app writes into this app's
cache through a `FileProvider` (`res/xml/file_paths.xml`), which is scoped to that one directory.

**Photos are compressed before upload** (SRS 2.5). A phone camera produces a 12-megapixel, 4–6 MB
file; the app shows it as a feed card and a phone-width image. Uploading the original would spend
a student's mobile data and the Cloudinary free tier on pixels no screen in the app ever displays.

**The success card loads the photo back from Cloudinary rather than from the local file.** If it
draws, the upload, the returned URL, the Firestore write and Coil's network loading have all
worked — the whole path confirmed on the device instead of in two web consoles.

---

## Browsing and viewing a listing

**The feed is one query, not one query plus N.** `status == available`, ordered by `createdAt`
descending (FR-LIST-003). Sold items are filtered out by Firestore rather than in Kotlin, so a
feed of twenty available items stays twenty document reads even once a semester of sold listings
has piled up behind them. Everything a card draws — photo, title, price, seller name, branch —
already lives on the listing document, which is what that costs (see *Data model* above).

**A one-shot read rather than a live listener.** A `snapshots()` listener would keep the feed
updating by itself, but it also holds a socket open for as long as the tab is on screen, and this
feed changes a few times a day rather than a few times a minute. Two things close the gap: a
Refresh action in the app bar, and a reload whenever the Feed tab is resumed — which is what makes
"post an item, then tap Feed" show the listing that was just published, since the bottom bar keeps
each tab's ViewModel alive across the trip.

**Item Detail reads the seller live.** The listing already carries the seller's name and branch,
but the detail screen re-reads their `users` document — for the WhatsApp number, which is
deliberately *not* denormalised, and while it is there for the branch and semester too, so a
seller who has since moved up a semester is not misreported. That read failing does not fail the
screen: the listing is already loaded, so the page draws normally and only the contact button is
disabled, with the reason under it.

**Three states the screen has to tell apart.** A listing the seller has deleted is a normal thing
to reach from a stale feed, so "this listing is gone" is its own state rather than an error — it
is a different, far less alarming message than "something went wrong", and it offers the way back
to the feed instead of a pointless retry.

**Your own listing shows no WhatsApp button.** Messaging yourself is not something the screen
should offer, so the green button is replaced by a "This is your listing" line rather than being
disabled — there is nothing wrong for the user to fix. The line points at My items, which is
where a listing is actually managed.

---

## Filtering and searching the feed

**Both filters run on the device, over listings already loaded.** For search that is not a
choice: Firestore has no full-text search of any kind, and the documented answer is to pay for a
third-party search service such as Algolia (FR-LIST-005, and *Challenges faced* below). For the
category chips it is a choice — filtering by category *could* be a real query, but making it one
would mean a network round trip, a loading spinner and a document read per tap, on a list small
enough to narrow instantly in memory. Doing both the same way also means tapping a chip while a
search is typed behaves the obvious way, rather than one filter re-querying underneath the other.

**Search matches every word, in any order, ignoring case.** A student looking for a drafter set
types "drafter set", "set drafter" or "drafter  set" more or less at random, and a plain
substring test would only match the first against a title of "Drafter set (mini)". Partial words
match too, so "calc" finds "Casio calculator" — on a list this size, finding too much is a much
smaller problem than finding nothing. The rules live in `ui/feed/FeedFilter.kt` with no Android
imports, so they are covered by fourteen ordinary JVM unit tests.

**"Nothing matched" is a different screen from "nothing posted".** They need opposite messages:
"nobody has posted anything, be the first" is wrong, and slightly insulting, when there are
thirty listings and the user simply mistyped a search term. The filtered-empty state offers
*Clear filters* instead, and a "3 of 12 listings" line sits under the chips whenever anything is
being narrowed — without it, a search that quietly hides half the feed looks identical to a feed
that only ever had half as much in it.

---

## Managing your own listings

**My Listings is `sellerUid == me`, and does not filter on status.** Unlike the feed it shows
sold items too, sorted below the available ones — a seller needs to see what they have already
sold, both to confirm it happened and to put one back up if the deal falls through. The
available-first ordering is done in Kotlin rather than in the query, because sorting by status
and then by date would need a third composite index for a list that is rarely more than a
handful of rows.

**Marking sold is one field write, and it is reversible.** `status` is the exact field the feed
query filters on, so flipping it to `sold` takes the listing off the feed and flipping it back
puts it on again (FR-LIST-008). Reversible on purpose: a deal that falls through should not cost
the seller a re-upload of the photo. Deleting is the one action that cannot be undone — the
Cloudinary image cannot be removed either, since that needs the API secret the app deliberately
does not ship — so it is the one action that asks first.

**Editing reuses Post Item's form, rules and error type rather than copying them.** They are
imported from `ui.post`, which is what guarantees a title too short to post is also too short to
edit down to: the two screens cannot drift apart, because there is only one set of rules. Save
stays disabled until something actually changes, so an Edit tapped by mistake costs a back press
rather than a pointless write. The photo is not editable — changing it would mean a second
Cloudinary upload and an orphan the app cannot delete, so a seller who photographed the wrong
thing deletes the listing and posts it again.

**An owner-only write is checked twice, and only one of those checks counts.** The repository
reads the listing back and compares `sellerUid` before writing. That is not security — anyone
running their own code skips it — and Day 8's Firestore rules are what actually enforce
ownership, on the server. The client check exists so the *honest* failure, a listing deleted
from another device, reads as "that listing no longer exists" instead of arriving as a bare
`PERMISSION_DENIED`.

**After a write the row is updated in place, not re-queried.** Firestore completes a write only
once the server has acknowledged it, so by the time the coroutine resumes the new state is known
for certain — re-reading would cost a document read per listing and blank the screen to redraw
rows that did not change. Refresh, and the reload whenever the screen comes back to the front,
cover what that cannot: a change made on another device, and a save on the Edit screen, which is
pushed over My items and so reloads it on the way back.

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

**The seller's number is never on screen.** It is read from the seller's `users` document when
Item Detail loads and only ever spent on building the link at the moment the button is tapped, so
it appears on no card, no feed row and no detail page (FR-CONTACT-003).

**Three attempts, in descending order of directness.** WhatsApp by package name first, so the
chat opens with no chooser; then WhatsApp Business, which a fair number of student sellers use;
then whatever handles `https`, which is a browser landing on WhatsApp's own "continue to chat"
page. That last fallback is what makes the button demonstrable on an emulator with no WhatsApp
installed — otherwise it would be a dead control until the app was on a real phone. If all three
fail, the screen says so rather than crashing.

**Android 11 package visibility.** `setPackage("com.whatsapp")` silently fails to resolve unless
the app declares what it is looking for, so `AndroidManifest.xml` carries a `<queries>` block
naming both WhatsApp packages. Without it the button would report WhatsApp as missing on every
modern phone — with WhatsApp sitting right there on the home screen.

---

## Data model

Two Firestore collections:

**`users`** — `uid`, `name`, `branch`, `semester`, `whatsappNumber`

`branch` and `semester` are self-declared but chosen from closed lists (CE / IT / CE-AI / Other,
and 1–8). The feed shows a seller's branch on every card, so free text would render "CE", "ce"
and "Computer Engg" as three different branches. `whatsappNumber` is stored as ten digits with no
country code or separators, whatever the user typed.

**`listings`** — `id`, `title`, `description`, `category`, `price`, `condition`, `photoUrl`,
`sellerUid`, `sellerName`, `sellerBranch`, `status`, `createdAt`

`category` and `condition` come from closed lists for the same reason branch does — the feed
filters by category, and a filter cannot work against values a hundred students typed by hand. That list is also exactly what ML Kit's labels have to be mapped onto on Day 9.
`sellerName` and `sellerBranch` are denormalised from the seller's `users` document so the feed
can draw a card without a second read per listing — a twenty-item feed would otherwise be
twenty-one queries. `sellerBranch` is the one field here that PRD Section 8 does not list: Section
9 specifies the feed card as *photo, title, price, seller branch*, and branch lives on the `users`
document, so it is copied across at publish time. Listings written before Day 6 carry an empty
branch and the card simply omits it. `createdAt` is written by Firestore's own `@ServerTimestamp`
rather than by the phone, because the feed is ordered by it and a device with a wrong clock would
otherwise pin its listings to the top of everyone's feed — or bury them.

See [`PRD.md`](PRD.md) Section 8 for field-level detail.

---

## Project structure

```
app/src/main/java/com/example/campuskart/
├── MainActivity.kt          # entry point — hosts the navigation graph
├── data/                    # backend access — the only place the SDKs are touched
│   ├── AuthRepository.kt    # signup, login, logout, and the users document
│   ├── UserProfile.kt       # the users collection schema (PRD Section 8)
│   ├── Listing.kt           # the listings collection schema (PRD Section 8)
│   ├── ListingRepository.kt # writing and reading listings
│   ├── ListingPhoto.kt      # compression, EXIF rotation, camera output files
│   ├── WhatsAppContact.kt   # the wa.me link and the handoff to WhatsApp
│   ├── CloudinaryUploader.kt # the unsigned multipart upload
│   └── MlKitCategorySuggester.kt # on-device image labeling for category hints
├── model/
│   └── CampusOptions.kt     # the fixed branch, semester, category and condition lists
└── ui/
    ├── auth/                # Login and Signup screens, their ViewModels, and form rules
    ├── post/                # Post Item screen, its ViewModel, and the listing form rules
    ├── feed/                # Home Feed screen, its ViewModel, and the search/category filter
    ├── detail/              # Item Detail screen, its ViewModel, and the WhatsApp button
    ├── mylistings/          # My Listings and Edit Listing, with their ViewModels
    ├── components/          # small composables shared across screens
    ├── navigation/          # routes, the bottom bar, and the NavHost
    ├── profile/             # profile screen and logout
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

**Firestore will not order a filtered query without being asked first.** The feed query is
"available listings, newest first" — an equality filter on `status` plus an ordering on
`createdAt` — which looks like the most ordinary query in the app and is the one that failed. A
single-field index is created automatically; combining a filter on one field with an ordering on
*another* needs a composite index, and Firestore will not invent one. It fails the query with
`FAILED_PRECONDITION` and puts a ready-made console link in Logcat — which is helpful to a
developer watching Logcat and invisible to everyone else, so the app translates that one error
code into on-screen instructions rather than a generic "something went wrong". The index is also
committed as [`firestore.indexes.json`](firestore.indexes.json) so it is a file in the repo
rather than a click someone has to remember on a fresh project.

**Firestore cannot search text, at all.** FR-LIST-005 asks for a keyword search over listing
titles, which in SQL is one `LIKE` and in Firestore is nothing: there is no substring operator,
no case-insensitive comparison, and the official answer in Firebase's own documentation is to
mirror the collection into a paid third-party search service such as Algolia. `>=`/`<` on a
string does give prefix matching, but only from the first character — it finds "Casio" typed as
"Cas" and never as "calc", and it cannot ignore case without storing a second lowercased copy
of every title. So the search is done on the device, over the listings the feed has already
downloaded. For a campus marketplace of tens to low hundreds of items that is the entire feed,
so the limitation is invisible; it would not hold at ten thousand listings, where the right
answer is the search service this project cannot pay for.

**Android 11 hides other apps, including the one the whole app depends on.** The
"Chat on WhatsApp" button uses `setPackage("com.whatsapp")` so the chat opens directly instead of
through a chooser. Since Android 11 an app cannot see what else is installed unless it declares
what it is looking for, so that intent fails to resolve on any modern phone — with WhatsApp
sitting right there on the home screen — until `<queries>` in the manifest names the package.
The related trap is `resolveActivity`, the usual "is this app installed?" check: under package
visibility it answers "no" for exactly the same reason, so a check that looks correct reports
WhatsApp as missing. Starting the intent and catching `ActivityNotFoundException` is what
actually works, and it is one call instead of two.

**A denormalised field is a trade, not a shortcut.** The feed card shows the seller's branch, but
branch lives on the `users` document while the card is drawn from a `listings` document. Reading
the seller per card would turn a twenty-item feed into twenty-one queries, so branch is copied
onto the listing at publish time alongside the name. The cost is that it is a *snapshot* — a
student who later switches branch keeps the old one on listings already posted — and that
listings written before the field existed carry an empty string, which the card has to handle by
dropping the separator with it rather than rendering "Aarav Shah  ·". The seller's WhatsApp
number deliberately did *not* get the same treatment: copying it onto every listing would put a
phone number in a document the whole campus can read, so Item Detail pays for a second read
instead.

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

**Cloudinary's Android SDK was the wrong size for the job.** The obvious move for Day 5 was to
add `com.cloudinary:cloudinary-android`, but it is built around a background upload service with
its own queue, retry policy, callbacks and lifecycle — all of which would then have to be bridged
back into the coroutine the ViewModel is already running in. The REST call underneath it is a
single multipart POST, and OkHttp was already in the build for Coil. Writing that one request by
hand (`data/CloudinaryUploader.kt`) turned out to be about sixty lines and removed a dependency
entirely, at the cost of parsing the JSON response manually.

**Every photo came back sideways.** The first camera capture uploaded fine and then rendered
rotated 90°. Phone cameras do not rotate the pixels when the phone is held upright — they store
the orientation in an EXIF tag and leave the image as the sensor saw it, and the gallery, Coil and
the picker preview all read that tag, so the photo looks correct everywhere *until* it is
re-encoded. Compressing a `Bitmap` back to JPEG drops the tag, so the rotation has to be baked
into the pixels before compressing (`ListingPhoto.applyOrientation`).

**Decoding a full-size photo is how image pickers run out of memory.** A 12-megapixel capture
decoded at full size needs roughly 48 MB of heap for a bitmap that is about to be scaled down and
thrown away. The fix is to decode twice: once with `inJustDecodeBounds` to read the header and
learn the real dimensions, then again with an `inSampleSize` that asks `BitmapFactory` for an
already-subsampled bitmap. Subsampling only works in powers of two, so an exact scale to 1600 px
still follows it.

**A null that meant success, read as a failure.** The first end-to-end test rejected every photo
with "that photo could not be read", while the preview of that exact same URI drew perfectly on
the screen above the message — and nothing was logged, because nothing had thrown. The
measure-the-header pass is written as `openInputStream(uri)?.use { decodeStream(...) } ?: fail`,
and with `inJustDecodeBounds = true` **`decodeStream` returns null by contract** — it is being
asked to measure the image, not allocate it. So the elvis was testing the decode result when the
only thing worth testing was whether the stream opened. Two nulls that mean opposite things, one
line apart. It was caught by running the real flow on the emulator rather than by any unit test,
since the rule it broke lives in `BitmapFactory`, not in the app's own logic.

**`file://` URIs cannot cross an app boundary.** Handing the camera app a plain file path to write
into throws `FileUriExposedException` — has done since Android 7. The photo has to be offered as a
`content://` URI from a `FileProvider` declared in the manifest and scoped, in
`res/xml/file_paths.xml`, to the one cache directory captures live in. The upside is that the app
needs no storage permission and no `CAMERA` permission at all: Android only demands `CAMERA` of an
app that declares it, and this one asks another app to take the photo rather than driving the
camera itself.

**A Firestore write with no network waits instead of failing.** Firestore applies a write to its
local cache immediately and only completes the `Task` once the server acknowledges it, so
publishing offline hangs rather than erroring. The tempting fix — a timeout — is wrong: the write
is already queued and will land when connectivity returns, so reporting a failure would produce a
duplicate listing as soon as the user retried. It is left alone deliberately, and documented in
`ListingRepository`; in practice the Cloudinary upload fails first when the network is down, which
is where an offline publish actually stops.

**A dependency ahead of the toolchain.** Coil 3.5.0+ is compiled with Kotlin 2.4, whose metadata
the Kotlin 2.2 compiler bundled with AGP 9.3.1 cannot read — the build failed with a wall of
`Class 'kotlin.Unit' was compiled with an incompatible version of Kotlin` errors that pointed at
this project's own source files rather than at the real culprit. Reading the dependency tree
(`gradlew :app:dependencyInsight`) traced it to Coil, and pinning Coil to 3.4.0 resolved it.

**ML Kit recognises objects, not campus items.** Google's default Image Labeling model has a
vocabulary of 400+ labels trained on general-purpose photos. It reliably returns "book" for a
textbook and "bicycle" for a cycle, but has no concept of a "drafter" or a "lab coat" as distinct
from an ordinary coat or jacket. CampusKart bridges this with a hand-written mapping table
(`CategorySuggestionMapper`) that maps broad ML Kit labels to the app's fixed categories — "coat"
and "jacket" both map to Lab Coat, "ruler" and "compass" map to Drafter — and falls back to Other
when no label clears the 70% confidence threshold. The mapping is necessarily lossy: a photo of a
drafter set photographed at an angle might be labelled "tool" rather than "ruler", and a lab coat
folded in a bag might not be recognised at all. The feature is presented as a convenience hint,
not a classification — the seller always has full control over which category is selected, and
posting is never blocked by a failed or missing suggestion.

**Bundled model vs. Play Services model.** ML Kit offers two deployment modes. The Play Services
variant adds only ~200 KB to the APK but downloads the model on first use, which means the
suggestion silently does nothing the first time it is tried on a fresh device — bad for a live
demo. The bundled variant adds ~5.7 MB to the APK but works immediately and offline. The bundled
model was chosen because a reliable demo matters more than APK size for a college presentation.

**Open signup and the trust trade-off.** Any email can create an account — there is no college
domain check and no email verification step. This is a deliberate choice: adding domain
restriction would lock out students whose college uses personal Gmail addresses, and email
verification adds a step that slows down a live demo. The trade-off is that a stranger could sign
up and post irrelevant items. For a class project this was judged acceptable; a production version
would add moderation or domain-based access control.

---

## Objectives

Per the course assignment (2CEIT5PE18 — Mobile Application Development, Semester V):

1. **Build a functional Android application** using Kotlin and Jetpack Compose that solves a real
   problem for university students — buying and selling used academic items within a campus.
2. **Demonstrate Firebase integration** — Authentication for user management and Cloud Firestore
   for real-time data storage, with security rules enforcing ownership constraints.
3. **Demonstrate sharing data between applications** — the WhatsApp deep link on Item Detail
   uses Android implicit intents to hand off a conversation to WhatsApp, satisfying the
   assignment's inter-app communication requirement.
4. **Demonstrate on-device AI** — ML Kit Image Labeling runs a pretrained model on the phone to
   suggest a listing's category from its photo, with no server call and no internet needed.
5. **Maintain clean documentation and version history** — daily commits with clear messages,
   and this README documenting features, objectives, tech stack, and challenges faced.

---

## Demo script

A short walkthrough for the live presentation. Bring a real item (a book, a calculator, anything
from the category list) to post during the demo.

1. **Open the app.** It lands on the Feed. If signed in from testing, listings appear; if not,
   the Login screen is shown.
2. **Sign up or log in.** Create an account with a real name, branch, semester, WhatsApp number.
   Show that the form validates each field before submission.
3. **Post an item.** Tap the Post tab. Take a photo of the real item with the Camera button.
   Point out the AI suggestion banner that appears — ML Kit scanned the photo on-device and
   pre-selected a category. Change the category manually to show override works. Fill in the
   title, description, price and condition. Tap Publish and watch the three-step progress
   (Preparing → Uploading → Saving).
4. **See it in the Feed.** Tap the Feed tab. The listing just posted appears at the top with its
   Cloudinary-hosted photo. Use the search bar to find it by title. Tap a category chip to
   filter.
5. **Open Item Detail.** Tap the listing card. The full detail loads with the seller's live
   profile. Tap "Chat on WhatsApp" — WhatsApp opens with a prefilled message naming the item.
   Press back to return.
6. **Manage the listing.** Tap My items. The listing appears with Edit, Mark sold, and Delete
   actions. Mark it sold — it disappears from the Feed. Undo — it reappears. Edit the price,
   save, confirm the change on the card.
7. **Show the Profile tab.** Name, branch, semester, WhatsApp number, and the logout button.
8. **Log out and back in.** Tap logout, confirm the Login screen appears, log back in, confirm
   the Feed loads with the session restored.

**Key points to mention during the demo:**
- Photos are compressed before upload (1600 px, JPEG 80) to save mobile data
- ML Kit runs entirely on-device — no internet needed for the category suggestion
- WhatsApp number is never shown on screen — only used to build the deep link
- Firestore security rules enforce that only the seller can edit or delete their own listings
- The app works offline for browsing already-loaded listings; posting requires connectivity
