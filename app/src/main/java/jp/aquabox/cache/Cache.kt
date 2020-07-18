package jp.aquabox.cache

import io.reactivex.Single
import java.io.Serializable
import java.util.concurrent.TimeUnit

interface Cache {
    fun <T : Serializable> get(key: String): Single<T>
    fun <T : Serializable> set(key: String, value: T, interval: Long = -1, timeUnit: TimeUnit = TimeUnit.MINUTES)
    fun delete(key: String)
}