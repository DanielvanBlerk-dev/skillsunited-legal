# Skills United — Android App Project Summary
*Paste this into a new Claude conversation to resume development instantly.*

---

## Project Overview
**Skills United** is a skill-swap matchmaking Android app. Users list skills they can teach and want to learn, browse others, send chat requests, and message each other. Built with Kotlin, XML layouts, and Firebase.

**Package:** `com.dkvb.skillswap`
**Min SDK:** 24, Target/Compile SDK: 36, AGP: 9.2.0
**Test devices:** Samsung Galaxy S23 Ultra (Android 15, SwiftKey), Pixel 6 emulator (API 35)
**Test accounts:** dkvb (UID: `h27zppsz2eZ45XTPDv1db4GTLVy2`), Todd Vanders (UID: `axYGOyfDRBRkyE2FDRqT8NO08A72`)

---

## Dependencies (build.gradle app)
```kotlin
implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.firebase:firebase-firestore-ktx")
implementation("com.google.firebase:firebase-storage-ktx")
implementation("com.google.firebase:firebase-analytics")
implementation("com.google.android.material:material:1.11.0")
implementation("androidx.cardview:cardview:1.0.0")
implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
implementation("com.github.bumptech.glide:glide:4.16.0")
implementation("com.github.skydoves:colorpickerview:2.2.4")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.google.firebase:firebase-appcheck-playintegrity")
```
build.gradle (project) requires: `maven { url = uri("https://jitpack.io") }`

---

## Kotlin Files

### Activities
- **`LoginActivity.kt`** — email login, MFA support, terms check via `TermsDialogHelper.showTermsIfNotAccepted()`, `tvTermsLink` click shows terms dialog, `applyTheme()` on resume, no inbox icon
- **`RegisterActivity.kt`** — unique username check, `validateInput()` with full password/username/name/email rules (including special char requirement), terms dialog shown before account creation, no inbox icon
- **`MainActivity.kt`** — browse skills, multi-tag space-separated search, Profile/Requests/Log Out buttons (32dp height, 11sp text), `checkTermsAccepted {}` wraps `setupUI()`, `migrateExistingChats()` on launch
- **`ProfileActivity.kt`** — programmatic LinearLayout skills (NOT RecyclerView), `adjustNothing` keyboard, MFA button, `sanitizeSkills()`, `profileLoaded` flag, screenshot prevention, terms footer link
- **`UserProfileActivity.kt`** — view profile, match request system (Request Chat / Request Sent / Open Chat button states), `checkExistingRequest()`, `sendMatchRequest()`, `reportUser()` with EmailJS, terms link at bottom, single `btnReport` (was duplicated — fixed)
- **`ChatActivity.kt`** — real-time chat, `sanitizeMessage()`, `ensureUserChatsUpdated()`, `updateLastRead()`, version-aware insets (API 34+), screenshot prevention, `markChatAsRead()` on open
- **`SettingsActivity.kt`** — 4 preset themes, color wheel picker, Terms/Privacy buttons with acceptance date display, no settings/inbox icon
- **`InboxActivity.kt`** — real-time inbox via `userChats/{uid}` document (chatIds array + `lastRead_{chatId}` timestamps), bold unread based on Firestore `lastRead` comparison, real-time `userChats` snapshot listener, `updateLastRead()` on chat tap
- **`MatchRequestActivity.kt`** — two tabs (Incoming/Outgoing), Incoming shows Accept/Decline, Outgoing shows Cancel, real-time Firestore listeners for both, theme-aware, nav bar insets handled, `applyTheme()` on resume
- **`MfaEnrollmentActivity.kt`** — optional SMS 2FA enrollment
- **`MfaSignInActivity.kt`** — handles MFA challenge on login
- **`BaseActivity.kt`** — theme, status bar, settings icon, inbox icon with red badge, banner container, message listener, match request banner, `checkTermsAccepted()`, `Toast` and `Firebase` imports present
- **`ThemeManager.kt`** — SharedPreferences colors + terms acceptance tracking (`hasAcceptedTerms`, `setTermsAcceptedWithDate`, `clearTermsAccepted`, `getTermsAcceptedDate`)
- **`MessageListenerService.kt`** — persistent Firestore listeners, unread tracking (`unreadChatIds`, `unreadPerChat`, `readChatIds`), `listenForMatchRequests()` uses `snapshot.documents` with `notifiedRequestIds` set, poll every 15s, `onNewMatchRequest` callback, NO duplicate unread increment block
- **`NotificationBanner.kt`** — slides banner from top, 6s auto-dismiss
- **`TermsDialogHelper.kt`** — shows ToS summary dialog with checkbox, `TERMS_URL` and `PRIVACY_URL` constants, `showTermsDialog()` and `showTermsIfNotAccepted()`
- **`EmailService.kt`** — OkHttp POST to EmailJS API for report notifications, strict mode OFF or private key added in EmailJS dashboard, non-browser API access enabled
- **`AvatarView.kt`** — colored circle with initials
- **`SkillsUnitedApp.kt`** — Application class, `ThemeManager.init()`

