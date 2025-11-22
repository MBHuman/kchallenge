package org.example.app.core

import org.rocksdb.Options
import org.rocksdb.RocksDB
import kotlin.test.Test
import kotlin.test.expect

class StorageTests {

    @Test
    fun `basic rocksdb storage test`() {
        RocksDB.loadLibrary()

        val path = "rocksdb_example"
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
}