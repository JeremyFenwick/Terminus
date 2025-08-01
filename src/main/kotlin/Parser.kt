import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.PrintWriter

object Parser {
  private val stdoutWriter = PrintWriter(System.out, true)

  fun parseInput(reader: BufferedReader): Command {
    val input = inputReader(reader)
    return inputToCommand(input)
  }

  fun inputToCommand(input: List<String>): Command {
    if (input.isEmpty()) {
      return Command(CommandType.UNKNOWN, input, stdoutWriter)
    }
    // Output the command to either standard output or a file
    val redirectIndex = input.indexOfFirst { it == ">" || it == "1>" }
    val commandList = if (redirectIndex != -1) input.subList(0, redirectIndex) else input
    val output =
        if (redirectIndex != -1) {
          // If a redirect is found, create a PrintWriter for the output file
          val outputFileName =
              input
                  .subList(redirectIndex + 1, input.size)
                  .filter { it.isNotBlank() }
                  .joinToString("")
          val outputFile = java.io.File(outputFileName)
          outputFile.parentFile?.mkdirs() // Ensure parent directories exist
          PrintWriter(FileOutputStream(outputFile), true)
        } else {
          stdoutWriter // Default to standard output
        }
    // Extract the command type and sub-command type
    val commandType = CommandType.fromInput(input[0])
    return Command(commandType, commandList, output)
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
    // Function to read a single-quoted string
    fun readSingleQuotedString() {
      // Read until the next single quote
      var nextChar = reader.read().toChar()
      while (nextChar != '\'') {
        buffer.append(nextChar)
        nextChar = reader.read().toChar()
      }
      flushBuffer()
    }
    // Function to read a double-quoted string
    fun readDoubleQuotedString() {
      // Read until the next double quote
      var nextChar = reader.read().toChar()
      while (nextChar != '"') {
        // Handle escaped characters
        if (nextChar == '\\') {
          // If the next character is escapable, append that character instead
          val lookAhead = reader.read().toChar()
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
        nextChar = reader.read().toChar()
      }
      flushBuffer()
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
          if (result.last() != " ") result.add(" ")
        }
        '\\' -> {
          // Read the next character and append it to the buffer
          val nextChar = reader.read().toChar()
          buffer.append(nextChar)
        }
        '\'' -> readSingleQuotedString()
        '"' -> readDoubleQuotedString()
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
