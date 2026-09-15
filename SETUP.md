# CampusKart — External Service Setup

You only have to do this once. It takes about 20 minutes.

CampusKart depends on two free external services:

| Service | Used for | Cost |
|---|---|---|
| **Firebase** (Auth + Cloud Firestore) | Login/signup and the listings database | Free (Spark plan) |
| **Cloudinary** | Hosting listing photos | Free tier |

Neither needs a credit card. Work through Part 1, then Part 2, then Part 3 to verify.

> **Why not Firebase Storage for photos?** Firebase Cloud Storage now requires the paid Blaze
> plan for newly created projects, so CampusKart uses Cloudinary's free tier instead.
> (Recorded in `PRD.md` Section 6, Key Decisions Log.)

---

## Part 1 — Firebase

### 1.1 Create the project

1. Go to <https://console.firebase.google.com> and sign in with a Google account.
2. Click **Create a project** (or **Add project**).
3. Project name: `CampusKart`. Accept the terms, click **Continue**.
4. On the Google Analytics step, **turn Analytics off**. CampusKart does not use it, and it
   just adds setup friction.
5. Click **Create project** and wait for it to finish, then **Continue**.

### 1.2 Enable Email/Password authentication

1. In the left sidebar, open **Build → Authentication**.
2. Click **Get started**.
3. Open the **Sign-in method** tab.
4. Click **Email/Password** in the providers list.
5. Turn on the first toggle (**Email/Password**). Leave *Email link (passwordless sign-in)* off.
6. Click **Save**.

### 1.3 Create the Firestore database

1. In the left sidebar, open **Build → Firestore Database**.
2. Click **Create database**.
3. Location: pick **asia-south1 (Mumbai)** — closest region, so the app feels fastest here.
   **This cannot be changed later**, so don't rush past it.
4. Choose **Start in test mode**.
5. Click **Enable**.

> ⚠️ **Test mode expires after 30 days.** Firebase creates a rule that allows all reads and
> writes until a date roughly one month out; after that date every request is denied and the app
> will look broken for no obvious reason. We replace these with real rules on **Day 8** of the
> build plan. If listings suddenly stop loading before then, check this first.

### 1.4 Register the Android app and download `google-services.json`

1. Click the **gear icon → Project settings** (top of the left sidebar).
2. Scroll to **Your apps** and click the **Android** icon.
3. Fill in:
   - **Android package name:** `com.example.campuskart` — this must match *exactly*, character
     for character. It is the `applicationId` in `app/build.gradle.kts`.
   - **App nickname:** `CampusKart` (optional)
   - **Debug signing certificate SHA-1:** leave blank. It is only needed for Google Sign-In and
     phone auth, and CampusKart uses neither.
4. Click **Register app**.
5. Click **Download google-services.json**.
6. Move that file into the **`app/`** folder of this project, so the final path is:

   ```
   CampusKart/app/google-services.json
   ```

   It will overwrite the placeholder that ships with the repo. That is correct and expected.
7. Back in the browser, click **Next** through the remaining SDK instructions and then
   **Continue to console** — those Gradle steps are already done in this repo.

> **This file is gitignored.** `app/google-services.json` is listed in `.gitignore`, so your real
> Firebase project identifiers never get pushed to GitHub. A placeholder
> `app/google-services.json.template` *is* committed so that a fresh clone still compiles.

---

## Part 2 — Cloudinary

### 2.1 Create the account

1. Go to <https://cloudinary.com> and click **Sign up for free**.
2. Register with an email address and verify it.
3. When asked what you're building, any answer is fine — it only affects onboarding tips.

### 2.2 Find your cloud name

1. Open the **Dashboard** (also called *Programmable Media dashboard*).
2. Find **Product Environment Credentials**. Your **Cloud name** is shown there — a short string
   like `dxk4v9abc`.
3. Copy it. You need only the cloud name. **Do not copy the API secret** — it must never go into
   an Android app, because anything shipped inside an APK can be extracted.

### 2.3 Create an unsigned upload preset

An *unsigned* preset lets the app upload photos without holding the API secret.

1. Click the **gear icon (Settings)**.
2. Open **Upload** → scroll to **Upload presets**.
3. Click **Add upload preset**.
4. Set:
   - **Preset name:** `campuskart_unsigned`
   - **Signing Mode:** **Unsigned** ← this is the important one; the default is *Signed*.
   - **Folder:** `campuskart` (optional, but keeps your media library tidy)
5. Click **Save**.

> **Known tradeoff:** anyone who extracts the preset name from the APK could upload images to
> your Cloudinary account. Signed uploads would fix this, but they require a server to sign the
> request — out of scope for a free, serverless class project. This is worth writing up in the
> README's "challenges faced" section.

### 2.4 Put the values in `local.properties`

Open `local.properties` in the project root and fill in the two blank keys at the bottom:

```properties
cloudinary.cloudName=dxk4v9abc
cloudinary.uploadPreset=campuskart_unsigned
```

No quotes, no spaces around the `=`.

`local.properties` is already gitignored, so these values stay off GitHub. The build reads them
and exposes them to the app as `BuildConfig.CLOUDINARY_CLOUD_NAME` and
`BuildConfig.CLOUDINARY_UPLOAD_PRESET`.

---

## Part 3 — Verify it worked

1. In Android Studio, click **File → Sync Project with Gradle Files**.
2. Run the app on a device or emulator **that has internet access**.
3. The app should build and launch to the **Login screen**. If it does, Firebase and Gradle are wired correctly.
4. Create an account (Signup) and post a test listing to verify Cloudinary uploads work.

---

## Troubleshooting

**"Firebase config — Still using the placeholder file"**
The real `google-services.json` isn't in place. Confirm the path is exactly
`app/google-services.json` (inside the `app` folder, not the project root), then Sync Gradle and
rebuild.

**Build fails: "File google-services.json is missing"**
Same cause. The repo ships a placeholder so this shouldn't happen, but if you deleted it, copy
`app/google-services.json.template` to `app/google-services.json` and continue.

**"Cloud Firestore — PERMISSION_DENIED"**
Either the database wasn't created in test mode, or the 30-day test-mode window has expired. Go
to **Firestore Database → Rules** in the console and check the date in the rule.

**"Cloudinary reachable — HTTP 404"**
The cloud name is misspelled in `local.properties`. Copy it again from the Cloudinary dashboard.
(Less likely: your account's demo `sample.jpg` was deleted, which is harmless.)

**Nothing loads / everything fails**
Check the device actually has internet, and that the emulator isn't offline.
