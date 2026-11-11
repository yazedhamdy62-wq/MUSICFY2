package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PlaylistEntity::class, SongMetadataEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MusicfyDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao

    companion object {
        @Volatile
        private var INSTANCE: MusicfyDatabase? = null

        fun getDatabase(context: Context): MusicfyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicfyDatabase::class.java,
                    "musicfy_database.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
