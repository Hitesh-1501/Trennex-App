package com.example.trennex.data.local.cart

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.trennex.data.local.wihslist.WishlistDao
import com.example.trennex.data.local.wihslist.WishlistItemEntity

@Database(entities = [CartItemEntity::class, WishlistItemEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase: RoomDatabase(){
    abstract fun cartDao():CartDao
    abstract fun wishlistDao(): WishlistDao

    companion object{
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase{
            return INSTANCE?: synchronized(this){
                INSTANCE?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}