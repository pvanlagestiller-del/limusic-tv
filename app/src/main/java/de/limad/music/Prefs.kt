package de.limad.music

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Prefs(ctx: Context) {

    private val sp = ctx.getSharedPreferences("limusic", Context.MODE_PRIVATE)

    var maxHeight: Int
        get() = sp.getInt("max_height", 1080)
        set(v) = sp.edit().putInt("max_height", v).apply()

    var musicAudioOnly: Boolean
        get() = sp.getBoolean("music_audio_only", true)
        set(v) = sp.edit().putBoolean("music_audio_only", v).apply()

    var autoplay: Boolean
        get() = sp.getBoolean("autoplay", true)
        set(v) = sp.edit().putBoolean("autoplay", v).apply()

    fun history(music: Boolean): List<Track> = load().filter { it.music == music }

    fun addHistory(t: Track) {
        val list = load().filterNot { it.url == t.url }.toMutableList()
        list.add(0, t)
        save(list.take(40))
    }

    fun clearHistory() = sp.edit().remove("history").apply()

    private fun load(): List<Track> {
        val raw = sp.getString("history", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Track(
                    title = o.getString("title"),
                    artist = o.optString("artist"),
                    url = o.getString("url"),
                    thumb = o.optString("thumb").ifEmpty { null },
                    durationSec = o.optLong("duration", -1),
                    music = o.optBoolean("music"),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun save(list: List<Track>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("title", it.title)
                    .put("artist", it.artist)
                    .put("url", it.url)
                    .put("thumb", it.thumb ?: "")
                    .put("duration", it.durationSec)
                    .put("music", it.music)
            )
        }
        sp.edit().putString("history", arr.toString()).apply()
    }

    companion object {
        val RESOLUTIONS = listOf(360, 480, 720, 1080, 1440, 2160)
    }
}
