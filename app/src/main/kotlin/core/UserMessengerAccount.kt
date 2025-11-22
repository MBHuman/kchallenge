package org.example.app.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlin.collections.mutableListOf

// -----------------------------------------------------------
// UserMessengerAccount — подписка на чаты в единой корутине
// -----------------------------------------------------------

class UserMessengerAccount(
    val user: User,
    private val messenger: Messenger,
) {
    val innerScope: CoroutineScope = CoroutineScope(Job())

    val notificators = mutableListOf<Notificator>()
    private val chats: MutableList<Chat> = mutableListOf()
    private val privateFlow: MutableSharedFlow<Chat> = MutableSharedFlow(1)

    init {
        innerScope.launch {
            privateFlow.collect {
                chats.add(it)
                waitMessage(it.subscribe())
            }
        }
    }

    fun createChat(chatName: String): Chat =
        messenger.createChat(chatName)

    fun addUserToChat(chat: Chat) {
        messenger.addUserToChat(user, chat.chatID)
        privateFlow.tryEmit(chat)
    }

    suspend fun sendMessage(message: Message, chatID: Int) {
        messenger.sendMessage(user, message, chatID)
    }

    fun readMessage(messageID: Int, chatID: Int): Message? =
        messenger.readMessage(messageID, chatID)


    fun waitMessage(chatFlow: Flow<PackedMessage>) {
        innerScope.launch {
            chatFlow.collect { pkdMsg ->
                notificators.forEach { it.notify(pkdMsg.message) }
            }
        }
    }

    fun close() {
        innerScope.cancel()
    }
}
