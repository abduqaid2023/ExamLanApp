package com.examlan.app.data.db

import android.content.Context
import androidx.room.*
import com.examlan.app.data.ExamEntity
import com.examlan.app.data.LocalExamProgress
import com.examlan.app.data.SubmissionEntity
import kotlinx.coroutines.flow.Flow

// ================== DAO الخاص بجهاز الأستاذ ==================
@Dao
interface TeacherDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity)

    @Query("SELECT * FROM exams ORDER BY createdAtEpochMs DESC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams ORDER BY createdAtEpochMs DESC LIMIT 1")
    suspend fun getLatestExam(): ExamEntity?

    @Query("SELECT * FROM exams WHERE id = :examId LIMIT 1")
    suspend fun getExamById(examId: String): ExamEntity?

    // إدراج إجابة جديدة، أو تحديثها إن أعاد الطالب الرفع (نفس الطالب+نفس الاختبار)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmission(submission: SubmissionEntity): Long

    @Query("SELECT * FROM submissions WHERE examId = :examId ORDER BY receivedAtEpochMs DESC")
    fun getSubmissionsForExam(examId: String): Flow<List<SubmissionEntity>>

    @Query("UPDATE submissions SET grade = :grade, isGraded = 1, feedback = :feedback WHERE autoId = :autoId")
    suspend fun gradeSubmission(autoId: Long, grade: Double, feedback: String?)

    @Query("SELECT * FROM submissions WHERE isGraded = 1")
    fun getAllGradedSubmissions(): Flow<List<SubmissionEntity>>
}

// ================== DAO الخاص بجهاز الطالب ==================
@Dao
interface StudentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: LocalExamProgress)

    @Query("SELECT * FROM local_exam_progress WHERE examId = :examId LIMIT 1")
    suspend fun getProgress(examId: String): LocalExamProgress?

    @Query("UPDATE local_exam_progress SET isUploaded = 1 WHERE examId = :examId")
    suspend fun markUploaded(examId: String)

    @Query("SELECT * FROM local_exam_progress WHERE isUploaded = 0")
    suspend fun getPendingUploads(): List<LocalExamProgress>
}

@Database(
    entities = [ExamEntity::class, SubmissionEntity::class, LocalExamProgress::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun teacherDao(): TeacherDao
    abstract fun studentDao(): StudentDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "exam_lan_db"
                )
                    // ترقية لمرة واحدة بسبب إضافة عمود "الصف" - بيانات الاختبارات التجريبية
                    // القديمة فقط تُمسح، ولا يؤثر هذا على أي بيانات تُجمع بعد هذا التحديث
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
