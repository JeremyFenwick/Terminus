import kotlinx.coroutines.runBlocking

const val HISTORY_LIMIT = 1000 // Maximum number of history entries

fun main() = runBlocking {
  val pathFolders = System.getenv("PATH")?.split(":") ?: listOf()

  val programs = AvailablePrograms(pathFolders)
  val shell = Shell("$ ", CommandType.commandList() + programs.executables.keys.toList())
  // We start the commander with a prior history if specified by spoofing the correct command
  val commander =
      Commander(shell).also {
        val histFile = System.getenv("HISTFILE")
        if (histFile != null) it.run(CommandGenerator.parseInput("history -r $histFile"), programs)
      }

  while (true) {
    val rawLine = shell.readLine()
    val command = CommandGenerator.parseInput(rawLine)
    commander.run(command, programs)
  }
}
