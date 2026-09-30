# Read Native Browser Authentication and Recovery Decision — 2026-09-30

Status: active implementation decision  
Scope: Floently Read mobile browser on iOS/iPadOS and Android  
Supersedes: treating the React Native WebView -> Browser V2 web shell -> remote Chromium stack as the long-term mobile browser

## Incident that forces the decision

On a fresh mobile app install, the Browser V2 surface opened once. During a website sign-in, a passkey/login-key prompt appeared outside the controllable remote page surface. The user could not dismiss/select the prompt to switch to password authentication. After refreshing, closing, reopening, and refreshing again, the Browser V2 surface no longer recovered.

This is not accepted as a normal browser limitation.

## Root cause

The current release browser has too many UI/process boundaries:

1. React Native owns the application screen.
2. A native WebView hosts the Floently Browser V2 web client.
3. Browser V2 owns a remote Chromium session.
4. The remote browser framebuffer/input transport owns the visible website.
5. Authentication/passkey UI may be owned by the local OS, local WebView, remote Chromium browser chrome, or a credential provider.

Those layers cannot provide one reliable focus/input/authentication owner. A passkey is specifically a device credential ceremony; a cloud Chromium session is not the device browser and does not automatically own the user's iCloud Keychain / Credential Manager credentials. Reloading the outer WebView also does not guarantee that a stale remote Chromium worker/transport is replaced.

Changing TypeScript to another language without changing this topology would keep the same failure mode.

## Decision

### Mobile uses a local native browser surface

The default mobile browser is no longer the remote Browser V2 framebuffer.

#### iOS/iPadOS

- Swift 6 + SwiftUI for application UI.
- UIKit interoperability only where needed.
- `WKWebView` is the website renderer.
- `WKWebsiteDataStore.default()` owns persistent cookies/storage.
- WebKit navigation/UI delegates own popups, permission prompts, renderer failure, and external schemes.
- AuthenticationServices owns browser credential integration.
- Floently will request/qualify the production browser for Apple's browser credential entitlements before claiming arbitrary-site passkey support:
  - `com.apple.developer.web-browser`
  - `com.apple.developer.web-browser.public-key-credential`
- `ASWebAuthenticationSession` is used only for authentication flows that explicitly support an app callback. It is not treated as a generic way to copy arbitrary Safari cookies back into WKWebView.
- BrowserEngineKit / an alternate rendering engine is not required for this problem. WebKit already supplies the native browser/authentication boundary we need.

#### Android

- Kotlin + Jetpack Compose for application UI.
- Android WebView / AndroidX WebKit is the website renderer.
- CookieManager/WebView storage owns persistent site state.
- Credential Manager is the credential/passkey integration.
- Floently will only enable WebAuthn-for-any-origin after the production browser is approved as a privileged browser caller and can legally use the browser origin APIs. Until then, unsupported third-party passkey ceremonies must fail closed and leave password/other site sign-in choices available rather than presenting a fake or unreachable passkey UI.
- Custom Tabs are used for provider-specific authentication where the provider requires the system/user browser and where the flow has a valid callback/session handoff. They are not treated as a universal cookie bridge back into WebView.

### Web keeps web technology

The web product can continue to use TypeScript/React and Browser V2 where a remote browser is useful. This decision is specifically that the mobile app must not depend on the remote-browser-in-a-WebView topology for its primary interactive browser.

## Recovery contract

A browser Reload button must recover from a damaged renderer, not merely ask the damaged renderer to reload.

On iOS:
- explicit Reload destroys/replaces the `WKWebView`;
- the replacement uses the same persistent `WKWebsiteDataStore`;
- current URL is restored;
- WebKit content-process termination triggers the same replacement path;
- repeated renderer death is bounded and surfaced to the user.

On Android:
- explicit Reload destroys/replaces the `WebView`;
- the replacement keeps the persistent WebView cookie/storage profile;
- `onRenderProcessGone` destroys the dead renderer surface and creates a fresh one;
- repeated crash loops are bounded and surfaced to the user.

A browser close/open cycle must never depend on an orphaned remote-worker connection.

## Authentication contract

1. Authentication UI must be owned by the phone/tablet OS or the local native browser surface.
2. A credential/passkey sheet must remain tappable and dismissible.
3. Cancelling a passkey attempt must return control to the website so the user can choose password/another supported method.
4. Floently must not spoof browser identity or bypass OAuth/passkey provider security policy.
5. Reader extraction/highlighting JavaScript is never injected into protected authentication-provider pages.
6. Credentials and authenticated HTML are not proxied through Floently servers merely to make login work.

## Implementation started

This branch changes the native browsers so explicit Reload means a browser-surface replacement rather than an in-place renderer reload:

- iOS `ReadBrowserController.reload()` now replaces the WKWebView while preserving persistent website storage and current URL.
- iOS WebKit process termination uses the same replacement path.
- Android Reload now destroys/recreates WebView while preserving cookie/site storage.
- Android renderer-process death already uses a bounded recreate path and remains the crash recovery authority.

## Required production credential work

Native rendering alone fixes the split input/reload architecture, but universal third-party passkeys require browser privileges from the platform credential ecosystems.

Before Floently claims arbitrary-site passkey support:

### Apple
- qualify the production target as a browser;
- obtain/configure the browser/public-key-credential entitlements;
- verify user authorization through AuthenticationServices;
- test iCloud Keychain and at least one third-party credential manager.

### Android
- qualify the production app as a browser;
- request Google Password Manager privileged-caller approval;
- add the required browser-origin Credential Manager permissions/configuration;
- enable `WEB_AUTHENTICATION_SUPPORT_FOR_BROWSER` only after approval;
- test Google Password Manager and at least one third-party credential provider.

## Release acceptance

The mobile browser is not release-qualified until physical devices pass all of these:

- open a normal article and interact with page controls;
- sign in with ordinary username/password;
- trigger a passkey ceremony, cancel it, and choose password/another method;
- complete a passkey ceremony after browser credential privileges are enabled;
- open a popup/new-window login flow;
- cancel an authentication modal without losing browser input;
- tap Reload during/after a failed authentication and receive a working page;
- terminate the WebKit/WebView renderer and recover to the same URL;
- close/reopen the Browser screen repeatedly;
- background/foreground the app during browser use;
- preserve legitimate cookies/site storage across browser-surface replacement;
- never reattach to a dead remote browser as the only recovery mechanism.

## Migration

The existing React Native/remote Browser V2 lane remains a temporary release fallback only. New mobile browser correctness work belongs in `Floently/floently-native`.

The migration order is:

1. qualify the native iOS browser because the reported incident occurred on iPhone;
2. qualify Android native browser;
3. connect the native browser to the canonical Read document/playback pipeline;
4. ship native browser behind a release flag;
5. remove the remote Browser V2 surface as the primary mobile browser after device acceptance.

The governing principle is simple:

> Website rendering, credential UI, touch/focus, and crash recovery must have one local platform owner on mobile.
