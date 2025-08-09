import java.io.File

data class Command(
    val type: CommandType,
    var input: List<String>,
    val outputFile: Output? = null,
    val errFile: Output? = null
)

data class Output(val outputFile: File, val appendMode: Boolean = false)

enum class CommandType() {
  ECHO,
  EXIT,
  TYPE,
  PWD,
  CD,
  PIPE,
  LS,
  HISTORY,
  NOTBUILTIN;

  companion object {
    fun fromInput(input: String): CommandType {
      return when (input) {
        "echo" -> ECHO
        "exit" -> EXIT
        "type" -> TYPE
        "cd" -> CD
        "pwd" -> PWD
        "history" -> HISTORY
        "ls" -> LS
        else -> NOTBUILTIN
      }
    }

    fun commandList(): List<String> {
      return listOf("echo", "exit", "type", "cd", "pwd", "history")
    }
  }
}
