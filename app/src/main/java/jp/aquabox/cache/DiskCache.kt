package jp.aquabox.cache

import com.jakewharton.disklrucache.DiskLruCache
import io.reactivex.Single
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.concurrent.TimeUnit

class DiskCache(private val cache: DiskLruCache) : Cache {
    override fun <T : Serializable> get(key: String): Single<T> {
        return Single.create<T> { emitter ->
            try {
                ObjectInputStream(cache.get(key).getInputStream(0)).use {
                    val v = it.readObject() as CacheValue<T>
                    when {
                        v.timestamp >= System.currentTimeMillis() -> emitter.onSuccess(v.value)
                        v.timestamp < 0 -> emitter.onSuccess(v.value)
                        else -> emitter.onError(IllegalStateException("cache expired"))
                    }
                }
            } catch (e: Exception) {
                emitter.onError(e)
            }

        }
    }

    override fun <T : Serializable> set(key: String, value: T, interval: Long, timeUnit: TimeUnit): Boolean {
        cache.edit(key).let {
            try {
                ObjectOutputStream(it.newOutputStream(0)).use {
                    it.writeObject(
                            CacheValue(
                                    value,
                                    if (interval >= 0) {
                                        System.currentTimeMillis() + timeUnit.convert(interval, TimeUnit.MILLISECONDS)
                                    } else {
                                        -1
                                    }))
                }
                cache.flush()
                it.commit()
            } catch (e: Exception) {
                return false
            }

            return true
        }
    }

    override fun delete(key: String) {
        cache.remove(key)
    }

    class CacheValue<T>(val value: T, val timestamp: Long) : Serializable
}