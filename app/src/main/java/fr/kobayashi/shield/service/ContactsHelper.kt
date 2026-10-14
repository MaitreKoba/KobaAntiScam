package fr.kobayashi.shield.service

import android.content.Context
import android.provider.ContactsContract
import fr.kobayashi.shield.model.Rules

/**
 * Helper class to manage contacts and neutral list
 */
object ContactsHelper {
    
    private const val PREFS_NAME = "shield"
    private const val KEY_STRICT_MODE = "strict_mode"
    private const val KEY_NEUTRAL_LIST = "neutral_list"
    private const val KEY_VOICEMAIL_ENABLED = "voicemail_enabled"
    private const val KEY_ALLOWLIST = "allowlist"
    
    /**
     * Check if strict mode is enabled (block all except contacts)
     */
    fun isStrictModeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_STRICT_MODE, false)
    }
    
    /**
     * Enable/disable strict mode
     */
    fun setStrictModeEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_STRICT_MODE, enabled).apply()
    }
    
    /**
     * Check if custom voicemail message is enabled
     */
    fun isVoicemailEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_VOICEMAIL_ENABLED, true)
    }
    
    /**
     * Enable/disable custom voicemail message
     */
    fun setVoicemailEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_VOICEMAIL_ENABLED, enabled).apply()
    }
    
    /**
     * Get the allowlist (contacts that will NOT get the voicemail message)
     */
    fun getAllowlist(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_ALLOWLIST, mutableSetOf()) ?: mutableSetOf()
    }
    
    /**
     * Add a contact to allowlist (will NOT get voicemail message)
     */
    fun addToAllowlist(context: Context, number: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_ALLOWLIST, mutableSetOf()) ?: mutableSetOf()
        val normalized = Rules.normalize(number)
        current.add(normalized)
        prefs.edit().putStringSet(KEY_ALLOWLIST, current).apply()
    }
    
    /**
     * Remove a contact from allowlist
     */
    fun removeFromAllowlist(context: Context, number: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_ALLOWLIST, mutableSetOf()) ?: mutableSetOf()
        val normalized = Rules.normalize(number)
        current.remove(normalized)
        prefs.edit().putStringSet(KEY_ALLOWLIST, current).apply()
    }
    
    /**
     * Check if a number is in allowlist (should NOT get voicemail message)
     */
    fun isInAllowlist(context: Context, number: String): Boolean {
        val normalizedNumber = Rules.normalize(number)
        if (normalizedNumber.isBlank()) return false
        return getAllowlist(context).contains(normalizedNumber)
    }
    
    /**
     * Get the neutral list (contacts that should be blocked even if in contacts)
     */
    fun getNeutralList(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_NEUTRAL_LIST, mutableSetOf()) ?: mutableSetOf()
    }
    
    /**
     * Add a number to neutral list
     */
    fun addToNeutralList(context: Context, number: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_NEUTRAL_LIST, mutableSetOf()) ?: mutableSetOf()
        val normalized = Rules.normalize(number)
        current.add(normalized)
        prefs.edit().putStringSet(KEY_NEUTRAL_LIST, current).apply()
    }
    
    /**
     * Remove a number from neutral list
     */
    fun removeFromNeutralList(context: Context, number: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_NEUTRAL_LIST, mutableSetOf()) ?: mutableSetOf()
        val normalized = Rules.normalize(number)
        current.remove(normalized)
        prefs.edit().putStringSet(KEY_NEUTRAL_LIST, current).apply()
    }
    
    /**
     * Get all contact numbers from the device
     */
    fun getAllContactNumbers(context: Context): Set<String> {
        val contacts = mutableSetOf<String>()
        
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                null,
                null,
                null
            )
            
            cursor?.use {
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    val rawNumber = it.getString(numberIndex)
                    if (!rawNumber.isNullOrBlank()) {
                        val normalized = Rules.normalize(rawNumber)
                        if (normalized.isNotBlank()) {
                            contacts.add(normalized)
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
            android.util.Log.e("ContactsHelper", "READ_CONTACTS permission not granted", e)
        } catch (e: Exception) {
            android.util.Log.e("ContactsHelper", "Error reading contacts", e)
        }
        
        return contacts
    }
    
    /**
     * Check if a number is in contacts and not in neutral list
     */
    fun isContactAndNotNeutral(context: Context, number: String): Boolean {
        val normalizedNumber = Rules.normalize(number)
        if (normalizedNumber.isBlank()) return false
        
        val contacts = getAllContactNumbers(context)
        val neutralList = getNeutralList(context)
        
        return contacts.contains(normalizedNumber) && !neutralList.contains(normalizedNumber)
    }
}
