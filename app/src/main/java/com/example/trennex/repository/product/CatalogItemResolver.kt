package com.example.trennex.repository.product

import com.example.trennex.data.model.catalog.CatalogDocumentMapper
import com.example.trennex.data.model.catalog.CatalogProduct
import com.example.trennex.data.model.catalog.CatalogVariant
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class CatalogItemReference(
    val product: CatalogProduct,
    val variant: CatalogVariant,
)

class CatalogItemResolver(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun resolveLegacyId(legacyId: Int): CatalogItemReference? {
        val productDocument = firestore.collection(PRODUCTS)
            .whereEqualTo("status", STATUS_ACTIVE)
            .whereEqualTo("legacyId", legacyId)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?: return null

        val product = CatalogDocumentMapper.product(productDocument.id, productDocument.data.orEmpty())
            ?: return null
        val variant = activeVariant(productDocument.reference)
            ?: return null
        return CatalogItemReference(product, variant)
    }

    suspend fun resolve(productId: String, variantId: String): CatalogItemReference? {
        val productDocument = firestore.collection(PRODUCTS).document(productId).get().await()
        if (!productDocument.exists()) return null
        val product = CatalogDocumentMapper.product(productDocument.id, productDocument.data.orEmpty())
            ?: return null
        val variantDocument = productDocument.reference.collection(VARIANTS).document(variantId).get().await()
        val variant = CatalogDocumentMapper.variant(variantDocument.id, variantDocument.data.orEmpty())
            ?: return null
        return if (variant.active) CatalogItemReference(product, variant) else null
    }

    private suspend fun activeVariant(productReference: com.google.firebase.firestore.DocumentReference): CatalogVariant? =
        productReference.collection(VARIANTS)
            .whereEqualTo("active", true)
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                CatalogDocumentMapper.variant(document.id, document.data.orEmpty())
            }
            .minByOrNull(CatalogVariant::sortOrder)

    private companion object {
        const val PRODUCTS = "products"
        const val VARIANTS = "variants"
        const val STATUS_ACTIVE = "ACTIVE"
    }
}
