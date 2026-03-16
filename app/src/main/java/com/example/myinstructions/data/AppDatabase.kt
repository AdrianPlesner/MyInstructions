package com.example.myinstructions.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myinstructions.data.dao.InstructionDao
import com.example.myinstructions.data.dao.TaskDao
import com.example.myinstructions.data.entity.InstructionEntity
import com.example.myinstructions.data.entity.TaskEntity

@Database(entities = [TaskEntity::class, InstructionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun instructionDao(): InstructionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "my_instructions_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
