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
        val encrypted = NeteaseCrypto.weapi(rawJson)
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
        val encrypted = NeteaseCrypto.eapi(eapiPath, rawJson)
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
