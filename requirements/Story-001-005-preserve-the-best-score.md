## STORY-001-005 Preserve the Best Score

### Background

Players need a simple measure of improvement across attempts. Retaining the highest completed score encourages repeat play while keeping the game self-contained and free of account or network requirements.

### Business Value

- Give players a persistent personal goal beyond completing one session.
- Make the difference between the current attempt and the best result easy to understand.
- Preserve progress across restarts and later launches without requiring sign-in.

### Dependencies and Assumptions

- **Parent epic**: [Epic-1 Snake Game](Epic-1-snake-game.md)
- **Prerequisites**: The game can calculate a final score and start a new session as described by [STORY-001-003](Story-001-003-finish-and-restart-a-game.md).
- **Data assumptions**: A player with no completed session has a best score of 0; only a completed session can establish a new best score.
- **Integration points**: The best score is available in the game view on the same supported target after a later launch; no account or network service is required.
- **Business constraints**: A lower score never replaces a higher score, and starting a new session resets only the current score.

### Scope In

- Showing the current score and best score together during play.
- Updating the best score when a completed session exceeds it.
- Retaining the best score after restart and after the application is opened again.

### Scope Out

- Online leaderboards or sharing scores with other players.
- Multiple player profiles or account management.
- Achievements, rankings, or rewards beyond the best score.

### Acceptance Criteria

#### AC1: A player with no history starts at zero

**Given** the player has no completed session on the target
**When** the game view is opened
**Then** the current score and best score are both shown as 0.

#### AC2: A new high score is recorded

**Given** the current best score is 40
**When** the player completes a session with a score of 50
**Then** the best score changes to 50 and the new value is visible with the final result.

#### AC3: A lower score does not replace the best score

**Given** the current best score is 50
**When** the player completes another session with a score of 30
**Then** the best score remains 50 while the completed session's final score remains 30.

#### AC4: Restart resets only the current score

**Given** the best score is 50 and a game-over state is displayed
**When** the player starts a new session
**Then** the current score is 0 and the best score remains 50.

#### AC5: The best score survives a later launch

**Given** the player has established a best score of 50
**When** the player closes and later reopens the application on the same target
**Then** the best score is still shown as 50 before the next session is completed.

### Non-Functional Expectations

- Current score and best score are labeled distinctly so a player cannot confuse an attempt with their record.
- Best-score retention does not require the player to create an account or connect to a network.
