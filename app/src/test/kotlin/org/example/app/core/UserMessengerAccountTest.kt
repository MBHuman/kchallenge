package org.example.app.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.expect

class UserMessengerAccountTest {

    @Test
    fun `basic user usecase`() = runTest {
        val user = User(userID = 1, userName = "Alice")
        val storage = Storage.Rocks("rocksdb_example_account")
        val messenger = Messenger(storage)
        val uma = UserMessengerAccount(user, messenger)
        val collected = mutableListOf<String>()
        val chat = uma.createChat("General")
        uma.addUserToChat(chat)
        uma.notificators += Notificator.TestNotificator(collected)
        val message = Message.TextMessage(1, "Hello, World!")
        uma.sendMessage(message, chat.chatID)

        delay(50)

        expect("Hello, World!", {
            collected.first()
        })

        val messageFromChat = uma.readMessage(message.getID(), chat.chatID)
        expect(message, {
            messageFromChat
        })
    }
}