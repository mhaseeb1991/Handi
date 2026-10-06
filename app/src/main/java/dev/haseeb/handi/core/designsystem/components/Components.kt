package dev.haseeb.handi.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import kotlin.math.abs

/** Small uppercase label that sits above headlines, cookbook style. */
@Composable
fun Kicker(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier,
    )
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun TintDot(tint: Long, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(tint)),
    )
}

/** Warm swatches used for recipes without a photo — picked deterministically from the title. */
private val placeholderSwatches = listOf(
    Color(0xFFB5412A) to Color(0xFFF7D9CF),
    Color(0xFF4E6A3D) to Color(0xFFDDE8CF),
    Color(0xFFB7791F) to Color(0xFFF8E3BD),
    Color(0xFF2F7A8C) to Color(0xFFD3E9EE),
    Color(0xFF7A4A8C) to Color(0xFFEADCF0),
    Color(0xFF8A5A3B) to Color(0xFFEFDFD1),
)

@Composable
fun RecipeImage(
    imagePath: String?,
    title: String,
    modifier: Modifier = Modifier,
    initialSize: Int = 56,
) {
    if (imagePath != null) {
        AsyncImage(
            model = File(imagePath),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        val (ink, wash) = placeholderSwatches[abs(title.hashCode()) % placeholderSwatches.size]
        Box(modifier.background(wash), contentAlignment = Alignment.Center) {
            Text(
                text = title.trim().firstOrNull()?.uppercase() ?: "·",
                color = ink,
                fontSize = initialSize.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.displayLarge,
            )
        }
    }
}

/** A tiny line illustration of a handi (clay pot) with steam — used for empty states. */
@Composable
fun HandiIllustration(modifier: Modifier = Modifier) {
    val pot = MaterialTheme.colorScheme.primary
    val band = MaterialTheme.colorScheme.tertiary
    val steam = MaterialTheme.colorScheme.outline
    Canvas(modifier.size(width = 160.dp, height = 140.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        // steam
        listOf(0.38f, 0.5f, 0.62f).forEach { x ->
            val p = Path().apply {
                moveTo(w * x, h * 0.36f)
                cubicTo(w * (x - 0.05f), h * 0.28f, w * (x + 0.05f), h * 0.2f, w * x, h * 0.08f)
            }
            drawPath(p, steam, style = stroke)
        }
        // rim
        drawRoundRect(
            color = pot,
            topLeft = Offset(w * 0.2f, h * 0.42f),
            size = Size(w * 0.6f, h * 0.07f),
            cornerRadius = CornerRadius(12f, 12f),
        )
        // body
        val body = Path().apply {
            moveTo(w * 0.26f, h * 0.49f)
            lineTo(w * 0.74f, h * 0.49f)
            cubicTo(w * 0.9f, h * 0.58f, w * 0.9f, h * 0.92f, w * 0.5f, h * 0.94f)
            cubicTo(w * 0.1f, h * 0.92f, w * 0.1f, h * 0.58f, w * 0.26f, h * 0.49f)
            close()
        }
        drawPath(body, pot)
        drawRect(band, topLeft = Offset(w * 0.17f, h * 0.64f), size = Size(w * 0.66f, h * 0.04f))
    }
}

@Composable
fun VSpace(height: Dp) = Spacer(Modifier.height(height))

@Composable
fun HSpace(width: Dp) = Spacer(Modifier.width(width))

@Composable
fun FillBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    Box(modifier.fillMaxSize()) { content() }
