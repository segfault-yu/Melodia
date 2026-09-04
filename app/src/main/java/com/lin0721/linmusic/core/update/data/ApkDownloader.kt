package com.lin0721.linmusic.core.update.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

sealed class DownloadState {
    data class Downloading(val progress: Int) : DownloadState()
    data class Success(val file: File) : DownloadState()
    data class Failed(val message: String) : DownloadState()
}

class ApkDownloader(
    private val context: Context,
    private val downloadClient: OkHttpClient
) {

    // 已存在同名且大小与远端一致的文件直接跳过重下，不做断点续传
    fun download(url: String, versionName: String): Flow<DownloadState> = flow {
        val dir = File(context.getExternalFilesDir(null), "updates").apply { mkdirs() }
        val targetFile = File(dir, "Melodia-$versionName.apk")

        val request = Request.Builder().url(url).build()
        downloadClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                emit(DownloadState.Failed("下载失败：HTTP ${response.code}"))
                return@flow
            }
            val body = response.body
            if (body == null) {
                emit(DownloadState.Failed("下载失败：响应为空"))
                return@flow
            }

            val total = body.contentLength()
            if (targetFile.exists() && total > 0 && targetFile.length() == total) {
                emit(DownloadState.Success(targetFile))
                return@flow
            }

            var written = 0L
            var lastProgress = -1
            targetFile.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        written += read
                        if (total > 0) {
                            val progress = (written * 100 / total).toInt()
                            if (progress != lastProgress) {
                                lastProgress = progress
                                emit(DownloadState.Downloading(progress))
                            }
                        }
                    }
                }
            }
            emit(DownloadState.Success(targetFile))
        }
    }.flowOn(Dispatchers.IO).catch { e ->
        emit(DownloadState.Failed(e.message ?: "下载失败"))
    }
}
