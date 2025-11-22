package org.example.app.core

sealed class Message {
    abstract fun getID(): Int
    data class TextMessage(val id: Int, val content: String) : Message() {
        override fun getID(): Int = id
    }
}
