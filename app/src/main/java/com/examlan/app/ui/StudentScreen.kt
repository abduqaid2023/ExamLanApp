package com.examlan.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.examlan.app.data.AnswerItem
import com.examlan.app.data.ImageUtils
import com.examlan.app.data.QuestionType
import com.examlan.app.viewmodel.StudentViewModel
import com.examlan.app.viewmodel.UploadState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun StudentScreen() {
    val vm: StudentViewModel = viewModel()
    val exam by vm.exam.collectAsState()
    val answers by vm.answers.collectAsState()
    val uploadState by vm.uploadState.collectAsState()
    val wasAutoSubmitted by vm.wasAutoSubmitted.collectAsState()

    var ip by remember { mutableStateOf("192.168.1.5") }
    var name by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var studentClass by remember { mutableStateOf("") }

    val dateText = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // true أثناء فتح منتقي الصور - حتى لا نعتبرها "خروج للغش" ونرفع الإجابة سهواً
    var isPickerLaunching by remember { mutableStateOf(false) }
    var showStartWarning by remember { mutableStateOf(false) }

    // نحتفظ بآخر قيمة للحالات حتى تكون متاحة داخل المراقب أدناه دون الحاجة لإعادة إنشائه
    val currentExam by rememberUpdatedState(exam)
    val currentUploadState by rememberUpdatedState(uploadState)
    val currentPickerLaunching by rememberUpdatedState(isPickerLaunching)

    // مراقبة خروج الطالب من التطبيق أو تصغيره أثناء وجود اختبار نشط - رفع تلقائي فوري لمنع الغش
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (currentExam != null && !currentPickerLaunching && currentUploadState !is UploadState.Success) {
                    vm.submitFinalAnswers(isAutoSubmit = true)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // إظهار تحذير مرة واحدة فور فتح الاختبار
    LaunchedEffect(exam) {
        if (exam != null) showStartWarning = true
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("وضع الطالب", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        if (exam == null) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الطالب") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = studentClass, onValueChange = { studentClass = it }, label = { Text("الصف") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = studentId, onValueChange = { studentId = it }, label = { Text("الرقم الجامعي/الأكاديمي") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("عنوان IP الخاص بجهاز الأستاذ") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { vm.connectAndFetchExam(teacherIp = ip, port = 8080, name = name, id = studentId, studentClass = studentClass) },
                enabled = name.isNotBlank() && studentId.isNotBlank() && ip.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("الاتصال وجلب الاختبار") }
        } else {
            val locked = uploadState is UploadState.Success

            LazyColumn(Modifier.weight(1f)) {
                item {
                    ExamHeaderView(
                        header = exam!!.header,
                        examTitle = exam!!.title,
                        studentName = name,
                        studentClass = studentClass,
                        dateText = dateText,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                item {
                    Card(
                        Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            "⚠️ تنبيه: الخروج من التطبيق أو تصغيره سيؤدي لرفع إجابتك تلقائياً فوراً، ولن تتمكن من إكمال الاختبار بعدها.",
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
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
                                                enabled = !locked,
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
                                    val attachedImage = answers[q.id]?.essayImageBase64
                                    var pendingCameraUri by remember(q.id) { mutableStateOf<Uri?>(null) }

                                    val cameraLauncher = rememberLauncherForActivityResult(
                                        contract = ActivityResultContracts.TakePicture()
                                    ) { success ->
                                        isPickerLaunching = false
                                        if (success) {
                                            val uri = pendingCameraUri
                                            if (uri != null) {
                                                scope.launch {
                                                    val base64 = ImageUtils.compressUriToBase64(context, uri)
                                                    if (base64 != null) {
                                                        vm.updateAnswer(
                                                            AnswerItem(
                                                                questionId = q.id,
                                                                essayText = text,
                                                                essayImageBase64 = base64
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = text,
                                        onValueChange = {
                                            text = it
                                            vm.updateAnswer(AnswerItem(questionId = q.id, essayText = it, essayImageBase64 = attachedImage))
                                        },
                                        label = { Text("إجابتك") },
                                        enabled = !locked,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(Modifier.padding(top = 6.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        TextButton(
                                            enabled = !locked,
                                            onClick = {
                                                isPickerLaunching = true
                                                val uri = ImageUtils.createCameraCaptureUri(context)
                                                pendingCameraUri = uri
                                                cameraLauncher.launch(uri)
                                            }
                                        ) {
                                            Text("📷 التقاط صورة بالكاميرا")
                                        }
                                        if (attachedImage != null && !locked) {
                                            TextButton(onClick = {
                                                vm.updateAnswer(AnswerItem(questionId = q.id, essayText = text, essayImageBase64 = null))
                                            }) {
                                                Text("✕ إزالة الصورة", color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }

                                    if (attachedImage != null) {
                                        ImageFromBase64(attachedImage)
                                    }
                                }
                                QuestionType.TRUE_FALSE -> {
                                    val selected = answers[q.id]?.selectedOptionIndex
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Button(
                                            enabled = !locked,
                                            onClick = { vm.updateAnswer(AnswerItem(questionId = q.id, selectedOptionIndex = 0)) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (selected == 0) androidx.compose.ui.graphics.Color(0xFF3F7D58) else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (selected == 0) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        ) { Text("✓ صحيح") }
                                        Button(
                                            enabled = !locked,
                                            onClick = { vm.updateAnswer(AnswerItem(questionId = q.id, selectedOptionIndex = 1)) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (selected == 1) androidx.compose.ui.graphics.Color(0xFFA6402F) else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (selected == 1) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        ) { Text("× خطأ") }
                                    }
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
                    if (wasAutoSubmitted) {
                        Text(
                            "⚠️ تم إنهاء الاختبار ورفع إجابتك تلقائياً لأنك غادرت التطبيق",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("✅ تم رفع إجابتك بنجاح", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    if (showStartWarning && exam != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("تنبيه قبل بدء الاختبار") },
            text = {
                Text("إذا خرجت من التطبيق أو صغّرته في أي وقت أثناء الاختبار، سيتم رفع إجابتك الحالية تلقائياً فوراً ولن تتمكن من إكمال باقي الأسئلة. تأكد من إغلاق أي إشعارات أو مكالمات قبل البدء.")
            },
            confirmButton = {
                TextButton(onClick = { showStartWarning = false }) { Text("فهمت، بدء الاختبار") }
            }
        )
    }
}
