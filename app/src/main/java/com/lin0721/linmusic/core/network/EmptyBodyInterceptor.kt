package com.lin0721.linmusic.core.network

import okhttp3.Interceptor
import okhttp3.Response

// 拦截器：检测空响应体。避免 kotlinx.serialization 抛出 EOF 异常。
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
