import java.io.File

suspend fun main() {

    val model = ThreeDModel(File("src/main/resources/infinity.stl"))
    val location = File("src/main/resources/gif.gif")

    if (!GifGenerator.checkServerStatus()) error("service not running")

    GifGenerator.generateGifBlocking(location, model)
}