# Epic: Epic-2 Optional 3D Snake Mode

## Epic-2 Optional 3D Snake Mode

### Original Requirement

`epic do add option play in 3D instead of 2D`

### Background

The current Snake experience gives players a familiar two-dimensional way to play. Some players want an alternative spatial challenge, so the product should let them choose to play in a three-dimensional space instead of the existing two-dimensional view. The new option must extend the current game without taking away the 2D experience that existing players already understand.

A 3D session presents a bounded play space with visible depth, a snake, food, and controls that let the player move through the space. The familiar objective remains intact: collect food, grow the snake, build a score, and avoid the boundaries and the snake's own body.

### Business Value

- Give players a meaningful choice between the familiar 2D game and a more challenging 3D game.
- Increase replay value by offering a different way to pursue a personal high score.
- Preserve the existing 2D experience for players who do not choose the new mode.
- Extend the game's recognizable scoring and session rules to the new play option without requiring an account or network connection.

### Dependencies and Assumptions

- **Prerequisites**: The existing [Epic-1 Snake Game](Epic-1-snake-game.md) is available as the established 2D game experience.
- **Data assumptions**: A 3D session contains one snake, one current score, one food item on an unoccupied cell in a bounded three-dimensional play space, and the player's best score.
- **Integration points**: The mode choice and gameplay use the input method available on each supported target, including keyboard controls on desktop targets and visible directional controls on touch targets.
- **Business constraints**: 2D remains available and is the default when the player has not selected 3D; the mode cannot change during an active session; the snake cannot immediately reverse direction; each food item adds one segment and 10 points; boundary and self-collisions end the session.
- **Score continuity**: A completed session in either mode can establish the player's single best score, and a lower result never replaces it.

### Scope In

- Offering a visible choice to start a game in 2D or 3D.
- Keeping the existing 2D mode available with its current player-facing rules.
- Showing a bounded 3D play space with visible depth, a snake, food, score, and mode-appropriate controls.
- Moving the snake through the 3D space and accepting valid direction changes.
- Collecting food, growing the snake, and awarding 10 points per food item in 3D.
- Supporting pause, resume, collision outcomes, restart, and best-score retention in 3D.
- Keeping score, outcome, and best-score behavior understandable and consistent between modes.

### Scope Out

- Removing, replacing, or changing the existing 2D mode.
- Multiplayer, network-based play, online leaderboards, accounts, or cloud synchronization.
- Virtual reality, augmented reality, motion-controller support, or custom camera controls.
- New power-ups, enemies, obstacles, levels, multiple food types, or mode-specific scoring bonuses.
- Custom themes, graphics-quality settings, or an in-game level editor.

### Acceptance Criteria

#### AC1: Players can choose the play dimension

**Given** the application is open before a session starts
**When** the player views the available game modes
**Then** both 2D and 3D choices are visible, and 2D is selected by default for a player who has not made a choice.

#### AC2: A player can start a 3D session

**Given** the player has selected 3D
**When** the player starts a new game
**Then** a bounded 3D play space shows a three-segment snake, exactly one food item on an unoccupied cell, a current score of 0, and controls for movement through the visible depth.

#### AC3: The snake responds to valid 3D directions

**Given** a 3D session is active and the snake is moving right
**When** the player chooses up, down, forward, backward, or right using the available target control
**Then** the snake changes to the selected valid direction at the next movement step and advances one cell through the 3D play space.

#### AC4: 3D food collection provides the same progression

**Given** the 3D snake has three segments, a current score of 0, and its head is one movement step from the food
**When** the snake moves onto the food
**Then** the snake grows to four segments, the score becomes 10, and exactly one replacement food item appears on an unoccupied cell.

#### AC5: 3D collisions end the session

**Given** the 3D snake's head reaches any boundary face or one of its own segments
**When** the collision occurs
**Then** movement stops, a clear game-over state is shown, and the final score remains visible.

#### AC6: Players can pause and resume a 3D session

**Given** an active 3D session has a current score of 20
**When** the player chooses pause and later resume
**Then** the board, snake, food, and score remain unchanged while paused, and the snake continues from the same position and direction after resume.

#### AC7: Restart preserves the selected mode and resets the attempt

**Given** a 3D game-over state is displayed with a final score of 30
**When** the player chooses restart
**Then** a fresh 3D session begins with a three-segment snake, a current score of 0, and food on an unoccupied cell.

#### AC8: Existing 2D play remains available

**Given** the player chooses 2D or leaves the default mode unchanged
**When** the player starts a new game
**Then** the existing 2D board, controls, movement, scoring, pause, collision, restart, and best-score behaviors remain available without requiring the player to use 3D.

#### AC9: The best score is shared across play dimensions

**Given** the player has completed a 3D session with a best score of 50
**When** the player starts a 2D session or reopens the application on the same target
**Then** the best score remains visible as 50, while a new session's current score starts at 0.

### Non-Functional Expectations

- Depth, snake position, food, current score, best score, and active mode are clear enough for a player to understand the 3D state during normal play.
- The 3D view and its controls remain usable at every supported window or screen size.
- A player can distinguish active, paused, and game-over states in both modes without reading technical documentation.
- The game remains playable without an account or network connection.

## Story Decomposition

### Abstract Task: "Optional 3D Snake Mode"

**Analysis Dimensions**:

- **Core Responsibility**: Let a player select and complete a Snake session in a bounded 3D space while preserving the existing 2D experience.
- **Primary Operations**: Choose a mode, start a session, navigate through three dimensions, collect food, grow, score, pause, resume, detect collisions, finish, restart, and retain the best score.
- **Key Constraints**: The 3D space is bounded; food occupies an unoccupied cell; the snake cannot immediately reverse; each food item awards 10 points and one segment; the mode remains fixed during a session; 2D behavior remains available.
- **Technical Complexity**: High — the feature adds a new play dimension while retaining the existing session behaviors and supported-target controls.
- **Business Complexity**: Medium — players need a clear mode choice, understandable depth-oriented controls, and confidence that familiar scoring and results still apply.

**Conclusion**: Needs splitting

### Split Strategy

**Dimension**: Player journey and gameplay complexity, with each story delivering a visible increment and no more than three core functional points.

| Story | Title | Core Functional Points | Expected Size |
|-------|-------|------------------------|---------------|
| STORY-002-001 | Choose the 3D play mode | Present the mode choice; start the selected mode; preserve 2D as the default alternative | 1–2 days |
| STORY-002-002 | Navigate and score in 3D | Show the bounded 3D space; accept valid movement; collect food and award progression | 3–5 days |
| STORY-002-003 | Complete a 3D session | Handle collisions and restart; pause and resume; retain results across modes | 2–4 days |

The recommended delivery order follows the player's path from choosing a mode through playing and completing a 3D session. Each story remains demonstrable with the existing game shell and preserves the 2D option as a usable alternative.
