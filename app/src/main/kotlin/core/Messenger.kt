package org.example.app.core

// -----------------------------------------------------------
// Messenger — глобальный менеджер чатов и пользователей
// -----------------------------------------------------------

class Messenger(
    private val chats: MutableMap<Int, Chat> = mutableMapOf(),
    private val usersChats: MutableMap<Int, MutableList<Int>> = mutableMapOf()
) {

    private var nextChatId = 1

    fun createChat(chatName: String): Chat {
        val chatID = nextChatId++
        val chat = Chat(chatID, chatName)
        chats[chatID] = chat
        return chat
    }

    fun addUserToChat(user: User, chatID: Int) {
        usersChats.getOrPut(user.userID) { mutableListOf() }.add(chatID)
        chats[chatID]?.addUser(user)
    }

    suspend fun sendMessage(user: User, message: Message, chatID: Int) {
        chats[chatID]?.sendMessage(user, message)
    }

    fun getChats(user: User): List<Chat> {
        val ids = usersChats[user.userID] ?: return emptyList()
        return ids.mapNotNull { chats[it] }
    }
}


