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