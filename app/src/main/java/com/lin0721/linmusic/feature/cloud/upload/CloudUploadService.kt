package com.lin0721.linmusic.feature.cloud.upload

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.lin0721.linmusic.MainActivity
import com.lin0721.linmusic.R
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.feature.cloud.data.CloudInfoRequest
import com.lin0721.linmusic.feature.cloud.data.CloudPublishRequest
import com.lin0721.linmusic.feature.cloud.data.CloudUploadApi
import com.lin0721.linmusic.feature.cloud.data.CloudUploadCheckRequest
import com.lin0721.linmusic.feature.cloud.data.NosTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.IOException

private const val TAG = "CloudUploadService"
private const val NOTIFICATION_CHANNEL_ID = "cloud_upload"
private const val NOTIFICATION_ID = 9001
private const val NOS_BUCKET = "jd-musicrep-privatecloud-audio-public"
// 参考实现固定值：客户端不感知真实码率，统一按最高档申请（服务端会按文件实际码率处理）
private const val UPLOAD_BITRATE = "999000"

const val ACTION_CLOUD_UPLOAD_ENQUEUE = "com.lin0721.linmusic.action.CLOUD_UPLOAD_ENQUEUE"
const val ACTION_CLOUD_UPLOAD_RETRY = "com.lin0721.linmusic.action.CLOUD_UPLOAD_RETRY"
const val EXTRA_CLOUD_UPLOAD_URIS = "extra_cloud_upload_uris"
const val EXTRA_CLOUD_UPLOAD_TASK_ID = "extra_cloud_upload_task_id"

// 云盘上传前台服务：批量排队串行上传，是 UploadTask 状态的唯一写入方。
// 独立于 CloudViewModel 的生命周期，退出云盘页/切后台都不中断
class CloudUploadService : Service() {

    private val uploadManager: CloudUploadManager by inject()
    private val cloudUploadApi: CloudUploadApi by inject()
    private val nosUploadClient: NosUploadClient by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingQueue = ArrayDeque<String>()
    private var processingJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CLOUD_UPLOAD_ENQUEUE -> {
                val uris = intent.getParcelableArrayListExtra<Uri>(EXTRA_CLOUD_UPLOAD_URIS).orEmpty()
                val tasks = uris.map { uri -> UploadTask(uri = uri, fileName = queryFileName(this, uri)) }
                if (tasks.isNotEmpty()) {
                    uploadManager.enqueue(tasks)
                    tasks.forEach { pendingQueue.addLast(it.id) }
                }
            }

