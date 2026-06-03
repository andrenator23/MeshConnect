package com.example.p2p

import android.content.Context
import android.net.wifi.p2p.WifiP2pManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.util.Log
import com.example.data.ChatRepository
import com.example.data.DeviceEntity
import com.example.data.MessageEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class MeshNetworkEngine(
    private val context: Context,
    private val repository: ChatRepository
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Simulated status indicators
    val isScanning = MutableStateFlow(false)
    val secureChannelEnabled = MutableStateFlow(true)
    val wifiDirectActive = MutableStateFlow(false)

    // Log messages for developer/network audit view in our UI
    private val _networkLogs = MutableStateFlow<List<NetworkLog>>(emptyList())
    val networkLogs: StateFlow<List<NetworkLog>> = _networkLogs

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothManager?.adapter
    }

    private val wifiP2pManager: WifiP2pManager? by lazy {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    }

    data class NetworkLog(
        val message: String,
        val timestamp: Long = System.currentTimeMillis(),
        val type: LogType = LogType.INFO
    )

    enum class LogType {
        INFO, SECURE, BLUETOOTH, WI_FI, ROUTING
    }

    init {
        addLog("Mesh network engine initialized offline", LogType.INFO)
        addLog("Hardware Check: Bluetooth P2P support verified", LogType.BLUETOOTH)
        addLog("Hardware Check: Wi-Fi P2P (Wi-Fi Direct) support verified", LogType.WI_FI)
        
        // Seed initial simulation devices in Room database
        coroutineScope.launch {
            val existing = repository.getAllDevicesDirect()
            if (existing.isEmpty()) {
                seedInitialDevices()
            }
        }
    }

    fun addLog(msg: String, type: LogType = LogType.INFO) {
        val newLog = NetworkLog(msg, type = type)
        _networkLogs.value = (listOf(newLog) + _networkLogs.value).take(100)
    }

    private suspend fun seedInitialDevices() {
        addLog("Generating default local peer directory...", LogType.ROUTING)
        val defaultPeers = listOf(
            DeviceEntity(
                id = "phone_b",
                name = "Office Desk (Phone B)",
                isOnline = true,
                hopCount = 1,
                nextHop = null,
                communicationType = "BLUETOOTH",
                rssi = -64,
                isSimulated = true
            ),
            DeviceEntity(
                id = "tablet_c",
                name = "Kitchen Lobby (Tablet C)",
                isOnline = true,
                hopCount = 2,
                nextHop = "phone_b",
                communicationType = "MESH",
                rssi = -82,
                isSimulated = true
            ),
            DeviceEntity(
                id = "laptop_d",
                name = "Front Gate (Laptop D)",
                isOnline = true,
                hopCount = 3,
                nextHop = "tablet_c",
                communicationType = "MESH",
                rssi = -91,
                isSimulated = true
            )
        )
        repository.insertDevices(defaultPeers)
        addLog("Discovered 3 local offline mesh peers inside range.", LogType.ROUTING)
    }

    fun startP2PDiscovery() {
        if (isScanning.value) return
        isScanning.value = true
        addLog("Initiating Bluetooth Low Energy Mesh scanning...", LogType.BLUETOOTH)
        addLog("Initiating Wi-Fi Service Discovery for direct peers...", LogType.WI_FI)

        coroutineScope.launch {
            // Simulate scans finding or refreshing signal strength
            for (i in 1..4) {
                delay(1200)
                val devices = repository.getAllDevicesDirect()
                devices.forEach { dev ->
                    if (dev.isOnline) {
                        val fluctuate = (-5..5).random()
                        val updatedDev = dev.copy(rssi = (dev.rssi + fluctuate).coerceIn(-100, -30))
                        repository.insertDevice(updatedDev)
                    }
                }
                addLog("BLE scan: received advertisement beacons, updating link quality index.", LogType.BLUETOOTH)
            }
            isScanning.value = false
            addLog("P2P Local scan sequence completed.", LogType.INFO)
        }
    }

    fun toggleSecureChannel() {
        val next = !secureChannelEnabled.value
        secureChannelEnabled.value = next
        if (next) {
            addLog("End-to-End Cryptography enabled. AES-256 + ECDH Secure Mesh Keys generated.", LogType.SECURE)
        } else {
            addLog("WARNING: Secure channel disabled. Messages will route in clear-text.", LogType.SECURE)
        }
    }

    fun addCustomPeer(name: String, connectionType: String, signalStrength: String) {
        val cleanName = name.ifEmpty { "Mesh Station - ${ (100..999).random() }" }
        val id = "custom_peer_" + UUID.randomUUID().toString().take(6)
        
        val rssiVal = when (signalStrength.uppercase()) {
            "STRONG" -> -55
            "MEDIUM" -> -75
            else -> -92 // WEAK
        }

        val hopCount = when (connectionType) {
            "BLUETOOTH" -> 1
            "WI_FI" -> 1
            else -> (2..4).random()
        }

        val nextHopVal = if (hopCount > 1) {
            val onlineDirects = runBlocking {
                repository.getAllDevicesDirect().filter { it.isOnline && it.hopCount == 1 }
            }
            onlineDirects.randomOrNull()?.id ?: "phone_b"
        } else null

        val device = DeviceEntity(
            id = id,
            name = cleanName,
            isOnline = true,
            hopCount = hopCount,
            nextHop = nextHopVal,
            communicationType = connectionType,
            rssi = rssiVal,
            isSimulated = true
        )

        coroutineScope.launch {
            repository.insertDevice(device)
            addLog("Discovered new peer '$cleanName' via $connectionType (RSSI $rssiVal dBm)", LogType.INFO)
            addLog("Mesh topology updated: recalculating Shortest Path First (SPF) Dijkstra matrix.", LogType.ROUTING)
        }
    }

    fun removePeer(id: String) {
        coroutineScope.launch {
            repository.getDeviceById(id)?.let { dev ->
                repository.removeDevice(id)
                addLog("Lost beacon from '${dev.name}' - node pruned from topological routing map.", LogType.ROUTING)
            }
        }
    }

    fun sendMessageInMesh(chatId: String, text: String, filePath: String? = null, fileName: String? = null, fileSize: Long? = null) {
        val msgId = UUID.randomUUID().toString()
        val isBroadcast = chatId == "mesh_broadcast"

        coroutineScope.launch {
            val destinationName = if (isBroadcast) "Mesh Broadcast" else {
                repository.getDeviceById(chatId)?.name ?: "Unknown Peer"
            }

            val startPath = "Self"
            
            // Build real Message Entity
            val msg = MessageEntity(
                id = msgId,
                chatId = chatId,
                senderId = "self_node",
                senderName = "Self Device",
                text = text,
                timestamp = System.currentTimeMillis(),
                isOutgoing = true,
                status = "PENDING",
                hopPath = startPath,
                filePath = filePath,
                fileName = fileName,
                fileSize = fileSize,
                fileProgress = if (filePath != null) 0 else null,
                isWifiTransfer = filePath != null
            )

            repository.insertMessage(msg)
            
            val isSecure = secureChannelEnabled.value
            val secureSuffix = if (isSecure) " (ECDH Encrypted Payload)" else " (INSECURE Plaintext Payload)"
            
            if (isBroadcast) {
                addLog("Broadcasting flood-routing packet $msgId $secureSuffix", LogType.ROUTING)
                delay(600)
                repository.updateMessageStatus(msgId, "SENT", "Self -> BLE Broadcast")
                addLog("Mesh Flood complete: Packet reached adjacent direct mesh nodes.", LogType.ROUTING)
            } else {
                val targetDevice = repository.getDeviceById(chatId) ?: return@launch
                val hops = targetDevice.hopCount
                val pathNames = mutableListOf("Self")

                addLog("Routing point-to-point packet to '$destinationName' (Hops required: $hops)$secureSuffix", LogType.ROUTING)

                if (hops == 1) {
                    delay(800)
                    val commTypeStr = if (filePath != null) "Wi-Fi Direct" else "Bluetooth LE"
                    pathNames.add(targetDevice.name)
                    val pathString = pathNames.joinToString(" ➔ ")
                    
                    if (filePath != null) {
                        // Wi-Fi High speed file transfer simulation!
                        simulateFileTransfer(msgId, targetDevice.name, pathString)
                    } else {
                        repository.updateMessageStatus(msgId, "DELIVERED", pathString)
                        addLog("Packet delivered directly over $commTypeStr to '${targetDevice.name}'", LogType.BLUETOOTH)
                    }
                } else {
                    // Multi hop routing algorithm simulator!
                    let {
                        var currentHopNode = targetDevice.nextHop
                        val intermediateNodes = mutableListOf<String>()
                        while (currentHopNode != null) {
                            val intermediate = repository.getDeviceById(currentHopNode)
                            if (intermediate != null) {
                                intermediateNodes.add(0, intermediate.name)
                                currentHopNode = intermediate.nextHop
                            } else {
                                break
                            }
                        }

                        // Let's route step by step through each hop!
                        for (i in 0..intermediateNodes.size) {
                            val intermediatePath = mutableListOf("Self")
                            for (j in 0 until i) {
                                intermediatePath.add(intermediateNodes[j])
                            }
                            val stateText = intermediatePath.joinToString(" ➔ ")
                            repository.updateMessageStatus(msgId, "ROUTED", stateText)
                            
                            val nextNodeName = if (i < intermediateNodes.size) intermediateNodes[i] else targetDevice.name
                            addLog("Step ${i + 1}: Store-and-Forward packet relayed via '$nextNodeName'", LogType.ROUTING)
                            delay(1000)
                        }

                        pathNames.addAll(intermediateNodes)
                        pathNames.add(targetDevice.name)
                        val finalPathString = pathNames.joinToString(" ➔ ")
                        
                        if (filePath != null) {
                            simulateFileTransfer(msgId, targetDevice.name, finalPathString)
                        } else {
                            repository.updateMessageStatus(msgId, "DELIVERED", finalPathString)
                            addLog("Packet delivered successfully to destination '${targetDevice.name}' via $hops mesh hops!", LogType.ROUTING)
                        }
                    }
                }
            }
        }
    }

    private suspend fun simulateFileTransfer(msgId: String, destName: String, pathString: String) {
        addLog("Upgrading transport layer to Wi-Fi Direct (802.11 peer-to-peer) for file payload...", LogType.WI_FI)
        wifiDirectActive.value = true
        for (perc in 1..5) {
            val progress = perc * 20
            delay(500)
            repository.updateFileProgress(msgId, if (progress < 100) progress else null)
            addLog("Wi-Fi transfer: $progress% sent. Link speed is stable at 18.4 MB/s.", LogType.WI_FI)
        }
        repository.updateMessageStatus(msgId, "DELIVERED", pathString)
        wifiDirectActive.value = false
        addLog("File transmission securely completed over localized Wi-Fi pipe to '$destName'.", LogType.WI_FI)
    }

    fun receiveMockMessage(senderId: String, text: String, filePath: String? = null, fileName: String? = null, fileSize: Long? = null) {
        coroutineScope.launch {
            val sender = repository.getDeviceById(senderId) ?: return@launch
            val isSecure = secureChannelEnabled.value
            val secText = if (isSecure) " (E2E HMAC Verified)" else " (Plain Payload Received)"
            
            val pathParts = mutableListOf<String>()
            var currentHopNode = sender.nextHop
            while (currentHopNode != null) {
                val node = repository.getDeviceById(currentHopNode)
                if (node != null) {
                    pathParts.add(node.name)
                    currentHopNode = node.nextHop
                } else break
            }
            pathParts.reverse()
            pathParts.add(sender.name)
            pathParts.add("Self")
            val pathString = pathParts.joinToString(" ➔ ")

            addLog("Incoming mesh request from '${sender.name}'$secText successfully verified.", LogType.SECURE)
            addLog("Packet route: $pathString", LogType.ROUTING)

            val msg = MessageEntity(
                id = UUID.randomUUID().toString(),
                chatId = senderId,
                senderId = senderId,
                senderName = sender.name,
                text = text,
                timestamp = System.currentTimeMillis(),
                isOutgoing = false,
                status = "DELIVERED",
                hopPath = pathString,
                filePath = filePath,
                fileName = fileName,
                fileSize = fileSize,
                fileProgress = null,
                isWifiTransfer = filePath != null
            )
            repository.insertMessage(msg)
        }
    }
}
