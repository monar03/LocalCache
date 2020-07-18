package jp.aquabox.cache

import com.jakewharton.disklrucache.DiskLruCache
import io.reactivex.Single
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.concurrent.TimeUnit

class DiskCache(val cache: DiskLruCache) : Cache {
    override fun <T : Serializable> get(key: String): Single<T> {
        return Single.create<T> { emitter ->
            var input: ObjectInputStream? = null
            try {
                input = ObjectInputStream(cache.get(key).getInputStream(0))
                val v = input.readObject() as CacheValue
                input.close()
                when {
                    v.timestamp >= System.currentTimeMillis() -> emitter.onSuccess(v.value as T)
                    v.timestamp < 0 -> emitter.onSuccess(v.value as T)
                    else -> emitter.onError(IllegalStateException("cache expired"))
                }
            } catch (e: Exception) {
                emitter.onError(e)
            } finally {
                input?.close()
            }

        }
    }

    override fun <T : Serializable> set(key: String, value: T, interval: Long, timeUnit: TimeUnit) {
        cache.edit(key).let {
            val output = ObjectOutputStream(it.newOutputStream(0))
            output.writeObject(
                    CacheValue(
                            value,
                            if (interval >= 0) {
                                System.currentTimeMillis() + timeUnit.convert(interval, TimeUnit.MILLISECONDS)
                            } else {
                                -1
                            }))

            output.close()
            cache.flush()
            it.commit()
        }
    }

    override fun delete(key: String) {
        cache.remove(key)
    }

    class CacheValue(val value: Serializable, val timestamp: Long) : Serializable
}