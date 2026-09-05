package com.lin0721.linmusic.feature.cloud.upload

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.security.MessageDigest

private const val MD5_BUFFER_SIZE = 8192

// 流式计算本地文件 MD5，避免大文件一次性读入内存
fun computeFileMd5(context: Context, uri: Uri): String {
    val digest = MessageDigest.getInstance("MD5")
    context.contentResolver.openInputStream(uri)?.use { input ->
        val buffer = ByteArray(MD5_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

// SAF 返回的 content:// Uri 没有真实路径，文件大小/原始文件名都要经 ContentResolver 查询
fun queryFileSize(context: Context, uri: Uri): Long {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (sizeIndex != -1 && cursor.moveToFirst() && !cursor.isNull(sizeIndex)) {
            return cursor.getLong(sizeIndex)
        }
    }
    return 0L
}

fun queryFileName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && cursor.moveToFirst()) {
            cursor.getString(nameIndex)?.let { return it }
        }
    }
    return uri.lastPathSegment.orEmpty()
}

// 参考实现里找不到扩展名时兜底 mp3
fun fileExtension(fileName: String): String {
    val dotIndex = fileName.lastIndexOf('.')
    if (dotIndex < 0 || dotIndex == fileName.lastIndex) return "mp3"
    return fileName.substring(dotIndex + 1).lowercase()
}
