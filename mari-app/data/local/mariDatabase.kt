package com.Mari.mobileapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.Mari.mobileapp.data.local.dao.TransactionDao
import com.Mari.mobileapp.data.local.dao.UserDao
import com.Mari.mobileapp.data.local.entity.TransactionEntity
import com.Mari.mobileapp.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class, TransactionEntity::class],
    version = 3,
    exportSchema = false
)
abstract class MariDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        const val DATABASE_NAME = "Mari-db"
    }
}
