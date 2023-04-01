import kotlinx.serialization.Serializable

@Serializable
data class GifOptions(var name: String = "model",
                      var backgroundColor: Int = 0x0,
                      var transparent: Boolean = true,
                      var objectColor: Int = 0x909190,
                      var cameraRotationAxis: RotationAxis = RotationAxis.X,
                      var axisSpace: AxisSpace = AxisSpace.WORLD,
                      var initialRotation: Rotation = Rotation(),
                      var width: Int = 512,
                      var height: Int = 512,
                      var loop: Int = 0,
                      var delay: Int = 100,
                      var anglePerFrame: Int = 10,
                      var label: String = "")


@Serializable
data class Rotation(var x: Double = 0.0,
                    var y: Double = 0.0,
                    var z: Double = 0.0)