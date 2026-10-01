package com.floently.read

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Same-process handoff for canonical manifests that may be far too large for
 * Android Binder/MediaSession command Bundles.
 *
 * The MediaSession command carries only an opaque one-time token. The service
 * takes the request from this registry and the entry is removed immediately.
 */
object ReadManifestHandoffRegistry {
    private val requests =
        ConcurrentHashMap<String, ReadManifestLoadRequest>()

    fun register(
        request: ReadManifestLoadRequest
    ): String {
        val token = UUID.randomUUID().toString()
        requests[token] = request
        return token
    }

    fun take(token: String): ReadManifestLoadRequest? =
        requests.remove(token)

    fun discard(token: String) {
        requests.remove(token)
    }
}
