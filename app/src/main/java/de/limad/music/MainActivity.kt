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

enum class Tab(val label: String) { MUSIK("Musik"), VIDEOS("Videos"), SUCHE("Suche"), EINSTELLUNGEN("Einstellungen") }

data class Shelf(
    val title: String,
    val tracks: List<Track> = emptyList(),
    val loading: Boolean = true,
    val error: Boolean = false,
)

private typealias Loader = suspend () -> List<Track>

class MainActivity : ComponentActivity() {

    private val tab = mutableStateOf(Tab.MUSIK)
    private val shelves = mutableStateListOf<Shelf>()
    private val settingsTick = mutableStateOf(0)
    private var lastQuery: String? = null
    private var loadJob: Job? = null
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        setContent {
            HomeScreen(
                tab = tab.value,
                shelves = shelves,
                prefs = prefs,
                settingsTick = settingsTick.value,
                onTab = ::selectTab,
                onPlay = ::play,
                onSearch = ::askSearch,
                onSettingChanged = { settingsTick.value++ },
            )
        }
        load()
    }

    override fun onResume() {
        super.onResume()
        refreshHistoryShelf()
    }

    private fun selectTab(t: Tab) {
        if (t == tab.value) return
        tab.value = t
        load()
    }

    private fun shelfSpecs(): List<Pair<String, Loader>> = when (tab.value) {
        Tab.MUSIK -> listOf(
            HISTORY_MUSIC to suspend { prefs.history(music = true) },
            "Charts" to suspend { Repo.search("Charts Deutschland", Repo.SONGS, true) },
            "Neu erschienen" to suspend { Repo.search("neue Songs", Repo.SONGS, true) },
            "Deutschpop" to suspend { Repo.search("Deutschpop", Repo.SONGS, true) },
            "Musikvideos" to suspend { Repo.search("offizielles Musikvideo", Repo.MUSIC_VIDEOS, false) },
        )
        Tab.VIDEOS -> listOf(
            HISTORY_VIDEO to suspend { prefs.history(music = false) },
            "Dokumentationen" to suspend { Repo.search("Dokumentation deutsch", Repo.VIDEOS, false) },
            "Natur" to suspend { Repo.search("Natur 4K", Repo.VIDEOS, false) },
            "Nachrichten" to suspend { Repo.search("Nachrichten heute", Repo.VIDEOS, false) },
        )
        Tab.SUCHE -> lastQuery?.let { q ->
            listOf(
                "Songs: $q" to suspend { Repo.search(q, Repo.SONGS, true) },
                "Videos: $q" to suspend { Repo.search(q, Repo.VIDEOS, false) },
            )
        } ?: emptyList()
        Tab.EINSTELLUNGEN -> emptyList()
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

    private fun refreshHistoryShelf() {
        val i = shelves.indexOfFirst { it.title == HISTORY_MUSIC || it.title == HISTORY_VIDEO }
        if (i < 0) return
        val music = shelves[i].title == HISTORY_MUSIC
        shelves[i] = Shelf(shelves[i].title, prefs.history(music), false, false)
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
            setText(lastQuery ?: "")
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

    private fun runSearch(query: String) {
        if (query.isBlank()) return
        lastQuery = query.trim()
        tab.value = Tab.SUCHE
        load()
    }

    private companion object {
        const val HISTORY_MUSIC = "Zuletzt gehört"
        const val HISTORY_VIDEO = "Zuletzt gesehen"
    }
}
