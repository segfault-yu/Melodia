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
    var loadProgress by remember { mutableIntStateOf(0) }
    
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundBlack
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundBlack)
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

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadProgress = newProgress
                                if (newProgress >= 100) {
                                    isLoading = false
                                }
                            }
                        }
                        
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            // 净化 UA
                            userAgentString = baseUA
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                // 3. JS 自动化劫持与 UI 优化
                                val script = """
                                    (function() {
                                        // A. 暗黑模式注入与元素精简
                                        var style = document.createElement('style');
                                        style.innerHTML = `
                                            body, .m-login, .g-bd { background-color: #121212 !important; color: #ffffff !important; }
                                            .m-topbar, .m-footer, .u-logo, .u-btn-back { display: none !important; }
                                            input { background-color: #1a1a1a !important; color: #ffffff !important; border: 1px solid #333 !important; }
                                            .u-btn-red { background-color: #EA4848 !important; border: none !important; }
                                            .m-login-type { padding-top: 50px !important; }
                                        `;
                                        document.head.appendChild(style);

                                        // B. 自动跳过首屏：点击协议并进入手机登录
                                        setTimeout(function() {
                                            // 勾选同意协议
                                            var checkbox = document.querySelector('.u-chkbx input') || document.querySelector('input[type="checkbox"]');
                                            if (checkbox && !checkbox.checked) checkbox.click();
                                            
                                            // 自动点击“手机号登录”
                                            var phoneBtn = document.querySelector('.u-btn-phone') || document.querySelector('.u-btn-red');
                                            if (phoneBtn && phoneBtn.innerText.indexOf('手机') !== -1) {
                                                phoneBtn.click();
                                            }
                                        }, 500);
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(script, null)
                                
                                // 4. 实时提取 Token (MUSIC_U)
                                checkCookies(onLoginSuccess)
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
                Box(modifier = Modifier.fillMaxSize().background(BackgroundBlack), contentAlignment = Alignment.Center) {
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
