package com.example.c001apk.compose.ui.component.cards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Created by bggRGjQaUbCoE on 2024/6/16
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchHistoryCard(
    modifier: Modifier = Modifier,
    data: String,
    onSearch: () -> Unit,
    onDelete: () -> Unit,
) {

    val shape = RoundedCornerShape(18.dp)
    Text(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                0.6.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                shape,
            )
            .combinedClickable(
                onClick = onSearch,
                onLongClick = onDelete
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        text = data,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        ),
        maxLines = 1,
    )

}
