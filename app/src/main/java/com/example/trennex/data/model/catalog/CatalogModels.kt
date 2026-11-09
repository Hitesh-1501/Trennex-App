package com.example.trennex.data.model.catalog

import com.example.trennex.data.model.CategoryApiResponse
import com.example.trennex.data.model.ProductResponse

data class CatalogImage(
    val storagePath: String = "",
    val url: String = "",
    val altText: String = "",
    val sortOrder: Int = 0,
)

data class CatalogProduct(
    val documentId: String,
    val legacyId: Int,
    val name: String,
    val description: String,
    val shortDescription: String,
    val brandId: String?,
    val categoryId: String,
    val tags: List<String>,
    val thumbnail: CatalogImage,
    val images: List<CatalogImage>,
    val minPriceMinor: Long,
    val discountPercent: Double,
    val ratingAverage: Double,
    val ratingCount: Int,
    val availability: String,
    val shippingInformation: String,
    val warrantyInformation: String,
    val returnPolicy: String,
    val specifications: Map<String, String>,
    val featured: Boolean,
    val newArrival: Boolean,
    val sortOrder: Int,
) {
    fun toProductResponse(variants: List<CatalogVariant> = emptyList()): ProductResponse {
        val imageUrls = images
            .sortedBy(CatalogImage::sortOrder)
            .map(CatalogImage::url)
            .filter(String::isNotBlank)
            .ifEmpty { listOf(thumbnail.url).filter(String::isNotBlank) }
        val colorOptions = variants
            .mapNotNull { it.attributes["color"] ?: it.attributes["shade"] }
            .distinct()
            .takeIf(List<String>::isNotEmpty)
        val variantOptions = variants
            .sortedBy(CatalogVariant::sortOrder)
            .map(CatalogVariant::name)
            .filter(String::isNotBlank)
            .takeIf(List<String>::isNotEmpty)

        return ProductResponse(
            id = legacyId,
            title = name,
            description = description.ifBlank { shortDescription },
            price = minPriceMinor / 100.0,
            images = imageUrls,
            discountPercentage = discountPercent,
            thumbnail = thumbnail.url,
            reviews = emptyList(),
            shippingInformation = shippingInformation,
            warrantyInformation = warrantyInformation,
            returnPolicy = returnPolicy,
            category = categoryId,
            brand = brandId?.replaceFirstChar(Char::uppercase),
            sku = variants.firstOrNull()?.sku,
            availabilityStatus = availability,
            tags = tags,
            colors = colorOptions,
            variants = variantOptions,
        )
    }
}

data class CatalogVariant(
    val documentId: String,
    val sku: String,
    val name: String,
    val attributes: Map<String, String>,
    val priceMinor: Long,
    val mrpMinor: Long,
    val active: Boolean,
    val sortOrder: Int,
)

data class CatalogCategory(
    val documentId: String,
    val slug: String,
    val name: String,
    val active: Boolean,
    val sortOrder: Int,
) {
    fun toCategoryApiResponse() = CategoryApiResponse(
        slug = slug,
        name = name,
        url = "",
    )
}

data class HomeCatalogContent(
    val products: List<ProductResponse>,
    val featuredProducts: List<ProductResponse>,
    val newArrivals: List<ProductResponse>,
    val banners: List<String>,
)
