package io.asset.store

import asset.store.ObjectStore

class InMemoryObjectStoreTest : ObjectStoreTest() {
    override fun createObjectStore(): ObjectStore {
        return InMemoryObjectStore()
    }
}
