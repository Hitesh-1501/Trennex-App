package com.example.trennex.crash

import android.content.Context
import android.util.Log
import com.example.trennex.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.CustomKeysAndValues
import com.google.firebase.crashlytics.FirebaseCrashlytics

class FirebaseCrashReporter private constructor(context: Context) : CrashReporter {
    override val isAvailable = true
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val firebase = FirebaseCrashlytics.getInstance()
    private val gate = CrashReportGate()

    override val isCollectionEnabled: Boolean get() = preferences.getBoolean(CONSENT, false)

    init {
        // Manifest default is false, including before Application.onCreate.
        runCatching {
            firebase.setCustomKey("environment", "staging")
            firebase.setCrashlyticsCollectionEnabled(isCollectionEnabled)
        }
            .onFailure { Log.w(TAG, "Crash collection configuration failed") }
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(CONSENT, enabled).apply()
        runCatching { firebase.setCrashlyticsCollectionEnabled(enabled) }
            .onFailure { Log.w(TAG, "Crash collection configuration failed") }
    }

    override fun recordUnexpected(area: CrashArea, error: Throwable) {
        if (!isCollectionEnabled || !CrashPrivacy.shouldReport(error) || !gate.allow(area)) return
        runCatching {
            val keys = CustomKeysAndValues.Builder()
                .putString("app_area", area.key)
                .putString("environment", "staging")
                .build()
            firebase.recordException(CrashPrivacy.sanitized(area, error), keys)
        }.onFailure { Log.w(TAG, "Sanitized crash report was not recorded") }
    }

    override fun log(area: CrashArea, step: String) {
        if (!isCollectionEnabled || !CrashPrivacy.safeStep(step)) return
        runCatching { firebase.log("${area.key}:$step") }
    }

    companion object {
        private const val TAG = "CrashReporter"
        private const val PREFERENCES = "crash_reporting_preferences"
        private const val CONSENT = "consent"

        fun create(context: Context): CrashReporter {
            val projectId = runCatching { FirebaseApp.getInstance().options.projectId }.getOrNull()
            if (!CrashEnvironment.canCollect(BuildConfig.BUILD_TYPE, projectId,
                    BuildConfig.USE_FIREBASE_EMULATORS)) {
                // The native SDK override persists, so clear it for demo/debug and other projects.
                runCatching { FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false) }
                return NoOpCrashReporter()
            }
            return FirebaseCrashReporter(context)
        }
    }
}

object AppCrash {
    @Volatile private var reporter: CrashReporter = NoOpCrashReporter()
    fun initialize(context: Context) { reporter = FirebaseCrashReporter.create(context) }
    fun get(): CrashReporter = reporter
}
