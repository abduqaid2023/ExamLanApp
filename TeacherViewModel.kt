package com.examlan.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.examlan.app.data.*
import com.examlan.app.data.db.AppDatabase
import com.examlan.app.network.TeacherServer
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

    private var server: TeacherServer? = null

    val submissionsForCurrentExam: StateFlow<List<SubmissionEntity>> =
        _currentExam.flatMapLatest { exam ->
            if (exam == null) flowOf(emptyList())
            else db.teacherDao().getSubmissionsForExam(exam.id)
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptyList())

    val connectedStudentNames: StateFlow<Set<String>>
        get() = server?.connectedStudents ?: MutableStateFlow(emptySet())

    /** إنشاء اختبار جديد وحفظه بشكل دائم فوراً */
    fun createExam(title: String, durationMinutes: Int, questions: List<Question>) {
        val exam = Exam(id = UUID.randomUUID().toString(), title = title, durationMinutes = durationMinutes, questions = questions)
        viewModelScope.launch {
            db.teacherDao().insertExam(
                ExamEntity(id = exam.id, title = exam.title, examJson = json.encodeToString(Exam.serializer(), exam), createdAtEpochMs = System.currentTimeMillis())
            )
            _currentExam.value = exam
        }
    }

    /** تشغيل الخادم المحلي حتى يتمكن الطلاب من الاتصال */
    fun startServer(port: Int = 8080) {
        if (server != null) return
        server = TeacherServer(
            port = port,
            getCurrentExam = { _currentExam.value },
            onSubmissionReceived = { payload -> handleIncomingSubmission(payload) }
        )
        server?.start()
    }

    fun stopServer() {
        server?.stop()
        server = null
    }

    private suspend fun handleIncomingSubmission(payload: SubmissionPayload) {
        // الحفظ الدائم فور الاستلام - قبل أي شيء آخر
        db.teacherDao().insertSubmission(
            SubmissionEntity(
                examId = payload.examId,
                studentId = payload.studentId,
                studentName = payload.studentName,
                answersJson = json.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(AnswerItem.serializer()),
                    payload.answers
                ),
                receivedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    /** وضع الدرجة لطالب معين وحفظها في كشف الدرجات بشكل دائم */
    fun gradeSubmission(autoId: Long, grade: Double, feedback: String? = null) {
        viewModelScope.launch {
            db.teacherDao().gradeSubmission(autoId, grade, feedback)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopServer()
    }
}
