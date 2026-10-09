package com.supgarou.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.supgarou.app.model.Team
import com.supgarou.app.model.tr

data class TileInfo(
    val pid: Int,
    val seat: Int,
    val name: String,
    val roleIcon: String,
    val roleName: String,
    val team: Team?,
    val alive: Boolean,
    val selected: Boolean,
    val dimmed: Boolean,
    val actor: Boolean,
    val badges: String,
)

private val MIN_W = 74.dp
private val MIN_H = 44.dp
private val MAX_H = 112.dp
private val GAP = 6.dp

/** Best column count so that [n] tiles fit in [w] x [h] without scrolling, or null if they can't. */
private fun fitColumns(n: Int, w: Dp, h: Dp): Int? {
    var best: Int? = null
    var bestScore = 0f
    for (c in 1..n) {
        val rows = (n + c - 1) / c
        val tw = (w - GAP * (c - 1)) / c
        val th = (h - GAP * (rows - 1)) / rows
        if (tw < MIN_W || th < MIN_H) continue
        val effH = minOf(th.value, MAX_H.value)
        val effW = minOf(tw.value, effH * 3.2f)
        val score = effW * effH
        if (score > bestScore) { bestScore = score; best = c }
    }
    return best
}

@Composable
fun PlayerGrid(
    tiles: List<TileInfo>,
    hideRoles: Boolean,
    split: Boolean,
    onTap: (Int) -> Unit,
    onLong: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val n = tiles.size
        if (n == 0) return@BoxWithConstraints
        val w = maxWidth
        val h = maxHeight
        if (split && n >= 16) {
            val half = (n + 1) / 2
            val pageW = (w - 13.dp) / 2
            val cols = fitColumns(half, pageW, h - 20.dp)
            if (cols != null) {
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1f)) {
                        PageHeader(tr("Seats 1–$half", "Places 1–$half"))
                        TileGrid(tiles.take(half), cols, pageW, h - 20.dp, hideRoles, onTap, onLong)
                    }
                    VerticalDivider(Modifier.padding(horizontal = 6.dp))
                    Column(Modifier.weight(1f)) {
                        PageHeader(tr("Seats ${half + 1}–$n", "Places ${half + 1}–$n"))
                        TileGrid(tiles.drop(half), cols, pageW, h - 20.dp, hideRoles, onTap, onLong)
                    }
                }
                return@BoxWithConstraints
            }
        }
        val cols = fitColumns(n, w, h)
        if (cols != null) {
            TileGrid(tiles, cols, w, h, hideRoles, onTap, onLong)
            return@BoxWithConstraints
        }
        // Too many players for one screen: swipeable pages.
        val pageH = h - 18.dp
        val colsP = maxOf(1, ((w + GAP) / (MIN_W + GAP)).toInt())
        val rowsP = maxOf(1, ((pageH + GAP) / (MIN_H + GAP)).toInt())
        val pages = tiles.chunked(colsP * rowsP)
        val pager = rememberPagerState { pages.size }
        Column(Modifier.fillMaxSize()) {
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                TileGrid(pages[page], colsP, w, pageH, hideRoles, onTap, onLong)
            }
            Row(Modifier.fillMaxWidth().height(18.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                repeat(pages.size) { i ->
                    Box(
                        Modifier.padding(horizontal = 3.dp).size(if (i == pager.currentPage) 10.dp else 7.dp).clip(CircleShape)
                            .background(if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    )
                }
            }
        }
    }
}

@Composable
private fun PageHeader(text: String) {
    Text(
        text,
        Modifier.height(20.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TileGrid(
    tiles: List<TileInfo>,
    cols: Int,
    w: Dp,
    h: Dp,
    hideRoles: Boolean,
    onTap: (Int) -> Unit,
    onLong: (Int) -> Unit,
) {
    val rows = (tiles.size + cols - 1) / cols
    val th = min((h - GAP * (rows - 1)) / rows, MAX_H)
    Column(Modifier.width(w), verticalArrangement = Arrangement.spacedBy(GAP)) {
        tiles.chunked(cols).forEach { rowTiles ->
            Row(Modifier.fillMaxWidth().height(th), horizontalArrangement = Arrangement.spacedBy(GAP)) {
                rowTiles.forEach { t ->
                    PlayerTile(t, hideRoles, { onTap(t.pid) }, { onLong(t.pid) }, Modifier.weight(1f).fillMaxHeight())
                }
                repeat(cols - rowTiles.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun PlayerTile(t: TileInfo, hideRoles: Boolean, onTap: () -> Unit, onLong: () -> Unit, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)
    val bg = when {
        t.selected -> cs.primaryContainer
        !t.alive -> cs.surfaceVariant.copy(alpha = 0.45f)
        else -> cs.surfaceContainerHigh
    }
    val border = when {
        t.selected -> BorderStroke(2.5.dp, cs.primary)
        t.actor -> BorderStroke(2.dp, cs.tertiary)
        else -> BorderStroke(1.dp, cs.outlineVariant)
    }
    BoxWithConstraints(
        modifier
            .alpha(if (t.dimmed && !t.selected) 0.42f else 1f)
            .clip(shape)
            .background(bg)
            .border(border, shape)
            .combinedClickable(onClick = onTap, onLongClick = onLong),
    ) {
        val big = maxHeight >= 76.dp && maxWidth >= 110.dp
        val mid = maxHeight >= 58.dp
        val nameSize = if (big) 17.sp else if (mid) 15.sp else 13.sp
        val smallSize = if (big) 13.sp else 11.sp
        val badges = if (hideRoles) "" else t.badges
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier.padding(vertical = 8.dp).padding(start = 4.dp, end = 6.dp).width(4.dp).fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp)).background(teamColor(if (hideRoles) null else t.team)),
            )
            Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.Center) {
                Text(
                    t.name,
                    fontSize = nameSize,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (!t.alive) TextDecoration.LineThrough else null,
                    color = if (t.alive) cs.onSurface else cs.onSurfaceVariant,
                )
                if (mid) {
                    if (!hideRoles) {
                        Text("${t.roleIcon} ${t.roleName}", fontSize = smallSize, maxLines = 1, overflow = TextOverflow.Ellipsis, color = cs.onSurfaceVariant)
                    }
                    if (badges.isNotEmpty()) Text(badges, fontSize = smallSize, maxLines = 1, overflow = TextOverflow.Clip)
                } else {
                    val line = (if (hideRoles) "" else t.roleIcon + " ") + badges
                    if (line.isNotBlank()) Text(line, fontSize = smallSize, maxLines = 1, overflow = TextOverflow.Clip)
                }
            }
        }
        Text(
            "${t.seat}",
            Modifier.align(Alignment.TopEnd).padding(horizontal = 4.dp, vertical = 2.dp),
            fontSize = 9.sp,
            color = cs.outline,
        )
        if (!t.alive) Text("💀", Modifier.align(Alignment.BottomEnd).padding(2.dp), fontSize = 12.sp)
    }
}
