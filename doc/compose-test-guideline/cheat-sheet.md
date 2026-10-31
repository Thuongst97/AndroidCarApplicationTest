# Compose UI Test Cheat Sheet

## Skeleton

```kotlin
@RunWith(AndroidJUnit4::class)
class MyScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test fun subject_condition_expectedResult() {
        rule.setContent { MyTheme { MyScreen(state, onAction = {}) } }   // Given
        rule.onNodeWithTag("button").performClick()                      // When
        rule.onNodeWithTag("result").assertTextEquals("Done")            // Then
    }
}
```

## Make UI testable (app code)

```kotlin
Modifier.testTag("button")                                     // stable handle
Icon(icon, contentDescription = "Close")                       // label for icon-only controls
Modifier.semantics { stateDescription = "Muted" }              // state
Modifier.semantics { mySemanticsKey = value }                  // custom meaning (Canvas, glow…)
@Composable fun Screen(state: UiState, onEvent: () -> Unit)    // state in, events out
```

## Find

| | |
|---|---|
| `onNodeWithTag("x")` | by tag ⭐ |
| `onNodeWithText("x", substring = true)` | by text |
| `onNodeWithContentDescription("x")` | by label |
| `onNode(hasText("a") and hasClickAction())` | combined |
| `onAllNodesWithTag("x")[i]` / `.fetchSemanticsNodes()` | many |
| `…, useUnmergedTree = true` | inside buttons/cards |
| `onRoot()` | whole screen |

## Act

| | |
|---|---|
| `performClick()` | tap |
| `performTouchInput { longClick() / doubleClick() }` | press variants |
| `performTouchInput { swipeUp() / swipeLeft() }` | swipe |
| `performTouchInput { swipeRight(startX = left + width*0.05f, endX = centerX) }` | drag |
| `performTouchInput { click(percentOffset(0.8f, 0.5f)) }` | tap at position |
| `performScrollToNode(hasTestTag("item"))` | scroll lazy list |
| `performTextInput("abc")` / `performTextClearance()` / `performImeAction()` | text |
| `performKeyInput { pressKey(Key.Enter) }` | keys (needs focus) |
| `requestFocus()` | focus (keyboard mode) |

## Assert

| | |
|---|---|
| `assertExists()` / `assertDoesNotExist()` | in tree |
| `assertIsDisplayed()` | visible |
| `assertTextEquals("a")` / `assertTextContains("a")` | text |
| `assert(hasContentDescription("x"))` / `assert(hasStateDescription("x"))` | labels |
| `assertIsSelected()` / `assertIsOn()` / `assertIsEnabled()` / `assertIsFocused()` | state |
| `assertWidthIsAtLeast(48.dp)` | size |
| `assertCountEquals(n)` | on `onAllNodes…` |
| `assert(SemanticsMatcher.expectValue(Key, v))` | custom semantics |

## Time and async

| | |
|---|---|
| (automatic) `waitForIdle()` before every find/act/assert | |
| `rule.runOnIdle { … }` | run on UI thread after idle; read callback results |
| `rule.waitUntil(5_000) { condition }` | wait for async data |
| `rule.mainClock.autoAdvance = false` + `advanceTimeBy(ms)` | step through animations |
| Inject time sources; freeze in tests | clocks, timers |

## Focus (D-pad / keyboard / rotary)

```kotlin
rule.setContent { inputMode = LocalInputModeManager.current; focus = LocalFocusManager.current; Screen() }
rule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
rule.onNodeWithTag("a").requestFocus()
rule.runOnIdle { focus.moveFocus(FocusDirection.Next) }
rule.onNodeWithTag("b").performKeyInput { pressKey(Key.DirectionCenter) }
```

## Debug

```kotlin
rule.onRoot().printToLog("DEBUG")
rule.onRoot(useUnmergedTree = true).printToLog("DEBUG")
```

## Run

```powershell
.\gradlew :app:connectedDebugAndroidTest
.\gradlew :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=<pkg>.<Class>#<method>"
start app\build\reports\androidTests\connected\debug\index.html
```

## Dependencies

```kotlin
androidTestImplementation(platform("androidx.compose:compose-bom:<ver>"))
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
debugImplementation("androidx.compose.ui:ui-test-manifest")
androidTestImplementation("androidx.test.ext:junit:1.3.0")
androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")   // Android 16
// defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
```
