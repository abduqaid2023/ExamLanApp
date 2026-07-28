package com.examlan.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.examlan.app.data.AnswerItem
import com.examlan.app.data.Question
import com.examlan.app.data.QuestionType
import com.examlan.app.viewmodel.TeacherViewModel
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Composable
fun TeacherScreen() {
    val vm: TeacherViewModel = viewModel()
    val exam by vm.currentExam.collectAsState()
    val submissions by vm.submissionsForCurrentExam.collectAsState()

    var title by remember { mutableStateOf("") }
    var serverStarted by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("وضع الأستاذ", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        if (exam == null) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان الاختبار") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                // مثال بسيط: سؤالان (اختيار من متعدد + مقالي) - وسّعها حسب الحاجة
                val questions = listOf(
                    Question(id = "q1", text = "ما ناتج 2 + 2؟", type = QuestionType.MULTIPLE_CHOICE, options = listOf("3", "4", "5")),
                    Question(id = "q2", text = "اشرح مفهوم الشبكة المحلية LAN", type = QuestionType.ESSAY)
                )
                vm.createExam(title.ifBlank { "اختبار بدون عنوان" }, durationMinutes = 30, questions = questions)
            }, modifier = Modifier.fillMaxWidth()) {
                Text("إنشاء الاختبار")
            }
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
            Text("الإجابات المستلمة (${submissions.size})", style = MaterialTheme.typography.titleMedium)

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
