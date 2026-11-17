package de.limad.music

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import coil.load
import kotlinx.coroutines.launch

/**
 * Plays a queue of tracks. Stream URLs are resolved lazily per track through
 * a ResolvingDataSource (limusic://v?u=… / limusic://a?u=…), so the whole queue
 * lives in ExoPlayer's playlist and next/previous work natively.
 */
class PlayerActivity : ComponentActivity() {

    companion object {
        var queue: List<Track> = emptyList()
        var startIndex = 0
    }

    private lateinit var prefs: Prefs
    private lateinit var player: ExoPlayer
    private lateinit var view: PlayerView
    private lateinit var cover: ImageView
    private lateinit var info: TextView
    private val items = mutableListOf<Track>()
    private var loadingRelated = false
    private var controllerVisible = true

    private val factory by lazy {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
        val resolving = ResolvingDataSource.Factory(http) { spec ->
            val uri = spec.uri
            if (uri.scheme == "limusic") {
                val target = Repo.streamUrl(uri.getQueryParameter("u") ?: "", uri.host, prefs.maxHeight)
                spec.withUri(Uri.parse(target))
            } else {
                spec
            }
        }
        ProgressiveMediaSource.Factory(resolving)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        items.addAll(queue)
        if (items.isEmpty()) {
            finish()
            return
        }

        buildViews()

        player = ExoPlayer.Builder(this).build()
        view.player = player
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = onTrackChanged()

            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(this@PlayerActivity, "Wiedergabefehler: ${error.errorCodeName}", Toast.LENGTH_LONG).show()
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    player.prepare()
                    player.play()
                }
            }
        })

        player.setMediaSources(items.map(::sourceFor), startIndex.coerceIn(0, items.size - 1), 0L)
        player.prepare()
        player.playWhenReady = true
        onTrackChanged()
    }

    private fun buildViews() {
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }

        cover = ImageView(this).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
        root.addView(cover, FrameLayout.LayoutParams(-1, -1).apply { setMargins(dp(80), dp(90), dp(80), dp(150)) })

        view = PlayerView(this).apply {
            setShutterBackgroundColor(Color.TRANSPARENT)
            setBackgroundColor(Color.TRANSPARENT)
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
            controllerShowTimeoutMs = 4000
            setShowNextButton(true)
            setShowPreviousButton(true)
            setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { v ->
                controllerVisible = v == View.VISIBLE
                updateInfoVisibility()
            })
        }
        root.addView(view, FrameLayout.LayoutParams(-1, -1))

        info = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 22f
            setPadding(dp(48), dp(28), dp(48), dp(12))
            setShadowLayer(10f, 0f, 0f, Color.BLACK)
        }
        root.addView(info, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))

        setContentView(root)
        view.requestFocus()
    }

    private fun isAudioOnly(t: Track) = t.music && prefs.musicAudioOnly

    private fun sourceFor(t: Track): MediaSource {
        val meta = MediaMetadata.Builder()
            .setTitle(t.title)
            .setArtist(t.artist)
            .setArtworkUri(t.thumb?.let(Uri::parse))
            .build()
        val u = Uri.encode(t.url)
        val audio = factory.createMediaSource(
            MediaItem.Builder().setUri("limusic://a?u=$u").setMediaId(t.url).setMediaMetadata(meta).build()
        )
        if (isAudioOnly(t)) return audio
        val video = factory.createMediaSource(
            MediaItem.Builder().setUri("limusic://v?u=$u").setMediaId(t.url).setMediaMetadata(meta).build()
        )
        return MergingMediaSource(video, audio)
    }

    private fun current(): Track? = items.getOrNull(player.currentMediaItemIndex)

    private fun onTrackChanged() {
        val t = current() ?: return
        prefs.addHistory(t)
        info.text = if (t.artist.isNotEmpty()) "${t.title}\n${t.artist}" else t.title

        if (isAudioOnly(t)) {
            cover.visibility = View.VISIBLE
            cover.load(t.thumb)
        } else {
            cover.visibility = View.GONE
        }
        updateInfoVisibility()
        maybeAppendRelated(t)
    }

    private fun updateInfoVisibility() {
        val t = current()
        val show = controllerVisible || (t != null && isAudioOnly(t))
        info.visibility = if (show) View.VISIBLE else View.GONE
    }

    /** Autoplay: when the last queue item starts, append related tracks. */
    private fun maybeAppendRelated(t: Track) {
        if (!prefs.autoplay || loadingRelated) return
        if (player.currentMediaItemIndex < player.mediaItemCount - 1) return
        loadingRelated = true
        lifecycleScope.launch {
            val known = items.map { it.url }.toSet()
            val next = runCatching { Repo.related(t.url, t.music) }.getOrDefault(emptyList())
                .filter { it.url !in known }
                .take(10)
            if (next.isNotEmpty()) {
                items.addAll(next)
                player.addMediaSources(next.map(::sourceFor))
            }
            loadingRelated = false
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT -> { player.seekToNextMediaItem(); return true }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> { player.seekToPreviousMediaItem(); return true }
                KeyEvent.KEYCODE_BACK -> if (view.isControllerFullyVisible) { view.hideController(); return true }
            }
        }
        return view.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)
    }

    override fun onStop() {
        super.onStop()
        if (::player.isInitialized) player.pause()
    }

    override fun onDestroy() {
        if (::player.isInitialized) player.release()
        super.onDestroy()
    }
}
