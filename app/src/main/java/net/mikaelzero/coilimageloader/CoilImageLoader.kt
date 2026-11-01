package net.mikaelzero.coilimageloader

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.example.c001apk.compose.util.CookieUtil
import net.mikaelzero.mojito.loader.ImageLoader
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Minimal OkHttp-backed implementation of Mojito's [ImageLoader].
 *
 * The upstream c001apk-compose project uses a private `coilimageLoader` module
 * which is not published. This implementation keeps the same public entry point
 * so the app can compile and still display remote images in Mojito's viewer.
 *
 * Important: Coolapk's image CDN rejects requests without a Coolapk user-agent
 * (and private images additionally need the login cookie). The grid thumbnails
 * are loaded through Coil with those headers, but the full-screen viewer goes
 * through this class. Without the headers the download silently failed and the
 * viewer stayed on its black background — the "click image, black screen" bug.
 */
class CoilImageLoader private constructor(
    private val context: Context,
    okHttpClient: OkHttpClient? = null,
) : ImageLoader {

    private val client = okHttpClient ?: OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val executor = Executors.newCachedThreadPool()
    private val runningCalls = ConcurrentHashMap<Int, okhttp3.Call>()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun loadImage(
        requestId: Int,
        uri: Uri,
        onlyRetrieveFromCache: Boolean,
        callback: ImageLoader.Callback,
    ) {
        val url = uri.toString()
        val cacheFile = cacheFileFor(url)
        if (cacheFile.exists() && cacheFile.length() > 0L) {
            callback.onSuccess(cacheFile)
            return
        }
        if (onlyRetrieveFromCache) {
            callback.onFail(IllegalStateException("Image not found in cache: $url"))
            return
        }

        callback.onStart()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", CookieUtil.userAgent.ifEmpty { DEFAULT_USER_AGENT })
            .header("Accept", "image/*,*/*;q=0.8")
            .apply {
                if (CookieUtil.token.isNotEmpty()) {
                    header(
                        "Cookie",
                        "uid=${CookieUtil.uid}; username=${CookieUtil.username}; token=${CookieUtil.token}"
                    )
                }
            }
            .build()
        val call = client.newCall(request)
        runningCalls[requestId] = call
        executor.execute {
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("HTTP ${response.code}: $url")
                    }
                    val body = response.body ?: throw IllegalStateException("Empty body: $url")
                    cacheFile.parentFile?.mkdirs()
                    val tmpFile = File(cacheFile.parentFile, cacheFile.name + ".tmp")
                    body.byteStream().use { input ->
                        tmpFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    if (tmpFile.length() <= 0L) {
                        tmpFile.delete()
                        throw IllegalStateException("Empty image: $url")
                    }
                    if (cacheFile.exists()) cacheFile.delete()
                    tmpFile.renameTo(cacheFile)
                }
                runningCalls.remove(requestId)
                mainHandler.post {
                    callback.onFinish()
                    callback.onSuccess(cacheFile)
                }
            } catch (e: Exception) {
                runningCalls.remove(requestId)
                mainHandler.post { callback.onFail(e) }
            }
        }
    }

    override fun prefetch(uri: Uri) {
        loadImage(uri.toString().hashCode(), uri, false, object : ImageLoader.Callback {
            override fun onStart() = Unit
            override fun onProgress(progress: Int) = Unit
            override fun onFinish() = Unit
            override fun onSuccess(image: File) = Unit
            override fun onFail(error: Exception) = Unit
        })
    }

    override fun cancel(requestId: Int) {
        runningCalls.remove(requestId)?.cancel()
    }

    override fun cancelAll() {
        runningCalls.values.forEach { it.cancel() }
        runningCalls.clear()
    }

    override fun cleanCache() {
        File(context.cacheDir, "mojito_images").deleteRecursively()
    }

    private fun cacheFileFor(url: String): File {
        val dir = File(context.cacheDir, "mojito_images")
        val ext = url.substringBefore('?').substringAfterLast('.', "jpg")
            .takeIf { it.length in 2..5 && it.all(Char::isLetterOrDigit) } ?: "jpg"
        val name = url.hashCode().toUInt().toString(16) + "." + ext
        return File(dir, name)
    }

    companion object {
        private const val DEFAULT_USER_AGENT =
            "Dalvik/2.1.0 (Linux; U; Android 12; 2201122C Build/SKQ1.211103.001) (#Build; Xiaomi; 2201122C; SKQ1.211103.001; 12) +CoolMarket/16.0.0"

        @JvmStatic
        @JvmOverloads
        fun with(context: Context, okHttpClient: OkHttpClient? = null): CoilImageLoader {
            return CoilImageLoader(context.applicationContext, okHttpClient)
        }
    }
}
