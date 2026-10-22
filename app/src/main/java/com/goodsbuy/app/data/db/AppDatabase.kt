package com.goodsbuy.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.goodsbuy.app.data.entity.CollectibleEntity

/** One index created by a migration: [name] and the single column it covers. */
internal data class IndexSpec(val name: String, val column: String)

/**
 * Indices added in v1 -> v2. Kept as data (not inline SQL strings) so
 * `SchemaMigrationConsistencyTest` can assert that this list matches the
 * indices Room derives from [CollectibleEntity] in the exported schema.
 */
internal val MIGRATION_1_2_INDICES: List<IndexSpec> = listOf(
    IndexSpec("index_collectibles_status", "status"),
    IndexSpec("index_collectibles_createdAt", "createdAt"),
    IndexSpec("index_collectibles_sellDate", "sellDate")
)

@Database(entities = [CollectibleEntity::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun collectibleDao(): CollectibleDao

    companion object {
        /** v1 -> v2 adds the query indices declared on [CollectibleEntity]. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2_INDICES.forEach { index ->
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `${index.name}` ON `collectibles` (`${index.column}`)"
                    )
                }
            }
        }

        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
    }
}
