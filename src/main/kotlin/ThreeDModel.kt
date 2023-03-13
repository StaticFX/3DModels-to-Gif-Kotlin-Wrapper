import java.io.File

class ThreeDModel(file: File) {
    val model: File

    init {
        if (file.extension != "stl") error("Unsupported file format")

        model = file
    }
}