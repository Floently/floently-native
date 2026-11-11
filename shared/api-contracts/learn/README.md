# Learn API Contracts

Native Learn must use backend APIs for:

- session/auth
- product access
- YKI deck/session
- Professional deck/session
- Combined entitlement
- card answers/progress
- speaking/roleplay when available

The card bank is not copied into the native app.


## Versioned semantic contracts

The clean KieliValmis rebuild begins its language-neutral contract family under `v1/`:

- `v1/activity-definition.schema.json`
- `v1/learning-session-plan.schema.json`
- `v1/learning-event.schema.json`

These schemas are the beginning of the semantic boundary shared by native clients, future web clients, and backend services. The UI is not the source of learner truth.
