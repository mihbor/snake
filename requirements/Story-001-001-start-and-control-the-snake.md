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
