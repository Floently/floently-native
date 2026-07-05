# M20-R YKI Mock Exam Full Parity Rebuild Contract

Updated: 2026-07-05

Status: REQUIRED BEFORE NEXT MOCK TEST

The YKI Mock Exam is not accepted yet. It must be rebuilt as an exam-true flow from screenshots, old app logic, project plan, and YKI bank behavior.

## Core distinction

### YKI Practice

- guided practice
- can show answer correctness
- can show explanations
- can show feedback immediately
- shorter learning loop

### YKI Mock Exam

- real exam simulation
- must not reveal correct/wrong during the exam
- must not highlight the correct answer during the exam
- must not show explanations during the exam
- saves answers silently
- evaluates only after Submit exam
- uses a full section sequence
- must match old app visual structure as closely as possible

## Source of truth order

Use these sources in this order:

1. user-provided screenshots from the old app/native test
2. old app YKI Mock code and flow logic
3. project plan/progress docs
4. existing YKI bank architecture
5. current native YKI Practice code only as a helper pattern, not as behavior truth

## Required top-level flow

1. Dark YKI landing screen
2. Level selector with all three level bands:
   - A1-A2
   - B1-B2
   - C1-C2
3. Exam overview cards:
   - Exam overview
   - Exam coverage
   - Start B1-B2 YKI exam now
   - Mock cycle
   - Recording speaking
   - Conversation speaking
   - Guided YKI practice
4. Start CTA
5. Light exam summary screen
6. Start YKI exam
7. Reading section
8. Listening section
9. Writing section
10. Speaking section
11. Submit exam
12. Results overview
13. Deep evaluation and export

## Start / landing screen requirements

The first Mock Exam route must not jump directly into a simplified light exam.

It must show:

- Floently logo/header
- Home and Menu pills
- YKI EXAM chip
- title: Full YKI exam simulation for real outcomes
- subtitle matching the old app style
- level selector:
  - A1-A2
  - B1-B2 selected
  - C1-C2
- Exam overview card:
  - certified bank count
  - authority/source line
  - explanation that it uses the certified YKI task index / level-banded pool
- Exam coverage card:
  - Reading
  - Listening
  - Writing
  - Speaking
  - task counts
  - approximate minutes
- action cards:
  - Start B1-B2 YKI exam now
  - Mock cycle
  - Recording speaking
  - Conversation speaking
  - Guided YKI practice
- bottom sticky Start B1-B2 YKI exam now button when scrolled

## Exam summary screen requirements

After pressing Start B1-B2 YKI exam now, show a light summary screen before task 1.

Must include:

- Back pill
- title: YKI B1-B2 exam
- intro text: Practice exam with real-format questions at B1-B2 level. Work through each section in order.
- Exam sections card:
  1. Reading comprehension
     - 25 min
     - 5 tasks
  2. Listening comprehension
     - 20 min
     - 4 tasks
  3. Writing tasks
     - 35 min
     - 4 tasks
  4. Speaking tasks
     - 15 min
     - 4 tasks
- total line:
  - 17 tasks total
  - approx. 95 min
- Start YKI exam button

## Progress indicator requirements

The in-exam flow must show progress dots at the top.

- total dots: 17 tasks
- completed tasks become green
- current task is blue
- future tasks are pale/grey
- progress persists across section transitions
- no answer correctness is revealed through progress colors
- green means completed/submitted, not correct

## Reading section requirements

Reading must match the screenshot structure.

Required:

- 5 reading tasks
- heading format:
  - READING COMPREHENSION · TASK X OF 5
- title:
  - Reading comprehension
- time label:
  - 25 min
- instruction:
  - Read each text carefully and choose the best answer.
- passage card:
  - TEKSTI label
  - light blue/grey inner card
  - blue vertical accent line
- question below the passage
- four answer options
- selected option blue border/background
- bottom button:
  - Next question
- no correct/wrong feedback
- no explanation
- no correct-answer reveal
- selecting any option only saves the answer and enables next

## Listening section requirements

Listening must not be skipped or collapsed.

Required:

- 4 listening tasks
- section header:
  - LISTENING COMPREHENSION · TASK X OF 4
