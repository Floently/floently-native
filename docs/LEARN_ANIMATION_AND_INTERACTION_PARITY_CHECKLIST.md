# Learn Animation and Interaction Parity Checklist

## Purpose

This checklist prevents hidden UX loss during the native Learn rebuild.

Learn must preserve motion, interaction quality, feedback, and user flow.

## For every old Learn screen, record

- screen name
- route/path
- source files
- main UI states
- loading state
- error state
- empty state
- success state
- disabled state
- navigation entry and exit
- animation on entry
- animation on exit
- button press feedback
- card flip/reveal behavior if present
- progress animation if present
- modal/bottom-sheet behavior if present
- tab/sidebar behavior if present
- scroll behavior
- keyboard behavior
- audio/speaking behavior if present
- haptic behavior if present
- accessibility labels and focus order
- backend API calls
- local storage/session behavior
- subscription/access behavior
- edge cases

## Native replacement requirement

For each screen or feature, the native version must be marked as one of:

- exact parity
- improved parity
- deliberately changed with written reason
- missing and blocked

A feature cannot be marked complete if animation or interaction behavior is unknown.

## No silent downgrade rule

Do not remove animations just because native rebuild is difficult.

Do not replace a rich interaction with a static screen unless the change is deliberate, documented, and approved.

## Learn high-risk areas

Pay special attention to:

- login/register flow
- Google auth
- onboarding
- product selection
- YKI practice
- YKI exam/runtime
- card practice
- answer feedback
- hints/follow-ups
- progress screens
- speaking/roleplay
- billing/subscription
- access-gated screens
- internal test/all-access behavior

## Acceptance gate

Before old Learn can be replaced:

- every old Learn screen must be listed
- every animation and interaction must be accounted for
- every missing item must have a decision
- every critical path must be tested on iOS and Android
