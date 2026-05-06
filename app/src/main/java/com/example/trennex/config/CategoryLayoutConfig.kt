package com.example.trennex.config

object CategoryLayoutConfig {
    val GRID_CATEGORIES = setOf(

        "shirt", "dress", "cloth", "tshirt", "t-shirt", "polo", "hoodie",
        "sweater", "jacket", "coat", "blazer", "cardigan", "pant", "pants",
        "jeans", "shorts", "skirt", "saree", "lehenga", "top", "camisole",
        "tank", "vest", "robe", "gown", "suit",

        "shoe", "shoes", "sneaker", "boot", "sandal", "flip", "flop",
        "slipper", "heel", "pump", "loafer", "oxford", "athletic",

        "makeup", "cosmetic", "cosmetics", "lipstick", "lip", "gloss", "balm",
        "foundation", "concealer", "compact", "powder", "blush", "highlighter",
        "bronzer", "eyeliner", "kajal", "mascara", "eyeshadow", "palette",
        "primer", "setting", "spray", "makeup kit",

        "skincare", "cleanser", "facewash", "face wash", "serum", "moisturizer",
        "cream", "lotion", "sunscreen", "spf", "toner", "mist", "gel",
        "scrub", "exfoliator", "face pack", "mask", "sheet mask", "night cream",

        "hair", "shampoo", "conditioner", "hair oil", "oil", "serum",
        "hair mask", "hair spa", "gel", "wax", "spray", "mousse",
        "hair color", "dye", "henna",

        "perfume", "fragrance", "deodorant", "deo", "body spray",
        "soap", "body wash", "shower gel", "handwash", "sanitizer",
        "shaving", "razor", "trimmer", "grooming", "beard oil",
        "cream", "aftershave",

        "kitchen", "utensil", "cookware", "pan", "kadai", "pressure cooker",
        "knife", "spoon", "fork", "plate", "bottle", "flask", "container",
        "storage", "tiffin",

        "toy", "toys", "game", "doll", "car toy", "lego", "puzzle",
        "baby", "diaper", "stroller", "pram", "feeding", "bottle",

        "book", "books", "novel", "pen", "pencil", "notebook", "diary",
        "stationery", "marker", "eraser",

        "grocery", "food", "snack", "chips", "drink", "juice", "tea", "coffee",
        "rice", "wheat", "atta", "oil", "spice", "masala", "fruit", "vegetable",

        "pet", "dog", "cat", "pet food", "leash", "collar", "litter", "toy pet",

        "accessory", "accessories", "belt", "bag", "purse", "wallet",
        "watch", "jewelry", "necklace", "bracelet", "earring", "ring",
        "pendant", "chain", "brooch", "pin", "tie", "necktie", "scarf",
        "cap", "hat", "beret", "beanie", "glove", "mitten", "sock",
        "sunglasses", "glass", "headband", "hairpin"
    )

    val LIST_CATEGORIES = setOf(

        "mobile", "phone", "smartphone", "iphone", "android", "laptop",
        "computer", "pc", "desktop", "notebook", "tablet", "ipad",
        "camera", "dslr", "camcorder", "projector", "speaker", "headphone",
        "earphone", "microphone", "keyboard", "mouse", "monitor", "display",
        "printer", "scanner", "charger", "power", "cable",


        "tv", "television", "screen", "refrigerator", "fridge", "ac",
        "air", "conditioner", "washing", "machine", "dryer", "oven",
        "microwave", "dishwasher", "blender", "mixer", "grinder", "cooker",
        "heater", "fan", "humidifier", "dehumidifier", "vacuum", "cleaner",


        "furniture", "sofa", "couch", "bed", "crib", "mattress", "pillow",
        "table", "desk", "dining", "chair", "stool", "bench", "cabinet",
        "wardrobe", "closet", "shelf", "rack", "bookcase", "tv stand",
        "nightstand", "dresser", "drawer", "ottoman",


        "bike", "bicycle", "motorcycle", "scooter", "scooty", "car",
        "vehicle", "auto", "automobile", "engine", "tire", "wheel",

        "treadmill", "dumbbell", "barbell", "gym", "equipment", "yoga",
        "mat", "exercise", "machine", "trainer"
    )

    fun isGridLayout(query: String, category: String?): Boolean{
        val queryLower = query.lowercase()
        val categoryLower = category?.lowercase() ?: ""

        if(GRID_CATEGORIES.any{queryLower.contains(it)}){
            return true
        }
        if(LIST_CATEGORIES.any { queryLower.contains(it)}){
            return false
        }
        if(GRID_CATEGORIES.any{categoryLower.contains(it)}){
            return true
        }
        if(LIST_CATEGORIES.any { categoryLower.contains(it)}){
            return false
        }
        return false
    }

    fun containsGridCategory(text: String): Boolean{
        val textLower = text.lowercase()
        return GRID_CATEGORIES.any { textLower.contains(it) }
    }
    fun containsListCategory(text: String): Boolean {
        val textLower = text.lowercase()
        return LIST_CATEGORIES.any { textLower.contains(it) }
    }
}