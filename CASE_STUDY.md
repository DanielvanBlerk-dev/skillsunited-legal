# Skills United — Case Study

**A real-time, cloud-backed skill-sharing Android app**
Kotlin · Firebase · Solo build

---

## Summary

Skills United is a peer-to-peer skill-sharing Android application. Users list skills they can teach and skills they want to learn, browse other members, send connection requests, and message each other in real time. I designed, built, secured, and tested the entire application end to end as a solo developer — from the Firebase data model and security rules through to the UI, a custom theming engine, and multi-factor authentication.

This case study covers what the app does, the engineering decisions behind it, the problems I solved along the way, and the deliberate decision to present it as a controlled demonstration rather than operate it as a live public service.

---

## My role

Sole developer. I was responsible for every layer:

- Product and feature scope
- UI/UX and layout (XML, Material Components)
- Client architecture (Kotlin, `BaseActivity` pattern)
- Backend data modelling (Cloud Firestore)
- Authentication and account security (Firebase Auth, SMS MFA)
- Server-side security (Firestore rules, validation, rate limiting)
- Real-time messaging and notifications
- Build configuration, ProGuard, and release hardening
- A regulatory/risk assessment of the platform before deciding how to ship it

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | XML layouts, Material Components, CardView, CoordinatorLayout |
| Backend | Firebase — Auth, Cloud Firestore, Storage, Analytics |
| App integrity | Firebase App Check (Play Integrity) |
| Image loading | Glide |
| Theming | Custom engine + ColorPickerView |
| Async | Kotlin Coroutines |
| Networking | OkHttp (EmailJS report notifications) |
| Min / Target / Compile SDK | 24 / 36 / 36 |

---

## Core features

- **Account system** — email/password registration with full client- and server-side validation (name, email, password complexity, and globally unique usernames reserved through atomic batch writes).
- **Optional SMS multi-factor authentication** — separate enrolment and sign-in flows.
- **Profiles** — editable bio, "skills I can teach," and "skills I want to learn," with avatars stored in Firebase Storage.
- **Skill discovery** — browse all members with multi-tag, space-separated search.
- **Connection requests** — send and respond to match requests before a conversation opens.
- **Real-time 1:1 messaging** — instant delivery via Firestore snapshot listeners, with an in-app message banner and an unread-count badge.
- **Inbox** — live conversation list with unread/read state tracking.
- **Custom theming** — four presets (Light, Dark, Forest, Sunset) plus a colour-wheel picker exposing eight independently customisable colour roles, persisted across sessions.
- **Safety tooling** — in-app user reporting, message sanitisation, and screenshot prevention on sensitive screens.

---

## Architecture & technical decisions

**`BaseActivity` pattern.** Every screen extends a shared `BaseActivity` that applies the active theme on each resume, injects the settings and inbox icons programmatically into any layout root, and gates protected screens behind a Terms-of-Service acceptance check. Screens that shouldn't show those icons (login, register, settings, inbox) override the relevant hooks. This keeps cross-cutting concerns — theming, navigation chrome, consent — in one place rather than duplicated across a dozen activities.

**Deterministic chat IDs.** Each conversation between two users is keyed as `min(uidA, uidB)_max(uidA, uidB)`. Because the ID is derived purely from the two participant UIDs, either user computes the same ID independently with no lookup or coordination — which makes opening, listening to, and securing a conversation trivial.

**A persistent notification service.** Real-time message alerts are driven by a singleton `MessageListenerService` started once at login and stopped only at logout. Its `onNewMessage` callback is rebound to whichever activity is currently in the foreground, so the banner always surfaces on the screen the user is actually looking at, and a red badge on the inbox icon reflects unread counts live.

**A custom theming engine.** Rather than relying solely on static XML themes, `ThemeManager` stores eight colour roles in `SharedPreferences` and the app walks the view tree on resume to apply them. This is what makes both the presets and the live colour-wheel customisation possible without restarting the app.

---

## Engineering challenges I solved

**Android 15 keyboard + RecyclerView clipping.** On the profile editor, a `RecyclerView` of skill entries kept collapsing and clipping its items when the soft keyboard opened on Android 15. After diagnosing it as an interaction between the insets behaviour and the recycler's measurement pass, I rebuilt that section using programmatically generated `LinearLayout` views inside a `ScrollView`, which rendered reliably across devices.

**A Firestore query returning empty results.** Loading the inbox by querying the `chats` collection directly returned empty results under certain conditions. Rather than fight the symptom, I changed the strategy: the inbox now derives the set of possible chat IDs from the user's connections and reads each conversation directly, attaching real-time listeners for live updates. More predictable, and it sidesteps the quirk entirely.

**Unique usernames without race conditions.** Usernames must be globally unique. I enforce this with atomic batch writes against a dedicated `usernames` collection so that reserving a username and creating the account either both succeed or both fail — no window for two users to claim the same handle.

**Keeping notifications alive across navigation.** Getting message alerts to appear reliably regardless of which screen the user was on — without spawning duplicate listeners — drove the singleton-service design described above.

---

## Security & privacy

Security was treated as a first-class requirement, not an afterthought:

- **Firestore security rules** with validation functions and rate limiting, enforcing that users can only write their own data and only read conversations they're part of.
- **Defence-in-depth validation** — input validated on the client *and* in the security rules.
- **Message sanitisation** — HTML stripped and length capped before storage.
- **HTTPS-only** networking enforced via a network security configuration.
- **Screenshot prevention** (`FLAG_SECURE`) on the chat and profile screens.
- **Firebase App Check** (Play Integrity) to attest that requests come from a genuine instance of the app.
- **Optional SMS MFA** for users who want a second factor.
- **ProGuard** enabled for release builds.
- **In-app reporting** that records reports to Firestore and dispatches a notification to the operator.

---

## Risk assessment & shipping decision

Before treating Skills United as something to operate publicly, I assessed the regulatory surface of running a user-generated-content platform in Australia: publisher/intermediary liability under the 2024 defamation reforms, obligations attaching to messaging services under the Online Safety Act 2021, and the social-media minimum-age regime. I concluded that operating it as a live public service would carry obligations and liabilities disproportionate to its purpose as a portfolio project.

I therefore made a deliberate decision to present Skills United as a **controlled, sandboxed demonstration** — seeded with sample data, restricted in distribution — rather than an open public deployment. The full engineering is intact and demonstrable; the operational risk surface of an open platform is simply not part of a portfolio piece.

I consider that assessment part of the work. Knowing *whether* and *how* to ship something is as much an engineering responsibility as building it.

---

## What this project demonstrates

- End-to-end ownership of a real, cloud-backed mobile application
- Practical Firebase architecture: Auth, Firestore data modelling, Storage, security rules, and App Check
- Real-time systems — snapshot listeners, live unread state, foreground-aware notifications
- Security engineering — server-side rules, validation, MFA, sanitisation, release hardening
- Pragmatic problem-solving against real device and platform constraints
- Sound product and risk judgment about how a service should be shipped

---

## Status

Feature-complete demonstration build. Tested on a Samsung Galaxy S23 Ultra (Android 15) and a Pixel 6 emulator (API 35). Presented as a sandboxed demo for portfolio purposes rather than a live public service.
