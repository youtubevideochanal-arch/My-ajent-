package com.example.myagent

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AgentPreferences(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences =
        EncryptedSharedPreferences.create(
            context,
            "ai_agent_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    // API key ko securely save karo
    fun saveApiKey(keyName: String, apiKey: String) {
        sharedPreferences.edit()
            .putString(keyName, apiKey)
            .apply()
    }

    // Saved API key hasil karo
    fun getApiKey(keyName: String): String? {
        return sharedPreferences.getString(keyName, null)
    }

    // API ko ON/OFF karo; key delete nahi hogi
    fun setApiEnabled(keyName: String, isEnabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean("${keyName}_enabled", isEnabled)
            .apply()
    }

    // Nayi API default OFF rahegi
    fun isApiEnabled(keyName: String): Boolean {
        return sharedPreferences.getBoolean(
            "${keyName}_enabled",
            false
        )
    }

    // API key sirf user ke kehne par delete hogi
    fun deleteApiKey(keyName: String) {
        sharedPreferences.edit()
            .remove(keyName)
            .remove("${keyName}_enabled")
            .apply()
    }

    // Agent ka naam save karo
    fun saveAgentName(name: String) {
        sharedPreferences.edit()
            .putString("AGENT_NAME", name)
            .apply()
    }

    fun getAgentName(): String {
        return sharedPreferences.getString(
            "AGENT_NAME",
            "MY AGENT"
        ) ?: "MY AGENT"
    }
}
