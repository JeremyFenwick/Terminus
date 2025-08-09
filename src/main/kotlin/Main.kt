import kotlinx.coroutines.runBlocking

const val HISTORY_LIMIT = 1000 // Maximum number of history entries

fun main() = runBlocking {
  val pathFolders = System.getenv("PATH")?.split(":") ?: listOf()
  val programs = AvailablePrograms(pathFolders)
  val shell = Shell("$ ", CommandType.commandList() + programs.executables.keys.toList())
  val commander = Commander(shell)

  while (true) {
    val rawLine = shell.readLine()
    val command = CommandGenerator.parseInput(rawLine)
    commander.run(command, programs)
  }
}
