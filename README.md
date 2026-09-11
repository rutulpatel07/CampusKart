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
6. **Create the feed's Firestore index** (once, after the first listing exists) — see below.

### The Home Feed needs one Firestore index

The feed asks Firestore for *available listings, newest first*. That pairs an equality filter on
`status` with an ordering on `createdAt`, and Firestore can only serve that combination from a
**composite index** — which it does not create by itself. Until the index exists, the Feed tab
shows "Firestore needs a one-time index for the feed" instead of listings.

Two ways to create it, either is fine:

- **From Logcat (easiest).** Open the Feed once, find the `FAILED_PRECONDITION` line from
  Firestore in Logcat, and tap the `https://console.firebase.google.com/...` link in it. The
  console opens with the index already filled in — press Create and wait a minute or two.
- **From the repo.** [`firestore.indexes.json`](firestore.indexes.json) is the same index written
  down. With the Firebase CLI: `firebase deploy --only firestore:indexes`.

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
| 2 | Home Feed | Listing cards (photo, title, price, seller and branch); search field and category chips land on Day 7 |
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

Until Days 7–8 build them, the remaining sketch destinations (My items and Profile) render their
Day 2 mockups under a banner naming the day the real screen arrives, so the shell can be walked
through as a whole app in the meantime. Post Item was the first sketch replaced by a real screen,
on Day 5; the Feed and Item Detail followed on Day 6. The Day 1 service check and the Day 2
mockup gallery are still reachable from a small **Dev tools** link at the bottom of the Login
screen; both go away on Day 10.

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
disabled — there is nothing wrong for the user to fix. Edit / Mark sold / Delete arrive on Day 7.

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
filters by category from Day 7, and a filter cannot work against values a hundred students typed
by hand. That list is also exactly what ML Kit's labels have to be mapped onto on Day 9.
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
│   └── CloudinaryUploader.kt # the unsigned multipart upload
├── model/
│   └── CampusOptions.kt     # the fixed branch, semester, category and condition lists
├── setup/                   # temporary: verifies Firebase + Cloudinary connectivity
│   ├── SetupCheck.kt
│   └── SetupStatusScreen.kt
└── ui/
    ├── auth/                # Login and Signup screens, their ViewModels, and form rules
    ├── post/                # Post Item screen, its ViewModel, and the listing form rules
    ├── feed/                # Home Feed screen and its ViewModel
    ├── detail/              # Item Detail screen, its ViewModel, and the WhatsApp button
    ├── components/          # small composables shared across screens
    ├── navigation/          # routes, the bottom bar, and the NavHost
    ├── screens/             # temporary: sketch-backed placeholders for Days 7–8
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