### Data Classes
- **`User.kt`** — uid, name, username, email, bio, skillsToTeach, skillsToLearn, createdAt
- **`Message.kt`** — senderId, text, timestamp
- **`InboxMessage.kt`** — chatId, senderId, senderName, text, timestamp, isUnread
- **`MatchRequest.kt`** — id, fromUid, fromName, fromUsername, fromBio, fromSkillsToTeach, fromSkillsToLearn, toUid, toName, toSkillsToTeach, toSkillsToLearn, status (pending/accepted/declined), timestamp

### Adapters
- **`UserAdapter.kt`** — browse cards, theme-aware
- **`MessageAdapter.kt`** — chat bubbles, theme colors via GradientDrawable
- **`InboxAdapter.kt`** — bold for unread using `message.isUnread` field directly (NOT `isChatRead()`), theme-aware
- **`MatchRequestAdapter.kt`** — `showAcceptDecline` parameter controls Incoming vs Outgoing display, shows both users' skills with section headers, theme-aware including CardView background, imports `LinearLayout` and `CardView`
- **`PresetAdapter.kt`** — theme preset grid
- **`SkillAdapter.kt`** — exists but profile uses programmatic LinearLayout

### Layout Files
All activities use FrameLayout root for settings/inbox icon injection.
- **`activity_main.xml`** — FrameLayout root, bottomButtons LinearLayout with Profile/Requests/Log Out (32dp height, 11sp, insetTop/Bottom=0dp)
- **`activity_profile.xml`** — FrameLayout→ScrollView(fillViewport, clipToPadding=false)→LinearLayout, `llTeachSkills` and `llLearnSkills` LinearLayout containers, `btnMfa`, `tvTermsFooter`
- **`activity_chat.xml`** — FrameLayout→LinearLayout(id=chatRootLayout, fitsSystemWindows=false)
- **`activity_match_request.xml`** — FrameLayout root, `matchRequestRoot` LinearLayout id, title with top padding for status bar, tab buttons (36dp height), two RecyclerViews with `layout_weight=1` and `paddingBottom=80dp`
- **`activity_user_profile.xml`** — FrameLayout→ScrollView→LinearLayout: avatar, name, bio, teach, learn, btnMessage, single btnReport, tvTermsLink
- **`item_match_request.xml`** — CardView with `cardContent` LinearLayout id, avatar, name, username, bio, dividers, `tvTheirSkillsHeader`, tvTeach, tvLearn, divider, `tvYourSkillsHeader`, tvYourTeach, tvYourLearn, Accept/Decline buttons, Cancel button (gone by default)

---

## Firebase Structure
- `users/{uid}` — user documents
- `usernames/{username}` — reserved usernames → uid
- `chats/{chatId}/messages/{messageId}` — chatId = sorted UIDs joined by `_`
- `userChats/{uid}` — chatIds array + `lastRead_{chatId}` timestamps
- `matchRequests/{requestId}` — fromUid, fromName, fromUsername, fromBio, fromSkillsToTeach, fromSkillsToLearn, toUid, toName, toSkillsToTeach, toSkillsToLearn, status, timestamp
- `reports/{reportId}` — reportedUid, reportedName, reporterUid, reason, timestamp
- `rateLimit/{uid}` — for spam prevention

