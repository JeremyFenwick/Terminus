import java.io.File
import kotlin.system.exitProcess

enum class CommandType() {
  ECHO,
  EXIT,
  TYPE,
  PWD,
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
      CommandType.PWD -> println(File(".").absolutePath)
      CommandType.UNKNOWN -> handleUnknownCommand(command, directories)
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

  private fun handleUnknownCommand(command: Command, directories: List<Directory>) {
    // If the command is unknown, try to find it in the directories
    val executable = findExecutable(command.rawInput[0], directories)
    when (executable) {
      null -> println("${command.rawInput[0]}: command not found")
      else -> executeFile(executable, command.rawInput.drop(1))
    }
  }

  private fun executeFile(file: File, arguments: List<String>) {
    val command = listOf(file.name) + arguments
    val processBuilder = ProcessBuilder(command)
    processBuilder.redirectErrorStream(true)
    val process = processBuilder.start()
    process.inputStream.bufferedReader().useLines { lines ->
      for (line in lines) {
        println(line)
      }
    }
  }

  private fun handleTypeCommand(command: Command, directories: List<Directory>) {
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

  private fun findExecutable(command: String, directories: List<Directory>): File? {
    for (dir in directories) {
      if (dir.executables.containsKey(command)) {
        return dir.executables[command]
      }
    }
    return null
  }
}
