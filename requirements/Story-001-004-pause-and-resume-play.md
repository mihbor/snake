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
