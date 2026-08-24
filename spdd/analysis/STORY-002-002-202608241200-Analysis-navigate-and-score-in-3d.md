# SPDD Analysis: Navigate and Score in 3D

## Original Business Requirement

## STORY-002-002 Navigate and Score in 3D

### Background

After choosing 3D, players need to understand movement through depth and receive the same immediate reward for reaching food as they do in the 2D game. This story makes the 3D space playable and turns movement into visible snake growth and score progression.

### Business Value

- Give players a controllable 3D challenge rather than a static alternate view.
- Make depth-oriented movement understandable through responsive, target-appropriate controls.
- Preserve the familiar food, growth, and scoring loop in the new dimension.

### Dependencies and Assumptions

- **Parent epic**: [Epic-2 Optional 3D Snake Mode](Epic-2-optional-3d-snake-mode.md)
- **Prerequisites**: A player can start a 3D session as described by [STORY-002-001](Story-002-001-choose-the-3d-play-mode.md), and the existing game provides the familiar movement and scoring expectations from [Epic-1 Snake Game](Epic-1-snake-game.md).
- **Data assumptions**: A 3D session begins with a three-segment snake, one food item on an unoccupied cell, and a current score of 0.
- **Integration points**: Desktop players use the available keyboard controls, and touch players use visible controls for movement in all three dimensions.
- **Business constraints**: The snake advances one cell per movement step; an immediate reversal is invalid; each food item adds exactly one segment and 10 points; replacement food must occupy an unoccupied cell.

### Scope In

- Showing a bounded 3D play space with visible depth and a distinguishable snake and food item.
- Moving the snake one cell at a time through the three-dimensional space with valid direction changes.
- Recognizing food collection, growing the snake, increasing the current score, and placing replacement food.

### Scope Out

- The mode-selection experience before a session.
- Boundary and self-collision consequences, game-over presentation, restart, pause, resume, and best-score retention.
- Power-ups, bonus scoring, levels, obstacles, enemies, or multiple food types.

### Acceptance Criteria

#### AC1: The snake can move through all three dimensions

**Given** a 3D session is active and the snake is moving right
**When** the player chooses up, down, forward, backward, or right using the available target control
**Then** the snake changes to that valid direction at the next movement step and advances one cell in the selected direction, with movement toward or away from depth visibly distinguishable.

#### AC2: The snake continues in its last valid direction

**Given** the snake has selected a valid direction in a 3D session
**When** the player provides no further direction
**Then** the snake continues advancing one cell per movement step in its last valid direction.

#### AC3: An immediate reversal is rejected

**Given** the 3D snake is moving right
**When** the player requests left before another valid direction is selected
**Then** the request is ignored and the snake continues moving right without ending the session because of that request alone.

#### AC4: Collecting food grows the 3D snake and awards points

**Given** the 3D snake has three segments, a current score of 0, and its head is one movement step from the food
**When** the snake moves onto the food
**Then** the snake grows from three segments to four, the score becomes 10, and the collected food is no longer shown at that location.

#### AC5: Replacement food remains available

**Given** the player has collected food in a 3D session
**When** the collection is reflected on the play space
**Then** exactly one new food item appears on a cell not occupied by the snake and the player can continue steering toward it.

#### AC6: Repeated collection accumulates progression

**Given** the player has collected two food items without ending the session
**When** the second collection is completed
**Then** the snake has gained two segments from its starting length and the current score is 20.

### Non-Functional Expectations

- Snake, food, depth, current score, and the next valid movement options remain distinguishable during normal play.
- Movement and food collection provide feedback quickly enough for the player to steer during normal play.

## Domain Concept Identification

