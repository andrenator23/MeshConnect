package com.example.data

import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {

    val allDevices: Flow<List<DeviceEntity>> = chatDao.getAllDevicesFlow()
    val allMessages: Flow<List<MessageEntity>> = chatDao.getAllMessages()

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> = 
        chatDao.getMessagesForChat(chatId)

    suspend fun getDeviceById(id: String): DeviceEntity? = 
        chatDao.getDeviceById(id)

    suspend fun getAllDevicesDirect(): List<DeviceEntity> =
        chatDao.getAllDevices()

    suspend fun insertMessage(message: MessageEntity) = 
        chatDao.insertMessage(message)

    suspend fun updateMessageStatus(id: String, status: String, hopPath: String) =
        chatDao.updateMessageStatus(id, status, hopPath)

    suspend fun updateFileProgress(id: String, progress: Int?) =
        chatDao.updateFileProgress(id, progress)

    suspend fun deleteMessage(id: String) =
        chatDao.deleteMessage(id)

    suspend fun insertDevice(device: DeviceEntity) =
        chatDao.insertDevice(device)

    suspend fun insertDevices(devices: List<DeviceEntity>) =
        chatDao.insertDevices(devices)

    suspend fun updateDeviceOnlineStatus(id: String, isOnline: Boolean) =
        chatDao.updateDeviceOnlineStatus(id, isOnline)

    suspend fun removeDevice(id: String) =
        chatDao.removeDevice(id)

    suspend fun clearAllData() {
        chatDao.clearAllDevices()
        chatDao.clearAllMessages()
    }
}
