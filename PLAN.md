# TASK SPECIFICATION: Android Automotive DIY Dashboard ("Pi-Dash & Media Companion")

## 1. Overview & Objective

Build an Android application using **100% Jetpack Compose** designed for a DIY Car Head-Unit running on a **Raspberry Pi** (Android OS / LineageOS / AAOS port).
The app serves as a central touch-friendly dashboard displaying a speedometer, weather/location updates, a media player companion, and quick actions for nearby spots.

The primary engineering goal is to maintain a **clean MVVM architecture** and write comprehensive **Jetpack Compose UI Tests** (`androidTest`) covering UI states, interactions, custom semantics, and hardware input adaptations.

---

## 2. Tech Stack Requirements

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (BOM)
- **Architecture:** MVVM with Clean Architecture & StateFlow
- **Asynchronous / Stream:** Kotlin Coroutines & Flow
- **Dependency Injection:** Hilt
- **Navigation:** Jetpack Compose Navigation
- **Testing Stack:**
    - JUnit4 / JUnit5
    - `androidx.compose.ui:ui-test-junit4` & `androidx.compose.ui:ui-test-manifest`
    - MockK & Turbine (for Flow testing)

---

## 3. Key UI Screens & Features

### Screen 1: Home Dashboard (Main Hub)

- **Speedometer & Trip Data:** Displays current speed (km/h) updated in real-time via GPS or Mock Flow generator.
- **Weather & Environment Widget:** Shows current temperature, condition icon, and location name.
- **Quick Action Controls:** Large touch targets (minimum 64dp) for:
    - Emergency Contact / Home Navigation
    - System Mute / Day-Night Theme Toggle
    - Quick Stop Finder (e.g., Coffee, Fuel, Rest Stops)

### Screen 2: Media Companion (Now Playing)

- **Music Player Layout:** Prominent album artwork, large play/pause/skip buttons optimized for glanceable touch interaction while driving.
- **Track Info:** Song title, artist, progress bar, and time indicators.

### Screen 3: Favorite Spots & Quick Search

- **Category Chips:** Filterable chips (Coffee, Gas Station, Rest Stop, Food).
- **LazyColumn Spot List:** Interactive list displaying distance, address, and quick "Navigate" trigger buttons.

---

## 4. Automotive UI/UX Guidelines (Strict Constraints)

1. **Touch Target Size:** All clickable elements (buttons, chips, icons) MUST have a minimum touch target size of **64dp x 64dp** to accommodate touchscreen use in a vehicle environment.
2. **Glanceability & Contrast:** High contrast colors, clear typography (large font sizes), and minimal screen clutter.
3. **State Management:** Isolate UI logic from business logic completely via ViewModels to allow isolated Compose Rule tests (`createComposeRule()`).

---

## 5. Compose Testing Requirements (`src/androidTest/`)

Implement the following test suites using `ComposeTestRule`:

### A. Node Finding & Display Assertions (`DashboardScreenTest.kt`)

- Verify that speedometer updates correctly when state changes.
- Verify that all critical car controls (Mute, Theme Switch, Home Action) are rendered with `assertIsDisplayed()`.
- Test tags (`Modifier.testTag()`) must be used for deterministic node selection.

### B. User Actions & Gestures (`MediaCompanionTest.kt`)

- Test Play/Pause button click toggles state and invokes correct ViewModel callbacks (`performClick()`).
- Test track progress slider dragging gesture (`performTouchInput`).

### C. Filtering & List Interaction (`FavoriteSpotsTest.kt`)

- Test selecting a category Chip (e.g., "Coffee") filters the `LazyColumn` items accordingly.
- Verify scrolling and clicking on a spot card fires the navigation intent/action.

### D. Accessibility & Semantics Test

- Ensure every icon-only button contains explicit `contentDescription` for accessibility.
- Test custom semantic properties or focus state switches (useful for Rotary Controller navigation).

---

## 6. Expected Deliverables

1. Complete Project Folder Structure (Clean MVVM separation: `data`, `domain`, `ui`, `di`).
2. Composable UI Components adhering to Automotive UX.
3. Fully functional Mock Data Generators for Speed & Media StateFlow.
4. Robust `androidTest` files demonstrating complete Compose UI test coverage for the core screens.

## 7. Premium Automotive UI/UX & Visual Styling Directives

To ensure the Dashboard UI looks high-end, futuristic, and premium (inspired by modern EV displays like Porsche Taycan, Mercedes Hyperscreen, and Polestar OS), strictly apply the following design guidelines:

### A. Design System & Color Palette (Dark-First Premium Theme)

- **Theme Concept:** "Obsidian Cyberpunk" or "Luxury Minimalist Dark". Dark mode must be the primary design focus to prevent glare during night driving.
- **Backgrounds:** Deep slate/charcoal dark tones (`#0D1117`, `#121824`, `#1A202C`) instead of pure flat black, paired with subtle semi-transparent frosted glass (Glassmorphism/Backdrop Blur) layers.
- **Accent Colors:** Electric Neon Turquoise (`#00F2FE`), Neon Cyan (`#4FACFE`), or Warm Amber Gold (`#FFB300`) for high-contrast active states, focal points, and speedometer gauges.
- **Typography:** Modern, clean, sans-serif, highly legible display font (e.g., Inter, Montserrat, or Roboto Condensed). Bold and oversized numbers for speed, temperature, and key metrics.

### B. Visual Effects & Material Design 3 Styling

- **Card & Container Design:** Sleek rounded corners (`16.dp` to `24.dp`), subtle light inner stroke/border (`Color.White.copy(alpha = 0.15f)`), and soft drop shadows to give depth.
- **Gradients & Glows:** Apply subtle radial gradients and neon glow effects around active controls, progress rings, and the speed gauge indicator using Compose `Brush` and `Canvas`.
- **Smooth Transitions & Animations:**
    - Animated numbers for speed transition using `AnimatedContent` or `animateIntAsState`.
    - Subtle pulsing glow for active media playback or connectivity status.
    - Spring-based click feedback (`scale` down slightly on tap) for tactile visual feel.

### C. Dashboard Component Specific Guidelines

1. **Digital Speedometer Gauge:**
    - Design a sleek circular/arc ring gauge using Compose `Canvas` drawing `drawArc`.
    - Display speed dynamically in the center with dynamic neon gradient color shifts (e.g., Cool Cyan for low speed, Energetic Orange/Red when speeding).
2. **Media Player Widget:**
    - Full blur/tinted album art background with a smooth gradient overlay.
    - Custom wave visualizer or sleek progress bar animation.
3. **Quick Action Cards:**
    - Big, tactile 3D-ish glass cards with distinct glowing vector icons for instant visual recognition.

### D. Reusable Premium UI Components to Implement

- `PremiumGlassCard`: A custom Composable wrapper providing frosted glass, rounded border, and shadow.
- `NeonArcGauge`: A custom Canvas-based gauge component for real-time speed/RPM rendering.
- `GlowButton`: A high-contrast, tactile touch button with active glow state effects.
