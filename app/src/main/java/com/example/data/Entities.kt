package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isOnline: Boolean,
    val hopCount: Int,
    val nextHop: String?,
    val communicationType: String, // "BLUETOOTH", "WI_FI", "MESH"
    val rssi: Int, // signal strength in dBm, e.g. -70
    val isSimulated: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String, // UUID
    val chatId: String, // Chat destination (peer deviceId or "mesh_broadcast")
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isOutgoing: Boolean,
    val status: String, // "PENDING", "SENT", "ROUTED", "DELIVERED"
    val hopPath: String, // Description of route, e.g., "Self -> PeerA -> PeerB"
    val filePath: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val fileProgress: Int? = null, // null means no ongoing transfer, 100 is completed
    val isWifiTransfer: Boolean = false
)
