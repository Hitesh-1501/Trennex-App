package com.example.trennex

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trennex.data.local.cart.AppDatabase
import com.example.trennex.data.local.AccountScope
import com.example.trennex.data.local.cart.CartItemEntity
import com.example.trennex.data.local.wihslist.CollectionEntity
import com.example.trennex.data.local.wihslist.WishlistItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountScopedDatabaseTest {
    @Test
    fun migrationRetainsUnownedRowsWithoutShowingThemToAnAccount() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "account-scope-migration-test"
        context.deleteDatabase(name)
        val oldDatabase = context.openOrCreateDatabase(name, 0, null)
        oldDatabase.execSQL("CREATE TABLE cart_items (id INTEGER NOT NULL PRIMARY KEY, title TEXT NOT NULL, description TEXT NOT NULL, mrp REAL NOT NULL, price REAL NOT NULL, rating REAL NOT NULL, ratingCount INTEGER NOT NULL, returnPolicy TEXT NOT NULL, deliveryDetails TEXT NOT NULL, imageUrl TEXT, imageRes INTEGER, quantity INTEGER NOT NULL, isSelected INTEGER NOT NULL)")
        oldDatabase.execSQL("INSERT INTO cart_items VALUES (7, 'Old cart', '', 100, 80, 0, 0, '', '', NULL, NULL, 1, 1)")
        oldDatabase.execSQL("CREATE TABLE wishlist_items (id INTEGER NOT NULL PRIMARY KEY, imageUrl TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, mrp REAL NOT NULL, price REAL NOT NULL, rating REAL NOT NULL, ratingCount INTEGER NOT NULL, returnPolicy TEXT NOT NULL, deliveryDetails TEXT NOT NULL)")
        oldDatabase.execSQL("INSERT INTO wishlist_items VALUES (7, '', 'Old wishlist', '', 100, 80, 0, 0, '', '')")
        oldDatabase.execSQL("CREATE TABLE collections (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL, itemJson TEXT NOT NULL)")
        oldDatabase.execSQL("INSERT INTO collections VALUES (7, 'Old collection', '[]')")
        oldDatabase.execSQL("CREATE TABLE recent_searches (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, query TEXT NOT NULL, timeStamp INTEGER NOT NULL)")
        oldDatabase.version = 6
        oldDatabase.close()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_6_7).build()
        try {
            assertEquals("Old cart", database.cartDao().getItemById(AccountScope.LEGACY_UNASSIGNED, 7)?.title)
            assertEquals(emptyList<CartItemEntity>(), database.cartDao().observeCartItems("user:account-a").first())
            assertEquals("Old wishlist", database.wishlistDao().observeWishlistItems(AccountScope.LEGACY_UNASSIGNED).first().single().title)
            assertEquals(emptyList<WishlistItemEntity>(), database.wishlistDao().observeWishlistItems("user:account-a").first())
            assertEquals("Old collection", database.collectionDao().observeCollections(AccountScope.LEGACY_UNASSIGNED).first().single().name)
            assertEquals(emptyList<CollectionEntity>(), database.collectionDao().observeCollections("user:account-a").first())
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun sameItemIdsStaySeparateAcrossAccountsAndGuest() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val accountA = "user:account-a"
            val accountB = "user:account-b"
            val guest = "guest"
            fun cart(owner: String, title: String) = CartItemEntity(
                ownerId = owner, id = 7, title = title, description = "", mrp = 100.0,
                price = 80.0, rating = 0.0, ratingCount = 0, returnPolicy = "",
                deliveryDetails = "", imageUrl = null, imageRes = null, quantity = 1,
            )
            database.cartDao().upsert(cart(accountA, "A cart"))
            database.cartDao().upsert(cart(accountB, "B cart"))
            database.cartDao().upsert(cart(guest, "Guest cart"))
            database.cartDao().updateSelection(accountA, 7, true)
            database.cartDao().deleteSelectedItems(accountA)
            assertEquals(emptyList<CartItemEntity>(), database.cartDao().observeCartItems(accountA).first())
            assertEquals("B cart", database.cartDao().observeCartItems(accountB).first().single().title)
            assertEquals("Guest cart", database.cartDao().observeCartItems(guest).first().single().title)

            fun wishlist(owner: String, title: String) = WishlistItemEntity(
                ownerId = owner, id = 7, imageUrl = "", title = title, description = "",
                mrp = 100.0, price = 80.0, rating = 0.0, ratingCount = 0,
                returnPolicy = "", deliveryDetails = "",
            )
            database.wishlistDao().upsert(wishlist(accountA, "A wishlist"))
            database.wishlistDao().upsert(wishlist(accountB, "B wishlist"))
            database.wishlistDao().removeItem(accountA, 7)
            assertEquals(emptyList<WishlistItemEntity>(), database.wishlistDao().observeWishlistItems(accountA).first())
            assertEquals("B wishlist", database.wishlistDao().observeWishlistItems(accountB).first().single().title)

            database.collectionDao().upsert(CollectionEntity(accountA, 7, "A collection", "[]"))
            database.collectionDao().upsert(CollectionEntity(accountB, 7, "B collection", "[]"))
            database.collectionDao().removeCollection(accountA, 7)
            assertEquals(emptyList<CollectionEntity>(), database.collectionDao().observeCollections(accountA).first())
            assertEquals("B collection", database.collectionDao().observeCollections(accountB).first().single().name)
        } finally {
            database.close()
        }
    }
}
