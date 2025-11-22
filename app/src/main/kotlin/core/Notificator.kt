package org.example.app.core

// Notificator abstraction
// -----------------------------------------------------------

sealed class Notificator {
    abstract fun notify(message: Message)

    class ConsoleNotificator : Notificator() {
        override fun notify(message: Message) {
            when (message) {
                is Message.TextMessage ->
                    println("New message: ${message.content}")
            }
        }
    }

    class TestNotificator(val collected: MutableList<String>) : Notificator() {
        override fun notify(message: Message) {
            when (message) {
                is Message.TextMessage ->
                    collected += message.content
            }
        }
    }
}