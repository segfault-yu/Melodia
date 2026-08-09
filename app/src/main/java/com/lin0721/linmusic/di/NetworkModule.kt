package com.lin0721.linmusic.di

import com.lin0721.linmusic.core.api.NeteaseApiService
import com.lin0721.linmusic.core.network.CryptoInterceptor
import com.lin0721.linmusic.core.network.EmptyBodyInterceptor
import com.lin0721.linmusic.core.network.HeaderInterceptor
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import com.lin0721.linmusic.BuildConfig
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import com.lin0721.linmusic.data.local.SettingsPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Koin 网络层依赖注入模块
 *
 * 提供：
 * - [Json]                ： kotlinx.serialization 全局实例
 * - [CryptoInterceptor]   ： 网易云加密拦截器
 * - [OkHttpClient]        ： 含加密拦截器 + 日志拦截器
 * - [Retrofit]            ： 基于 kotlinx.serialization 的转换器
 * - [NeteaseApiService]   ： Retrofit 代理接口
 */
val networkModule = module {

    // ─── kotlinx.serialization Json 实例 ───
    single {
        Json {
            ignoreUnknownKeys = true       // 忽略服务端返回的未知字段
            coerceInputValues = true        // null → 默认值
            encodeDefaults = true           // 序列化时包含默认值
            isLenient = true                // 宽松解析
            explicitNulls = false           // 序列化时跳过 null 字段
        }
    }

    // ─── 加密拦截器 ───
    single { CryptoInterceptor() }

    // ─── 空响应体拦截器 ───
    single { EmptyBodyInterceptor() }

    // ─── 请求头拦截器 ───
    single { HeaderInterceptor(get(), get()) }

    // ─── OkHttpClient ───
    single {
        val settingsPreferences: SettingsPreferences = get()
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            settingsPreferences.useProxy.collectLatest { enabled ->
                NetworkConfig.useProxy = enabled
            }
        }

        val proxySelector = object : java.net.ProxySelector() {
            override fun select(uri: java.net.URI?): MutableList<java.net.Proxy> {
                return if (NetworkConfig.useProxy) {
                    java.net.ProxySelector.getDefault()?.select(uri) ?: mutableListOf(java.net.Proxy.NO_PROXY)
                } else {
                    mutableListOf(java.net.Proxy.NO_PROXY)
                }
            }

            override fun connectFailed(uri: java.net.URI?, sa: java.net.SocketAddress?, ioe: java.io.IOException?) {
                java.net.ProxySelector.getDefault()?.connectFailed(uri, sa, ioe)
            }
        }

        OkHttpClient.Builder()
            .proxySelector(proxySelector)
            .addInterceptor(get<EmptyBodyInterceptor>())
            .addInterceptor(get<HeaderInterceptor>())
            .addInterceptor(get<CryptoInterceptor>())
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // ─── Retrofit ───
    single {
        val json: Json = get()
        val contentType = "application/json".toMediaType()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(get())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    // ─── API Service ───
    single<NeteaseApiService> {
        get<Retrofit>().create(NeteaseApiService::class.java)
    }
}

private const val BASE_URL = "https://music.163.com"

object NetworkConfig {
    @Volatile
    var useProxy: Boolean = false
}
