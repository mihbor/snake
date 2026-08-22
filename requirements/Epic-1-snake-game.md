# Epic: Epic-1 Snake Game

## Epic-1 Snake Game

### Background

The product is a small, immediately understandable arcade game for people who want a short, repeatable play session. A player guides a snake around a bounded board, collects food, grows longer, and tries to achieve the highest score before colliding with the board boundary or the snake itself.

The game will be delivered as a Kotlin Compose Multiplatform application so that the same core experience is available on each selected target platform. It must be demonstrable without an account or an online service: a player should be able to launch the application, start a game, understand the controls, and complete a full play session.

### Business Value

- Provide a complete, familiar arcade experience that a new player can understand within one play session.
- Give players immediate feedback through visible movement, food collection, score growth, and game outcomes.
- Support repeat play by making restart, pause, and best-score comparison easy to find.
- Establish a small cross-platform game experience that behaves consistently on every supported Kotlin Compose Multiplatform target.

### Dependencies and Assumptions

- **Prerequisites**: None; the epic includes the playable game loop and its player-facing controls.
- **Data assumptions**: A game session contains one snake, one current score, food placed on an unoccupied board cell, and a best score for the player.
- **Integration points**: The application receives the input method provided by each supported target, including keyboard controls on desktop targets and visible directional controls on touch targets.
- **Business constraints**: The game is single-player, works without an account or network connection, and must present the same rules and outcomes on every supported target.
- **Scoring rule**: Each food item increases the current score by 10 points and adds one segment to the snake.
- **Movement rule**: The snake advances one grid cell per movement step and cannot immediately reverse into its own body.

### Scope In

- Starting a new game and displaying a bounded play board.
- Steering a continuously moving snake with platform-appropriate controls.
- Displaying food, growing the snake when food is collected, and awarding points.
- Detecting boundary and self-collisions, showing the final outcome, and restarting.
- Pausing and resuming an active session.
- Showing and retaining the player's best score for later sessions.

### Scope Out

- Multiplayer or network-based play.
- Online leaderboards, accounts, or cloud synchronization.
- Power-ups, enemies, levels, obstacles, or multiple food types.
- Audio, music, monetization, advertisements, or social sharing.
- Custom themes or an in-game level editor.

### Acceptance Criteria

#### AC1: A player can start a session on every supported target

**Given** the application is opened on a supported Kotlin Compose Multiplatform target
**When** the player chooses to start a new game
**Then** a visible board shows a three-segment snake, one food item on an unoccupied cell, a current score of 0, and the controls needed for that target.

#### AC2: The snake responds to platform-appropriate controls

**Given** a game is active and the snake is moving right
**When** the player chooses up, down, or right using the target's available control method
**Then** the snake changes to the selected valid direction at the next movement step and continues advancing one cell per step.

#### AC3: Food collection provides visible progression

**Given** the snake has a current score of 0 and reaches the food
**When** the food is collected
**Then** the snake grows from three segments to four, the score becomes 10, and a replacement food item appears on an unoccupied cell.

#### AC4: Collisions end the current session

**Given** the snake's head reaches the board boundary or one of its own segments
**When** the collision occurs
**Then** movement stops, the player sees a clear game-over state, and the final score remains visible.

#### AC5: A player can immediately play again

**Given** a game-over state is displayed with a final score
**When** the player chooses to restart
**Then** a fresh session begins with a three-segment snake, a current score of 0, and food on an unoccupied cell.

#### AC6: Pausing preserves the session

**Given** a game is active with a current score of 20
**When** the player chooses pause
**Then** the board and score remain unchanged, movement and scoring stop, and the paused state is clearly visible until the player resumes.

#### AC7: The best result is retained

**Given** the player finishes a session with a score of 50
**When** the player starts another session or reopens the application
**Then** the best score is shown as 50 while the new session's current score starts at 0.

#### AC8: Rules are consistent across targets

**Given** the same sequence of valid moves and food collections is performed on two supported targets
**When** the sequence reaches the same board state
**Then** both targets produce the same snake length, score, collision result, pause behavior, and best-score result.

### Story Decomposition

### Abstract Task: "Snake Game"

**Analysis Dimensions**:

- **Core Responsibility**: Give a player a complete, responsive single-player snake game with clear progression and an understandable outcome.
- **Primary Operations**: Start a session, steer the snake, advance movement, collect food, grow, score, pause, resume, detect collisions, end a session, restart, and retain the best score.
- **Key Constraints**: Food must occupy an unoccupied cell; the snake cannot immediately reverse; boundary and self-collisions end the session; each food item awards 10 points; the experience must be consistent across supported targets.
- **Technical Complexity**: Medium — the experience must coordinate shared game behavior with platform-appropriate Compose Multiplatform presentation and input.
- **Business Complexity**: Low — the rules are familiar, the player role is singular, and the outcome is determined within one session.

**Conclusion**: Needs splitting

### Split Strategy

**Dimension**: Player journey and gameplay complexity, keeping each story to one demonstrable increment with no more than three core functional points.

| Story | Title | Core Functional Points |
|-------|-------|------------------------|
| STORY-001-001 | Start and control the snake | Start a session; display the board; accept valid directional input |
| STORY-001-002 | Eat food and build score | Place food; grow after collection; award points |
| STORY-001-003 | Finish and restart a game | Detect collisions; show game over; reset the session |
| STORY-001-004 | Pause and resume play | Pause movement; preserve state; resume the session |
| STORY-001-005 | Preserve the best score | Compare results; show the best result; retain it between sessions |
| STORY-001-006 | Host the browser game and provide download builds | Publish browser play; provide Android and desktop downloads; align links with the current release |

The recommended delivery order follows the player journey, but each story describes a visible business capability that can be demonstrated independently with the necessary game shell in place.
