package com.example.c001apk.compose.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.c001apk.compose.util.ImageShowUtil
import com.example.c001apk.compose.util.copyText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * iOS 26 设计系统补充组件。
 *
 * 弹窗统一：居中、圆角 26dp、弹性缩放入场、淡出离场、点击遮罩先播完动画，
 * 且弹窗自身 window 也做沉浸处理（状态栏/导航栏透明、关闭对比度遮罩），
 * 避免「弹出时状态栏变灰」。
 */

val Ios26DialogShape = RoundedCornerShape(26.dp)

/** 弹窗卡片底色：完全不透明，避免透出灰底显得脏。 */
@Composable
fun ios26CardColor(): Color = MaterialTheme.colorScheme.surfaceContainerHigh

/** 弹窗 / 卡片的发丝描边色。 */
@Composable
fun ios26Hairline(): Color =
    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)

/**
 * 居中弹窗容器。
 */
@Composable
fun Ios26Dialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "ios26DialogAlpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.90f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "ios26DialogScale",
    )

    LaunchedEffect(Unit) { visible = true }

    val dismiss: () -> Unit = {
        if (visible) {
            visible = false
            scope.launch {
                delay(190)
                onDismissRequest()
            }
        }
    }

    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = true,
            decorFitsSystemWindows = false,
        ),
    ) {
        // 弹窗自身 window 完全沉浸：
        // Compose Dialog 默认会铺一层灰色 dim（连状态栏一起变灰），
        // 所以这里清掉 FLAG_DIM_BEHIND、把 window 背景设透明，
        // 遮罩完全交给我们自己的半透明 Box 绘制。
        val dialogView = LocalView.current
        LaunchedEffect(Unit) {
            runCatching {
                val win = (dialogView.parent as? android.app.Dialog)?.window ?: return@runCatching
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(win, false)

                // 关键：去掉系统调暗层，否则状态栏会跟着变灰
                win.clearFlags(
                    android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND
                )
                win.addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)

                // window 背景透明，避免一层灰底
                win.setBackgroundDrawable(
                    android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
                )

                @Suppress("DEPRECATION")
                win.statusBarColor = android.graphics.Color.TRANSPARENT
                @Suppress("DEPRECATION")
                win.navigationBarColor = android.graphics.Color.TRANSPARENT

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    win.isStatusBarContrastEnforced = false
                    win.isNavigationBarContrastEnforced = false
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    win.attributes = win.attributes.apply {
                        layoutInDisplayCutoutMode =
                            android.view.WindowManager.LayoutParams
                                .LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f * progress))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { dismiss() }
            )
            Column(
                modifier = modifier
                    .padding(horizontal = 26.dp)
                    .widthIn(max = 360.dp)
                    .graphicsLayer {
                        alpha = progress
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(Ios26DialogShape)
                    .background(ios26CardColor())
                    .border(0.6.dp, ios26Hairline(), Ios26DialogShape),
                content = content,
            )
        }
    }
}

@Composable
fun Ios26DialogTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
fun Ios26DialogMessage(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** iOS 26 提示弹窗。 */
@Composable
fun Ios26AlertDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String = "确定",
    onConfirm: (() -> Unit)? = null,
    dismissText: String? = "取消",
    message: String? = null,
    destructive: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Ios26Dialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(top = 22.dp, start = 20.dp, end = 20.dp)) {
            Ios26DialogTitle(title)
            if (!message.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Ios26DialogMessage(message)
            }
            content?.invoke(this)
        }
        Spacer(modifier = Modifier.height(18.dp))
        HorizontalDivider(thickness = 0.6.dp, color = ios26Hairline())
        Row(modifier = Modifier.height(50.dp)) {
            if (dismissText != null) {
                Ios26DialogButton(
                    text = dismissText,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ) { onDismiss() }
                Box(
                    modifier = Modifier
                        .width(0.6.dp)
                        .height(50.dp)
                        .background(ios26Hairline())
                )
            }
            Ios26DialogButton(
                text = confirmText,
                modifier = Modifier.weight(1f),
                bold = true,
                color = if (destructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
            ) {
                if (onConfirm != null) onConfirm() else onDismiss()
            }
        }
    }
}

/** 弹窗底部文字按钮（按压缩放反馈）。 */
@Composable
fun Ios26DialogButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    bold: Boolean = false,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val alpha by animateFloatAsState(
        targetValue = if (pressed) 0.55f else 1f,
        animationSpec = tween(120),
        label = "ios26DialogButton",
    )
    Box(
        modifier = modifier
            .height(50.dp)
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            color = color.copy(alpha = alpha),
        )
    }
}

