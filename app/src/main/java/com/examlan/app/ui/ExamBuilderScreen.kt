package com.examlan.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.examlan.app.data.Question
import com.examlan.app.data.QuestionType
import java.util.UUID

/**
 * نموذج مؤقت (قابل للتعديل) لسؤال أثناء بناء الاختبار، قبل تحويله لـ Question نهائي.
 */
private data class DraftQuestion(
    val id: String = UUID.randomUUID().toString(),
    var text: String = "",
    var type: QuestionType = QuestionType.MULTIPLE_CHOICE,
    var options: MutableList<String> = mutableStateListOf("", ""),
    var points: String = "1"
)

@Composable
fun ExamBuilderScreen(
    onCreateExam: (title: String, durationMinutes: Int, questions: List<Question>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("30") }
    val draftQuestions = remember { mutableStateListOf<DraftQuestion>() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("إنشاء اختبار جديد", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("عنوان/مادة الاختبار (مثال: English - Unit 3)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = duration,
            onValueChange = { duration = it.filter { c -> c.isDigit() } },
            label = { Text("مدة الاختبار بالدقائق") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Text("الأسئلة (${draftQuestions.size})", style = MaterialTheme.typography.titleMedium)

        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(draftQuestions) { index, q ->
                QuestionEditorCard(
                    index = index,
                    question = q,
                    onDelete = { draftQuestions.remove(q) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    draftQuestions.add(DraftQuestion(type = QuestionType.MULTIPLE_CHOICE))
                },
                modifier = Modifier.weight(1f)
            ) { Text("+ اختيار من متعدد") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = {
                    draftQuestions.add(DraftQuestion(type = QuestionType.ESSAY))
                },
                modifier = Modifier.weight(1f)
            ) { Text("+ سؤال مقالي") }
        }

        Spacer(Modifier.height(12.dp))

        val canCreate = title.isNotBlank() && draftQuestions.isNotEmpty() &&
            draftQuestions.all { it.text.isNotBlank() && (it.type == QuestionType.ESSAY || it.options.count { o -> o.isNotBlank() } >= 2) }

        Button(
            onClick = {
                val finalQuestions = draftQuestions.mapIndexed { idx, d ->
                    Question(
                        id = "q${idx + 1}",
                        text = d.text.trim(),
                        type = d.type,
                        options = if (d.type == QuestionType.MULTIPLE_CHOICE) d.options.filter { it.isNotBlank() } else emptyList(),
                        points = d.points.toDoubleOrNull() ?: 1.0
                    )
                }
                onCreateExam(title.trim(), duration.toIntOrNull() ?: 30, finalQuestions)
            },
            enabled = canCreate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("إنشاء الاختبار وبدء استقبال الطلاب")
        }

        if (!canCreate && draftQuestions.isNotEmpty()) {
            Text(
                "تأكد من كتابة نص كل سؤال، ولأسئلة الاختيار من متعدد أضف خيارين على الأقل",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun QuestionEditorCard(
    index: Int,
    question: DraftQuestion,
    onDelete: () -> Unit
) {
    var text by remember { mutableStateOf(question.text) }
    var points by remember { mutableStateOf(question.points) }

    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "السؤال ${index + 1} - ${if (question.type == QuestionType.MULTIPLE_CHOICE) "اختيار من متعدد" else "مقالي"}",
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف السؤال")
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it; question.text = it },
                label = { Text("نص السؤال") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            if (question.type == QuestionType.MULTIPLE_CHOICE) {
                question.options.forEachIndexed { optIndex, _ ->
                    var optionText by remember { mutableStateOf(question.options[optIndex]) }
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = optionText,
                            onValueChange = {
                                optionText = it
                                question.options[optIndex] = it
                            },
                            label = { Text("خيار ${optIndex + 1}") },
                            modifier = Modifier.weight(1f)
                        )
                        if (question.options.size > 2) {
                            IconButton(onClick = { question.options.removeAt(optIndex) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف الخيار")
                            }
                        }
                    }
                }
                TextButton(onClick = { question.options.add("") }) {
                    Text("+ إضافة خيار")
                }
            }

            OutlinedTextField(
                value = points,
                onValueChange = { points = it.filter { c -> c.isDigit() || c == '.' }; question.points = points },
                label = { Text("الدرجة المخصصة لهذا السؤال") },
                modifier = Modifier.fillMaxWidth(0.5f)
            )
        }
    }
}
