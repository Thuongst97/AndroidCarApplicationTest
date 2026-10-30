# Pi-Dash — Test Instructions

This document describes **what** is tested, **how the tests are built**, how to **add new tests**,
and the **manual checklist** to run on the head unit. For commands, see
[HOW_TO_RUN_TESTS.md](HOW_TO_RUN_TESTS.md).

## 1. Test strategy

| Layer | Location | Runs on | Tools | Purpose |
|---|---|---|---|---|
| Unit | `app/src/test/` | JVM (your PC) | JUnit4, MockK, Turbine, coroutines-test | ViewModel state, use-case logic, mock data generators |
| Compose UI | `app/src/androidTest/` | Device (the Pi) | `createComposeRule()`, MockK-android | Rendering, interactions, gestures, semantics, accessibility, navigation |
| Manual | Section 5 | Device | adb, your eyes | Real Hilt wiring, real audio mute, external intents, visual quality |

UI tests render **stateless screens** (`DashboardScreen(state, actions)`) directly, so they need no
Hilt, Activity or network. Where a test must prove that the ViewModel callbacks are wired, it builds
a real ViewModel with a MockK repository.

## 2. Test inventory

### 2.1 Unit tests (12)

| Class | Test | Verifies |
|---|---|---|
| `DashboardViewModelTest` | speed emissions flow into ui state | Trip emissions reach `uiState`; `isSpeeding`; weather merged |
| | toggles update settings through repository | Mute/theme toggles call `SettingsRepository` and update state |
| | mini player toggle plays when paused | Play/pause delegates to `MediaRepository.play()` |
| `MediaViewModelTest` | toggle pauses when playing | Calls `pause()` when currently playing |
| | seek is clamped to valid range | `onSeek(1.7f)` → `seekTo(1f)` |
| | skip delegates to repository | Next/previous calls |
| `SpotsViewModelTest` | category filter narrows and sorts spots | Filter, sorting by distance, re-select clears the filter |
| | initial category comes from navigation argument | `SavedStateHandle["category"]` pre-selects |
| `MockSpeedGeneratorTest` | speed stays realistic and trip distance accumulates | 0–140 km/h, ≤8 km/h jump per tick, monotonic distance, elapsed time (seeded `Random(42)`) |
| `MockMediaRepositoryTest` | playing advances position and pause stops it | Virtual-time ticking |
| | track auto advances at the end and skip wraps around | End-of-track and queue wrap |
| | skip previous restarts track when past three seconds | "Previous" behaviour |

### 2.2 Compose UI tests (40)

The UI suite follows the [Compose UI Test Standard](COMPOSE_TEST_STANDARD.md): every class uses
`piDashComposeRule()`, interacts only through screen robots (`robots/`), and every screen has a
contract test that applies the generic UI standards.

**Contract tests — 18 (6 per screen)** in `DashboardScreenContractTest`, `MediaScreenContractTest`,
`SpotsScreenContractTest` (inherited from `ScreenContractTest`)

| Test | Verifies |
|---|---|
| `contract_rendersInDarkTheme` / `contract_rendersInLightTheme` | Screen renders in both themes; saves `screens/<Screen>_dark.png` / `_light.png` |
| `contract_rendersAtMaxFontScale` | Renders at 1.3× with all controls still labelled; saves `_maxFont.png` |
| `contract_clickablesHaveAccessibleLabels` | Every clickable has a label; icon-only ones have a `contentDescription` |
| `contract_clickablesMeetMinimumTouchTarget` | Every clickable is ≥ 64dp × 64dp |
| `contract_clickablesReachableByRotary` | Focus traversal (rotary knob) reaches every clickable |

**A. `DashboardScreenTest` — node finding and display assertions**

| Test | Verifies |
|---|---|
| `speedometer_stateChanges_updatesDisplayedSpeed` | State 0 → 88 updates the speed text (after the animation settles), `SpeedKmh` semantics and description |
| `speedometer_overSpeedLimit_raisesAlert` | `SpeedAlert` turns true above the limit; limit badge description |
| `controls_initialRender_allCriticalControlsDisplayed` | Home, SOS, Mute, Theme, all quick stops, gauge and mini player pass `assertIsDisplayed()` |
| `weatherWidget_withWeather_showsTemperatureConditionAndLocation` | Temperature, location, condition icon description |
| `controls_tapped_invokeMatchingCallbacksInOrder` | Each control fires the right callback, in order |
| `muteAndTheme_toggled_reflectNewSettingsState` | Content/state descriptions and `GlowActive` change after toggling |

**B. `MediaCompanionTest` — user actions and gestures**

| Test | Verifies |
|---|---|
| `playPause_tapped_togglesStateAndCallsViewModel` | Real `MediaViewModel` + MockK repository: tap → `verify { play() }` and UI shows Pause/Playing; tap → `verify { pause() }` |
| `skipButtons_tapped_invokeCallbacks` | Next ×2, previous ×1 |
| `trackInfo_withTrack_showsTitleArtistAndTimes` | Title, "artist · album", `1:05`, `3:34`, artwork |
| `progressSlider_dragged_seeksToReleasedPosition` | Drag gesture to the middle → seek ≈ 0.5 and position text updates |
| `progressSlider_tapped_seeksToTappedPosition` | Tap at 80 % → seek ≈ 0.8 |

**C. `FavoriteSpotsTest` — filtering and list interaction** (real `SpotsViewModel` + fake repository)

