package com.hisense.remote.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisense.remote.model.DiscoveredTv
import com.hisense.remote.model.TvState
import com.hisense.remote.service.MqttService
import com.hisense.remote.service.TvDiscoveryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RemoteViewModel : ViewModel() {
    private val mqtt = MqttService()

    private val _state = MutableStateFlow(TvState())
    val state: StateFlow<TvState> = _state.asStateFlow()

    private val _discoveredTvs = MutableStateFlow<List<DiscoveredTv>>(emptyList())
    val discoveredTvs: StateFlow<List<DiscoveredTv>> = _discoveredTvs.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    // Keyboard buffer
    private val _kbBuffer = MutableStateFlow("")
    val kbBuffer: StateFlow<String> = _kbBuffer.asStateFlow()

    init {
        mqtt.onConnectionChanged = { connected ->
            _state.value = _state.value.copy(connected = connected)
        }
        mqtt.onPairingCode = { code ->
            _state.value = _state.value.copy(pairingCode = code)
        }
        mqtt.onStateUpdate = { updated ->
            _state.value = _state.value.copy(
                volume = updated.volume,
                source = updated.source,
                channel = updated.channel,
            )
        }
    }

    fun connect(host: String, mac: String = "") {
        viewModelScope.launch {
            _state.value = _state.value.copy(errorMessage = "")
            val success = mqtt.connect(host, mac = mac)
            if (success) {
                _state.value = _state.value.copy(
                    connected = true, host = host, mac = mac,
                    name = "Hisense TV ($host)", errorMessage = ""
                )
            } else {
                _state.value = _state.value.copy(
                    errorMessage = "Could not connect to $host (Port 36669). Ensure your TV is on Vidaa OS and powered on.", connected = false
                )
            }
        }
    }

    fun disconnect() {
        mqtt.disconnect()
        _state.value = TvState()
        _kbBuffer.value = ""
    }

    fun wakeOnLan(mac: String, ip: String = "") {
        mqtt.sendWakeOnLan(mac, ip)
    }

    fun sendKey(key: String) = mqtt.sendKey(key)

    fun sendText(text: String, sendEnter: Boolean = true) {
        viewModelScope.launch {
            _isSending.value = true
            mqtt.sendText(text, sendEnter)
            kotlinx.coroutines.delay(500)
            _isSending.value = false
        }
    }

    fun setVolume(level: Int) = mqtt.setVolume(level)

    fun launchApp(appName: String) = mqtt.launchApp(appName)

    fun sendPairingCode(code: String) {
        mqtt.sendPairingCode(code)
        _state.value = _state.value.copy(paired = true, pairingCode = "")
    }

    fun discoverTvs() {
        viewModelScope.launch {
            _isDiscovering.value = true
            _discoveredTvs.value = emptyList()
            try {
                _discoveredTvs.value = TvDiscoveryService.discover()
            } catch (_: Exception) {}
            _isDiscovering.value = false
        }
    }

    // Keyboard buffer
    fun addCharToKb(char: String) { _kbBuffer.value += char }
    fun removeCharFromKb() {
        if (_kbBuffer.value.isNotEmpty())
            _kbBuffer.value = _kbBuffer.value.dropLast(1)
    }
    fun clearKb() { _kbBuffer.value = "" }

    fun sendKbBuffer() {
        val text = _kbBuffer.value
        if (text.isNotEmpty()) {
            _kbBuffer.value = ""
            sendText(text)
        }
    }

    override fun onCleared() {
        mqtt.destroy()
        super.onCleared()
    }
}