#### Existing Concepts (from codebase)
- **Session lifecycle and state**: `GameState` is the immutable snapshot with `status`, `board`, `snake`, `currentDirection`, `pendingDirection`, `score`, `food`, `collisionCause`, and `mode` (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/GameState.kt:3-13`). `SessionStatus` distinguishes READY/ACTIVE/PAUSED/GAME_OVER and `PlayMode` distinguishes TWO_D/THREE_D (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/PlayMode.kt:3-6`).
- **Board and Cell with depth**: `Board` now validates `columns`, `rows`, and `depth` (3D requires >=2) and `contains` checks all three axes (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/Board.kt:3-18`). `Cell` already has `column`, `row`, `depth` with default 0 (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/Cell.kt:3-7`).
- **Snake**: Ordered head-first segments with `moveTo` and `moveToAndGrow` (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/Snake.kt:3-17`). Already depth-aware because it stores `Cell`.
- **Direction (2D only)**: `Direction` currently models only UP/DOWN/LEFT/RIGHT with planar offsets and `opposite()` (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/Direction.kt:3-21`). No forward/backward.
- **GameRules single source of truth**: `GameRules.startNewGame` creates a centered 3-segment snake, score 0, one unoccupied food, with 3D default board 20x20x3 (`composeApp/src/commonMain/kotlin/com/example/snake/game/rules/GameRules.kt:18-60`). `requestDirection` and `advance` currently short-circuit 3D to `IGNORED_UNSUPPORTED_MODE` / `UNSUPPORTED_MODE` (`composeApp/src/commonMain/kotlin/com/example/snake/game/rules/GameRules.kt:84-86`, `111-113`). The 2D logic includes pendingDirection gating, reversal rejection, and food growth + replacement.
- **GameController session management**: Holds `selectedMode` and `state` as StateFlows, generation-guarded movement clock, lifecycle gating for selectMode/startNewGame/pause/resume (`composeApp/src/commonMain/kotlin/com/example/snake/game/controller/GameController.kt:37-147`). `startClock` is currently disabled for 3D (`composeApp/src/commonMain/kotlin/com/example/snake/game/controller/GameController.kt:137`).
- **UI presentation**: `GameScreen` renders `ThreeDSnakeBoard` layered depth view and `ThreeDControls` (currently disabled buttons with label "Unavailable until 3D movement is implemented"), plus mode label, score, and depth layers label (`composeApp/src/commonMain/kotlin/com/example/snake/game/ui/GameScreen.kt:265-577`). Keyboard handling for 3D currently swallows direction input without dispatching (`composeApp/src/commonMain/kotlin/com/example/snake/game/ui/GameScreen.kt:615-616`).
- **Input mapping**: `KeyboardDirectionMapper` and `GameKey` cover 4 planar directions + pause keys; no forward/backward keys mapped. `InputCapabilities` covers keyboard/touch availability.

#### New Concepts Required
- **Depth direction pair**: The missing axis for movement through depth (Forward/Backward). Needs first-class direction values with depth offsets and correct opposite pairing, so AC1 and AC3 can be evaluated uniformly.
- **Playable 3D navigation flow**: Removing the 3D block in `GameRules.requestDirection` and `GameRules.advance`, and re-enabling `GameController.startClock` for 3D, to turn the existing static 3D view into continuous tick-driven movement (AC1, AC2).
- **3D-aware input wiring**: Keyboard and touch controls that can request all six valid directions and surface the same pendingDirection feedback already used in 2D. This connects the existing disabled `ThreeDControls` to `GameRules.requestDirection`.
- **Depth-distinguishable movement feedback**: The visual/behavioral guarantee that forward/backward steps are distinguishable (the existing layered renderer already offsets layers, but the progression of segments across depth layers becomes observable only when navigation is active).

#### Key Business Rules
- Each movement step advances exactly one cell in the current or newly accepted valid direction; without input the snake continues in its last valid direction — governs Snake, Board, GameRules.advance, MovementClock.
- An immediate reversal (direction opposite to currentDirection) is rejected and does not end the session; pendingDirection gating means a second different request cannot replace an accepted but not yet applied turn — governs Direction.opposite, GameRules.requestDirection.
- Food collection grows snake by exactly one segment (head advances, tail retained), increases score by exactly 10, removes consumed food from its cell, and spawns exactly one replacement food on an unoccupied cell — governs Snake, GameState, GameRules.advance.
- Repeated collection accumulates linearly (n foods → +n segments, +10n score) while session remains active — governs score and snake length progression.
- Bounded 3D space: movement stays within Board.contains; boundary and self-collision outcomes are explicitly out of scope for this story and must not be introduced here beyond the existing collision checks needed to avoid conflating with STORY-002-003.

## Strategic Approach

