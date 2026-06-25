# Native Learn API Contract

## Goal

Native Learn rebuild must use the existing Floently backend and card bank through APIs.

The YKI/card bank remains backend source of truth.

## Product areas

Native Learn must support:

- YKI
- Professional Finnish
- Combined/bundle access
- Speaking/roleplay if available
- Progress/session history
- Subscription/access state

## Existing backend areas to inspect/use

Raw backend map found relevant route areas:

- app/routers/v1_auth.py
- app/routers/v1_subscription.py
- app/routers/v1_payment.py
- app/routers/v1_yki.py
- app/routers/yki_exam.py
- app/routers/yki_practice.py
- app/routers/professional.py
- app/routers/v1_roleplay.py
- app/routers/v1_voice.py
- app/cards/runtime/api/router.py
- app/audio/router.py

## Required native Learn API surface

Native Learn needs these clean capabilities:

### Auth

- login
- register
- Google sign-in exchange
- current session
- logout

### Access

- get normalized entitlement/access state
- get active product tier
- get selected profession if applicable
- device-limit check if active

### Card learning

- start card session
- fetch next card
- submit answer
- get hint/follow-up if available
- get session summary
- get progress/revision state

### YKI

- get YKI practice materials
- start YKI practice/exam session
- submit YKI answer
- fetch YKI result/feedback
- fetch certificate/result screen data if supported

### Professional

- list professions
- select profession/path
- get professional cards/session
- submit professional answers

### Speaking/roleplay

- list roleplay scenarios
- start roleplay session
- send user turn
- receive AI response
- store session state

## Native caching rule

Native app may cache:

- delivered card payloads
- current session state
- progress display data
- user preferences

Native app must not cache or ship full canonical card bank as source of truth.

## Migration rule

Old Expo app and native Learn can coexist using the same backend until native Learn is ready.
