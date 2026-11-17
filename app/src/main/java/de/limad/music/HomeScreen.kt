package de.limad.music

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

// Colours from LiMusic 2.2.1 desktop style.css
private val Bg = Color(0xFF0B0B10)
private val Panel = Color(0xFF1B1B22)
private val Panel2 = Color(0xFF282830)
private val Border = Color(0x16FFFFFF)
private val TextMain = Color(0xFFF7F8FA)
private val Muted = Color(0xFF8F98A8)
private val SectionMuted = Color(0xFF6F7887)
private val Accent = Color(0xFFE5193A)
private val AccentLight = Color(0xFFF0304D)
private val AccentDark = Color(0xFFB00A24)

enum class Nav(val label: String, val symbol: String) {
    START("Start", "⌂"),
    ENTDECKEN("Entdecken", "◎"),
    SUCHE("Suche", "⌕"),
    BIBLIOTHEK("Bibliothek", "♫"),
    EINSTELLUNGEN("Einstellungen", "⚙"),
}

enum class Mode { MUSIK, VIDEOS }

@Composable
fun HomeScreen(
    nav: Nav,
    mode: Mode,
    genres: List<String>,
    genre: String?,
    query: String?,
    featured: Track?,
    shelves: List<Shelf>,
    prefs: Prefs,
    settingsTick: Int,
    onNav: (Nav) -> Unit,
    onMode: (Mode) -> Unit,
    onGenre: (String) -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    onSearch: () -> Unit,
    onSettingChanged: () -> Unit,
) {
    val firstNav = remember { FocusRequester() }

    Row(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .background(
                Brush.radialGradient(
                    listOf(Color(0x33E5193A), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(1800f, 0f),
                    radius = 1400f,
                )
            )
            .padding(20.dp)
    ) {
        Sidebar(nav, onNav, firstNav)
        Spacer(Modifier.width(24.dp))

        Column(Modifier.fillMaxSize()) {
            Header(
                title = when (nav) {
                    Nav.SUCHE -> query?.let { "„$it“" } ?: "Suche"
                    else -> nav.label
                },
                mode = mode,
                showMode = nav != Nav.EINSTELLUNGEN && nav != Nav.BIBLIOTHEK,
                onMode = onMode,
            )
            Spacer(Modifier.height(10.dp))

            when (nav) {
                Nav.EINSTELLUNGEN -> SettingsPane(prefs, settingsTick, onSettingChanged)
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 48.dp, end = 8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (nav == Nav.SUCHE) {
                        item(key = "searchbtn") { SearchField(query, onSearch) }
                    }
                    if (nav == Nav.ENTDECKEN && genres.isNotEmpty()) {
                        item(key = "chips") { ChipRow(genres, genre, onGenre) }
                    }
                    if (featured != null) {
                        item(key = "featured") { FeaturedCard(featured) { onPlay(listOf(featured), 0) } }
                    }
                    items(shelves.size, key = { "shelf$it" }) { i -> ShelfView(shelves[i], onPlay) }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(200)
        runCatching { firstNav.requestFocus() }
    }
}

// ---------------------------------------------------------------- Sidebar

@Composable
private fun Sidebar(active: Nav, onNav: (Nav) -> Unit, firstNav: FocusRequester) {
    Column(
        Modifier
            .width(230.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF15151C), Color(0xFF0F0F14))))
            .border(1.dp, Color(0x12FFFFFF), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, bottom = 18.dp)) {
            Image(painterResource(R.drawable.ic_launcher), null, Modifier.size(40.dp))
            Spacer(Modifier.width(10.dp))
            BasicText("LiMusic", style = TextStyle(color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold))
        }
        SectionLabel("MEDIATHEK")
        Nav.entries.forEachIndexed { i, n ->
            if (n == Nav.EINSTELLUNGEN) {
                Spacer(Modifier.weight(1f))
            }
            NavItem(n, n == active, if (i == 0) Modifier.focusRequester(firstNav) else Modifier) { onNav(n) }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    BasicText(
        text,
        modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp),
        style = TextStyle(color = SectionMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp),
    )
}

@Composable
private fun NavItem(n: Nav, active: Boolean, modifier: Modifier, onSelect: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                when {
                    focused -> Brush.horizontalGradient(listOf(AccentLight, AccentDark))
                    active -> Brush.horizontalGradient(listOf(Color(0x38E5193A), Color(0x0EFFFFFF)))
                    else -> Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                }
            )
            .border(1.dp, if (active && !focused) Color(0x4DE5193A) else Color.Transparent, shape)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onSelect()
            }
            .clickable(onClick = onSelect)
    ) {
        // inset red bar for the active entry, like the desktop sidebar
        Box(
            Modifier
                .width(3.dp)
                .height(44.dp)
                .background(if (active && !focused) Accent else Color.Transparent)
        )
        BasicText(
            n.symbol,
            modifier = Modifier.width(36.dp).padding(start = 10.dp),
            style = TextStyle(color = if (focused || active) Color.White else Muted, fontSize = 18.sp),
        )
        BasicText(
            n.label,
            style = TextStyle(
                color = if (focused || active) Color.White else Color(0xFFB9C0CA),
                fontSize = 17.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            ),
        )
    }
}

