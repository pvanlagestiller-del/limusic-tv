package de.limad.music

import android.app.Application
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import java.util.concurrent.TimeUnit

class LiMusicApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        NewPipe.init(NpDownloader(client), Localization("de", "DE"), ContentCountry("DE"))
    }
}
