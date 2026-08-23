# SPDD Analysis: Finish and Restart a Game

## Original Business Requirement

## STORY-001-003 Finish and Restart a Game

### Background

Players need a clear outcome when their snake can no longer continue and a fast way to try again. Boundary and self-collisions provide the challenge of the game, while a predictable restart prevents a finished session from becoming a dead end.

### Business Value

- Make the rules and consequences of risky movement understandable.
- Preserve the player's final result so the session feels complete.
- Reduce friction between one attempt and the next attempt.

### Dependencies and Assumptions

- **Parent epic**: [Epic-1 Snake Game](Epic-1-snake-game.md)
- **Prerequisites**: An active session can move and grow the snake as described by [STORY-001-001](Story-001-001-start-and-control-the-snake.md) and [STORY-001-002](Story-001-002-eat-food-and-build-score.md).
- **Data assumptions**: A collision is evaluated when the snake's head enters a board boundary or a cell occupied by its body.
- **Integration points**: The game view presents the outcome and a restart action without requiring an external service.
- **Business constraints**: A game-over session cannot continue moving or collect additional food until it is restarted.

### Scope In

- Detecting a collision with the board boundary.
- Detecting a collision with the snake's own body.
- Showing the final score and restarting with a clean initial session.

### Scope Out

- Best-score retention between sessions.
- Pause and resume behavior.
- Lives, continues, undo, or recovery after a collision.

### Acceptance Criteria

#### AC1: Reaching the boundary ends the session

**Given** the snake is moving toward the right edge of the board
**When** its head attempts to move beyond the last available cell
**Then** the session ends, the snake stops moving, and a clear game-over state is shown.

#### AC2: Hitting the snake's body ends the session

**Given** the snake has at least four segments and its head is directed toward an occupied body cell
**When** the head enters that cell
**Then** the session ends, the snake stops moving, and a clear game-over state is shown.

#### AC3: The final score remains visible

**Given** the player reaches a collision with a current score of 30
**When** the game-over state appears
**Then** the player can see the final score of 30 and can identify how to start another game.

#### AC4: Restart creates a fresh session

**Given** a game-over state is displayed with a final score of 30
**When** the player chooses restart
**Then** the board shows a new three-segment snake, the current score is 0, and exactly one food item is placed on an unoccupied cell.

#### AC5: A finished session cannot be changed by gameplay input

**Given** the game-over state is displayed
**When** the player presses a direction or uses a directional control
**Then** the snake does not move, the score does not increase, and the game-over state remains until restart is chosen.

### Non-Functional Expectations

- The cause of the outcome is understandable from the game-over presentation without reading documentation.
- Restart is available without leaving the game view or reopening the application.

## Domain Concept Identification

#### Existing Concepts (from codebase)

