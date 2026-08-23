# SPDD Analysis: Eat Food and Build Score

## Original Business Requirement

## STORY-001-002 Eat Food and Build Score

### Background

A snake game needs a visible objective that rewards movement. Food gives the player a clear short-term goal, makes the snake grow, and turns survival into a measurable score chase.

### Business Value

- Give players a concrete reason to steer the snake around the board.
- Make successful play visible through growth and score feedback.
- Create a repeatable scoring loop that supports competition with the player's own best result.

### Dependencies and Assumptions

- **Parent epic**: [Epic-1 Snake Game](Epic-1-snake-game.md)
- **Prerequisites**: A session can display a board and move a snake as described by [STORY-001-001](Story-001-001-start-and-control-the-snake.md).
- **Data assumptions**: A new session starts with one food item on an unoccupied cell and a score of 0.
- **Integration points**: Food and score are presented within the game view; no account or network service is required.
- **Business constraints**: Each collected food item awards exactly 10 points and adds exactly one snake segment.

### Scope In

- Showing one food item in the active session.
- Recognizing food collection and adding one snake segment.
- Increasing and displaying the current score by 10 for every collected food item.

### Scope Out

- Collision consequences and the game-over state.
- Pause and resume controls.
- Power-ups, bonus scoring, levels, or multiple food types.

### Acceptance Criteria

#### AC1: Food gives the player a visible target

**Given** a new session has started with a three-segment snake and a score of 0
**When** the board is displayed
**Then** exactly one food item is visible on a cell that is not occupied by the snake.

#### AC2: Collecting food grows the snake and awards points

**Given** the snake's head is one movement step from the food and the score is 0
**When** the snake moves onto the food
**Then** the snake grows from three segments to four, the score changes to 10, and the collected food is no longer shown at that location.

#### AC3: A replacement food item appears after collection

**Given** the player has collected food
**When** the collection is reflected on the board
**Then** one new food item appears on an unoccupied cell and the player can continue steering toward it.

#### AC4: Repeated collection accumulates score

**Given** the player has collected two food items without ending the session
**When** the second collection is completed
**Then** the snake has gained two segments from its starting length and the current score is 20.

#### AC5: Food never occupies the snake

**Given** the snake occupies one or more board cells
**When** a food item is first placed or replaced
**Then** the food is visible on a different, unoccupied cell and remains reachable within the bounded board.

### Non-Functional Expectations

- The score and snake growth are visible immediately after each collection.
- Food is visually distinct from the snake and the board so that the next objective is obvious.

## Domain Concept Identification

#### Existing Concepts (from codebase)

