package com.example.trennex.viewmodel.category

import androidx.lifecycle.ViewModel
import com.example.trennex.R
import com.example.trennex.ui.category.model.CategoryModel
import com.example.trennex.ui.category.model.ElectronicsCategoryModel
import com.example.trennex.ui.category.adapter.ElectronicsAdapter.Companion.TYPE_ELECTRONICS_BANNER
import com.example.trennex.ui.category.adapter.ElectronicsAdapter.Companion.TYPE_ELECTRONICS_ITEM
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CategoryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        val list = listOf(
            CategoryModel(1, "Top Picks", R.drawable.iv_top_picks),
            CategoryModel(2, "Fashion", R.drawable.iv_fashion),
            CategoryModel(3, "Appliances", R.drawable.iv_appliances),
            CategoryModel(4, "Mobiles", R.drawable.iv_mobiles),
            CategoryModel(5, "Electronics", R.drawable.iv_electronics),
            CategoryModel(6, "Home", R.drawable.iv_home),
            CategoryModel(7, "Beauty", R.drawable.iv_beauty),
            CategoryModel(8, "Furniture", R.drawable.iv_furniture),
            CategoryModel(9, "Toys,Baby,Books", R.drawable.iv_toys),
            CategoryModel(10, "Sports", R.drawable.iv_sport)
        )
        _uiState.update { it.copy(categories = list, selectedCategory = list.first()) }
    }

    fun selectCategory(category: CategoryModel) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun getElectronicsItems(): List<ElectronicsCategoryModel> {
        return listOf(
            ElectronicsCategoryModel(TYPE_ELECTRONICS_BANNER),
            ElectronicsCategoryModel(TYPE_ELECTRONICS_ITEM, R.drawable.electronics_laptops, "Laptops"),
            ElectronicsCategoryModel(TYPE_ELECTRONICS_ITEM, R.drawable.electronics_tab, "Tablets"),
            ElectronicsCategoryModel(TYPE_ELECTRONICS_ITEM, R.drawable.electronicss_grommings, "Gromming"),
            ElectronicsCategoryModel(TYPE_ELECTRONICS_ITEM, R.drawable.electronics_desktop, "Desktop"),
            ElectronicsCategoryModel(TYPE_ELECTRONICS_ITEM, R.drawable.electronics_acc, "Mobile Accessories"),
            ElectronicsCategoryModel(TYPE_ELECTRONICS_ITEM, R.drawable.electronics_power_banks, "Power Banks"),
            ElectronicsCategoryModel(TYPE_ELECTRONICS_ITEM, R.drawable.electronics_gaming, "Gaming"),
            ElectronicsCategoryModel(TYPE_ELECTRONICS_ITEM, R.drawable.electronic_headphones, "Headphones"),
            ElectronicsCategoryModel(TYPE_ELECTRONICS_ITEM, R.drawable.electronnic_watches, "Watches"),
            ElectronicsCategoryModel(TYPE_ELECTRONICS_ITEM, R.drawable.electronics_camera, "Camera")
        )
    }
}