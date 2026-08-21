## STORY-001-002 Eat Food and Build Score

### Background

A snake game needs a visible objective that rewards movement. Food gives the player a clear short-term goal, makes the snake grow, and turns survival into a measurable score chase.

### Business Value

- Give players a concrete reason to steer the snake around the board.
- Make successful play visible through growth and score feedback.
- Create a repeatable scoring loop that supports competition with the player's own best result.

### Dependencies and Assumptions

- **Parent epic**: [Epic-1 Snake Game](Epic-1-snake-game.md)
- **Prerequisites**: A session can display a board and move a snake as described by [STORY-001-001](Story-001-001-start-and-control-the-snake.md).
- **Data assumptions**: A new session starts with one food item on an unoccupied cell and a score of 0.
- **Integration points**: Food and score are presented within the game view; no account or network service is required.
- **Business constraints**: Each collected food item awards exactly 10 points and adds exactly one snake segment.

### Scope In

- Showing one food item in the active session.
- Recognizing food collection and adding one snake segment.
- Increasing and displaying the current score by 10 for every collected food item.

### Scope Out

- Collision consequences and the game-over state.
- Pause and resume controls.
- Power-ups, bonus scoring, levels, or multiple food types.

### Acceptance Criteria

#### AC1: Food gives the player a visible target

**Given** a new session has started with a three-segment snake and a score of 0
**When** the board is displayed
**Then** exactly one food item is visible on a cell that is not occupied by the snake.

#### AC2: Collecting food grows the snake and awards points

**Given** the snake's head is one movement step from the food and the score is 0
**When** the snake moves onto the food
**Then** the snake grows from three segments to four, the score changes to 10, and the collected food is no longer shown at that location.

#### AC3: A replacement food item appears after collection

**Given** the player has collected food
**When** the collection is reflected on the board
**Then** one new food item appears on an unoccupied cell and the player can continue steering toward it.

#### AC4: Repeated collection accumulates score

**Given** the player has collected two food items without ending the session
**When** the second collection is completed
**Then** the snake has gained two segments from its starting length and the current score is 20.

#### AC5: Food never occupies the snake

**Given** the snake occupies one or more board cells
**When** a food item is first placed or replaced
**Then** the food is visible on a different, unoccupied cell and remains reachable within the bounded board.

### Non-Functional Expectations

- The score and snake growth are visible immediately after each collection.
- Food is visually distinct from the snake and the board so that the next objective is obvious.
