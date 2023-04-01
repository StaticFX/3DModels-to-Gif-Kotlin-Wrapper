import java.io.File

class ThreeDModel(file: File) {
    val model: File

    private val extensions = listOf("stl", "obj")

    init {
        if (file.extension !in extensions) error("Unsupported file format")
        model = file
    }
}