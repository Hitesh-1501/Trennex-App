package com.example.trennex.data.model.catalog

object CatalogDocumentMapper {
    fun product(documentId: String, data: Map<String, Any?>): CatalogProduct? {
        val legacyId = data.number("legacyId")?.toInt() ?: return null
        val name = data.string("name") ?: return null
        val categoryId = data.string("categoryId") ?: return null
        val thumbnail = image(data["thumbnail"]) ?: return null

        return CatalogProduct(
            documentId = documentId,
            legacyId = legacyId,
            name = name,
            description = data.string("description").orEmpty(),
            shortDescription = data.string("shortDescription").orEmpty(),
            brandId = data.string("brandId"),
            categoryId = categoryId,
            tags = data.stringList("tags"),
            thumbnail = thumbnail,
            images = data.list("images").mapNotNull(::image).ifEmpty { listOf(thumbnail) },
            minPriceMinor = data.number("minPriceMinor")?.toLong() ?: return null,
            discountPercent = data.number("discountPercent")?.toDouble() ?: 0.0,
            ratingAverage = data.number("ratingAverage")?.toDouble() ?: 0.0,
            ratingCount = data.number("ratingCount")?.toInt() ?: 0,
            availability = data.string("availability").orEmpty(),
            shippingInformation = data.string("shippingInformation").orEmpty(),
            warrantyInformation = data.string("warrantyInformation").orEmpty(),
            returnPolicy = data.string("returnPolicy").orEmpty(),
            specifications = data.stringMap("specifications"),
            featured = data["featured"] as? Boolean ?: false,
            newArrival = data["newArrival"] as? Boolean ?: false,
            sortOrder = data.number("sortOrder")?.toInt() ?: Int.MAX_VALUE,
        )
    }

    fun variant(documentId: String, data: Map<String, Any?>): CatalogVariant? {
        val sku = data.string("sku") ?: return null
        val name = data.string("name") ?: return null
        val priceMinor = data.number("priceMinor")?.toLong() ?: return null
        val mrpMinor = data.number("mrpMinor")?.toLong() ?: return null

        return CatalogVariant(
            documentId = documentId,
            sku = sku,
            name = name,
            attributes = data.stringMap("attributes"),
            priceMinor = priceMinor,
            mrpMinor = mrpMinor,
            active = data["active"] as? Boolean ?: false,
            sortOrder = data.number("sortOrder")?.toInt() ?: Int.MAX_VALUE,
        )
    }

    fun category(documentId: String, data: Map<String, Any?>): CatalogCategory? {
        val slug = data.string("slug") ?: return null
        val name = data.string("name") ?: return null
        return CatalogCategory(
            documentId = documentId,
            slug = slug,
            name = name,
            active = data["active"] as? Boolean ?: false,
            sortOrder = data.number("sortOrder")?.toInt() ?: Int.MAX_VALUE,
        )
    }

    private fun image(value: Any?): CatalogImage? {
        val data = value as? Map<*, *> ?: return null
        val url = data["url"] as? String ?: return null
        return CatalogImage(
            storagePath = data["storagePath"] as? String ?: "",
            url = url,
            altText = data["altText"] as? String ?: "",
            sortOrder = (data["sortOrder"] as? Number)?.toInt() ?: 0,
        )
    }

    private fun Map<String, Any?>.string(key: String) = this[key] as? String
    private fun Map<String, Any?>.number(key: String) = this[key] as? Number
    private fun Map<String, Any?>.list(key: String) = this[key] as? List<*> ?: emptyList<Any?>()
    private fun Map<String, Any?>.stringList(key: String) = list(key).mapNotNull { it as? String }
    private fun Map<String, Any?>.stringMap(key: String): Map<String, String> =
        (this[key] as? Map<*, *>)
            ?.mapNotNull { (mapKey, value) ->
                val stringKey = mapKey as? String ?: return@mapNotNull null
                val stringValue = value as? String ?: return@mapNotNull null
                stringKey to stringValue
            }
            ?.toMap()
            .orEmpty()
}
