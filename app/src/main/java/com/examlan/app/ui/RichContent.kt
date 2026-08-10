package com.examlan.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.examlan.app.data.QuestionResource
import ru.noties.jlatexmath.JLatexMathDrawable

private val TOKEN_REGEX = Regex("\\{\\{(EQ|IMG)(\\d+)\\}\\}")

/**
 * يعرض نصاً قد يحتوي على رموز {{EQ1}} أو {{IMG1}} فيستبدلها بمعادلة أو صورة فعلية.
 * تُستخدم هذي الدالة في كل من معاينة الأستاذ وشاشة الطالب حتى يتطابق العرض تماماً.
 */
@Composable
fun RichContent(
    text: String,
    resources: Map<String, QuestionResource>,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 17.sp,
    bold: Boolean = false
) {
    val matches = remember(text) { TOKEN_REGEX.findAll(text).toList() }

    if (matches.isEmpty()) {
        Text(text, fontSize = fontSize, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, modifier = modifier)
        return
    }

    FlowRow(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        var lastIndex = 0
        matches.forEach { match ->
            if (match.range.first > lastIndex) {
                val chunk = text.substring(lastIndex, match.range.first)
                if (chunk.isNotBlank()) {
                    Text(chunk.trim(), fontSize = fontSize, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
                }
            }
            val id = match.groupValues[1] + match.groupValues[2]
            val resource = resources[id]
            when (resource?.type) {
                "eq" -> EquationView(resource.value, fontSize)
                "img" -> ImageFromBase64(resource.value)
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < text.length) {
            val chunk = text.substring(lastIndex)
            if (chunk.isNotBlank()) {
                Text(chunk.trim(), fontSize = fontSize, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

/** يعرض معادلة رياضية مكتوبة بصيغة LaTeX كصورة ثابتة الشكل على كل الأجهزة */
@Composable
fun EquationView(latex: String, fontSize: TextUnit = 17.sp) {
    AndroidView(
        factory = { ctx -> ImageView(ctx) },
        update = { imageView ->
            try {
                val drawable = JLatexMathDrawable.builder(latex)
                    .textSize(fontSize.value * imageView.resources.displayMetrics.scaledDensity)
                    .padding(6)
                    .align(JLatexMathDrawable.ALIGN_CENTER)
                    .build()
                imageView.setImageDrawable(drawable)
            } catch (e: Exception) {
                imageView.setImageDrawable(null)
            }
        },
        modifier = Modifier.padding(horizontal = 2.dp)
    )
}

/** يعرض صورة مخزّنة كنص Base64 */
@Composable
fun ImageFromBase64(base64: String) {
    val bitmap: Bitmap? = remember(base64) {
        try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .heightIn(max = 200.dp)
                .padding(vertical = 4.dp)
        )
    }
}
