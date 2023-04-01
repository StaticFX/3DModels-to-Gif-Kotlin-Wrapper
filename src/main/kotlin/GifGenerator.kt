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
import java.util.*
import java.util.concurrent.CompletableFuture


/**
 * Wrapper class using to generate gif from 3d modelsØ
 *
 * @param serverURL of the service to use, if on localhost, http://localhost:3000
 */
class GifGenerator(private val serverURL: String) {

    private val HOST_BLOCKING = "$serverURL/create/gif"

    private val readyFunctions = mutableListOf<() -> Unit>()

    /**
     * If the service is ready to use or not
     */
    var isReady = false
        private set

    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 100000
        }
    }

    /**
     * Executes the supplied functions as soon as the service is ready
     * @param ready function
     */
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

    /**
     * Checks the server status of the service
     * @return boolean whether the service is online or not
     */
    suspend fun checkServerStatus(): Boolean {
        return try {
            val res = client.get("$serverURL/check/status") {
                timeout {
                    requestTimeoutMillis = 1000
                }
            }
            res.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Generates a gif blocking from the supplied model
     *
     * @param file to generate the gif to, this file will always be deleted before a gif is being written
     * @param model to generate the model from
     * @param gifOptions the generation options
     * @return file of the generated gif
     */
    @OptIn(InternalAPI::class)
    fun generateGifBlocking(file: File, model: ThreeDModel, gifOptions: GifOptions = GifOptions()): File {
        runBlocking {
            if (!checkServerStatus()) error("Javascript service is not running")
        }

        val json = Gson().toJson(gifOptions)

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
                if (it.status != HttpStatusCode.OK) error("Unexpected error while generating gif: ${it.status} ${it.bodyAsText()}")
                val channel: ByteReadChannel = it.body()
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                    while (!packet.isEmpty) {
                        val bytes = packet.readBytes()
                        file.appendBytes(bytes)
                    }
                }
            }
        }
        return file
    }

    /**
     * Generates a gif non-blocking
     *
     * @param file to generate the gif to, will always be deleted before the new gif being written
     * @param model to generate  the gif from
     * @param gifOptions options to generate
     * @param done functions executed as soon as the file is finished
     * @return future including the finished file
     */
    @OptIn(InternalAPI::class, DelicateCoroutinesApi::class)
    fun generateGif(file: File, model: ThreeDModel, gifOptions: GifOptions = GifOptions(), done: (File) -> Unit): CompletableFuture<File> {
        val future = CompletableFuture<File>()
        GlobalScope.async {
            if (!checkServerStatus()) error("Javascript service is not running")

            val json = Gson().toJson(gifOptions)

            if (file.exists()) file.delete()

            val multipart = MultiPartFormDataContent(
                formData {
                    append("options", json)
                    append("file", model.model.readBytes(), Headers.build {
                        append(HttpHeaders.ContentType, "application/octet-stream")
                        append(HttpHeaders.ContentDisposition, "filename=${model.model.name}")
                    })
                })
            client.preparePost(HOST_BLOCKING) { body = multipart }.execute {
                if (it.status != HttpStatusCode.OK) error("Unexpected error while generating gif: ${it.status} ${it.bodyAsText()}")
                val channel: ByteReadChannel = it.body()
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                    while (!packet.isEmpty) {
                        val bytes = packet.readBytes()
                        file.appendBytes(bytes)
                    }
                }
            }

            done(file)
            future.complete(file)
        }
        return future
    }
}