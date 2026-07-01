# Old Source Asset and File Inventory

Updated: 2026-07-01T16:18:08

Old source truth directory: `/home/vitus/floently-old-latest-source-truth-m18x6/files`

Purpose: inventory the large old source zip/tar truth before importing or changing native resources. This is docs-only and does not change Kotlin app code.

## Counts

- Old assets found: 30
- Important old asset hits: 29
- Old source screen/component hits: 37
- Theme/token hits: 7
- Navigation/drawer hits: 17
- i18n/translation hits: 3
- Access/account/billing/auth hits: 22
- Native Android resource files currently present: 2

## Old asset extension summary

| Extension | Count |
|---|---:|
| `.json` | 1 |
| `.png` | 19 |
| `.svg` | 3 |
| `.wav` | 7 |

## Important old asset candidates

| Old path | Size | Import decision |
|---|---:|---|
| `apps/client/components/public/google/android/continue.png` | 9414 | audit before import |
| `apps/client/components/public/google/android/continue.svg` | 19444 | audit before import |
| `apps/client/components/public/google/android/continue_1x.png` | 2282 | audit before import |
| `apps/client/components/public/google/android/continue_2x.png` | 4675 | audit before import |
| `apps/client/components/public/google/android/continue_3x.png` | 7104 | audit before import |
| `apps/client/components/public/google/android/signin.png` | 9147 | audit before import |
| `apps/client/components/public/google/android/signin_1x.png` | 2182 | audit before import |
| `apps/client/components/public/google/android/signin_2x.png` | 4526 | audit before import |
| `apps/client/components/public/google/android/signin_3x.png` | 6810 | audit before import |
| `apps/client/components/public/google/android/signup.png` | 9348 | audit before import |
| `apps/client/components/public/google/android/signup_1x.png` | 2268 | audit before import |
| `apps/client/components/public/google/android/signup_2x.png` | 4673 | audit before import |
| `apps/client/components/public/google/android/singin.svg` | 18326 | audit before import |
| `apps/client/components/public/google/android/singup.svg` | 19493 | audit before import |
| `apps/client/components/public/google/android/singup_3x.png` | 6879 | audit before import |
| `apps/client/components/public/google/iOS/continue.png` | 9585 | audit before import |
| `apps/client/components/public/google/iOS/signin.png` | 9253 | audit before import |
| `apps/client/components/public/google/iOS/signup.png` | 9565 | audit before import |
| `apps/client/components/public/google/web/continue.png` | 9414 | audit before import |
| `apps/client/components/public/google/web/signin.png` | 9147 | audit before import |
| `apps/client/components/public/google/web/signup.png` | 9348 | audit before import |
| `apps/client/components/public/logo.png` | 2162686 | candidate for native drawable/mipmap |
| `apps/client/components/public/sounds/ui/error.wav` | 36780 | candidate only if UI sound policy stays enabled |
| `apps/client/components/public/sounds/ui/mic_off.wav` | 258126 | candidate only if UI sound policy stays enabled |
| `apps/client/components/public/sounds/ui/mic_on.wav` | 202830 | candidate only if UI sound policy stays enabled |
| `apps/client/components/public/sounds/ui/pop_light.wav` | 67709 | candidate only if UI sound policy stays enabled |
| `apps/client/components/public/sounds/ui/send1.wav` | 125952 | candidate only if UI sound policy stays enabled |
| `apps/client/components/public/sounds/ui/success_chime.wav` | 91115 | candidate only if UI sound policy stays enabled |
| `apps/client/components/public/sounds/ui/tap_soft.wav` | 5015 | candidate only if UI sound policy stays enabled |

## Old screen/component source hits

- `apps/client/app/progress/index.tsx`
- `apps/client/app/yki-exam/certificate.tsx`
- `apps/client/app/yki-exam/index.tsx`
- `apps/client/app/yki-exam/mock-cycle.tsx`
- `apps/client/app/yki-exam/results.tsx`
- `apps/client/app/yki-exam/review.tsx`
- `apps/client/app/yki-exam/runtime.tsx`
- `apps/client/app/yki-practice/index.tsx`
- `apps/client/config/navigation/AppShell_sidebar_sections.ts`
- `apps/client/features/cards/components/CardBanksPanel.tsx`
- `apps/client/features/cards/components/CardModeTabs.tsx`
- `apps/client/features/cards/components/CardPracticeSession.tsx`
- `apps/client/features/cards/hooks/useCardPractice.ts`
- `apps/client/features/cards/screens/CardPracticeScreen.tsx`
- `apps/client/features/speaking/components/RoleplayMicButton.tsx`
- `apps/client/features/speaking/components/RoleplayScenarioHeader.tsx`
- `apps/client/features/speaking/components/RoleplayTranscriptList.tsx`
- `apps/client/features/speaking/hooks/useRoleplayRecorder.ts`
- `apps/client/features/speaking/screens/RoleplayConversationScreen.tsx`
- `apps/client/features/speaking/services/roleplayAudio.ts`
- `apps/client/features/yki-exam/screens/YkiExamScreen.tsx`
- `apps/client/features/yki-exam/services/ykiExamService.ts`
- `apps/client/features/yki-practice/screens/YkiPracticeScreen.tsx`
- `apps/client/features/yki-practice/services/ykiPracticeService.ts`
- `apps/client/state/AppShell.tsx`
- `apps/client/state/ProgressRoute.tsx`
- `apps/client/state/SettingsRoute.tsx`
- `apps/client/state/YkiExamRoute.tsx`
- `apps/client/state/YkiPracticeRoute.tsx`
- `packages/ui/components/ProgressRing.tsx`
- `packages/ui/components/SmartHintPopup.tsx`
- `packages/ui/components/UtilityDrawer.tsx`
- `packages/ui/screens/HomeScreen.tsx`
- `packages/ui/screens/LearnScreen.tsx`
- `packages/ui/screens/ProfessionalFinnishScreen.tsx`
- `packages/ui/screens/ProgressScreen.tsx`
- `packages/ui/screens/SpeakingLabScreen.tsx`

