import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import java.io.File
import java.lang.Exception
import java.lang.ProcessBuilder.Redirect
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class GifGenerator(private val serverURL: String) {

    private val HOST_BLOCKING = "$serverURL/create/gif"

    private val readyFunctions = mutableListOf<() -> Unit>()

    var isReady = false
        private set

    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 100000
        }
    }

    fun onReady(ready: () -> Unit) {
        readyFunctions.add(ready)
    }

    init {
        Timer().scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                runBlocking {
                    isReady = checkServerStatus()
                }

                if (isReady) {
                    this.cancel()
                    readyFunctions.forEach { it() }
                }
            }
        }, 10, 1000)
    }

    private suspend fun checkServerStatus(): Boolean {
        return try {
            val res = client.get("$serverURL/check/status") {
                timeout {
                    requestTimeoutMillis = 1000
                }
            }
            println(res.status)
            res.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    @OptIn(InternalAPI::class)
    fun generateGifBlocking(file: File, model: ThreeDModel, gifOptions: GifOptions = GifOptions()): File {
        runBlocking {
            if (!checkServerStatus()) error("Javascript service is not running")
        }

        val json = Gson().toJson(gifOptions)
        println(json)

        if (file.exists()) file.delete()

        val multipart = MultiPartFormDataContent(
            formData {
                append("options", json)
                append("file", model.model.readBytes(), Headers.build {
                    append(HttpHeaders.ContentType, "application/octet-stream")
                    append(HttpHeaders.ContentDisposition, "filename=${model.model.name}")
                })
            }
        )

        runBlocking {
            client.preparePost(HOST_BLOCKING) { body = multipart }.execute {
                println(it.status)
                val channel: ByteReadChannel = it.body()
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                    while (!packet.isEmpty) {
                        val bytes = packet.readBytes()
                        file.appendBytes(bytes)
                        println("Received ${file.length()} bytes from ${it.contentLength()}")
                    }
                }
                println("A file saved to ${file.path}")
            }
        }
        return file
    }
}