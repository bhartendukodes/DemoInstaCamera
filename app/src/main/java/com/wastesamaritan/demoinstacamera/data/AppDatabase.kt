package com.wastesamaritan.demoinstacamera.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ImageEntity::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "your_db_name"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE images_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        filePath TEXT NOT NULL,
                        yaw REAL,
                        pitch REAL,
                        roll REAL,
                        rotationMatrix TEXT
                    )"""
                )
                database.execSQL(
                    "INSERT INTO images_new (id, filePath) SELECT id, filePath FROM images"
                )
                database.execSQL("DROP TABLE images")
                database.execSQL("ALTER TABLE images_new RENAME TO images")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE images ADD COLUMN comment TEXT")
                database.execSQL("ALTER TABLE images ADD COLUMN timestamp INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
            }
        }
    }
} 