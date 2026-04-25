package com.lin0721.linmusic.data.remote.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 网易云反风控拦截器
 * 
 * 强制补充 PC 端的 UA、Referer、海外 IP 伪装以及 os Cookie，
 * 避免免登录或其他公开接口被判定为由于缺失设备指纹或受限于国内地域而封禁 (响应体为空等)。
 */
class HeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url
        val urlString = url.toString()
        val newRequestBuilder = originalRequest.newBuilder()

        if (urlString.contains("/eapi/")) {
            // 核心修复：EApi 必须使用 interface 地址，否则网页版域名会过滤/丢弃原生加密包
            val newUrl = url.newBuilder()
                .host("interface.music.163.com")
                .build()
            newRequestBuilder.url(newUrl)

            // App端请求使用移动配置
            newRequestBuilder.header("User-Agent", "NeteaseMusic 9.0.90/5038 (iPhone; iOS 16.2; zh_CN)")
            newRequestBuilder.removeHeader("Referer")
            
            val cookies = originalRequest.headers("Cookie").joinToString(";")
            val filteredCookies = cookies.split(";").filterNot { it.trim().startsWith("os=") }.joinToString(";")
            val mobileCookies = "os=ios; appver=9.0.90; osver=16.2"
            newRequestBuilder.header("Cookie", if (filteredCookies.isEmpty()) mobileCookies else "$filteredCookies; $mobileCookies")
        } else {
            // PC端请求使用浏览器配置
            newRequestBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            newRequestBuilder.header("Referer", "https://music.163.com")
            
            val cookies = originalRequest.headers("Cookie").joinToString(";")
            if (!cookies.contains("os=")) {
                val osCookie = "os=pc; osver=Microsoft-Windows-10-Professional-build-10586-64bit; appver=2.0.3.131777"
                newRequestBuilder.header("Cookie", if (cookies.isEmpty()) osCookie else "$cookies; $osCookie")
            }
        }
        
        return chain.proceed(newRequestBuilder.build())
    }
}
