package com.example.trennex.analytics

import com.example.trennex.data.model.ProductResponse
import com.example.trennex.ui.cart.model.CartItemModel
import com.example.trennex.ui.home.model.ProductModel

/** The only entry point used by screens and repositories. Analytics must never affect a user action. */
interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
    fun trackPurchaseOnce(orderId: String, event: AnalyticsEvent)
    fun setCollectionEnabled(enabled: Boolean)
    val isCollectionEnabled: Boolean
}

data class AnalyticsEvent(val name: String, val parameters: Map<String, Any> = emptyMap())

object AnalyticsNames {
    const val SCREEN_VIEW = "screen_view"
    const val VIEW_ITEM_LIST = "view_item_list"
    const val SELECT_ITEM = "select_item"
    const val VIEW_ITEM = "view_item"
    const val SELECT_PROMOTION = "select_promotion"
    const val SEARCH = "search"
    const val ADD_TO_CART = "add_to_cart"
    const val REMOVE_FROM_CART = "remove_from_cart"
    const val ADD_TO_WISHLIST = "add_to_wishlist"
    const val VIEW_CART = "view_cart"
    const val BEGIN_CHECKOUT = "begin_checkout"
    const val ADD_SHIPPING_INFO = "add_shipping_info"
    const val ADD_PAYMENT_INFO = "add_payment_info"
    const val PURCHASE = "purchase"
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
    const val ASSISTANT_REQUEST = "assistant_request"
    const val ASSISTANT_RESPONSE = "assistant_response"
    const val ASSISTANT_SELECT_ITEM = "assistant_select_item"
    const val ASSISTANT_ERROR = "assistant_error"
}

object AnalyticsParams {
    const val ITEMS = "items"
    const val ITEM_ID = "item_id"
    const val ITEM_VARIANT = "item_variant"
    const val ITEM_CATEGORY = "item_category"
    const val QUANTITY = "quantity"
    const val PRICE = "price"
    const val CURRENCY = "currency"
    const val VALUE = "value"
    const val TRANSACTION_ID = "transaction_id"
    const val ITEM_LIST_ID = "item_list_id"
    const val PROMOTION_ID = "promotion_id"
    const val SCREEN_NAME = "screen_name"
    const val METHOD = "method"
    const val RESULT_COUNT = "result_count"
    const val SEARCH_SOURCE = "search_source"
    const val RESPONSE_LATENCY_MS = "response_latency_ms"
    const val RECOMMENDATION_COUNT = "recommendation_count"
    const val ERROR_CODE = "error_code"
    const val PAYMENT_TYPE = "payment_type"
    const val PAYMENT_STATUS = "payment_status"
    const val ORDER_VALUE = "order_value"
}

data class AnalyticsItem(
    val id: String,
    val variantId: String? = null,
    val category: String? = null,
    val price: Double,
    val quantity: Int = 1,
) {
    fun parameters(): Map<String, Any> = buildMap {
        put(AnalyticsParams.ITEM_ID, id)
        variantId?.let { put(AnalyticsParams.ITEM_VARIANT, it) }
        category?.let { put(AnalyticsParams.ITEM_CATEGORY, it) }
        put(AnalyticsParams.PRICE, price)
        put(AnalyticsParams.QUANTITY, quantity)
    }
}

object AnalyticsEvents {
    fun item(product: ProductResponse, variantId: String? = null): AnalyticsItem = AnalyticsItem(
        id = product.catalogProductId ?: "legacy:${product.id}", variantId = variantId,
        category = product.category?.lowercase()?.replace(Regex("[^a-z0-9_-]"), "_")
            ?.take(100)?.takeIf { it.isNotBlank() },
        price = product.catalogVariants.firstOrNull { it.id == variantId }?.price ?: product.price,
    )

    fun item(product: ProductModel): AnalyticsItem = AnalyticsItem("legacy:${product.id}", price = product.price)

    fun item(product: CartItemModel): AnalyticsItem = AnalyticsItem(
        id = product.productId ?: "legacy:${product.id}",
        variantId = product.variantId, price = product.price, quantity = product.quantity,
    )

    fun commerce(name: String, items: List<AnalyticsItem>, value: Double? = null,
                 extra: Map<String, Any> = emptyMap()): AnalyticsEvent = AnalyticsEvent(name, buildMap {
        put(AnalyticsParams.CURRENCY, "INR")
        put(AnalyticsParams.ITEMS, items.map(AnalyticsItem::parameters))
        value?.let { put(AnalyticsParams.VALUE, it) }
        putAll(extra)
    })

    /** COD order value is reported separately from collected revenue. */
    fun confirmedCodOrder(orderId: String, totalMinor: Long, items: List<AnalyticsItem>): AnalyticsEvent =
        commerce(AnalyticsNames.PURCHASE, items, extra = mapOf(
            AnalyticsParams.TRANSACTION_ID to orderId,
            AnalyticsParams.ORDER_VALUE to totalMinor / 100.0,
            AnalyticsParams.PAYMENT_TYPE to "COD",
            AnalyticsParams.PAYMENT_STATUS to "NOT_COLLECTED",
        ))