- **Game session and lifecycle**: `GameState` is the immutable session snapshot containing `status`, board, snake, direction intent, score, and food (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/GameState.kt:3-16`). `SessionStatus` currently distinguishes only `READY` and `ACTIVE` (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/SessionStatus.kt:3-6`), so it provides the lifecycle foundation but not a terminal state.
- **Bounded board**: `Board` validates positive dimensions and owns the in-bounds coordinate concept through `contains` (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/Board.kt:3-13`). The established default session uses a `20 x 20` board (`composeApp/src/commonMain/kotlin/com/example/snake/game/rules/GameRules.kt:16-18`), making the board the boundary authority for AC1.
- **Snake and body occupancy**: `Snake` stores an ordered, head-first segment list and exposes the current head plus ordinary movement and growth operations (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/Snake.kt:3-17`). The existing model can represent the four-or-more-segment bodies needed for AC2, although the rules do not yet treat occupied body cells as collision targets.
- **Direction intent and input surfaces**: `Direction`, `GameRules.requestDirection`, `KeyboardDirectionMapper`, and the shared touch controls normalize keyboard and directional-control input into the same direction concept (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/Direction.kt:3-20`, `composeApp/src/commonMain/kotlin/com/example/snake/game/rules/GameRules.kt:48-69`, `composeApp/src/commonMain/kotlin/com/example/snake/game/ui/GameScreen.kt:204-247`). Direction requests already return an inactive result when the session is not active.
- **Logical movement transition**: `GameRules.advance` calculates one-cell movement and returns a typed `StepTransition`; it currently returns `BOUNDARY_BLOCKED` and leaves the state unchanged when the next cell is outside the board (`composeApp/src/commonMain/kotlin/com/example/snake/game/rules/GameRules.kt:71-111`, `composeApp/src/commonMain/kotlin/com/example/snake/game/rules/StepOutcome.kt:3-9`). This is the existing progression seam that must gain terminal collision outcomes.
- **Food and current score**: `GameState` already enforces an in-bounds food cell outside the snake and a non-negative score (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/GameState.kt:12-16`). `GameScreen` renders the score and the board's single food marker from the same state (`composeApp/src/commonMain/kotlin/com/example/snake/game/ui/GameScreen.kt:100-104`, `152-200`), so final-score retention can build on an existing presentation contract.
- **Session controller and movement clock**: `GameController` owns the mutable `StateFlow`, delegates actions to `GameRules`, and advances the game from an injected movement clock (`composeApp/src/commonMain/kotlin/com/example/snake/game/controller/GameController.kt:22-86`). It already replaces state through one update path and cancels the clock on close, but it has no terminal-session clock behavior.
- **Game view and fresh-session action**: `SnakeApp` observes the controller and supplies `startNewGame` and direction callbacks to `GameScreen` (`composeApp/src/commonMain/kotlin/com/example/snake/game/ui/SnakeApp.kt:14-32`). `GameScreen` currently branches between `READY` and an assumed active state, showing `Start New Game` only in the ready branch (`composeApp/src/commonMain/kotlin/com/example/snake/game/ui/GameScreen.kt:107-147`); there is no game-over or restart presentation.

#### New Concepts Required

- **Finished game session**: A terminal lifecycle state that preserves the result of a boundary or self-collision and prevents further movement, collection, or gameplay input until a new session begins.
- **Collision outcome and cause**: A domain-level distinction between reaching the boundary and entering the snake's body, so the game view can explain why the session ended rather than presenting an undifferentiated stop.
- **Game-over presentation**: A same-view outcome surface that keeps the final score visible, communicates the collision cause, and exposes a restart action without reopening the application.
- **Restart/fresh-session transition**: A player action that replaces the finished snapshot with the established clean initial session, including the three-segment snake, score `0`, and one valid food item.

#### Conceptual Relationships

- A game session owns the bounded board, ordered snake, current score, food target, and lifecycle. A successful movement keeps the session active; a boundary or body collision moves that lifecycle to finished while retaining the final session snapshot.
- The collision cause is produced by shared gameplay rules, carried through the controller's observable state, and translated by the game view into understandable game-over text and controls. The presentation must not infer collisions from pixels or from a stopped clock.
- A finished session is an absorbing state for the movement clock and all direction sources. Restart is the only transition out of it and creates a new session rather than mutating the old score or body incrementally.
- The fresh session continues to use the existing food invariant: exactly one food cell is in bounds and outside the new snake. Best-score retention, persistence, and external services remain outside the lifecycle.

#### Key Business Rules

- In an active session, an attempted next head outside the board ends the session with a boundary cause; the board must not wrap.
- In an active session, an attempted next head in an occupied body cell ends the session with a self-collision cause. The body-occupancy policy, including treatment of a tail that might otherwise move away, must be explicit rather than assumed.
- A collision is terminal: later clock ticks, direction requests, and directional controls cannot move the snake, change the score, collect food, or replace the game-over state.
- The terminal snapshot preserves the current score, including a value such as `30`, and exposes enough cause information for the player to understand the outcome without documentation.
- Restart creates a fresh state using the established initial-session contract: a three-segment snake, score `0`, one in-bounds unoccupied food cell, and no carry-over of the prior session's score or growth.
- Restart must be available within the existing game view and must start gameplay again without creating a second competing movement loop.
- Boundary and self-collision behavior are added without conflating the existing non-terminal `FOOD_COLLECTION_BLOCKED` capacity result with a collision game-over.
- Best-score retention, pause/resume, lives, continues, undo, recovery, accounts, networking, and persistence remain excluded.

