package com.example.trennex.ui.category

import com.example.trennex.data.model.ProductResponse

internal object CategoryProductFilter {
    fun filter(products: List<ProductResponse>, tile: String?): List<ProductResponse> {
        if (tile.isNullOrBlank()) return products
        val terms = tile.lowercase().replace("\\n", " ")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in setOf("and", "the", "wear", "items", "essentials") }
            .map { it.removeSuffix("s") }
        if (terms.isEmpty()) return products
        return products.filter { product ->
            val searchable = listOfNotNull(product.title, product.brand, product.description)
                .plus(product.tags.orEmpty())
                .joinToString(" ").lowercase()
            terms.any { term -> searchable.contains(term) }
        }
    }
}
