package common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

interface WithUIStateManger<UIState> {
    val uiStateM: UIStateManager<UIState>
}

fun <UIState> WithUIStateManger<UIState>.provideUIState() = uiStateM.uiState
val <UIState> WithUIStateManger<UIState>.uiState get() = uiStateM.uiState.value
fun <UIState> WithUIStateManger<UIState>.push(state: UIState) = uiStateM.push(state)

class UIStateManager<UIState> @Inject constructor(defaultState: UIState) {
    private val _uiState = MutableStateFlow<UIState>(defaultState)
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()

    fun push(state: UIState) {
        _uiState.tryEmit(state)
    }
}
