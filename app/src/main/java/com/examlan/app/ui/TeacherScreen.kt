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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.examlan.app.data.AnswerItem
import com.examlan.app.data.Exam
import com.examlan.app.data.GradeExporter
import com.examlan.app.data.QuestionType
import com.examlan.app.data.SubmissionEntity
import com.examlan.app.viewmodel.TeacherViewModel
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val submissionJson = Json { ignoreUnknownKeys = true }

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
                onCreateExam = { title, durationMinutes, questions, header ->
                    vm.createExam(title, durationMinutes, questions, header)
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
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة كشف الدرجات"))
                }) {
                    Text("📤 تصدير كشف الدرجات")
                }
            }

            TextButton(onClick = { vm.startNewExam() }) {
                Text("+ بدء اختبار جديد (الاختبار الحالي يبقى محفوظاً)")
            }

            LazyColumn(Modifier.weight(1f)) {
                items(submissions) { sub ->
                    SubmissionCard(exam = exam!!, submission = sub, onGrade = { g -> vm.gradeSubmission(sub.autoId, g) })
                }
            }
        }
    }
}

@Composable
private fun SubmissionCard(
    exam: Exam,
    submission: SubmissionEntity,
    onGrade: (Double) -> Unit
) {
    var expanded by remember(submission.autoId) { mutableStateOf(false) }
    var gradeInput by remember(submission.autoId) { mutableStateOf(submission.grade?.toString() ?: "") }

    val answers = remember(submission.answersJson) {
        try {
            submissionJson.decodeFromString(ListSerializer(AnswerItem.serializer()), submission.answersJson)
                .associateBy { it.questionId }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text("الطالب: ${submission.studentName} (${submission.studentId})" + if (submission.studentClass.isNotBlank()) " - الصف: ${submission.studentClass}" else "")
            Text(if (submission.isGraded) "الدرجة: ${submission.grade}" else "لم يتم التصحيح بعد")

            Spacer(Modifier.height(6.dp))
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "▲ إخفاء إجابات الطالب" else "▼ عرض إجابات الطالب")
            }

            if (expanded) {
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    exam.questions.forEachIndexed { index, q ->
                        val answer = answers[q.id]
                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Row {
                                    Text("س${index + 1}: ", style = MaterialTheme.typography.bodyMedium)
                                    RichContent(text = q.text, resources = q.resources, fontSize = 15.sp)
                                }
                                Spacer(Modifier.height(4.dp))
                                when (q.type) {
                                    QuestionType.MULTIPLE_CHOICE -> {
                                        val selectedIndex = answer?.selectedOptionIndex
                                        val selectedText = selectedIndex?.let { q.options.getOrNull(it) }
                                        if (selectedText != null) {
                                            Row {
                                                Text("إجابة الطالب: ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                                                RichContent(text = selectedText, resources = q.resources, fontSize = 16.sp)
                                            }
                                        } else {
                                            Text("إجابة الطالب: لم يُجب", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    QuestionType.ESSAY -> {
                                        Text(
                                            "إجابة الطالب: ${answer?.essayText?.ifBlank { "لم يُجب" } ?: "لم يُجب"}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

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
                    if (g != null) onGrade(g)
                }) { Text("حفظ") }
            }
        }
    }
}
