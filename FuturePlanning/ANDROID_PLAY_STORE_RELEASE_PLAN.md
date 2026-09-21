# Android and Google Play Release Plan

## 1. Final goal

Create an Android edition of **ASHGROVE: THE LAST CURE** that can be installed
from Google Play and supports solo play and two-player online co-op.

The Google Play release should provide:

- A signed Android App Bundle (`.aab`) for Google Play.
- APK builds for local development and device testing.
- A touch-friendly mobile launcher and game interface.
- All required maps, cinematics, subtitles, audio, and fonts.
- Persistent player accounts and save data through an online backend.
- Remote two-player co-op without requiring Java, MySQL, Gradle, or Radmin VPN
  on the player's phone.
- Compatibility checks between desktop and Android builds if cross-platform
  co-op is enabled.

This is a separate platform project, not a direct conversion of the Windows
`.exe`. The existing libGDX gameplay code is reusable, but desktop-only pieces
must be replaced or isolated.

## 2. Important architecture decisions

### What can be reused

- Most platform-independent gameplay in `core`.
- Shared network messages and constants in `shared`.
- Maps, textures, cinematics, dialogue, audio, and other game content.
- The existing Spring Boot backend after it is prepared for secure public
  hosting.
- The existing co-op protocol where it does not depend on desktop-only APIs.

### What cannot run unchanged on Android

- The JavaFX launcher in `fx-launcher`.
- The LWJGL desktop launcher in `lwjgl3`.
- Windows EXE packaging and Windows Firewall scripts.
- A bundled MySQL Windows service.
- Desktop keyboard/mouse-only controls.
- File access that assumes a normal Windows project directory.

JavaFX should not be placed inside the Android application. Build a dedicated
Android launcher and recreate the launcher screens with libGDX Scene2D UI or
native Android UI. Scene2D is the recommended first choice because it keeps the
game and menu rendering model consistent across devices.

## 3. Recommended mobile networking model

Do not make one phone run Spring Boot and a database for the other phone. Phone
networks use NAT, carrier-grade NAT, changing addresses, background-process
limits, and aggressive battery management. A phone-hosted server would be
unreliable for a public Play Store game.

Use this release architecture:

```text
Android Player 1 ─┐
                  ├── HTTPS / game connection ── Hosted game services
Android Player 2 ─┘                              ├── Spring Boot backend
                                                 ├── Production database
                                                 └── Match/session service
```

The hosted services should handle:

- Registration, login, account deletion, and authentication.
- Save slots, statistics, leaderboards, and match history.
- Lobby creation and joining.
- Matchmaking or invitation codes.
- Discovery of the authoritative host/relay for the match.
- Version compatibility.
- Reconnection and disconnect cleanup.

Radmin VPN can remain an optional development method for Windows testing, but
the public Android version should not depend on it. For normal mobile users,
use a public HTTPS backend plus either:

1. A hosted authoritative/relay game server, which is the most reliable option;
   or
2. A peer-to-peer connection assisted by a rendezvous and relay service, which
   is more complex and still needs relay fallback.

The first public Android version should use hosted or relay-based networking.

## 4. Add an Android Gradle module

Add a new `android` module to the existing multi-module Gradle project.

Expected high-level structure:

```text
infected-hour/
  android/
    build.gradle.kts
    src/main/AndroidManifest.xml
    src/main/java/.../AndroidLauncher.java
    src/main/res/
    src/main/assets/       or a linked/generated assets directory
  core/
  shared/
  backend/
  fx-launcher/             desktop only
  lwjgl3/                  desktop only
```

The Android module should:

- Apply the Android application plugin.
- Depend on `core` and `shared`.
- Use the libGDX Android backend.
- Exclude JavaFX and LWJGL desktop dependencies.
- Define a stable application ID, for example
  `com.infectedhour.ashgrovethelastcure`.
- Provide a launcher extending libGDX `AndroidApplication`.
- Package only Android-compatible native libraries.
- Build separate debug and release variants.

Choose the final application ID carefully. After publishing, it permanently
identifies the app and cannot be casually changed without creating a different
Google Play listing.

## 5. Audit `core` and `shared` for Android compatibility

Before adding mobile UI, inspect every dependency used by `core` and `shared`.

- Move desktop-only code behind platform interfaces.
- Remove direct LWJGL, JavaFX, AWT, Swing, or Windows API use from shared code.
- Replace desktop file paths with libGDX internal/local file APIs.
- Replace unsupported Java APIs or configure Android desugaring where suitable.
- Keep networking and serialization compatible with Android.
- Confirm that reflection-dependent classes are preserved in release builds.
- Ensure background threads pause, resume, and stop with the Android lifecycle.
- Keep teammate-owned `level_final` content authoritative; only make minimal
  compatibility changes when the level itself requires them.

