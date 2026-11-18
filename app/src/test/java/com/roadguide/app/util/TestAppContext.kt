package com.roadguide.app.util

import android.content.Context
import android.content.ContextWrapper

/**
 * Helper class to provide test context for Robolectric tests.
 * Note: This is a simple wrapper for test context access.
 */
object TestAppContext {

    @Volatile
    private var applicationContext: Context? = null

    fun setApplicationContext(context: Context) {
        applicationContext = context.applicationContext
    }

    fun getApplicationContext(): Context {
        return applicationContext
            ?: throw IllegalStateException("Application context not set")
    }
}