## Strategic Approach

#### Solution Direction

- Extend the shared `commonMain` session lifecycle with an explicit finished state and a collision cause, and make the existing `GameRules` movement transition the single authority for boundary and body-collision decisions. This preserves the current Kotlin Compose Multiplatform architecture, where Android, desktop JVM, and Wasm/browser use equivalent game rules (`composeApp/build.gradle.kts:27-62`).
- Replace the current boundary-blocking outcome with a terminal collision transition while preserving the existing one-cell movement, direction buffering, food, and score semantics for non-collision steps. Add self-collision to the same transition boundary so collision detection is not duplicated in controller or UI code.
- Keep the final immutable state observable through `GameController.state`. The controller should ensure terminal sessions no longer receive meaningful clock progression, while still allowing the existing single-owner state path to publish one coherent game-over snapshot.
- Reuse the existing fresh-session initialization path for restart. The new action should replace the finished session with a newly initialized state satisfying the established three-segment, score-zero, one-food invariant, rather than resetting individual fields in the view or controller.
- Extend the shared `GameScreen` with a terminal presentation that retains the final score and shows the collision cause and restart action in the same game view. Directional input should remain routed through the existing common acceptance rules, with the terminal state making it inert on both keyboard and touch surfaces.

#### Key Design Decisions

- **Explicit terminal lifecycle versus a separate boolean**: A dedicated lifecycle concept makes active, ready, and finished behavior mutually understandable and gives every rule a consistent gate; a boolean could be added with less model change but permits contradictory combinations and leaves the UI guessing. Prefer an explicit terminal session state with an associated cause.
- **Typed collision outcomes versus exceptions or silent blocking**: Typed outcomes keep expected player actions out of exception handling and let the presentation distinguish boundary from self-collision; they add a small public domain contract that all exhaustive handling and tests must update. Prefer typed terminal results because AC1, AC2, and the non-functional cause requirement need observable semantics.
- **Shared rule transition versus controller/UI collision detection**: Detecting collisions in the view or controller might appear local, but it would duplicate coordinate semantics and allow target divergence. Keep collision decisions in the shared rules and publish the resulting terminal snapshot atomically with the existing state flow.
- **Body occupancy policy**: The story says that entering a cell occupied by the body ends the session, while some Snake variants allow a head to enter the tail's cell when that tail leaves during the same step. Prefer honoring the literal current-body wording for this story unless product explicitly chooses the tail exception; either way, document and test one policy in the shared rules.
- **Terminal clock handling versus perpetual no-op ticks**: Leaving the clock running would preserve safety if rules reject inactive states but wastes work and complicates restart races; stopping it reduces unnecessary activity but requires restart to establish exactly one new active clock. Prefer stopping or otherwise terminal-gating the clock at the controller boundary while retaining rule-level inactive no-ops as a safeguard.
- **Reuse initializer versus field-by-field reset**: Reusing the existing fresh-session initializer centralizes board, snake, score, direction, and food invariants; a manual reset could be shorter but risks carrying over score, growth, food, or pending direction. Prefer the established initializer and the existing injected randomness boundary.
- **In-place game-over presentation versus application navigation**: A same-view status preserves context, final score, and a fast retry, while a separate route or application restart adds friction and could lose the final snapshot. Prefer a clear game-over branch within `GameScreen`, with the final board/result remaining discoverable and restart prominent.

#### Alternatives Considered

- **Keep returning `BOUNDARY_BLOCKED` at the edge**: Rejected because the current no-op semantics directly conflict with AC1's required session termination and game-over presentation.
- **Infer game over from a stopped movement clock**: Rejected because a scheduler state is not a business outcome, cannot explain the cause, and does not reliably block direct or touch input.
- **Detect body collisions in Compose or platform event handlers**: Rejected because coordinate and occupancy rules would diverge across targets and could not be exercised through the existing pure transition tests.
- **Reset immediately after a collision**: Rejected because it would erase the final score and prevent AC3 from presenting a complete result.
- **Open a new application/session screen for restart**: Rejected because the requirement explicitly keeps restart in the game view and does not require navigation or an external service.
- **Use one generic “stopped” result without a cause**: Rejected because players must understand the consequence of risky movement without reading documentation, and boundary and self-collision are distinct business rules.
- **Add best-score persistence during restart**: Rejected because the story explicitly places best-score retention out of scope.

