package com.examlan.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * يضغط الصورة المختارة (من الكاميرا أو المعرض) ثم يحوّلها لنص Base64
 * حتى تُرسل ضمن ملف الاختبار نفسه دون الحاجة لخادم ملفات منفصل.
 */
object ImageUtils {

    private const val MAX_DIMENSION = 800

    /** ينشئ ملفاً مؤقتاً فارغاً في ذاكرة التخزين المؤقت ويرجع رابطه (Uri) لالتقاط صورة كاميرا فيه */
    fun createCameraCaptureUri(context: Context): Uri {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun compressUriToBase64(context: Context, uri: Uri, maxDimension: Int = MAX_DIMENSION, quality: Int = 75): String? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(input)
            input.close()
            if (original == null) return null

            val scaled = scaleDown(original, maxDimension)
            val output = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maxSide = maxOf(width, height)
        if (maxSide <= maxDimension) return bitmap

        val ratio = maxDimension.toFloat() / maxSide
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
