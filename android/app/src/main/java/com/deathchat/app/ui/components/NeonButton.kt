package com.deathchat.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deathchat.app.ui.theme.*

enum class NeonButtonVariant {
    PRIMARY,
    SECONDARY,
    DANGER
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NeonButtonVariant = NeonButtonVariant.PRIMARY,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1f, label = "button_press")

    val shape = RoundedCornerShape(12.dp)

    val backgroundBrush = when (variant) {
        NeonButtonVariant.PRIMARY -> Brush.horizontalGradient(
            listOf(Color(0xFF7E22CE), ElectricPurple, PurpleGlow)
        )
        NeonButtonVariant.SECONDARY -> Brush.horizontalGradient(
            listOf(Color(0x2206B6D4), Color(0x3306B6D4))
        )
        NeonButtonVariant.DANGER -> Brush.horizontalGradient(
            listOf(Color(0x33EF4444), Color(0x44EF4444))
        )
    }

    val borderColor = when (variant) {
        NeonButtonVariant.PRIMARY -> PurpleGlow
        NeonButtonVariant.SECONDARY -> CyanAccent
        NeonButtonVariant.DANGER -> ErrorRed
    }

    val textColor = when (variant) {
        NeonButtonVariant.PRIMARY -> TextPrimary
        NeonButtonVariant.SECONDARY -> CyanAccent
        NeonButtonVariant.DANGER -> Color(0xFFFCA5A5)
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(backgroundBrush)
            .border(1.dp, if (enabled) borderColor else Color.Gray, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = textColor,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text.uppercase(),
                color = if (enabled) textColor else TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
