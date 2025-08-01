import java.io.File

class Directory(val path: String) {
  val executables: MutableList<File> = mutableListOf()

  init {
    generateExecutables()
  }

  fun generateExecutables() {
    val dir = File(path)
    if (dir.exists() && dir.isDirectory) {
      dir.listFiles()?.forEach { file ->
        if (file.isFile && file.canExecute()) {
          executables.add(file)
        }
      }
    }
  }
}
