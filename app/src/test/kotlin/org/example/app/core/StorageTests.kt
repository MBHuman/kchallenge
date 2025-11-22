package org.example.app.core

import org.rocksdb.Options
import org.rocksdb.RocksDB
import kotlin.test.Test
import kotlin.test.expect

class StorageTests {

    @Test
    fun `basic rocksdb storage test`() {
        RocksDB.loadLibrary()

        val path = "rocksdb_test_storage"
        val options = Options().setCreateIfMissing(true)

        options.use { opts ->
            RocksDB.open(opts, path).use { db ->
                val key = "my_key".toByteArray()
                val value = "my_value".toByteArray()

                // Put key-value pair
                db.put(key, value)

                // Get value by key
                val retrievedValue = db.get(key)

                // Verify the value
                assert(retrievedValue != null)
                assert(String(retrievedValue!!) == "my_value")

                // Delete the key
                db.delete(key)

                // Verify deletion
                val deletedValue = db.get(key)
                expect(null, {
                    deletedValue
                })
            }
        }
    }

    @Test
    fun `rocksdb read and write message test`() {
        val rocksStorage = Storage.Rocks("rocksdb_example_read_write")
        val chatID = 8
        val messages = listOf(
            Message.TextMessage(1, "Hello"),
            Message.TextMessage(2, "World"),
            Message.TextMessage(3, "Test Message")
        )

        // Simple
        rocksStorage.writeBatchMessages(messages, chatID)

        val messagesFromDB = rocksStorage.readMessages(chatID, 10, -1)
        expect(3, {
            messagesFromDB.size
        })
        expect("Test Message", {
            (messagesFromDB[0] as Message.TextMessage).content
        })
        expect("World", {
            (messagesFromDB[1] as Message.TextMessage).content
        })
        expect("Hello", {
            (messagesFromDB[2] as Message.TextMessage).content
        })
    }

    @Test
    fun `rocksdb encodeKey, decodeKey`() {
        val chatID = 1
        val msgID = 42
        val encoded = Storage.encodeKey(chatID, msgID.toLong())
        val (decodedChatID, decodedMsgID) = Storage.decodeKey(encoded)

        expect(chatID, {
            decodedChatID
        })
        expect(msgID, {
            decodedMsgID.toInt()
        })
    }
}