## Old theme/token hits

- `packages/ui/theme/colors.ts`
- `packages/ui/theme/floentlyPalette.ts`
- `packages/ui/theme/index.ts`
- `packages/ui/theme/radius.ts`
- `packages/ui/theme/spacing.ts`
- `packages/ui/theme/tokens.ts`
- `packages/ui/theme/typography.ts`

## Old navigation/drawer hits

- `apps/client/config/navigation/AppShell_sidebar_sections.ts`
- `apps/client/state/AuthRoute.tsx`
- `apps/client/state/BillingRoute.tsx`
- `apps/client/state/FeatureEntryRoute.tsx`
- `apps/client/state/HelpRoute.tsx`
- `apps/client/state/HomeRoute.tsx`
- `apps/client/state/LandingRoute.tsx`
- `apps/client/state/LearningRoute.tsx`
- `apps/client/state/PlacementRoute.tsx`
- `apps/client/state/ProfessionalRoute.tsx`
- `apps/client/state/ProgressRoute.tsx`
- `apps/client/state/SettingsRoute.tsx`
- `apps/client/state/SpeakingRoute.tsx`
- `apps/client/state/YkiExamRoute.tsx`
- `apps/client/state/YkiPracticeRoute.tsx`
- `apps/client/state/navigationModel.ts`
- `packages/ui/components/UtilityDrawer.tsx`

## Old i18n/translation hits

- `apps/client/features/i18n/LanguageSelector.tsx`
- `apps/client/features/i18n/index.ts`
- `apps/client/features/i18n/languages.ts`

## Old account/access/billing/auth hits

- `apps/client/app/account-deletion.tsx`
- `apps/client/app/auth/forgot-password.tsx`
- `apps/client/app/auth/login.tsx`
- `apps/client/app/auth/register.tsx`
- `apps/client/app/auth/reset-password.tsx`
- `apps/client/app/billing/subscription.tsx`
- `apps/client/app/delete-account.tsx`
- `apps/client/app/learn/account-deletion.tsx`
- `apps/client/app/learn/delete-account.tsx`
- `apps/client/app/legal/account-deletion.tsx`
- `apps/client/features/auth/screens/AuthScreen.tsx`
- `apps/client/features/auth/screens/ForgotPasswordScreen.tsx`
- `apps/client/features/auth/screens/LoginScreen.tsx`
- `apps/client/features/auth/screens/RegisterScreen.tsx`
- `apps/client/features/auth/screens/ResetPasswordScreen.tsx`
- `apps/client/features/auth/services/useGoogleSignIn.shared.ts`
- `apps/client/features/auth/services/useGoogleSignIn.ts`
- `apps/client/features/auth/services/useGoogleSignIn.web.ts`
- `apps/client/state/AuthRoute.tsx`
- `apps/client/state/BillingRoute.tsx`
- `apps/client/state/authStore.ts`
- `apps/client/state/subscriptionStore.ts`

## Current native resource files

- `apps/android/FloentlyLearn/app/src/main/res/values/strings.xml`
- `apps/android/FloentlyLearn/app/src/main/res/values/styles.xml`

## Import rules

- Do not bulk-copy React Native, TSX, JS, or web files into Android Kotlin.
- Only import assets after deciding target Android resource type: `drawable`, `mipmap`, `raw`, or no import.
- Keep Read/Create assets separate from Learn unless the native suite shell explicitly needs product logos.
- UI sounds must remain optional and should not be imported until Android sound policy is confirmed.
- Logo imports need density and adaptive icon review before release.
- Any imported asset must be followed by debug and release builds.

## Recommended next work

1. M18-O3: locate and document exact reusable drawer old source behavior, then patch native drawer only if safe.
2. M18-O4: Settings/Account clickable external links and account deletion boundary.
3. M18-O5: native debug APK install/smoke-test script and visual checklist.
4. M19: device testing and screenshot comparison.

