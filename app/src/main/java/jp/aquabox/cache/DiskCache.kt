package jp.aquabox.cache

import com.jakewharton.disklrucache.DiskLruCache
import io.reactivex.Single
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.concurrent.TimeUnit

class DiskCache(val cache: DiskLruCache) : Cache {
    override fun <T : Serializable> get(key: String): Single<T> {
        return Single.create {
            val v = ObjectInputStream(cache.get(key).getInputStream(0)).readObject() as CacheValue
            if (v.timestamp >= System.currentTimeMillis()) {
                v.value
            } else {
                throw IllegalStateException("cache expired")
            }
        }
    }

    override fun <T : Serializable> set(key: String, value: T, interval: Long, timeUnit: TimeUnit): Single<Boolean> {
        return Single.create {
            ObjectOutputStream(cache.edit(key).newOutputStream(0))
                    .writeObject(CacheValue(value, System.currentTimeMillis() + timeUnit.convert(interval, TimeUnit.MILLISECONDS)))
        }
    }

    override fun delete(key: String) {
        cache.remove(key)
    }

    class CacheValue(val value: Serializable, val timestamp: Long) : Serializable
}