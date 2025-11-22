package org.example.app.core

import org.rocksdb.Options
import org.rocksdb.RocksDB
import org.rocksdb.WriteOptions


sealed class Storage {

    abstract fun writeMessage(message: Message, chatID: Int)
    abstract fun readMessage(messageID: Int, chatID: Int): Message?
    abstract fun close()

    class Rocks(val dbPath: String) : Storage() {

        private val options = Options().setCreateIfMissing(true)
        private val writeOpts = WriteOptions().setDisableWAL(false)

        private val db: RocksDB

        init {
            RocksDB.loadLibrary()
            db = RocksDB.open(options, dbPath)
        }

        override fun writeMessage(message: Message, chatID: Int) {
            val msgID = message.getID()
            val timestamp = System.currentTimeMillis()

            val key = "chat$chatID$msgID$timestamp".toByteArray()
            val value = when (message) {
                is Message.TextMessage -> message.content.toByteArray()
            }

            db.put(writeOpts, key, value)
        }

        override fun readMessage(messageID: Int, chatID: Int): Message? {
            val iterator = db.newIterator()
            iterator.seekToFirst()

            while (iterator.isValid) {
                val key = iterator.key()
                val keyStr = String(key)

                if (keyStr.startsWith("chat$chatID") && keyStr.contains(messageID.toString())) {
                    val value = iterator.value()
                    val content = String(value)
                    iterator.close()
                    return Message.TextMessage(messageID, content)
                }
                iterator.next()
            }
            iterator.close()
            return null
        }

        override fun close()  {
            writeOpts.close()
            db.close()
            options.close()
        }
    }
}