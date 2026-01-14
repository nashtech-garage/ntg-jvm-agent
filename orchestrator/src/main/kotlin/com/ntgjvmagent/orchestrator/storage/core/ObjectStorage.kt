package com.ntgjvmagent.orchestrator.storage.core

import java.io.InputStream

interface ObjectStorage {
    fun store(
        storageKey: String,
        inputStream: InputStream,
        contentType: String?,
    )

    fun load(storageKey: String): InputStream

    fun exists(storageKey: String): Boolean

    fun delete(storageKey: String)
}
