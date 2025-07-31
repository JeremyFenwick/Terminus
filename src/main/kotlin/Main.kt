fun main() {
  while (true) {
    print("$ ")
    val userInput = readln() // Wait for user input
    Commander.execute(userInput) // Parse the input command
  }
}
