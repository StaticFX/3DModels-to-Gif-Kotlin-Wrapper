import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import java.io.File
import java.util.concurrent.CompletableFuture

object GifGenerator {

    var HOST = "http://localhost:3000/create/gif"

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    @OptIn(InternalAPI::class)
    suspend fun generateGifBlocking(model: ThreeDModel, gifOptions: GifOptions = GifOptions()): File? {

        val json = Gson().toJson(gifOptions)
        println(json)

        val multipart = MultiPartFormDataContent(
            formData {
                append("options", json)
                append("file", model.model.readBytes(), Headers.build {
                    append(HttpHeaders.ContentType, "application/octet-stream")
                    append(HttpHeaders.ContentDisposition, "filename=name.stl")
                })
            }
        )

        val response = client.post(HOST) {
            body = multipart
        }

        println(response)
        return null
    }

    suspend fun generateGifAsync(model: ThreeDModel, progress: (Int) -> Unit): CompletableFuture<File> {
        TODO()
    }


}