The desktop project currently targets Java 21. The Android module may need a
different source-compatibility level or core-library desugaring. Treat this as
a build concern, and do not reduce the desktop toolchain until the shared-code
audit shows what is actually required.

## 6. Replace the JavaFX launcher on mobile

Recreate the following flows as mobile-friendly screens:

- Splash/loading screen
- Login and registration
- Account deletion and privacy links
- Main menu
- Solo/online co-op selection
- Create lobby / join lobby
- Character selection
- Load game / save slots
- Settings
- Profile, collection, leaderboard, and results
- Cinematic player with subtitles and skip controls

Recommended implementation:

- Use libGDX Scene2D UI for the first Android release.
- Share view models and backend DTOs where practical.
- Keep JavaFX screens as the Windows desktop implementation.
- Avoid putting mobile layout assumptions into desktop screens.

## 7. Mobile controls and user experience

Desktop controls must be redesigned for touch.

Add:

- Left virtual movement stick.
- Aim control or right virtual stick.
- Fire/attack button.
- Interact, reload, heal, and ability buttons as needed.
- Pause button and Android Back-button behavior.
- Adjustable control size, position, opacity, and sensitivity.
- Optional controller support.
- Haptic feedback settings.
- Safe-area handling for display cutouts and gesture navigation.

Test UI scaling on:

- Small phones
- Normal phones
- Tall aspect-ratio phones
- Tablets
- Foldables and large screens where practical

Decide whether the game is landscape-only. A shooter should normally lock to
landscape and correctly handle rotation, interruption, and app resume.

## 8. Mobile gameplay adaptation

The same levels can be retained, but mobile hardware requires performance
budgets.

- Add graphics presets: Low, Medium, and High.
- Limit particle counts, dynamic lights, and simultaneous enemies on weaker
  devices when necessary.
- Use texture atlases and device-appropriate texture sizes.
- Avoid decoding multiple full-resolution cinematic images at once.
- Stream or dispose of audio and textures between scenes.
- Compress music and voice-over appropriately, preferably using Android-safe
  OGG/Vorbis settings verified on real devices.
- Cap the frame rate when useful for heat and battery control.
- Pause audio, networking, and simulation correctly when the app is backgrounded.
- Recover safely after Android destroys and recreates the activity.

Do not silently change level design only for performance. Measure first, then
make explicit mobile-quality adjustments.

## 9. Assets and Play Asset Delivery

Initially, package shared game resources as Android assets using a single
authoritative source directory so desktop and mobile do not drift.

Inventory:

- Maps
- Character and enemy textures
- Cinematic images
- Dialogue/subtitle data
- Voice-over, sound effects, and music
- Fonts and UI skins
- Shaders
- Localization files

Google Play requires new apps to publish using Android App Bundles. If the game
becomes large, use Play Asset Delivery for large game content rather than old
APK expansion files. Choose among install-time, fast-follow, and on-demand
asset packs based on which content is needed at startup.

Do not download essential executable code from an arbitrary server. Keep the
base application functional and follow Google Play's delivery policies.

## 10. Production backend deployment

The Android app must not contain MySQL credentials or connect directly to the
database.

Deploy the Spring Boot backend to a managed server or container platform:

1. Build a production backend image or executable JAR.
2. Use a managed PostgreSQL or MySQL database.
3. Put the API behind an HTTPS domain and valid TLS certificate.
4. Store database passwords and JWT secrets in the hosting provider's secret
   manager or environment configuration.
5. Run schema migrations during controlled deployments.
6. Add health checks, monitoring, structured logs, and automated backups.
7. Add rate limiting and abuse protection to authentication endpoints.
8. Configure CORS only if a web client later requires it; native Android HTTP
   clients are not governed by browser CORS.
9. Separate development, testing, and production environments.
10. Never expose the database port publicly.

Use a release configuration value for the HTTPS API base URL. Do not leave
`localhost:8080` as the Android default.

## 11. Adapt multiplayer for mobile internet

The current KryoNet ports are suitable for controlled LAN/VPN tests but public
mobile networking needs additional work.

- Create server-issued lobby IDs or invitation codes.
- Authenticate every game connection with a short-lived match token.
- Add relay/authoritative server support for users behind NAT and CGNAT.
- Encrypt traffic or tunnel gameplay through a secure transport.
- Add timeouts, reconnect support, heartbeat handling, and duplicate-session
  protection.
