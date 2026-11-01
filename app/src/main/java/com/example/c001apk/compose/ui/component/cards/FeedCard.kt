package com.example.c001apk.compose.ui.component.cards

import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.ui.base.LikeStateSync
import com.example.c001apk.compose.ui.base.LikeType
import com.example.c001apk.compose.ui.component.CoilLoader
import com.example.c001apk.compose.ui.component.IconText
import com.example.c001apk.compose.ui.component.Ios26IconText
import com.example.c001apk.compose.ui.component.LinkText
import com.example.c001apk.compose.ui.component.NineImageView
import com.example.c001apk.compose.ui.theme.cardBg
import com.example.c001apk.compose.util.CookieUtil
import com.example.c001apk.compose.util.CookieUtil.isLogin
import com.example.c001apk.compose.util.DateUtils.fromToday
import com.example.c001apk.compose.util.ReportType
import com.example.c001apk.compose.util.ShareType
import com.example.c001apk.compose.util.Utils.richToString
import com.example.c001apk.compose.util.copyText
import com.example.c001apk.compose.util.getShareText
import com.example.c001apk.compose.util.longClick

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedCard(
    modifier: Modifier = Modifier,
    isFeedContent: Boolean,
    isFeedTop: Boolean = false,
    data: HomeFeedResponse.Data,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    onReport: ((String, ReportType) -> Unit)? = null,
    onLike: ((String, Int, LikeType) -> Unit)? = null,
    onDelete: ((String, LikeType, String?) -> Unit)? = null,
    onBlockUser: (String) -> Unit,
    onCollect: ((String, Boolean) -> Unit)? = null,
    onCollectPick: ((String) -> Unit)? = null,
    /** 强制初始「已收藏」状态（收藏单列表里每条都已收藏，按钮应显示实心） */
    initialCollected: Boolean? = null,
) {
    // 本地收藏状态（点击后即时变色，再点移除）
    var fav by remember(data.id, data.userAction?.collect, initialCollected) {
        mutableStateOf(initialCollected ?: ((data.userAction?.collect ?: 0) == 1))
    }
    // 收藏数（点击后即时 +1 / -1）
    var favNumState by remember(data.id, data.favnum) {
        mutableStateOf(data.favnum.orEmpty())
    }
    val horizontal = if (isFeedContent) 4.dp else 10.dp
    // val vertical = if (isFeedContent) 12.dp else 10.dp
    Column(
        modifier = run {
            val tmp = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = if (isFeedContent) 12.dp else 0.dp)
                .clip(
                    if (isFeedContent) RoundedCornerShape(18.dp)
                    else RoundedCornerShape(16.dp)
                )
                .background(
                    if (isFeedContent) MaterialTheme.colorScheme.surface
                    else cardBg()
                )
            if (isFeedContent)
                tmp.longClick {
                    onCopyText(data.message)
                }
            else
                tmp.combinedClickable(
                    onClick = {
                        onViewFeed(data.id.orEmpty(), false)
                    },
                    onLongClick = {
                        onCopyText(data.message)
                    }
                )
        }
    ) {
        if (!isFeedContent) {
            FeedHeader(
                modifier = Modifier.padding(start = horizontal),
                data = data,
                onViewUser = onViewUser,
                isFeedContent = false,
                onReport = onReport,
                isFeedTop = isFeedTop,
                onDelete = onDelete,
                onBlockUser = onBlockUser,
            )
        }
        FeedMessage(
            modifier = Modifier
                .padding(horizontal = horizontal)
                .fillMaxWidth(),
            data = data,
            onOpenLink = onOpenLink,
            isFeedContent = isFeedContent,
            onViewFeed = onViewFeed,
            onCopyText = onCopyText,
        )
        FeedBottomInfo(
            modifier = Modifier
                .padding(horizontal = horizontal)
                .padding(bottom = if (data.targetRow == null && data.relationRows.isNullOrEmpty()) 12.dp else 0.dp),
            isFeedContent = isFeedContent,
            ip = data.ipLocation.orEmpty(),
            dateline = data.dateline ?: 0,
            replyNum = data.replynum.orEmpty(),
            likeNum = data.likenum.orEmpty(),
            favNum = favNumState,
            isFav = fav,
            feedId = data.id,
            onViewFeed = {
                onViewFeed(data.id.orEmpty(), true)
            },
            onLike = {
                if (isLogin) {
                    onLike?.let {
                        it(data.id.orEmpty(), data.userAction?.like ?: 0, LikeType.FEED)
                    }
                }
            },
            onCollect = {
                if (isLogin) {
                    val want = !fav
                    fav = want
                    // 收藏数同步变化
                    favNumState = run {
                        val n = favNumState.toIntOrNull()
                        if (n != null) (n + if (want) 1 else -1).coerceAtLeast(0).toString()
                        else favNumState
                    }
                    onCollect?.invoke(data.id.orEmpty(), want)
                }
            },
            onCollectPick = {
                if (isLogin) {
                    onCollectPick?.invoke(data.id.orEmpty())
                }
            },
            like = data.userAction?.like
        )
        FeedRows(
            modifier = Modifier.padding(bottom = 10.dp),
            isFeedContent = isFeedContent,
            relationRows = data.relationRows,
            targetRow = data.targetRow,
            onOpenLink = onOpenLink
        )
    }
}

