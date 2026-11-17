package de.limad.music

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.VideoStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

data class Track(
    val title: String,
    val artist: String,
    val url: String,
    val thumb: String?,
    val durationSec: Long,
    val music: Boolean,
)

object Repo {

    const val SONGS = YoutubeSearchQueryHandlerFactory.MUSIC_SONGS
    const val MUSIC_VIDEOS = YoutubeSearchQueryHandlerFactory.MUSIC_VIDEOS
    const val VIDEOS = YoutubeSearchQueryHandlerFactory.VIDEOS

    private val yt get() = ServiceList.YouTube
    private val cache = ConcurrentHashMap<String, StreamInfo>()

    suspend fun search(query: String, filter: String, music: Boolean): List<Track> = withContext(Dispatchers.IO) {
        val handler = yt.searchQHFactory.fromQuery(query, listOf(filter), "")
        SearchInfo.getInfo(yt, handler).relatedItems
            .filterIsInstance<StreamInfoItem>()
            .map { it.toTrack(music) }
    }

    suspend fun related(url: String, music: Boolean): List<Track> = withContext(Dispatchers.IO) {
        info(url).relatedItems.filterIsInstance<StreamInfoItem>().map { it.toTrack(music) }
    }

    /** Blocking; called from ExoPlayer's loader thread. kind = "v" (video only) or "a" (audio). */
    fun streamUrl(url: String, kind: String?, maxHeight: Int): String {
        val info = info(url)
        return if (kind == "v") {
            pickVideo(info, maxHeight)?.content ?: throw IOException("No video stream")
        } else {
            pickAudio(info)?.content ?: throw IOException("No audio stream")
        }
    }

    private fun info(url: String): StreamInfo = cache.getOrPut(url) { StreamInfo.getInfo(yt, url) }

    private fun pickVideo(info: StreamInfo, maxHeight: Int): VideoStream? {
        val candidates = info.videoOnlyStreams.filter {
            it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.height in 1..maxHeight
        }
        return candidates.sortedWith(
            compareBy<VideoStream>({ it.height }, { if (it.format == MediaFormat.MPEG_4) 1 else 0 })
        ).lastOrNull()
    }

    private fun pickAudio(info: StreamInfo): AudioStream? {
        val candidates = info.audioStreams.filter {
            it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP
        }
        return candidates.sortedWith(
            compareBy<AudioStream>({ if (it.format == MediaFormat.M4A) 1 else 0 }, { it.averageBitrate })
        ).lastOrNull()
    }

    private fun StreamInfoItem.toTrack(music: Boolean) = Track(
        title = name,
        artist = uploaderName ?: "",
        url = url,
        thumb = thumbnails.maxByOrNull { it.height }?.url,
        durationSec = duration,
        music = music,
    )
}