#### Solution Direction
- Enable the already-present 3D spatial model (Cell depth, Board depth=3, mode-aware initialization) by lifting the intentional 3D blocks in the shared `commonMain` rules and controller, rather than introducing a parallel 3D engine. Reuse the existing `GameState` invariants, `Snake` operations, `randomUnoccupiedCell` (already depth-aware), and `GameController` generation/clock lifecycle.
- Extend `Direction` with the depth axis so opposition and offset logic remain centralized and the existing reversal/pendingDirection policy applies uniformly to all six directions.
- Wire input for all targets (keyboard + disabled `ThreeDControls`) to dispatch depth directions through the same `GameRules.requestDirection → GameController.requestDirection → GameRules.advance` flow used by 2D, and re-enable `GameController.startClock` for 3D so ticks drive observable movement. Keep rendering changes minimal because `ThreeDSnakeBoard` already provides distinguishable depth layers.
- Validate with deterministic common tests mirroring 2D coverage (direction acceptance, reversal, continuation, food growth/replacement, accumulation) but in 3D board coordinates, plus interaction checks that each depth direction is reachable from both keyboard and touch.

#### Key Design Decisions
- **Extend Direction vs introduce a separate depth enum**: Separate enum would require bifurcated request/advance logic and duplicate opposite checks. Extending `Direction` with FORWARD/BACKWARD keeps single offset/opposite source of truth, reuses existing pendingDirection and reversal logic, and minimizes divergence — recommended: extend `Direction`.
- **Reuse GameRules.advance vs fork 3D advance path**: Current 2D advance already handles pendingDirection application, wall/self collision, food growth and replacement. Forking would duplicate score (+10) and food invariant logic and risk scoring drift. Reuse the same advance implementation with depth-aware offsets — recommended: lift the UNSUPPORTED_MODE guard and reuse.
- **Re-enable movement clock for 3D vs manual stepping only**: AC2 and non-functional responsiveness require autonomous per-tick advancement; manual stepping would not satisfy "continues advancing one cell per movement step". Lifting the `mode == THREE_D` guard in `startClock` preserves existing generation/cancellation safety — recommended: re-enable clock.
- **Touch controls reuse vs new 3D control abstraction**: Existing `ThreeDControls` already renders six labeled buttons with correct semantics but is disabled and disconnected. Wiring it to `onDirection` like `DirectionControls` avoids a new abstraction and preserves accessibility stateDescriptions — recommended: wire existing controls.
- **Keyboard mapping choice for depth**: Need unambiguous keys for forward/backward that do not conflict with pause keys. Options include Q/E or bracket keys; choice should align with target `GameKey` and `KeyboardDirectionMapper` conventions and be reflected in `controlHint`. Recommend product-confirmed mapping, with Q/E as low-conflict candidate.
- **Scope containment for collision**: AC scope out excludes game-over presentation, but `advance` must still handle boundary/self collision internally to avoid infinite movement tests masking out-of-bounds growth. Recommendation: keep collision detection in `advance` (already present for 2D) but defer game-over UI/restart handling to STORY-002-003.

#### Alternatives Considered
- **Keep Direction planar and map Forward/Backward via modifier keys or view-only actions**: Rejected — adds indirection, complicates reversal checks, and leaves pendingDirection policy inconsistent across axes.
- **Create a separate ThreeDGameRules / ThreeDController**: Rejected — duplicates board/food/score invariants and lifecycle generation logic; the codebase already shares these via `GameRules` and `GameController`.
- **Implement 3D rendering upgrade before gameplay**: Rejected — `ThreeDSnakeBoard` already provides visible depth with layered offsets and depth colors; story's distinguishability requirement is about movement feedback, not renderer replacement.
- **Treat 3D movement as discrete player-stepped only (no clock)**: Rejected — violates AC2 continuation requirement and the shared tick model used by 2D.

## Risk & Gap Analysis

#### Requirement Ambiguities
- **Forward vs Backward semantics**: The requirement says "forward, backward" but does not define which depth sign is forward (depth+1 vs depth-1) or whether forward means "away from viewer" vs "toward viewer". Affects Direction offsets, rendering expectation, and keyboard labeling.
- **Keyboard binding for depth**: No keys specified for forward/backward on desktop; available keys (Q/E, etc.) and focus behavior for Wasm/desktop not defined. AC1 requires "available target control" but mapping is undefined.
- **Touch control layout for six directions**: Requirement says "visible controls for movement in all three dimensions" but does not specify six-button arrangement, reachability on small screens, or selection feedback parity with 2D's "Turn accepted" slot.
- **Scope of AC1 example set**: AC1 enumerates "up, down, forward, backward, or right" as examples — omits left intentionally or as shorthand. AC wording implies all valid non-reversal directions should work; product confirmation needed that all six are in scope.
- **Timing of direction change**: "At the next movement step" implies accepted direction is applied on next tick, consistent with 2D's pendingDirection model. Not explicitly stated for 3D whether multiple queued inputs collapse to one pending turn; implicit 2D rule should be confirmed to apply.

