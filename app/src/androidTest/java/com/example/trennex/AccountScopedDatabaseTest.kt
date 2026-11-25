package com.example.trennex

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trennex.data.local.cart.AppDatabase
import com.example.trennex.data.local.AccountScope
import com.example.trennex.data.local.cart.CartItemEntity
import com.example.trennex.data.local.cart.PendingCartSyncEntity
import com.example.trennex.repository.cart.CartRepository
import com.example.trennex.ui.cart.model.CartItemModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    fun versionSevenCartRowsMigrateToLegacyKeys() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "variant-cart-migration-test"
        context.deleteDatabase(name)
        val old = context.openOrCreateDatabase(name, 0, null)
        old.execSQL("CREATE TABLE cart_items (ownerId TEXT NOT NULL, id INTEGER NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, mrp REAL NOT NULL, price REAL NOT NULL, rating REAL NOT NULL, ratingCount INTEGER NOT NULL, returnPolicy TEXT NOT NULL, deliveryDetails TEXT NOT NULL, imageUrl TEXT, imageRes INTEGER, quantity INTEGER NOT NULL, isSelected INTEGER NOT NULL, PRIMARY KEY(ownerId, id))")
        old.execSQL("INSERT INTO cart_items VALUES ('user:account-a', 7, 'Existing cart', '', 100, 80, 0, 0, '', '', NULL, NULL, 2, 1)")
        old.execSQL("CREATE TABLE wishlist_items (ownerId TEXT NOT NULL, id INTEGER NOT NULL, imageUrl TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, mrp REAL NOT NULL, price REAL NOT NULL, rating REAL NOT NULL, ratingCount INTEGER NOT NULL, returnPolicy TEXT NOT NULL, deliveryDetails TEXT NOT NULL, PRIMARY KEY(ownerId, id))")
        old.execSQL("CREATE TABLE collections (ownerId TEXT NOT NULL, id INTEGER NOT NULL, name TEXT NOT NULL, itemJson TEXT NOT NULL, PRIMARY KEY(ownerId, id))")
        old.execSQL("CREATE TABLE recent_searches (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, query TEXT NOT NULL, timeStamp INTEGER NOT NULL)")
        old.version = 7
        old.close()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9).build()
        try {
            val item = database.cartDao().getItemByKey("user:account-a", "legacy:7")
            assertEquals("Existing cart", item?.title)
            assertEquals(2, item?.quantity)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

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
            .addMigrations(AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9).build()
        try {
            assertEquals("Old cart", database.cartDao().getItemByKey(AccountScope.LEGACY_UNASSIGNED, "legacy:7")?.title)
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
            database.cartDao().updateSelection(accountA, "legacy:7", true)
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

    @Test
    fun variantsOfSameProductRemainSeparate() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            fun variant(id: String) = CartItemEntity(
                ownerId = "user:account-a", id = 7, title = "Tee", description = "",
                mrp = 100.0, price = 80.0, rating = 0.0, ratingCount = 0,
                returnPolicy = "", deliveryDetails = "", imageUrl = null, imageRes = null,
                quantity = 1, cartKey = id, productId = "tee", variantId = id,
                variantName = id,
            )
            database.cartDao().upsert(variant("small"))
            database.cartDao().upsert(variant("medium"))
            database.cartDao().updateQuantity("user:account-a", "small", 2)
            val items = database.cartDao().getAllCartItems("user:account-a")
            assertEquals(2, items.size)
            assertEquals(2, items.single { it.variantId == "small" }.quantity)
            assertEquals(1, items.single { it.variantId == "medium" }.quantity)
        } finally {
            database.close()
        }
    }

    @Test
    fun pendingSyncSurvivesReopenAndOldGenerationCannotClearNewChange() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "pending-cart-sync-test"
        context.deleteDatabase(name)
        val first = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        val old = PendingCartSyncEntity("user:account-a", "red", 7, "red", "UPSERT", "generation-1")
        try {
            first.cartDao().upsertPending(old)
        } finally {
            first.close()
        }
        val reopened = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            assertEquals(old, reopened.cartDao().pendingSync().single())
            val updated = old.copy(operation = "DELETE", generation = "generation-2")
            reopened.cartDao().upsertPending(updated)
            reopened.cartDao().clearPending(old.ownerId, old.cartKey, old.generation)
            assertEquals(updated, reopened.cartDao().pendingForKey(old.ownerId, old.cartKey))
            reopened.cartDao().clearPending(updated.ownerId, updated.cartKey, updated.generation)
            assertEquals(emptyList<PendingCartSyncEntity>(), reopened.cartDao().pendingSync())
        } finally {
            reopened.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun pendingKeysAreIsolatedByAccount() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val a = PendingCartSyncEntity("user:account-a", "red", 7, "red", "UPSERT", "a")
            val b = a.copy(ownerId = "user:account-b", generation = "b")
            database.cartDao().upsertPending(a)
            database.cartDao().upsertPending(b)
            database.cartDao().clearPending(a.ownerId, a.cartKey, a.generation)
            assertEquals(b, database.cartDao().pendingSync().single())
        } finally {
            database.close()
        }
    }

    @Test
    fun concurrentCartAddsKeepOneRowAndOnePendingAbsoluteWrite() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repo = CartRepository(database)
            val item = CartItemModel(id = 7, title = "Tee", description = "", mrp = 100.0,
                price = 80.0, quantity = 1, productId = "tee", variantId = "red")
            coroutineScope {
                (1..10).map { async { repo.addItemLocally("user:account-a", item) } }.awaitAll()
            }
            assertEquals(10, database.cartDao().getItemByKey("user:account-a", "red")?.quantity)
            val pending = database.cartDao().pendingSync()
            assertEquals(1, pending.size)
            assertEquals("UPSERT", pending.single().operation)
        } finally {
            database.close()
        }
    }
}
