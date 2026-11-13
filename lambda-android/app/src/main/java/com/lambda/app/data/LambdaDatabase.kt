package com.lambda.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TaskEntity::class, SubjectEntity::class, GroupEntity::class, ChapterEntity::class, HabitEntity::class, HabitLogEntity::class, SessionEntity::class, DailyLogEntity::class, NoteEntity::class, GoalEntity::class, MilestoneEntity::class, AppMetaEntity::class], version = 2, exportSchema = true)
abstract class LambdaDatabase : RoomDatabase() {
    abstract fun dao(): LambdaDao
    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN attachmentsJson TEXT NOT NULL DEFAULT '[]'")
            }
        }

        @Volatile private var INSTANCE: LambdaDatabase? = null
        fun get(context: Context): LambdaDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, LambdaDatabase::class.java, "lambda-local.db").addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
        }
    }
}
