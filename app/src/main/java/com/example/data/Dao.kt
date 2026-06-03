package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    // ---- Messages ----
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET status = :status, hopPath = :hopPath WHERE id = :id")
    suspend fun updateMessageStatus(id: String, status: String, hopPath: String)

    @Query("UPDATE messages SET fileProgress = :progress WHERE id = :id")
    suspend fun updateFileProgress(id: String, progress: Int?)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    // ---- Devices ----
    @Query("SELECT * FROM devices ORDER BY isOnline DESC, name ASC")
    fun getAllDevicesFlow(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices")
    suspend fun getAllDevices(): List<DeviceEntity>

    @Query("SELECT * FROM devices WHERE id = :id LIMIT 1")
    suspend fun getDeviceById(id: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<DeviceEntity>)

    @Query("UPDATE devices SET isOnline = :isOnline WHERE id = :id")
    suspend fun updateDeviceOnlineStatus(id: String, isOnline: Boolean)

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun removeDevice(id: String)

    @Query("DELETE FROM devices")
    suspend fun clearAllDevices()

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}
