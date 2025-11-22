package org.example.app.core

import org.rocksdb.Options
import org.rocksdb.ReadOptions
import org.rocksdb.RocksDB
import org.rocksdb.Slice
import org.rocksdb.WriteOptions


sealed class Storage {

    abstract fun writeMessage(message: Message, chatID: Int)
    abstract fun writeBatchMessages(messages: List<Message>, chatID: Int)
    abstract fun readMessage(messageID: Int, chatID: Int): Message?

    // if lastID == -1 then return the latest messages
    // if limit == 0 then set deafult limit 50
    abstract fun readMessages(chatID: Int, limit: Int, lastID: Int = 0): List<Message>
    abstract fun close()

    companion object {
        fun encodeKeyToLong(chatID: Int, msgID: Long): Long {
            // упаковываем chatID в старшие 32 бита, msgID в младшие 32 бита
            return (chatID.toLong() shl 32) or (msgID and 0xFFFFFFFFL)
        }

        fun encodeKey(chatID: Int, msgID: Long): ByteArray {
            val buf = ByteArray(8) // 8 байт для упакованного Long
            val bb = java.nio.ByteBuffer.wrap(buf)
            bb.putLong(encodeKeyToLong(chatID, msgID))
            return buf
        }

        fun decodeKey(bytes: ByteArray): Pair<Int, Long> {
            val bb = java.nio.ByteBuffer.wrap(bytes)
            val packed = bb.long
            val chatID = (packed ushr 32).toInt()
            val msgID = packed and 0xFFFFFFFFL
            return chatID to msgID
        }

        fun decodeKeyFromLong(packed: Long): Pair<Int, Long> {
            val chatID = (packed ushr 32).toInt()
            val msgID = packed and 0xFFFFFFFFL
            return chatID to msgID
        }

    }


    class Rocks(val dbPath: String) : Storage() {

        private val options = Options().setCreateIfMissing(true)
        private val writeOpts = WriteOptions().setDisableWAL(false)

        private val db: RocksDB

        init {
            RocksDB.loadLibrary()
            db = RocksDB.open(options, dbPath)
        }


        override fun writeMessage(message: Message, chatID: Int) {
            val msgID = message.getID().toLong()
            val key = encodeKey(chatID, msgID)
            val value = when (message) {
                is Message.TextMessage -> message.content.toByteArray()
            }
            db.put(writeOpts, key, value)
        }

        override fun writeBatchMessages(messages: List<Message>, chatID: Int) {
            val batch = org.rocksdb.WriteBatch()
            try {
                for (message in messages) {
                    val msgID = message.getID().toLong()
                    val key = encodeKey(chatID, msgID)
                    val value = when (message) {
                        is Message.TextMessage -> message.content.toByteArray()
                    }
                    batch.put(key, value)
                }
                db.write(writeOpts, batch)
            } finally {
                batch.close()
            }
        }

        override fun readMessage(messageID: Int, chatID: Int): Message? {
            val key = encodeKey(chatID, messageID.toLong())
            val bytes = db.get(key) ?: return null
            val content = String(bytes)
            return Message.TextMessage(messageID, content)
        }

        override fun readMessages(chatID: Int, limit: Int, lastID: Int): List<Message> {
            val result = mutableListOf<Message>()

            // Верхняя граница — ключ следующего чата
            val upper = Slice(encodeKey(chatID + 1, 0L))
            val readOpts = ReadOptions().setIterateUpperBound(upper)

            val iter = db.newIterator(readOpts)

            try {
                val actualLimit = if (limit == 0) 50 else limit

                val startKey = if (lastID == -1)
                    encodeKey(chatID, Long.MAX_VALUE)
                else
                    encodeKey(chatID, lastID.toLong())

                iter.seek(startKey)
                if (!iter.isValid) iter.seekToLast()

                while (iter.isValid && result.size < actualLimit) {
                    val (cid, msgID) = decodeKey(iter.key())

                    if (cid != chatID) break  // формально уже не нужно, но оставим

                    val content = String(iter.value())
                    result.add(Message.TextMessage(msgID.toInt(), content))

                    iter.prev()
                }

            } finally {
                iter.close()
                readOpts.close()
                upper.close()  // ← обязательно закрыть Slice
            }

            return result
        }


        override fun close() {
            writeOpts.close()
            db.close()
            options.close()
        }
    }
}