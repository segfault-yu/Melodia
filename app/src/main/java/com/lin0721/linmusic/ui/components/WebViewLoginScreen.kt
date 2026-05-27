package com.lin0721.linmusic.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lin0721.linmusic.ui.theme.BackgroundBlack
import com.lin0721.linmusic.ui.theme.NeteaseRed
import com.lin0721.linmusic.ui.theme.SurfaceDark

/**
 * 沉浸式网页授权登录界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewLoginScreen(
    onClose: () -> Unit,
    onLoginSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    
    // 1. 深度伪装：净化 UA（剔除 wv/WebView 关键字）
    val baseUA = "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36"
    
    val loginUrl = "https://music.163.com/m/login"
    val extraHeaders = mapOf(
        "X-Real-IP" to "116.25.250.66",
        "X-Forwarded-For" to "116.25.250.66"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "安全授权登录",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
					)
				},
				navigationIcon = {
					IconButton(onClick = onClose) {
						Icon(
							imageVector = Icons.Default.Close,
							contentDescription = "关闭",
							tint = Color.White
						)
					}
				},
				colors = TopAppBarDefaults.topAppBarColors(
					containerColor = BackgroundBlack
				)
			)
		},
        containerColor = Color(0xFFF5F5F7) // 容器底色改为网页同款浅灰，彻底避免键盘弹出或加载时闪黑屏/白屏
	) { paddingValues ->
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues)
                .background(Color(0xFFF5F5F7)) // 外层容器底色一致
		) {
			AndroidView(
				factory = { context ->
					WebView(context).apply {
						// 2. 完全隔离容器：初始化前清空旧状态
						CookieManager.getInstance().let { manager ->
							manager.setAcceptCookie(true)
							manager.setAcceptThirdPartyCookies(this, true)
							manager.removeAllCookies(null)
							manager.flush()
						}

                        // 将 WebView 底色置为浅灰，防止 resize 布局重绘时闪烁
                        setBackgroundColor(android.graphics.Color.parseColor("#F5F5F7"))

                        webChromeClient = WebChromeClient()
						
						settings.apply {
							javaScriptEnabled = true
							domStorageEnabled = true
							loadWithOverviewMode = true
							useWideViewPort = true
							mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
							// 净化 UA
							userAgentString = baseUA
						}

						webViewClient = object : WebViewClient() {
							override fun onPageFinished(view: WebView?, url: String?) {
								// 实时提取 Token (MUSIC_U)
								checkCookies(onLoginSuccess)
                                isLoading = false // 页面完全加载渲染完成后，关闭加载指示器
							}

							override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
								checkCookies(onLoginSuccess)
							}

							override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
								return false
							}
						}
						
						loadUrl(loginUrl, extraHeaders)
					}
				},
				modifier = Modifier.fillMaxSize()
			)

			// 加载过渡
			AnimatedVisibility(
				visible = isLoading,
				enter = fadeIn(),
				exit = fadeOut(),
				modifier = Modifier.fillMaxSize()
			) {
                // 加载页背景也改成网页同款浅灰，保持视觉过渡一致
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F7)), contentAlignment = Alignment.Center) {
					CircularProgressIndicator(color = NeteaseRed)
				}
			}
		}
	}
}

private fun checkCookies(onLoginSuccess: (String) -> Unit) {
    val cookieManager = CookieManager.getInstance()
    val cookies = cookieManager.getCookie("https://music.163.com")
    
    // 状态同步：一旦检测到 MUSIC_U= 则视为成功
    if (cookies != null && cookies.contains("MUSIC_U=")) {
        onLoginSuccess(cookies)
    }
}
