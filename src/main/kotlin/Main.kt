fun main() {
  while (true) {
    print("$ ")
    val userInput = readln() // Wait for user input
    val command = Commander.parse(userInput) // Parse the input command
    Commander.execute(command) // Parse the input command
  }
}
