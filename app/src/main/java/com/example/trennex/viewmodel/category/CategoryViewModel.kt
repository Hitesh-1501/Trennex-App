package com.example.trennex.viewmodel.category

import androidx.lifecycle.ViewModel
import com.example.trennex.R
import com.example.trennex.ui.category.model.*
import com.example.trennex.ui.category.adapter.*
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

    fun getFashionItems(): List<FashionCategoryModel> {
        val list = mutableListOf<FashionCategoryModel>()
        val typeItem = FashionCategoryAdapter.TYPE_ITEM
        val typeHeader = FashionCategoryAdapter.TYPE_HEADER
        val typeBanner = FashionCategoryAdapter.TYPE_BANNER

        list.add(FashionCategoryModel(typeBanner))
        list.add(FashionCategoryModel(typeHeader, "Men's Clothing"))
        list.add(FashionCategoryModel(typeItem, "Winter Wear", R.drawable.men_winter))
        list.add(FashionCategoryModel(typeItem, "Bottom wear", R.drawable.men_bottom))
        list.add(FashionCategoryModel(typeItem, "Top wear", R.drawable.men_top))
        list.add(FashionCategoryModel(typeItem, "Ethnic Wear", R.drawable.men_ethic))
        list.add(FashionCategoryModel(typeItem, "Tshirts", R.drawable.men_tshirts))
        list.add(FashionCategoryModel(typeItem, "Casual Wear", R.drawable.men_casual))
        list.add(FashionCategoryModel(typeItem, "Suits", R.drawable.men_suits))
        list.add(FashionCategoryModel(typeItem, "Formal wear", R.drawable.men_formal))
        list.add(FashionCategoryModel(typeItem, "Sports wear", R.drawable.men_sports))

        list.add(FashionCategoryModel(typeHeader, "Men's Footwear"))
        list.add(FashionCategoryModel(typeItem, "Sport shoes", R.drawable.shoes_sport))
        list.add(FashionCategoryModel(typeItem, "Casual shoes", R.drawable.shoes_casual))
        list.add(FashionCategoryModel(typeItem, "Shoes", R.drawable.shoes))
        list.add(FashionCategoryModel(typeItem, "Sneakers", R.drawable.sneakers))
        list.add(FashionCategoryModel(typeItem, "Formal Shoes", R.drawable.formal_shoes))

        list.add(FashionCategoryModel(typeHeader, "Women's Clothing"))
        list.add(FashionCategoryModel(typeItem, "Sarees", R.drawable.women_sarees))
        list.add(FashionCategoryModel(typeItem, "Kurtis", R.drawable.women_kurtis))
        list.add(FashionCategoryModel(typeItem, "Sweaters", R.drawable.women_sweaters))
        list.add(FashionCategoryModel(typeItem, "Jeans", R.drawable.women_jeans))
        list.add(FashionCategoryModel(typeItem, "Jackets", R.drawable.women_jackets))
        list.add(FashionCategoryModel(typeItem, "Blazer, Coat", R.drawable.women_blazer))
        list.add(FashionCategoryModel(typeItem, "TrackSuit", R.drawable.women_tracksuit))
        list.add(FashionCategoryModel(typeItem, "Lehenga Choli", R.drawable.women_lehenga))
        list.add(FashionCategoryModel(typeItem, "Apparel Sets", R.drawable.women_apparel))

        list.add(FashionCategoryModel(typeHeader, "Women's Footwear"))
        list.add(FashionCategoryModel(typeItem, "Sport Shoes", R.drawable.women_sports))
        list.add(FashionCategoryModel(typeItem, "Heels & Flats", R.drawable.women_hills))
        list.add(FashionCategoryModel(typeItem, "Slippers", R.drawable.slippers))
        list.add(FashionCategoryModel(typeItem, "Boots", R.drawable.women_boots))
        list.add(FashionCategoryModel(typeItem, "Sneakers", R.drawable.women_sneakers))

        list.add(FashionCategoryModel(typeHeader, "Suitcase, Bags & Backpacks"))
        list.add(FashionCategoryModel(typeItem, "Backpacks", R.drawable.backpacks))
        list.add(FashionCategoryModel(typeItem, "Sling Bags", R.drawable.sling_bags))
        list.add(FashionCategoryModel(typeItem, "Travel Bags", R.drawable.travel_bags))
        list.add(FashionCategoryModel(typeItem, "Duffel Bags", R.drawable.duffel_bags))

        list.add(FashionCategoryModel(typeHeader, "Kid's Fashion"))
        list.add(FashionCategoryModel(typeItem, "Kid's Styles", R.drawable.kids_style))
        list.add(FashionCategoryModel(typeItem, "Kid's Winter", R.drawable.kids_winter))
        list.add(FashionCategoryModel(typeItem, "Kid's Sweater", R.drawable.kids_sweater))
        list.add(FashionCategoryModel(typeItem, "Kid's Shoes", R.drawable.kids_shoes))
        list.add(FashionCategoryModel(typeItem, "Kid's Ethic Sets", R.drawable.kids_ethic))

        return list
    }

    fun getApplianceItems(): List<ApplianceCategoryModel> {
        val typeItem = ApplianceAdapter.TYPE_ITEM
        val typeBanner = ApplianceAdapter.TYPE_BANNER
        val typeHeader = ApplianceAdapter.TYPE_HEADER
        return listOf(
            ApplianceCategoryModel(typeBanner),
            ApplianceCategoryModel(typeHeader, "Top Appliances"),
            ApplianceCategoryModel(typeItem, "Refrigerators", R.drawable.fridge),
            ApplianceCategoryModel(typeItem, "Televisions", R.drawable.appliance_tv),
            ApplianceCategoryModel(typeItem, "Washing Machines", R.drawable.appliance_washing_machine),
            ApplianceCategoryModel(typeItem, "Air Conditioners", R.drawable.appliance_air_conditioner)
        )
    }

    fun getMobileItems(): List<MobileCategoryModel> {
        return listOf(
            MobileCategoryModel(R.drawable.iv_mi),
            MobileCategoryModel(R.drawable.iv_samsung),
            MobileCategoryModel(R.drawable.iv_iphone),
            MobileCategoryModel(R.drawable.iv_realme),
            MobileCategoryModel(R.drawable.iv_poco),
            MobileCategoryModel(R.drawable.iv_vivo),
            MobileCategoryModel(R.drawable.iv_oppo),
            MobileCategoryModel(R.drawable.iv_moto),
            MobileCategoryModel(R.drawable.iv_gpixel),
            MobileCategoryModel(R.drawable.iv_nothing)
        )
    }

    fun getBeautyItems(): List<BeautyCategoryModel> {
        val typeItem = BeautyCategoryAdapter.TYPE_BEAUTY_ITEM
        val typeHeader = BeautyCategoryAdapter.TYPE_BEAUTY_HEADER
        val typeBanner = BeautyCategoryAdapter.TYPE_BEAUTY_BANNER
        return listOf(
            BeautyCategoryModel(typeBanner),
            BeautyCategoryModel(typeHeader, title = "Personal Care"),
            BeautyCategoryModel(typeItem, image = R.drawable.bath_spa, title = "Bath & Spa"),
            BeautyCategoryModel(typeItem, image = R.drawable.face_wash, title = "Face Wash"),
            BeautyCategoryModel(typeHeader, title = "Makeup"),
            BeautyCategoryModel(typeItem, image = R.drawable.lipstick, title = "Lipstick"),
            BeautyCategoryModel(typeItem, image = R.drawable.mascara, title = "Mascara")
        )
    }

    fun getHomeItems(): List<HomeCategoryModel> {
        val typeItem = HomeCategoryAdapter.TYPE_HOME_ITEM
        val typeHeader = HomeCategoryAdapter.TYPE_HOME_HEADER
        val typeBanner = HomeCategoryAdapter.TYPE_HOME_BANNER
        return listOf(
            HomeCategoryModel(typeBanner),
            HomeCategoryModel(typeHeader, title = "Home Decor"),
            HomeCategoryModel(typeItem, image = R.drawable.home_wallclock, title = "Wall Clocks"),
            HomeCategoryModel(typeItem, image = R.drawable.home_walldecor, title = "Wall Decor"),
            HomeCategoryModel(typeHeader, title = "Home Furnishing"),
            HomeCategoryModel(typeItem, image = R.drawable.home_bedsheets, title = "Bedsheets"),
            HomeCategoryModel(typeItem, image = R.drawable.home_curtains, title = "Curtains")
        )
    }

    fun getFurnitureItems(): List<FurnitureCategoryModel> {
        val typeItem = FurnitureCategoryAdapter.TYPE_FURNITURE_ITEM
        val typeHeader = FurnitureCategoryAdapter.TYPE_FURNITURE_HEADER
        val typeBanner = FurnitureCategoryAdapter.TYPE_FURNITURE_BANNER
        return listOf(
            FurnitureCategoryModel(typeBanner),
            FurnitureCategoryModel(typeHeader, title = "Bedroom Furniture"),
            FurnitureCategoryModel(typeItem, image = R.drawable.beds, title = "Beds"),
            FurnitureCategoryModel(typeItem, image = R.drawable.mattresses, title = "Mattresses"),
            FurnitureCategoryModel(typeHeader, title = "Living Room Furniture"),
            FurnitureCategoryModel(typeItem, image = R.drawable.home_sofa, title = "Sofas"),
            FurnitureCategoryModel(typeItem, image = R.drawable.coffee_tables, title = "Coffee Tables")
        )
    }

    fun getToysItems(): List<ToysCategoryModel> {
        val typeItem = ToysCategoryAdapter.TYPE_TOYS_ITEM
        val typeHeader = ToysCategoryAdapter.TYPE_TOYS_HEADER
        val typeBanner = ToysCategoryAdapter.TYPE_TOYS_BANNER
        return listOf(
            ToysCategoryModel(typeBanner),
            ToysCategoryModel(typeHeader, title = "Toys & Games"),
            ToysCategoryModel(typeItem, image = R.drawable.toys, title = "Soft Toys"),
            ToysCategoryModel(typeItem, image = R.drawable.toys_games, title = "Board Games")
        )
    }

    fun getSportsItems(): List<SportsCategoryModel> {
        val typeItem = SportsCategoryAdapter.TYPE_SPORTS_ITEM
        val typeHeader = SportsCategoryAdapter.TYPE_SPORTS_HEADER
        val typeBanner = SportsCategoryAdapter.TYPE_SPORTS_BANNER
        return listOf(
            SportsCategoryModel(typeBanner),
            SportsCategoryModel(typeHeader, title = "Cricket"),
            SportsCategoryModel(typeItem, image = R.drawable.cricket, title = "Cricket Bats"),
            SportsCategoryModel(typeItem, image = R.drawable.badminton, title = "Badminton")
        )
    }

    fun getTopPicksItems(): List<SubCategoryModel> {
        return listOf(
            SubCategoryModel(1, "Fashion", R.drawable.fashion),
            SubCategoryModel(2, "Mobiles", R.drawable.mobile),
            SubCategoryModel(3, "Electronics", R.drawable.electronics),
            SubCategoryModel(4, "Home", R.drawable.home_category),
            SubCategoryModel(5, "Beauty", R.drawable.beauty),
            SubCategoryModel(6, "Appliances", R.drawable.appliances),
            SubCategoryModel(7, "Furniture", R.drawable.furniture),
            SubCategoryModel(8, "Sports", R.drawable.sports)
        )
    }
}