#### Edge Cases
- **Reversal via pending direction**: Player moving RIGHT queues FORWARD as pending, then requests LEFT (opposite of current but not of pending) — should LEFT be rejected because currentDirection is still RIGHT, or evaluated against pending? 2D policy rejects against currentDirection only; edge case needs consistent application.
- **Opposite depth reversal**: Moving FORWARD and requesting BACKWARD must be rejected as immediate reversal, analogous to LEFT/RIGHT. Confirm Direction opposite pairs cover depth axis correctly.
- **Food placed on different depth layer**: After collection, replacement food may spawn on any depth (randomUnoccupiedCell already does this). Steering challenge across depth layers is implicit but not explicitly called out in ACs — verify expected spawn distribution.
- **Board capacity exhaustion**: On a 20x20x3 board (1200 cells) this is unlikely, but FOOD_COLLECTION_BLOCKED outcome exists; requirement does not describe terminal behavior when no free cell remains. Explicitly deferred as out-of-scope collision handling.
- **GoldFishes**: Movement tick interval vs input responsiveness: 150ms interval may need validation that direction acceptance feedback remains immediate enough for depth steering on all targets (non-functional expectation).

#### Technical Risks
- **Direction extension impact radius**: `Direction` is used in `GameScreen` labels, semantics, and tests; adding FORWARD/BACKWARD changes `Direction.values()` iteration and exhaustive `when` branches. Mitigate by auditing all `when(direction)` sites and updating `label()`/`offset()`/`opposite()` together.
- **Unblocking 3D advance re-exposes collision ends**: Once `advance` is enabled for 3D, `BOUNDARY_COLLISION` and `SELF_COLLISION` will produce `GAME_OVER` transitions whose presentation is out of scope. Guard against UI scope creep by keeping game-over handling to minimal state change only, with presentation deferred to STORY-002-003.
- **Controller clock re-enable for 3D**: Removing the THREE_D guard in `startClock` must not create duplicate jobs or stale generation ticks; existing generation/cancellation logic should be re-validated for mode-switched restarts.
- **Enabled touch controls may conflict with disabled-state accessibility**: Current ThreeDControlButtons have stateDescription "Unavailable until 3D movement is implemented" — when wired, that description and enabled flag must change, and selectedDirection highlighting parity with 2D must be added.
- **Keyboard mapper collision**: Adding forward/backward keys must not alias P/Space pause handling in `handleKeyboardEvent` and must map through `GameKey` consistently across desktop/Wasm.

#### Acceptance Criteria Coverage
| AC# | Description | Addressable? | Gaps/Notes |
|-----|-------------|--------------|------------|
| AC1 | Snake can move through all three dimensions (up/down/forward/backward/right via target control, next-step direction change, depth distinguishable) | Yes | Blocked only by UNSUPPORTED_MODE guards and missing Direction values. Existing layered board already provides visible depth; needs input wiring and Direction extension. Key mapping for depth needs product choice. |
| AC2 | Continues in last valid direction without further input | Yes | Shared tick model + pendingDirection clearing already implements this for 2D; reusing for 3D and re-enabling clock satisfies AC. |
| AC3 | Immediate reversal rejected (RIGHT→LEFT ignored) | Yes | Existing reversal check generalizes if FORWARD↔BACKWARD opposite defined. Apply same IGNORED_REVERSAL result. |
| AC4 | Collecting food grows snake 3→4, score 0→10, food removed | Yes | `moveToAndGrow` and +10 scoring already implemented; depth offset already included in nextHead calculation once Direction extended. |
| AC5 | Replacement food appears on unoccupied cell, steering can continue | Yes | `randomUnoccupiedCell` already iterates depth; FOOD_COLLECTION_BLOCKED edge case exists but is out-of-scope termination. |
| AC6 | Two collections → +2 segments, score 20 | Yes | Accumulated growth/score follows directly from AC4×2; no new logic beyond repeated advance cycles. |

