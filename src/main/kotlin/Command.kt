data class Command(val type: CommandType, val rawInput: List<String>)

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
  }
}
