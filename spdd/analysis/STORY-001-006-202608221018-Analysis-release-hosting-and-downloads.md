# SPDD Analysis: Host the Browser Game and Provide Download Builds

## Original Business Requirement

## STORY-001-006 Host the Browser Game and Provide Download Builds

### Background

Players need a public place to try Snake without installing anything and a trusted way to obtain the Android and desktop versions. A GitHub Pages site gives visitors one recognizable entry point for the browser game and the current installable releases, making the game accessible outside the development environment.

### Business Value

- Let visitors play a complete game session in a browser without an account or installation.
- Give Android players a direct download for the current APK.
- Give desktop players clear downloads for each supported operating system.
- Establish one public, versioned place to discover the current release.

### Dependencies and Assumptions

- **Parent epic**: [Epic-1 Snake Game](Epic-1-snake-game.md)
- **Prerequisites**: The game is playable on every advertised target, and a release owner has a browser version, an Android APK, and desktop packages ready to publish for the same release.
- **Data assumptions**: A release such as `1.0.0` has one identifiable browser version, one Android APK, and one desktop package for each supported operating system.
- **Integration points**: The story uses the project's public GitHub Pages site and publicly accessible release download locations.
- **Business constraints**: Visitors must not need to sign in to play or download; every visible download must identify its platform and release version.

### Scope In

- Publishing the browser game at the project's public GitHub Pages address.
- Providing a clear action to play the browser version from the public page.
- Providing versioned download links for the Android APK and supported desktop packages.
- Keeping the browser entry point and download links aligned with the current release.

### Scope Out

- Implementing or changing Snake gameplay rules and controls.
- Publishing through mobile or desktop app stores.
- Automatic update installation, account management, analytics, or online services for gameplay.
- Building release artifacts that are not available for the advertised release.

### Acceptance Criteria

#### AC1: A public release page is available

**Given** release `1.0.0` is ready to publish
**When** an unauthenticated visitor opens the project's public GitHub Pages address
**Then** the page loads publicly and shows the game name, the current release `1.0.0`, an option to play in the browser, and a downloads section.

#### AC2: Visitors can play without installing the game

**Given** a visitor is on the public release page
**When** the visitor chooses to play in the browser
**Then** the browser version opens and the visitor can start and complete a game session without installing an application or signing in.

#### AC3: The current Android APK is downloadable

**Given** release `1.0.0` includes an Android APK
**When** an Android visitor selects the Android download
**Then** the visitor receives the APK for release `1.0.0`, and the link is clearly labeled as an Android download.

#### AC4: Desktop downloads are separated by platform

**Given** release `1.0.0` includes packages for Windows, macOS, and Linux
**When** a visitor views the downloads section
**Then** one clearly labeled link is available for each of Windows, macOS, and Linux, and each selected link provides the package for release `1.0.0`.

#### AC5: All published options refer to the same release

**Given** a visitor views the page for release `1.0.0`
**When** the visitor chooses browser play, the Android download, or any desktop download
**Then** the selected option corresponds to release `1.0.0` rather than an older or mixed release.

#### AC6: An unavailable platform is not presented as a broken download

**Given** a release does not include a package for one advertised platform
**When** the public page is published
**Then** the page either omits that platform's download or marks it clearly as unavailable, while browser play and all available downloads remain usable.

### Non-Functional Expectations

- The public page and its available downloads can be reached without authentication from both desktop and mobile screens.
- Browser play and download actions are visually distinct, and platform labels are understandable before a visitor selects a link.
- A newly published release replaces the page's current version and links without leaving stale links presented as current.

## Domain Concept Identification

#### Existing Concepts (from codebase)

