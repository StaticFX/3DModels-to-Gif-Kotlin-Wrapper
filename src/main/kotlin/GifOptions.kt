import kotlinx.serialization.Serializable

@Serializable
data class GifOptions(val name: String = "model",
                      val background: Int = 0x0,
                      val transparent: Boolean = true,
                      val modelColor: Int = 0xFF00FF,
                      val width: Int = 1024,
                      val height: Int = 1024,
                      val loop: Int = 0,
                      val delay: Int = 100,
                      val anglePerFrame: Int = 10)