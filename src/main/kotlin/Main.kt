fun main() {
  val pathFolders = System.getenv("PATH")?.split(":") ?: listOf()
  val directories =
      pathFolders.map { dir -> Directory(dir) } // Create Directory objects for each path folder

  while (true) {
    print("$ ")
    val userInput = readln() // Wait for user input
    val command = Commander.parse(userInput) // Parse the input command
    Commander.execute(command, directories) // Execute the command
  }
}
