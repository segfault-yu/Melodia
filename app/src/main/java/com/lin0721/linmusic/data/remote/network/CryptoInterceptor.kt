package com.lin0721.linmusic.data.remote.network

import com.lin0721.linmusic.data.remote.crypto.NeteaseCrypto
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer

/**
 * OkHttp 拦截器 —— 自动识别网易云 API 类型并加密请求体
 *
 * 路由规则：
 * - URL 包含 `/eapi/`  → EApi 加密
 * - URL 包含 `/api/`   → WeApi 加密（含 LinuxApi 降级可能）
 * - 其它                → 透传，不加密
 */
class CryptoInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()

        // 仅拦截带有请求体的请求
        val originalBody = originalRequest.body ?: return chain.proceed(originalRequest)

        val cryptoType = resolveCryptoType(url) ?: return chain.proceed(originalRequest)
        val rawJson = originalBody.readString()

        val encryptedForm = when (cryptoType) {
            CryptoType.WEAPI -> buildWeApiForm(rawJson)
            CryptoType.EAPI -> buildEApiForm(url, rawJson)
            CryptoType.LINUXAPI -> buildLinuxApiForm(rawJson)
        }

        val newRequest = originalRequest.newBuilder()
            .post(encryptedForm)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()

        return chain.proceed(newRequest)
    }

    // ---------- 路由判定 ----------

    private enum class CryptoType { WEAPI, EAPI, LINUXAPI }

    /**
     * 根据 URL 路径判断加密类型
     */
    private fun resolveCryptoType(url: String): CryptoType? = when {
        url.contains("/eapi/") -> CryptoType.EAPI
        url.contains("/linux/api/") -> CryptoType.LINUXAPI
        url.contains("/weapi/") || url.contains("/api/") -> CryptoType.WEAPI
        else -> null
    }

    // ---------- 各模式加密并构造 FormBody ----------

    /**
     * WeApi：加密后包含 params + encSecKey
     */
    private fun buildWeApiForm(rawJson: String): FormBody {
        val jsonPayload = try {
            val orgJson = org.json.JSONObject(rawJson)
            if (!orgJson.has("csrf_token")) {
                orgJson.put("csrf_token", "")
            }
            orgJson.toString()
        } catch (e: Exception) {
            rawJson
        }

        val encrypted = NeteaseCrypto.weapi(jsonPayload)
        return FormBody.Builder()
            .add("params", encrypted.getValue("params"))
            .add("encSecKey", encrypted.getValue("encSecKey"))
            .build()
    }

    /**
     * EApi：加密后仅包含 params
     */
    private fun buildEApiForm(url: String, rawJson: String): FormBody {
        // 从完整 URL 中提取 /eapi/... 路径部分作为 EApi 所需的 url 参数
        val eapiPath = extractEApiPath(url)
        val apiPath = eapiPath.replace("/eapi/", "/api/")
        
        // 给原生 JSON payload 加入风控 Header 上下文
        val rootObj = try {
            org.json.JSONObject(rawJson)
        } catch (e: Exception) {
            org.json.JSONObject()
        }
        
        val headerObj = org.json.JSONObject().apply {
            put("osver", "16.2")
            put("deviceId", "iPhone14,2")
            put("os", "ios")
            put("appver", "9.0.90")
            put("versioncode", "140")
            put("mobilename", "")
            put("buildver", System.currentTimeMillis().toString().substring(0, 10))
            put("resolution", "1920x1080")
            put("requestId", "${System.currentTimeMillis()}_${(1000..9999).random()}")
        }
        rootObj.put("header", headerObj)
        rootObj.put("e_r", false)

        val encrypted = NeteaseCrypto.eapi(apiPath, rootObj.toString())
        return FormBody.Builder()
            .add("params", encrypted.getValue("params"))
            .build()
    }

    /**
     * LinuxApi：加密后仅包含 eparams
     */
    private fun buildLinuxApiForm(rawJson: String): FormBody {
        val encrypted = NeteaseCrypto.linuxapi(rawJson)
        return FormBody.Builder()
            .add("eparams", encrypted.getValue("eparams"))
            .build()
    }

    // ---------- 工具方法 ----------

    /**
     * 将 [RequestBody] 读取为字符串
     */
    private fun RequestBody.readString(): String {
        val buffer = Buffer()
        writeTo(buffer)
        return buffer.readUtf8()
    }
    /**
     * 从完整 URL 中提取 /eapi/ 及之后的路径
     * 例如 https://music.163.com/eapi/song/detail → /eapi/song/detail
     */
    private fun extractEApiPath(url: String): String {
        val idx = url.indexOf("/eapi/")
        return if (idx != -1) url.substring(idx) else url
    }
}
