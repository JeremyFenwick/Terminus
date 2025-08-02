fun main() {
  val pathFolders = System.getenv("PATH")?.split(":") ?: listOf()
  val directories = AvailablePrograms(pathFolders)
  val shell = Shell("$ ")
  //  val reader = BufferedReader(InputStreamReader(System.`in`))

  while (true) {
    val line = shell.readLine() // Read a line from the shell
    val command = Parser.parseInput(line)
    Commander.execute(command, directories) // Execute the command
  }
}
