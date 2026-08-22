# SPDD Analysis: Start and Control the Snake

## Original Business Requirement

## STORY-001-001 Start and Control the Snake

### Background

Players need an immediate way to begin a session and understand how to guide the snake. This story establishes the visible board, the initial state, and the directional interaction that makes the game playable before progression and end-of-session rules are added.

### Business Value

- Let a first-time player start playing without setup or an account.
- Make the snake's current position and direction understandable at a glance.
- Provide a consistent control experience on keyboard-capable and touch-capable supported targets.

### Dependencies and Assumptions

- **Parent epic**: [Epic-1 Snake Game](Epic-1-snake-game.md)
- **Prerequisites**: The application can open a game view; no completed gameplay story is required to demonstrate the initial board and movement.
- **Data assumptions**: A new session begins with a three-segment snake and a current score of 0.
- **Integration points**: Desktop players use arrow keys or W/A/S/D; touch players use visible up, down, left, and right controls.
- **Business constraints**: A direction directly opposite to the snake's current direction is not valid for the next movement step.

### Scope In

- Starting a new session.
- Showing the bounded board, snake, current score, and target-appropriate controls.
- Moving the snake one grid cell per movement step and changing its direction through valid input.

### Scope Out

- Food collection and score increases.
- Collision outcomes and game-over presentation.
- Pause, resume, and best-score retention.

### Acceptance Criteria

#### AC1: A new session presents a playable board

**Given** the player opens the application on a supported target
**When** the player chooses to start a new game
**Then** the application shows a bounded board with a three-segment snake, a current score of 0, and the controls available on that target.

#### AC2: Keyboard players can steer the snake

**Given** a desktop session is active and the snake is moving right
**When** the player presses Up, Down, Right, W, S, or D
**Then** the snake uses the selected valid direction at the next movement step and advances one grid cell per step.

#### AC3: Touch players can steer the snake

**Given** a touch session is active and the snake is moving right
**When** the player taps Up, Down, or Right on the visible directional controls
**Then** the snake changes to that valid direction at the next movement step and the selected control provides clear feedback that it was accepted.

#### AC4: The snake keeps moving in its last valid direction

**Given** the player has selected a valid direction
**When** the player provides no further direction
**Then** the snake continues advancing one grid cell per movement step in the last valid direction.

#### AC5: Immediate reversal is rejected

**Given** the snake is moving right
**When** the player requests left before another valid direction is selected
**Then** the request is ignored, the snake continues moving right, and the session does not end because of that request alone.

### Non-Functional Expectations

- The board, snake, score, and controls remain clear and usable at every supported window or screen size.
- Directional input feels immediate enough for a player to correct the snake during normal play.

## Domain Concept Identification

#### Existing Concepts (from codebase)

- **No implemented runtime game concepts**: The repository contains the requirements and development-container configuration, but no application source, build file, game state, UI, or input implementation. The intended product is described as a Kotlin Compose Multiplatform application in `requirements/Epic-1-snake-game.md:7-10`, while the only development configuration is a TypeScript/Node container in `.devcontainer/devcontainer.json:1-16` and `.devcontainer/Dockerfile:1-17`.

#### New Concepts Required

- **Game session**: The bounded lifecycle that begins when a player starts a new game and owns the initial playable state; it contains the board, snake, current direction, and score.
- **Bounded board**: The finite grid on which the session is rendered and the snake advances; it is the spatial boundary for the current story and later collision rules.
- **Snake**: The player-controlled, ordered three-segment body whose visible position and direction change during movement.
- **Movement step**: The shared unit of progression in which the snake advances exactly one grid cell and applies the most recently accepted direction.
- **Direction intent**: A requested up, down, left, or right movement originating from a keyboard mapping or a visible touch control; the session accepts only valid intents.
- **Target-appropriate control surface**: The input affordances exposed for the current platform, namely keyboard controls on desktop and visible directional controls on touch-capable targets.
- **Current score**: Session-visible numeric state initialized to 0; it is displayed now even though score increases are explicitly outside this story.

#### Key Business Rules

- A new game creates exactly one session with a three-segment snake and current score 0.
- The board is bounded and the snake advances one grid cell for every movement step.
- An accepted direction takes effect at the next movement step, and the snake continues in its last valid direction when no new intent arrives.
- A direction directly opposite to the snake's current direction is invalid for the next movement step and must be ignored.
- The initial state must support the story's right-moving scenarios; the exact initial orientation and placement still need to be made explicit.
- Keyboard and touch inputs are alternative ways to express the same directional intent, while the visible controls must match the supported target.
- An ignored reversal request alone must neither change the direction nor end the session.
- Food, score increases, collision outcomes, pause/resume, and best-score retention remain outside this story, as stated by the requirement and the epic's story decomposition in `requirements/Epic-1-snake-game.md:112-118`.

