package de.limad.music

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class Shelf(
    val title: String,
    val tracks: List<Track> = emptyList(),
    val loading: Boolean = true,
    val error: Boolean = false,
)

private typealias Loader = suspend () -> List<Track>

class MainActivity : ComponentActivity() {

    private val nav = mutableStateOf(Nav.START)
    private val mode = mutableStateOf(Mode.MUSIK)
    private val genre = mutableStateOf<String?>(null)
    private val query = mutableStateOf<String?>(null)
    private val shelves = mutableStateListOf<Shelf>()
    private val settingsTick = mutableStateOf(0)
    private var loadJob: Job? = null
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        setContent {
            val genres = if (mode.value == Mode.MUSIK) MUSIC_GENRES else VIDEO_GENRES
            HomeScreen(
                nav = nav.value,
                mode = mode.value,
                genres = genres,
                genre = genre.value,
                query = query.value,
                featured = featured(),
                shelves = shelves,
                prefs = prefs,
                settingsTick = settingsTick.value,
                onNav = ::selectNav,
                onMode = ::selectMode,
                onGenre = ::selectGenre,
                onPlay = ::play,
                onSearch = ::askSearch,
                onSettingChanged = { settingsTick.value++ },
            )
        }
        load()
    }

    override fun onResume() {
        super.onResume()
        refreshHistoryShelves()
    }

    /** Featured card on the start page: first track of the first non-history shelf. */
    private fun featured(): Track? {
        if (nav.value != Nav.START) return null
        return shelves.firstOrNull { !it.title.startsWith(HISTORY_PREFIX) && it.tracks.isNotEmpty() }
            ?.tracks?.firstOrNull()
    }

    private fun selectNav(n: Nav) {
        if (n == nav.value) return
        nav.value = n
        load()
    }

    private fun selectMode(m: Mode) {
        if (m == mode.value) return
        mode.value = m
        genre.value = null
        load()
    }

    private fun selectGenre(g: String) {
        genre.value = g
        load()
    }

    private fun shelfSpecs(): List<Pair<String, Loader>> {
        val music = mode.value == Mode.MUSIK
        return when (nav.value) {
            Nav.START -> if (music) listOf(
                HISTORY_MUSIC to suspend { prefs.history(music = true) },
                "Charts" to suspend { Repo.search("Charts Deutschland", Repo.SONGS, true) },
                "Neu erschienen" to suspend { Repo.search("neue Songs", Repo.SONGS, true) },
                "Deutschpop" to suspend { Repo.search("Deutschpop", Repo.SONGS, true) },
                "Musikvideos" to suspend { Repo.search("offizielles Musikvideo", Repo.MUSIC_VIDEOS, false) },
            ) else listOf(
                HISTORY_VIDEO to suspend { prefs.history(music = false) },
                "Trends" to suspend { Repo.search("Trends Deutschland", Repo.VIDEOS, false) },
                "Dokumentationen" to suspend { Repo.search("Dokumentation deutsch", Repo.VIDEOS, false) },
                "Natur in 4K" to suspend { Repo.search("Natur 4K", Repo.VIDEOS, false) },
                "Nachrichten" to suspend { Repo.search("Nachrichten heute", Repo.VIDEOS, false) },
            )
            Nav.ENTDECKEN -> {
                val g = genre.value ?: (if (music) MUSIC_GENRES else VIDEO_GENRES).first()
                if (genre.value == null) genre.value = g
                if (music) listOf(
                    "$g – Songs" to suspend { Repo.search("$g Hits", Repo.SONGS, true) },
                    "$g – Musikvideos" to suspend { Repo.search("$g Musikvideo", Repo.MUSIC_VIDEOS, false) },
                    "$g – Neu" to suspend { Repo.search("$g neu", Repo.SONGS, true) },
                ) else listOf(
                    "$g – Beliebt" to suspend { Repo.search(g, Repo.VIDEOS, false) },
                    "$g – Neu" to suspend { Repo.search("$g neu", Repo.VIDEOS, false) },
                )
            }
            Nav.SUCHE -> query.value?.let { q ->
                if (music) listOf(
                    "Songs" to suspend { Repo.search(q, Repo.SONGS, true) },
                    "Musikvideos" to suspend { Repo.search(q, Repo.MUSIC_VIDEOS, false) },
                ) else listOf(
                    "Videos" to suspend { Repo.search(q, Repo.VIDEOS, false) },
                )
            } ?: emptyList()
            Nav.BIBLIOTHEK -> listOf(
                HISTORY_MUSIC to suspend { prefs.history(music = true) },
                HISTORY_VIDEO to suspend { prefs.history(music = false) },
            )
            Nav.EINSTELLUNGEN -> emptyList()
        }
    }

    private fun load() {
        loadJob?.cancel()
        shelves.clear()
        val specs = shelfSpecs()
        specs.forEach { shelves.add(Shelf(it.first)) }
        loadJob = lifecycleScope.launch {
            specs.forEachIndexed { i, (title, loader) ->
                launch {
                    val result = runCatching { loader() }
                    if (i < shelves.size && shelves[i].title == title) {
                        shelves[i] = Shelf(title, result.getOrDefault(emptyList()), false, result.isFailure)
                    }
                }
            }
        }
    }

    private fun refreshHistoryShelves() {
        shelves.forEachIndexed { i, s ->
            if (s.title == HISTORY_MUSIC || s.title == HISTORY_VIDEO) {
                shelves[i] = Shelf(s.title, prefs.history(s.title == HISTORY_MUSIC), false, false)
            }
        }
    }

    private fun play(tracks: List<Track>, index: Int) {
        PlayerActivity.queue = tracks
        PlayerActivity.startIndex = index
        startActivity(Intent(this, PlayerActivity::class.java))
    }

    private fun askSearch() {
        val input = EditText(this).apply {
            hint = "Song, Künstler oder Video"
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setText(query.value ?: "")
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Suchen")
            .setView(input)
            .setPositiveButton("Suchen") { _, _ -> runSearch(input.text.toString()) }
            .setNegativeButton("Abbrechen", null)
            .create()
        input.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEARCH) {
                dialog.dismiss()
                runSearch(input.text.toString())
                true
            } else {
                false
            }
        }
        dialog.show()
    }

    private fun runSearch(q: String) {
        if (q.isBlank()) return
        query.value = q.trim()
        nav.value = Nav.SUCHE
        load()
    }

    private companion object {
        const val HISTORY_PREFIX = "Zuletzt"
        const val HISTORY_MUSIC = "Zuletzt gehört"
        const val HISTORY_VIDEO = "Zuletzt gesehen"
        val MUSIC_GENRES = listOf("Pop", "Deutschpop", "Rock", "Hip-Hop", "Schlager", "Electronic", "Chill", "Workout", "80er", "90er", "2000er")
        val VIDEO_GENRES = listOf("Doku", "Natur", "Reisen", "Kochen", "Wissen", "Sport", "Comedy", "Live-Konzerte")
    }
}