/** 动作面板条目。 */
data class Ios26Action(
    val label: String,
    val icon: ImageVector? = null,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

/** iOS 26 居中动作面板。 */
@Composable
fun Ios26ActionSheet(
    title: String? = null,
    subtitle: String? = null,
    actions: List<Ios26Action>,
    onDismiss: () -> Unit,
) {
    Ios26Dialog(onDismissRequest = onDismiss) {
        if (!title.isNullOrEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            HorizontalDivider(thickness = 0.6.dp, color = ios26Hairline())
        }
        actions.forEachIndexed { index, action ->
            Ios26ActionRow(action) { action.onClick() }
            if (index != actions.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 52.dp),
                    thickness = 0.6.dp,
                    color = ios26Hairline(),
                )
            }
        }
        HorizontalDivider(thickness = 0.6.dp, color = ios26Hairline())
        Ios26DialogButton(
            text = "取消",
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            bold = true,
        ) { onDismiss() }
    }
}

@Composable
private fun Ios26ActionRow(action: Ios26Action, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val color = if (action.destructive) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = if (pressed) 0.08f else 0f)
            )
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (action.icon != null) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = if (action.destructive) color else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
        }
        Text(
            text = action.label,
            fontSize = 15.sp,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * iOS 26 搜索框：BasicTextField 自绘，42dp 固定高度，彻底避免被裁剪/遮挡。
 */
@Composable
fun Ios26SearchField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Search,
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .border(0.7.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), shape)
            .padding(start = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            modifier = Modifier.size(17.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(42.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.text.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.merge(
                    TextStyle(
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = imeAction),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { onSearch?.invoke() },
                    onDone = { onSearch?.invoke() },
                    onGo = { onSearch?.invoke() },
                ),
            )
        }
        if (value.text.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onValueChange(TextFieldValue("")) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "清空",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        trailing?.invoke()
    }
}

/** 通用按压缩放容器。 */
@Composable
fun Ios26Pressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    scaleDown: Float = 0.94f,
    content: @Composable (pressed: Boolean) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "ios26Press",
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
            ) { onClick() },
    ) {
        content(pressed)
    }
}

/** iOS 26 胶囊按钮。 */
@Composable
fun Ios26Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = false,
    icon: ImageVector? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    val shape = RoundedCornerShape(50)
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        filled -> MaterialTheme.colorScheme.onPrimary
        else -> tint
    }
    Ios26Pressable(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        scaleDown = 0.93f,
    ) {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(
                    if (filled) MaterialTheme.colorScheme.primary.copy(
                        alpha = if (enabled) 1f else 0.35f
                    )
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
                .border(
                    0.7.dp,
                    if (filled) Color.Transparent
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape,
                )
                .padding(horizontal = 15.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

/** iOS 26 圆形玻璃图标按钮。 */
@Composable
fun Ios26IconAction(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: androidx.compose.ui.unit.Dp = 36.dp,
    glass: Boolean = false,
) {
    Ios26Pressable(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        scaleDown = 0.88f,
    ) { pressed ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (glass || pressed) MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = if (pressed) 0.9f else 0.55f
                    )
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(size * 0.52f),
            )
        }
    }
}

/** iOS 26 文本输入弹窗。 */
@Composable
fun Ios26EditTextDialog(
    data: String,
    title: String,
    onDismiss: () -> Unit,
    setData: (String) -> Unit,
    hint: String? = null,
    maxLength: Int? = null,
    hexOnly: Boolean = false,
) {
    var text by remember {
        mutableStateOf(TextFieldValue(text = data, selection = TextRange(data.length)))
    }
    val focusRequest = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequest.requestFocus() } }

    Ios26Dialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(20.dp)) {
            Ios26DialogTitle(title)
            Spacer(modifier = Modifier.height(14.dp))
            val shape = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(0.7.dp, ios26Hairline(), shape)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (text.text.isEmpty() && hint != null) {
                    Text(
                        text = hint,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = { newValue ->
                        when {
                            hexOnly && maxLength != null ->
                                if (newValue.text.matches(Regex("^[0-9a-fA-F]{0,$maxLength}$"))) text = newValue
                            maxLength != null ->
                                if (newValue.text.length <= maxLength) text = newValue
                            else -> text = newValue
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequest),
                    textStyle = LocalTextStyle.current.merge(
                        TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Ios26DialogButton(
                    text = "取消",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                ) { onDismiss() }
                Ios26DialogButton(
                    text = "确定",
                    color = MaterialTheme.colorScheme.primary,
                    bold = true,
                    modifier = Modifier.weight(1f),
                ) {
                    onDismiss()
                    setData(text.text)
                }
            }
        }
    }
}

/**
 * 图片长按动作面板。
 *
 * 直接复用「全屏预览大图时长按弹出」的那套原生 AlertDialog —— 它在状态栏
 * 表现上已经验证完美（不会把状态栏改成灰底），因此长按图片的入口也走同一实现。
 */
@Composable
fun Ios26ImageActionsDialog(
    url: String,
    allUrls: List<String> = listOf(url),
    userAgent: String? = null,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(url) {
        ImageShowUtil.showSaveImgDialog(context, url, allUrls, userAgent)
        onDismiss()
    }
}
