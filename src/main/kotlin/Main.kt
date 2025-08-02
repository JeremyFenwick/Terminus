fun main() {
  val pathFolders = System.getenv("PATH")?.split(":") ?: listOf()
  val directories = AvailablePrograms(pathFolders)
  val shell = Shell("$ ", CommandType.commandList() + directories.executables.keys.toList())

  while (true) {
    val line = shell.readLine() // Read a line from the shell
    val command = Parser.parseInput(line)
    Commander.execute(command, directories) // Execute the command
  }
}
