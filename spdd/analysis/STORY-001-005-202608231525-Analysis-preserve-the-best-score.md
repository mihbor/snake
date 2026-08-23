# SPDD Analysis: Preserve the Best Score

## Original Business Requirement

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

## Traceability

- **Source requirement**: `requirements/Story-001-005-preserve-the-best-score.md`
- **Prerequisite lifecycle analysis**: `spdd/analysis/STORY-001-003-202608231038-Analysis-finish-and-restart-a-game.md`
- **Related pause/resume analysis**: `spdd/analysis/STORY-001-004-202608231344-Analysis-pause-and-resume-play.md`

## Domain Concept Identification

#### Existing Concepts (from codebase)

- **Current game session**: `GameState` is the immutable snapshot for one attempt, owning `status`, board, snake, direction intent, current `score`, food, and collision cause (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/GameState.kt:3-20`). Its score is non-negative, but the snapshot currently has no best-score field or durable-record reference.
- **Session lifecycle**: `SessionStatus` distinguishes `READY`, `ACTIVE`, `PAUSED`, and `GAME_OVER` (`composeApp/src/commonMain/kotlin/com/example/snake/game/model/SessionStatus.kt:3-8`). The existing lifecycle therefore identifies the terminal boundary at which a completed score can be evaluated, while pause and ready states must not establish a record.
- **Score progression**: `GameRules.advance` adds exactly 10 points when an active snake collects food and otherwise preserves the current score (`composeApp/src/commonMain/kotlin/com/example/snake/game/rules/GameRules.kt:85-136`). This is the authoritative source of an attempt's current score and must remain separate from the personal record.
- **Terminal result and restart**: A collision changes an active state to `GAME_OVER` while retaining its score, and `GameRules.startNewGame` creates a three-segment active session with score `0` (`composeApp/src/commonMain/kotlin/com/example/snake/game/rules/GameRules.kt:96-109`, `139-146`, `17-43`). These transitions provide the completion and reset seams required by AC2–AC4.
- **Controller and observable state**: `GameController` owns the mutable `StateFlow<GameState>`, delegates transitions to `GameRules`, starts and stops the movement clock, and replaces the session on `startNewGame` (`composeApp/src/commonMain/kotlin/com/example/snake/game/controller/GameController.kt:22-47`, `84-115`). It currently creates only in-memory state and has no persistence dependency or best-score flow.
- **Game-view presentation**: `GameScreen` renders one `Score: ...` value and changes its accessibility description between current and final score, but it does not receive or display a best score (`composeApp/src/commonMain/kotlin/com/example/snake/game/ui/GameScreen.kt:51-115`). Its lifecycle branches already keep the final score visible in `GAME_OVER` and offer restart in the same view (`composeApp/src/commonMain/kotlin/com/example/snake/game/ui/GameScreen.kt:118-215`).
- **Application composition boundary**: `SnakeApp` remembers a default `GameController` and passes its state to `GameScreen` without a storage object (`composeApp/src/commonMain/kotlin/com/example/snake/game/ui/SnakeApp.kt:14-36`). This is the injection point for a target-specific durable store or for a controller constructed with one.
- **Supported target set**: The Multiplatform build configures Android, desktop JVM, and Wasm/browser targets (`composeApp/build.gradle.kts:27-63`). Android, desktop, and Wasm entry points each create the common `SnakeApp` with target-specific input capabilities (`composeApp/src/androidMain/kotlin/com/example/snake/MainActivity.kt:9-15`, `composeApp/src/desktopMain/kotlin/com/example/snake/Main.kt:8-14`, `composeApp/src/wasmJsMain/kotlin/com/example/snake/Main.kt:9-13`).
- **Available platform facilities**: The project has only Compose, coroutines, Android activity, and `kotlinx.browser` dependencies (`composeApp/build.gradle.kts:38-61`, `gradle/libs.versions.toml:12-16`); no persistence library or storage abstraction exists. The target APIs can provide a small local implementation without an account or network: Android preferences, desktop user preferences, and browser origin-local storage are candidate adapters.
- **Existing verification seams**: Common tests already assert score changes after food collection and final-score retention on collisions, and controller tests assert restart creates a zero-score active session (`composeApp/src/commonTest/kotlin/com/example/snake/game/GameRulesTest.kt:187-200`, `261-309`, `composeApp/src/commonTest/kotlin/com/example/snake/game/GameControllerTest.kt:239-305`). There are no tests for a durable record, later controller construction, or paired current/best-score presentation.

#### New Concepts Required

- **Personal best score**: A non-negative scalar representing the highest score from a completed session for the local player on one target. It is distinct from the current attempt score and defaults to `0` when no valid history exists.
- **Completed-score evaluation**: A shared lifecycle event or controller boundary that evaluates the final score only when a session reaches `GAME_OVER`; active food collection, pause, restart, and application close do not by themselves establish a record.
- **Best-score persistence contract**: A small common abstraction that reads the saved record at application/controller startup and writes a strictly higher completed score. The contract must be injectable so common tests can use an in-memory fake without platform APIs.
- **Target-local storage adapter**: Per-target implementation of the common contract using the platform's local durable storage, scoped to the application identity/origin and not to an account or server.
- **Paired score presentation**: A game-view data path that exposes both the current/final attempt score and the best score at the same time, with distinct labels and accessibility semantics in ready, active, paused, and game-over states.

#### Conceptual Relationships

- One application target owns one local personal best record, while each `GameState` represents only the current attempt. Restart replaces the attempt snapshot but does not reset the record.
- `GameRules` remains the authority for current score progression and terminal transitions. When its result becomes `GAME_OVER`, `GameController` compares the retained record with the final `state.score` and accepts the maximum.
- A new high score must update the in-memory value and durable store at the same completion boundary, then be observable with the final result. A lower or equal completion changes neither the stored record nor the current final score.
- The initial controller/view reads the record before showing the game state. With no record, the view shows current score `0` and best score `0`; with history, it shows current score `0` and the loaded best before the next session is completed.
- Closing and reopening restores only the scalar best score. It must not restore the prior snake, food, pending direction, pause state, current score, or game-over snapshot; this preserves the pause story's explicit session-restoration exclusion.
- Android, desktop JVM, and Wasm/browser adapters implement the same read/write semantics. The common controller and rules determine when a value is eligible, so equivalent gameplay sequences do not diverge by target.

#### Key Business Rules

- A missing best-score record is interpreted as `0`.
- Only a completed session, represented by the established `GAME_OVER` transition, can establish or improve the best score. An active session with a temporarily high current score is not persisted until it completes.
- When a completed score is greater than the current best, the best becomes exactly that final score and is persisted; the final current score remains unchanged.
- When a completed score is lower than or equal to the current best, the best remains unchanged and no replacement record is required.
- A lower completed score remains visible as that attempt's final score even when the best score is higher; the two values must never be collapsed into a single maximum display.
- Starting or restarting a session sets only the current score through the established fresh-session initializer. It preserves the loaded/in-memory best score and must not clear or rewrite it to `0`.
- A paused session retains its current score but cannot establish a new best merely by being paused, resumed, or closed; only a later completed session can qualify.
- The record is local to the same target/application storage identity. Accounts, synchronization, leaderboards, cross-device sharing, and network access are not part of this story.
- Stored values must satisfy the non-negative score invariant. Missing, malformed, or negative stored data must not crash startup; the safe baseline is `0`, subject to the final storage error policy.
- Best-score reads and writes must be serialized with completion and restart actions so a stale terminal tick or immediate restart cannot overwrite a newer record.
- The current and best values must be labeled distinctly in visible text and accessibility semantics in every game lifecycle where they are shown.

## Strategic Approach

#### Solution Direction

- Preserve `GameState.score` as the score for the current attempt and keep persistence out of `GameRules`. Add a small common `BestScoreStore` contract and inject it into `GameController`, allowing the controller to load a best value at construction and expose it to `GameScreen` alongside the session state.
- Use the existing `GAME_OVER` transition as the single completion boundary. After `GameRules.advance` returns a terminal state, compare its final score with the controller's loaded best; only a strict increase updates the in-memory record and durable store. Ordinary food collection continues to change only the current score.
- Keep score evaluation and publication serialized in the controller. The screen should observe a coherent pair of current/final score and best score, especially when a collision creates a new record or when restart follows immediately. A small presentation-state aggregate may be used if two independent flows could otherwise expose a transient mismatch.
- Implement target adapters behind the common contract rather than adding persistence logic to platform entry points or the UI. Android can use app-local preferences, desktop can use user-scoped JVM preferences, and Wasm/browser can use origin-local storage through the existing browser dependency.
- Pass the target store through the existing `SnakeApp`/`GameController` composition boundary, while retaining constructor injection for tests. The default ready state should load the best record but continue to initialize the current attempt with score `0`.
- Extend `GameScreen` with distinct current/final and best labels. During play and pause it should show the current score and best score together; after game over it should show the final score and best score, including a newly established record, without replacing the terminal presentation or restart action.
- Keep storage synchronous or otherwise fully initialized before the first game-view score is rendered unless a new loading state is explicitly justified. A single integer does not warrant a new persistence framework or a network service in this project.

#### Key Design Decisions

- **Separate record versus session field**: Putting the best into the snake's session semantics risks resetting or restoring it with the attempt, while a separate controller-owned record naturally survives restart and launch. Prefer a separate persisted record exposed with the view state; if it is copied into a presentation snapshot, it must remain clearly distinct from `GameState.score`.
- **Controller completion boundary versus rule-side storage**: Storage is an integration concern and would make the pure shared rules platform-aware and hard to test. Prefer having `GameRules` return the terminal score and having `GameController` apply the record policy at the one place that already serializes state and clock transitions.
- **Strict maximum versus unconditional overwrite**: Overwriting on every game over would violate AC3, while comparing with `maxOf` makes lower and equal results idempotent. Prefer updating only when `completedScore > bestScore`.
- **Terminal-only update versus live tracking**: Updating the record whenever the current score increases would allow an unfinished or paused session to establish history, contradicting the data assumption. Prefer recording only at `GAME_OVER`.
- **Injected common contract versus direct platform calls**: Direct Android, desktop, and browser calls in shared code would diverge and make tests dependent on a runtime. Prefer a minimal common interface with target adapters and an in-memory test implementation.
- **Native scalar storage versus a new persistence library**: A new library could offer a shared API but adds dependency, initialization, and platform compatibility cost for one integer. Prefer existing local platform facilities behind the interface unless implementation constraints prove otherwise.
- **Safe read fallback versus startup failure**: Corrupt or unavailable local data should not prevent a player from opening the game. Prefer treating missing/invalid reads as `0` and keeping gameplay available, while defining whether write failures are surfaced, logged, or reported in detailed design.
- **One coherent score presentation versus maximum-only display**: Showing only the maximum would hide a lower attempt's final result, and showing only current score would hide progress. Prefer two explicit values with status-appropriate accessibility descriptions.
- **Application-local scope versus account/network storage**: The requirement explicitly excludes sign-in and connectivity. Prefer the platform's local application/origin namespace and make same-target retention, not cross-device synchronization, the durability guarantee.

#### Alternatives Considered

- **Keep the best score in memory only**: Rejected because a new controller after process close or browser reload would lose the value and fail AC5.
- **Persist the entire `GameState`**: Rejected because the story asks only for a completed best score; restoring snake position, food, pause status, or current score would expand scope and conflict with the preceding pause analysis.
- **Use `rememberSaveable` or Compose state as storage**: Rejected because UI state does not reliably survive application termination on all three targets and would couple durability to one composition.
- **Update the best on every food collection**: Rejected because an active or paused unfinished attempt must not establish a completed-session record, and it would make AC2's completion boundary ambiguous.
- **Replace the current score with `max(current, best)`**: Rejected because AC3 requires a lower attempt's final score, such as `30`, to remain visible beside the best `50`.
- **Use account or network storage**: Rejected because the requirement explicitly excludes sign-in, online leaderboards, and network dependence.
- **Add a shared persistence framework immediately**: Rejected as the default because the project has no such dependency and only needs one local integer; a framework remains an option if target adapters cannot meet the durability contract cleanly.
- **Write a record during restart without a terminal comparison**: Rejected because restart must reset only the current score and could accidentally promote or clear the wrong value during a stale-tick race.

## Risk & Gap Analysis

#### Requirement Ambiguities

- **Meaning of “completed session”**: The story does not explicitly name `GAME_OVER`, although the prerequisite story defines final scores through game over. The recommended interpretation is a boundary or self-collision terminal transition, not pause, close, or an abandoned active attempt.
- **Record update timing**: It is unspecified whether persistence must finish before the terminal result is rendered or may happen after it. The implementation needs an ordering contract so a new best is visible with the final result and cannot be lost to immediate restart.
- **Tie behavior**: The requirement says only a score that exceeds the best replaces it, but it does not say whether an equal score should be rewritten. Treat equal scores as unchanged and avoid an unnecessary write.
- **Initial view lifecycle**: AC1 and AC5 require scores before a next session is completed, but do not specify whether the view opens in `READY` or automatically starts an active game. The current application starts ready, so both score values should be available in that state.
- **Persistence failure feedback**: The story assumes retention but does not define behavior for unavailable storage, quota/security errors, or a failed write. Detailed design must choose whether to retain an in-memory value, show a non-blocking warning, retry, or use another local adapter.
- **Stored-data compatibility**: The format, key name, migration policy, and handling of malformed or negative values are not specified. A single non-negative integer with a safe `0` fallback is the smallest compatible contract.
- **Storage identity**: “Same target” could mean same installed application, user profile, browser origin, or package/version. The adapters need stable namespaces and should not promise retention across uninstall, cleared site data, or a changed browser origin.
- **Multiple instances**: The requirement does not state whether multiple windows, activities, or browser tabs can update one record concurrently. The normal application has one controller, but max-preserving writes should be considered if the platform permits concurrent instances.

#### Edge Cases

- **No history**: A fresh store must load `0`, and the initial current score must also be `0` without a special placeholder or missing label.
- **A completed zero-score session**: Game over at score `0` must not create a visible distinction from the default record or cause a write that changes the baseline.
- **Active score above the record**: A player may temporarily reach `50` while active while the record is `40`; closing, pausing, or restarting without completion must leave the stored record at `40`.
- **New record at collision**: A final score of `50` against a stored `40` must preserve the terminal snake/final score, update the record once, and show both values in the same result.
- **Lower and equal completions**: Scores of `30` and `50` against a record of `50` must leave the record unchanged while showing each attempt's own final score.
- **Restart after either result**: Restart after a new or non-new game over must set current score to `0`, retain the best, and not create a second write or clear the store.
- **Pause and close before completion**: A paused current score must not be promoted by application closure; a later launch restores only the previously persisted best.
- **Repeated terminal ticks or stale ticks**: A collision result delivered more than once must not downgrade the record, duplicate harmful side effects, or let a prior session overwrite a newer restart.
- **Malformed local value**: Empty, non-numeric, negative, or out-of-range storage data should fall back safely rather than crash controller construction; the normalization policy should be deterministic.
- **Write failure or quota/security denial**: Gameplay and final-score presentation should remain usable even if persistence cannot be completed, but the limitation and next-launch behavior need an explicit policy.
- **Target storage reset**: Uninstalling an application, clearing browser site data, changing origin, or using a different desktop user may legitimately return the best score to `0`; this is not cross-target synchronization.
- **Concurrent controllers**: Two instances completing different scores can race. The durable operation should avoid allowing a lower value to replace a higher value if concurrent access is supported, or the limitation should be recorded as out of scope.

#### Technical Risks

- **No existing persistence seam**: `GameController` currently constructs an in-memory ready state and has no storage dependency (`composeApp/src/commonMain/kotlin/com/example/snake/game/controller/GameController.kt:22-47`). Adding a contract and wiring it through all entry points is required before AC5 can be demonstrated.
- **Cross-target API differences**: Android preferences, JVM preferences, and browser local storage have different lifecycles and failure modes. A narrow common contract and target-specific tests are needed to keep read/write semantics aligned.
- **Android composition recreation**: `MainActivity` creates the Compose content directly, so a recreated activity/controller must reload the persisted scalar without restoring the old gameplay session or losing a previously written record.
- **Wasm/browser storage availability**: `localStorage` can be unavailable or throw under browser privacy, security, quota, or origin conditions. Browser adapter errors must not become uncaught startup failures.
- **Atomicity between best and session state**: Updating a best-score flow separately from `GameState` can briefly expose a new final score with an old record or vice versa. Controller serialization or one presentation snapshot should make the player-observable result coherent.
- **Completion detection placement**: Updating on every `advance` or every score change would violate terminal-only semantics; the controller must recognize the `GAME_OVER` result without duplicating collision logic in the UI.
- **Restart and clock race**: `GameController` already uses session generations to reject stale ticks (`composeApp/src/commonMain/kotlin/com/example/snake/game/controller/GameController.kt:84-115`). Best-score evaluation must occur before a newer restart can erase the terminal context, while the record itself must never be reset by `startNewGame`.
- **State-construction compatibility**: Adding a best value to an existing public state type would affect every manual `GameState` construction and every rule copy. Keeping it separate reduces domain coupling, but the chosen presentation path must still be covered by common tests.
- **UI label and accessibility regression**: The current screen has one score label and status-specific semantics (`composeApp/src/commonMain/kotlin/com/example/snake/game/ui/GameScreen.kt:103-115`). A second value must be visibly distinct and must not replace the final-score meaning in game over or become ambiguous while paused.
- **Validation gap across storage lifecycles**: Current tests cover current-score movement, game over, and restart, but not store reads/writes, controller recreation, corrupted values, write failure, or all three platform adapters. These cases need focused unit and target verification during implementation.
- **Persistence dependency creep**: Introducing a library for one scalar could add incompatible initialization or platform packaging requirements, especially for Wasm. The implementation should first validate whether existing platform facilities satisfy durability and testability.

#### Acceptance Criteria Coverage

| AC# | Description | Addressable? | Gaps/Notes |
|-----|-------------|--------------|------------|
| AC1 | With no completed history, the game view shows current score `0` and best score `0`. | Partial | The current session already starts with score `0`, but there is no loaded record or best-score presentation. The store's empty-read behavior and initial ready-state rendering must be added. |
| AC2 | A completed score of `50` replaces a current best of `40` and is visible with the final result. | Yes | The existing game-over transition preserves the final current score; a controller completion boundary can compare, persist, and expose the higher best atomically. |
| AC3 | A completed score of `30` leaves best `50` while final score remains `30`. | Yes | Separate current and best values plus a strict-greater comparison directly satisfy the criterion; no current-score rewrite is needed. |
| AC4 | Restart sets current score to `0` while retaining best `50`. | Yes | The established fresh-session initializer resets current score, and a controller-owned record remains outside that reset path. |
| AC5 | A best score of `50` remains visible after closing and reopening on the same target. | Partial | The requirement is structurally addressable through target-local adapters, but the repository has no persistence implementation, stable storage wiring, or relaunch tests yet. |

All five acceptance criteria are addressable through the existing shared-state architecture with a controller-level best-score contract and target-local storage adapters. AC1 and AC5 remain implementation-dependent on initial loading, durable storage behavior, and platform failure policy; the explicit exclusions of accounts, networking, leaderboards, and full session restoration remain outside this story.