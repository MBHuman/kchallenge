package org.example.app.core

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow


// -----------------------------------------------------------
// Chat entity — хранит сообщения + SharedFlow для уведомлений
// -----------------------------------------------------------



class Chat(val chatID: Int, val chatName: String) {

    private val users = mutableListOf<User>()
    private val messages = mutableListOf<PackedMessage>()

    // Горячий поток сообщений чата
    private val messageFlow = MutableSharedFlow<PackedMessage>(
        replay = 20,
        extraBufferCapacity = 50,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    fun addUser(user: User) {
        users.add(user)
    }

    fun subscribe(): Flow<PackedMessage> = messageFlow

    suspend fun sendMessage(from: User, message: Message) {
        val pm = PackedMessage(from.userID, message)
        messages.add(pm)
        messageFlow.emit(pm)
    }
}