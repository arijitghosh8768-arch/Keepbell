package com.example.notificationvault.ui.common

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Renders the application icon if available, or a graceful monogram letter fallback.
 */
@Composable
fun AppIcon(
    icon: Drawable?,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val bitmap = remember(icon) {
        icon?.let { drawableToBitmap(it) }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "$label icon",
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
        )
    } else {
        // Fallback letter avatar
        val firstChar = remember(label) {
            label.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        }
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = firstChar,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = (size.value * 0.45f).sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap? {
    return try {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    } catch (e: Exception) {
        null
    }
}