            ACTION_CLOUD_UPLOAD_RETRY -> {
                val taskId = intent.getStringExtra(EXTRA_CLOUD_UPLOAD_TASK_ID)
                if (taskId != null) {
                    uploadManager.retryTask(taskId)
                    pendingQueue.addLast(taskId)
                }
            }
        }

        startForegroundWithNotification()
        ensureProcessing()
        return START_NOT_STICKY
    }

    private fun ensureProcessing() {
        if (processingJob?.isActive == true) return
        processingJob = serviceScope.launch {
            while (pendingQueue.isNotEmpty()) {
                val taskId = pendingQueue.removeFirst()
                val task = uploadManager.taskById(taskId) ?: continue
                runUploadPipeline(task)
                updateNotification()
            }
            stopForegroundAndSelf()
        }
    }

    // 每一步成功即写回 CloudUploadManager，保证进度弹窗/通知随时看到最新状态，
    // 也保证同一个文件重试时能从已缓存的字段继续（阶段级跳过，见 UploadTask 注释）
    private suspend fun runUploadPipeline(initialTask: UploadTask) {
        var task = initialTask
        fun apply(transform: (UploadTask) -> UploadTask) {
            uploadManager.updateTask(task.id, transform)
            task = uploadManager.taskById(task.id) ?: task
        }

        try {
            if (task.md5 == null || task.fileSize == null) {
                apply { it.copy(status = UploadStatus.CHECKING) }
                val fileSize = queryFileSize(this, task.uri)
                val md5 = computeFileMd5(this, task.uri)
                val metadata = extractLocalAudioMetadata(this, task.uri)
                apply { it.copy(md5 = md5, fileSize = fileSize, metadata = metadata) }
            }

            val md5 = requireNotNull(task.md5)
            val fileSize = requireNotNull(task.fileSize)
            val ext = fileExtension(task.fileName)

            if (task.checkedSongId == null) {
                apply { it.copy(status = UploadStatus.CHECKING) }
                val checkResp = cloudUploadApi.checkUpload(
                    CloudUploadCheckRequest(bitrate = UPLOAD_BITRATE, length = fileSize, md5 = md5)
                )
                if (!checkResp.isSuccess) throw IOException("查重失败 code=${checkResp.code}")
                apply { it.copy(checkedSongId = checkResp.songId, needUpload = checkResp.needUpload) }
            }

            val checkedSongId = requireNotNull(task.checkedSongId)
            val needUpload = task.needUpload ?: true

            // Stage 2/3：token 不缓存，每次重新申请，避免复用可能已过期的旧凭证
            apply { it.copy(status = UploadStatus.ALLOCATING_TOKEN, progress = 0f) }
            val tokenResp = cloudUploadApi.allocNosToken(
                NosTokenRequest(ext = ext, filename = task.fileName, md5 = md5)
            )
            val tokenResult = tokenResp.result ?: throw IOException("申请上传凭证失败")
            val resourceId = tokenResult.resourceId
            val bucket = tokenResult.bucket.ifBlank { NOS_BUCKET }

            if (needUpload) {
                val hosts = nosUploadClient.fetchUploadHosts(bucket)
                val host = hosts.firstOrNull() ?: throw IOException("获取上传服务器地址失败")

                apply { it.copy(status = UploadStatus.UPLOADING) }
                val mimeType = contentResolver.getType(task.uri) ?: "audio/mpeg"
                nosUploadClient.uploadBytes(
                    host = host,
                    bucket = bucket,
                    objectKey = tokenResult.objectKey,
                    token = tokenResult.token,
                    md5 = md5,
                    mimeType = mimeType,
                    contentLength = fileSize,
                    openStream = {
                        contentResolver.openInputStream(task.uri) ?: throw IOException("无法打开文件")
                    },
                    onProgress = { progress ->
                        apply { it.copy(progress = progress) }
                        updateNotification()
                    }
                )
            } else {
                AppLogger.d(TAG, "文件已存在于服务器，跳过字节上传 file=${task.fileName}")
            }

            apply { it.copy(status = UploadStatus.REGISTERING, progress = 1f) }
            val metadata = task.metadata
            val songName = metadata?.title ?: task.fileName.substringBeforeLast('.', task.fileName)
            val infoResp = cloudUploadApi.registerCloudInfo(
                CloudInfoRequest(
                    md5 = md5,
                    songid = checkedSongId,
                    filename = task.fileName,
                    song = songName,
                    album = metadata?.album ?: "未知专辑",
                    artist = metadata?.artist ?: "未知艺术家",
                    bitrate = UPLOAD_BITRATE,
                    resourceId = resourceId
                )
            )
            if (!infoResp.isSuccess) throw IOException("登记歌曲信息失败 code=${infoResp.code}")

            apply { it.copy(status = UploadStatus.PUBLISHING) }
            val pubResp = cloudUploadApi.publishCloud(CloudPublishRequest(songid = infoResp.songId))
            if (!pubResp.isSuccess) throw IOException("发布失败 code=${pubResp.code}")

            apply { it.copy(status = UploadStatus.SUCCESS, progress = 1f) }
            uploadManager.notifyPublished()
        } catch (e: Exception) {
            AppLogger.w(TAG, "上传失败 file=${task.fileName}", e)
            apply { it.copy(status = UploadStatus.FAILED, errorMessage = e.message ?: "上传失败") }
        }
    }

    // ======================= 通知 =======================

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "云盘上传",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        // 未授权通知权限时静默跳过，不阻断上传本身
        val granted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !granted) return

        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val tasks = uploadManager.state.value
        val total = tasks.size
        val doneCount = tasks.count { it.status == UploadStatus.SUCCESS }
        val activeTask = tasks.firstOrNull {
            it.status != UploadStatus.SUCCESS && it.status != UploadStatus.FAILED && it.status != UploadStatus.QUEUED
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(activeTask != null)

        return if (activeTask != null) {
            val percent = (activeTask.progress * 100).toInt()
            builder
                .setContentTitle("正在上传 ($doneCount/$total)")
                .setContentText("${activeTask.fileName} · $percent%")
                .setProgress(100, percent, activeTask.status != UploadStatus.UPLOADING)
                .build()
        } else {
            val failedCount = tasks.count { it.status == UploadStatus.FAILED }
            val summary = if (failedCount > 0) "完成，$failedCount 首失败" else "全部上传完成"
            builder
                .setContentTitle(summary)
                .setContentText("$doneCount/$total 首成功")
                .setProgress(0, 0, false)
                .build()
        }
    }

    private fun stopForegroundAndSelf() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
