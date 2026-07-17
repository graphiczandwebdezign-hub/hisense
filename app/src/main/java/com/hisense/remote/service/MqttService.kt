package com.hisense.remote.service

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.hisense.remote.model.TvState
import kotlinx.coroutines.*
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

class MqttService {
    companion object {
        private const val TAG = "MqttService"
        private const val PORT = 36669
        private const val USER = "hisenseservice"
        private const val PASS = "multimqttservice"
    }

    // Callbacks
    var onConnectionChanged: ((Boolean) -> Unit)? = null
    var onPairingCode: ((String) -> Unit)? = null
    var onStateUpdate: ((TvState) -> Unit)? = null

    var isConnected = false; private set
    var isPaired = false; private set
    var pairingCode = ""; private set

    private fun setConnectedState(value: Boolean) { isConnected = value }
    private fun setPairedState(value: Boolean) { isPaired = value }
    private fun setPairingCodeState(value: String) { pairingCode = value }

    private var mqttClient: IMqttAsyncClient? = null
    private var clientId = ""
    private var host = ""
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var textJob: Job? = null

    suspend fun connect(host: String, mac: String = ""): Boolean = withContext(Dispatchers.IO) {
        this@MqttService.host = host
        clientId = generateClientId(mac)

        // Disconnect existing client if connected
        disconnect()

        // If MAC provided, attempt Wake-on-LAN first
        if (mac.isNotEmpty()) {
            sendWakeOnLan(mac, host)
            delay(300)
        }

        // Try connecting with TLS v1.2 first, then fallback to standard TLS
        val protocols = listOf("TLSv1.2", "TLS")
        for (protocol in protocols) {
            if (tryConnect(host, clientId, protocol)) {
                return@withContext true
            }
        }
        return@withContext false
    }

    private fun tryConnect(host: String, clientId: String, protocol: String): Boolean {
        var client: MqttAsyncClient? = null
        try {
            val sslCtx = SSLContext.getInstance(protocol).apply {
                init(null, arrayOf(trustAllManager), SecureRandom())
            }
            val uri = "ssl://$host:$PORT"
            client = MqttAsyncClient(uri, clientId, MemoryPersistence())
            client.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    Log.d(TAG, "Connected to $host via $protocol")
                    setConnectedState(true)
                    subscribe()
                    sendConnectMsg()
                    onConnectionChanged?.invoke(true)
                }
                override fun connectionLost(cause: Throwable?) {
                    Log.d(TAG, "Connection lost")
                    setConnectedState(false)
                    onConnectionChanged?.invoke(false)
                }
                override fun messageArrived(topic: String, msg: MqttMessage) {
                    handleMsg(topic, String(msg.payload))
                }
                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            val opts = MqttConnectOptions().apply {
                userName = USER
                password = PASS.toCharArray()
                connectionTimeout = 10
                keepAliveInterval = 30
                isCleanSession = true
                mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1
                socketFactory = sslCtx.socketFactory
            }

            val token = client.connect(opts)
            token.waitForCompletion(10000)
            if (client.isConnected) {
                mqttClient = client
                return true
            } else {
                try { client.close() } catch (_: Exception) {}
                return false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Connect attempt failed ($protocol): ${e.message}")
            try { client?.close() } catch (_: Exception) {}
            return false
        }
    }

    private fun subscribe() {
        val mob = "/remoteapp/mobile/$clientId/"
        val brd = "/remoteapp/mobile/broadcast/"
        try {
            mqttClient?.subscribe("${brd}ui_service/state", 0)
            mqttClient?.subscribe("${mob}ui_service/data/#", 0)
            mqttClient?.subscribe("${mob}platform_service/data/#", 0)
            mqttClient?.subscribe("/remoteapp/mobile/broadcast/platform_service/actions/authenticationcode", 0)
        } catch (_: Exception) {}
    }

    private fun sendConnectMsg() {
        val base = "/remoteapp/tv/ui_service/$clientId/"
        val json = gson.toJson(mapOf(
            "app_version" to 2, "connect_result" to 0, "device_type" to "Mobile App"
        ))
        try {
            mqttClient?.publish("${base}actions/vidaa_app_connect", MqttMessage(json.toByteArray()))
        } catch (_: Exception) {}
    }

