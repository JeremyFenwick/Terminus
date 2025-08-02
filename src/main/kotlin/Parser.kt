import java.io.FileOutputStream
import java.io.PrintWriter

object Parser {
  private val defaultWriter = PrintWriter(System.out, true)
  private val stdOutSymbols = listOf(">", "1>", ">>", "1>>")
  private val stdErrSymbols = listOf("2>", "2>>")

  fun parseInput(line: String): Command {
    val input = rawParser(line)
    return inputToCommand(input)
  }

  fun inputToCommand(input: List<String>): Command {
    if (input.isEmpty()) {
      return Command(CommandType.UNKNOWN, input, Writer(defaultWriter), Writer(defaultWriter))
    }
    // Output the command to either standard output or a file
    val redirectIndex = input.indexOfFirst { it in stdOutSymbols || it in stdErrSymbols }
    val commandList = if (redirectIndex != -1) input.subList(0, redirectIndex) else input
    val stdOut = getRedirect(input, stdOutSymbols)
    val stdErr = getRedirect(input, stdErrSymbols)
    // Extract the command type and sub-command type
    val commandType = CommandType.fromInput(input[0])
    return Command(commandType, commandList, stdOut, stdErr)
  }

  private fun getRedirect(input: List<String>, sep: List<String>): Writer {
    val redirectIndex = input.indexOfFirst { it in sep }
    if (redirectIndex == -1 || redirectIndex + 2 > input.size)
        return Writer(defaultWriter) // No redirection found, return standard output
    val appendMode = input[redirectIndex].endsWith(">>")
    val outputFile = java.io.File(input[redirectIndex + 2])
    outputFile.parentFile?.mkdirs() // Ensure parent directories exist
    return Writer(PrintWriter(FileOutputStream(outputFile, appendMode), true))
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
