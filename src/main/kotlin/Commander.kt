import kotlin.system.exitProcess

enum class CommandType {
  ECHO,
  EXIT,
  TYPE,
  UNKNOWN;

  companion object {
    fun fromInput(input: String): CommandType {
      return when (input) {
        "echo" -> ECHO
        "exit" -> EXIT
        "type" -> TYPE
        else -> UNKNOWN
      }
    }
  }
}

data class Command(val type: CommandType, val content: String, val subType: CommandType)

object Commander {
  fun execute(command: Command) {
    when (command.type) {
      CommandType.ECHO -> println(command.content)
      CommandType.EXIT -> exitProcess(0)
      CommandType.TYPE -> {
        when (command.subType) {
          CommandType.UNKNOWN -> println("${command.content}: not found")
          else -> println("${command.subType.toString().lowercase()} is a shell builtin")
        }
      }

      CommandType.UNKNOWN -> println("${command.content}: command not found")
    }
  }

  fun parse(input: String): Command {
    val parts = input.split(" ")
    if (parts.isEmpty()) {
      return Command(CommandType.UNKNOWN, "", CommandType.UNKNOWN)
    }
    val commandType = CommandType.fromInput(parts[0])
    val subCommandType =
        if (parts.size > 1) CommandType.fromInput(parts[1]) else CommandType.UNKNOWN
    val content =
        when {
          commandType == CommandType.UNKNOWN -> parts.joinToString(" ")
          subCommandType != CommandType.UNKNOWN -> parts.drop(2).joinToString(" ")
          else -> parts.drop(1).joinToString(" ")
        }
    return Command(commandType, content, subCommandType)
  }
}
