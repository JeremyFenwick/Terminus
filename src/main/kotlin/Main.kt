fun main() {
  val pathFolders = System.getenv("PATH")?.split(":") ?: listOf()
  val programs = AvailablePrograms(pathFolders)
  val shell = Shell("$ ", CommandType.commandList() + programs.executables.keys.toList())

  while (true) {
    val rawLine = shell.readLine()
    val command = CommandGenerator.parseInput(rawLine)
    Commander.execute(command, programs)
  }
}
