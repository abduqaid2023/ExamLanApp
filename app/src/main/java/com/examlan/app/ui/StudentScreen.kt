package com.examlan.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.examlan.app.data.AnswerItem
import com.examlan.app.data.QuestionType
import com.examlan.app.viewmodel.StudentViewModel
import com.examlan.app.viewmodel.UploadState

@Composable
fun StudentScreen() {
    val vm: StudentViewModel = viewModel()
    val exam by vm.exam.collectAsState()
    val answers by vm.answers.collectAsState()
    val uploadState by vm.uploadState.collectAsState()

    var ip by remember { mutableStateOf("192.168.1.5") }
    var name by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("وضع الطالب", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        if (exam == null) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الطالب") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = studentId, onValueChange = { studentId = it }, label = { Text("الرقم الجامعي/الأكاديمي") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("عنوان IP الخاص بجهاز الأستاذ") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { vm.connectAndFetchExam(teacherIp = ip, port = 8080, name = name, id = studentId) },
                enabled = name.isNotBlank() && studentId.isNotBlank() && ip.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("الاتصال وجلب الاختبار") }
        } else {
            Text(exam!!.title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            LazyColumn(Modifier.weight(1f)) {
                items(exam!!.questions) { q ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            RichContent(text = q.text, resources = q.resources, fontSize = 17.sp, bold = true)
                            Spacer(Modifier.height(6.dp))

                            when (q.type) {
                                QuestionType.MULTIPLE_CHOICE -> {
                                    val selected = answers[q.id]?.selectedOptionIndex
                                    q.options.forEachIndexed { index, option ->
                                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = selected == index,
                                                onClick = {
                                                    vm.updateAnswer(AnswerItem(questionId = q.id, selectedOptionIndex = index))
                                                }
                                            )
                                            RichContent(text = option, resources = q.resources, fontSize = 15.sp)
                                        }
                                    }
                                }
                                QuestionType.ESSAY -> {
                                    var text by remember(q.id) { mutableStateOf(answers[q.id]?.essayText ?: "") }
                                    OutlinedTextField(
                                        value = text,
                                        onValueChange = {
                                            text = it
                                            vm.updateAnswer(AnswerItem(questionId = q.id, essayText = it))
                                        },
                                        label = { Text("إجابتك") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            when (uploadState) {
                is UploadState.Idle, is UploadState.Failed -> {
                    Button(onClick = { vm.submitFinalAnswers() }, modifier = Modifier.fillMaxWidth()) {
                        Text("رفع الإجابة النهائية")
                    }
                    if (uploadState is UploadState.Failed) {
                        Text(
                            "فشل الرفع: ${(uploadState as UploadState.Failed).message} - إجابتك محفوظة، أعد المحاولة",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                UploadState.Uploading -> {
                    CircularProgressIndicator()
                    Text("جارٍ الرفع...")
                }
                UploadState.Success -> {
                    Text("✅ تم رفع إجابتك بنجاح", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
