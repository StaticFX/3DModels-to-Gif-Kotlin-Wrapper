import java.io.File

suspend fun main() {


    val model = ThreeDModel(File("src/main/resources/infinity.stl"))
    GifGenerator.generateGifBlocking(model)

}