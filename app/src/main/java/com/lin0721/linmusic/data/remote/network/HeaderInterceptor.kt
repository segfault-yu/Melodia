package com.lin0721.linmusic.data.remote.network

import com.lin0721.linmusic.data.local.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 网易云反风控拦截器
 * 
 * 强制补充 PC 端的 UA、Referer、海外 IP 伪装以及 os Cookie，
 * 同时注入持久化的用户登录 Cookie。
 */
class HeaderInterceptor(private val userPreferences: UserPreferences) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url
        val urlString = url.toString()
        val newRequestBuilder = originalRequest.newBuilder()

        // 获取持久化的 Cookie
        val storedCookies = runBlocking { userPreferences.cookies.first() }

        if (urlString.contains("/eapi/")) {
            // 核心修复：EApi 必须使用 interface 地址，否则网页版域名会过滤/丢弃原生加密包
            val newUrl = url.newBuilder()
                .host("interface.music.163.com")
                .build()
            newRequestBuilder.url(newUrl)

            // App端请求使用移动配置
            newRequestBuilder.header("User-Agent", "NeteaseMusic 9.0.90/5038 (iPhone; iOS 16.2; zh_CN)")
            newRequestBuilder.removeHeader("Referer")
            
            val requestCookies = originalRequest.headers("Cookie").toMutableList()
            if (storedCookies != null) requestCookies.add(storedCookies)
            
            val cookiesStr = requestCookies.joinToString(";")
            val filteredCookies = cookiesStr.split(";").filterNot { it.trim().startsWith("os=") }.joinToString(";")
            val mobileCookies = "os=ios; appver=9.0.90; osver=16.2"
            newRequestBuilder.header("Cookie", if (filteredCookies.isEmpty()) mobileCookies else "$filteredCookies; $mobileCookies")
        } else {
            // PC端请求使用浏览器配置
            newRequestBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            newRequestBuilder.header("Referer", "https://music.163.com")
            
            val requestCookies = originalRequest.headers("Cookie").toMutableList()
            if (storedCookies != null) requestCookies.add(storedCookies)
            
            val cookiesStr = requestCookies.joinToString(";")
            if (!cookiesStr.contains("os=")) {
                val osCookie = "os=pc; osver=Microsoft-Windows-10-Professional-build-10586-64bit; appver=2.0.3.131777"
                newRequestBuilder.header("Cookie", if (cookiesStr.isEmpty()) osCookie else "$cookiesStr; $osCookie")
            } else {
                newRequestBuilder.header("Cookie", cookiesStr)
            }
        }
        
        return chain.proceed(newRequestBuilder.build())
    }
}
