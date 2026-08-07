package com.examlan.app.network

import com.examlan.app.data.Exam
import com.examlan.app.data.SubmissionPayload
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import java.time.Duration

/**
 * خادم يعمل داخل تطبيق الأستاذ نفسه، على المنفذ 8080 مثلاً.
 * الطلاب يتصلون به عبر: http://<IP الأستاذ>:8080
 *
 * البورت الثابت + IP الأستاذ الظاهر على شاشته هو ما يدخله الطالب يدوياً.
 */
class TeacherServer(
    private val port: Int = 8080,
    private val onSubmissionReceived: suspend (SubmissionPayload) -> Unit,
    private val getCurrentExam: suspend () -> Exam?
) {
    private var server: ApplicationEngine? = null

    // حالة الطلاب المتصلين حالياً (لعرضها لحظياً في واجهة الأستاذ)
    val connectedStudents = MutableStateFlow<Set<String>>(emptySet())

    fun start() {
        server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(15)
                timeout = Duration.ofSeconds(30)
            }

            routing {
                // 1) الطالب يطلب الاختبار الحالي
                get("/exam") {
                    val exam = getCurrentExam()
                    if (exam == null) {
                        call.respond(HttpStatusCode.NotFound, "لا يوجد اختبار نشط حالياً")
                    } else {
                        call.respond(exam)
                    }
                }

                // 2) الطالب يرفع إجاباته النهائية
                post("/submit") {
                    val payload = call.receive<SubmissionPayload>()
                    onSubmissionReceived(payload)
                    connectedStudents.value = connectedStudents.value + payload.studentName
                    call.respond(HttpStatusCode.OK, mapOf("status" to "received"))
                }

                // 3) قناة حالة اتصال لحظية (اختياري لعرض من انضم فعلياً)
                webSocket("/status") {
                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val name = frame.readText()
                                connectedStudents.value = connectedStudents.value + name
                                send(Frame.Text("ok"))
                            }
                        }
                    } catch (e: Exception) {
                        // تجاهل قطع الاتصال المفاجئ - لا يوقف الخادم
                    }
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        server?.stop(1000, 2000)
    }
}
