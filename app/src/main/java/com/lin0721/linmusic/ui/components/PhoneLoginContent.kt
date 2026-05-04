package com.lin0721.linmusic.ui.components

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lin0721.linmusic.ui.theme.BackgroundBlack
import com.lin0721.linmusic.ui.theme.NeteaseRed
import com.lin0721.linmusic.ui.theme.SurfaceDark
import com.lin0721.linmusic.ui.theme.TextGray
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 纯原生手机号登录界面 (无头浏览器劫持方案)
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PhoneLoginContent(
    onBack: () -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var captchaCode by remember { mutableStateOf("") }
    var countdown by remember { mutableIntStateOf(0) }
    var showHiddenWeb by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 倒计时逻辑
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000L)
            countdown--
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 顶部标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
            }
            Text(
                text = "手机号验证登录",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 2. 原生输入框表单
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { if (it.length <= 11) phoneNumber = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("手机号码", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = NeteaseRed) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeteaseRed,
                unfocusedBorderColor = SurfaceDark
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = captchaCode,
                onValueChange = { if (it.length <= 6) captchaCode = it },
                modifier = Modifier.weight(1f),
                label = { Text("短信验证码", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = NeteaseRed) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeteaseRed,
                    unfocusedBorderColor = SurfaceDark
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = {
                    if (phoneNumber.length == 11) {
                        countdown = 60
                        // 注入 JS 触发网页发送验证码
                        webViewInstance?.evaluateJavascript(
                            "(function() {" +
                                "var input = document.querySelector('input[type=tel]');" +
                                "if(input) { input.value = '$phoneNumber'; input.dispatchEvent(new Event('input', {bubbles:true})); }" +
                                "var btn = document.querySelector('.j-getcode') || document.querySelector('.getcode');" +
                                "if(btn) btn.click();" +
                            "})();", null
                        )
                    }
                },
                enabled = countdown == 0 && phoneNumber.length == 11,
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if(countdown == 0) NeteaseRed else SurfaceDark)
            ) {
                Text(if (countdown > 0) "${countdown}s" else "获取验证码", fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // 3. 立即登录大按钮
        Button(
            onClick = {
                if (captchaCode.length >= 4) {
                    // 注入 JS 填入验证码并提交
                    webViewInstance?.evaluateJavascript(
                        "(function() {" +
                            "var codeInput = document.querySelectorAll('input[type=number]')[0] || document.querySelector('.j-code');" +
                            "if(codeInput) { codeInput.value = '$captchaCode'; codeInput.dispatchEvent(new Event('input', {bubbles:true})); }" +
                            "var submitBtn = document.querySelector('.j-btn') || document.querySelector('.btn-submit');" +
                            "if(submitBtn) submitBtn.click();" +
                        "})();", null
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = NeteaseRed)
        ) {
            Text("立即登录", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. 无头浏览器
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (showHiddenWeb) Color.White else Color.Transparent)
                .then(
                    if (showHiddenWeb) Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                    else Modifier
                        .size(1.dp)
                        .alpha(0f)
                )
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewInstance = this
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36"
                        }
                        
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onCaptchaShow() {
                                scope.launch { showHiddenWeb = true }
                            }
                            @JavascriptInterface
                            fun onCaptchaHide() {
                                scope.launch { showHiddenWeb = false }
                            }
                        }, "AndroidBridge")

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                // 注入风控雷达：轮询探测易盾滑块
                                view?.evaluateJavascript(
                                    "setInterval(function() {" +
                                        "var captcha = document.querySelector('.yidun_popup') || document.querySelector('#yidun_captcha') || document.querySelector('.yidun_modal');" +
                                        "if(captcha && captcha.style.display !== 'none') {" +
                                            "AndroidBridge.onCaptchaShow();" +
                                        "} else {" +
                                            "AndroidBridge.onCaptchaHide();" +
                                        "}" +
                                    "}, 1000);", null
                                )
                                // 复用 Cookie 探测逻辑
                                checkCookies(onLoginSuccess)
                            }
                        }
                        loadUrl("https://st.music.163.com/login")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            if (showHiddenWeb) {
                // 引导提示
                Box(modifier = Modifier.fillMaxWidth().background(NeteaseRed.copy(alpha = 0.1f)).padding(8.dp)) {
                    Text("请完成安全验证以继续", color = NeteaseRed, fontSize = 12.sp, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

private fun checkCookies(onLoginSuccess: (String) -> Unit) {
    val cookieManager = android.webkit.CookieManager.getInstance()
    val cookies = cookieManager.getCookie("https://music.163.com")
    if (cookies != null && cookies.contains("MUSIC_U=")) {
        onLoginSuccess(cookies)
    }
}
