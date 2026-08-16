package com.example.trennex.viewmodel.category

import com.example.trennex.ui.category.model.CategoryModel

data class CategoryUiState(
    val categories: List<CategoryModel> = emptyList(),
    val selectedCategory: CategoryModel? = null
)