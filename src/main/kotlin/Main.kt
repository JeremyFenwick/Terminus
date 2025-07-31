fun main() {
  // Uncomment this block to pass the first stage
  print("$ ")
  val userInput = readln() // Wait for user input
  CommandParser.parse(userInput) // Parse the input command
}
