package com.calmpulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmpulse.ui.theme.DarkBackground

data class QuickPrompt(
    val icon: String,
    val title: String,
    val messageText: String,
    val isAction: Boolean = false,
    val actionType: String = ""
)

val defaultQuickPrompts = listOf(
    QuickPrompt(
        icon = "🫁",
        title = "Respiração 4-7-8",
        messageText = "",
        isAction = true,
        actionType = "BREATHING"
    ),
    QuickPrompt(
        icon = "🌿",
        title = "Alívio de Ansiedade",
        messageText = "Estou sentindo uma crise de ansiedade agora, você pode me acolher e ajudar a me acalmar?"
    ),
    QuickPrompt(
        icon = "💭",
        title = "Acalmar Pensamentos",
        messageText = "Minha cabeça está muito cheia e acelerada. Como posso organizar meus pensamentos?"
    ),
    QuickPrompt(
        icon = "🌙",
        title = "Ajuda para Dormir",
        messageText = "Não estou conseguindo relaxar para dormir. Tem alguma técnica que possa me orientar?"
    ),
    QuickPrompt(
        icon = "💛",
        title = "Apoio Emocional",
        messageText = "Preciso desabafar sobre algo que está me pesando hoje."
    )
)

/**
 * Pílulas horizontais de sugestão inspiradas na tela de pesquisa Meta AI do Figma WhatsApp 2025.
 */
@Composable
fun QuickPromptChips(
    prompts: List<QuickPrompt> = defaultQuickPrompts,
    onPromptClick: (QuickPrompt) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground

    val chipBg = if (isDark) Color(0xFF1F2C34) else Color(0xFFFFFFFF)
    val chipBorder = if (isDark) Color(0xFF2A3942) else Color(0xFFE2E8F0)
    val textColor = if (isDark) Color(0xFFE9EDEF) else Color(0xFF111B21)

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(prompts) { prompt ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(chipBg)
                    .border(0.8.dp, chipBorder, RoundedCornerShape(20.dp))
                    .clickable { onPromptClick(prompt) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = prompt.icon,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = prompt.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor
                        )
                    )
                }
            }
        }
    }
}