    private fun handleMsg(topic: String, payload: String) {
        try {
            val json = gson.fromJson(payload, JsonObject::class.java) ?: return
            if (topic.contains("authenticationcode") && json.has("authNum")) {
                setPairingCodeState(json["authNum"].asString)
                onPairingCode?.invoke(pairingCode)
            }
            if (topic.contains("tokenissuance") && (json.has("access_token") || json.has("refreshtoken"))) {
                setPairedState(true)
            }
            if (topic.contains("ui_service/state") || topic.contains("state")) {
                onStateUpdate?.invoke(TvState(
                    connected = isConnected, paired = isPaired,
                    host = host, name = "Hisense TV ($host)",
                    volume = json["volume"]?.asInt ?: 0,
                    source = json["sourcename"]?.asString ?: json["sourceid"]?.asString ?: "",
                    channel = json["channel_num"]?.asString ?: "",
                ))
            }
        } catch (_: Exception) {}
    }

    fun sendKey(key: String) {
        val code = com.hisense.remote.model.KeyCodes.get(key)
        val jsonPayload = gson.toJson(mapOf("key" to code, "action" to "Click"))
        publish("actions/remotekey", jsonPayload)
        publishToRemoteService("actions/sendkey", code)
    }

    fun sendText(text: String, sendEnter: Boolean = true) {
        textJob?.cancel()
        textJob = scope.launch {
            for (ch in text) {
                val code = com.hisense.remote.model.CharToKey.get(ch.toString()) ?: continue
                sendKeyDirect(code)
                delay(80)
            }
            if (sendEnter) { delay(120); sendKey("enter") }
        }
    }

    private fun sendKeyDirect(keyCode: String) {
        val jsonPayload = gson.toJson(mapOf("key" to keyCode, "action" to "Click"))
        publish("actions/remotekey", jsonPayload)
    }

    fun setVolume(level: Int) {
        publish("actions/setvolume", gson.toJson(mapOf("volume" to level.coerceIn(0, 100))))
    }

    fun launchApp(name: String) {
        publish("actions/launchapp", gson.toJson(mapOf("app" to name)))
    }

    fun sendPairingCode(code: String) {
        val authNum = code.toIntOrNull() ?: 0
        publish("actions/authenticationcode", gson.toJson(mapOf("authNum" to authNum)))
        setPairedState(true)
    }

    private fun publish(action: String, json: String) {
        try {
            val topic = "/remoteapp/tv/ui_service/$clientId/$action"
            mqttClient?.publish(topic, MqttMessage(json.toByteArray()))
        } catch (_: Exception) {}
    }

    private fun publishToRemoteService(action: String, message: String) {
        try {
            val topic = "/remoteapp/tv/remote_service/$clientId/$action"
            mqttClient?.publish(topic, MqttMessage(message.toByteArray()))
        } catch (_: Exception) {}
    }

    fun sendWakeOnLan(macStr: String, ipStr: String = "255.255.255.255") {
        scope.launch(Dispatchers.IO) {
            try {
                val cleanMac = macStr.replace(":", "").replace("-", "")
                if (cleanMac.length != 12) return@launch
                val bytes = ByteArray(6 + 16 * 6)
                for (i in 0..5) bytes[i] = 0xFF.toByte()
                for (i in 6 until bytes.size step 6) {
                    for (j in 0..5) {
                        bytes[i + j] = cleanMac.substring(j * 2, j * 2 + 2).toInt(16).toByte()
                    }
                }
                val address = InetAddress.getByName(if (ipStr.isNotBlank()) ipStr else "255.255.255.255")
                val socket = DatagramSocket().apply { broadcast = true }
                val packet9 = DatagramPacket(bytes, bytes.size, address, 9)
                val packet7 = DatagramPacket(bytes, bytes.size, address, 7)
                socket.send(packet9)
                socket.send(packet7)
                socket.close()
                Log.d(TAG, "Sent WoL magic packet to $macStr")
            } catch (e: Exception) {
                Log.e(TAG, "WoL error: ${e.message}")
            }
        }
    }

    fun disconnect() {
        try { mqttClient?.disconnect() } catch (_: Exception) {}
        try { mqttClient?.close() } catch (_: Exception) {}
        mqttClient = null
        setConnectedState(false)
        setPairingCodeState("")
        onConnectionChanged?.invoke(false)
    }

    fun destroy() { textJob?.cancel(); scope.cancel(); disconnect() }

    private val trustAllManager = object : X509TrustManager {
        override fun checkClientTrusted(c: Array<X509Certificate>, a: String?) {}
        override fun checkServerTrusted(c: Array<X509Certificate>, a: String?) {}
        override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
    }

    private fun generateClientId(mac: String): String {
        val m = mac.replace(":", "").replace("-", "").uppercase()
        val r = System.currentTimeMillis()
        val dollar = "${'$'}"
        return if (m.length < 12) "${m.padEnd(12,'0')}${dollar}his${dollar}${r}_vidaacommon_001"
        else "${m}${dollar}his${dollar}${m.hashCode()}_vidaacommon_001"
    }
}
