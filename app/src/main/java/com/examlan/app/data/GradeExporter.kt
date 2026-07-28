package com.examlan.app.data

import android.content.Context
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * يصدّر كشف الدرجات (كل الإجابات المصححة لاختبار معين) إلى ملف Excel (.xlsx)
 * ويحفظه في: <External Files Dir>/exports/
 *
 * يُرجع الملف الناتج حتى يتم مشاركته أو فتحه مباشرة.
 */
object GradeExporter {

    fun exportGradesToExcel(
        context: Context,
        examTitle: String,
        submissions: List<SubmissionEntity>
    ): File {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("كشف الدرجات")

        // تنسيق رأس الجدول
        val headerStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply { bold = true }
            setFont(font)
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }

        val headerRow = sheet.createRow(0)
        val headers = listOf("اسم الطالب", "الرقم الأكاديمي", "الدرجة", "وقت التسليم", "ملاحظات الأستاذ")
        headers.forEachIndexed { index, title ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        submissions.forEachIndexed { rowIndex, submission ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(submission.studentName)
            row.createCell(1).setCellValue(submission.studentId)
            row.createCell(2).setCellValue(submission.grade ?: -1.0)
            if (submission.grade == null) row.getCell(2).setCellValue("لم يُصحح بعد")
            row.createCell(3).setCellValue(dateFormat.format(Date(submission.receivedAtEpochMs)))
            row.createCell(4).setCellValue(submission.feedback ?: "")
        }

        // ضبط عرض الأعمدة تلقائياً
        for (i in headers.indices) {
            sheet.autoSizeColumn(i)
        }

        // مجلد التصدير داخل تخزين التطبيق الخاص (لا يحتاج صلاحيات تخزين إضافية)
        val exportDir = File(context.getExternalFilesDir(null), "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val safeTitle = examTitle.replace(Regex("[^A-Za-z0-9\\u0600-\\u06FF_-]"), "_")
        val outputFile = File(exportDir, "كشف_درجات_${safeTitle}.xlsx")

        FileOutputStream(outputFile).use { out ->
            workbook.write(out)
        }
        workbook.close()

        return outputFile
    }
}
