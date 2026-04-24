package com.lin0721.linmusic.data.remote.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 拦截器：检测空响应体
 *
 * 如果服务端返回 200 OK，但响应体为空或者长度为0，将导致 kotlinx.serialization
 * 抛出 EOF 异常从而引发 Crash。
 * 此时直接抛出 [ApiException]，以便 Repository 能够将其统一转换为流的 Error 状态。
 */
class EmptyBodyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val bodyString = response.peekBody(Long.MAX_VALUE).string()
        if (bodyString.isBlank()) {
            throw ApiException("API body is empty, possibly auth failed")
        }
        
        return response
    }
}
