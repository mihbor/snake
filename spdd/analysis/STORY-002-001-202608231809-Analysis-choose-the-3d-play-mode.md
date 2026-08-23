# SPDD Analysis: Choose the 3D Play Mode

## Original Business Requirement

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

## Domain Concept Identification

#### Existing Concepts (from codebase)

- **Game session and lifecycle**: `GameState` is the immutable snapshot exposed by `GameController`, and `SessionStatus` distinguishes `READY`, `ACTIVE`, `PAUSED`, and `GAME_OVER` (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/GameState.kt:3-20`, `composeApp/src/commonMain/kotlin/com/example/snake/game/model/SessionStatus.kt:3-8`). The session already owns the board, snake, direction intent, score, food, and collision result.
- **Bounded 2D board**: `Board` validates positive column and row dimensions and provides the in-bounds concept used by the current game (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/Board.kt:3-13`). The established initializer uses a `20 x 20` board and centers the initial snake (`composeApp/src/commonMain/kotlin/com/example/snake/game/rules/GameRules.kt:17-32`).
- **Planar cell and snake**: `Cell` currently contains only column and row coordinates, while `Snake` stores a head-first ordered list of segments and supports ordinary movement and growth (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/Cell.kt:3-6`, `composeApp/src/commonMain/kotlin/com/example/snake/game/model/Snake.kt:3-17`). These concepts fully describe the existing 2D session but not depth.
- **2D direction intent**: `Direction` currently represents only up, down, left, and right, with one-cell planar offsets and opposite-direction rules (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/Direction.kt:3-20`). `GameRules` centralizes acceptance of direction requests and logical movement (`composeApp/src/commonMain/kotlin/com/example/snake/game/rules/GameRules.kt:62-136`).
- **Initial session invariants**: The shared rules create a three-segment snake, initial direction `RIGHT`, score `0`, and one food cell outside the snake and inside the board (`composeApp/src/commonMain/kotlin/com/example/snake/game/rules/GameRules.kt:17-43`). `GameState` enforces the non-negative score and food occupancy invariants (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/GameState.kt:13-19`).
- **Session controller and clock**: `GameController` creates a ready snapshot, replaces it with a fresh active session on start, exposes the state through a `StateFlow`, and owns movement-clock lifecycle (`composeApp/src/commonMain/kotlin/com/example/snake/game/controller/GameController.kt:24-53`, `117-143`). This is the existing boundary for capturing a mode at session start and preventing stale session activity.
- **Player-facing start and game view**: `GameScreen` presents the ready state, `Start New Game`, current and best scores, the bounded 2D canvas, and target-appropriate control hints and controls (`composeApp/src/commonMain/kotlin/com/example/snake/game/ui/GameScreen.kt:51-131`, `229-339`). `SnakeApp` connects the controller and view in common code (`composeApp/src/commonMain/kotlin/com/example/snake/game/ui/SnakeApp.kt:16-38`).
- **Cross-target input capability**: Desktop exposes keyboard input, Android exposes touch input, and Wasm/browser exposes both through the same `SnakeApp` composition (`composeApp/src/desktopMain/kotlin/com/example/snake/Main.kt:9-17`, `composeApp/src/androidMain/kotlin/com/example/snake/MainActivity.kt:10-18`, `composeApp/src/wasmJsMain/kotlin/com/example/snake/Main.kt:11-16`). `InputCapabilities` currently models only keyboard and touch availability (`composeApp/src/commonMain/kotlin/com/example/snake/game/ui/InputCapabilities.kt:3-6`).
- **Local best score boundary**: Best-score retention is already isolated behind `BestScoreStore` and controller-owned state (`composeApp/src/commonMain/kotlin/com/example/snake/game/persistence/BestScoreStore.kt:3-9`, `composeApp/src/commonMain/kotlin/com/example/snake/game/controller/GameController.kt:35-43`). It is explicitly outside this story and should remain a single shared record rather than become mode-specific.

#### New Concepts Required

- **Play mode**: The player-facing dimension choice with the two allowed values, 2D and 3D; it governs which initial play-space representation is created without changing the familiar 2D rules.
- **Pre-session mode selection**: The temporary choice shown in the ready/start experience, including its default and selected-state meaning. It belongs to the next-session decision rather than to an already active attempt.
- **Session mode identity**: The mode captured when a new session starts and retained for that session's lifetime. It is the authoritative answer to which dimension is active and must not be replaced by later selector input.
- **Bounded 3D play space**: The new session-level spatial concept that has visible depth, a finite set of valid locations, and enough capacity for the initial snake and food. Its navigation and scoring behavior belong to the subsequent 3D gameplay story.
- **Mode-aware initial presentation**: The player-visible initial state that combines the selected mode, its bounded board or space, the three-segment snake, one valid food item, score `0`, and controls understandable for that mode.

#### Conceptual Relationships

- The start experience owns the pre-session mode selection. With no applicable prior choice, it selects 2D so the existing experience remains the default.
- Starting a new game transfers the selected mode into one session mode identity. The session then owns the selected spatial representation, snake, food, score, and lifecycle; the selector must not remain an alternate authority over the active snapshot.
- A 2D session continues to use the existing bounded board, planar snake, food, score, and control concepts. A 3D session adds bounded depth and a depth-aware initial presentation while sharing the same session-start invariants.
- The controller coordinates selection-to-session creation and continues to expose one observable session state. The common view presents the active mode and mode-specific initial space, while target entry points continue to supply only platform capabilities.
- A mode-selection attempt during an active session is not a gameplay transition. It must leave the active session mode, snake, food, score, and progress unchanged and may affect only a later-session selection according to the clarified lifecycle policy.
- Best-score state remains independent of the mode identity. This story does not change collection, completion, restart, pause, or score-retention behavior already covered by other stories.

#### Key Business Rules

- Before a session starts, both 2D and 3D are visible as mutually understandable choices and the unchosen/default state is 2D.
- A player may choose one mode before starting; the selected value determines the next new session and is visibly identifiable when that session opens.
- Every new session begins with exactly three snake segments, exactly one food item on an unoccupied valid cell, and current score `0`, regardless of selected mode.
- A 3D session must expose a bounded space with visible depth and controls appropriate to depth-oriented play, but ordinary 3D movement and its progression rules remain outside this story.
- A 2D session must preserve the established board, initial arrangement, food invariant, score initialization, and available 2D controls without redesigning its gameplay rules.
- Once a session is active, its mode is fixed. Attempts to select another mode cannot change any active session state and can take effect only at a later-session boundary.
- Mode selection and both initial session types work locally without account or network access.
- The active mode must be represented as player-understandable state, not inferred from an incidental renderer or from which control was most recently pressed.

## Strategic Approach

#### Solution Direction

- Extend the shared `commonMain` session contract with a mode identity and a mode-aware start boundary. Keep the selected mode in the authoritative session state once a session begins, while keeping the pre-session selection available to the ready experience until the next session is created.
- Add the 2D/3D choice to the existing ready/start presentation and make the selected value explicit in both text and accessible state. Reuse the existing `GameController` as the single owner of new-session creation and clock ownership rather than introducing platform-specific mode coordinators.
- Reuse the current 2D initialization and rendering path for 2D so its `20 x 20` arrangement, three-segment snake, food exclusion, score `0`, and keyboard/touch behavior remain unchanged. The mode feature should add a selection boundary, not fork or redesign the established rules.
- Establish a distinct bounded 3D initial presentation that visibly communicates depth and includes the same initial snake, food, score, and mode identity. Keep the spatial representation compatible with the next story's navigation work, but do not add ordinary 3D movement, food collection, collision, pause, restart, or best-score behavior here.
- Keep mode semantics and initial-state invariants in shared code so desktop, Android, and Wasm/browser produce equivalent results for the same selection. Continue using each target's existing capability boundary for keyboard and touch presentation.
- Treat the mode selector as a pre-session interaction. Once the session is active, the session-owned mode is authoritative and the selector is disabled or otherwise inert; a later session boundary is the only place where a new selection can take effect.
- Validate the change with common deterministic checks for default selection, each mode's initial invariants, active-mode immutability, and 2D regression, plus target-level checks that labels, controls, and visible depth remain understandable on desktop, Android, and browser configurations.

#### Key Design Decisions

- **Session-owned mode versus view-only selection**: A view-only value is easy to add but can disagree with the active board and can appear to change a running session. Capture the selection as part of the immutable session identity and keep the pre-session choice separate; this costs a small model boundary but makes AC4 enforceable and observable.
- **One shared session lifecycle versus separate 2D and 3D game loops**: Independent loops could make the 3D prototype quicker, but would duplicate start, status, score, and input gating and invite cross-target divergence. Keep one lifecycle and common initialization invariants, with mode-specific spatial presentation and future movement policies at the relevant boundary.
- **Reuse the established 2D path versus normalize both modes to a replacement abstraction immediately**: Replacing the current 2D board would increase regression risk and contradict the story's preservation promise. Reuse the current 2D behavior and introduce only the abstraction needed to describe a mode-aware initial space; defer broader spatial refactoring until 3D navigation proves it necessary.
- **Durable last-choice preference versus session-local selection**: Persisting the last mode could satisfy one interpretation of “previous choice” but introduces storage semantics that the story does not explicitly request and that differ across the existing targets. Prefer 2D as the default when no current selection exists and keep the choice at the next-session boundary until product clarifies relaunch persistence.
- **Locking mode during active play versus allowing a pending next-session choice**: Allowing the selector to mutate the active state would violate AC4. Lock the effective session mode during active play; if a future selection is allowed while a terminal screen is shown, keep it explicitly separate from the completed session and define that behavior with the lifecycle story.
- **Shared game view with mode-specific rendering versus separate application screens**: Separate screens could simplify the visual prototype but would duplicate score, lifecycle, accessibility, and target wiring. Keep the existing game-view composition boundary and let it choose a mode-appropriate initial presentation, including a real bounded-depth visual treatment rather than a 2D board with only a changed label.
- **Visible 3D control affordances now versus implementing 3D movement now**: Omitting controls would fail AC2's discoverability expectation, while implementing movement would consume the explicitly out-of-scope gameplay work assigned to `STORY-002-002`. Show and label the controls needed to understand the 3D session, while keeping their movement semantics and full interaction contract for the next story.

#### Alternatives Considered

- **Make 3D the only or new default experience**: Rejected because the story and parent epic require the familiar 2D mode to remain available and default when no choice exists.
- **Keep the mode only in Compose UI state**: Rejected because the controller and domain state would not have an authoritative mode, allowing the selector and active session to diverge.
- **Implement a separate 3D application or target-specific game loop**: Rejected because the project already shares rules and composition across Android, desktop JVM, and Wasm/browser; duplication would threaten equivalent session behavior.
- **Persist the player's last mode immediately**: Rejected as the default because the persistence lifetime and cross-target semantics are not specified, and durable best-score storage is an unrelated existing boundary.
- **Start a 3D session as a labeled 2D board**: Rejected because AC2 requires visible depth and a bounded 3D play space, not merely a different mode label.
- **Add 3D navigation and scoring in this story**: Rejected because movement, food collection, progression, and later lifecycle outcomes are explicitly assigned to subsequent stories.
- **Hide all controls until 3D movement is implemented**: Rejected because a player must see controls appropriate to the selected mode when the initial session opens; the current story can establish the affordance without owning the movement rules.

## Risk & Gap Analysis

#### Requirement Ambiguities

- **Meaning of “no previous choice exists”**: It is unclear whether a prior choice means a selection made earlier in the same ready screen, a choice used for the previous session, or a persisted choice after relaunch. The default and any persistence lifetime need one product rule.
- **Mode-selector lifecycle**: AC4 forbids changing the active session, but the story does not say whether the selector is hidden, disabled, or visibly inert during `ACTIVE` and `PAUSED`, or whether it becomes available after `GAME_OVER`. This intersects the later restart and completion stories.
- **Start interaction and ready-state preview**: The current application opens in `READY` and already renders a generated 2D snapshot before `Start New Game` (`GameController.kt:35-36`, `GameScreen.kt:122-131`). The story does not state whether selecting 3D must immediately change that preview or only affects the session created by the start action.
- **Definition of visible 3D depth**: “Bounded 3D play space with visible depth” does not define the visual representation, projection, camera perspective, minimum discernible depth, or accessibility description. A 2D projection, layered view, or other presentation could satisfy the words differently.
- **Initial 3D dimensions and placement**: The story does not specify the depth capacity, initial coordinates/orientation, deterministic versus random food placement, or the minimum capacity needed for three segments plus one food item in 3D.
- **Meaning of controls appropriate for 3D movement**: The criterion requires controls to be shown while scope out excludes moving the snake through 3D space. Clarify whether controls only need to be visible and labeled in this story or must already accept input, and reserve the latter's direction semantics for `STORY-002-002`.
- **Target control mapping**: The project currently maps four planar directions and has keyboard, touch, and hybrid target capability flags. The story does not define the keyboard keys, touch layout, focus behavior, or accessibility wording for forward/backward depth controls.
- **Active-session attempt feedback**: AC4 says the state remains unchanged but does not say whether an attempted mode change should be rejected silently, produce feedback, or offer a pending choice for a later session.
- **Selection precedence**: If the player changes the choice repeatedly before starting, or selects a mode while a start action is being processed, the story does not state whether the last visible choice wins and how duplicate starts are handled.
- **Mode label scope**: The requirement says the player can identify the selected mode from session start, but does not specify whether the label must remain visible during the whole session or only in the initial view.

#### Edge Cases

- A player opens the app with no mode history, verifies that 2D is selected, and starts without touching the selector; the existing 2D initial state must remain unchanged.
- A player selects 3D, changes back to 2D before starting, and starts once; only the final pre-session selection should determine the created session, without creating competing clocks or stale previews.
- A player selects 3D and attempts to select 2D repeatedly after the session becomes active; mode, board/space, snake, food, score, progress, and active clock must remain unchanged.
- A player tries to change mode while paused or immediately at a terminal transition; the story's “active session” wording leaves the expected lock and later-session availability open.
- A player selects 3D, leaves the view, reloads the browser, recreates an Android activity, or reopens desktop; the selected-mode retention behavior must not be confused with the already-defined best-score persistence behavior.
- A 3D initial space is too small to contain the three-segment snake and one unoccupied food cell; initialization must fail safely or use a product-approved capacity, never violate the food invariant.
- Random or repeated initialization produces food on a snake segment or outside the valid 3D space; the existing unoccupied-food invariant must hold for the new spatial representation.
- A narrow, rotated, resized, or touch-only screen cannot show the depth cue and controls together; the initial view must preserve readable mode labels, visible depth, and reachable controls.
- A hybrid browser target exposes both keyboard and touch capabilities, while Android and desktop expose one primary surface; all should communicate the same selected mode and initial state.
- An input event arrives before start, during mode selection, or immediately after start; it must not mutate a `READY` choice into active gameplay or alter the selected session mode unexpectedly.
- The existing best score is nonzero while a new mode is selected; the current score must reset to `0` and the unrelated best-score value must not be split, cleared, or silently replaced by a mode-specific record.
- A user uses assistive technology or keyboard focus to select a mode; selected/unselected state and the start action must remain understandable without relying only on color or depth imagery.

#### Technical Risks

- **Planar model does not represent depth**: `Cell` has only column and row, `Board` has only columns and rows, and `Direction` has four planar values (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/Cell.kt:3-6`, `Board.kt:3-13`, `Direction.kt:3-20`). A mode addition that merely relabels the current model cannot support a genuine bounded 3D initial space or the next story's navigation. The spatial boundary should be chosen now without unnecessarily breaking the established 2D contracts.
- **Mode authority can split across controller, state, and UI**: `GameController` currently starts every session through one 2D initializer, while `GameScreen` branches on lifecycle and renders a single 2D canvas (`composeApp/src/commonMain/kotlin/com/example/snake/game/controller/GameController.kt:45-53`, `GameScreen.kt:122-225`). If selection remains only in the view, AC4 can be violated or the mode label can disagree with the rendered session. Capture the effective mode at the shared start boundary and expose it from the session state.
- **Ready-state snapshot may show the wrong dimension**: The controller constructs a ready state by creating a complete 2D game and changing only its status (`GameController.kt:145-146`). If the selection changes but the preview is not updated deliberately, the player may see 2D while expecting 3D, or a 3D renderer may receive a stale planar snapshot. Detailed design must define whether ready state previews are mode-aware or intentionally neutral.
- **3D rendering without an existing graphics layer**: The build currently uses Compose Foundation, Material 3, Compose UI, runtime, coroutines, and target-specific platform dependencies; no 3D rendering dependency or abstraction is present (`composeApp/build.gradle.kts:38-61`, `gradle/libs.versions.toml:12-22`). A projection or layered visual must remain usable and cross-target without introducing unjustified technology or making the initial story depend on a heavy rendering stack.
- **Responsive and accessible depth presentation**: The current `GameScreen` sizes a planar canvas and touch controls adaptively, but a depth cue, mode label, and 3D control set will consume more visual and interaction space (`GameScreen.kt:78-101`, `237-333`). Poor scaling could make depth indistinguishable or controls unreachable on Android and browser sizes; textual state and semantics must supplement visual depth.
- **Input model mismatch**: `KeyboardDirectionMapper` and `GameKey` currently support four movement directions plus pause keys (`composeApp/src/commonMain/kotlin/com/example/snake/game/input/GameKey.kt:3-14`, `KeyboardDirectionMapper.kt:5-12`). Showing 3D controls now while deferring movement requires a clear boundary so future depth inputs do not accidentally enter planar rules or bypass the common reversal policy.
- **Cross-target divergence at composition boundaries**: The three target entry points all pass capabilities and a target-local best-score store into the common app, but they do not currently carry mode information (`composeApp/src/androidMain/kotlin/com/example/snake/MainActivity.kt:13-17`, `desktopMain/kotlin/com/example/snake/Main.kt:14-17`, `wasmJsMain/kotlin/com/example/snake/Main.kt:13-16`). Mode logic added in one entry point would make equivalent selections behave differently; keep it in common state and presentation.
- **Lifecycle and start races**: `GameController` cancels the previous movement job and uses a session generation to guard clock ticks when starting, pausing, resuming, or closing (`GameController.kt:45-53`, `72-85`, `128-143`). Mode selection must be captured before that transition and must not create a second session, let a stale tick affect the new mode, or allow a post-start selector event to rewrite the active state.
- **Invariant preservation across spatial representations**: `GameState` currently requires an in-bounds food cell outside the snake, and common tests assert the three-segment start arrangement and food validity (`GameState.kt:13-19`, `composeApp/src/commonTest/kotlin/com/example/snake/game/GameRulesTest.kt:61-80`). A 3D equivalent must maintain exactly one unoccupied food item and score `0` while making its valid-space boundary meaningful.
- **Verification gap at the UI boundary**: Existing common tests cover 2D initialization, controller start behavior, labels, and keyboard mapping (`GameRulesTest.kt:61-80`, `GameControllerTest.kt:31-70`, `GameScreenTest.kt:13-38`), but no tests cover a mode selector, selected semantics, mode-specific initialization, active-mode immutability, or 3D visibility. The implementation phase will need deterministic common tests and target interaction/accessibility checks without weakening current 2D tests.
- **Scope leakage into later stories**: The 3D view, controls, and spatial model can easily pull in navigation, food progression, collision, pause, restart, or best-score changes. Keep this story's acceptance boundary at selection and initial presentation, and make any shared abstraction extensible without implementing later gameplay behavior prematurely.

