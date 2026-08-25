package com.examlan.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.examlan.app.data.AnswerItem
import com.examlan.app.data.ImageUtils
import com.examlan.app.data.Question
import kotlinx.coroutines.launch

/**
 * ورقة إجابة مطابقة لترقيم فقرات ورقة اختبار مرفوعة كصورة:
 * بطاقة السؤال الأول (صح/خطأ) - بطاقة السؤال الثاني (اختيار رقم) - بطاقة السؤال الثالث (مقالي + كاميرا)
 */
@Composable
fun StructuredAnswerSheet(
    questions: List<Question>,
    answers: Map<String, AnswerItem>,
    locked: Boolean,
    onUpdateAnswer: (AnswerItem) -> Unit,
    onPickerLaunchingChange: (Boolean) -> Unit
) {
    val tfQuestions = remember(questions) { questions.filter { it.id.startsWith("tf_") }.sortedBy { it.id.substringAfter("_").toIntOrNull() ?: 0 } }
    val mcqQuestions = remember(questions) { questions.filter { it.id.startsWith("mcq_") }.sortedBy { it.id.substringAfter("_").toIntOrNull() ?: 0 } }
    val essayQuestions = remember(questions) { questions.filter { it.id.startsWith("essay_") }.sortedBy { it.id.substringAfter("_").toIntOrNull() ?: 0 } }

    Column(Modifier.fillMaxWidth()) {
        if (tfQuestions.isNotEmpty()) {
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("السؤال الأول (صح أو خطأ)", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    tfQuestions.forEach { q ->
                        val number = q.id.substringAfter("_")
                        val selected = answers[q.id]?.selectedOptionIndex
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الفقرة $number")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    enabled = !locked,
                                    onClick = { onUpdateAnswer(AnswerItem(questionId = q.id, selectedOptionIndex = 0)) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected == 0) Color(0xFF3F7D58) else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (selected == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) { Text("✓") }
                                Button(
                                    enabled = !locked,
                                    onClick = { onUpdateAnswer(AnswerItem(questionId = q.id, selectedOptionIndex = 1)) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected == 1) Color(0xFFA6402F) else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (selected == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) { Text("×") }
                            }
                        }
                    }
                }
            }
        }

        if (mcqQuestions.isNotEmpty()) {
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("السؤال الثاني (اختيار رقم الإجابة الصحيحة)", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    mcqQuestions.forEach { q ->
                        val number = q.id.substringAfter("_")
                        val selected = answers[q.id]?.selectedOptionIndex
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الفقرة $number")
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                q.options.forEachIndexed { idx, optionLabel ->
                                    Button(
                                        enabled = !locked,
                                        onClick = { onUpdateAnswer(AnswerItem(questionId = q.id, selectedOptionIndex = idx)) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selected == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (selected == idx) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) { Text(optionLabel) }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (essayQuestions.isNotEmpty()) {
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("السؤال الثالث (إجابة مقالية)", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    essayQuestions.forEach { q ->
                        EssayRow(question = q, answer = answers[q.id], locked = locked, onUpdateAnswer = onUpdateAnswer, onPickerLaunchingChange = onPickerLaunchingChange)
                    }
                }
            }
        }
    }
}

@Composable
private fun EssayRow(
    question: Question,
    answer: AnswerItem?,
    locked: Boolean,
    onUpdateAnswer: (AnswerItem) -> Unit,
    onPickerLaunchingChange: (Boolean) -> Unit
) {
    val number = question.id.substringAfter("_")
    var text by remember(question.id) { mutableStateOf(answer?.essayText ?: "") }
    val attachedImage = answer?.essayImageBase64
    var pendingCameraUri by remember(question.id) { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        onPickerLaunchingChange(false)
        if (success) {
            val uri = pendingCameraUri
            if (uri != null) {
                scope.launch {
                    val base64 = ImageUtils.compressUriToBase64(context, uri)
                    if (base64 != null) {
                        onUpdateAnswer(AnswerItem(questionId = question.id, essayText = text, essayImageBase64 = base64))
                    }
                }
            }
        }
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text("الفقرة $number", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onUpdateAnswer(AnswerItem(questionId = question.id, essayText = it, essayImageBase64 = attachedImage))
            },
            label = { Text("إجابتك") },
            enabled = !locked,
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                enabled = !locked,
                onClick = {
                    onPickerLaunchingChange(true)
                    val uri = ImageUtils.createCameraCaptureUri(context)
                    pendingCameraUri = uri
                    cameraLauncher.launch(uri)
                }
            ) { Text("📷 التقاط صورة") }
            if (attachedImage != null && !locked) {
                TextButton(onClick = { onUpdateAnswer(AnswerItem(questionId = question.id, essayText = text, essayImageBase64 = null)) }) {
                    Text("✕ إزالة", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (attachedImage != null) {
            ImageFromBase64(attachedImage)
        }
    }
}
