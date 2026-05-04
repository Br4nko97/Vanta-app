package com.vanta.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vanta.app.data.models.ButtonBinding
import com.vanta.app.data.models.Workflow
import com.vanta.app.data.models.WorkflowStep

@Database(entities = [ButtonBinding::class, Workflow::class, WorkflowStep::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vantaDao(): VantaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vanta_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
