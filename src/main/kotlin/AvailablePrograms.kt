import java.io.File

class AvailablePrograms(val paths: List<String>) {
  val executables: MutableMap<String, File> = mutableMapOf()

  init {
    generateExecutables()
  }

  private fun generateExecutables() {
    fun exploreDirectory(path: String) {
      val dir = File(path)
      if (!dir.exists() || !dir.isDirectory) return
      dir.listFiles()?.forEach { file ->
        if (file.isFile && file.canExecute()) {
          executables.put(file.name, file)
        }
      }
    }
    paths.map { exploreDirectory(it) }
  }
}