- **Shared Snake game foundation**: `composeApp/src/commonMain` contains the platform-neutral game state, board, snake, direction rules, controller, and Compose presentation. `GameRules` starts a deterministic active session and advances one grid cell at a time, while `SnakeApp` and `GameScreen` expose the playable UI; this is the common experience that browser and native targets should share, but it currently represents the initial movement increment rather than the complete game described by the prerequisite.
- **Android application target**: `composeApp/src/androidMain` provides the Android activity and touch-oriented entry point for the shared game. It is an existing target that can produce an application artifact, but the repository contains no published APK or public download surface.
- **JVM desktop application target**: `composeApp/src/desktopMain` provides the desktop window and keyboard-oriented entry point. The application configuration already identifies Windows, macOS, and Linux distribution formats as MSI, DMG, and DEB respectively, giving the release concept an existing native target matrix.
- **Desktop package identity**: The Compose desktop configuration has package name `snake` and package version `1.0.0`, so a versioned desktop artifact concept exists in build configuration even though the packages have not been published or linked from a public page.

#### New Concepts Required

- **Browser game release**: A browser-consumable version of the shared Snake experience that can be opened from a public page and supports a complete session without installation or sign-in. No browser target or browser artifact exists in the current build configuration.
- **Public release page**: The unauthenticated GitHub Pages entry point that identifies the game and current release, presents browser play, and organizes native downloads for visitors on desktop and mobile screens.
- **Versioned release**: A single release identity, such as `1.0.0`, that owns the browser version, Android APK, and each available desktop package and prevents the page from mixing artifacts from different releases.
- **Platform release artifact**: A publicly accessible Android APK or desktop package associated with a specific version and platform label; the page needs one independently addressable option for every available supported platform.
- **Platform availability state**: The publication status indicating whether a platform artifact is available for the current release, so an absent package is omitted or presented as clearly unavailable rather than as a broken link.
- **Release publication surface**: The lifecycle by which a new release replaces the current page version and links while preserving a coherent, publicly discoverable entry point. No Pages configuration, publication workflow, or release metadata is present in the repository.

#### Key Business Rules

- The public release page and every available artifact link must be reachable without authentication.
- The page must identify the game, the current release version, browser play, and a downloads section before a visitor selects an option.
- Browser play, the Android APK, and each desktop package exposed for a release must correspond to the same release identity.
- Download actions must be understandable before selection through explicit platform labels; Windows, macOS, and Linux must be separated when their packages are available.
- A platform without an artifact must not appear as a broken current download; it must be omitted or marked unavailable while other available options remain usable.
- Publishing a new current release must replace the page's current version and links without presenting stale artifacts as current.
- The browser version must preserve the shared game rules and provide a complete session; the release-hosting story does not change those rules.
- The current build configuration's native platform mapping is Windows/MSI, macOS/DMG, and Linux/DEB, but the final advertised package formats and supported architectures must be agreed before publication.

## Strategic Approach

#### Solution Direction

- Treat the release as one versioned publication unit rather than as unrelated links. A public GitHub Pages entry point should expose the current release identity, a browser-play action, and the publicly accessible native artifacts that belong to that release.
- Extend the existing Kotlin Compose Multiplatform delivery direction with a browser target that reuses the shared game concepts and rules in `commonMain`, while keeping target-specific packaging and input/presentation concerns at the platform boundary. This minimizes divergence between browser, Android, and desktop behavior.
- Keep the public page static and unauthenticated, with release metadata and artifact availability determining which actions are presented. Use a canonical public artifact location for native packages so the Pages site remains a lightweight discovery surface rather than a binary repository.
- Make release identity and publication status explicit across the browser build, Android APK, and desktop packages. The page should be updated as one release change so its displayed version and all current links move together.
- Gate publication on artifact readiness and gameplay completeness. The repository currently has the initial shared movement increment, Android/JVM desktop targets, and desktop distribution declarations, but the complete game prerequisite and browser artifact still need to be available before this story can be accepted.

#### Key Design Decisions

