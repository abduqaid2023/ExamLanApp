package com.examlan.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.examlan.app.data.ExamHeader

/**
 * يعرض ترويسة الاختبار الرسمية بنفس شكل ورقة الاختبار التقليدية:
 * خانة بيانات الطالب - خانة بيانات الاختبار - خانة بيانات المدرسة/البلد
 */
@Composable
fun ExamHeaderView(
    header: ExamHeader,
    examTitle: String,
    studentName: String,
    studentClass: String,
    dateText: String,
    modifier: Modifier = Modifier
) {
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(border),
    ) {
        // خانة بيانات الطالب
        Column(
            Modifier
                .weight(1f)
                .border(border)
                .padding(10.dp)
        ) {
            HeaderLine("الاسم", studentName)
            HeaderLine("الصف", studentClass)
            HeaderLine("التاريخ", dateText)
        }

        // خانة بيانات الاختبار
        Column(
            Modifier
                .weight(1f)
                .border(border)
                .padding(10.dp)
        ) {
            HeaderLine("اختبار", examTitle)
            HeaderLine("العام الدراسي", header.academicYear)
            HeaderLine("المادة", header.subjectName)
        }

        // خانة بيانات المدرسة والبلد
        Column(
            Modifier
                .weight(1f)
                .border(border)
                .padding(10.dp)
        ) {
            HeaderLine("اسم البلد", header.countryName)
            HeaderLine("وزارة", header.ministryName)
            HeaderLine("محافظة", header.governorateName)
            HeaderLine("مدرسة", header.schoolName)
        }
    }
}

@Composable
private fun HeaderLine(label: String, value: String) {
    Text(
        text = if (value.isNotBlank()) "$label: $value" else "$label: ....................",
        style = MaterialTheme.typography.bodySmall
    )
}