## Strategic Approach

#### Solution Direction

- Establish the game as an offline, shared-rule, cross-platform experience consistent with the Kotlin Compose Multiplatform product direction stated by the epic. A platform-neutral session model should own the board, snake, direction acceptance, movement progression, and score initialization, while each target supplies its appropriate input surface and renders the same visible state.
- Treat keyboard events and touch taps as translations into one directional language rather than as separate gameplay rules. The shared movement policy accepts a valid intent for the next movement step, preserves the last valid direction, and rejects immediate reversal before the state is rendered again.
- Make the first playable state easy to understand and repeat: start with a known, valid three-segment arrangement, a clearly visible rightward direction, and score 0. Keep the presentation responsive so the grid remains legible and touch controls remain usable across the supported sizes.
- Preserve a clean boundary between this foundational movement capability and later progression, collision, pause, and score-retention capabilities. The initial session state should be extensible for those stories without introducing their behavior into this story.

#### Key Design Decisions

- **One shared gameplay rule set versus target-specific gameplay**: Target-specific rules could fit each platform's event model but would duplicate reversal and movement behavior and risk violating the epic's cross-target consistency rule (`requirements/Epic-1-snake-game.md:18-25`). Use one shared conceptual rule set and keep platform differences at the input and presentation boundaries.
- **Deterministic initial arrangement versus randomized placement**: Random placement could provide variety but makes the initial state harder to explain and compare across targets; a deterministic, valid arrangement makes AC1 demonstrable and supports reliable cross-target checks. Prefer deterministic initialization for this story, with the exact board dimensions and placement explicitly agreed before detailed design.
- **Canonical grid-step progression versus presentation-driven movement**: Allowing each target's rendering cadence to drive movement may make motion feel different across devices. Prefer a canonical logical movement step so one step means one cell and input timing has the same meaning on every target; the visual cadence should then be tuned for responsiveness without changing the rule.
- **Common direction intents versus separate keyboard and touch semantics**: Separate semantics would simplify local event handling but make accepted/rejected directions inconsistent. Normalize both control types into common direction intents and provide target-specific feedback only at the presentation boundary.
- **Explicit input acceptance policy versus unrestricted queued inputs**: A clear policy is needed for several inputs arriving before the next movement step, especially a turn followed by its opposite. Prefer defining acceptance relative to the current and pending direction, applying at most the valid next turn for a step, so the immediate-reversal invariant cannot be bypassed by rapid input.
- **Adaptive board and control layout versus fixed dimensions**: Fixed dimensions simplify layout but can make the board or touch targets unusable at supported sizes. Prefer an adaptive presentation that preserves grid clarity, control reachability, and directional visibility while leaving gameplay coordinates independent of screen pixels.

#### Alternatives Considered

- **Implement independent desktop and touch game loops**: Rejected because duplicated rules would increase divergence risk and make AC8 from the parent epic harder to satisfy.
- **Use raw frame movement instead of logical grid steps**: Rejected because frame rate and platform scheduling would undermine the one-cell-per-step contract and cross-target comparability.
- **Expose only keyboard shortcuts or only touch controls**: Rejected because the story explicitly requires target-appropriate support for both keyboard-capable and touch-capable targets.
- **Introduce food, collision, pause, or persistence behavior in this increment**: Rejected because those capabilities are explicitly out of scope and are already assigned to later stories in the epic.

## Risk & Gap Analysis

#### Requirement Ambiguities

- The start interaction is not named: it is unclear whether the application opens directly into a new session or presents a specific start action, and what happens if the player requests a new game while one is active.
- The supported-target inventory is not stated. The requirement distinguishes desktop and touch capability, but does not define the concrete targets or how a hybrid device exposes controls.
- The initial snake position, orientation, and minimum board dimensions are unspecified, although AC2 and AC3 assume that the active snake is moving right.
- The movement-step cadence is not measurable. “Immediate enough” does not define an acceptable input-to-motion or feedback latency.
- The handling of multiple directional requests before one movement step is not specified, including whether a queued turn is replaced, retained, or followed by another turn.
- The reversal rule refers to the current direction, but it is not explicit whether a request opposite to an already accepted-but-not-yet-applied turn is also rejected.
- The story names arrow keys and W/A/S/D in its integration assumptions, while AC2 lists only the valid right-moving inputs; the exact mapping, case behavior, focus behavior, and treatment of left/A at session start should be made explicit.
- “Clear feedback” for an accepted touch control is not defined, and the requirement does not state whether rejected touch input needs feedback.
- “Clear and usable at every supported window or screen size” lacks minimum board size, touch-target size, accessibility, orientation, and resize expectations.