- Validate every client message on the server.
- Apply protocol and content version checks before entering a match.
- Make the server authoritative for important gameplay state to reduce cheating.
- Test Wi-Fi-to-mobile-data transitions and high-latency connections.

If cross-platform co-op is planned, Windows and Android must use the same
protocol version, content version, timing rules, and server validation.

## 12. Android permissions and manifest

Request only permissions the game truly needs. A typical configuration may
need:

- Internet access.
- Network-state access for connection messages.
- Vibration only if haptic feedback is provided.

Avoid storage, contacts, location, microphone, camera, phone, and other
sensitive permissions unless a confirmed game feature requires them. Modern
Android app-private storage does not require broad storage permission.

Declare:

- Landscape orientation if chosen.
- Supported OpenGL ES version.
- Required or optional hardware features.
- Application and activity lifecycle settings.
- Backup behavior based on the save/account design.

## 13. Current Google Play technical requirements

Requirements must be rechecked immediately before each release because Google
updates them regularly.

As of September 2026, plan for:

- New apps and updates targeting Android 16 / API level 36 or newer.
- A signed Android App Bundle (`.aab`) for publishing.
- Play App Signing and a securely stored upload key.
- 64-bit native support, including `arm64-v8a`.
- 16 KB memory-page compatibility for every native dependency, including
  libGDX native libraries.
- Unique and increasing `versionCode` values.
- A user-facing semantic `versionName`.

Use a current Android Gradle Plugin and Android SDK. Verify that the selected
libGDX version and every native library satisfy current 16 KB page-size rules
before creating the production bundle.

## 14. Signing and secret management

Create an upload keystore for release builds and protect it carefully.

- Never commit the keystore, passwords, or `keystore.properties` to Git.
- Keep at least two secure backups accessible to the project owners.
- Use Play App Signing for the final signing key.
- Use the upload key only to authenticate future bundle uploads.
- Store CI signing secrets in protected secret storage.
- Record the package name and signing certificate fingerprints.

Losing control of signing credentials can disrupt future updates, so ownership
must be decided before the first public release.

## 15. Player data, privacy, and account deletion

This project supports registration and player accounts. Google Play requires
apps that let users create accounts to provide a way to request deletion of the
account and associated data.

Before review:

- Add account deletion inside the app.
- Provide a functional public web page for account-deletion requests.
- Publish a privacy policy at a stable public URL.
- Link the privacy policy inside the app and Play Store listing.
- Explain collected account, save, statistics, crash, and network data.
- Implement the declared retention and deletion behavior in the backend.
- Complete the Play Console Data safety form accurately.
- Avoid collecting device identifiers or analytics unless needed and disclosed.

Also complete:

- Ads declaration
- Target audience declaration
- Content rating questionnaire
- App access instructions for reviewers if login is required
- Any required permission declarations

Provide Play reviewers with a working test account or clear instructions when
the app cannot be reviewed without authentication or a second player.

## 16. Build outputs

Development outputs:

```text
android/build/outputs/apk/debug/
  android-debug.apk
```

Play Store output:

```text
android/build/outputs/bundle/release/
  android-release.aab
```

Expected Gradle tasks after the Android module is implemented:

```powershell
.\gradlew.bat :android:assembleDebug
.\gradlew.bat :android:bundleRelease
```

The exact tasks and paths must be verified after selecting the Android Gradle
Plugin and final module configuration.

## 17. Testing strategy

### Local development

- Android Studio emulator for quick UI checks.
- At least one real low/mid-range Android phone.
- At least one modern 64-bit phone.
- Debug APK installation through Android Studio or ADB.

### Functional testing

- Fresh install, registration, login, and account deletion.
- Complete solo walkthrough.
- Every cinematic, subtitle, and voice-over file.
- Save/load and recovery after force-close.
- Background/resume during gameplay and cinematics.
- Touch controls with different hand sizes and screen ratios.
- Bluetooth controller where supported.
- Incoming notification, call interruption, and screen lock.

### Multiplayer testing

- Android-to-Android.
- Android-to-Windows if cross-platform play is enabled.
- Different Wi-Fi networks.
- Wi-Fi versus mobile data.
- High latency and packet loss.
- Temporary network loss and reconnection.
- Host/server failure and clean lobby recovery.
- Mismatched versions.

### Performance testing

- CPU, GPU, memory, and battery use.
- Thermal throttling during a long match.
- Loading time and peak memory during cinematics.
- Low-storage behavior.
- Android 15+ emulator/device configured for 16 KB memory pages.
- Native-library checks for every packaged ABI.