## Risk & Gap Analysis

#### Requirement Ambiguities

- **Game-over wording and visual treatment are not specified**: The requirement requires a clear state and understandable cause but does not define the exact labels, iconography, overlay, or accessibility wording for boundary versus body collision.
- **Final board position is unspecified**: It is unclear whether the rendered snake remains at its last safe position, shows the attempted collision cell, or uses another visual treatment when the terminal transition occurs.
- **Tail-cell collision semantics are unspecified**: “A cell occupied by its body” can mean all segments in the pre-step state or can allow the traditional moving-tail exception; AC2 should be explicit before detailed design.
- **Restart action details are unspecified**: The required action label, whether it is shown only after game over, and how repeated activation or an activation racing with a final tick should behave are not defined.
- **Food visibility in the terminal state is unspecified**: The story requires one food item after restart but does not say whether the last food remains visible, is hidden, or is visually de-emphasized while showing the final result.
- **Terminal clock behavior is not user-visible but is architecturally open**: The requirement says the snake stops, but it does not state whether the movement task must be cancelled or may continue producing safe no-op ticks.
- **Input feedback after game over is unspecified**: AC5 requires no gameplay change, but it does not say whether rejected keyboard/touch input should receive feedback or whether controls should be hidden, disabled, or left visibly inert.
- **Restart baseline is referred to indirectly**: AC4 names a three-segment snake, score `0`, and one food item but does not repeat the established `20 x 20`, centered, right-moving initial arrangement from the preceding stories; the existing project should inherit that contract unless product intends a different reset layout.

#### Edge Cases

- **Attempts at every boundary**: The same terminal rule must apply at the left, top, and bottom edges as well as the right edge, without wrapping or changing the pending direction before the terminal snapshot is shown.
- **Self-collision at different body positions**: A head may target the segment immediately behind it, an interior segment, or the tail after several growth steps; each case depends on the chosen occupancy policy and should not be left to list-order accidents.
- **Collision after one or more food collections**: A score of `10`, `20`, or `30` and a longer snake must be retained exactly when the next movement collides; the terminal transition must not award another point or lose prior growth.
- **Input racing with the collision tick**: A direction request arriving near the same logical tick as a collision must be applied either before that step or not at all, and no request may reopen or mutate the finished session.
- **Controls used repeatedly after game over**: Keyboard presses, touch taps, repeated reversal attempts, and simultaneous controls must leave the snake, score, cause, and terminal status unchanged.
- **Restart racing with stale ticks**: A tick belonging to the prior finished session must not move the newly initialized snake or publish a second transition; restart must leave one active movement schedule.
- **Restart after either cause**: Boundary and self-collision must both reset score, length, direction intent, and food placement identically according to the fresh-session contract.
- **Near-capacity food state**: The existing `FOOD_COLLECTION_BLOCKED` behavior can leave an active state unchanged when no replacement cell exists; this capacity outcome must not be accidentally converted into collision game-over or make the restart path invalid.
- **Lifecycle disposal around game over**: Closing the controller or leaving a platform view near a collision must not expose a stale clock update or leak a job into the restarted session.
- **Target differences**: Desktop keyboard, Android touch, and browser hybrid input must all show the same terminal result and preserve the same input blocking even though their launch and event plumbing differs.

#### Technical Risks