- **GitHub Pages entry point versus a separate web site**: A separate site could offer more hosting flexibility but adds infrastructure and another public destination; use the repository's GitHub Pages site because the requirement explicitly calls for one recognizable, low-operations public entry point.
- **Shared multiplatform game versus a separate browser implementation**: A separate browser game could be optimized independently but would duplicate game behavior and threaten the epic's cross-target consistency rule; use the existing shared game foundation and add only the browser-specific delivery boundary.
- **Pages-hosted binaries versus linked release artifacts**: Storing all native binaries in the Pages site would simplify the visible URL space but creates repository/site size and replacement concerns; use publicly accessible, versioned release assets as the download source and let Pages provide the clear discovery links.
- **One canonical release version versus per-target version labels**: Independent target versions make partial releases easier but allow mixed browser, APK, and desktop builds; use one canonical release identity and require every exposed artifact to be traceable to it. The current Android `versionName` of `1.0` and desktop package version `1.0.0` must be aligned before `1.0.0` can be advertised.
- **Availability-aware presentation versus permanent platform links**: Permanent links risk exposing 404s when a package is missing; derive the visible platform options from actual artifact availability and either omit missing platforms or label them explicitly unavailable, with no dead current link.
- **Automated coordinated publication versus independent manual edits**: Independent edits are quick for a first release but increase stale-link and version-mismatch risk; prefer a coordinated release update driven by one authoritative version and artifact set, even if the first implementation keeps the workflow simple.

#### Alternatives Considered

- **Use GitHub Releases without a GitHub Pages page**: Rejected because it does not provide the required single browser-play and downloads entry point with a clearly structured visitor experience.
- **Publish only native packages and omit browser delivery**: Rejected because browser play without installation is the primary accessibility value and is explicit in AC1 and AC2.
- **Advertise every platform with placeholder or failing links**: Rejected because AC6 requires missing packages to be omitted or clearly unavailable rather than presented as broken current downloads.
- **Distribute through app stores**: Rejected because mobile and desktop stores are explicitly outside the story's scope.
- **Build a separate game for the browser**: Rejected because duplicated rules and state behavior would make the parent epic's cross-target consistency harder to maintain.

## Risk & Gap Analysis

#### Requirement Ambiguities

- **GitHub Pages address and ownership**: The repository remote is `https://github.com/mihbor/snake`, but no Pages configuration or public address is present in the repository. Confirm whether the intended site is the project Pages address derived from that repository, which branch/folder publishes it, and whether a custom domain is involved.
- **Canonical release source**: The requirement names `1.0.0` but does not define whether the authoritative release is a Git tag, a GitHub Release, a generated metadata file, or another release record. A single source of truth is needed to prevent mixed links.
- **Artifact hosting location**: “Publicly accessible release download locations” does not identify whether artifacts belong in GitHub release assets, Pages storage, or another host, nor how long old release URLs remain available.
- **Browser target and artifact contract**: The requirement does not specify the browser build technology, supported browsers, base path behavior for a project Pages site, or whether a browser artifact must be versioned independently from the page.
- **Advertised desktop package formats and architectures**: The current build declares DMG, MSI, and DEB formats, but the story only names operating systems. Clarify whether one package per operating system is sufficient and which CPU architectures must be supported.
- **Version identity across targets**: Android currently declares `versionName = "1.0"`, while desktop packaging declares `packageVersion = "1.0.0"`. Confirm the canonical value and how Android's numeric version code relates to it.
- **Current versus historical releases**: The requirement says a new release replaces the page's current version but does not define whether previous releases remain discoverable elsewhere or whether only the latest release should be shown on the public page.
- **Missing-platform presentation**: AC6 allows omission or an unavailable label. Choose one default policy and define the wording and visual treatment so visitors do not interpret an unavailable option as a broken link.
- **Release readiness boundary**: The prerequisite assumes a complete game session on every advertised target, while the repository currently contains only the initial start-and-control increment. Clarify whether story acceptance is blocked until the remaining gameplay stories are complete, as the dependency wording suggests.

#### Edge Cases

- A release has the page and browser build but no APK or one missing desktop package; the page must continue to offer browser play and all valid downloads without exposing a dead option.
- A page update succeeds while one artifact upload fails, or an artifact is replaced after the page is cached; publication needs an atomic or readiness-checked promotion concept so a version is not presented as complete prematurely.
- A link points to a valid package from an older release, has a platform label that does not match the package, or uses an unversioned “latest” location that silently moves; these cases can pass basic link checks while violating AC5.
- The browser is opened under the repository project path rather than a domain root; incorrect asset resolution could make the public page load while browser play fails.
- Visitors open the page on a narrow mobile viewport, rotate the device, use a touch-only browser, or use a keyboard-capable browser; the page and browser game must preserve readable labels, reachable actions, and usable controls.
- A desktop package exists for one operating-system family but not every architecture or distribution variant; the page needs a clear supported-platform boundary rather than implying universal compatibility.
- An Android device downloads an APK through a redirect, an unsupported architecture, or an older cached URL; the visible version and the delivered artifact must remain aligned and publicly retrievable.
- A new release leaves old version text or links in cached/generated page content, repository assets, or an alternate entry path, causing visitors to see a mixed current release.
- A visitor opens an unavailable option directly from a bookmark or stale page; the resulting response should not misrepresent an old artifact as the current release.

