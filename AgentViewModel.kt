package com.example.myagent

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AgentViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val prefs = AgentPreferences(application)
    private val apiManager = ApiManager(prefs)

    private val _agentName =
        MutableStateFlow(prefs.getAgentName())
    val agentName: StateFlow<String> =
        _agentName.asStateFlow()

    private val _isAgentOn = MutableStateFlow(false)
    val isAgentOn: StateFlow<Boolean> =
        _isAgentOn.asStateFlow()

    private val _aiResponse = MutableStateFlow("")
    val aiResponse: StateFlow<String> =
        _aiResponse.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()

    fun toggleAgent(isOn: Boolean) {
        _isAgentOn.value = isOn

        if (!isOn) {
            _aiResponse.value = "MY AGENT band hai."
        } else {
            _aiResponse.value = "MY AGENT taiyar hai, boss!"
        }
    }

    fun updateAgentName(newName: String) {
        val cleanedName = newName.trim()

        if (cleanedName.isNotEmpty()) {
            prefs.saveAgentName(cleanedName)
            _agentName.value = cleanedName
        }
    }

    fun sendMessageToAgent(userPrompt: String) {
        val prompt = userPrompt.trim()

        if (!_isAgentOn.value) {
            _aiResponse.value = "Pehle Agent ko ON karein, boss!"
            return
        }

        if (prompt.isEmpty()) {
            _aiResponse.value = "Boss, pehle apna message likhein."
            return
        }

        if (_isLoading.value) {
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                _aiResponse.value =
                    apiManager.getAIResponse(prompt)

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                _aiResponse.value =
                    "AI se connect nahi ho paya. API settings aur internet check karein."

            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveApiKey(apiName: String, key: String) {
        if (apiName.isBlank() || key.isBlank()) {
            return
        }

        prefs.saveApiKey(apiName.trim(), key.trim())
    }

    fun deleteApiKey(apiName: String) {
        if (apiName.isBlank()) return

        prefs.deleteApiKey(apiName.trim())
    }

    fun toggleApi(apiName: String, isEnabled: Boolean) {
        if (apiName.isBlank()) return

        prefs.setApiEnabled(apiName.trim(), isEnabled)
    }

    fun getApiKey(apiName: String): String {
        return prefs.getApiKey(apiName) ?: ""
    }

    fun getApiStatus(apiName: String): Boolean {
        return prefs.isApiEnabled(apiName)
    }
}
