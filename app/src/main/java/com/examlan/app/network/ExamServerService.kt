package com.examlan.app.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.examlan.app.MainActivity
import com.examlan.app.data.AnswerItem
import com.examlan.app.data.ExamServerState
import com.examlan.app.data.SubmissionEntity
import com.examlan.app.data.db.AppDatabase
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * خدمة تعمل في المقدمة (Foreground Service) مع إشعار دائم، حتى يستمر خادم
 * استقبال إجابات الطلاب يعمل حتى لو خرج الأستاذ من التطبيق أو قفل الشاشة.
 *
 * هذا ضروري لأن أندرويد يوقف أي عمل شبكي داخل تطبيق عادي بمجرد الانتقال للخلفية،
 * أما خدمة Foreground فتبقى تعمل طالما الإشعار ظاهر.
 */
class ExamServerService : Service() {

    private var teacherServer: TeacherServer? = null
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val CHANNEL_ID = "exam_server_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.examlan.app.action.STOP_SERVER"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopServerAndSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())

        if (teacherServer == null) {
            val db = AppDatabase.getInstance(applicationContext)
            teacherServer = TeacherServer(
                port = 8080,
                getCurrentExam = { ExamServerState.currentExam.value },
                onSubmissionReceived = { payload ->
                    // الحفظ الدائم فور الاستلام مباشرة في قاعدة البيانات
                    db.teacherDao().insertSubmission(
                        SubmissionEntity(
                            examId = payload.examId,
                            studentId = payload.studentId,
                            studentName = payload.studentName,
                            answersJson = json.encodeToString(
                                ListSerializer(AnswerItem.serializer()), payload.answers
                            ),
                            receivedAtEpochMs = System.currentTimeMillis()
                        )
                    )
                }
            )
            teacherServer?.start()
        }

        // START_STICKY: لو نظام أندرويد أوقف الخدمة قسراً بسبب نقص ذاكرة، يعيد تشغيلها تلقائياً
        return START_STICKY
    }

    private fun stopServerAndSelf() {
        teacherServer?.stop()
        teacherServer = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        teacherServer?.stop()
        teacherServer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "خادم الاختبار",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "يبقي خادم استقبال إجابات الطلاب يعمل أثناء الاختبار"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("خادم الاختبار يعمل ✅")
            .setContentText("جاري استقبال إجابات الطلاب - لا تغلق هذا الإشعار أثناء الاختبار")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }
}
