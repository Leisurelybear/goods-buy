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
    val loggingEnabled: Boolean = false,
    val galleryEntryHome: Boolean = false,
    /** Whether multi-image cards on the home screen cycle through their images. */
    val homeImageAutoRotate: Boolean = false,
    /** Number of seconds each image remains visible before advancing. */
    val homeImageRotationIntervalSeconds: Int = 3
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
            prefs.getBoolean(PREF_LOGGING, false),
            prefs.getBoolean(PREF_GALLERY_ENTRY_HOME, false),
            prefs.getBoolean(PREF_HOME_IMAGE_AUTO_ROTATE, false),
            prefs.getInt(PREF_HOME_IMAGE_ROTATION_INTERVAL_SECONDS, 3).coerceIn(1, 60)
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
    val galleryEntryHome: Boolean get() = prefs.getBoolean(PREF_GALLERY_ENTRY_HOME, false)
    val homeImageAutoRotate: Boolean get() = prefs.getBoolean(PREF_HOME_IMAGE_AUTO_ROTATE, false)
    val homeImageRotationIntervalSeconds: Int
        get() = prefs.getInt(PREF_HOME_IMAGE_ROTATION_INTERVAL_SECONDS, 3).coerceIn(1, 60)

    val preferencesState: State<GridPreferences> get() = _state

    fun save(prefs: GridPreferences) {
        val normalizedPrefs = prefs.copy(
            homeImageRotationIntervalSeconds = prefs.homeImageRotationIntervalSeconds.coerceIn(1, 60)
        )
        this._state.value = normalizedPrefs
        with(PreferenceManager.getDefaultSharedPreferences(context).edit()) {
            putInt(PREF_COLUMNS, normalizedPrefs.columns)
            putInt(PREF_CARD_SIZE, normalizedPrefs.cardSize)
            putBoolean(PREF_SHOW_NAME, normalizedPrefs.showName)
            putBoolean(PREF_SHOW_PRICE, normalizedPrefs.showPrice)
            putBoolean(PREF_SHOW_STATUS, normalizedPrefs.showStatus)
            putString(PREF_SORT_FIELD, normalizedPrefs.sortField)
            putBoolean(PREF_SORT_ASCENDING, normalizedPrefs.sortAscending)
            putInt(PREF_FONT_SIZE, normalizedPrefs.fontSize)
            putBoolean(PREF_SHOW_SORT, normalizedPrefs.showSortControl)
            putBoolean(PREF_LOGGING, normalizedPrefs.loggingEnabled)
            putBoolean(PREF_GALLERY_ENTRY_HOME, normalizedPrefs.galleryEntryHome)
            putBoolean(PREF_HOME_IMAGE_AUTO_ROTATE, normalizedPrefs.homeImageAutoRotate)
            putInt(
                PREF_HOME_IMAGE_ROTATION_INTERVAL_SECONDS,
                normalizedPrefs.homeImageRotationIntervalSeconds
            )
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
        private const val PREF_GALLERY_ENTRY_HOME = "gallery_entry_home"
        private const val PREF_HOME_IMAGE_AUTO_ROTATE = "home_image_auto_rotate"
        private const val PREF_HOME_IMAGE_ROTATION_INTERVAL_SECONDS = "home_image_rotation_interval_seconds"
    }
}
