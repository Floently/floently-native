# KieliValmis Native Session Architecture — V1

**Status:** IMPLEMENTATION CONTRACT  
**Effective:** 2026-10-03

## 1. Purpose

KieliValmis screens must not own durable product state.

This document defines the first native state-owner layer for iOS and Android.

## 2. App-level owners

The native Learn process owns these long-lived domains:

### AuthSession

Owns:

- secure session restore;
- authenticated user;
- bearer token lifecycle;
- login/register/logout;
- account bootstrap state.

### AccessSession

Owns:

- subscription/access snapshot;
- YKI access;
- Professional access;
- accessible/selected professions;
- payment-warning state;
- refresh state.

Access is not inferred from which screen the user reached.

### LearningSession

Owns:

- `LearningSessionPlanV1`;
- current activity cursor;
- session revision;
- completion state;
- pending activity interaction state;
- current pathway.

The first implementation may hold this in memory while the durable event/outbox foundation is added. It may not fabricate server state.

### LearningEventOutbox

Owns:

- pending `LearningEventV1`;
- idempotency keys;
- local enqueue order;
- acknowledgement/removal;
- retry state;
- persistence boundary.

Events may be queued offline. Server-derived progress is not generated locally from them unless a separately governed deterministic projection exists.

### SpeechSession

Reserved owner for:

- microphone/recording lifecycle;
- upload/stream state;
- transcription/evaluation;
- audio route/interruption state;
- roleplay turn continuity.

## 3. Lifecycle

Owners live at application/root-navigation scope.

A learning screen may disappear and reappear without resetting:

- current session;
- activity cursor;
- pending outbox;
- auth/access state.

Logout clears account-scoped in-memory state and account-scoped pending data according to the final persistence policy.

## 4. Native implementation mapping

### iOS

Use `@MainActor ObservableObject` state owners.

App root creates/owns the state objects.

SwiftUI screens receive them through explicit injection/environment ownership.

### Android

Use long-lived state holders/ViewModels with Coroutines + StateFlow.

Activity/composable recreation must not silently destroy the logical session.

Durable outbox persistence will use Room when the event layer is promoted from in-memory bootstrap to offline-capable production state.

## 5. Network authority

- Auth backend owns identity/session validity.
- Subscription backend owns access.
- Learn backend owns canonical content and server-derived learner truth.
- Native state owners coordinate lifecycle and offline delivery; they do not replace backend authority.

## 6. Error states

Owners expose typed/structured state:

- idle;
- loading;
- ready;
- retryable unavailable;
- unauthenticated;
- non-retryable failure where applicable.

Screens render these states instead of inventing their own unrelated booleans.

## 7. Immediate V1 implementation

This phase implements:

1. AuthSession/App bootstrap owner;
2. access snapshot owner;
3. in-memory `LearningSessionPlanV1` holder;
4. in-memory event outbox with stable idempotency keys and replace-safe state APIs.

Durable Room/SQLite outbox persistence follows after the data contract and backend ingestion endpoint are fixed.

## 8. Non-goals

V1 does not:

- invent mastery locally;
- duplicate the canonical card bank;
- persist fake progress;
- rewrite the YKI engine in mobile code;
- introduce Rust merely for state management.
