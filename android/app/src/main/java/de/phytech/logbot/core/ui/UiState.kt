/** Einfacher Zustand für daten-ladende Screens. */
package de.phytech.logbot.core.ui

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>

    /** [code] ist der HTTP-Status, falls der Server geantwortet hat. */
    data class Error(val message: String, val code: Int? = null) : UiState<Nothing>
}

/** Der Wert, falls geladen – sonst null. */
val <T> UiState<T>.dataOrNull: T? get() = (this as? UiState.Success)?.data
