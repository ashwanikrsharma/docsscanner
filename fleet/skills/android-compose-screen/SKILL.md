---
name: android-compose-screen
description: Generate a Jetpack Compose Material 3 screen with a Hilt-injected ViewModel, UiState sealed interface, StateFlow, typed nav route, and @Preview. Use when the user asks to add a Compose screen, build a UI in Kotlin, or wire a screen to a ViewModel in a native Android project. Not for React Native screens.
---

# Android Compose Screen

Generate one Compose feature screen following unidirectional data flow.

## Inputs

- `feature_name` (PascalCase, e.g. `Payments`)
- `module_path` (default: `feature/<kebab-name>`)
- Optional: description from spec

## Files to generate

Under `feature/<kebab-name>/src/main/java/<pkg>/feature/<kebab>/`:

1. **`<Feature>Screen.kt`** — Compose function, takes `viewModel: <Feature>ViewModel = hiltViewModel()` and nav lambdas
2. **`<Feature>ViewModel.kt`** — `@HiltViewModel`, exposes `StateFlow<<Feature>UiState>`, handles intents via `onEvent(<Feature>Event)`
3. **`<Feature>UiState.kt`** — `sealed interface <Feature>UiState { object Loading; data class Success(...); data class Error(val message: String) }`
4. **`<Feature>Event.kt`** — `sealed interface <Feature>Event` for user actions
5. **`<Feature>NavGraph.kt`** — typed route + `composable<<Feature>Route>` extension on `NavGraphBuilder`
6. **Test:** `feature/<kebab>/src/test/java/<pkg>/<Feature>ViewModelTest.kt` — Turbine + MockK, covers Loading→Success and Loading→Error transitions

## Template skeleton

```kotlin
// <Feature>Screen.kt
@Composable
fun <Feature>Screen(
    onNavigateBack: () -> Unit,
    viewModel: <Feature>ViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text("<Feature>") }) }
    ) { padding ->
        when (val s = state) {
            is <Feature>UiState.Loading -> CircularProgressIndicator(Modifier.padding(padding))
            is <Feature>UiState.Success -> <Feature>Content(s, viewModel::onEvent, Modifier.padding(padding))
            is <Feature>UiState.Error -> ErrorView(s.message, Modifier.padding(padding))
        }
    }
}

@Preview(name = "Light") @Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun Preview() { AppTheme { <Feature>Screen(onNavigateBack = {}) } }
```

```kotlin
// <Feature>ViewModel.kt
@HiltViewModel
class <Feature>ViewModel @Inject constructor(
    // private val repo: <Feature>Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<<Feature>UiState>(<Feature>UiState.Loading)
    val uiState: StateFlow<<Feature>UiState> = _uiState.asStateFlow()

    fun onEvent(event: <Feature>Event) { /* ... */ }
}
```

## Wiring

After generating, append the module to root `settings.gradle.kts`:

```kotlin
include(":feature:<kebab-name>")
```

Register the nav graph in `app/src/main/java/<pkg>/MainActivity.kt`'s NavHost:

```kotlin
<Feature>NavGraph(navController)
```

## Output contract

Return `{status, files_touched: [...6 files], next_step: "run android-ci-agent on :feature:<kebab>", error?}`.
