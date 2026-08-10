package com.examlan.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examlan.app.data.ImageUtils
import com.examlan.app.data.Question
import com.examlan.app.data.QuestionResource
import com.examlan.app.data.QuestionType
import java.util.UUID
import kotlinx.coroutines.launch

/** أي حقل نص هو المستهدف الآن لإدراج معادلة أو صورة فيه */
private sealed class ActiveTarget {
    data class QuestionField(val questionId: String) : ActiveTarget()
    data class OptionField(val questionId: String, val optionIndex: Int) : ActiveTarget()
}

/** حالة خيار مؤقت أثناء تحرير سؤال اختيار من متعدد */
private class DraftOption(initial: String = "") {
    var value by mutableStateOf(TextFieldValue(initial))
}

/** حالة سؤال مؤقت أثناء البناء، قبل تحويله لـ Question نهائي */
private class DraftQuestion(
    val id: String = UUID.randomUUID().toString(),
    initialType: QuestionType = QuestionType.MULTIPLE_CHOICE
) {
    var type by mutableStateOf(initialType)
    var textValue by mutableStateOf(TextFieldValue(""))
    val options = mutableStateListOf(DraftOption(), DraftOption())
    var points by mutableStateOf("1")
    val resources = mutableStateMapOf<String, QuestionResource>()
    var tokenCounter = 0
}

