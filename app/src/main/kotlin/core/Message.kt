package org.example.app.core

sealed class Message {
    data class TextMessage(val id: Int, val content: String) : Message()
}
