import java.io.File
import java.nio.file.Path
import jdk.jshell.spi.ExecutionControl
import kotlin.io.path.exists
import kotlin.system.exitProcess

object Commander {
  var currentDir: Path = File(".").toPath().toAbsolutePath().normalize()

  fun execute(command: Command, directories: List<Directory> = listOf()) {
    when (command.type) {
      CommandType.ECHO -> command.output.println(command.rawInput.drop(2).joinToString(""))
      CommandType.EXIT -> exitProcess(0)
      CommandType.PWD -> command.output.println(currentDir.toString())
      CommandType.TYPE -> handleTypeCommand(command, directories)
      CommandType.CD -> changeDir(command)
      CommandType.REDIRECT ->
          throw ExecutionControl.NotImplementedException("Redirect command not implemented")
      CommandType.UNKNOWN -> handleUnknownCommand(command, directories)
    }
  }

  private fun changeDir(command: Command) {
    if (command.rawInput.size < 3) return
    // If the user input starts with a tilde (~), replace it with the user's home directory
    val homeDir = System.getenv("HOME") ?: "/"
    // Generate the new path the user has requested
    val userPath =
        when {
          command.rawInput[2] == "~" -> Path.of(homeDir)
          command.rawInput[2].startsWith("~/") -> Path.of(homeDir, command.rawInput[2].substring(2))
          else -> Path.of(command.rawInput[2])
        }

    val proposedPath = currentDir.resolve(userPath).normalize()
    // If the proposed path exists, change the current directory
    // Otherwise, print an error message
    if (proposedPath.exists()) {
      currentDir = proposedPath
    } else {
      command.output.println("cd: ${proposedPath}: No such file or directory")
    }
  }

  private fun handleUnknownCommand(command: Command, directories: List<Directory>) {
    // If the command is unknown, try to find it in the directories
    val executable = findExecutable(command.rawInput[0], directories)
    when (executable) {
      null -> command.output.println("${command.rawInput[0]}: command not found")
      else -> executeFile(command, executable, command.rawInput.drop(2).filter(String::isNotBlank))
    }
  }

  private fun executeFile(command: Command, file: File, arguments: List<String>) {
    val args = listOf(file.name) + arguments
    val processBuilder = ProcessBuilder(args)
    val process = processBuilder.start()
    process.inputStream.bufferedReader().use { reader ->
      for (line in reader.lines()) {
        command.output.println(line)
      }
    }

    process.errorStream.bufferedReader().use { reader ->
      for (line in reader.lines()) {
        println(line)
      }
    }
  }

  private fun handleTypeCommand(command: Command, directories: List<Directory>) {
    if (command.rawInput.size < 3) {
      return
    }
    // If the sub-command is unknown, try to find it in the directories
    val subCommand = CommandType.fromInput(command.rawInput[2])
    if (subCommand == CommandType.UNKNOWN) {
      val executable = findExecutable(command.rawInput[2], directories)
      if (executable == null) command.output.println("${command.rawInput[2]}: not found")
      else command.output.println("${executable.name} is ${executable.absolutePath}")
      return
    }
    // If the sub-command is a known command, print that it is a shell builtin
    command.output.println("${subCommand.toString().lowercase()} is a shell builtin")
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
