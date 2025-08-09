object CommandGenerator {
  private val stdOutSymbols = listOf(">", "1>", ">>", "1>>")
  private val stdErrSymbols = listOf("2>", "2>>")

  fun parseInput(line: String): Command {
    val input = rawParser(line)
    return inputToCommand(input, line)
  }

  fun inputToCommand(input: List<String>, rawText: String? = null): Command {
    if (input.isEmpty()) {
      return Command(CommandType.NOTBUILTIN, input, rawText)
    }
    // Check if this is a pipe command
    if (input.contains("|")) return Command(CommandType.PIPE, input, rawText)
    // Output the command to either standard output or a file
    val redirectIndex = input.indexOfFirst { it in stdOutSymbols || it in stdErrSymbols }
    val commandList = if (redirectIndex != -1) input.subList(0, redirectIndex) else input
    val stdOut = getRedirect(input, stdOutSymbols)
    val stdErr = getRedirect(input, stdErrSymbols)
    // Extract the command type and sub-command type
    val commandType = CommandType.fromInput(input[0])
    return Command(commandType, commandList, rawText, stdOut, stdErr)
  }

  fun commandSplitter(input: List<String>): List<Command> {
    val commands = mutableListOf<Command>()
    val currentCommand = mutableListOf<String>()
    for (token in input) {
      if (token == " " && currentCommand.isEmpty()) {
        // If we hit a pipe and the current command is empty, we skip it
        continue
      }
      if (token == "|") {
        // If we hit a pipe, we finalize the current command
        if (currentCommand.isNotEmpty()) {
          commands.add(inputToCommand(currentCommand.toList()))
          currentCommand.clear()
        }
      } else {
        // Otherwise, we add the token to the current command
        currentCommand.add(token)
      }
    }
    // Add the last command if it exists
    if (currentCommand.isNotEmpty()) {
      commands.add(inputToCommand(currentCommand))
    }
    return commands
  }

  private fun getRedirect(input: List<String>, sep: List<String>): Output? {
    val redirectIndex = input.indexOfFirst { it in sep }
    // No redirection found, return standard output
    if (redirectIndex == -1 || redirectIndex + 2 > input.size) return null
    val appendMode = input[redirectIndex].endsWith(">>")
    val outputFile = java.io.File(input[redirectIndex + 2])
    return Output(outputFile, appendMode)
  }

  private fun rawParser(input: String): List<String> {
    val result = mutableListOf<String>()
    val buffer = StringBuilder()
    var index = 0

    // Function to flush the buffer to the result list
    fun flushBuffer() {
      if (buffer.isNotEmpty()) {
        result.add(buffer.toString())
        buffer.clear()
      }
    }
    // Function to get the next character from the input
    fun nextChar(): Char {
      index++
      if (index >= input.length)
          throw IndexOutOfBoundsException("Unexpected end of input. Input was '$input'")
      return input[index]
    }

    // Function to read a single-quoted string
    fun readSingleQuotedString() {
      // Read until the next single quote
      var nextChar = nextChar()
      while (nextChar != '\'') {
        buffer.append(nextChar)
        nextChar = nextChar()
      }
      flushBuffer()
    }

    // Function to read a double-quoted string
    fun readDoubleQuotedString() {
      // Read until the next double quote
      var nextChar = nextChar()
      while (nextChar != '"') {
        // Handle escaped characters
        if (nextChar == '\\') {
          // If the next character is escapable, append that character instead
          val lookAhead = nextChar()
          if (lookAhead == '"' ||
              lookAhead == '\\' ||
              lookAhead == '$' ||
              lookAhead == '`' ||
              lookAhead == '\n') {
            nextChar = lookAhead
          } else {
            // If it's not an escaped quote or backslash, just append the current character
            // We skip reading the next character at the end as we normally would
            buffer.append(nextChar)
            nextChar = lookAhead
            continue
          }
        }
        buffer.append(nextChar)
        nextChar = nextChar()
      }
      flushBuffer()
    }

    // Read input until a newline character is encountered
    while (index < input.length) {
      val char = input[index]
      when (char) {
        ' ' -> {
          flushBuffer()
          if (result.last() != " ") result.add(" ")
        }

        '\\' -> {
          // Read the next character and append it to the buffer
          val nextChar = nextChar()
          buffer.append(nextChar)
        }

        '\'' -> readSingleQuotedString()
        '"' -> readDoubleQuotedString()
        else -> buffer.append(char)
      }
      index++
    }
    // If the buffer is not empty add it
    flushBuffer()
    return result
  }
}
