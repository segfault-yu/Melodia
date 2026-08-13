package com.lin0721.linmusic.core.network

import com.lin0721.linmusic.R

// 将 AppError 类型解析为面向用户的中文提示，文案统一收在 strings.xml
fun AppError.toMessage(resourceProvider: ResourceProvider): String = when (this) {
    AppError.NetworkError -> resourceProvider.getString(R.string.app_error_network)
    AppError.RiskControl -> resourceProvider.getString(R.string.app_error_risk_control)
    AppError.Unauthorized -> resourceProvider.getString(R.string.app_error_unauthorized)
    AppError.ParseError -> resourceProvider.getString(R.string.app_error_parse)
    is AppError.BizError -> resourceProvider.getString(R.string.app_error_biz_default)
}

// 统一取错误提示：Repository 出口应始终是 AppError，非 AppError 兜底走原始 message
fun Throwable.toUserMessage(resourceProvider: ResourceProvider): String =
    (this as? AppError)?.toMessage(resourceProvider) ?: (message ?: resourceProvider.getString(R.string.app_error_biz_default))
