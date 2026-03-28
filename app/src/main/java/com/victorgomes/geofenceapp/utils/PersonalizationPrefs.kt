package com.victorgomes.geofenceapp.utils

import android.content.Context
import androidx.annotation.DrawableRes
import com.victorgomes.geofenceapp.R
import org.osmdroid.views.overlay.Marker

object PersonalizationPrefs {

    private const val PREFS_NAME = "personalization"
    private const val KEY_CHAR  = "char_icon"
    private const val KEY_FENCE = "fence_icon"
    private const val KEY_EVENT_NOTIFICATIONS = "event_notifications"

    // Character icon keys
    const val CHAR_MALE   = "male"
    const val CHAR_FEMALE = "female"
    const val CHAR_CAT    = "cat"
    const val CHAR_DOG    = "dog"

    // Fence marker icon keys
    const val FENCE_PIN   = "pin"
    const val FENCE_HOME  = "home"
    const val FENCE_FLAG  = "flag"
    const val FENCE_STAR  = "star"
    const val FENCE_BELL  = "bell"
    const val FENCE_HEART = "heart"

    fun getCharIcon(context: Context): String =
        prefs(context).getString(KEY_CHAR, CHAR_MALE) ?: CHAR_MALE

    fun setCharIcon(context: Context, key: String) =
        prefs(context).edit().putString(KEY_CHAR, key).apply()

    fun getFenceIcon(context: Context): String =
        prefs(context).getString(KEY_FENCE, FENCE_PIN) ?: FENCE_PIN

    fun setFenceIcon(context: Context, key: String) =
        prefs(context).edit().putString(KEY_FENCE, key).apply()

    fun isEventNotificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_EVENT_NOTIFICATIONS, true)

    fun setEventNotificationsEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_EVENT_NOTIFICATIONS, enabled).apply()

    fun hasSeenOnboarding(context: Context): Boolean =
        prefs(context).getBoolean("onboarding_done", false)

    fun setOnboardingSeen(context: Context) =
        prefs(context).edit().putBoolean("onboarding_done", true).apply()

    fun isDebugUnlocked(context: Context): Boolean =
        prefs(context).getBoolean("debug_unlocked", false)

    fun setDebugUnlocked(context: Context, unlocked: Boolean) =
        prefs(context).edit().putBoolean("debug_unlocked", unlocked).apply()

    fun hasSeenTutorial(context: Context): Boolean =
        prefs(context).getBoolean("tutorial_seen", false)

    fun setTutorialSeen(context: Context) =
        prefs(context).edit().putBoolean("tutorial_seen", true).apply()

    @DrawableRes fun charDrawableRes(key: String): Int = when (key) {
        CHAR_FEMALE -> R.drawable.ic_char_female
        CHAR_CAT    -> R.drawable.ic_char_cat
        CHAR_DOG    -> R.drawable.ic_char_dog
        else        -> R.drawable.ic_char_male
    }

    @DrawableRes fun fenceDrawableRes(key: String): Int = when (key) {
        FENCE_HOME  -> R.drawable.ic_fence_home
        FENCE_FLAG  -> R.drawable.ic_fence_flag
        FENCE_STAR  -> R.drawable.ic_fence_star
        FENCE_BELL  -> R.drawable.ic_fence_bell
        FENCE_HEART -> R.drawable.ic_fence_heart
        else        -> R.drawable.ic_fence_pin
    }

    // All icons share the same badge-pin shape — tail tip is at the image bottom.
    fun fenceAnchorY(key: String): Float = Marker.ANCHOR_BOTTOM

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
