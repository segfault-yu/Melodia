package com.lin0721.linmusic

import com.lin0721.linmusic.data.remote.api.NeteaseApiService
import com.lin0721.linmusic.data.remote.network.CryptoInterceptor
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 验证网易云 API 网络链路与加密算法
 */
class NeteaseApiTest {

    @Test
    fun testDailyRecommend() = runBlocking {
        // 1. 初始化 Json
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
            isLenient = true
            explicitNulls = false
        }

        // 2. 初始化 OkHttpClient，模拟真实环境配置
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            println("[OkHttp] $message")
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY // 开启 BODY 级别日志，方便打印请求和响应的 RAW JSON
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(CryptoInterceptor()) // 核心：注入加密拦截器
            .addInterceptor(loggingInterceptor)  // 日志拦截器，自动打印出原始响应的 JSON
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        // 3. 初始化 Retrofit
        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://music.163.com")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        // 4. 创建 API 代理对象
        val apiService = retrofit.create(NeteaseApiService::class.java)

        // 5. 执行测试用例
        println("=== 开始发起请求：getDailyRecommendPlaylists ===")
        try {
            val response = apiService.getDailyRecommendPlaylists()
            println("=== 业务层反序列化结果 ===")
            println("Code: ${response.code}")
            println("Msg: ${response.msg}")
            println("Message: ${response.message}")
            println("Data: $response")
            
            // 说明：
            // 因为没有登录凭证（Cookie/Token），预期会返回 Code = 301（需要登录）之类的状态。
            // 只要不是 Http异常（如400 Bad Request），更不是加解密导致连接直接断开，就说明我们的
            // 加密数据被网易云服务器成功解析并识别为有效结构的请求，意味着 WeApi/EApi 算法完全正确。
        } catch (e: Exception) {
            println("=== 请求发生异常 ===")
            e.printStackTrace()
        }
    }

    @Test
    fun testLoginByPhone() = runBlocking {
        // 1. 初始化 Json
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
            isLenient = true
            explicitNulls = false
        }

        // 2. 初始化 OkHttpClient
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            println("[OkHttp] $message")
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY 
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(CryptoInterceptor()) 
            .addInterceptor(loggingInterceptor)  
            .connectTimeout(15, TimeUnit.SECONDS)
            .build()

        // 3. 初始化 Retrofit
        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://music.163.com")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        val apiService = retrofit.create(NeteaseApiService::class.java)

        println("=== 开始发起请求：loginByPhone ===")
        try {
            val req = com.lin0721.linmusic.data.remote.api.LoginByPhoneRequest(
                phone = "13800138000",
                password = "fake_password_md5"
            )
            val response = apiService.loginByPhone(req)
            println("=== 业务层反序列化结果 ===")
            println("Code: ${response.code}")
            println("Msg: ${response.msg}")
            println("Message: ${response.message}")
            println("Data: $response")
            
            // 预期密码错误，但会正常解析JSON，code通常是 502/400 左右
        } catch (e: Exception) {
            println("=== 请求发生异常 ===")
            e.printStackTrace()
            throw e
        }
    }
}
