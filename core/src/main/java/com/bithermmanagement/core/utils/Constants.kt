package com.bithermmanagement.core.utils

object Constants {
    // Shared Preferences
    const val SHARED_PREFS_NAME = "BithermManagementPrefs"
    const val PREF_USERNAME = "username"
    const val PREF_USER_ID = "user_id"
    const val PREF_IS_LOGGED_IN = "is_logged_in"
    
    // Location Service
    const val LOCATION_SERVICE_ENABLED = "location_service_enabled"
    const val LOCATION_TRACKING_INTERVAL = "location_tracking_interval"
    const val LOCATION_TRACKING_MODE = "location_tracking_mode"
    
    // Google Sheets
    const val DEFAULT_SPREADSHEET_ID = "1IyWGyxYDDTWY5SHh2xLBxtakSZX_xhZFo2jta4JeSW4"
    const val LOCALE_LOGS_SHEET = "LOCALE-LOGS"
    const val CHECKIN_SHEET = "CHECKIN"
    const val EQUIPOS_SHEET = "EQUIPOS"
    
    // Location Tracking Modes
    const val TRACKING_MODE_ALWAYS = "SI"
    const val TRACKING_MODE_WORK = "WK"
    const val TRACKING_MODE_DISABLED = "NO"
    
    // Default Values
    const val DEFAULT_TRACKING_INTERVAL_MINUTES = 15L
    const val MIN_DISTANCE_METERS = 100
    const val MAX_LOCATION_DISTANCE_METERS = 500
} 