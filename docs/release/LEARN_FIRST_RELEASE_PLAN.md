# Learn-first release plan

## Decision

Floently Learn is the first release target.

The immediate priority is to build Learn to completion and polish it to match or improve the quality of the previous pre-native Learn experience. After Learn is released, Floently Read and Floently Create Studio continue toward release quality in later milestones.

## Release scope

### In scope for first release

- Floently Learn as the primary released product.
- Native login/session flow.
- Native Learn home and navigation.
- YKI practice direction.
- Professional Finnish direction.
- Roleplay with OpenAI-backed dynamic generation and anti-repetition verified against backend.
- Cards/spaced repetition direction.
- Progress/account/sign-out.
- Product access check for Learn.
- Store-ready Learn screenshots.
- Premium Learn UI polish, preserving the previous Learn product direction.

### Not release-blocking for first release

- Floently Read release-quality completion.
- Floently Create Studio release-quality completion.
- CapCut-level Create editing features.
- Read/Create public access unless they pass their own release QA.

Read and Create may remain hidden, gated, beta, or coming-soon in the first release depending on QA outcome.

## Learn completion requirements

Learn is complete for first release only when all of these pass:

- Login and session persistence work on a real device.
- Learn opens directly and reliably after access is granted.
- Learn UI matches or improves the previous Learn quality before the native switch.
- YKI flow is usable and visually polished.
- Professional Finnish flow is usable and visually polished.
- Roleplay starts sessions, sends learner messages, and returns dynamic partner/coach responses.
- Roleplay avoids broken repetition loops.
- Cards session starts, accepts ratings, and advances cleanly.
- Progress screen shows backend data or honest fallback without looking broken.
- Account/sign-out works.
- Loading, empty, error, and offline states are polished.
- No Read/Create tabs appear inside Learn.
- Debug, release, and bundle builds pass.
- Manual QA has pass/fail notes.
- Store assets and policy requirements are ready for Learn release.

## Premium polish requirements

The Learn release UI should have:

- Consistent spacing and layout rhythm.
- Premium cards and surfaces.
- Clear hierarchy for primary actions.
- Stable navigation and back behaviour.
- Good loading states.
- Good error states.
- Good empty states.
- Screenshot-quality home, practice, roleplay, cards, progress, and account screens.
- Smooth enough interactions for first public release.

## Read/Create after Learn release

After Learn release:

1. Bring Read to release quality: import/upload, language detection, generation, reader, library, payments/access, premium reader UI, QA, store assets.
2. Bring Create Studio to release quality: direct tools, project history, export/share, brand brain, premium creation UI, backend verification, QA, store assets.
3. Build advanced Create features later, including media/timeline/editor capabilities if still desired.

## Current next milestone

M16-A: Learn-first release scope lock and premium polish audit.

Goal: inspect the current native Learn implementation, compare against the previous Learn direction, identify exact polish/build gaps, then patch Learn without rebuilding it from scratch.
