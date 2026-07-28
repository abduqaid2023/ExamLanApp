package com.examlan.app.network

import com.examlan.app.data.Exam
import com.examlan.app.data.SubmissionPayload
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * عميل يعمل داخل تطبيق الطالب، يتصل بجهاز الأستاذ عبر IP الذي يدخله الطالب يدوياً.
 * مثال العنوان الكامل: http://192.168.1.5:8080
 */
class StudentClient(private val teacherBaseUrl: String) {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        // مهلة معقولة حتى لا يتجمد التطبيق لو انقطعت الشبكة لحظياً
        engine {
            requestTimeout = 10_000
        }
    }

    /** جلب الاختبار الحالي من جهاز الأستاذ */
    suspend fun fetchExam(): Result<Exam> {
        return try {
            val exam: Exam = client.get("$teacherBaseUrl/exam").body()
            Result.success(exam)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** رفع إجابات الطالب النهائية - يُعيد نجاح/فشل بدون تعليق التطبيق */
    suspend fun submitAnswers(payload: SubmissionPayload): Result<Unit> {
        return try {
            client.post("$teacherBaseUrl/submit") {
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(payload)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() {
        client.close()
    }
}
