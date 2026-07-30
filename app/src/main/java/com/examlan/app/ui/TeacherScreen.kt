package com.examlan.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.examlan.app.data.GradeExporter
import com.examlan.app.viewmodel.TeacherViewModel

@Composable
fun TeacherScreen() {
    val vm: TeacherViewModel = viewModel()
    val exam by vm.currentExam.collectAsState()
    val submissions by vm.submissionsForCurrentExam.collectAsState()
    val context = LocalContext.current

    var serverStarted by remember { mutableStateOf(false) }

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

            if (!serverStarted) {
                Button(onClick = {
                    vm.startServer(8080)
                    serverStarted = true
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("بدء استقبال الطلاب (تشغيل الخادم)")
                }
            } else {
                Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("الخادم يعمل ✅ - أعطِ الطلاب عنوان IP جهازك والمنفذ 8080")
                        Text("مثال: http://<IP جهازك>:8080")
                    }
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