## Firestore Rules (complete)
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isAuthenticated() { return request.auth != null; }
    function isOwner(userId) { return request.auth.uid == userId; }
    function isValidString(field, maxLength) { return field is string && field.size() <= maxLength; }
    function isValidList(field, maxSize) { return field is list && field.size() <= maxSize; }
    function isValidUser() {
      return isValidString(request.resource.data.name, 50) &&
             isValidString(request.resource.data.username, 30) &&
             isValidString(request.resource.data.bio, 500) &&
             isValidList(request.resource.data.skillsToTeach, 20) &&
             isValidList(request.resource.data.skillsToLearn, 20);
    }
    function isValidMessage() {
      return isValidString(request.resource.data.text, 1000) &&
             request.resource.data.senderId == request.auth.uid &&
             request.resource.data.timestamp is number;
    }
    function isPartOfChat(chatId) { return request.auth.uid in chatId.split('_'); }
    function notSpamming() {
      return !exists(/databases/$(database)/documents/rateLimit/$(request.auth.uid)) ||
        get(/databases/$(database)/documents/rateLimit/$(request.auth.uid)).data.lastMessage < request.time.toMillis() - 1000;
    }

    match /users/{userId} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated() && isOwner(userId) && isValidUser();
      allow update: if isAuthenticated() && isOwner(userId) && isValidUser();
      allow delete: if false;
    }
    match /usernames/{username} {
      allow read: if true;
      allow create: if isAuthenticated() && username.size() >= 3 && username.size() <= 30 && username.matches('[a-z0-9._]+');
      allow delete: if isAuthenticated() && resource.data.uid == request.auth.uid;
      allow update: if false;
    }
    match /userChats/{userId} {
      allow read: if isAuthenticated() && isOwner(userId);
      allow write: if isAuthenticated();
    }
    match /chats/{chatId} {
      allow read: if isAuthenticated() && isPartOfChat(chatId);
      allow create: if false;
      allow update: if false;
      allow delete: if false;
      match /messages/{messageId} {
        allow read: if isAuthenticated() && isPartOfChat(chatId);
        allow create: if isAuthenticated() && isPartOfChat(chatId) && isValidMessage() && notSpamming();
        allow update: if false;
        allow delete: if false;
      }
    }
    match /{path=**}/messages/{messageId} { allow read: if isAuthenticated(); }
    match /rateLimit/{userId} {
      allow read: if isAuthenticated() && isOwner(userId);
      allow write: if false;
    }
    match /reports/{reportId} {
      allow create: if isAuthenticated();
      allow read, update, delete: if false;
    }
    match /matchRequests/{requestId} {
      allow read: if isAuthenticated() &&
        (resource == null ||
         resource.data.fromUid == request.auth.uid ||
         resource.data.toUid == request.auth.uid);
      allow create: if isAuthenticated() && request.resource.data.fromUid == request.auth.uid;
      allow update: if isAuthenticated() &&
        (resource.data.toUid == request.auth.uid || resource.data.fromUid == request.auth.uid);
      allow delete: if isAuthenticated() &&
        (resource.data.fromUid == request.auth.uid || resource.data.toUid == request.auth.uid);
    }
  }
}
```

## Firestore Indexes Required
- Collection group: `messages`, field: `timestamp` descending
- Collection group: `messages`, fields: `senderId` ascending + `timestamp` descending
- Collection: `matchRequests`, fields: `toUid` ascending + `status` ascending
- Collection: `matchRequests`, fields: `fromUid` ascending + `toUid` ascending
- Collection: `matchRequests`, fields: `fromUid` ascending + `status` ascending

---

## AndroidManifest.xml Activities
```xml
<activity android:name=".LoginActivity" android:exported="true">
  <intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter>
