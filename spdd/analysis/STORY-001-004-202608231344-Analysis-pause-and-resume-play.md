# SPDD Analysis: Pause and Resume Play

## Original Business Requirement

## STORY-001-004 Pause and Resume Play

### Background

Players may need to temporarily stop a session without losing progress. A visible pause state allows them to handle an interruption and return to the same board, score, and snake position.

### Business Value

- Let players pause safely during a high-score attempt.
- Prevent unintended movement and score changes while the player is away.
- Make it obvious when play is active and when it is waiting for the player to return.

### Dependencies and Assumptions

- **Parent epic**: [Epic-1 Snake Game](Epic-1-snake-game.md)
- **Prerequisites**: An active session can move, score, and show its current board state.
- **Data assumptions**: Pausing preserves the current snake position, direction, food location, and score.
- **Integration points**: Desktop players can use a visible pause control or the P/Space shortcut; touch players can use the visible pause control.
- **Business constraints**: Pause applies only to an active session; a game-over session remains finished until restart.

### Scope In

- Pausing an active session through the target-appropriate pause action.
- Freezing gameplay and retaining the current session state.
- Resuming the same session through a clearly visible action.

### Scope Out

- Saving a session for restoration after the application is closed.
- Changing movement speed or difficulty while paused.
- Multiplayer synchronization.

### Acceptance Criteria

#### AC1: An active session can be paused

**Given** an active session has a current score of 20
**When** the player chooses pause using the visible pause control or the supported shortcut
**Then** the board, snake, food, and score remain at their current values and a clear Paused state is shown.

#### AC2: Gameplay does not continue while paused

**Given** the session is paused with a score of 20
**When** enough time passes for at least three normal movement steps and the player provides directional input
**Then** the snake does not move, the food is not collected, and the score remains 20.

#### AC3: The same session can be resumed

**Given** the session is paused with the snake and food in known positions
**When** the player chooses resume
**Then** the Paused state is removed and the snake continues from the same position and direction with the score still at 20.

#### AC4: Pausing is unavailable after game over

**Given** a collision has produced a game-over state
**When** the player views the available actions
**Then** restart is offered as the next gameplay action and pause does not change the finished session.

### Non-Functional Expectations

- The paused state is visually distinct from an active session and does not obscure the player's score.
- The player can find the resume action without leaving the current game view.

## Domain Concept Identification

#### Existing Concepts (from codebase)

