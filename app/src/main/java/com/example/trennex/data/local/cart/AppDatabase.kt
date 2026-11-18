package com.example.trennex.data.local.cart

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.trennex.data.local.search.RecentSearchDao
import com.example.trennex.data.local.search.RecentSearchEntity
import com.example.trennex.data.local.wihslist.CollectionDao
import com.example.trennex.data.local.wihslist.CollectionEntity
import com.example.trennex.data.local.wihslist.WishlistDao
import com.example.trennex.data.local.wihslist.WishlistItemEntity

@Database(entities = [CartItemEntity::class, WishlistItemEntity::class, CollectionEntity::class, RecentSearchEntity::class], version = 8, exportSchema = false)
abstract class AppDatabase: RoomDatabase(){
    abstract fun cartDao():CartDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun collectionDao(): CollectionDao

    abstract fun recentSearchDao(): RecentSearchDao

    companion object{
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase{
            return INSTANCE?: synchronized(this){
                INSTANCE?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).addMigrations(MIGRATION_6_7, MIGRATION_7_8).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }

        internal val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Existing rows have no trustworthy owner. Retain them without exposing them to any account.
                db.execSQL("""CREATE TABLE IF NOT EXISTS cart_items_new (ownerId TEXT NOT NULL, id INTEGER NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, mrp REAL NOT NULL, price REAL NOT NULL, rating REAL NOT NULL, ratingCount INTEGER NOT NULL, returnPolicy TEXT NOT NULL, deliveryDetails TEXT NOT NULL, imageUrl TEXT, imageRes INTEGER, quantity INTEGER NOT NULL, isSelected INTEGER NOT NULL, PRIMARY KEY(ownerId, id))""")
                db.execSQL("""INSERT INTO cart_items_new SELECT '${com.example.trennex.data.local.AccountScope.LEGACY_UNASSIGNED}', id, title, description, mrp, price, rating, ratingCount, returnPolicy, deliveryDetails, imageUrl, imageRes, quantity, isSelected FROM cart_items""")
                db.execSQL("DROP TABLE cart_items")
                db.execSQL("ALTER TABLE cart_items_new RENAME TO cart_items")

                db.execSQL("""CREATE TABLE IF NOT EXISTS wishlist_items_new (ownerId TEXT NOT NULL, id INTEGER NOT NULL, imageUrl TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, mrp REAL NOT NULL, price REAL NOT NULL, rating REAL NOT NULL, ratingCount INTEGER NOT NULL, returnPolicy TEXT NOT NULL, deliveryDetails TEXT NOT NULL, PRIMARY KEY(ownerId, id))""")
                db.execSQL("""INSERT INTO wishlist_items_new SELECT '${com.example.trennex.data.local.AccountScope.LEGACY_UNASSIGNED}', id, imageUrl, title, description, mrp, price, rating, ratingCount, returnPolicy, deliveryDetails FROM wishlist_items""")
                db.execSQL("DROP TABLE wishlist_items")
                db.execSQL("ALTER TABLE wishlist_items_new RENAME TO wishlist_items")

                db.execSQL("""CREATE TABLE IF NOT EXISTS collections_new (ownerId TEXT NOT NULL, id INTEGER NOT NULL, name TEXT NOT NULL, itemJson TEXT NOT NULL, PRIMARY KEY(ownerId, id))""")
                db.execSQL("""INSERT INTO collections_new SELECT '${com.example.trennex.data.local.AccountScope.LEGACY_UNASSIGNED}', id, name, itemJson FROM collections""")
                db.execSQL("DROP TABLE collections")
                db.execSQL("ALTER TABLE collections_new RENAME TO collections")
            }
        }

        internal val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS cart_items_new (ownerId TEXT NOT NULL, id INTEGER NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, mrp REAL NOT NULL, price REAL NOT NULL, rating REAL NOT NULL, ratingCount INTEGER NOT NULL, returnPolicy TEXT NOT NULL, deliveryDetails TEXT NOT NULL, imageUrl TEXT, imageRes INTEGER, quantity INTEGER NOT NULL, isSelected INTEGER NOT NULL, cartKey TEXT NOT NULL, productId TEXT, variantId TEXT, variantName TEXT, PRIMARY KEY(ownerId, cartKey))""")
                db.execSQL("""INSERT INTO cart_items_new SELECT ownerId, id, title, description, mrp, price, rating, ratingCount, returnPolicy, deliveryDetails, imageUrl, imageRes, quantity, isSelected, 'legacy:' || id, NULL, NULL, NULL FROM cart_items""")
                db.execSQL("DROP TABLE cart_items")
                db.execSQL("ALTER TABLE cart_items_new RENAME TO cart_items")
            }
        }
    }
}
