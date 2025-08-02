fun main() {
  val pathFolders = System.getenv("PATH")?.split(":") ?: listOf()
  val programs = AvailablePrograms(pathFolders)
  val shell = Shell("$ ", CommandType.commandList() + programs.executables.keys.toList())

  while (true) {
    val line = shell.readLine() // Read a line from the shell
    val command = Parser.parseInput(line)
    Commander.execute(command, programs) // Execute the command
  }
}
