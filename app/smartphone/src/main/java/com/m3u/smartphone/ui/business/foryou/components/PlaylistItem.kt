package com.m3u.smartphone.ui.business.foryou.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.m3u.core.foundation.components.AbsoluteSmoothCornerShape
import com.m3u.smartphone.ui.common.internal.PreviewTheme
import com.m3u.smartphone.ui.material.components.Badge
import com.m3u.smartphone.ui.material.components.FontFamilies
import com.m3u.smartphone.ui.material.components.TextBadge
import com.m3u.smartphone.ui.material.model.LocalSpacing

@Composable
internal fun PlaylistItem(
    label: String,
    type: String?,
    count: Int,
    refreshable: Boolean,
    subscribingOrRefreshing: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val shape = AbsoluteSmoothCornerShape(spacing.large, 65)
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
    val accent = when {
        subscribingOrRefreshing -> MaterialTheme.colorScheme.tertiary
        refreshable -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        shape = shape,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
        ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier.semantics(mergeDescendants = true) { }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
            modifier = Modifier
                .clip(shape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.20f),
                            containerColor,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                        )
                    )
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .fillMaxWidth()
                .heightIn(min = 136.dp)
                .padding(spacing.medium)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                type
                    ?.takeIf(String::isNotBlank)
                    ?.let {
                        TextBadge(
                            text = it.uppercase(),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                if (subscribingOrRefreshing) {
                    Badge(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Rounded.Sync,
                                contentDescription = null
                            )
                            Text(
                                text = "SYNCING",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamilies.LexendExa
                            )
                        }
                    }
                }
            }

            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamilies.LexendExa
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
                ) {
                    Text(
                        text = "CHANNELS",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.4.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (refreshable) "remote playlist" else "saved source",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun PlaylistItemPreviewRemote() {
    PreviewTheme {
        PlaylistItem(
            label = "LIVE SPORTS ULTRA HD",
            type = "m3u",
            count = 182,
            refreshable = true,
            subscribingOrRefreshing = true,
            onClick = {},
            onLongClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun PlaylistItemPreviewSaved() {
    PreviewTheme(useDarkTheme = true) {
        PlaylistItem(
            label = "CINEMA NIGHT ARCHIVE",
            type = "xtream live",
            count = 64,
            refreshable = false,
            subscribingOrRefreshing = false,
            onClick = {},
            onLongClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
