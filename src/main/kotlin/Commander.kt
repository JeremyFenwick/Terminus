import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.system.exitProcess

object Commander {
  var currentDir: Path = File(".").toPath().toAbsolutePath().normalize()

  data class ExecutionStream(val stdOut: InputStream?, val errOut: InputStream?)

  fun execute(command: Command, programs: AvailablePrograms) {
    when (command.type) {
      CommandType.ECHO -> write(command, echoCommand(command))
      CommandType.EXIT -> exitProcess(0)
      CommandType.PWD -> write(command, pwdCommand())
      CommandType.TYPE -> write(command, typeCommand(command, programs))
      CommandType.CD -> write(command, changeDirCommand(command))
      CommandType.PIPE -> {
        // If the command is a pipe, we need to split it into multiple commands
        val commandList = CommandGenerator.commandSplitter(command.rawInput)
        write(command, pipeCommands(commandList, programs))
      }
      CommandType.NOTBUILDIN -> write(command, nonBuiltinCommand(command, programs))
    }
  }

  private fun pipeCommands(
      commandList: List<Command>,
      programs: AvailablePrograms
  ): ExecutionStream {
    if (commandList.isEmpty())
        return ExecutionStream(
            ByteArrayInputStream("".toByteArray()), ByteArrayInputStream("".toByteArray()))

    // Create a ProcessBuilder for each command
    val processBuilders =
        commandList.map { command ->
          val program =
              programs.executables.getOrElse(command.rawInput[0]) {
                return ExecutionStream(
                    ByteArrayInputStream("".toByteArray()),
                    ByteArrayInputStream("${command.rawInput[0]}: command not found".toByteArray()))
              }
          val args = command.rawInput.drop(2).filter { it.isNotBlank() }
          ProcessBuilder(listOf(program.name) + args)
        }

    // Use Java's built-in pipeline support
    val processes = ProcessBuilder.startPipeline(processBuilders)

    // Return streams from the last process
    val lastProcess = processes.last()
    return ExecutionStream(lastProcess.inputStream, lastProcess.errorStream)
  }

  private fun changeDirCommand(command: Command): ExecutionStream {
    if (command.rawInput.size < 3) return ExecutionStream(null, null)
    var inputStream: InputStream? = null
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
      return ExecutionStream(null, null)
    }
    val message = "cd: ${proposedPath}: No such file or directory\n"
    inputStream = ByteArrayInputStream(message.toByteArray())
    return ExecutionStream(inputStream, null)
  }

  private fun echoCommand(command: Command): ExecutionStream {
    val message = command.rawInput.drop(2).joinToString("") + "\n"
    val inputStream = ByteArrayInputStream(message.toByteArray())
    return ExecutionStream(inputStream, ByteArrayInputStream("".toByteArray()))
  }

  private fun pwdCommand(): ExecutionStream {
    val message = "${currentDir}\n"
    val inputStream = ByteArrayInputStream(message.toByteArray())
    return ExecutionStream(inputStream, ByteArrayInputStream("".toByteArray()))
  }

  private fun nonBuiltinCommand(command: Command, programs: AvailablePrograms): ExecutionStream {
    if (command.rawInput.isEmpty())
        return ExecutionStream(
            ByteArrayInputStream("".toByteArray()), ByteArrayInputStream("".toByteArray()))
    val program =
        programs.executables.getOrElse(command.rawInput[0]) {
          val inputStream =
              ByteArrayInputStream("${command.rawInput[0]}: command not found\n".toByteArray())
          return ExecutionStream(inputStream, ByteArrayInputStream("".toByteArray()))
        }
    return executeProgram(command, program)
  }

  private fun executeProgram(command: Command, program: File): ExecutionStream {
    val args = command.rawInput.drop(2).filter() { it.isNotBlank() } // Filter out empty arguments
    val process =
        ProcessBuilder(listOf(program.name) + args)
            .redirectInput(ProcessBuilder.Redirect.PIPE) // optional, no stdin
            .start()

    return ExecutionStream(process.inputStream, process.errorStream)
  }

  private fun typeCommand(command: Command, programs: AvailablePrograms): ExecutionStream {
    if (command.rawInput.size < 3)
        return ExecutionStream(
            ByteArrayInputStream("".toByteArray()), ByteArrayInputStream("".toByteArray()))

    val sub = command.rawInput[2]

    val message =
        when (val subCommand = CommandType.fromInput(sub)) {
          CommandType.NOTBUILDIN -> {
            programs.executables[sub]?.let { "$sub is ${it.absolutePath}\n" } ?: "$sub: not found\n"
          }
          else -> "${subCommand.toString().lowercase()} is a shell builtin\n"
        }

    val inputStream = ByteArrayInputStream(message.toByteArray())
    return ExecutionStream(inputStream, ByteArrayInputStream("".toByteArray()))
  }

  private fun write(command: Command, execution: ExecutionStream) {
    // Function to write the output stream to the specified output file or stdout
    fun writeToOutput(inputStream: InputStream, output: Output?) {
      // The default output is just the system out
      if (output == null) {
        inputStream.copyTo(System.out)
        return
      }
      output.outputFile.parentFile?.mkdirs()
      FileOutputStream(output.outputFile, output.appendMode).use { fileOutputStream ->
        inputStream.copyTo(fileOutputStream)
      }
    }

    if (execution.stdOut != null) {
      writeToOutput(execution.stdOut, command.stdOut)
    }
    if (execution.errOut != null) {
      writeToOutput(execution.errOut, command.stdErr)
    }
  }
}
