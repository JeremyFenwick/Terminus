import java.io.File
import kotlin.system.exitProcess

enum class CommandType() {
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

data class Command(val type: CommandType, val rawInput: List<String>)

object Commander {
  fun execute(command: Command, directories: List<Directory>) {
    when (command.type) {
      CommandType.ECHO -> println(command.rawInput.drop(1).joinToString(" "))
      CommandType.EXIT -> exitProcess(0)
      CommandType.TYPE -> handleTypeCommand(command, directories)
      CommandType.UNKNOWN -> println("${command.rawInput.getOrNull(0) ?: ""}: command not found")
    }
  }

  fun parse(input: String): Command {
    val parts = input.split(" ")
    if (parts.isEmpty()) {
      return Command(CommandType.UNKNOWN, parts)
    }
    // Extract the command type and sub-command type
    val commandType = CommandType.fromInput(parts[0])
    return Command(commandType, parts)
  }

  fun handleTypeCommand(command: Command, directories: List<Directory>) {
    if (command.rawInput.size < 2) {
      return
    }
    // If the sub-command is unknown, try to find it in the directories
    val subCommand = CommandType.fromInput(command.rawInput[1])
    if (subCommand == CommandType.UNKNOWN) {
      val executable = findExecutable(command.rawInput[1], directories)
      if (executable == null) println("${command.rawInput[1]}: not found")
      else println("${executable.name} is ${executable.absolutePath}")
      return
    }
    // If the sub-command is a known command, print that it is a shell builtin
    println("${subCommand.toString().lowercase()} is a shell builtin")
  }

  fun findExecutable(command: String, directories: List<Directory>): File? {
    for (dir in directories) {
      val executable = dir.executables.find { it.name == command }
      if (executable != null) {
        return executable
      }
    }
    return null
  }
}
