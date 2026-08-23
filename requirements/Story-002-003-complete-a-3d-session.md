## STORY-002-003 Complete a 3D Session

### Background

A 3D game is complete only when players can pause an attempt, understand why it ended, start another 3D attempt, and carry their personal result forward. This story applies the existing session-lifecycle expectations to the 3D play space and keeps the player's best result continuous across both dimensions.

### Business Value

- Give players a clear and fair outcome when movement reaches a 3D boundary or the snake's body.
- Let players pause a 3D attempt safely and resume without losing progress.
- Make it quick to try the 3D challenge again while preserving the personal best from either mode.

### Dependencies and Assumptions

- **Parent epic**: [Epic-2 Optional 3D Snake Mode](Epic-2-optional-3d-snake-mode.md)
- **Prerequisites**: A 3D session can move and collect food as described by [STORY-002-002](Story-002-002-navigate-and-score-in-3d.md); the existing game provides the established collision, pause, restart, and best-score expectations from [Epic-1 Snake Game](Epic-1-snake-game.md).
- **Data assumptions**: A collision occurs when the 3D snake's head reaches any face of the bounded play space or a cell occupied by its body; a player with no completed session has a best score of 0.
- **Integration points**: The 3D view presents pause, resume, restart, game-over, current-score, and best-score feedback on each supported target.
- **Business constraints**: A game-over session cannot move or collect food until restarted; pausing preserves the current 3D state; a lower completed score never replaces a higher best score; the selected mode remains 3D after restart.

### Scope In

- Ending a 3D session after a boundary or self-collision and showing the final result.
- Pausing and resuming an active 3D session without changing its state.
- Restarting a 3D session and retaining the player's best score across 3D, 2D, and later launches.

### Scope Out

- Choosing the play mode before a session starts.
- 3D movement, food placement, growth, and score awards during ordinary play.
- Lives, continues, undo, recovery after a collision, online leaderboards, or multiple player profiles.

### Acceptance Criteria

#### AC1: Reaching any 3D boundary ends the session

**Given** the 3D snake is moving toward any face of the bounded play space
**When** its head attempts to move beyond the last available cell at that face
**Then** the session ends, the snake stops moving, and a clear game-over state is shown.

#### AC2: Hitting the 3D snake's body ends the session

**Given** the 3D snake has at least four segments and its head is directed toward an occupied body cell
**When** the head enters that cell
**Then** the session ends, the snake stops moving, and a clear game-over state is shown.

#### AC3: A paused 3D session keeps its state

**Given** an active 3D session has a current score of 20
**When** the player chooses pause and at least three normal movement steps pass while the player provides directional input
**Then** the 3D space, snake, food, and score remain unchanged and a clear Paused state is shown.

#### AC4: The same 3D session can be resumed

**Given** a 3D session is paused with the snake and food in known positions
**When** the player chooses resume
**Then** the Paused state is removed and the snake continues from the same position and direction with the score still at 20.

#### AC5: Restart begins another 3D attempt

**Given** a 3D game-over state is displayed with a final score of 30
**When** the player chooses restart
**Then** a new 3D session begins with a three-segment snake, a current score of 0, exactly one food item on an unoccupied cell, and the 3D mode still selected.

#### AC6: A finished 3D session cannot be changed by gameplay input

**Given** the 3D game-over state is displayed
**When** the player presses a direction or uses a directional control
**Then** the snake does not move, the score does not increase, and the game-over state remains until restart is chosen.

#### AC7: A 3D result can establish the player's best score

**Given** the player's current best score is 40
**When** the player completes a 3D session with a score of 50
**Then** the best score changes to 50 and the value is visible with the final result.

#### AC8: The best score remains continuous between modes

**Given** the player's best score is 50 after a 3D session
**When** the player completes a 2D session with a score of 30 or starts another session
**Then** the best score remains 50 while the completed or new session's current or final score reflects its own result.

### Non-Functional Expectations

- The cause of a 3D game-over outcome and the next available action are understandable without reading technical documentation.
- Pause, resume, restart, current score, and best score remain findable without leaving the game view.