- section intro/pre-start timing before listening begins
- audio/play control from bank audio script
- timer visible
- options selectable
- selected state only
- no correct/wrong feedback
- no answer reveal
- no explanation during the exam
- saved answers evaluated only after Submit exam

Listening section must appear after Reading and before Writing unless old app code proves a different order.

## Writing section requirements

Required:

- 4 writing tasks
- heading:
  - WRITING TASKS · TASK X OF 4
- fields start empty
- no placeholder answer text
- editable multiline input
- autocorrect disabled
- save answer before next
- no evaluation during exam
- no scoring during exam
- written responses stored for final evaluation

## Speaking section requirements

Required:

- 4 speaking tasks
- heading:
  - SPEAKING TASKS · TASK X OF 4
- instruction:
  - Read each prompt, prepare for 30 seconds, then speak for 45-60 seconds.
- prompt card matching screenshot style
- timed sub-flow per speaking task:

### Speaking task timing model

1. Prompt reading time
   - 10 seconds
   - user reads the prompt
   - no recording yet

2. Response preparation
   - 30 seconds
   - user prepares answer
   - no button needed after countdown

3. Recording
   - starts automatically
   - records up to 60 seconds
   - minimum valid recording: 30 seconds
   - before 30 seconds, do not allow moving forward as completed
   - after 30 seconds, user may save/finish early
   - at 60 seconds, auto-complete

4. Recording complete state
   - show Recording complete
   - show recorded duration
   - button:
     - Next question
     - or Submit exam on final speaking task

## Speaking visual states

The screenshots must be represented as real states:

- prompt visible
- 10s prompt-reading countdown
- 30s preparation countdown
- active recording red state
- recording countdown
- minimum 30s validation
- recording complete state
- recorded seconds displayed
- final Submit exam button

## Results and evaluation requirements

The current results are too shallow and must be replaced.

Required:

- Results overview
- B1-B2 exam summary
- objective score:
  - reading objective score
  - listening objective score
- section breakdown:
  - reading
  - listening
  - writing
  - speaking
- written-response evaluation:
  - task-by-task feedback
  - grammar
  - vocabulary
  - structure
  - task completion
  - clarity
  - improvement suggestions
- spoken-response evaluation:
  - task-by-task feedback
  - fluency
  - pronunciation note
  - grammar
  - vocabulary
  - coherence
  - task completion
  - improvement suggestions
- readiness estimate:
  - not just percentage
  - clear human-readable readiness band
- next practice recommendations
- Download / Share result button
- PDF export
- Word-compatible export

## Evaluation implementation note

If full AI evaluation is not yet wired, the native app must still produce a structured deterministic evaluation from:

- selected objective answers
- written text length and basic structure
- speaking recording presence and duration
- completed sections
- task coverage

The result must not say writing/speaking are 0/0 objective correct. Writing and speaking are productive skills and need separate evaluation sections.

## Required bank counts for B1-B2 mock

The current tested screen showed this target structure:

- Reading: 5 tasks
- Listening: 4 tasks
- Writing: 4 tasks
- Speaking: 4 tasks
- Total: 17 tasks
- Approximate total: 95 minutes

Native B1-B2 Mock must match this section count.

## Acceptance guards

Before another user test, these must pass:

- all 3 level bands visible on Mock landing
- exam overview card visible
- exam coverage card visible
- start/action cards visible
- light summary screen exists before task 1
- reading count = 5
- listening count = 4
- writing count = 4
- speaking count = 4
- total tasks = 17
- no correct/wrong feedback strings in in-exam screens
- no answer explanation during exam
- listening pre-start timer exists
- speaking 10s prompt-read timer exists
- speaking 30s preparation timer exists
- speaking 60s recording timer exists
- speaking minimum 30s record requirement exists
- final result has writing evaluation
- final result has speaking evaluation
- PDF export exists
- Word export exists
- debug build passes
- release build passes

## Next coding pass

Next patch must be M20-S:

`M20-S YKI Mock full parity rebuild`

It must patch:

- `YkiMockExamBank.kt`
- `YkiMockExamExactScreen.kt`
- possibly a new `YkiMockEvaluation.kt`
- progress docs

It must not touch unrelated app sections.
