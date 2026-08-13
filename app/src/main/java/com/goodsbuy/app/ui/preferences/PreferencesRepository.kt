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
    val showStatus: Boolean = true,
    val sortField: String = "CREATED_AT",
    val sortAscending: Boolean = false,
    val fontSize: Int = 1,           // 0=小 1=中 2=大
    val showSortControl: Boolean = true,
    val loggingEnabled: Boolean = false
)

class PreferencesRepository(private val context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val _state = mutableStateOf(
        GridPreferences(
            columns, cardSize, showName, showPrice, showStatus,
            prefs.getString(PREF_SORT_FIELD, "CREATED_AT") ?: "CREATED_AT",
            prefs.getBoolean(PREF_SORT_ASCENDING, false),
            prefs.getInt(PREF_FONT_SIZE, 1),
            prefs.getBoolean(PREF_SHOW_SORT, true),
            prefs.getBoolean(PREF_LOGGING, false)
        )
    )

    val columns: Int get() = prefs.getInt(PREF_COLUMNS, 2)
    val cardSize: Int get() = prefs.getInt(PREF_CARD_SIZE, 140)
    val showName: Boolean get() = prefs.getBoolean(PREF_SHOW_NAME, true)
    val showPrice: Boolean get() = prefs.getBoolean(PREF_SHOW_PRICE, true)
    val showStatus: Boolean get() = prefs.getBoolean(PREF_SHOW_STATUS, true)
    val sortField: String get() = prefs.getString(PREF_SORT_FIELD, "CREATED_AT") ?: "CREATED_AT"
    val sortAscending: Boolean get() = prefs.getBoolean(PREF_SORT_ASCENDING, false)
    val fontSize: Int get() = prefs.getInt(PREF_FONT_SIZE, 1)
    val showSortControl: Boolean get() = prefs.getBoolean(PREF_SHOW_SORT, true)
    val loggingEnabled: Boolean get() = prefs.getBoolean(PREF_LOGGING, false)

    val preferencesState: State<GridPreferences> get() = _state

    fun save(prefs: GridPreferences) {
        this._state.value = prefs
        with(PreferenceManager.getDefaultSharedPreferences(context).edit()) {
            putInt(PREF_COLUMNS, prefs.columns)
            putInt(PREF_CARD_SIZE, prefs.cardSize)
            putBoolean(PREF_SHOW_NAME, prefs.showName)
            putBoolean(PREF_SHOW_PRICE, prefs.showPrice)
            putBoolean(PREF_SHOW_STATUS, prefs.showStatus)
            putString(PREF_SORT_FIELD, prefs.sortField)
            putBoolean(PREF_SORT_ASCENDING, prefs.sortAscending)
            putInt(PREF_FONT_SIZE, prefs.fontSize)
            putBoolean(PREF_SHOW_SORT, prefs.showSortControl)
            putBoolean(PREF_LOGGING, prefs.loggingEnabled)
            apply()
        }
    }

    companion object {
        private const val PREF_COLUMNS = "grid_columns"
        private const val PREF_CARD_SIZE = "grid_card_size"
        private const val PREF_SHOW_NAME = "show_name"
        private const val PREF_SHOW_PRICE = "show_price"
        private const val PREF_SHOW_STATUS = "show_status"
        private const val PREF_SORT_FIELD = "sort_field"
        private const val PREF_SORT_ASCENDING = "sort_ascending"
        private const val PREF_FONT_SIZE = "font_size"
        private const val PREF_SHOW_SORT = "show_sort"
        private const val PREF_LOGGING = "logging_enabled"
    }
}
