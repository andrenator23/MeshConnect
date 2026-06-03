package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ChatRepository
import com.example.data.DeviceEntity
import com.example.data.MessageEntity
import com.example.p2p.MeshNetworkEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ChatRepository(database.chatDao())
    val meshEngine = MeshNetworkEngine(application, repository)

    // UI state
    val devices: StateFlow<List<DeviceEntity>> = repository.allDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeChatId = MutableStateFlow("mesh_broadcast")
    val activeChatId: StateFlow<String> = _activeChatId

    // Dynamically retrieve messages for the active conversation
    val activeMessages: StateFlow<List<MessageEntity>> = _activeChatId
        .flatMapLatest { chatId ->
            repository.getMessagesForChat(chatId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val networkLogs = meshEngine.networkLogs
    val isScanning = meshEngine.isScanning
    val secureChannelEnabled = meshEngine.secureChannelEnabled
    val wifiDirectActive = meshEngine.wifiDirectActive

    // Self credentials
    private val _selfName = MutableStateFlow("Self Device")
    val selfName: StateFlow<String> = _selfName

    init {
        // Start offline scan initially to make app feel alive
        meshEngine.startP2PDiscovery()
    }

    fun selectChat(chatId: String) {
        _activeChatId.value = chatId
        meshEngine.addLog("Switched conversation view to '$chatId'", MeshNetworkEngine.LogType.INFO)
    }

    fun sendMessage(text: String, fileUri: String? = null, fileName: String? = null, fileSize: Long? = null) {
        if (text.isBlank() && fileUri == null) return
        meshEngine.sendMessageInMesh(_activeChatId.value, text, fileUri, fileName, fileSize)
    }

    fun toggleSecureHandshake() {
        meshEngine.toggleSecureChannel()
    }

    fun startDiscovery() {
        meshEngine.startP2PDiscovery()
    }

    fun addSimulatedNode(name: String, connectionType: String, signalStrength: String) {
        meshEngine.addCustomPeer(name, connectionType, signalStrength)
    }

    fun toggleNodeOnlineState(id: String, currentOnline: Boolean) {
        viewModelScope.launch {
            repository.updateDeviceOnlineStatus(id, !currentOnline)
            val updatedState = if (!currentOnline) "ONLINE" else "OFFLINE"
            meshEngine.addLog("Node '$id' status forced to $updatedState.", MeshNetworkEngine.LogType.ROUTING)
            meshEngine.addLog("Re-evaluating dynamic mesh graph vectors for auto-healing.", MeshNetworkEngine.LogType.ROUTING)
        }
    }

    fun deleteNode(id: String) {
        meshEngine.removePeer(id)
        if (_activeChatId.value == id) {
            _activeChatId.value = "mesh_broadcast"
        }
    }

    fun updateSelfName(newName: String) {
        if (newName.isNotBlank()) {
            _selfName.value = newName
            meshEngine.addLog("Self broadcast identity updated: '$newName'", MeshNetworkEngine.LogType.INFO)
        }
    }

    fun injectIncomingPeerSimulation(chatId: String, text: String, hasFile: Boolean = false) {
        val selectedPeerId = if (chatId == "mesh_broadcast") {
            // Pick an online peer
            devices.value.firstOrNull { it.isOnline }?.id ?: "phone_b"
        } else {
            chatId
        }

        viewModelScope.launch {
            if (hasFile) {
                meshEngine.receiveMockMessage(
                    senderId = selectedPeerId,
                    text = "Sending photo of project blueprint over Wi-Fi direct.",
                    filePath = "mock://image_blueprint.png",
                    fileName = "blueprint.png",
                    fileSize = 4800000L
                )
            } else {
                meshEngine.receiveMockMessage(
                    senderId = selectedPeerId,
                    text = text
                )
            }
        }
    }

    fun wipeLocalStorage() {
        viewModelScope.launch {
            repository.clearAllData()
            meshEngine.addLog("Database schema hard purges. Sinks and topology re-initialized.", MeshNetworkEngine.LogType.INFO)
            _activeChatId.value = "mesh_broadcast"
        }
    }
}

class ChatViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