#### Technical Risks

- **No browser delivery target**: `composeApp/build.gradle.kts` declares only `androidTarget()` and JVM `desktop`; no browser source set, web artifact, or Pages deployment exists. Adding browser delivery may expose Compose/web compatibility, browser input, rendering, packaging, and base-path issues that are not exercised by the current Android and desktop build.
- **Incomplete release prerequisite**: The existing shared rules start and move a snake, but `GameState` and `GameRules` do not yet include food, growth, scoring progression, self-collision/game-over behavior, pause/resume, or best-score retention. Publishing now would not satisfy the requirement's complete-session prerequisite even though hosting work itself is separate from gameplay rules.
- **Version drift**: Android and desktop use different visible release versions, and no shared release metadata binds them. This can produce a page that says `1.0.0` while delivering an APK labeled `1.0` or artifacts built from different source revisions; establish one release identity and validate every artifact before publication.
- **Missing publication automation and configuration**: No `.github` workflow or other Pages/release configuration was found, so deployment, artifact availability checks, permissions, and replacement behavior are currently undefined. A manual process without a coordinated promotion step increases stale-link and partial-release risk.
- **Artifact size and hosting limits**: Native packages can be large and have different retention or bandwidth characteristics from a static Pages site. Keeping Pages as the index and using an appropriate public release-asset location reduces site-repository growth and makes versioned downloads easier to manage.
- **Cross-target behavioral divergence**: The common game logic is a useful consistency boundary, but a new browser input or lifecycle path could bypass it or behave differently from `MainActivity` and the desktop `Main.kt`. Validate the same completed-session behavior across targets before advertising them together.
- **Public download trust and integrity**: Direct APK and native-package links must remain public, stable enough for the current release, and attributable to the stated version. Release ownership, signing, checksums, and link validation are not specified and should be addressed in the detailed design or release policy.
- **Responsive and accessible discovery surface**: The existing `GameScreen` has platform-specific controls and semantics, but it is not a public release page and there is no mobile web layout. The release page needs independent responsive, readable, and accessible action presentation.

#### Acceptance Criteria Coverage

| AC# | Description | Addressable? | Gaps/Notes |
|-----|-------------|--------------|------------|
| AC1 | An unauthenticated GitHub Pages visitor sees the game name, current release `1.0.0`, browser-play action, and downloads section. | Partial | The proposed static release page addresses the behavior, but no Pages site, public URL/configuration, browser target, or coordinated release metadata exists yet. |
| AC2 | Selecting browser play opens a browser version that supports starting and completing a session without installation or sign-in. | Partial | The shared game foundation can support reuse, but no browser target/artifact exists and the current repository does not yet contain the complete gameplay prerequisite. |
| AC3 | The clearly labeled Android option delivers the `1.0.0` APK. | Partial | An Android target exists, but no APK or public download link is published; the current Android version label also differs from the desktop `1.0.0` value. |
| AC4 | The downloads section provides distinct Windows, macOS, and Linux links for `1.0.0` packages. | Partial | Native distribution formats are declared as MSI, DMG, and DEB, but packages, architecture support, public locations, and page links are not present. |
| AC5 | Browser play and all native options correspond to the same `1.0.0` release. | Partial | A canonical release unit and coordinated publication strategy address the rule, but no shared release identity or validation process currently binds the targets. |
| AC6 | Missing platform artifacts are omitted or clearly marked unavailable while other options remain usable. | Yes | Availability-aware page presentation directly supports this criterion; the missing-artifact policy and its exact visitor-facing treatment still require a product decision. |
