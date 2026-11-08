package com.example.trennex.data.model.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogDocumentMapperTest {
    @Test
    fun `maps Firestore numeric values and image maps into a catalog product`() {
        val product = CatalogDocumentMapper.product(
            documentId = "prd_test",
            data = mapOf(
                "legacyId" to 101L,
                "name" to "Test Phone",
                "description" to "A useful test phone",
                "shortDescription" to "Test phone",
                "brandId" to "trennex",
                "categoryId" to "mobiles",
                "tags" to listOf("phone", "5g"),
                "thumbnail" to mapOf(
                    "storagePath" to "catalog/products/prd_test/primary.png",
                    "url" to "http://10.0.2.2:9199/test.png",
                    "altText" to "Test Phone",
                ),
                "images" to listOf(
                    mapOf(
                        "storagePath" to "catalog/products/prd_test/primary.png",
                        "url" to "http://10.0.2.2:9199/test.png",
                        "altText" to "Test Phone",
                        "sortOrder" to 1L,
                    )
                ),
                "minPriceMinor" to 4199900L,
                "discountPercent" to 8.7,
                "ratingAverage" to 4.5,
                "ratingCount" to 42L,
                "availability" to "IN_STOCK",
                "featured" to true,
                "newArrival" to true,
                "sortOrder" to 1L,
            ),
        )

        assertNotNull(product)
        assertEquals(101, product?.legacyId)
        assertEquals(4199900L, product?.minPriceMinor)
        assertEquals("http://10.0.2.2:9199/test.png", product?.thumbnail?.url)
        assertEquals(listOf("phone", "5g"), product?.tags)
    }

    @Test
    fun `converts integer paise and variants to the current UI response`() {
        val product = CatalogProduct(
            documentId = "prd_test",
            legacyId = 101,
            name = "Test Phone",
            description = "Description",
            shortDescription = "Short description",
            brandId = "trennex",
            categoryId = "mobiles",
            tags = listOf("phone"),
            thumbnail = CatalogImage(url = "phone.png"),
            images = listOf(CatalogImage(url = "phone.png", sortOrder = 1)),
            minPriceMinor = 4199900,
            discountPercent = 8.7,
            ratingAverage = 4.5,
            ratingCount = 42,
            availability = "IN_STOCK",
            shippingInformation = "Free delivery",
            warrantyInformation = "1 year",
            returnPolicy = "7-day replacement",
            specifications = emptyMap(),
            featured = true,
            newArrival = true,
            sortOrder = 1,
        )
        val variants = listOf(
            CatalogVariant(
                documentId = "var_black",
                sku = "PHONE-BLK",
                name = "128 GB · Black",
                attributes = mapOf("color" to "Black", "storage" to "128 GB"),
                priceMinor = 4199900,
                mrpMinor = 4599900,
                active = true,
                sortOrder = 1,
            )
        )

        val response = product.toProductResponse(variants)

        assertEquals(41999.0, response.price, 0.0)
        assertEquals(listOf("Black"), response.colors)
        assertEquals(listOf("128 GB · Black"), response.variants)
        assertEquals("PHONE-BLK", response.sku)
    }

    @Test
    fun `rejects incomplete versioned product documents`() {
        val product = CatalogDocumentMapper.product(
            documentId = "prd_invalid",
            data = mapOf("name" to "Missing required fields"),
        )

        assertNull(product)
    }
}
