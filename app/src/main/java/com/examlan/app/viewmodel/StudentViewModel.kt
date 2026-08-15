package com.examlan.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.examlan.app.data.*
import com.examlan.app.data.db.AppDatabase
import com.examlan.app.network.StudentClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

sealed class UploadState {
    object Idle : UploadState()
    object Uploading : UploadState()
    object Success : UploadState()
    data class Failed(val message: String) : UploadState()
}

class StudentViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val json = Json { ignoreUnknownKeys = true }
    private var client: StudentClient? = null

    private val _exam = MutableStateFlow<Exam?>(null)
    val exam: StateFlow<Exam?> = _exam

    // إجابات الطالب الحالية - محفوظة تلقائياً في كل تعديل
    private val _answers = MutableStateFlow<Map<String, AnswerItem>>(emptyMap())
    val answers: StateFlow<Map<String, AnswerItem>> = _answers

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState

    private var studentId: String = ""
    private var studentName: String = ""
    private var studentClass: String = ""

    /** الاتصال بجهاز الأستاذ وجلب الاختبار */
    fun connectAndFetchExam(teacherIp: String, port: Int, name: String, id: String, studentClass: String = "") {
        studentName = name
        studentId = id
        this.studentClass = studentClass
        client = StudentClient("http://$teacherIp:$port")

        viewModelScope.launch {
            val result = client?.fetchExam()
            result?.onSuccess { fetchedExam ->
                _exam.value = fetchedExam
                // محاولة استرجاع تقدم سابق محفوظ (لو التطبيق أُغلق سابقاً)
                val saved = db.studentDao().getProgress(fetchedExam.id)
                if (saved != null && !saved.isUploaded) {
                    val savedAnswers = json.decodeFromString(
                        ListSerializer(AnswerItem.serializer()), saved.answersJson
                    ).associateBy { it.questionId }
                    _answers.value = savedAnswers
                } else {
                    // حفظ أولي فور جلب الاختبار
                    saveProgressLocally(fetchedExam)
                }
            }
        }
    }

    /** تحديث إجابة سؤال - يُحفظ فوراً بشكل دائم (لا ننتظر زر الرفع) */
    fun updateAnswer(item: AnswerItem) {
        _answers.value = _answers.value + (item.questionId to item)
        _exam.value?.let { saveProgressLocally(it) }
    }

    private fun saveProgressLocally(exam: Exam) {
        viewModelScope.launch {
            db.studentDao().saveProgress(
                LocalExamProgress(
                    examId = exam.id,
                    studentName = studentName,
                    studentId = studentId,
                    examJson = json.encodeToString(Exam.serializer(), exam),
                    answersJson = json.encodeToString(
                        ListSerializer(AnswerItem.serializer()), _answers.value.values.toList()
                    ),
                    lastSavedEpochMs = System.currentTimeMillis()
                )
            )
        }
    }

    /** رفع الإجابات النهائية - يمكن إعادة المحاولة عند الفشل دون فقدان أي شيء */
    fun submitFinalAnswers() {
        val currentExam = _exam.value ?: return
        _uploadState.value = UploadState.Uploading
        viewModelScope.launch {
            val payload = SubmissionPayload(
                examId = currentExam.id,
                studentName = studentName,
                studentId = studentId,
                studentClass = studentClass,
                answers = _answers.value.values.toList(),
                submittedAtEpochMs = System.currentTimeMillis()
            )
            val result = client?.submitAnswers(payload)
            result?.onSuccess {
                db.studentDao().markUploaded(currentExam.id)
                _uploadState.value = UploadState.Success
            }?.onFailure { e ->
                // الإجابة تبقى محفوظة محلياً - الطالب يقدر يعيد المحاولة بدون خسارة شيء
                _uploadState.value = UploadState.Failed(e.message ?: "فشل الاتصال، حاول مرة أخرى")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        client?.close()
    }
}
