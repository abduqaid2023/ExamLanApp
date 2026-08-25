package com.examlan.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.examlan.app.data.ExamHeader
import com.examlan.app.data.ExamMode
import com.examlan.app.data.ImageUtils
import com.examlan.app.data.Question
import com.examlan.app.data.QuestionType
import kotlinx.coroutines.launch

/**
 * إنشاء اختبار عن طريق رفع صورة لورقة الاختبار (مصمّمة خارجياً بالكامل من الأستاذ)
 * مع تحديد عدد فقرات كل قسم من أقسام ورقة الإجابة الثلاثة.
 */
@Composable
fun ImageBasedExamBuilderScreen(
    onCreateExam: (title: String, durationMinutes: Int, questions: List<Question>, header: ExamHeader, mode: ExamMode, paperImageBase64: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("30") }

    var headerExpanded by remember { mutableStateOf(false) }
    var countryName by remember { mutableStateOf("") }
    var ministryName by remember { mutableStateOf("") }
    var governorateName by remember { mutableStateOf("") }
    var schoolName by remember { mutableStateOf("") }
    var academicYear by remember { mutableStateOf("") }
    var subjectName by remember { mutableStateOf("") }

    var paperImageBase64 by remember { mutableStateOf<String?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }

    var tfCount by remember { mutableStateOf("") }
    var mcqCount by remember { mutableStateOf("") }
    var mcqOptionsCount by remember { mutableStateOf("3") }
    var essayCount by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploadingImage = true
            scope.launch {
                // دقة أعلى (1600px) حتى يبقى نص ورقة الاختبار واضحاً للقراءة
                paperImageBase64 = ImageUtils.compressUriToBase64(context, uri, maxDimension = 1600)
                isUploadingImage = false
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
        Text("إنشاء اختبار من صورة", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "صمّم الاختبار بأي برنامج (Word أو خط اليد) وارفعه كصورة، وحدد عدد الفقرات لتوليد ورقة إجابة مطابقة",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("عنوان/مادة الاختبار") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = duration,
            onValueChange = { duration = it.filter { c -> c.isDigit() } },
            label = { Text("مدة الاختبار بالدقائق") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { headerExpanded = !headerExpanded }) {
            Text(if (headerExpanded) "▲ إخفاء بيانات الترويسة الرسمية" else "▼ إضافة بيانات الترويسة الرسمية (اختياري)")
        }
        if (headerExpanded) {
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    OutlinedTextField(value = countryName, onValueChange = { countryName = it }, label = { Text("اسم البلد") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(value = ministryName, onValueChange = { ministryName = it }, label = { Text("الوزارة") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(value = governorateName, onValueChange = { governorateName = it }, label = { Text("المحافظة") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(value = schoolName, onValueChange = { schoolName = it }, label = { Text("المدرسة") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(value = academicYear, onValueChange = { academicYear = it }, label = { Text("العام الدراسي") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(value = subjectName, onValueChange = { subjectName = it }, label = { Text("المادة") }, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("صورة ورقة الاختبار", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))

        OutlinedButton(
            onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (paperImageBase64 == null) "📎 رفع صورة الاختبار (JPG / PNG)" else "🔄 تغيير الصورة المرفوعة")
        }

        if (isUploadingImage) {
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator()
        }
        paperImageBase64?.let { img ->
            Spacer(Modifier.height(8.dp))
            ImageFromBase64(img)
        }

        Spacer(Modifier.height(16.dp))
        Text("إعداد ورقة الإجابة", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))

        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("السؤال الأول (صح / خطأ)", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = tfCount,
                    onValueChange = { tfCount = it.filter { c -> c.isDigit() } },
                    label = { Text("عدد الفقرات") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("السؤال الثاني (اختيار من متعدد)", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = mcqCount,
                    onValueChange = { mcqCount = it.filter { c -> c.isDigit() } },
                    label = { Text("عدد الفقرات") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = mcqOptionsCount,
                    onValueChange = { mcqOptionsCount = it.filter { c -> c.isDigit() } },
                    label = { Text("عدد الخيارات لكل فقرة") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("السؤال الثالث (مقالي)", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = essayCount,
                    onValueChange = { essayCount = it.filter { c -> c.isDigit() } },
                    label = { Text("عدد الفقرات") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        val tf = tfCount.toIntOrNull() ?: 0
        val mcq = mcqCount.toIntOrNull() ?: 0
        val mcqOpts = mcqOptionsCount.toIntOrNull() ?: 0
        val essay = essayCount.toIntOrNull() ?: 0

        val canCreate = title.isNotBlank() &&
            paperImageBase64 != null &&
            (tf + mcq + essay) > 0 &&
            (mcq == 0 || mcqOpts >= 2)

        Button(
            onClick = {
                val questions = mutableListOf<Question>()
                repeat(tf) { i ->
                    questions.add(Question(id = "tf_${i + 1}", text = "السؤال الأول - الفقرة ${i + 1}", type = QuestionType.TRUE_FALSE))
                }
                repeat(mcq) { i ->
                    questions.add(
                        Question(
                            id = "mcq_${i + 1}",
                            text = "السؤال الثاني - الفقرة ${i + 1}",
                            type = QuestionType.MULTIPLE_CHOICE,
                            options = (1..mcqOpts).map { it.toString() }
                        )
                    )
                }
                repeat(essay) { i ->
                    questions.add(Question(id = "essay_${i + 1}", text = "السؤال الثالث - الفقرة ${i + 1}", type = QuestionType.ESSAY))
                }

                onCreateExam(
                    title.trim(),
                    duration.toIntOrNull() ?: 30,
                    questions,
                    ExamHeader(
                        countryName = countryName.trim(),
                        ministryName = ministryName.trim(),
                        governorateName = governorateName.trim(),
                        schoolName = schoolName.trim(),
                        academicYear = academicYear.trim(),
                        subjectName = subjectName.trim()
                    ),
                    ExamMode.IMAGE_BASED,
                    paperImageBase64
                )
            },
            enabled = canCreate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("إنشاء الاختبار وبدء استقبال الطلاب")
        }

        if (!canCreate) {
            Text(
                "تأكد من: كتابة العنوان، رفع صورة الاختبار، وإدخال عدد فقرات لقسم واحد على الأقل (وعدد خيارات صحيح للسؤال الثاني إن وُجد)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
