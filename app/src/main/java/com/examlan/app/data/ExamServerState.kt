package com.examlan.app.data

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * حالة مشتركة داخل نفس عملية التطبيق بين TeacherViewModel و ExamServerService.
 * تُستخدم لتمرير الاختبار الحالي للخدمة الخلفية دون الحاجة لـ Intent extras كبيرة
 * أو ربط (bind) معقد بين المكوّنين.
 */
object ExamServerState {
    val currentExam = MutableStateFlow<Exam?>(null)
}
