package com.deathchat.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deathchat.app.ui.theme.BorderGlow
import com.deathchat.app.ui.theme.BorderSubtle
import com.deathchat.app.ui.theme.PurpleGlow
import com.deathchat.app.ui.theme.SurfaceDark

object AvatarHelper {
    val AVATAR_MAP = mapOf(
        "avatar_reaper" to "💀",
        "avatar_phantom" to "👻",
        "avatar_spectre" to "👤",
        "avatar_demon" to "😈",
        "avatar_stalker" to "🕶️",
        "avatar_wraith" to "🥷",
        "avatar_skull" to "☠️",
        "avatar_glitch" to "🤖"
    )

    fun getEmoji(avatarKey: String?): String {
        return AVATAR_MAP[avatarKey] ?: "💀"
    }
}

@Composable
fun AvatarPicker(
    selectedAvatar: String,
    onAvatarSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val avatars = AvatarHelper.AVATAR_MAP.entries.toList()
    val rows = avatars.chunked(4)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { (key, emoji) ->
                    val isSelected = key == selectedAvatar
                    val shape = RoundedCornerShape(12.dp)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(shape)
                            .background(if (isSelected) Color(0x33A855F7) else SurfaceDark)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) PurpleGlow else BorderSubtle,
                                shape = shape
                            )
                            .clickable { onAvatarSelected(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 28.sp)
                    }
                }
            }
        }
    }
}
