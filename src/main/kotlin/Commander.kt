import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.system.exitProcess

object Commander {
  var currentDir: Path = File(".").toPath().toAbsolutePath().normalize()

  fun execute(command: Command, programs: AvailablePrograms) {
    when (command.type) {
      CommandType.ECHO -> writeToOutput(command.rawInput.drop(2).joinToString(""), command.stdOut)
      CommandType.EXIT -> exitProcess(0)
      CommandType.PWD -> writeToOutput(currentDir.toString(), command.stdOut)
      CommandType.TYPE -> handleTypeCommand(command, programs)
      CommandType.CD -> changeDir(command)
      CommandType.UNKNOWN -> handleUnknownCommand(command, programs)
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
      writeToOutput("cd: ${proposedPath}: No such file or directory", command.stdOut)
    }
  }

  private fun handleUnknownCommand(command: Command, programs: AvailablePrograms) {
    if (command.rawInput.isEmpty()) return
    val program =
        programs.executables.getOrElse(command.rawInput[0]) {
          writeToOutput("${command.rawInput[0]}: command not found", command.stdOut)
          return
        }
    executeFile(command, program, command.rawInput.drop(2).filter(String::isNotBlank))
  }

  private fun executeFile(command: Command, file: File, arguments: List<String>) {
    val args = listOf(file.name) + arguments
    val processBuilder = ProcessBuilder(args)
    val process = processBuilder.start()
    process.inputStream.bufferedReader().use { reader ->
      for (line in reader.lines()) {
        command.stdOut.writer.println(line)
      }
    }

    process.errorStream.bufferedReader().use { reader ->
      for (line in reader.lines()) {
        command.errOut.writer.println(line)
      }
    }

    if (command.errOut.closeMe) command.errOut.writer.close()
    if (command.stdOut.closeMe) command.stdOut.writer.close()
  }

  private fun handleTypeCommand(command: Command, programs: AvailablePrograms) {
    if (command.rawInput.size < 3) {
      return
    }
    // If the sub-command is unknown, try to find it in the directories
    val subCommand = CommandType.fromInput(command.rawInput[2])
    if (subCommand == CommandType.UNKNOWN) {
      val executable =
          programs.executables.getOrElse(command.rawInput[2]) {
            writeToOutput("${command.rawInput[2]}: not found", command.stdOut)
            return
          }
      writeToOutput("${executable.name} is ${executable.absolutePath}", command.stdOut)
      return
    }
    // If the sub-command is a known command, print that it is a shell builtin
    writeToOutput("${subCommand.toString().lowercase()} is a shell builtin", command.stdOut)
  }

  private fun writeToOutput(text: String, writer: Writer) {
    writer.writer.println(text)
    if (writer.closeMe) writer.writer.close()
  }
}
