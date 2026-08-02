package com.examlan.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.examlan.app.data.GradeExporter
import com.examlan.app.viewmodel.TeacherViewModel

@Composable
fun TeacherScreen() {
    val vm: TeacherViewModel = viewModel()
    val exam by vm.currentExam.collectAsState()
    val submissions by vm.submissionsForCurrentExam.collectAsState()
    val serverRunning by vm.isServerRunning.collectAsState()
    val context = LocalContext.current

    // طلب صلاحية الإشعارات (مطلوبة من أندرويد 13 فأعلى) حتى تظهر إشعار "الخادم يعمل"
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // حتى لو رفض الإذن، الخادم يستمر يعمل - بس بدون إشعار مرئي واضح
        vm.startServer()
    }

    fun requestStartServer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                vm.startServer()
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            vm.startServer()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("وضع الأستاذ", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        if (exam == null) {
            // شاشة إنشاء الاختبار الكاملة (عنوان + مدة + أسئلة ديناميكية بأي لغة)
            ExamBuilderScreen(
                onCreateExam = { title, durationMinutes, questions ->
                    vm.createExam(title, durationMinutes, questions)
                }
            )
        } else {
            Text("الاختبار الحالي: ${exam!!.title}")
            Spacer(Modifier.height(8.dp))

            if (!serverRunning) {
                Button(onClick = { requestStartServer() }, modifier = Modifier.fillMaxWidth()) {
                    Text("بدء استقبال الطلاب (تشغيل الخادم)")
                }
                Text(
                    "ملاحظة: سيظهر إشعار دائم أثناء عمل الخادم - هذا مقصود حتى يستمر العمل حتى لو خرجت من التطبيق",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("الخادم يعمل ✅ - أعطِ الطلاب عنوان IP جهازك والمنفذ 8080")
                        Text("مثال: http://<IP جهازك>:8080")
                        Text("يمكنك الآن الخروج من التطبيق بأمان - سيستمر الخادم بفضل الإشعار الدائم")
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = { vm.stopServer() }, modifier = Modifier.fillMaxWidth()) {
                    Text("إيقاف الخادم")
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("الإجابات المستلمة (${submissions.size})", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = {
                    val file = GradeExporter.exportGradesToExcel(context, exam!!.title, submissions)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة كشف الدرجات"))
                }) {
                    Text("📤 تصدير كشف الدرجات")
                }
            }

            LazyColumn(Modifier.weight(1f)) {
                items(submissions) { sub ->
                    var gradeInput by remember(sub.autoId) { mutableStateOf(sub.grade?.toString() ?: "") }
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("الطالب: ${sub.studentName} (${sub.studentId})")
                            Text(if (sub.isGraded) "الدرجة: ${sub.grade}" else "لم يتم التصحيح بعد")
                            Spacer(Modifier.height(6.dp))
                            Row {
                                OutlinedTextField(
                                    value = gradeInput,
                                    onValueChange = { gradeInput = it },
                                    label = { Text("الدرجة") },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = {
                                    val g = gradeInput.toDoubleOrNull()
                                    if (g != null) vm.gradeSubmission(sub.autoId, g)
                                }) { Text("حفظ") }
                            }
                        }
                    }
                }
            }
        }
    }
}