- **Game session**: `GameState` and `SessionStatus` represent the local session lifecycle and the state exposed through `GameController`; the current lifecycle distinguishes `READY` from `ACTIVE` and already owns the board, snake, direction, and score (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/GameState.kt:3-14`, `composeApp/src/commonMain/kotlin/com/example/snake/game/controller/GameController.kt:30-41`).
- **Bounded board**: `Board` defines positive row and column dimensions and determines whether a cell lies inside the play area; the established default is a `20 x 20` grid (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/Board.kt:3-13`, `composeApp/src/commonMain/kotlin/com/example/snake/game/rules/GameRules.kt:15-24`).
- **Snake**: `Snake` is an ordered body whose head is the first segment, and the existing session starts with three centered, distinct segments moving right (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/Snake.kt:3-14`, `composeApp/src/commonMain/kotlin/com/example/snake/game/rules/GameRules.kt:16-32`).
- **Movement step and direction intent**: `Direction` supplies grid offsets and opposites, while `GameRules` retains an accepted turn and advances one cell per logical step; this is the shared progression boundary that food collection must extend (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/Direction.kt:3-20`, `composeApp/src/commonMain/kotlin/com/example/snake/game/rules/GameRules.kt:40-86`).
- **Current score**: The session already carries a non-negative current score, and `GameScreen` renders it with accessible current-score semantics, so this story adds score changes to an existing player-facing concept rather than introducing a second score surface (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/GameState.kt:3-12`, `composeApp/src/commonMain/kotlin/com/example/snake/game/ui/GameScreen.kt:98-104`).
- **Player-facing game view**: `SnakeApp` collects the shared state and passes it to `GameScreen`; the screen already renders the bounded grid and snake through Compose and is the established presentation boundary for food and score feedback (`composeApp/src/commonMain/kotlin/com/example/snake/game/ui/SnakeApp.kt:14-32`, `composeApp/src/commonMain/kotlin/com/example/snake/game/ui/GameScreen.kt:151-192`).

#### New Concepts Required

- **Food item**: The single active target owned by a session, represented conceptually by one board cell that is always inside the board and outside the snake; it begins with the active session and is replaced after collection.
- **Food collection progression**: The business event that links a movement step to one-segment growth, a 10-point award, removal of the collected target, and creation of the next target; it must remain part of the same shared gameplay outcome across platforms.

#### Conceptual Relationships

- A game session owns one bounded board, one snake, one current score, and at most one active food item; the food item's valid location is constrained by the board and the snake's occupied cells.
- A movement step changes the snake's position and may produce a food collection progression; collection changes the snake length and score together and replaces the food before the resulting session state is presented.
- The game view observes the session state rather than determining collection; it presents the score, snake growth, and food target so the player can immediately understand the next objective.
- Food placement is session-scoped and resets with a new session; best-score retention, accounts, and network services remain outside this story.

#### Key Business Rules

- An active new session has exactly one food item, and its cell is distinct from every snake segment.
- Moving onto the active food item collects it exactly once, adds exactly one snake segment, and increases the current score by exactly 10.
- The collected location must no longer be rendered as food after collection; exactly one replacement target is then visible on a valid unoccupied cell.
- After two collections in one uninterrupted session, the snake has two more segments than its starting length and the current score is 20; previous score and growth must accumulate rather than reset.
- Food placement and replacement must never knowingly select a snake-occupied cell or a cell outside the bounded board.
- Score and growth must become visible as part of the same player-observable state change; food must have a visual treatment distinct from both the snake and the board.
- Collision consequences, game-over, pause/resume, bonus scoring, levels, power-ups, and multiple food types must not be introduced as implicit side effects of this story.

## Strategic Approach

#### Solution Direction

- Extend the existing shared session model and `GameRules` progression boundary with one session-owned food lifecycle. The shared rules should decide whether the next movement reaches food, apply the collection progression, and preserve the existing one-cell movement semantics when it does not.
- Keep snake growth and the 10-point award in one coherent gameplay transition so the snake, score, and food cannot be observed in an intermediate combination. The existing `GameController` state flow can continue to publish the resulting immutable session state, while its clock and direction-input responsibilities remain unchanged.
- Reuse the current Compose game view and board renderer for presentation. The score is already reactive and visible; the board should add one clearly contrasting food representation and update it from the same state that reports the new snake length and score.
- Keep the food rules in `commonMain` so Android, desktop JVM, and browser targets use the same placement constraints and collection outcomes. The build currently configures all three target families in `composeApp/build.gradle.kts:27-45`, and no backend or network integration is needed.
- Treat food placement as a bounded free-cell concern owned by the gameplay domain, not as a visual random decoration. The policy must account for the current snake occupancy at initial placement and after every collection while preserving a valid continuation of play.

#### Key Design Decisions

- **Session-owned food versus view-computed food**: Computing a target only in the UI could make the displayed target differ from the rule target and would make deterministic acceptance checks difficult. Keep one authoritative food concept in the shared session state so rules, controller, renderer, and tests observe the same target.
- **Deterministic versus random placement**: Random placement gives traditional game variety but introduces reproducibility and cross-target consistency risks; deterministic placement fits the existing deterministic initialization and the epic's shared-rule goal. Prefer deterministic placement for this increment, or require a shared reproducible placement source if product later chooses randomness.
- **Atomic collection progression versus separate updates**: Updating growth, score, and replacement independently can expose stale food, a wrong length, or an intermediate score to the reactive UI. Treat collection as one logical state change; the trade-off is that the domain transition must own more of the progression invariant.
- **Normal movement versus growth-aware movement**: The current snake movement preserves its length by dropping the tail, which is correct for ordinary steps but cannot represent collection growth. Extend the conceptual movement policy so a food-reaching step retains the appropriate additional segment while all non-collection steps keep their existing behavior.
- **Shared rendering versus target-specific food presentation**: A separate food rule or visual on each target could produce inconsistent gameplay feedback. Use the existing shared board presentation contract and adapt only platform input surfaces, with sufficient contrast and state feedback for the target marker.
- **Full-board behavior versus silent placement failure**: Ending the game when no free cell remains would overlap the later collision/game-over story, while silently omitting a replacement violates AC3. Do not invent a terminal outcome in this increment; clarify the capacity rule before detailed design, with the placement policy explicitly reporting or handling exhaustion rather than looping indefinitely.

#### Alternatives Considered

- **Keep food only in the Compose canvas and infer collection from pixels or rendered coordinates**: Rejected because gameplay rules would depend on presentation, weaken common tests, and risk different outcomes across Android, desktop, and browser targets.
- **Generate a separate random target in each platform entry point**: Rejected because the same sequence of movement steps could produce different food positions and scores, violating the epic's cross-target consistency objective.
- **Pre-place multiple food items to avoid replacement timing**: Rejected because the story requires exactly one visible food item and explicitly excludes multiple food types or targets.
- **Add collision/game-over handling when the board has no free cell**: Rejected for this increment because collision consequences and the game-over state belong to `STORY-001-003`; the product must decide the capacity behavior without silently expanding scope.
- **Treat growth as a later visual effect after awarding score**: Rejected because the non-functional expectation requires immediate, coherent visibility of score and snake growth after each collection.

## Risk & Gap Analysis

#### Requirement Ambiguities

- **Placement distribution is unspecified**: The story requires a valid target but does not say whether the initial and replacement positions should be deterministic, random, or repeatable across sessions; this affects testability and the player's perception of variety.
- **“Reachable” is undefined**: AC5 may mean merely inside the bounded board and not occupied, or it may require a currently navigable path; no path rule, obstacle rule, or self-collision rule exists in this story to distinguish those meanings.
- **No-free-cell behavior is absent**: As the snake grows, a finite board can eventually have no unoccupied cell. AC3 still requires one replacement, but the story does not define whether collection is prevented, the session pauses, or a later outcome is used.
- **Pre-game food visibility is unclear**: The current `READY` state is derived from a fresh game state, while the requirement speaks about the active session; it is not stated whether food should be present before the start action or only after the session becomes active.
- **Collection precedence with future collisions is unspecified**: Boundary and self-collision consequences are out of scope, but the ordering between reaching food and a future collision on the same movement step must eventually be consistent with `STORY-001-003`.
- **Minimum board capacity is unspecified**: The existing rules reject a board too small for the initial three-segment snake, but the requirement does not state the additional free-cell capacity needed to guarantee an initial food item and later replacements on custom board sizes.

#### Edge Cases

- **Initial placement on a nearly full board**: A board may be valid for the three-segment snake but leave no distinct cell for food; initialization then cannot satisfy AC1 without a documented capacity constraint.
- **Replacement when the snake occupies almost every cell**: The free-cell set can become empty immediately after a collection, making AC3 and AC5 impossible unless the product defines a terminal or exhaustion policy.
- **A blocked movement step**: The existing boundary behavior returns a no-op and retains the session state; a step that does not enter the food must not award points, grow the snake, or replace the target.
- **Food next to the head with a pending turn**: Collection must follow the same accepted-direction and logical-step semantics as ordinary movement, not the order in which platform input events happen to arrive.
- **Repeated collections**: The second collection must preserve the first award and segment, producing five segments from the three-segment start and a score of 20, while still leaving one valid next target.
- **Restarting or starting a new session after prior progress**: The new session must reset current score and snake length and create a fresh valid food state rather than carry over the previous target or score; best-score behavior remains explicitly out of scope.
- **Food and the moving tail**: Future self-collision rules may distinguish a tail cell that is about to leave from a body cell that remains; this story should not silently decide that collision policy while enforcing the no-overlap invariant for placed food.

#### Technical Risks

- **Growth conflicts with the current movement abstraction**: `Snake.moveTo` always drops the last segment, so collection cannot be added as a superficial score change; the shared movement concept must support a longer result without regressing ordinary movement (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/Snake.kt:10-14`).
- **Non-atomic reactive state updates**: `GameController` exposes a `StateFlow` and the Compose screen renders immediately from it. Separate updates for snake, score, and food could briefly show inconsistent progression or allow a second tick to observe a partially applied collection (`composeApp/src/commonMain/kotlin/com/example/snake/game/controller/GameController.kt:30-40`, `composeApp/src/commonMain/kotlin/com/example/snake/game/ui/SnakeApp.kt:16-29`).
- **Free-cell selection reliability**: Placement must terminate and remain valid as the available area shrinks; an unbounded retry strategy or an unchecked random choice could hang or place food on the snake near capacity.
- **Cross-target divergence**: Platform-specific placement or collection logic would undermine the existing shared `commonMain` architecture and the epic's requirement that equivalent move and collection sequences produce equivalent states.
- **Visual clarity and responsive layout**: The current canvas scales the grid and uses distinct snake colors but has no food drawing. A food marker that is too small or too close in contrast to the board could violate the objective-visibility expectation even if the domain state is correct (`composeApp/src/commonMain/kotlin/com/example/snake/game/ui/GameScreen.kt:152-192`).
- **Validation coverage gap**: Existing common tests cover deterministic initialization, movement, direction handling, and controller ticks, but there are no food or growth scenarios. The shared rule tests and controller-level state propagation need to cover collection, replacement, accumulation, and placement exclusion before the story can be considered demonstrable (`composeApp/src/commonTest/kotlin/com/example/snake/game/GameRulesTest.kt:58-214`, `composeApp/src/commonTest/kotlin/com/example/snake/game/GameControllerTest.kt:27-100`).