| Test | Verifies |
|---|---|
| `categoryChip_coffeeSelected_listShowsOnlyCoffeeSpots` | Chip selection state; only coffee cards rendered; every coffee card reachable by scrolling |
| `categoryChip_selectedAgain_clearsFilter` | Tapping the selected chip returns to "All" |
| `categoryChip_fromNavigationArgument_isPreselected` | Navigation argument pre-selects Gas Station |
| `navigateButton_farSpotAfterScrolling_firesNavigationAction` | Scrolls to the farthest spot, taps Navigate, callback receives that spot |
| `spotCard_tapped_firesNavigationAction` | Tapping the card itself navigates |
| `categoryChip_categoryWithoutSpots_showsEmptyState` | Empty-state node shown, list removed |

**D. `AccessibilitySemanticsTest` — app-specific accessibility and custom semantics**

| Test | Verifies |
|---|---|
| `rotaryFocus_requested_isExposedThroughCustomSemantics` | Focus request → focused and `RotaryFocused = true` |
| `rotaryKnob_rotatedAndPressed_movesFocusAndActivatesControl` | One detent moves focus Home → SOS; D-pad centre activates SOS |
| `spotControls_rendered_exposeSelectionAndDescriptiveLabels` | Chip `isSelected`, "Navigate to Highlands Coffee" |

**E. `NavigationTest` — rail navigation regression**

| Test | Verifies |
|---|---|
| `railReturnsToDashboard_afterQuickStopOpenedSpots` | Media → Drive → quick stop → Drive ends on Dashboard with back stack `[dashboard]` |
| `railSwitchesBetweenAllDestinations_keepingDashboardAsRoot` | Back stack is always `[dashboard, X]` |

## 3. Conventions for writing UI tests

The full standard, including the layer diagram, templates and how to port it to another app, is in
[COMPOSE_TEST_STANDARD.md](COMPOSE_TEST_STANDARD.md). In short:

1. **One rule:** `@get:Rule val rule = piDashComposeRule()`. It applies the theme, pins the font
   scale to 1.0 (the test Pi runs at 1.8×), saves a screenshot on failure, and provides
   `enableRotaryInput()` / `rotate(n)`.
2. **Interact through robots.** Tests call `rule.dashboard { tapMute(); assertMuted(true) }`. Only
   robots know test tags, gestures and semantics keys. New tags go in `ui/components/TestTags.kt`.
3. **Name and structure:** `subject_condition_expectedResult`, with Given / When / Then sections and one
   behaviour per test.
4. **Assert logical state through semantics, not pixels.** Canvas-drawn state is exposed via
   `DashSemantics` and checked inside robot `assertX` methods.
5. **One `setContent` per test.** Drive changes through `mutableStateOf` or `collectAsState()` returned
   from the robot's `launch { }` lambda.
6. **Async results:** read values set by coroutines (slider drag end) inside `rule.runOnIdle { }`.
7. **Focus:** call `rule.enableRotaryInput()` first; in touch mode clickables are not focusable.
8. **Every screen gets a contract test.** Generic checks (themes, font scale, labels, 64dp, rotary) are
   never hand-written per test; new controls are covered automatically.

### Example

```kotlin
@Test
fun muteAndTheme_toggled_reflectNewSettingsState() = rule.dashboard {
    // Given
    var settings by mutableStateOf(SystemSettings(isDarkTheme = true, isMuted = false))
    launch(DashboardActions(onToggleMute = { settings = settings.copy(isMuted = !settings.isMuted) })) {
        TestData.dashboard.copy(settings = settings)
    }
    assertMuted(false)

    // When
    tapMute()

    // Then
    assertMuted(true)
}
```

## 4. Conventions for unit tests

- Add `@get:Rule val mainRule = MainDispatcherRule()`. ViewModels use `viewModelScope` (Main).
- Collect `StateFlow`s with Turbine: `vm.uiState.test { … expectMostRecentItem() … }`.
- For code that launches long-running coroutines (the media ticker), pass `backgroundScope` from
  `runTest` and drive time with `advanceTimeBy`.
- Inside a `mockk { }` block, do not give test fields the same name as mocked members (for example
  `settings`). The mock's own member shadows them.

## 5. Manual test checklist (on the head unit)

Run after any UI change, with the app installed (`.\gradlew :app:installDebug`).

| # | Step | Expected |
|---|---|---|
| M1 | Launch the app | Fullscreen (system bars hidden), dashboard visible, speed changes every ~0.5 s, no crash |
| M2 | Watch the gauge for ~1 min | Arc colour shifts: ice/platinum → champagne/amber near the limit → red and "OVER LIMIT" above it |
| M3 | Tap **Sound** | Button turns amber "MUTED"; `adb shell dumpsys audio` shows `STREAM_MUSIC` `Muted: true`. Tap again → `false` |
| M4 | Tap **Night** | Whole UI switches to the Ivory & Bronze day theme; tap **Day** to return |
| M5 | Tap the mini-player play button | Visualizer animates, progress line advances |
| M6 | Tap the mini-player card | Opens Media screen |
| M7 | Media: play, next, previous, drag the slider | Playback controls respond; drag seeks (time label updates) |
| M8 | Dashboard: tap **Coffee** | Spots opens with the COFFEE pill selected and only coffee places |
| M9 | Spots: tap Navigate (champagne circle) | Opens a maps app, or shows the snackbar "… no app available on this head unit" |
| M10 | After M9, tap **Drive** and **Media** in the rail | Navigation works (regression check for the rail bug) |
| M11 | Tap **SOS** / **Home** | Dialer/maps opens, or the snackbar appears; no crash |
| M12 | Check text at system font scale 1.8× | No truncated labels (scale capped at 1.3× in-app) |
| M13 | Check for crashes | `adb logcat -b crash -d` is empty for `com.pidash.companion` |

Tips:

- `adb exec-out screencap -p > shot.png` captures the screen.
- `uiautomator dump` usually fails on the dashboard ("could not get idle state") because the gauge
  animates continuously. Tap by coordinates, or dump on the Spots screen.