#### Edge Cases

- A player presses a direction before the session is active, immediately after starting, or after the session becomes unavailable; the required handling is not stated.
- The player sends an unknown key, a lowercase/uppercase letter variant, a key while focus is outside the game, or simultaneous/conflicting keys.
- The player taps controls rapidly, taps the same direction repeatedly, taps left while moving right, or uses multi-touch; acceptance and feedback behavior should remain consistent.
- The player requests a sequence such as up then left before the next movement step; without an explicit buffering policy, different event schedules could produce different turns.
- The board is resized or rotated while a session is active, or the available area is too small to show three segments and usable controls.
- A movement step would place the snake at or beyond the board boundary. Collision outcomes are out of scope, but the current story needs a defined demonstration boundary so the foundational movement behavior does not become ambiguous.
- The first frame after starting may be rendered before the first movement step; the expected initial direction indicator and score visibility should be specified.
- A touch target has a safe-area, pointer, or accessibility interaction that differs from the nominal screen coordinates.
- The score must remain 0 throughout this story even though the later food story will change it; current-state rendering should not imply unavailable progression.

#### Technical Risks

- **Greenfield implementation gap**: No Kotlin, Compose, Gradle, or application source files exist, so there is no current code location, test harness, architecture convention, or build to extend. The epic specifies Kotlin Compose Multiplatform (`requirements/Epic-1-snake-game.md:7-10`), but the checked-in container assumes TypeScript/Node and runs `npm ci` (`.devcontainer/devcontainer.json:1-16`) despite there being no `package.json`; project bootstrap must be resolved before implementation can be validated.
- **Cross-target timing drift**: If movement and input acceptance are governed by platform-specific scheduling, the same actions can result in different board states. A canonical logical step and shared rule semantics mitigate this risk.
- **Input-event race conditions**: Rapid or simultaneous directional events can bypass the intended reversal invariant unless the accepted-next-direction policy is explicit and centralized.
- **Responsive rendering constraints**: A board that scales poorly or controls that are too small will violate the non-functional expectations even if the gameplay state is correct; layout must preserve grid legibility and touch reachability.
- **Boundary ownership across story increments**: This story needs movement on a bounded board while collision outcomes belong to a later story. The boundary between rendering a bounded play area and ending a session must be explicit so the foundational increment does not accidentally define conflicting game-over behavior.
- **Validation blind spot**: There are no existing application or test files to demonstrate AC1–AC5. A later implementation phase will need shared rule tests plus target-level interaction checks before the acceptance criteria can be considered implemented.

#### Acceptance Criteria Coverage

| AC# | Description | Addressable? | Gaps/Notes |
|-----|-------------|--------------|------------|
| AC1 | Start a new game and show a bounded board, three-segment snake, score 0, and target controls. | Partial | The behavior is strategically covered, but the start action, supported-target list, initial placement, and board dimensions are unspecified; no application exists yet. |
| AC2 | On desktop, accept Up/Down/Right and W/S/D while moving right and apply a valid turn at the next step. | Yes | The shared direction policy covers the listed inputs; key mapping, focus/case behavior, and measurable movement cadence need clarification. |
| AC3 | On touch, accept Up/Down/Right and give clear feedback at the next movement step. | Yes | The target-specific control surface and common direction policy cover the behavior; feedback definition and rejected-left behavior remain open. |
| AC4 | Continue one-cell movement in the last valid direction when no further input arrives. | Yes | The persistence rule is explicit; the movement cadence and behavior at the board boundary need definition. |
| AC5 | Ignore an immediate left reversal while moving right, continue right, and do not end the session from that request alone. | Yes | The invariant is explicit; the input buffering rule and any feedback for a rejected request should be made consistent across targets. |

All five acceptance criteria are covered by the proposed strategic direction; AC1 is only partially specified and requires product clarification before detailed design. The table assesses addressability, not implementation status: the current repository has no application implementation.