- **Existing boundary behavior conflicts with AC1**: `GameRules.advance` currently returns `BOUNDARY_BLOCKED` and the unchanged active state (`composeApp/src/commonMain/kotlin/com/example/snake/game/rules/GameRules.kt:82-85`), so simply adding a UI message would leave the session movable and violate the business rule. The boundary outcome must become a shared terminal transition.
- **No body-collision check exists**: The current progression checks board containment and food before ordinary movement but never checks `snake.segments` (`composeApp/src/commonMain/kotlin/com/example/snake/game/rules/GameRules.kt:76-111`). Collision detection must be ordered consistently with the existing snake movement and growth concepts.
- **Lifecycle expansion can create inconsistent gates**: Adding a finished status affects `requestDirection`, `advance`, clock startup, focus behavior, and the `GameScreen` branch that currently treats every non-`READY` state as active (`composeApp/src/commonMain/kotlin/com/example/snake/game/ui/GameScreen.kt:107-147`). All consumers need the same terminal semantics.
- **Clock/restart race and duplicate-job risk**: `GameController` prevents a second active clock but currently has no terminal restart contract (`composeApp/src/commonMain/kotlin/com/example/snake/game/controller/GameController.kt:38-43`, `79-86`). Cancelling, restarting, and publishing state must preserve one logical owner and prevent stale ticks from affecting the new session.
- **Cause propagation may be lost**: `StepTransition` currently carries only a state and `StepOutcome` (`composeApp/src/commonMain/kotlin/com/example/snake/game/rules/StepTransition.kt:3-8`), while `GameScreen` receives only `GameState` (`composeApp/src/commonMain/kotlin/com/example/snake/game/ui/GameScreen.kt:49-56`). The design must make the cause available to the presentation without deriving it from incidental movement state.
- **Final-score presentation is not yet terminal-aware**: The score renderer already reads the current state (`composeApp/src/commonMain/kotlin/com/example/snake/game/ui/GameScreen.kt:100-104`), but the screen has no game-over branch or restart callback. A partial UI change could accidentally hide the score or leave active controls visible.
- **State invariants must survive terminal and restart transitions**: Every published state must keep the food inside the board and outside the snake, retain a non-negative score, and avoid partial updates. Restart should use the same invariant-preserving construction as initial start.
- **Validation coverage is incomplete for this story**: Existing common tests cover movement, food, capacity blocking, and non-wrapping boundary no-ops (`composeApp/src/commonTest/kotlin/com/example/snake/game/GameRulesTest.kt:105-120`, `213-222`, `318-330`), but no tests demonstrate terminal causes, body hits, final-score retention, restart, or input immutability after game over.
- **Accessibility and cross-target clarity risk**: The current screen provides score and board semantics but no terminal cause or restart semantics (`composeApp/src/commonMain/kotlin/com/example/snake/game/ui/GameScreen.kt:152-160`). If cause is communicated only through color or if the restart action is unavailable to keyboard/touch users, the non-functional expectations will fail.

#### Acceptance Criteria Coverage

| AC# | Description | Addressable? | Gaps/Notes |
|-----|-------------|--------------|------------|
| AC1 | A head attempt beyond the board ends the session, stops movement, and shows a clear game-over state. | Partial | The shared approach addresses it, but the current rule returns an active `BOUNDARY_BLOCKED` no-op and the exact game-over presentation/cause wording is unspecified. |
| AC2 | A head entering an occupied body cell ends the session, stops movement, and shows a clear game-over state. | Partial | The snake model supports the required body, but collision detection is absent and the tail-occupancy policy plus final collision rendering need clarification. |
| AC3 | A collision at score `30` leaves the final score visible and identifies how to start another game. | Yes | The score is already state-driven and visible; a terminal presentation must retain that snapshot and add an understandable cause and restart affordance. |
| AC4 | Restart creates a three-segment snake, score `0`, and exactly one unoccupied food item. | Yes | The established initializer already creates the required baseline and food invariant; the missing piece is making it the terminal-state restart transition with correct clock ownership. |
| AC5 | Directional input after game over cannot change the snake, score, or terminal state until restart. | Yes | Existing inactive-input semantics provide a foundation; the finished state must use the same rule gate, and both keyboard and touch presentation paths must remain inert. |

All five acceptance criteria are addressable through the existing shared-state architecture, but AC1 and AC2 remain partially specified around presentation and body occupancy. No in-scope requirement is being deferred; the explicit out-of-scope items remain excluded from this story.