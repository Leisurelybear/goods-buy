package com.goodsbuy.app.ui.preferences

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.preference.PreferenceManager

data class GridPreferences(
    val columns: Int = 2,
    val cardSize: Int = 140,
    val showName: Boolean = true,
    val showPrice: Boolean = true,
    val showStatus: Boolean = true
)

class PreferencesRepository(private val context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val _state = mutableStateOf(GridPreferences(columns, cardSize, showName, showPrice, showStatus))

    val columns: Int get() = prefs.getInt(PREF_COLUMNS, 2)
    val cardSize: Int get() = prefs.getInt(PREF_CARD_SIZE, 140)
    val showName: Boolean get() = prefs.getBoolean(PREF_SHOW_NAME, true)
    val showPrice: Boolean get() = prefs.getBoolean(PREF_SHOW_PRICE, true)
    val showStatus: Boolean get() = prefs.getBoolean(PREF_SHOW_STATUS, true)

    val preferencesState: State<GridPreferences> get() = _state

    fun save(prefs: GridPreferences) {
        this._state.value = prefs
        with(PreferenceManager.getDefaultSharedPreferences(context).edit()) {
            putInt(PREF_COLUMNS, prefs.columns)
            putInt(PREF_CARD_SIZE, prefs.cardSize)
            putBoolean(PREF_SHOW_NAME, prefs.showName)
            putBoolean(PREF_SHOW_PRICE, prefs.showPrice)
            putBoolean(PREF_SHOW_STATUS, prefs.showStatus)
            apply()
        }
    }

    companion object {
        private const val PREF_COLUMNS = "grid_columns"
        private const val PREF_CARD_SIZE = "grid_card_size"
        private const val PREF_SHOW_NAME = "show_name"
        private const val PREF_SHOW_PRICE = "show_price"
        private const val PREF_SHOW_STATUS = "show_status"
    }
}
