package com.tvsencilla.iptv.data.net

import com.tvsencilla.iptv.domain.provider.ProviderException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.PushbackInputStream
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException

/**
 * Downloads playlists and guides without ever materialising them. Both M3U playlists and XMLTV
 * guides routinely run to hundreds of megabytes, which no TV box can hold in memory.
 */
@Singleton
class StreamDownloader @Inject constructor(
    private val client: OkHttpClient,
) {

    suspend fun <T> readLines(url: String, consume: (Sequence<String>) -> T): T =
        readStream(url) { stream ->
            BufferedReader(stream.reader()).use { reader -> consume(reader.lineSequence()) }
        }

    suspend fun <T> readStream(url: String, consume: (InputStream) -> T): T =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw response.code.toProviderException()
                    val body = response.body ?: throw ProviderException(ProviderException.Reason.EMPTY)
                    consume(body.byteStream().maybeGunzip())
                }
            } catch (e: ProviderException) {
                throw e
            } catch (e: UnknownHostException) {
                throw ProviderException(ProviderException.Reason.NO_NETWORK, e)
            } catch (e: SocketTimeoutException) {
                throw ProviderException(ProviderException.Reason.UNREACHABLE, e)
            } catch (e: IOException) {
                throw ProviderException(ProviderException.Reason.UNREACHABLE, e)
            } catch (e: IllegalArgumentException) {
                throw ProviderException(ProviderException.Reason.UNREACHABLE, e)
            }
        }

    /**
     * Guides are often published as plain `.gz` files rather than with a gzip Content-Encoding,
     * in which case OkHttp hands them over compressed. Sniffing the magic bytes covers both.
     */
    private fun InputStream.maybeGunzip(): InputStream {
        val pushback = PushbackInputStream(this, GZIP_MAGIC.size)
        val header = ByteArray(GZIP_MAGIC.size)
        val read = pushback.read(header)
        if (read > 0) pushback.unread(header, 0, read)
        val isGzip = read == GZIP_MAGIC.size && header[0] == GZIP_MAGIC[0] && header[1] == GZIP_MAGIC[1]
        return if (isGzip) GZIPInputStream(pushback) else pushback
    }

    private companion object {
        val GZIP_MAGIC = byteArrayOf(0x1f, 0x8b.toByte())
    }
}

fun Int.toProviderException(): ProviderException = when (this) {
    401, 403 -> ProviderException(ProviderException.Reason.BAD_CREDENTIALS)
    404, 410 -> ProviderException(ProviderException.Reason.EMPTY)
    else -> ProviderException(ProviderException.Reason.UNREACHABLE)
}

/** Turns any failure from the network layer into something the UI can phrase plainly. */
fun Throwable.toProviderException(): ProviderException = when (this) {
    is ProviderException -> this
    is HttpException -> code().toProviderException()
    is UnknownHostException -> ProviderException(ProviderException.Reason.NO_NETWORK, this)
    is SocketTimeoutException -> ProviderException(ProviderException.Reason.UNREACHABLE, this)
    is IOException -> ProviderException(ProviderException.Reason.UNREACHABLE, this)
    else -> ProviderException(ProviderException.Reason.UNKNOWN, this)
}
