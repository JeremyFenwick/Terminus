import java.io.PrintWriter

data class Command(val type: CommandType, val rawInput: List<String>, val output: PrintWriter)

enum class CommandType() {
  ECHO,
  EXIT,
  TYPE,
  PWD,
  CD,
  REDIRECT,
  UNKNOWN;

  companion object {
    fun fromInput(input: String): CommandType {
      return when (input) {
        "echo" -> ECHO
        "exit" -> EXIT
        "type" -> TYPE
        "cd" -> CD
        "pwd" -> PWD
        else -> UNKNOWN
      }
    }
  }
}
