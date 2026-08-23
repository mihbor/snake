## STORY-002-002 Navigate and Score in 3D

### Background

After choosing 3D, players need to understand movement through depth and receive the same immediate reward for reaching food as they do in the 2D game. This story makes the 3D space playable and turns movement into visible snake growth and score progression.

### Business Value

- Give players a controllable 3D challenge rather than a static alternate view.
- Make depth-oriented movement understandable through responsive, target-appropriate controls.
- Preserve the familiar food, growth, and scoring loop in the new dimension.

### Dependencies and Assumptions

- **Parent epic**: [Epic-2 Optional 3D Snake Mode](Epic-2-optional-3d-snake-mode.md)
- **Prerequisites**: A player can start a 3D session as described by [STORY-002-001](Story-002-001-choose-the-3d-play-mode.md), and the existing game provides the familiar movement and scoring expectations from [Epic-1 Snake Game](Epic-1-snake-game.md).
- **Data assumptions**: A 3D session begins with a three-segment snake, one food item on an unoccupied cell, and a current score of 0.
- **Integration points**: Desktop players use the available keyboard controls, and touch players use visible controls for movement in all three dimensions.
- **Business constraints**: The snake advances one cell per movement step; an immediate reversal is invalid; each food item adds exactly one segment and 10 points; replacement food must occupy an unoccupied cell.

### Scope In

- Showing a bounded 3D play space with visible depth and a distinguishable snake and food item.
- Moving the snake one cell at a time through the three-dimensional space with valid direction changes.
- Recognizing food collection, growing the snake, increasing the current score, and placing replacement food.

### Scope Out

- The mode-selection experience before a session.
- Boundary and self-collision consequences, game-over presentation, restart, pause, resume, and best-score retention.
- Power-ups, bonus scoring, levels, obstacles, enemies, or multiple food types.

### Acceptance Criteria

#### AC1: The snake can move through all three dimensions

**Given** a 3D session is active and the snake is moving right
**When** the player chooses up, down, forward, backward, or right using the available target control
**Then** the snake changes to that valid direction at the next movement step and advances one cell in the selected direction, with movement toward or away from depth visibly distinguishable.

#### AC2: The snake continues in its last valid direction

**Given** the snake has selected a valid direction in a 3D session
**When** the player provides no further direction
**Then** the snake continues advancing one cell per movement step in its last valid direction.

#### AC3: An immediate reversal is rejected

**Given** the 3D snake is moving right
**When** the player requests left before another valid direction is selected
**Then** the request is ignored and the snake continues moving right without ending the session because of that request alone.

#### AC4: Collecting food grows the 3D snake and awards points

**Given** the 3D snake has three segments, a current score of 0, and its head is one movement step from the food
**When** the snake moves onto the food
**Then** the snake grows from three segments to four, the score becomes 10, and the collected food is no longer shown at that location.

#### AC5: Replacement food remains available

**Given** the player has collected food in a 3D session
**When** the collection is reflected on the play space
**Then** exactly one new food item appears on a cell not occupied by the snake and the player can continue steering toward it.

#### AC6: Repeated collection accumulates progression

**Given** the player has collected two food items without ending the session
**When** the second collection is completed
**Then** the snake has gained two segments from its starting length and the current score is 20.

### Non-Functional Expectations

- Snake, food, depth, current score, and the next valid movement options remain distinguishable during normal play.
- Movement and food collection provide feedback quickly enough for the player to steer during normal play.
