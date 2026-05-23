package com.lin0721.linmusic

import com.lin0721.linmusic.data.remote.api.NeteaseApiService
import com.lin0721.linmusic.data.remote.api.*
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
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(CryptoInterceptor()) // 注入加密拦截器
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
            

        } catch (e: Exception) {
            println("=== 请求发生异常 ===")
            e.printStackTrace()
        }
    }

    @Test
    fun testGetComments() = runBlocking {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
            isLenient = true
            explicitNulls = false
        }

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
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://music.163.com")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        val apiService = retrofit.create(NeteaseApiService::class.java)

        println("=== 开始发起请求：getComments ===")
        try {
            val req = CommentsRequest(
                threadId = "R_SO_4_186016",
                rid = "186016",
                limit = 5
            )
            val response = apiService.getComments("R_SO_4_186016", req)
            println("=== 业务层反序列化结果 ===")
            println("Code: ${response.code}")
            println("Total: ${response.total}")
            println("HotComments count: ${response.hotComments.size}")
            println("Comments count: ${response.comments.size}")
            if (response.comments.isNotEmpty()) {
                val c = response.comments[0]
                println("First comment author: ${c.user.nickname}")
                println("First comment content: ${c.content}")
            }
        } catch (e: Exception) {
            println("=== 请求发生异常 ===")
            e.printStackTrace()
            throw e
        }
    }

    @Test
    fun testGetArtistFollowCount() = runBlocking {
        // 1. 初始化 Json
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
            isLenient = true
            explicitNulls = false
        }

        // 2. 初始化 OkHttpClient，并启用日志拦截器以查看详细请求响应
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            println("[OkHttp] $message")
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY 
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    // 添加 Referer 与 PC 端 Cookie，模拟真实环境以防止网易云接口返回空数据
                    .header("Referer", "https://music.163.com")
                    .header("Cookie", "os=pc; osver=Microsoft-Windows-10-Professional-build-10512-64bit; appver=3.0.1.201552")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(CryptoInterceptor()) 
            .addInterceptor(loggingInterceptor)  
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

        val apiService = retrofit.create(NeteaseApiService::class.java)

        println("=== 开始发起请求：getArtistFollowCount ===")
            // 使用松本文纪的歌手ID进行测试，验证粉丝数量接口
            val req = ArtistFollowCountRequest(id = 954433L)
            
            try {
                println("--- 1. Testing getArtistDetailDynamic ---")
                val responseDynamic = apiService.getArtistDetailDynamic(req)
                println("Dynamic Response Code: ${responseDynamic.code}")
                println("Dynamic Response: $responseDynamic")
            } catch (e: Exception) {
                println("getArtistDetailDynamic failed: ${e.message}")
            }

            try {
                println("--- 2. Testing getArtistFollowCount ---")
                val responseFollow = apiService.getArtistFollowCount(req)
                println("Follow Response Code: ${responseFollow.code}")
                println("Follow Response: $responseFollow")
            } catch (e: Exception) {
                println("getArtistFollowCount failed: ${e.message}")
            }

            try {
                println("--- 3. Testing getArtistDetail ---")
                // 使用松本文纪的歌手ID进行测试，验证基本详情接口
                val reqDetail = ArtistDetailRequest(id = 954433L)
                val responseDetail = apiService.getArtistDetail(reqDetail)
                println("Detail Response Code: ${responseDetail.code}")
                println("Detail Response data: ${responseDetail.data}")
            } catch (e: Exception) {
                println("getArtistDetail failed: ${e.message}")
            }

            try {
                println("--- 4. Testing getArtistAlbums ---")
                val responseAlbums = apiService.getArtistAlbums(id = 954433L)
                println("Albums Response Code: ${responseAlbums.code}")
                println("Albums count: ${responseAlbums.hotAlbums.size}")
            } catch (e: Exception) {
                println("getArtistAlbums failed: ${e.message}")
            }
    }

    @Test
    fun testWeapiCryptoComparison() {
        val text = "{\"id\":954433,\"csrf_token\":\"\",\"e_r\":false}"
        val secretKey = "abcdef1234567890"
        val result = com.lin0721.linmusic.data.remote.crypto.NeteaseCrypto.testWeapi(text, secretKey)
        println("Android p1: ${result["p1"]}")
        println("Android params: ${result["params"]}")
        println("Android encSecKey: ${result["encSecKey"]}")
    }
}

