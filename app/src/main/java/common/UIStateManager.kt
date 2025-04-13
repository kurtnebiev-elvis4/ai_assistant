package common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

interface WithActionManger<Action> {
    val actionM: ActionManager<Action>
}

fun <Action> WithActionManger<Action>.provideAction() = actionM.action
suspend fun <Action> WithActionManger<Action>.send(action: Action) = actionM.send(action)

class ActionManager<Action> @Inject constructor() {
    private val _actionEvent = MutableSharedFlow<Action>()
    val action = _actionEvent.asSharedFlow()

    suspend fun send(action: Action) {
        _actionEvent.emit(action)
    }
}