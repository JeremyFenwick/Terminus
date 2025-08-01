import java.io.BufferedReader
import java.io.InputStreamReader

fun main() {
  val pathFolders = System.getenv("PATH")?.split(":") ?: listOf()
  val directories =
      pathFolders.map { dir -> Directory(dir) } // Create Directory objects for each path folder

  val reader = BufferedReader(InputStreamReader(System.`in`))

  while (true) {
    print("$ ")
    val command = Parser.parseInput(reader)
    Commander.execute(command, directories) // Execute the command
  }
}
