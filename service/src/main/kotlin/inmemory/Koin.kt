package io.inmemory

import asset.store.ObjectStore
import io.asset.store.InMemoryObjectStore
import org.koin.core.module.Module
import org.koin.dsl.module

fun inMemoryObjectStoreModule(): Module =
    module {
        single<ObjectStore> {
            InMemoryObjectStore()
        }
    }
