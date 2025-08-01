import java.io.BufferedReader

object Parser {
  fun parseInput(input: String): Command {
    val parts = input.split(" ")
    if (parts.isEmpty()) {
      return Command(CommandType.UNKNOWN, parts)
    }
    // Extract the command type and sub-command type
    val commandType = CommandType.fromInput(parts[0])
    return Command(commandType, parts)
  }

  fun parseInput(reader: BufferedReader): Command {
    val input = inputReader(reader)
    return inputToCommand(input)
  }

  private fun inputToCommand(input: List<String>): Command {
    if (input.isEmpty()) {
      return Command(CommandType.UNKNOWN, input)
    }
    // Extract the command type and sub-command type
    val commandType = CommandType.fromInput(input[0])
    return Command(commandType, input)
  }

  private fun inputReader(reader: BufferedReader): List<String> {
    val result = mutableListOf<String>()
    val buffer = StringBuilder()
    // Function to flush the buffer to the result list
    fun flushBuffer() {
      if (buffer.isNotEmpty()) {
        result.add(buffer.toString())
        buffer.clear()
      }
    }
    // Read input until a newline character is encountered
    while (true) {
      val char = reader.read().toChar()
      when (char) {
        '\n' -> {
          flushBuffer()
          break // End of input
        }
        ' ' -> {
          flushBuffer()
          if (result.last() == " ") continue else result.add(" ")
        }
        '\\' -> {
          // Read the next character and append it to the buffer
          val nextChar = reader.read().toChar()
          buffer.append(nextChar)
        }
        '\'' -> {
          // Read until the next single quote
          var nextChar = reader.read().toChar()
          while (nextChar != '\'') {
            buffer.append(nextChar)
            nextChar = reader.read().toChar()
          }
          flushBuffer()
        }
        '"' -> {
          // Read until the next double quote
          var nextChar = reader.read().toChar()
          while (nextChar != '"') {
            buffer.append(nextChar)
            nextChar = reader.read().toChar()
          }
          flushBuffer()
        }
        else -> buffer.append(char)
      }
    }
    // Trim the final space if it exists
    if (result.last() == " ") {
      result.removeAt(result.size - 1)
    }
    return result
  }
}