- **Game session lifecycle**: `GameState` is the immutable snapshot for a local play session, and `SessionStatus` already distinguishes `READY`, `ACTIVE`, and `GAME_OVER`. The lifecycle owns the board, snake, direction intent, food, score, and collision result (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/GameState.kt:3-21`, `composeApp/src/commonMain/kotlin/com/example/snake/game/model/SessionStatus.kt:3-7`).
- **Bounded board**: `Board` defines the positive grid dimensions and in-bounds cell boundary used by the session (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/Board.kt:3-13`).
- **Snake and body occupancy**: `Snake` keeps an ordered, head-first body and supports ordinary movement and growth, so its position and length are already session-owned concepts (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/Snake.kt:3-17`).
- **Direction intent and target control surface**: `Direction`, the shared direction rules, `KeyboardDirectionMapper`, and the Compose directional controls normalize desktop keyboard and touch input into the same gameplay concept (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/Direction.kt:3-20`, `composeApp/src/commonMain/kotlin/com/example/snake/game/input/KeyboardDirectionMapper.kt:5-12`, `composeApp/src/commonMain/kotlin/com/example/snake/game/ui/GameScreen.kt:244-287`). Platform entry points currently expose keyboard capability on desktop, touch capability on Android, and both on Wasm/browser (`composeApp/src/desktopMain/kotlin/com/example/snake/Main.kt:8-14`, `composeApp/src/androidMain/kotlin/com/example/snake/MainActivity.kt:9-14`, `composeApp/src/wasmJsMain/kotlin/com/example/snake/Main.kt:9-13`).
- **Logical movement step**: `GameRules` advances the snake one cell, applies the accepted direction, collects food, and produces collision outcomes. This is the shared progression boundary that must stop while paused (`composeApp/src/commonMain/kotlin/com/example/snake/game/rules/GameRules.kt:73-125`).
- **Food and current score**: `GameState` enforces a non-negative score and one in-bounds food cell outside the snake; the current Compose view renders both from the same observed snapshot (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/GameState.kt:13-20`, `composeApp/src/commonMain/kotlin/com/example/snake/game/ui/GameScreen.kt:100-110`, `191-241`).
- **Session controller and movement clock**: `GameController` owns the observable state, delegates gameplay transitions, and runs an injected `MovementClock` at the established normal interval. It already cancels movement on game over, restart, and disposal, and uses a generation boundary to reject stale ticks (`composeApp/src/commonMain/kotlin/com/example/snake/game/controller/GameController.kt:22-118`, `composeApp/src/commonMain/kotlin/com/example/snake/game/controller/MovementClock.kt:9-21`).
- **Player-facing game view and terminal actions**: `SnakeApp` connects the controller to `GameScreen`; the screen has separate ready, active, and game-over branches, keeps the final score visible, and offers restart in the same view (`composeApp/src/commonMain/kotlin/com/example/snake/game/ui/SnakeApp.kt:14-34`, `composeApp/src/commonMain/kotlin/com/example/snake/game/ui/GameScreen.kt:114-187`).

#### New Concepts Required

- **Paused session lifecycle**: A non-terminal session state between active play and resumption that retains the exact board, snake, food, score, and direction state without allowing gameplay progression.
- **Pause/resume interaction**: A target-appropriate player action that transitions an active session to paused and returns that same session to active, without creating a new attempt or resetting progress.
- **Paused-state presentation**: A visible, accessible game-view state that distinguishes paused play from active play, keeps the score and board context available, and exposes resume without navigation.

#### Conceptual Relationships

- A game session owns one bounded board, one snake, one food target, the current score, direction intent, and lifecycle. Pausing changes only the lifecycle; it does not create a second session or a separate copy of gameplay data.
- The lifecycle should allow `ACTIVE` to become `PAUSED` through a pause action and `PAUSED` to become `ACTIVE` through resume. While paused, movement steps, food collection, scoring, and new directional intent do not change the preserved snapshot.
- The movement clock is an execution mechanism for the session, not the definition of the business state. The player-facing paused state must be derived from the session lifecycle rather than inferred only from whether a clock happens to be running.
- The game view observes the lifecycle and keeps the same board and score visible. Keyboard and touch pause actions are alternative expressions of one shared pause intent, while the visible resume affordance remains available in the current view.
- `GAME_OVER` remains a separate absorbing lifecycle state. Pause must not reopen or alter it; restart continues to be the only action that creates a fresh session from the terminal state.

#### Key Business Rules

- Only an `ACTIVE` session can be paused; ready and finished sessions must not enter the paused lifecycle.
- Pausing preserves the complete current gameplay snapshot: snake position and length, food location, current score, current direction, and any already accepted next-direction state.
- Once paused, normal movement steps do not change the snake, collect food, replace food, or change the score, regardless of elapsed time.
- Directional input supplied while paused is ignored rather than queued as a hidden gameplay change, so the resumed session retains the same direction state described by the requirement.
- Resume returns the same session to active play; it does not reset the board, snake, food, score, or direction, and it does not apply elapsed-time catch-up steps.
- Pause and resume actions are idempotent outside their valid lifecycle transitions: repeated pause does not replace the snapshot, and resume is unavailable or inert unless the session is paused.
- A game-over session remains finished when pause is requested, and the game-over presentation continues to offer restart rather than resume or pause.
- Pause behavior, movement freezing, input handling, and state preservation are shared rules across Android, desktop JVM, and Wasm/browser targets.
- The visible paused state must remain distinct from active play while keeping the current score legible and the resume action discoverable in the same game view.
- Session restoration after application closure, difficulty or speed changes, and multiplayer synchronization remain outside this story.

## Strategic Approach

#### Solution Direction

- Extend the existing shared session lifecycle with an explicit paused state rather than treating a stopped movement task as the state itself. Reuse the immutable `GameState` snapshot and the established `SessionStatus`/`GameRules` boundaries so pause and resume have the same meaning on every supported target.
- Keep lifecycle transitions and pause eligibility in the shared gameplay boundary, while letting `GameController` coordinate the movement clock and publish one coherent state snapshot. The controller should stop active progression when paused and establish one active progression schedule when resumed, with the existing stale-generation protection remaining a safeguard.
- Normalize the visible pause control and the keyboard P/Space shortcuts into one pause intent. Keep directional input on its existing common path, where the paused lifecycle makes it inert, instead of implementing separate desktop, touch, and browser pause rules.
- Add a paused branch to the existing game-view lifecycle presentation. It should preserve the board and score context, make the paused status understandable without relying on color alone, and expose resume without navigating away; exact visual treatment can follow the current Material and accessibility conventions.
- Deliberately do not add application-close persistence or any change to movement speed. The story is an in-session lifecycle capability, so the preserved snapshot lives only for the current application session.

#### Key Design Decisions

- **Explicit paused lifecycle versus a pause boolean**: A dedicated lifecycle state keeps active, paused, ready, and finished behavior mutually exclusive and forces every state consumer to handle pause deliberately; a boolean would require additional combinations and could leave the UI or clock inconsistent. Prefer an explicit paused lifecycle.
- **Clock cancellation versus continued no-op ticking**: Leaving the clock running and rejecting every tick is simpler at the scheduler boundary but wastes work and leaves pause behavior dependent on a hidden task; stopping progression reduces unnecessary activity but requires resume to re-establish exactly one schedule. Prefer controller-owned cancellation or suspension with idempotent resume and a rule-level inactive safeguard.
- **Ignoring versus queuing directional input while paused**: Queuing a turn can feel convenient but changes a hidden part of the session and may make resume immediately diverge from the preserved direction; ignoring it directly supports the unchanged-session contract. Prefer ignoring new directional input while preserving any intent already accepted before pause.
- **Shared pause transition versus UI-only pause**: A UI-only overlay could hide controls but cannot guarantee that the clock, direct controller calls, or scoring rules stop consistently across targets. Put the lifecycle meaning in the shared session boundary and keep the view responsible for presentation.
- **Visible control plus shortcut versus shortcut-only pause**: A shortcut-only design is efficient for desktop players but undiscoverable for touch users and does not satisfy the target-appropriate visible affordance. Prefer a visible in-view control on supported targets, with P and Space as additional keyboard shortcuts where keyboard input is available.
- **Preserved snapshot versus reconstructed resume state**: Rebuilding fields on resume risks losing a pending direction, food placement, score, or growth; retaining the same immutable session snapshot gives pause/resume atomicity and aligns with the no-persistence scope. Prefer snapshot preservation and a normal active transition on resume.

#### Alternatives Considered

- **Infer pause from the absence of movement ticks**: Rejected because scheduler state cannot communicate a business-visible Paused status, cannot reliably govern direct input, and can be indistinguishable from disposal or game over.
- **Use platform-specific pause implementations**: Rejected because desktop, Android, and browser behavior could diverge and would undermine the epic's cross-target consistency requirement.
- **Queue all inputs received while paused**: Rejected because it silently changes the future session and conflicts with preserving the same direction and gameplay snapshot; a later story could define buffered input if product needs it.
- **Navigate to a separate pause screen**: Rejected because the player must resume from the current game view and the board/score context should remain immediately visible.
- **Persist paused sessions across application closure**: Rejected because restoration after close is explicitly out of scope; it would introduce storage and lifecycle semantics not needed for this story.

## Risk & Gap Analysis

#### Requirement Ambiguities

- **Paused-state presentation**: The requirement says the state must be clear and visually distinct but does not define the exact text, overlay treatment, contrast, or accessibility announcement. The design should communicate Paused without obscuring the score or board context.
- **Pause control placement and labeling**: It is not explicit whether the visible pause control must be present on desktop in addition to P/Space, whether the same control toggles to resume, or what labels and semantics are required. A visible action on every target is the safest interpretation.
- **Shortcut details**: “P/Space shortcut” does not specify whether both keys are required, how key repeat is handled, or how Space interacts with browser scrolling and focus. The supported keyboard mapping and event-consumption behavior need one consistent product contract.
- **Directional input during pause**: AC2 requires directional input not to produce movement or scoring, while AC3 requires continuation from the same direction. It should be explicit that new direction requests are discarded and that an already accepted but not yet applied direction is preserved as part of the snapshot.
- **Pause/resume and movement ordering**: The requirement does not define whether a pause arriving at the same instant as a movement, food collection, or collision is applied before or after that logical step. The transition must be serialized so the published snapshot is never partial.
- **Resume timing**: It is not stated whether the first step after resume waits for a full normal interval or occurs immediately, nor whether elapsed paused time is ignored. The recommended behavior is no catch-up and normal cadence from resume.
- **Actions outside active play**: The business constraint excludes pausing non-active sessions, but the behavior and visibility in `READY`, repeated pause/resume requests, and a request to start a new game while paused are not explicitly stated.
- **Application lifecycle**: The scope excludes restoring after closure but does not say whether minimizing, backgrounding, rotating, or losing browser visibility should automatically pause. Automatic lifecycle pause should not be invented without a product decision.

#### Edge Cases

- **Pause before the first movement step**: The session must remain at its initial position and score, and resuming must not skip or duplicate the first normal step.
- **Pause after a food collection**: The grown snake, replacement food, and updated score must remain visible and unchanged while paused.
- **Pause with an accepted pending turn**: The pending direction must either be explicitly preserved or cleared by product decision; preserving the complete snapshot avoids losing a valid input already accepted before pausing.
- **Pause exactly on a clock tick**: A tick that races with pause must produce one ordered outcome, never a partial combination such as a moved snake with an old score or a post-pause collection.
- **Repeated or conflicting actions**: Repeated P/Space presses, repeated touch taps, pause while already paused, and resume while active must not create duplicate clocks or alter the snapshot unexpectedly.
- **Directional input while paused**: Arrow/WASD input, touch directions, reversals, rapid sequences, and simultaneous controls must leave position, score, food, and preserved direction state unchanged.
- **Boundary or self-collision immediately after resume**: The session may become game over on the first resumed step, but pause must not prevent the existing collision cause and final score from being retained.
- **Pause requested in ready or game-over state**: The request must not change lifecycle, collision cause, score, board, or restart availability; the game-over state must remain terminal until restart.
- **Stale ticks after pause or resume**: A cancelled or delayed tick from the prior active period must not move the paused session or move the newly resumed session twice.
- **Target transitions and view disposal**: A keyboard-capable browser, touch device, window resize, rotation, or disposal near a pause action must keep the state coherent without restoring data after application closure.

#### Technical Risks

- **Lifecycle exhaustiveness across shared consumers**: Adding a paused status affects the state model, rule gates, controller clock startup, keyboard focus, board sizing, score semantics, and every `GameScreen` status branch. Missing one consumer could expose active controls or hide the resume action.
- **Clock cancellation and resume races**: `GameController` already protects restart and game-over transitions with a movement job and session generation, but pause adds another stop/start boundary. Without one owner and stale-tick protection, resume could create duplicate movement loops or allow a late tick through.
- **Non-atomic state publication**: `StateFlow` drives Compose immediately. Publishing a lifecycle change separately from a snapshot or handling pause outside the shared state transition could briefly expose active-looking controls with paused data, or vice versa.
- **Input mapping and focus differences**: The current keyboard surface handles arrows and W/A/S/D only while active. P/Space must be added without allowing browser defaults, key repeats, or a lost game focus to bypass the common pause semantics; touch and keyboard capability combinations must remain consistent.
- **Responsive and accessible presentation**: The active screen already allocates space for score, board, and controls. Adding a status and pause/resume affordance must keep the score visible, preserve usable touch targets, and communicate state through text/semantics rather than color alone.
- **Cross-target rule divergence**: Putting pause behavior in platform entry points or only in Compose event handlers would allow Android, desktop, and Wasm to disagree about elapsed time, input, or resume behavior. Shared `commonMain` lifecycle rules are the mitigation.
- **Validation coverage gap**: Existing common tests cover movement, food, collisions, controller clock behavior, restart, and inactive direction handling, but no test currently exercises pause, frozen time, ignored paused input, exact snapshot restoration, shortcut mapping, or pause exclusion after game over. These scenarios need common rule/controller tests plus target-level presentation checks during detailed design.

#### Acceptance Criteria Coverage

| AC# | Description | Addressable? | Gaps/Notes |
|-----|-------------|--------------|------------|
| AC1 | An active session can be paused through the visible control or supported shortcut while board, snake, food, and score stay unchanged and Paused is shown. | Partial | The existing shared snapshot, controller, and view provide the right boundaries, but the paused lifecycle, pause actions, shortcut mapping, and exact visual/accessibility treatment are not implemented or fully specified. |
| AC2 | After time for at least three movement steps and directional input, a paused session does not move, collect food, or change score. | Yes | An explicit paused lifecycle combined with clock gating/cancellation and inactive direction handling directly addresses the criterion; the tick/action ordering contract must be made deterministic. |
| AC3 | Resume removes Paused and continues the same session from the same position and direction with score 20. | Partial | Snapshot preservation and the existing active clock provide the strategic path, but pending-direction retention and first-step timing after resume need explicit agreement and tests. |
| AC4 | After game over, restart remains the next gameplay action and pause does not change the finished session. | Yes | Existing game-over absorption, final-score presentation, restart action, and active-only direction handling establish the foundation; the new pause action must use the same terminal gate. |

All four acceptance criteria are addressable through the existing shared-state architecture. AC1 and AC3 remain partially specified around presentation, shortcut behavior, pending direction, and resume timing; no in-scope requirement is being deferred, and application-close restoration and other listed exclusions remain outside this story.