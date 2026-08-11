package com.goodsbuy.app.ui.preferences

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.preference.PreferenceManager

data class GridPreferences(
    val columns: Int = 2,
    val cardSize: Int = 140
)

class PreferencesRepository(private val context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    val columns: Int
        get() = prefs.getInt(PREF_COLUMNS, 2)
    val cardSize: Int
        get() = prefs.getInt(PREF_CARD_SIZE, 140)

    fun save(columns: Int, cardSize: Int) {
        prefs.edit().putInt(PREF_COLUMNS, columns).putInt(PREF_CARD_SIZE, cardSize).apply()
    }

    @Composable
    fun collectGridPreferences(): State<GridPreferences> = produceState(
        initialValue = GridPreferences(columns, cardSize)
    ) {
        value
    }

    companion object {
        private const val PREF_COLUMNS = "grid_columns"
        private const val PREF_CARD_SIZE = "grid_card_size"
    }
}
