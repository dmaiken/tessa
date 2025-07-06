package io.inmemory

import asset.store.InMemoryObjectStore
import asset.store.ObjectStore
import org.koin.core.module.Module
import org.koin.dsl.module

fun inMemoryObjectStoreModule(): Module =
    module {
        single<ObjectStore> {
            InMemoryObjectStore()
        }
    }
