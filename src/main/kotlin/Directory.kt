import java.io.File

class Directory(val path: String) {
  val executables: MutableMap<String, File> = mutableMapOf()

  init {
    generateExecutables()
  }

  private fun generateExecutables() {
    val dir = File(path)
    if (dir.exists() && dir.isDirectory) {
      dir.listFiles()?.forEach { file ->
        if (file.isFile && file.canExecute()) {
          executables.put(file.name, file)
        }
      }
    }
  }
}
