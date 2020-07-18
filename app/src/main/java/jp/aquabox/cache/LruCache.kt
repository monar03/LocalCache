package jp.aquabox.cache

import android.util.LruCache
import io.reactivex.Single
import java.io.Serializable
import java.util.concurrent.TimeUnit

class LruCache : Cache {
    private val cache: LruCache<String, CacheValue> = android.util.LruCache(1000000)

    override fun <T : Serializable> get(key: String): Single<T> {
        return Single.create<T> { emitter ->
            try {
                val v = cache.get(key)
                when {
                    v.timestamp >= System.currentTimeMillis() -> emitter.onSuccess(v.value as T)
                    v.timestamp < 0 -> emitter.onSuccess(v.value as T)
                    else -> emitter.onError(IllegalStateException("cache expired"))
                }
            } catch (e: Exception) {
                emitter.onError(e)
            }

        }
    }

    override fun <T : Serializable> set(key: String, value: T, interval: Long, timeUnit: TimeUnit): Boolean {
        try {
            cache.put(key, CacheValue(
                    value,
                    if (interval >= 0) {
                        System.currentTimeMillis() + timeUnit.convert(interval, TimeUnit.MILLISECONDS)
                    } else {
                        -1
                    })
            )
        } catch (e: Exception) {
            return false
        }

        return true
    }

    override fun delete(key: String) {
        cache.remove(key)
    }

    class CacheValue(val value: Serializable, val timestamp: Long) : Serializable
}