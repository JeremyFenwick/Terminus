import java.io.File

data class Command(
    val type: CommandType,
    val rawInput: List<String>,
    val stdOut: Output? = null,
    val stdErr: Output? = null
)

data class Output(val outputFile: File, val appendMode: Boolean = false)

enum class CommandType() {
  ECHO,
  EXIT,
  TYPE,
  PWD,
  CD,
  PIPE,
  NOTBUILDIN;

  companion object {
    fun fromInput(input: String): CommandType {
      return when (input) {
        "echo" -> ECHO
        "exit" -> EXIT
        "type" -> TYPE
        "cd" -> CD
        "pwd" -> PWD
        else -> NOTBUILDIN
      }
    }

    fun commandList(): List<String> {
      return listOf("echo", "exit", "type", "cd", "pwd")
    }
  }
}
