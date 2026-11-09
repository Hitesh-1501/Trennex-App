package com.example.trennex.repository.product

import com.example.trennex.data.model.CategoryApiResponse
import com.example.trennex.data.model.ProductResponse
import com.example.trennex.data.model.catalog.CatalogCategory
import com.example.trennex.data.model.catalog.CatalogDocumentMapper
import com.example.trennex.data.model.catalog.CatalogProduct
import com.example.trennex.data.model.catalog.CatalogVariant
import com.example.trennex.data.model.catalog.HomeCatalogContent
import com.example.trennex.data.remote.network.RetrofitInstance
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class ProductRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun getProducts(limit: Int = 30, skip: Int = 0): List<ProductResponse> {
        return try {
            val catalogProducts = loadActiveCatalogProducts()
            if (catalogProducts.isNotEmpty()) {
                catalogProducts
                    .sortedBy(CatalogProduct::sortOrder)
                    .window(limit, skip)
                    .map { it.toProductResponse() }
            } else {
                loadLegacyProducts().ifEmpty {
                    RetrofitInstance.api.getProducts(limit, skip).products
                }
            }
        } catch (_: Exception) {
            RetrofitInstance.api.getProducts(limit, skip).products
        }
    }

    suspend fun getProductDetail(id: Int): ProductResponse {
        return try {
            val catalogDocument = firestore.collection(PRODUCTS)
                .whereEqualTo("status", STATUS_ACTIVE)
                .whereEqualTo("legacyId", id)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()

            if (catalogDocument != null) {
                val product = catalogDocument.toCatalogProduct()
                if (product != null) {
                    val variants = loadVariants(catalogDocument)
                    return product.toProductResponse(variants)
                }
            }

            loadLegacyProductDetail(id) ?: RetrofitInstance.api.getProductDetail(id)
        } catch (_: Exception) {
            RetrofitInstance.api.getProductDetail(id)
        }
    }

    suspend fun getCategory(): List<CategoryApiResponse> {
        return try {
            val catalogCategories = firestore.collection(CATEGORIES)
                .whereEqualTo("active", true)
                .get()
                .await()
                .documents
                .mapNotNull { it.toCatalogCategory() }
                .sortedBy(CatalogCategory::sortOrder)

            if (catalogCategories.isNotEmpty()) {
                catalogCategories.map { it.toCategoryApiResponse() }
            } else {
                loadLegacyCategories().ifEmpty { RetrofitInstance.api.getCategories() }
            }
        } catch (_: Exception) {
            RetrofitInstance.api.getCategories()
        }
    }

    suspend fun getProductByCategory(slug: String): List<ProductResponse> {
        return try {
            val query = if (slug == TOP_PICKS_CATEGORY) {
                activeProductsQuery().whereEqualTo("featured", true)
            } else {
                activeProductsQuery().whereEqualTo("categoryId", slug)
            }
            val catalogProducts = query.get().await().documents
                .mapNotNull { it.toCatalogProduct() }
                .sortedBy(CatalogProduct::sortOrder)

            if (catalogProducts.isNotEmpty()) {
                catalogProducts.map { it.toProductResponse() }
            } else {
                loadLegacyProductsByCategory(slug).ifEmpty {
                    RetrofitInstance.api.getProductByCategory(slug).products
                }
            }
        } catch (_: Exception) {
            RetrofitInstance.api.getProductByCategory(slug).products
        }
    }

    suspend fun searchProducts(query: String): List<ProductResponse> {
        return try {
            val catalogProducts = loadActiveCatalogProducts()
            if (catalogProducts.isNotEmpty()) {
                val normalizedQuery = query.trim()
                catalogProducts
                    .filter { product ->
                        product.name.contains(normalizedQuery, ignoreCase = true) ||
                            product.description.contains(normalizedQuery, ignoreCase = true) ||
                            product.tags.any { it.contains(normalizedQuery, ignoreCase = true) }
                    }
                    .sortedBy(CatalogProduct::sortOrder)
                    .map { it.toProductResponse() }
            } else {
                val legacyResults = loadLegacyProducts().filter { product ->
                    product.title.contains(query, ignoreCase = true) ||
                        product.description.contains(query, ignoreCase = true)
                }
                legacyResults.ifEmpty { RetrofitInstance.api.searchProducts(query).products }
            }
        } catch (_: Exception) {
            RetrofitInstance.api.searchProducts(query).products
        }
    }

    suspend fun getHomeCatalogContent(): HomeCatalogContent? {
        return try {
            val catalogProducts = loadActiveCatalogProducts()
                .sortedBy(CatalogProduct::sortOrder)
            if (catalogProducts.isEmpty()) return null

            val productResponses = catalogProducts.map { it.toProductResponse() }
            val responsesById = catalogProducts.associate { it.documentId to it.toProductResponse() }

            val featuredProducts = catalogProducts
                .filter(CatalogProduct::featured)
                .map { it.toProductResponse() }
            val newArrivals = catalogProducts
                .filter(CatalogProduct::newArrival)
                .map { it.toProductResponse() }

            val curatedSection = firestore.collection(HOME_SECTIONS)
                .whereEqualTo("active", true)
                .get()
                .await()
                .documents
                .firstOrNull { it.getString("type") == "CURATED_PRODUCTS" }

            val curatedProducts = if (curatedSection == null) {
                emptyList()
            } else {
                curatedSection.reference.collection(HOME_ITEMS)
                    .whereEqualTo("active", true)
                    .get()
                    .await()
                    .documents
                    .sortedBy { it.getLong("sortOrder") ?: Long.MAX_VALUE }
                    .mapNotNull { item -> responsesById[item.getString("productId")] }
            }

            val bannerUrls = firestore.collection(BANNERS)
                .whereEqualTo("active", true)
                .get()
                .await()
                .documents
                .sortedBy { it.getLong("sortOrder") ?: Long.MAX_VALUE }
                .mapNotNull { document ->
                    @Suppress("UNCHECKED_CAST")
                    val image = document.get("image") as? Map<String, Any?>
                    image?.get("url") as? String
                }

            HomeCatalogContent(
                products = if (curatedProducts.isNotEmpty()) curatedProducts else productResponses,
                featuredProducts = featuredProducts,
                newArrivals = newArrivals,
                banners = bannerUrls,
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun activeProductsQuery(): Query = firestore.collection(PRODUCTS)
        .whereEqualTo("status", STATUS_ACTIVE)

    private suspend fun loadActiveCatalogProducts(): List<CatalogProduct> =
        activeProductsQuery()
            .get()
            .await()
            .documents
            .mapNotNull { it.toCatalogProduct() }

    private suspend fun loadVariants(productDocument: DocumentSnapshot): List<CatalogVariant> =
        productDocument.reference.collection(VARIANTS)
            .whereEqualTo("active", true)
            .get()
            .await()
            .documents
            .mapNotNull { it.toCatalogVariant() }
            .sortedBy(CatalogVariant::sortOrder)

    private suspend fun loadLegacyProducts(): List<ProductResponse> =
        firestore.collection(PRODUCTS)
            .get()
            .await()
            .documents
            .mapNotNull { document -> runCatching { document.toObject(ProductResponse::class.java) }.getOrNull() }

    private suspend fun loadLegacyProductDetail(id: Int): ProductResponse? =
        firestore.collection(PRODUCTS)
            .whereEqualTo("id", id)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.let { document -> runCatching { document.toObject(ProductResponse::class.java) }.getOrNull() }

    private suspend fun loadLegacyCategories(): List<CategoryApiResponse> =
        firestore.collection(CATEGORIES)
            .get()
            .await()
            .documents
            .mapNotNull { document -> runCatching { document.toObject(CategoryApiResponse::class.java) }.getOrNull() }

    private suspend fun loadLegacyProductsByCategory(slug: String): List<ProductResponse> =
        firestore.collection(PRODUCTS)
            .whereEqualTo("category", slug)
            .get()
            .await()
            .documents
            .mapNotNull { document -> runCatching { document.toObject(ProductResponse::class.java) }.getOrNull() }

    private fun DocumentSnapshot.toCatalogProduct(): CatalogProduct? =
        CatalogDocumentMapper.product(id, data.orEmpty())

    private fun DocumentSnapshot.toCatalogVariant(): CatalogVariant? =
        CatalogDocumentMapper.variant(id, data.orEmpty())

    private fun DocumentSnapshot.toCatalogCategory(): CatalogCategory? =
        CatalogDocumentMapper.category(id, data.orEmpty())

    private fun <T> List<T>.window(limit: Int, skip: Int): List<T> {
        val safeLimit = limit.coerceAtLeast(0)
        val safeSkip = skip.coerceAtLeast(0)
        return if (safeSkip < size) drop(safeSkip).take(safeLimit) else take(safeLimit)
    }

    private companion object {
        const val PRODUCTS = "products"
        const val CATEGORIES = "categories"
        const val VARIANTS = "variants"
        const val STATUS_ACTIVE = "ACTIVE"
        const val TOP_PICKS_CATEGORY = "top-picks"
        const val BANNERS = "banners"
        const val HOME_SECTIONS = "homeSections"
        const val HOME_ITEMS = "items"
    }
}
