## STORY-002-001 Choose the 3D Play Mode

### Background

The existing game starts in a familiar 2D view, but players who want an alternative challenge need a clear way to choose 3D before play begins. This story adds the player-facing mode choice and opens a session in the selected dimension without disrupting the established 2D option.

### Business Value

- Let players choose the type of game they want to play before committing to a session.
- Make the new 3D capability discoverable without removing the familiar 2D experience.
- Ensure a player can identify the selected mode from the moment a session starts.

### Dependencies and Assumptions

- **Parent epic**: [Epic-2 Optional 3D Snake Mode](Epic-2-optional-3d-snake-mode.md)
- **Prerequisites**: The existing 2D game-start flow from [Epic-1 Snake Game](Epic-1-snake-game.md) is available.
- **Data assumptions**: A new session starts with a three-segment snake, one food item on an unoccupied cell, and a current score of 0.
- **Integration points**: The mode choice is shown in the game's start experience on every supported target.
- **Business constraints**: 2D is the default when no choice has been made; the selected mode applies to the next session and cannot change during an active session.

### Scope In

- Showing 2D and 3D as available play-mode choices before a session starts.
- Starting a session in the mode selected by the player.
- Showing the selected mode's initial board, snake, food, score, and available controls.
- Keeping 2D available as the default alternative to 3D.

### Scope Out

- Moving the snake through 3D space after the session starts.
- Food collection, score increases, collision outcomes, pause, resume, and best-score retention.
- Changing the mode while a session is active.
- Removing or redesigning the existing 2D gameplay rules.

### Acceptance Criteria

#### AC1: Both play modes are offered before a session

**Given** the application is open before a game starts
**When** the player views the start experience
**Then** the player can clearly choose between 2D and 3D, and 2D is selected by default when no previous choice exists.

#### AC2: Selecting 3D opens a 3D session

**Given** the player has selected 3D
**When** the player chooses to start a new game
**Then** the game opens in a bounded 3D play space with visible depth, a three-segment snake, exactly one food item on an unoccupied cell, a current score of 0, and controls appropriate for 3D movement.

#### AC3: Selecting 2D keeps the familiar start experience

**Given** the player has selected 2D or has left the default choice unchanged
**When** the player chooses to start a new game
**Then** the game opens with the existing 2D board, a three-segment snake, exactly one food item on an unoccupied cell, a current score of 0, and the controls already available for 2D play.

#### AC4: The mode does not change during an active session

**Given** a 3D session is active
**When** the player attempts to choose 2D
**Then** the current session remains in 3D with its snake, food, score, and progress unchanged, and the player can change mode only for a later session.

### Non-Functional Expectations

- Mode labels and the selected state are understandable before the player starts a session.
- A player can reach either mode without an account or network connection.
