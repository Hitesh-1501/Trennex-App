package com.example.trennex.crash

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/** Available only in the staging source set; launch manually with ADB for B3 verification. */
class StagingCrashTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }
        content.addView(TextView(this).apply {
            text = "Staging Crashlytics test. Enable crash-report consent in Profile first."
        })
        content.addView(Button(this).apply {
            text = "Record safe non-fatal"
            setOnClickListener {
                if (!AppCrash.get().isCollectionEnabled) {
                    Toast.makeText(this@StagingCrashTestActivity, "Enable crash reports in Profile first", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                AppCrash.get().recordUnexpected(CrashArea.STAGING_TEST,
                    IllegalStateException("Staging non-fatal source message must be removed"))
                Toast.makeText(this@StagingCrashTestActivity, "Non-fatal queued; restart app", Toast.LENGTH_LONG).show()
            }
        })
        content.addView(Button(this).apply {
            text = "Trigger controlled fatal crash"
            setOnClickListener {
                if (!AppCrash.get().isCollectionEnabled) {
                    Toast.makeText(this@StagingCrashTestActivity, "Enable crash reports in Profile first", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                AlertDialog.Builder(this@StagingCrashTestActivity)
                    .setTitle("Crash staging app?")
                    .setMessage("This closes the staging app so Firebase can record one test crash.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Crash") { _, _ ->
                        throw IllegalStateException("TrenNex B3 controlled staging crash")
                    }
                    .show()
            }
        })
        setContentView(content)
    }
}