@Composable
fun FeedRows(
    modifier: Modifier = Modifier,
    isFeedContent: Boolean,
    relationRows: List<HomeFeedResponse.RelationRows>?,
    targetRow: HomeFeedResponse.TargetRow?,
    onOpenLink: (String, String?) -> Unit
) {
    val dataList = relationRows?.toMutableList() ?: ArrayList()
    targetRow?.let {
        dataList.add(
            0,
            HomeFeedResponse.RelationRows(
                it.id.orEmpty(),
                it.logo,
                it.title,
                it.url,
                it.targetType.toString()
            )
        )
    }

    if (dataList.isNotEmpty()) {
        LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = if (isFeedContent) 16.dp else 10.dp)
        ) {
            dataList.forEach {
                item(key = it.url) {
                    IconMiniScrollCardItem(
                        isFeedContent = isFeedContent,
                        logoUrl = it.logo.orEmpty(),
                        linkUrl = it.url.orEmpty(),
                        titleText = it.title.orEmpty(),
                        onOpenLink = onOpenLink
                    )
                }
            }
        }
    }

}

@Composable
fun FeedBottomInfo(
    modifier: Modifier = Modifier,
    isFeedContent: Boolean,
    ip: String,
    dateline: Long,
    replyNum: String,
    likeNum: String,
    favNum: String = "",
    isFav: Boolean = false,
    feedId: String? = null,
    onViewFeed: () -> Unit,
    onLike: () -> Unit,
    onCollect: () -> Unit = {},
    onCollectPick: (() -> Unit)? = null,
    like: Int?,
) {

    // 读取全局点赞同步缓存：详情页点赞后返回，这里实时显示最新状态
    val sync = LikeStateSync.get(feedId ?: "")
    val effLike = sync?.liked?.let { if (it) 1 else 0 } ?: (like ?: 0)
    val effLikeNum = sync?.count ?: likeNum

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = if (isFeedContent) {
                if (ip.isNotEmpty()) "发布于 $ip"
                else EMPTY_STRING
            } else fromToday(dateline),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.outline
        )

        Ios26IconText(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            title = replyNum,
            onClick = onViewFeed,
        )

        Ios26IconText(
            modifier = Modifier.padding(start = 8.dp),
            imageVector = if (effLike == 1) Icons.Filled.ThumbUp
            else Icons.Outlined.ThumbUp,
            title = effLikeNum,
            onClick = onLike,
            isLike = effLike == 1,
        )

        Ios26IconText(
            modifier = Modifier.padding(start = 8.dp),
            imageVector = if (isFav) Icons.Filled.Star
            else Icons.Outlined.StarOutline,
            title = favNum,
            onClick = onCollect,
            onLongClick = onCollectPick,
            isLike = isFav,
        )
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedMessage(
    modifier: Modifier = Modifier,
    data: HomeFeedResponse.Data,
    onOpenLink: (String, String?) -> Unit,
    isFeedContent: Boolean,
    onViewFeed: (String, Boolean) -> Unit,
    onCopyText: (String?) -> Unit,
) {

    if (!data.messageTitle.isNullOrEmpty()) {
        LinkText(
            text = data.messageTitle,
            textSize = 16f,
            isBold = true,
            lineSpacingMultiplier = 1.3f,
            modifier = modifier.padding(top = 10.dp),
            onOpenLink = onOpenLink
        )
    }
    if (!data.message.isNullOrEmpty()) {
        LinkText(
            text = data.message,
            textSize = 16f,
            lineSpacingMultiplier = 1.3f,
            modifier = modifier.padding(top = 10.dp),
            onOpenLink = onOpenLink
        )
    }

    if (!data.picArr.isNullOrEmpty()) {
        NineImageView(
            modifier = modifier.padding(top = 10.dp),
            pic = data.pic,
            picArr = data.picArr,
            feedType = data.feedType
        )
    }

    if (!data.forwardSourceType.isNullOrEmpty()) {
        if (data.forwardSourceFeed == null) {
            Text(
                text = "动态已被删除",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = modifier
                    .padding(top = 10.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        if (isFeedContent)
                            cardBg()
                        else
                            MaterialTheme.colorScheme.surface
                    )
                    .padding(10.dp)
            )
        } else {
            Column(
                modifier = modifier
                    .padding(top = 10.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        if (isFeedContent)
                            cardBg()
                        else
                            MaterialTheme.colorScheme.surface
                    )
                    .combinedClickable(
                        onClick = {
                            onOpenLink(data.forwardSourceFeed.url.orEmpty(), null)
                        },
                        onLongClick = {
                            onCopyText(data.forwardSourceFeed.message)
                        }
                    )
                    .padding(10.dp)
            ) {
                if (!data.forwardSourceFeed.messageTitle.isNullOrEmpty()) {
                    LinkText(
                        text = """<a class="feed-link-uname" href="/u/${data.forwardSourceFeed.uid}">@${data.forwardSourceFeed.username}</a>: ${data.forwardSourceFeed.messageTitle}""",
                        onOpenLink = onOpenLink,
                        lineSpacingMultiplier = 1.2f,
                    )
                    if (!data.forwardSourceFeed.message.isNullOrEmpty())
                        LinkText(
                            text = data.forwardSourceFeed.message,
                            onOpenLink = onOpenLink,
                            lineSpacingMultiplier = 1.2f,
                        )
                } else {
                    LinkText(
                        text = """<a class="feed-link-uname" href="/u/${data.forwardSourceFeed.uid}">@${data.forwardSourceFeed.username}</a>: ${data.forwardSourceFeed.message}""",
                        onOpenLink = onOpenLink,
                        lineSpacingMultiplier = 1.2f,
                    )
                }
                if (!data.forwardSourceFeed.picArr.isNullOrEmpty()) {
                    NineImageView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        pic = data.forwardSourceFeed.pic,
                        picArr = data.forwardSourceFeed.picArr,
                        feedType = data.forwardSourceFeed.feedType
                    )
                }
            }
        }
    }

    if (!data.replyRows.isNullOrEmpty()) {
        val reply = data.replyRows?.getOrNull(0)
        val replyPic = when (reply?.pic) {
            EMPTY_STRING -> EMPTY_STRING
            else -> """ <a class=\"feed-forward-pic\" href=${reply?.pic}>查看图片(${reply?.picArr?.size})</a>"""
        }
        LinkText(
            text = """<a class="feed-link-uname" href="/u/${reply?.uid}">${reply?.username}</a>: ${reply?.message}$replyPic""",
            onOpenLink = onOpenLink,
            lineSpacingMultiplier = 1.2f,
            imgList = reply?.picArr,
            textSize = 14f,
            modifier = modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
                .combinedClickable(
                    onClick = {
                        onViewFeed(data.id.orEmpty(), true)
                    },
                    onLongClick = {
                        onCopyText(reply?.message)
                    }
                )
                .padding(10.dp)
        )
    }

    if (!data.extraUrl.isNullOrEmpty()) {
        ConstraintLayout(
            modifier = modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(
                    if (isFeedContent)
                        cardBg()
                    else
                        MaterialTheme.colorScheme.surface
                )
                .clickable {
                    onOpenLink(data.extraUrl, data.extraTitle)
                }
                .padding(10.dp)
        ) {
            val (pic, title, url) = createRefs()

            if (!data.extraPic.isNullOrEmpty()) {
                CoilLoader(
                    url = data.extraPic,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .constrainAs(pic) {
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                            start.linkTo(parent.start)
                            height = Dimension.fillToConstraints
                        })
            } else {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .constrainAs(pic) {
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                            start.linkTo(parent.start)
                            height = Dimension.fillToConstraints
                        }
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
                    )
                }
            }

            if (!data.extraTitle.isNullOrEmpty()) {
                Text(
                    text = data.extraTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .constrainAs(title) {
                            start.linkTo(pic.end)
                            top.linkTo(parent.top)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                        }
                )
            }

            Text(
                text = data.extraUrl,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                modifier = Modifier
                    .padding(start = 10.dp)
                    .constrainAs(url) {
                        start.linkTo(pic.end)
                        top.linkTo(if (!data.extraTitle.isNullOrEmpty()) title.bottom else parent.top)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                    }
            )
        }
    }
}