#### Acceptance Criteria Coverage

| AC# | Description | Addressable? | Gaps/Notes |
|-----|-------------|--------------|------------|
| AC1 | Before a session, the player sees clear 2D and 3D choices, with 2D selected when no previous choice exists. | Partial | The existing `READY` start experience provides a clear start action but no mode selector or mode concept. The meaning and persistence lifetime of “previous choice,” selected-state semantics, and exact selector lifecycle need clarification. |
| AC2 | Selecting 3D starts a bounded, visibly deep session with three segments, one unoccupied food item, score `0`, and appropriate 3D controls. | Partial | The shared initializer already establishes the three-segment, food, and score invariants for 2D, but the repository has no depth-aware spatial model or renderer, and the control criterion conflicts with movement being out of scope. The proposed shared mode-aware start boundary can address the normal case after those design choices are fixed. |
| AC3 | Selecting 2D, or leaving the default unchanged, preserves the existing 2D board, initial state, food, score, and controls. | Yes | The current common rules, controller, view, and target capability wiring already provide the required 2D baseline. Mode selection should wrap that path without changing its initialization or gameplay semantics; regression coverage is still required. |
| AC4 | Attempting to choose 2D during an active 3D session leaves the complete 3D session unchanged and permits change only for a later session. | Partial | The immutable `GameState`, active-only gameplay gates, and controller generation boundary provide useful foundations, but no mode identity or selector lock exists. The treatment of attempts during paused/game-over states and any user feedback remains unspecified. |

All four acceptance criteria are structurally addressable through the existing shared-state architecture, with AC1, AC2, and AC4 requiring product clarification and new mode/depth presentation concepts. No in-scope requirement is being deferred; movement, progression, collision, pause/resume, restart, and best-score behavior remain explicitly assigned outside this story.