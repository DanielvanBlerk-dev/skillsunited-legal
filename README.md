# Skills United

A real-time, cloud-backed **skill-sharing Android app**. Users list skills they can teach and skills they want to learn, browse other members, send connection requests, and chat in real time.

Built solo with **Kotlin** and **Firebase**.

© 2026 Daniel van Blerk. All rights reserved. Source-available for portfolio review only — see [License & ownership](#license--ownership).

> **Note:** This repository is a portfolio demonstration. Skills United is presented as a sandboxed demo (seeded sample data, restricted distribution), not operated as a live public service. See [Project status](#project-status).

---

## Demo

<!-- Replace these with your own links/files once uploaded -->
- 🎥 **Walkthrough video:** _add link_
- 🖼️ **Screenshots:** see [`/screenshots`](./public/screenshots)

| Browse | Profile | Chat |
|---|---|---|
| _screenshot_ | _screenshot_ | _screenshot_ |

---

## Features

- **Email/password auth** with full validation and globally unique usernames (atomic batch-write reservation)
- **Optional SMS multi-factor authentication** (separate enrolment and sign-in flows)
- **Editable profiles** — bio, skills to teach, skills to learn, avatar (Firebase Storage)
- **Skill discovery** — browse members with multi-tag, space-separated search
- **Connection requests** before a conversation opens
- **Real-time 1:1 messaging** via Firestore snapshot listeners
- **Live inbox** with unread-count badge and read/unread state
- **In-app message banner** that follows the active screen
- **Custom theming** — 4 presets + a colour-wheel picker over 8 colour roles, persisted
- **Safety tooling** — user reporting, message sanitisation, screenshot prevention

---

## Tech stack

- **Language:** Kotlin
- **UI:** XML layouts, Material Components, CardView, CoordinatorLayout
- **Backend:** Firebase — Auth, Cloud Firestore, Storage, Analytics
- **App integrity:** Firebase App Check (Play Integrity)
- **Images:** Glide
- **Theming:** custom `ThemeManager` + ColorPickerView
- **Async:** Kotlin Coroutines
- **Networking:** OkHttp (report notifications via EmailJS)
- **SDK:** min 24 / target 36 / compile 36 · AGP 9.2.0

---

## Architecture

A high-level overview lives in [`ARCHITECTURE.md`](./ARCHITECTURE.md). In short:

- A shared `BaseActivity` centralises theming, navigation chrome, and consent gating.
- Conversations use deterministic IDs (`min(uidA,uidB)_max(uidA,uidB)`).
- A singleton `MessageListenerService` drives foreground-aware real-time notifications.
- Firestore security rules enforce per-user access with validation and rate limiting.

---

## Project structure

```
app/
├── src/main/
│   ├── java/com/dkvb/skillswap/
│   │   ├── LoginActivity.kt
│   │   ├── RegisterActivity.kt
│   │   ├── MainActivity.kt
│   │   ├── ProfileActivity.kt
│   │   ├── UserProfileActivity.kt
│   │   ├── ChatActivity.kt
│   │   ├── InboxActivity.kt
│   │   ├── MatchRequestActivity.kt
│   │   ├── SettingsActivity.kt
│   │   ├── MfaEnrollmentActivity.kt
│   │   ├── MfaSignInActivity.kt
│   │   ├── BaseActivity.kt
│   │   ├── ThemeManager.kt
│   │   ├── TermsDialogHelper.kt
│   │   ├── MessageListenerService.kt
│   │   ├── MessageAdapter.kt
│   │   ├── Message.kt
│   │   ├── AvatarView.kt
│   │   └── EmailService.kt
│   ├── res/
│   │   ├── layout/
│   │   ├── drawable/
│   │   ├── values/
│   │   └── xml/network_security_config.xml
│   └── AndroidManifest.xml
├── build.gradle.kts
├── proguard-rules.pro
└── google-services.json        ← NOT committed (see Setup)
firestore.rules                  ← security rules
firestore.indexes.json           ← required composite indexes
```

---

## Setup

> The app will **not build or run without your own Firebase project**, because the credential files are intentionally excluded from version control.

1. **Clone**
   ```bash
   git clone https://github.com/<you>/skills-united.git
   ```

2. **Create a Firebase project** at the Firebase console and add an Android app with the package name `com.dkvb.skillswap` (or change the `applicationId` in `app/build.gradle.kts` to your own).

3. **Enable** Authentication (Email/Password, and Phone if you want MFA), Cloud Firestore, and Storage.

4. **Download `google-services.json`** for your app and place it in `app/`. A template is provided as [`app/google-services.json.example`](./app/google-services.json.example).

5. **Deploy the security rules and indexes:**
   ```bash
   firebase deploy --only firestore:rules,firestore:indexes
   ```

6. **Configure report-email notifications (optional).** Copy `local.properties.example` to `local.properties` and add your EmailJS keys (see [Configuration](#configuration)). These are read at build time — no secrets are hard-coded.

7. **Build & run** from Android Studio, or:
   ```bash
   ./gradlew assembleDebug
   ```

---

## Configuration

Secrets are supplied at build time via `local.properties` (git-ignored) and exposed through `BuildConfig`, rather than committed to source:

```properties
# local.properties (DO NOT COMMIT)
EMAILJS_SERVICE_ID=your_service_id
EMAILJS_TEMPLATE_ID=your_template_id
EMAILJS_PUBLIC_KEY=your_public_key
```

---

## Security

- Firestore security rules with validation functions and rate limiting
- Client- and server-side input validation
- Message sanitisation (HTML stripped, length-capped)
- HTTPS-only network security configuration
- `FLAG_SECURE` screenshot prevention on chat and profile screens
- Firebase App Check (Play Integrity)
- Optional SMS MFA
- ProGuard enabled for release builds

No credentials, keystores, or service files are committed to this repository. See [`.gitignore`](./.gitignore).

---

## Project status

Feature-complete **demonstration** build, tested on a Samsung Galaxy S23 Ultra (Android 15) and a Pixel 6 emulator (API 35).

Skills United is presented as a portfolio piece and a sandboxed demo. It is intentionally **not** operated as a live, openly registrable public service. See [`CASE_STUDY.md`](./CASE_STUDY.md) for the reasoning, including the regulatory assessment behind that decision.

---

## License & ownership

© 2026 Daniel van Blerk. All rights reserved.

This repository is **source-available for portfolio and evaluation purposes only**. You're welcome to read the code and clone it to review my work — but copying, modifying, redistributing, or reusing it in any other project (commercial or non-commercial) is not permitted without my prior written permission. See the [`LICENSE`](./LICENSE) file for the full terms.

The complete commit history in this repository serves as the record of authorship and the date the work was created.
