package com.example.trennex.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.example.trennex.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

class FirebaseAnalyticsTracker private constructor(
    context: Context,
    private val eligible: Boolean,
) : AnalyticsTracker {
    private val preferences = context.applicationContext.getSharedPreferences("analytics_preferences", Context.MODE_PRIVATE)
    private val firebase by lazy { FirebaseAnalytics.getInstance(context.applicationContext) }
    private val purchaseGate = PurchaseEventGate(object : PurchaseEventGate.Storage {
        override fun contains(orderId: String) = preferences.getBoolean("purchase_$orderId", false)
        override fun reserve(orderId: String) =
            preferences.edit().putBoolean("purchase_$orderId", true).commit()
    })
    override val isCollectionEnabled: Boolean get() = eligible && preferences.getBoolean("consent", false)

    init {
        // The manifest disables collection before the first SDK initialization.
        if (eligible) runCatching { firebase.setAnalyticsCollectionEnabled(isCollectionEnabled) }
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("consent", enabled).apply()
        Log.i(TAG, "Analytics preference updated; enabled=$enabled, eligible=$eligible")
        if (eligible) runCatching { firebase.setAnalyticsCollectionEnabled(enabled) }
            .onFailure { Log.w(TAG, "Could not update analytics preference", it) }
    }

    override fun track(event: AnalyticsEvent) {
        if (!isCollectionEnabled || !AnalyticsPrivacy.isSafe(event)) return
        runCatching { firebase.logEvent(event.name, event.parameters.toBundle()) }
            .onFailure { Log.w(TAG, "Analytics event dropped: ${event.name}", it) }
    }

    override fun trackPurchaseOnce(orderId: String, event: AnalyticsEvent) {
        if (!isCollectionEnabled || !purchaseGate.reserve(orderId, event)) return
        track(event)
    }

    private fun Map<String, Any>.toBundle(): Bundle = Bundle().also { bundle ->
        for ((key, value) in this) when (value) {
            is String -> bundle.putString(key, value)
            is Int -> bundle.putInt(key, value)
            is Long -> bundle.putLong(key, value)
            is Double -> bundle.putDouble(key, value)
            is List<*> -> bundle.putParcelableArray(key, value.mapNotNull {
                (it as? Map<*, *>)?.let { map ->
                    @Suppress("UNCHECKED_CAST")
                    (map as Map<String, Any>).toBundle()
                }
            }.toTypedArray())
        }
    }

    companion object {
        private const val TAG = "AnalyticsTracker"
        fun create(context: Context): AnalyticsTracker {
            val projectId = runCatching { FirebaseApp.getInstance().options.projectId }.getOrNull()
            val eligible = AnalyticsEnvironment.canCollect(BuildConfig.BUILD_TYPE,
                projectId, BuildConfig.USE_FIREBASE_EMULATORS)
            Log.i(TAG, "Analytics route build=${BuildConfig.BUILD_TYPE}, eligible=$eligible, " +
                "firebaseInitialized=${projectId != null}")
            if (!eligible) {
                // Build variants share an application ID, so clear any persisted SDK override.
                runCatching { FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(false) }
                return NoOpAnalyticsTracker()
            }
            return FirebaseAnalyticsTracker(context, true)
        }
    }
}

object AppAnalytics {
    @Volatile private var tracker: AnalyticsTracker = NoOpAnalyticsTracker()
    fun initialize(context: Context) { tracker = FirebaseAnalyticsTracker.create(context) }
    fun get(): AnalyticsTracker = tracker
}
