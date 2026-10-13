package com.crystal.alarm.data.local

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

internal object PreferenceKeys {
    // Alarm
    val ALARM_MINUTE_OF_DAY = intPreferencesKey("alarm_minute_of_day")
    /** Repeat days of older versions: ignored, removed on the next write. */
    val LEGACY_ALARM_REPEAT_DAYS = stringSetPreferencesKey("alarm_repeat_days")
    val ALARM_TRIGGER_AT = longPreferencesKey("alarm_trigger_at")
    val ALARM_RINGING_SINCE = longPreferencesKey("alarm_ringing_since")

    // Lock
    val LOCK_SINCE = longPreferencesKey("lock_since")
    val LOCK_UNTIL = longPreferencesKey("lock_until")

    // Wake codes (JSON list of WakeCodeDto)
    val WAKE_CODES_JSON = stringPreferencesKey("wake_codes_json")
    val ACTIVE_WAKE_CODE_ID = stringPreferencesKey("active_wake_code_id")

    /** Single code of v1.0, migrated on read into [WAKE_CODES_JSON]. */
    val LEGACY_WAKE_CODE_VALUE = stringPreferencesKey("wake_code_value")
    val LEGACY_WAKE_CODE_FORMAT = stringPreferencesKey("wake_code_format")

    // Settings
    val LOCK_MINUTES = intPreferencesKey("lock_minutes")
    val SOUND_RINGTONE_ID = stringPreferencesKey("sound_ringtone_id")
    val SOUND_VOLUME = intPreferencesKey("sound_volume")
    val SOUND_PROGRESSIVE = booleanPreferencesKey("sound_progressive")
    val SOUND_VIBRATE = booleanPreferencesKey("sound_vibrate")
    val SLEEP_GOAL_MINUTES = intPreferencesKey("sleep_goal_minutes")

    // Wake-up history (JSON list of WakeUpRecordDto, newest first)
    val WAKE_UP_HISTORY_JSON = stringPreferencesKey("wake_up_history_json")

    // Imported ringtones (JSON list of ImportedRingtoneDto)
    val IMPORTED_RINGTONES_JSON = stringPreferencesKey("imported_ringtones_json")
}