## 18. Google Play Console process

1. Create and verify the correct personal or organization Play Console account.
2. Pay the Play Console registration fee shown during enrollment.
3. Create the app and reserve the final package name.
4. Set the default language and declare that it is a game.
5. Enable Play App Signing.
6. Create the store listing: title, short description, full description, icon,
   feature graphic, screenshots, support email, and privacy-policy URL.
7. Complete App content, Data safety, account-deletion, ads, target-audience,
   content-rating, and app-access sections.
8. Upload the signed `.aab` to Internal testing.
9. Test the Play-delivered build on multiple real devices.
10. Move to Closed testing and collect structured feedback.
11. Fix crashes, Android vitals issues, policy warnings, and compatibility
    problems.
12. Apply for production access if the account is subject to that requirement.
13. Create a production release with release notes.
14. Use a staged rollout rather than immediately releasing to everyone.
15. Monitor crashes, ANRs, reviews, server health, and matchmaking after launch.

For personal developer accounts created after November 13, 2023, Google
currently requires a closed test with at least 12 opted-in testers continuously
for at least 14 days before applying for production access.

## 19. Store listing materials

Prepare:

- Final game title and package/application ID.
- High-resolution launcher icon.
- Feature graphic.
- Phone and tablet screenshots from the real Android build.
- Gameplay trailer hosted according to Play Console requirements.
- Short and full store descriptions.
- Support email and support/privacy website.
- Privacy policy and account-deletion page.
- Content rating answers based on violence, horror, blood, and online interaction.
- Clear disclosure that online co-op requires an internet connection.

Do not reuse Windows screenshots if they show controls or UI that the Android
version does not have.

## 20. Suggested implementation order

1. Finish and stabilize the desktop gameplay and content.
2. Decide whether Android multiplayer is Android-only or cross-platform.
3. Deploy a secure test backend and database over HTTPS.
4. Add the Android Gradle module and minimal launcher.
5. Audit `core` and `shared` for Android compatibility.
6. Launch a simple gameplay screen on a real Android device.
7. Implement touch controls and mobile UI scaling.
8. Rebuild launcher/account/lobby screens for mobile.
9. Adapt assets, audio, memory use, and lifecycle behavior.
10. Replace Radmin-dependent networking with hosted lobby and relay/server flow.
11. Add privacy, account deletion, and production security features.
12. Upgrade native dependencies and verify 64-bit/16 KB compatibility.
13. Produce signed debug/release candidates and run full device testing.
14. Create the Play Console listing and complete policy declarations.
15. Run internal and closed testing.
16. Publish through a staged production rollout.

## 21. Definition of done

The Android release is complete when:

- The game installs from Google Play without Java or any separate runtime.
- The JavaFX and LWJGL desktop launchers are not packaged in Android.
- All menus and gameplay controls work comfortably on touchscreens.
- Solo mode, cinematics, dialogue, audio, and saves work across app restarts.
- Two players can connect over ordinary internet connections without Radmin.
- Android and Windows can play together if cross-platform support is advertised.
- The backend uses HTTPS and no database credentials are inside the app.
- Account deletion, privacy policy, and Data safety behavior are implemented.
- The `.aab` meets current target API, 64-bit, and 16 KB native requirements.
- The Play-delivered build passes testing on real low-, mid-, and high-range
  devices.
- The store listing accurately represents the Android build.
- No development password, localhost backend URL, absolute Windows path,
  debug-only menu, or developer asset is included in production.

## 22. Official references to recheck before release

- [Google Play target API requirements](https://developer.android.com/google/play/requirements/target-sdk)
- [Android App Bundles](https://developer.android.com/guide/app-bundle)
- [Build and test an App Bundle](https://developer.android.com/guide/app-bundle/test)
- [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756)
- [Google Play account-deletion requirements](https://support.google.com/googleplay/android-developer/answer/13327111)
- [Prepare an app for Play review](https://support.google.com/googleplay/android-developer/answer/9859455)
- [Testing requirements for new personal accounts](https://support.google.com/googleplay/android-developer/answer/14151465)
- [Android 64-bit support](https://developer.android.com/games/optimize/64-bit)
- [Android 16 KB page-size support](https://developer.android.com/guide/practices/page-sizes)
- [Official libGDX project structure](https://libgdx.com/wiki/start/project-generation)
- [Official libGDX deployment guide](https://libgdx.com/wiki/deployment/deploying-your-application)

