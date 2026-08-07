package com.examlan.app.data

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * يصدّر كشف الدرجات إلى ملف CSV (يفتح مباشرة في Excel أو Google Sheets).
 * تم تفضيل CSV بدلاً من Apache POI لأن مكتبة POI تعتمد على java.awt
 * غير المتوفرة في أندرويد وتسبب كراش فوري عند الاستخدام.
 */
object GradeExporter {

    fun exportGradesToExcel(
        context: Context,
        examTitle: String,
        submissions: List<SubmissionEntity>
    ): File {
        val exportDir = File(context.getExternalFilesDir(null), "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val safeTitle = examTitle.replace(Regex("[^A-Za-z0-9\\u0600-\\u06FF_-]"), "_")
        val outputFile = File(exportDir, "كشف_درجات_${safeTitle}.csv")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        FileOutputStream(outputFile).use { out ->
            // BOM حتى يعرض Excel النص العربي بشكل صحيح
            out.write(0xEF); out.write(0xBB); out.write(0xBF)

            val header = listOf("اسم الطالب", "الرقم الأكاديمي", "الدرجة", "وقت التسليم", "ملاحظات الأستاذ")
            out.write((header.joinToString(",") { csvEscape(it) } + "\n").toByteArray(Charsets.UTF_8))

            submissions.forEach { sub ->
                val row = listOf(
                    sub.studentName,
                    sub.studentId,
                    sub.grade?.toString() ?: "لم يُصحح بعد",
                    dateFormat.format(Date(sub.receivedAtEpochMs)),
                    sub.feedback ?: ""
                )
                out.write((row.joinToString(",") { csvEscape(it) } + "\n").toByteArray(Charsets.UTF_8))
            }
        }

        return outputFile
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