</activity>
<activity android:name=".RegisterActivity"/>
<activity android:name=".MainActivity"/>
<activity android:name=".ProfileActivity" android:windowSoftInputMode="adjustNothing"/>
<activity android:name=".UserProfileActivity"/>
<activity android:name=".ChatActivity" android:windowSoftInputMode="adjustResize"/>
<activity android:name=".SettingsActivity"/>
<activity android:name=".InboxActivity"/>
<activity android:name=".MatchRequestActivity"/>
<activity android:name=".MfaEnrollmentActivity"/>
<activity android:name=".MfaSignInActivity"/>
```

---

## Key Architecture Decisions
- **BaseActivity** injects settings gear + inbox chat bubble icons programmatically into any FrameLayout root
- **Message notifications** use persistent `MessageListenerService` singleton — started once on login, stopped only on logout
- **Match requests** use Firestore `matchRequests` collection. `listenForMatchRequests()` iterates `snapshot.documents` with `notifiedRequestIds` set
- **Inbox** reads from `userChats/{uid}` document (chatIds array). `lastRead_{chatId}` timestamps in same document determine bold/unread state
- **Profile skills** use programmatic LinearLayout views (not RecyclerView)
- **Terms acceptance** stored in SharedPreferences via ThemeManager. `checkTermsAccepted()` in BaseActivity gates all main screens. Cleared on logout
- **Chat IDs** always `min(uid1,uid2)_max(uid1,uid2)`
- **userChats migration** runs once on MainActivity launch to populate userChats for existing users

---

## Security Hardening (all completed)
✅ Firestore security rules with validation functions
✅ Input validation in RegisterActivity (name, username, email, password with special char requirement)
✅ Message sanitization (strip HTML, 1000 char limit)
✅ Network security config (HTTPS only) — `res/xml/network_security_config.xml`
✅ ProGuard in release builds
✅ Report user system → Firestore + EmailJS email notification
✅ Rate limiting in Firestore rules
✅ Screenshot prevention in ChatActivity and ProfileActivity
✅ Optional SMS MFA (MfaEnrollmentActivity, MfaSignInActivity)
✅ Terms of Service acceptance flow (ThemeManager + TermsDialogHelper)
❌ App Check — Firebase Console Terms of Service acceptance failed, skip for now

## EmailJS Config (in EmailService.kt)
- Non-browser API access: ENABLED in EmailJS dashboard
- Strict mode: OFF (or Private Key added)
- Sends email on user report with: reportedName, reportedUid, reporterUid, reason, timestamp

## ThemeManager — 4 Presets
Light, Dark, Forest, Sunset. 8 customizable colors: primary, secondary, background, surface, textPrimary, textSecondary, bubbleSent, bubbleReceived.

---

## Legal Documents (generated, need bracket fields filled)
- `Skills_United_Terms_of_Service_v2.docx` — liability-focused ToS, Australian law, Section 230-style neutral intermediary language
- `Skills_United_Privacy_Policy.docx` — Privacy Act 1988 (Cth) compliant, covers Firebase data storage, OAIC complaint process

---

## Working Features
✅ Login/logout with MFA support
✅ Register with unique username and strong password validation
✅ Terms of Service / Privacy Policy acceptance flow on login and registration
✅ Browse users with multi-tag space-separated search
✅ User profiles with skills (add/edit/remove)
✅ Match request system (Request Chat → Accept/Decline → Open Chat)
✅ Chat requests screen with Incoming and Outgoing tabs
✅ Real-time chat with message sanitization
✅ In-app message alerts (banner on every screen)
✅ Match request banner notifications
✅ Red badge on inbox icon for unread messages
✅ Recent messages inbox with bold unread (Firestore lastRead timestamps)
✅ Generated avatars
✅ Full theme system with color wheel picker
✅ All screens theme-aware including dark mode
✅ Status bar color follows theme
✅ Settings + inbox icons on every screen
✅ Keyboard/navigation bar handling (Android 13/15/16)
✅ Optional SMS MFA
✅ Report user with EmailJS email notifications
✅ userChats migration for existing users
✅ ProGuard obfuscation in release builds
✅ Network security config (HTTPS only)

---

## Known Pending Issues
- Match request banner notification not yet confirmed working end-to-end (Firestore permission errors being resolved)
- App Check skipped (Firebase Console ToS acceptance error)
- Terms/Privacy Policy URLs in TermsDialogHelper are placeholders (`[YOUR_WEBSITE]`) — need real hosted URLs
- Legal documents need bracket fields filled before publishing
