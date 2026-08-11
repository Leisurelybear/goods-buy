package com.graincabinet.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.graincabinet.app.data.entity.CollectibleEntity

@Database(entities = [CollectibleEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun collectibleDao(): CollectibleDao
}
