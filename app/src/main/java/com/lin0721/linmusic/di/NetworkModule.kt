package com.lin0721.linmusic.di

import com.lin0721.linmusic.data.remote.api.NeteaseApiService
import com.lin0721.linmusic.data.remote.network.CryptoInterceptor
import com.lin0721.linmusic.data.remote.network.EmptyBodyInterceptor
import com.lin0721.linmusic.data.remote.network.HeaderInterceptor
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

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
    single { HeaderInterceptor() }

    // ─── OkHttpClient ───
    single {
        OkHttpClient.Builder()
            .addInterceptor(get<EmptyBodyInterceptor>())
            .addInterceptor(get<HeaderInterceptor>())
            .addInterceptor(get<CryptoInterceptor>())
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
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
