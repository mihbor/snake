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
