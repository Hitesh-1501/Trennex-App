package com.example.trennex.crash

import java.io.IOException
import java.util.concurrent.CancellationException
import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctionsException

/** Fixed labels only. Callers must never pass user input or server response text. */
enum class CrashArea(val key: String) {
    AUTH("auth"), HOME("home"), CATALOG("catalog"), SEARCH("search"),
    PRODUCT("product"), CART("cart"), WISHLIST("wishlist"), ROOM("room"),
    CART_SYNC("cart_sync"), CHECKOUT("checkout"), ASSISTANT("assistant"),
    PROFILE("profile"), ADDRESS("address"), STAGING_TEST("staging_test")
}

interface CrashReporter {
    val isAvailable: Boolean
    val isCollectionEnabled: Boolean
    fun setCollectionEnabled(enabled: Boolean)
    fun recordUnexpected(area: CrashArea, error: Throwable)
    fun log(area: CrashArea, step: String)
}

class NoOpCrashReporter : CrashReporter {
    override val isAvailable = false
    override val isCollectionEnabled = false
    override fun setCollectionEnabled(enabled: Boolean) = Unit
    override fun recordUnexpected(area: CrashArea, error: Throwable) = Unit
    override fun log(area: CrashArea, step: String) = Unit
}

object CrashEnvironment {
    // Production reporting needs a separate owner-approved release decision.
    fun canCollect(buildType: String, projectId: String?, usesEmulators: Boolean): Boolean =
        buildType == "staging" && projectId == "trennex-staging-ebbeb" && !usesEmulators
}

object CrashPrivacy {
    private val safeSteps = setOf("start", "loaded", "retry", "complete", "failed")

    fun safeStep(step: String): Boolean = step in safeSteps

    fun shouldReport(error: Throwable): Boolean = when (error) {
        is CancellationException, is IOException, is IllegalArgumentException -> false
        is FirebaseFirestoreException -> error.code.name in setOf("INTERNAL", "UNKNOWN", "DATA_LOSS")
        is FirebaseFunctionsException -> error.code.name in setOf("INTERNAL", "UNKNOWN", "DATA_LOSS")
        is FirebaseException -> false
        else -> true
    }

    /** Drop the original message, causes and non-app stack frames before sending a handled error. */
    fun sanitized(area: CrashArea, error: Throwable): Throwable =
        IllegalStateException("Unexpected ${area.key} failure").apply {
            stackTrace = error.stackTrace
                .filter { it.className.startsWith("com.example.trennex.") }
                .take(32)
                .map { StackTraceElement(it.className, it.methodName, null, it.lineNumber) }
                .toTypedArray()
        }
}

/** Avoid flooding Crashlytics when the same loading or sync failure repeats. */
class CrashReportGate(private val nowMs: () -> Long = System::currentTimeMillis) {
    private val lastByArea = mutableMapOf<CrashArea, Long>()
    private var sent = 0

    @Synchronized fun allow(area: CrashArea): Boolean {
        val now = nowMs()
        val last = lastByArea[area]
        if (sent >= 8 || (last != null && now - last < 60_000L)) return false
        lastByArea[area] = now
        sent++
        return true
    }
}
