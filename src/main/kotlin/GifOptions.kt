import kotlinx.serialization.Serializable

@Serializable
data class GifOptions(val name: String = "model",
                      val background: Int = 0x0,
                      val transparent: Boolean = true,
                      val objectColor: Int = 0x909190,
                      val rotationAxis: RotationAxis = RotationAxis.X,
                      val width: Int = 512,
                      val height: Int = 512,
                      val loop: Int = 0,
                      val delay: Int = 100,
                      val anglePerFrame: Int = 10)