#### Acceptance Criteria Coverage

| AC# | Description | Addressable? | Gaps/Notes |
|-----|-------------|--------------|------------|
| AC1 | A new session shows exactly one food item on a cell not occupied by the three-segment snake. | Partial | The existing session, board, snake, score, and renderer establish the needed boundary, but food state, presentation, placement distribution, and minimum capacity are new or unspecified. |
| AC2 | Moving onto food grows the snake by one, changes score from 0 to 10, and removes food from the collected cell. | Yes | A shared collection transition can preserve the current movement contract while applying the three coupled changes; collision consequences remain outside this story. |
| AC3 | After collection, exactly one replacement food item appears on an unoccupied cell. | Partial | A session-owned replacement lifecycle addresses the normal case, but the no-free-cell outcome and the exact timing of the reflected replacement need product clarification. |
| AC4 | Two collections produce two additional segments from the start and a current score of 20. | Yes | Accumulating the fixed award and growth per collection directly addresses the criterion, provided collection updates are not reset between targets. |
| AC5 | Initial and replacement food never occupy the snake and remain reachable within the bounded board. | Partial | Occupancy exclusion is directly addressable through shared placement rules; “reachable” and finite-board exhaustion are not defined sufficiently to guarantee the literal wording in every state. |

All five acceptance criteria are assessed and structurally addressable by the shared-session approach. AC1, AC3, and AC5 remain partially specified around placement and capacity; no in-scope requirement is being deferred, and the current repository should be understood as having the prerequisite movement capability but not this food capability yet.