@Composable
fun ExamBuilderScreen(
    onCreateExam: (title: String, durationMinutes: Int, questions: List<Question>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("30") }
    val draftQuestions = remember { mutableStateListOf<DraftQuestion>() }
    var activeTarget by remember { mutableStateOf<ActiveTarget?>(null) }

    var showEquationDialog by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun findQuestion(id: String) = draftQuestions.find { it.id == id }

    fun insertResource(id: String, resource: QuestionResource) {
        val token = "{{$id}}"
        when (val target = activeTarget) {
            is ActiveTarget.QuestionField -> {
                val q = findQuestion(target.questionId) ?: return
                q.resources[id] = resource
                val tfv = q.textValue
                val newText = tfv.text.substring(0, tfv.selection.start) + " $token " + tfv.text.substring(tfv.selection.end)
                val newPos = tfv.selection.start + token.length + 2
                q.textValue = TextFieldValue(newText, TextRange(newPos))
            }
            is ActiveTarget.OptionField -> {
                val q = findQuestion(target.questionId) ?: return
                q.resources[id] = resource
                val opt = q.options.getOrNull(target.optionIndex) ?: return
                val tfv = opt.value
                val newText = tfv.text.substring(0, tfv.selection.start) + " $token " + tfv.text.substring(tfv.selection.end)
                val newPos = tfv.selection.start + token.length + 2
                opt.value = TextFieldValue(newText, TextRange(newPos))
            }
            null -> {}
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val target = activeTarget
            if (target != null) {
                val questionId = when (target) {
                    is ActiveTarget.QuestionField -> target.questionId
                    is ActiveTarget.OptionField -> target.questionId
                }
                scope.launch {
                    val base64 = ImageUtils.compressUriToBase64(context, uri)
                    if (base64 != null) {
                        val q = findQuestion(questionId)
                        if (q != null) {
                            q.tokenCounter++
                            val id = "IMG${q.tokenCounter}"
                            insertResource(id, QuestionResource(type = "img", value = base64))
                        }
                    }
                }
            }
        }
        showImageDialog = false
    }

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
                    onDelete = { draftQuestions.remove(q) },
                    onFocusQuestionField = { activeTarget = ActiveTarget.QuestionField(q.id) },
                    onFocusOptionField = { optIndex -> activeTarget = ActiveTarget.OptionField(q.id, optIndex) },
                    onOpenEquation = {
                        activeTarget = ActiveTarget.QuestionField(q.id)
                        showEquationDialog = true
                    },
                    onOpenEquationForOption = { optIndex ->
                        activeTarget = ActiveTarget.OptionField(q.id, optIndex)
                        showEquationDialog = true
                    },
                    onOpenImage = {
                        activeTarget = ActiveTarget.QuestionField(q.id)
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onOpenImageForOption = { optIndex ->
                        activeTarget = ActiveTarget.OptionField(q.id, optIndex)
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { draftQuestions.add(DraftQuestion(initialType = QuestionType.MULTIPLE_CHOICE)) },
                modifier = Modifier.weight(1f)
            ) { Text("+ اختيار من متعدد") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { draftQuestions.add(DraftQuestion(initialType = QuestionType.ESSAY)) },
                modifier = Modifier.weight(1f)
            ) { Text("+ سؤال مقالي") }
        }

        Spacer(Modifier.height(12.dp))

        val canCreate = title.isNotBlank() && draftQuestions.isNotEmpty() &&
            draftQuestions.all { it.textValue.text.isNotBlank() && (it.type == QuestionType.ESSAY || it.options.count { o -> o.value.text.isNotBlank() } >= 2) }

        Button(
            onClick = {
                val finalQuestions = draftQuestions.mapIndexed { idx, d ->
                    Question(
                        id = "q${idx + 1}",
                        text = d.textValue.text.trim(),
                        type = d.type,
                        options = if (d.type == QuestionType.MULTIPLE_CHOICE) d.options.map { it.value.text }.filter { it.isNotBlank() } else emptyList(),
                        points = d.points.toDoubleOrNull() ?: 1.0,
                        resources = d.resources.toMap()
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

    if (showEquationDialog) {
        EquationInsertDialog(
            onDismiss = { showEquationDialog = false },
            onInsert = { latex ->
                val target = activeTarget
                val questionId = when (target) {
                    is ActiveTarget.QuestionField -> target.questionId
                    is ActiveTarget.OptionField -> target.questionId
                    null -> null
                }
                val q = questionId?.let { findQuestion(it) }
                if (q != null) {
                    q.tokenCounter++
                    val id = "EQ${q.tokenCounter}"
                    insertResource(id, QuestionResource(type = "eq", value = latex))
                }
                showEquationDialog = false
            }
        )
    }
}

@Composable
private fun EquationInsertDialog(
    onDismiss: () -> Unit,
    onInsert: (String) -> Unit
) {
    var latex by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة معادلة رياضية") },
        text = {
            Column {
                OutlinedTextField(
                    value = latex,
                    onValueChange = { latex = it },
                    label = { Text("صيغة LaTeX") },
                    placeholder = { Text("مثال: x = \\frac{-b \\pm \\sqrt{b^2-4ac}}{2a}") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("المعاينة:", style = MaterialTheme.typography.labelMedium)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .heightIn(min = 48.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    if (latex.isNotBlank()) {
                        EquationView(latex)
                    } else {
                        Text("اكتب صيغة لرؤية المعاينة", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (latex.isNotBlank()) onInsert(latex) }) { Text("إدراج") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun QuestionEditorCard(
    index: Int,
    question: DraftQuestion,
    onDelete: () -> Unit,
    onFocusQuestionField: () -> Unit,
    onFocusOptionField: (Int) -> Unit,
    onOpenEquation: () -> Unit,
    onOpenEquationForOption: (Int) -> Unit,
    onOpenImage: () -> Unit,
    onOpenImageForOption: (Int) -> Unit
) {
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
                value = question.textValue,
                onValueChange = { question.textValue = it },
                label = { Text("نص السؤال") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusEvent { if (it.isFocused) onFocusQuestionField() }
            )

            Row(Modifier.padding(top = 6.dp)) {
                TextButton(onClick = { onFocusQuestionField(); onOpenEquation() }) {
                    Text("∑ إضافة معادلة")
                }
                TextButton(onClick = { onFocusQuestionField(); onOpenImage() }) {
                    Text("🖼 إضافة صورة")
                }
            }

            // معاينة حية لنص السؤال
            if (question.textValue.text.isNotBlank()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    RichContent(text = question.textValue.text, resources = question.resources, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(6.dp))

            if (question.type == QuestionType.MULTIPLE_CHOICE) {
                question.options.forEachIndexed { optIndex, opt ->
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = opt.value,
                                onValueChange = { opt.value = it },
                                label = { Text("خيار ${optIndex + 1}") },
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusEvent { if (it.isFocused) onFocusOptionField(optIndex) }
                            )
                            if (question.options.size > 2) {
                                IconButton(onClick = { question.options.removeAt(optIndex) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف الخيار")
                                }
                            }
                        }
                        Row {
                            TextButton(onClick = { onFocusOptionField(optIndex); onOpenEquationForOption(optIndex) }) {
                                Text("∑ معادلة", style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(onClick = { onFocusOptionField(optIndex); onOpenImageForOption(optIndex) }) {
                                Text("🖼 صورة", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        if (opt.value.text.isNotBlank()) {
                            RichContent(text = opt.value.text, resources = question.resources, fontSize = 14.sp)
                        }
                    }
                }
                TextButton(onClick = { question.options.add(DraftOption()) }) {
                    Text("+ إضافة خيار")
                }
            }

            OutlinedTextField(
                value = question.points,
                onValueChange = { question.points = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("الدرجة المخصصة لهذا السؤال") },
                modifier = Modifier.fillMaxWidth(0.5f)
            )
        }
    }
}