@Composable
fun FeedHeader(
    modifier: Modifier = Modifier,
    data: HomeFeedResponse.Data,
    onViewUser: (String) -> Unit,
    isFeedContent: Boolean,
    isFeedTop: Boolean,
    onReport: ((String, ReportType) -> Unit)? = null,
    onDelete: ((String, LikeType, String?) -> Unit)? = null,
    onBlockUser: ((String) -> Unit)? = null,
) {

    val vertical = if (isFeedContent) 12.dp else 10.dp
    var dropdownMenuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        val (avatar, username, from, device, expand, stickTop) = createRefs()

        CoilLoader(
            url = data.userAvatar,
            modifier = Modifier
                .padding(top = vertical)
                .clip(CircleShape)
                .aspectRatio(1f, false)
                .constrainAs(avatar) {
                    top.linkTo(username.top)
                    bottom.linkTo(device.bottom)
                    start.linkTo(parent.start)
                    height = Dimension.fillToConstraints
                }
                .clickable {
                    onViewUser(data.uid.orEmpty())
                },
        )

        Text(
            modifier = Modifier
                .padding(
                    start = 10.dp,
                    top = vertical,
                    end = if (isFeedTop) 0.dp else if (!isFeedContent) 10.dp else 16.dp
                )
                .constrainAs(username) {
                    start.linkTo(avatar.end)
                    top.linkTo(parent.top)
                    end.linkTo(
                        if (isFeedContent) parent.end
                        else if (data.isStickTop == 1) stickTop.start
                        else expand.start
                    )
                    width = Dimension.fillToConstraints
                },
            text = data.username.orEmpty(),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (!isFeedContent && data.isStickTop == 1) {
            Text(
                text = "置顶",
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                    .constrainAs(stickTop) {
                        top.linkTo(username.top)
                        bottom.linkTo(username.bottom)
                        end.linkTo(expand.start)
                    }
                    .padding(horizontal = 6.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 10.sp)
            )
        }

        if (isFeedContent || !data.infoHtml.isNullOrEmpty()) {
            Text(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .constrainAs(from) {
                        start.linkTo(avatar.end)
                        top.linkTo(username.bottom)
                    },
                text = if (isFeedContent) fromToday(
                    data.dateline ?: 0
                ) else data.infoHtml.orEmpty().richToString(),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }


        IconText(
            modifier = Modifier
                .padding(
                    start = 10.dp,
                    end = if (isFeedTop) 0.dp else if (!isFeedContent) 10.dp else 16.dp
                )
                .constrainAs(device) {
                    start.linkTo(if (isFeedContent || !data.infoHtml.isNullOrEmpty()) from.end else avatar.end)
                    if (isFeedContent || !data.infoHtml.isNullOrEmpty())
                        bottom.linkTo(from.bottom)
                    else
                        top.linkTo(username.bottom)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                },
            imageVector = Icons.Outlined.Smartphone,
            title = data.deviceTitle?.richToString().orEmpty(),
            textSize = 13f,
        )

        if (!isFeedContent) {
            Box(
                modifier = Modifier
                    .aspectRatio(1f, false)
                    .constrainAs(expand) {
                        top.linkTo(parent.top)
                        end.linkTo(parent.end)
                        bottom.linkTo(device.bottom)
                        height = Dimension.fillToConstraints
                    }) {

                IconButton(
                    onClick = {
                        dropdownMenuExpanded = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }

                DropdownMenu(
                    expanded = dropdownMenuExpanded,
                    onDismissRequest = {
                        dropdownMenuExpanded = false
                    },
                ) {
                    listOf("复制链接", "拉黑").forEachIndexed { index, menu ->
                        DropdownMenuItem(
                            text = { Text(menu) },
                            onClick = {
                                dropdownMenuExpanded = false
                                when (index) {
                                    0 -> context.copyText(
                                        getShareText(ShareType.FEED, data.id.orEmpty())
                                    )

                                    1 -> onBlockUser?.let { it(data.uid.orEmpty()) }
                                }
                            }
                        )
                    }
                    if (isLogin) {
                        DropdownMenuItem(
                            text = { Text("举报") },
                            onClick = {
                                dropdownMenuExpanded = false
                                onReport?.let { it(data.id.orEmpty(), ReportType.FEED) }
                            }
                        )
                    }
                    if (data.uid == CookieUtil.uid) {
                        DropdownMenuItem(
                            text = { Text("删除") },
                            onClick = {
                                dropdownMenuExpanded = false
                                onDelete?.let { it(data.id.orEmpty(), LikeType.FEED, null) }
                            }
                        )
                    }
                }

            }
        }

    }

}