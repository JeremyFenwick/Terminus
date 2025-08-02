import java.io.PrintWriter

data class Command(
    val type: CommandType,
    val rawInput: List<String>,
    val stdOut: PrintWriter,
    val errOut: PrintWriter
)

enum class CommandType() {
  ECHO,
  EXIT,
  TYPE,
  PWD,
  CD,
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

    fun commandList(): List<String> {
      return listOf("echo", "exit", "type", "cd", "pwd")
    }
  }
}
