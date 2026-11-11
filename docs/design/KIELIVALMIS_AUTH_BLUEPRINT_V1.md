# KieliValmis Native Authentication Blueprint — V1

**Status:** FROZEN FOR IMPLEMENTATION  
**Effective:** 2026-10-03  
**Applies to:** next-generation native KieliValmis iOS and Android clients

This blueprint implements the suite design system and the KieliValmis clean-rebuild rule. It replaces the temporary generic auth scaffold visually and behaviourally without changing backend auth authority.

## 1. Product intent

Authentication should feel like the first KieliValmis screen, not a developer utility.

The screen must be:

- visually quiet;
- strongly aligned;
- native;
- accessible;
- truthful about loading/errors;
- compatible with the existing Floently account backend.

The old Expo auth UI is a behaviour reference only.

## 2. Screen structure

```text
safe area

brand / product identity

large auth title
supporting copy

mode selector
    Sign in | Create account

form fields
    Name (create only)
    Email
    Password + reveal/hide

inline error/status region

primary action

secondary actions
    Forgot password
    Google sign-in (only when provider/config is ready)

legal/supporting copy if required

safe area
```

## 3. Container geometry

Phone horizontal inset:

- compact/standard: 20
- large phone: 24
- tablet: 32

Form max width on tablet: 560.

Top content inset after safe area: 32 minimum.

Bottom inset: 32 + safe area.

The form may vertically center only when content height allows it without hiding keyboard-critical controls.

Keyboard appearance must scroll/reflow content rather than cover the focused field or primary action.

## 4. Typography

- product/eyebrow: 12/16 or 14/20, medium/semibold
- screen title: 34/40, weight 800
- supporting copy: 16/24
- field label: 14/20, weight 600
- field input: 16/24
- inline error/status: 14/20
- primary button: 16/20, weight 600
- tertiary action: 14/20 or 16/24, weight 600

## 5. Mode selector

Use a two-option segmented surface.

Geometry:

- visual height: 44
- minimum interactive target: 48
- container radius: 14
- selected segment radius: 12
- internal padding: 4
- selected fill: surface3 / brand tint
- unselected background: surface1
- labels: 14/20, weight 600

Changing mode:

- preserves email;
- clears only errors that are no longer relevant;
- does not erase the password unless security policy requires it;
- shows/hides Name using a short standard content transition;
- updates password content type/autofill semantics.

## 6. Fields

All fields use the KieliValmis standard input geometry:

- height: 52
- radius: 14
- horizontal padding: 16
- border: 1
- label-to-field gap: 8
- field-to-field gap: 16

Email:

- email keyboard;
- no automatic capitalization;
- no autocorrection;
- correct autofill/content-type metadata.

Password:

- reveal/hide control has a 48 target;
- visual icon 20–22;
- reveal state must be announced accessibly;
- Enter/Return submits when the form is valid and not busy.

Name:

- create-account mode only;
- optional;
- correct name autofill metadata.

## 7. Primary action

- height: 52
- radius: 16
- full form width
- one primary action only
- loading state keeps the same dimensions
- duplicate submission is disabled while busy

Labels:

- Sign in
- Create account
- Please wait… only when actively submitting

## 8. Error and status behaviour

No fake success.

Inline auth failures are displayed near the action/form, not as disappearing toasts.

The UI may translate known technical failure categories into concise user-facing copy, but must preserve truthful meaning.

Examples:

- invalid credentials;
- account already exists;
- network unavailable;
- service unavailable;
- validation failure.

Unknown errors use a neutral retry-safe message and do not expose stack traces.

## 9. Forgot password

Forgot password opens a dedicated sheet/dialog flow.

Step 1:

- email field, prefilled from auth form when available;
- Send reset link button;
- truthful loading/error state.

After backend acceptance:

- show a neutral confirmation that reset instructions were requested;
- do not claim an email was delivered if the backend intentionally uses account-enumeration-safe messaging beyond that guarantee.

The reset-token confirmation screen may be implemented later with deep-link routing; the request surface is part of V1 auth.

## 10. Session restore

At app launch:

```text
secure local session?
    no -> signed out
    yes -> verify via /api/v1/auth/session
              |
              + success -> signed in
              + invalid/unauthenticated -> clear secure session -> signed out
              + retryable network failure -> preserve local secure session but show reconnect state
```

A valid stored bearer token must never be replaced by an empty token merely because the session-info endpoint does not return fresh credentials.

## 11. Access status after auth

After authentication, request `/api/v1/subscription/status`.

Authentication and entitlement are separate concepts.

A valid account may be signed in without a paid YKI/Professional entitlement.

The client must not silently turn authentication into all-access.

## 12. Accessibility

- logical focus order;
- clear labels for reveal/hide password;
- error text exposed to VoiceOver/TalkBack;
- 44 pt minimum iOS target, 48 dp Android target;
- dynamic type/font scaling without clipping;
- keyboard/IME submit action;
- reduced-motion safe.

## 13. Motion

- mode-change content transition: 160–240 ms
- sheet entry: platform-native standard
- loading: no layout jump
- no decorative continuous animation

## 14. Acceptance

V1 auth is complete when:

- real backend login/register is wired;
- secure session storage is used;
- restore is attempted on launch;
- password reveal/hide works;
- Enter/IME submit works;
- duplicate submit is prevented;
- forgot-password request uses real backend;
- entitlement status is fetched separately;
- old Expo source is untouched;
- both native implementations use the same frozen geometry.
