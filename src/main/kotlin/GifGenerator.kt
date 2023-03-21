import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.CompletableFuture

object GifGenerator {

    private var HOST_BLOCKING = "http://localhost:3000/create/gif"

    private val client = HttpClient(CIO)

    suspend fun checkServerStatus(): Boolean {
        val res = client.get("http://localhost:3000/status/status")
        println(res.status)
        return res.status == HttpStatusCode.OK
    }

    @OptIn(InternalAPI::class)
    fun generateGifBlocking(file: File, model: ThreeDModel, gifOptions: GifOptions = GifOptions()): File {
        val json = Gson().toJson(gifOptions)
        println(json)

        if (file.exists()) file.delete()

        val multipart = MultiPartFormDataContent(
            formData {
                append("options", json)
                append("file", model.model.readBytes(), Headers.build {
                    append(HttpHeaders.ContentType, "application/octet-stream")
                    append(HttpHeaders.ContentDisposition, "filename=${file.name}")
                })
            }
        )

        runBlocking {
            client.preparePost(HOST_BLOCKING) { body = multipart }.execute {
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