// ---------------------------------------------------------------- Header

@Composable
private fun Header(title: String, mode: Mode, showMode: Boolean, onMode: (Mode) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        BasicText(
            title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            style = TextStyle(color = TextMain, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.6).sp),
        )
        if (showMode) ModeSwitch(mode, onMode)
    }
}

@Composable
private fun ModeSwitch(mode: Mode, onMode: (Mode) -> Unit) {
    val pill = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(pill)
            .background(Panel)
            .border(1.dp, Border, pill)
            .padding(4.dp)
    ) {
        listOf(Mode.VIDEOS to "▶  Videos", Mode.MUSIK to "♪  Musik").forEach { (m, label) ->
            var focused by remember { mutableStateOf(false) }
            val checked = m == mode
            BasicText(
                label,
                modifier = Modifier
                    .clip(pill)
                    .background(
                        when {
                            focused -> Brush.linearGradient(listOf(Color.White, Color.White))
                            checked -> Brush.linearGradient(listOf(Accent, AccentDark))
                            else -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                    )
                    .onFocusChanged { focused = it.isFocused }
                    .clickable { onMode(m) }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                style = TextStyle(
                    color = if (focused) Bg else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

// ---------------------------------------------------------------- Content pieces

@Composable
private fun SearchField(query: String?, onSearch: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(13.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .padding(vertical = 8.dp)
            .clip(shape)
            .background(Color(0xFF18181E))
            .border(if (focused) 2.dp else 1.dp, if (focused) Accent else Color(0x17FFFFFF), shape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onSearch)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        BasicText("⌕  ", style = TextStyle(color = Muted, fontSize = 20.sp))
        BasicText(
            query ?: "Songs, Künstler oder Videos suchen …",
            style = TextStyle(color = if (query != null) TextMain else Muted, fontSize = 17.sp),
        )
    }
}

@Composable
private fun ChipRow(genres: List<String>, selected: String?, onGenre: (String) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(genres, key = { it }) { g ->
            var focused by remember { mutableStateOf(false) }
            val checked = g == selected
            val pill = RoundedCornerShape(50)
            BasicText(
                g,
                modifier = Modifier
                    .clip(pill)
                    .background(
                        when {
                            focused -> Brush.linearGradient(listOf(Color.White, Color.White))
                            checked -> Brush.linearGradient(listOf(Accent, AccentDark))
                            else -> Brush.linearGradient(listOf(Panel, Panel))
                        }
                    )
                    .border(1.dp, if (checked || focused) Color.Transparent else Border, pill)
                    .onFocusChanged { focused = it.isFocused }
                    .clickable { onGenre(g) }
                    .padding(horizontal = 18.dp, vertical = 9.dp),
                style = TextStyle(color = if (focused) Bg else Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

@Composable
private fun FeaturedCard(t: Track, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    if (focused) listOf(Color(0x66E5193A), Color(0x0DFFFFFF)) else listOf(Color(0x42E5193A), Color(0x08FFFFFF))
                )
            )
            .border(if (focused) 2.dp else 1.dp, if (focused) AccentLight else Color(0x38E5193A), shape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Box(
            Modifier
                .width(if (t.music) 150.dp else 260.dp)
                .aspectRatio(if (t.music) 1f else 16f / 9f)
                .clip(RoundedCornerShape(14.dp))
                .background(Panel2)
        ) {
            AsyncImage(t.thumb, t.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(24.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                if (t.music) "EMPFOHLEN · SONG" else "EMPFOHLEN · VIDEO",
                style = TextStyle(color = AccentLight, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.9.sp),
            )
            Spacer(Modifier.height(6.dp))
            BasicText(
                t.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp),
            )
            Spacer(Modifier.height(4.dp))
            BasicText(t.artist, maxLines = 1, style = TextStyle(color = Color(0xFFAAB2BF), fontSize = 15.sp))
            Spacer(Modifier.height(14.dp))
            BasicText(
                "▶  Abspielen",
                modifier = Modifier
                    .clip(RoundedCornerShape(11.dp))
                    .background(Brush.verticalGradient(listOf(AccentLight, AccentDark)))
                    .padding(horizontal = 18.dp, vertical = 9.dp),
                style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun ShelfView(s: Shelf, onPlay: (List<Track>, Int) -> Unit) {
    if (!s.loading && !s.error && s.tracks.isEmpty()) return
    Column(Modifier.padding(top = 10.dp)) {
        BasicText(
            s.title,
            style = TextStyle(color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
        )
        when {
            s.loading -> LoadingRow()
            s.error -> BasicText(
                "Konnte nicht geladen werden",
                Modifier.padding(vertical = 24.dp),
                style = TextStyle(color = AccentLight, fontSize = 15.sp),
            )
            else -> LazyRow(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(vertical = 14.dp, horizontal = 6.dp),
            ) {
                itemsIndexed(s.tracks, key = { _, t -> t.url }) { j, t ->
                    StreamCard(t) { onPlay(s.tracks, j) }
                }
            }
        }
    }
}

@Composable
private fun LoadingRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(vertical = 14.dp)) {
        repeat(5) {
            Box(
                Modifier
                    .width(200.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(Panel2, Panel)))
            )
        }
    }
}

@Composable
private fun StreamCard(t: Track, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.07f else 1f, label = "scale")
    val width: Dp = if (t.music) 170.dp else 260.dp
    val thumbShape = RoundedCornerShape(if (t.music) 10.dp else 12.dp)

    Column(
        Modifier
            .width(width)
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(if (t.music) 1f else 16f / 9f)
                .graphicsLayer {
                    shadowElevation = if (focused) 28f else 0f
                    shape = thumbShape
                    clip = true
                }
                .background(Brush.linearGradient(listOf(Panel2, Panel)))
                .border(if (focused) 3.dp else 0.dp, if (focused) Accent else Color.Transparent, thumbShape)
        ) {
            AsyncImage(t.thumb, t.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            if (focused) {
                Box(Modifier.fillMaxSize().background(Color(0x33000000)))
                BasicText(
                    "▶",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color(0x73000000))
                        .border(1.dp, Color(0x59FFFFFF), CircleShape)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    style = TextStyle(color = Color.White, fontSize = 22.sp),
                )
            }
            if (t.durationSec > 0) {
                BasicText(
                    formatDuration(t.durationSec),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(7.dp)
                        .background(Color(0xB8000000), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                )
            }
        }
        Spacer(Modifier.height(9.dp))
        BasicText(
            t.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = if (focused) Color.White else TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
        )
        BasicText(
            t.artist,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = Muted, fontSize = 12.sp),
        )
    }
}

// ---------------------------------------------------------------- Settings

@Composable
private fun SettingsPane(prefs: Prefs, tick: Int, changed: () -> Unit) {
    // `tick` changes after every edit, so this pane recomposes and re-reads prefs (focus stays).
    check(tick >= 0)
    val onOff = { v: Boolean -> if (v) "An" else "Aus" }
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .clip(RoundedCornerShape(17.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF141419), Color(0xFF111116))))
            .border(1.dp, Color(0x13FFFFFF), RoundedCornerShape(17.dp))
            .padding(18.dp),
    ) {
        SettingRow("Maximale Auflösung", "${prefs.maxHeight}p") {
            val list = Prefs.RESOLUTIONS
            prefs.maxHeight = list[(list.indexOf(prefs.maxHeight) + 1) % list.size]
            changed()
        }
        SettingRow("Musik nur als Audio", onOff(prefs.musicAudioOnly)) {
            prefs.musicAudioOnly = !prefs.musicAudioOnly
            changed()
        }
        SettingRow("Automatisch weiterspielen", onOff(prefs.autoplay)) {
            prefs.autoplay = !prefs.autoplay
            changed()
        }
        SettingRow("Verlauf löschen", "") {
            prefs.clearHistory()
            changed()
        }
        Spacer(Modifier.height(6.dp))
        BasicText(
            "Keine Werbung: Streams werden direkt geladen, ohne YouTube-Oberfläche.",
            style = TextStyle(color = Muted, fontSize = 14.sp),
        )
    }
}

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(11.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (focused) Brush.verticalGradient(listOf(AccentLight, AccentDark)) else Brush.verticalGradient(listOf(Color(0xFF1E1E26), Color(0xFF1E1E26))))
            .border(1.dp, if (focused) Color(0x73FF5A6E) else Color(0x16FFFFFF), shape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        BasicText(label, modifier = Modifier.weight(1f), style = TextStyle(color = Color.White, fontSize = 17.sp))
        if (value.isNotEmpty()) {
            BasicText(value, style = TextStyle(color = if (focused) Color.White else Muted, fontSize = 17.sp, fontWeight = FontWeight.Bold))
        }
    }
}

private fun formatDuration(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
