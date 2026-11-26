package com.example.trennex.analytics

import org.junit.Assert.*
import org.junit.Test

class AnalyticsEventsTest {
    @Test fun codOrderUsesAuthoritativeTotalButNoCollectedRevenue() {
        val event = AnalyticsEvents.confirmedCodOrder("order_123", 4199900,
            listOf(AnalyticsItem("phone_1", "black_128", "mobiles", 41999.0, 1)))
        assertEquals(AnalyticsNames.PURCHASE, event.name)
        assertEquals("INR", event.parameters[AnalyticsParams.CURRENCY])
        assertEquals(41999.0, event.parameters[AnalyticsParams.ORDER_VALUE])
        assertEquals("NOT_COLLECTED", event.parameters[AnalyticsParams.PAYMENT_STATUS])
        assertFalse(event.parameters.containsKey(AnalyticsParams.VALUE))
        assertTrue(AnalyticsPrivacy.isSafe(event))
    }

    @Test fun rejectsSensitiveAndUnrestrictedSearchParameters() {
        assertFalse(AnalyticsPrivacy.isSafe(AnalyticsEvent(AnalyticsNames.SEARCH,
            mapOf("query" to "my phone number"))))
        assertFalse(AnalyticsPrivacy.isSafe(AnalyticsEvent(AnalyticsNames.SEARCH,
            mapOf("search_term" to "name@example.com"))))
        assertFalse(AnalyticsPrivacy.isSafe(AnalyticsEvent(AnalyticsNames.ASSISTANT_REQUEST,
            mapOf("prompt" to "my address"))))
        assertTrue(AnalyticsPrivacy.isSafe(AnalyticsEvents.search("photo", 2)))
    }

    @Test fun purchaseReservationPreventsDuplicateCallbacksAndRecreation() {
        val reserved = mutableSetOf<String>()
        val store = object : PurchaseEventGate.Storage {
            override fun contains(orderId: String) = orderId in reserved
            override fun reserve(orderId: String) = reserved.add(orderId)
        }
        val event = AnalyticsEvents.confirmedCodOrder("order_123", 10000,
            listOf(AnalyticsItem("product_1", price = 100.0)))
        assertTrue(PurchaseEventGate(store).reserve("order_123", event))
        assertFalse(PurchaseEventGate(store).reserve("order_123", event))
        assertFalse(PurchaseEventGate(store).reserve("other_order", event))
    }

    @Test fun noOpNeverCollects() {
        val tracker = NoOpAnalyticsTracker()
        tracker.setCollectionEnabled(true)
        tracker.track(AnalyticsEvents.search("text", 3))
        assertFalse(tracker.isCollectionEnabled)
    }

    @Test fun environmentRoutingRejectsCiAndCrossProjectConfigs() {
        assertFalse(AnalyticsEnvironment.canCollect("staging", "demo-trennex", false))
        assertFalse(AnalyticsEnvironment.canCollect("staging", "trennex-ebbeb", false))
        assertFalse(AnalyticsEnvironment.canCollect("staging", "trennex-staging-ebbeb", true))
        assertFalse(AnalyticsEnvironment.canCollect("debug", "trennex-staging-ebbeb", false))
        assertTrue(AnalyticsEnvironment.canCollect("staging", "trennex-staging-ebbeb", false))
    }
}
