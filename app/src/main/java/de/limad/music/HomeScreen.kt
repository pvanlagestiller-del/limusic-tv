package de.limad.music

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

private val Black = Color(0xFF0A0A0A)
private val CardBg = Color(0xFF1C1C1C)
private val Red = Color(0xFFE53935)
private val Gray = Color(0x99FFFFFF)

@Composable
fun HomeScreen(
    tab: Tab,
    shelves: List<Shelf>,
    prefs: Prefs,
    settingsTick: Int,
    onTab: (Tab) -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    onSearch: () -> Unit,
    onSettingChanged: () -> Unit,
) {
    val firstTab = remember { FocusRequester() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Black)
            .padding(start = 56.dp, end = 56.dp, top = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText("LiMusic", style = TextStyle(color = Red, fontSize = 30.sp, fontWeight = FontWeight.Bold))
            Spacer(Modifier.width(48.dp))
            Tab.entries.forEachIndexed { i, t ->
                TabChip(
                    label = t.label,
                    selected = t == tab,
                    modifier = if (i == 0) Modifier.focusRequester(firstTab) else Modifier,
                    onSelect = { onTab(t) },
                )
                Spacer(Modifier.width(12.dp))
            }
        }
        Spacer(Modifier.height(24.dp))

        when (tab) {
            Tab.EINSTELLUNGEN -> SettingsPane(prefs, settingsTick, onSettingChanged)
            Tab.SUCHE -> Column {
                ActionRow("Suchen …", onClick = onSearch)
                Spacer(Modifier.height(16.dp))
                Shelves(shelves, onPlay)
            }
            else -> Shelves(shelves, onPlay)
        }
    }

    LaunchedEffect(Unit) {
        delay(200)
        runCatching { firstTab.requestFocus() }
    }
}

/** Apple-TV-style top tab: focusing selects it. */
@Composable
private fun TabChip(label: String, selected: Boolean, modifier: Modifier, onSelect: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(
            label,
            modifier = modifier
                .clip(RoundedCornerShape(50))
                .background(if (focused) Color.White else Color.Transparent)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onSelect()
                }
                .clickable(onClick = onSelect)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            style = TextStyle(
                color = when {
                    focused -> Black
                    selected -> Color.White
                    else -> Gray
                },
                fontSize = 20.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            ),
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .width(28.dp)
                .height(3.dp)
                .background(if (selected && !focused) Red else Color.Transparent, RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun Shelves(shelves: List<Shelf>, onPlay: (List<Track>, Int) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 56.dp),
    ) {
        items(shelves.size) { i ->
            val s = shelves[i]
            if (!s.loading && !s.error && s.tracks.isEmpty()) return@items
            Column {
                BasicText(s.title, style = TextStyle(color = Gray, fontSize = 20.sp, fontWeight = FontWeight.Medium))
                when {
                    s.loading -> BasicText("Lädt …", Modifier.padding(vertical = 24.dp), style = TextStyle(color = Gray, fontSize = 16.sp))
                    s.error -> BasicText("Konnte nicht geladen werden", Modifier.padding(vertical = 24.dp), style = TextStyle(color = Red, fontSize = 16.sp))
                    else -> LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        contentPadding = PaddingValues(vertical = 20.dp, horizontal = 8.dp),
                    ) {
                        itemsIndexed(s.tracks, key = { _, t -> t.url }) { j, t ->
                            TrackCard(t) { onPlay(s.tracks, j) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackCard(t: Track, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, label = "scale")
    val width = if (t.music) 200.dp else 300.dp
    val shape = RoundedCornerShape(12.dp)

    Column(Modifier.width(width).zIndex(if (focused) 1f else 0f)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(if (t.music) 1f else 16f / 9f)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(shape)
                .background(CardBg)
                .border(if (focused) 3.dp else 0.dp, if (focused) Red else Color.Transparent, shape)
                .onFocusChanged { focused = it.isFocused }
                .clickable(onClick = onClick)
        ) {
            AsyncImage(
                model = t.thumb,
                contentDescription = t.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (t.durationSec > 0) {
                BasicText(
                    formatDuration(t.durationSec),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    style = TextStyle(color = Color.White, fontSize = 12.sp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        BasicText(
            t.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = if (focused) Color.White else Color(0xDDFFFFFF), fontSize = 16.sp),
        )
        BasicText(
            t.artist,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = Gray, fontSize = 13.sp),
        )
    }
}

@Composable
private fun SettingsPane(prefs: Prefs, tick: Int, changed: () -> Unit) {
    val onOff = { v: Boolean -> if (v) "An" else "Aus" }
    // `tick` changes after every edit, so this pane recomposes and re-reads prefs (focus stays).
    check(tick >= 0)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ActionRow("Maximale Auflösung", "${prefs.maxHeight}p") {
            val list = Prefs.RESOLUTIONS
            prefs.maxHeight = list[(list.indexOf(prefs.maxHeight) + 1) % list.size]
            changed()
        }
        ActionRow("Musik nur als Audio", onOff(prefs.musicAudioOnly)) {
            prefs.musicAudioOnly = !prefs.musicAudioOnly
            changed()
        }
        ActionRow("Automatisch weiterspielen", onOff(prefs.autoplay)) {
            prefs.autoplay = !prefs.autoplay
            changed()
        }
        ActionRow("Verlauf löschen", "") {
            prefs.clearHistory()
            changed()
        }
        Spacer(Modifier.height(16.dp))
        BasicText(
            "Keine Werbung: Streams werden direkt geladen, ohne YouTube-Oberfläche.",
            style = TextStyle(color = Gray, fontSize = 15.sp),
        )
    }
}

@Composable
private fun ActionRow(label: String, value: String = "", onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth(0.6f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) Color.White else CardBg)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        BasicText(
            label,
            modifier = Modifier.weight(1f),
            style = TextStyle(color = if (focused) Black else Color.White, fontSize = 18.sp),
        )
        if (value.isNotEmpty()) {
            BasicText(value, style = TextStyle(color = if (focused) Red else Gray, fontSize = 18.sp))
        }
    }
}

private fun formatDuration(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
