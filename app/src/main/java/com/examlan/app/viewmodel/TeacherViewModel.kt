package com.examlan.app.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.examlan.app.data.*
import com.examlan.app.data.db.AppDatabase
import com.examlan.app.network.ExamServerService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID

class TeacherViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val json = Json { ignoreUnknownKeys = true }

    private val _currentExam = MutableStateFlow<Exam?>(null)
    val currentExam: StateFlow<Exam?> = _currentExam

    val allExams = db.teacherDao().getAllExams()

    init {
        // استعادة آخر اختبار نشط تلقائياً - حتى لو أعاد أندرويد إنشاء الشاشة بعد الخروج من التطبيق
        viewModelScope.launch {
            val latest = db.teacherDao().getLatestExam()
            if (latest != null) {
                val restoredExam = json.decodeFromString(Exam.serializer(), latest.examJson)
                _currentExam.value = restoredExam
                ExamServerState.currentExam.value = restoredExam
            }
        }
    }

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning

    val submissionsForCurrentExam: StateFlow<List<SubmissionEntity>> =
        _currentExam.flatMapLatest { exam ->
            if (exam == null) flowOf(emptyList())
            else db.teacherDao().getSubmissionsForExam(exam.id)
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptyList())

    /** إنشاء اختبار جديد وحفظه بشكل دائم فوراً */
    fun createExam(title: String, durationMinutes: Int, questions: List<Question>, header: ExamHeader = ExamHeader()) {
        val exam = Exam(id = UUID.randomUUID().toString(), title = title, durationMinutes = durationMinutes, questions = questions, header = header)
        viewModelScope.launch {
            db.teacherDao().insertExam(
                ExamEntity(id = exam.id, title = exam.title, examJson = json.encodeToString(Exam.serializer(), exam), createdAtEpochMs = System.currentTimeMillis())
            )
            _currentExam.value = exam
            // مشاركة الاختبار مع الخدمة الخلفية حتى تقدر تخدمه للطلاب
            ExamServerState.currentExam.value = exam
        }
    }

    /** تشغيل الخادم كخدمة Foreground حتى يستمر يعمل حتى لو خرج الأستاذ من التطبيق */
    fun startServer() {
        val context = getApplication<Application>()
        val intent = Intent(context, ExamServerService::class.java)
        ContextCompat.startForegroundService(context, intent)
        _isServerRunning.value = true
    }

    fun stopServer() {
        val context = getApplication<Application>()
        val intent = Intent(context, ExamServerService::class.java).apply {
            action = ExamServerService.ACTION_STOP
        }
        context.startService(intent)
        _isServerRunning.value = false
    }

    /** وضع الدرجة لطالب معين وحفظها في كشف الدرجات بشكل دائم */
    fun gradeSubmission(autoId: Long, grade: Double, feedback: String? = null) {
        viewModelScope.launch {
            db.teacherDao().gradeSubmission(autoId, grade, feedback)
        }
    }

    /** الرجوع لشاشة إنشاء اختبار جديد (لا يحذف الاختبار القديم ولا إجاباته من قاعدة البيانات) */
    fun startNewExam() {
        _currentExam.value = null
    }
}
