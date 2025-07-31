fun main() {
  while (true) {
    print("$ ")
    val userInput = readln() // Wait for user input
    CommandParser.parse(userInput) // Parse the input command
  }
}
