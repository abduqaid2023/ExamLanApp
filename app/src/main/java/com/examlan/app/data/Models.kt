package com.examlan.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * نوع السؤال: اختيار من متعدد أو مقالي
 */
enum class QuestionType { MULTIPLE_CHOICE, ESSAY }

/**
 * سؤال واحد داخل الاختبار
 */
@Serializable
data class Question(
    val id: String,
    val text: String,
    val type: QuestionType,
    val options: List<String> = emptyList(), // فقط لأسئلة الاختيار من متعدد
    val correctOptionIndex: Int? = null,      // للتصحيح التلقائي إن رغبت لاحقاً
    val points: Double = 1.0
)

/**
 * الاختبار الكامل - يُنشئه الأستاذ ويُرسل نسخة منه للطالب عند الاتصال
 */
@Serializable
data class Exam(
    val id: String,
    val title: String,
    val durationMinutes: Int,
    val questions: List<Question>
)

/**
 * إجابة الطالب على سؤال واحد
 */
@Serializable
data class AnswerItem(
    val questionId: String,
    val selectedOptionIndex: Int? = null, // لأسئلة الاختيار من متعدد
    val essayText: String? = null          // للأسئلة المقالية
)

/**
 * حزمة الإجابات الكاملة التي يرفعها الطالب لجهاز الأستاذ
 */
@Serializable
data class SubmissionPayload(
    val examId: String,
    val studentName: String,
    val studentId: String,
    val answers: List<AnswerItem>,
    val submittedAtEpochMs: Long
)

/**
 * --- كيانات Room (الحفظ الدائم) ---
 */

// عند جهاز الطالب: نسخة محلية من الاختبار + الإجابات (تُحفظ فور الكتابة، قبل الرفع)
@Entity(tableName = "local_exam_progress")
data class LocalExamProgress(
    @PrimaryKey val examId: String,
    val studentName: String,
    val studentId: String,
    val examJson: String,        // الاختبار نفسه مخزن كـ JSON
    val answersJson: String,     // آخر نسخة من إجابات الطالب (JSON) - تُحدّث تلقائياً
    val isUploaded: Boolean = false,
    val lastSavedEpochMs: Long
)

// عند جهاز الأستاذ: كل اختبار يُنشئه
@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey val id: String,
    val title: String,
    val examJson: String,
    val createdAtEpochMs: Long
)

// عند جهاز الأستاذ: كل إجابة تصل من طالب
@Entity(tableName = "submissions")
data class SubmissionEntity(
    @PrimaryKey(autoGenerate = true) val autoId: Long = 0,
    val examId: String,
    val studentId: String,
    val studentName: String,
    val answersJson: String,
    val receivedAtEpochMs: Long,
    val grade: Double? = null,       // الدرجة (تُملأ عند التصحيح)
    val isGraded: Boolean = false,
    val feedback: String? = null
)