    fun search(source: String, resultCount: Int): AnalyticsEvent = AnalyticsEvent(AnalyticsNames.SEARCH,
        mapOf(AnalyticsParams.SEARCH_SOURCE to source,
            AnalyticsParams.RESULT_COUNT to resultCount.coerceAtLeast(0)))
}

/** Deny by default. No raw query, conversation, identity, address or payment details are allowed. */
object AnalyticsPrivacy {
    private val names = setOf(
        AnalyticsNames.SCREEN_VIEW, AnalyticsNames.VIEW_ITEM_LIST, AnalyticsNames.SELECT_ITEM,
        AnalyticsNames.VIEW_ITEM, AnalyticsNames.SELECT_PROMOTION, AnalyticsNames.SEARCH,
        AnalyticsNames.ADD_TO_CART, AnalyticsNames.REMOVE_FROM_CART,
        AnalyticsNames.ADD_TO_WISHLIST, AnalyticsNames.VIEW_CART,
        AnalyticsNames.BEGIN_CHECKOUT, AnalyticsNames.ADD_SHIPPING_INFO,
        AnalyticsNames.ADD_PAYMENT_INFO, AnalyticsNames.PURCHASE, AnalyticsNames.LOGIN,
        AnalyticsNames.SIGN_UP, AnalyticsNames.ASSISTANT_REQUEST,
        AnalyticsNames.ASSISTANT_RESPONSE, AnalyticsNames.ASSISTANT_SELECT_ITEM,
        AnalyticsNames.ASSISTANT_ERROR, "select_category",
    )
    private val allowed = setOf(
        AnalyticsParams.ITEMS, AnalyticsParams.ITEM_ID, AnalyticsParams.ITEM_VARIANT,
        AnalyticsParams.ITEM_CATEGORY, AnalyticsParams.QUANTITY, AnalyticsParams.PRICE,
        AnalyticsParams.CURRENCY, AnalyticsParams.VALUE, AnalyticsParams.TRANSACTION_ID,
        AnalyticsParams.ITEM_LIST_ID, AnalyticsParams.PROMOTION_ID, AnalyticsParams.SCREEN_NAME,
        AnalyticsParams.METHOD, AnalyticsParams.RESULT_COUNT, AnalyticsParams.SEARCH_SOURCE,
        AnalyticsParams.RESPONSE_LATENCY_MS, AnalyticsParams.RECOMMENDATION_COUNT,
        AnalyticsParams.ERROR_CODE, AnalyticsParams.PAYMENT_TYPE, AnalyticsParams.PAYMENT_STATUS,
        AnalyticsParams.ORDER_VALUE,
    )

    fun isSafe(event: AnalyticsEvent): Boolean = event.name in names &&
        event.parameters.all { (key, value) -> key in allowed && safeValue(key, value) }

    private fun safeValue(key: String, value: Any): Boolean = when {
        key == AnalyticsParams.ITEMS -> value is List<*> && value.size <= 200 && value.all {
            it is Map<*, *> && it.all { (k, v) -> k is String && k in allowed &&
                k != AnalyticsParams.ITEMS && safeValue(k, v ?: return@all false) }
        }
        key == AnalyticsParams.ITEM_ID || key == AnalyticsParams.ITEM_VARIANT ||
            key == AnalyticsParams.TRANSACTION_ID -> value is String &&
            value.matches(Regex("[A-Za-z0-9:_-]{1,100}"))
        key == AnalyticsParams.CURRENCY -> value == "INR"
        key == AnalyticsParams.PAYMENT_TYPE -> value == "COD"
        key == AnalyticsParams.PAYMENT_STATUS -> value == "NOT_COLLECTED"
        value is String -> value.matches(Regex("[A-Za-z0-9_-]{1,100}"))
        value is Int -> value >= 0
        value is Long -> value >= 0
        value is Double -> value.isFinite() && value >= 0
        else -> false
    }
}

class NoOpAnalyticsTracker : AnalyticsTracker {
    override val isCollectionEnabled = false
    override fun track(event: AnalyticsEvent) = Unit
    override fun trackPurchaseOnce(orderId: String, event: AnalyticsEvent) = Unit
    override fun setCollectionEnabled(enabled: Boolean) = Unit
}

/** Reserve before dispatch to favor at-most-once reporting across process recreation. */
class PurchaseEventGate(private val storage: Storage) {
    interface Storage {
        fun contains(orderId: String): Boolean
        fun reserve(orderId: String): Boolean
    }

    @Synchronized fun reserve(orderId: String, event: AnalyticsEvent): Boolean {
        if (event.name != AnalyticsNames.PURCHASE ||
            event.parameters[AnalyticsParams.TRANSACTION_ID] != orderId ||
            !AnalyticsPrivacy.isSafe(event) || storage.contains(orderId)) return false
        return storage.reserve(orderId)
    }
}

object AnalyticsEnvironment {
    fun canCollect(buildType: String, projectId: String?, usesEmulators: Boolean): Boolean =
        !usesEmulators && when (buildType) {
            "staging" -> projectId == "trennex-staging-ebbeb"
            "release" -> projectId == "trennex-ebbeb"
            else -> false
        }
}
