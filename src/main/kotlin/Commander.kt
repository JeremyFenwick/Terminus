import java.io.File
import java.io.FileOutputStream
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.system.exitProcess
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class Commander(private val shell: Shell) {
  var currentDir: Path = File(".").toPath().toAbsolutePath().normalize()

  suspend fun run(command: Command, programs: AvailablePrograms) {
    val outChannel = Channel<String>(Channel.BUFFERED)
    val errChannel = Channel<String>(Channel.BUFFERED)
    execute(command, programs, outChannel, errChannel)
    writeOut(outChannel, errChannel, command)
  }

  private suspend fun execute(
      command: Command,
      programs: AvailablePrograms,
      stdOutput: SendChannel<String>,
      errOutput: SendChannel<String>,
      input: ReceiveChannel<String>? = null,
  ) {
    when (command.type) {
      CommandType.ECHO -> echoCommand(command, stdOutput, errOutput)
      CommandType.EXIT -> exitCommand()
      CommandType.PWD -> pwdCommand(stdOutput, errOutput)
      CommandType.TYPE -> typeCommand(command, programs, stdOutput, errOutput)
      CommandType.CD -> changeDirCommand(stdOutput, errOutput, command)
      CommandType.PIPE -> {
        // If the command is a pipe, we need to split it into multiple commands
        val commandList = CommandGenerator.commandSplitter(command.input)
        pipeCommands(commandList, programs)
        closeChannels(stdOutput, errOutput)
      }
      CommandType.LS -> lsCommand(command, programs, stdOutput, errOutput)
      CommandType.NOTBUILTIN -> nonBuiltInCommand(command, programs, stdOutput, errOutput, input)
      CommandType.HISTORY -> historyCommand(stdOutput, errOutput, command)
    }
  }

  private suspend fun pipeCommands(commands: List<Command>, programs: AvailablePrograms) =
      coroutineScope {
        val stdOutputs = List(commands.size) { Channel<String>(Channel.BUFFERED) }
        val errOutputs = List(commands.size) { Channel<String>(Channel.BUFFERED) }
        val jobs = mutableListOf<Job>()
        // Launch a coroutine for each command in the pipeline
        for ((index, command) in commands.withIndex()) {
          // For all but the first command, we need to connect the output of the previous command
          // to the input of the current command
          val input = if (index == 0) null else stdOutputs[index - 1]
          // Now launch the program
          val program =
              launch(Dispatchers.IO) {
                execute(command, programs, stdOutputs[index], errOutputs[index], input)
              }
          // If this is the last command, we print the output to the system out
          if (index == commands.size - 1) {
            launch(Dispatchers.IO) {
              for (line in stdOutputs[index]) {
                print(line)
                if (!line.endsWith('\n')) print('\n') // Ensure we end with a newline
              }
              stdOutputs[index].close()
            }
          }
          jobs.add(program)
        }
        // Wait for all jobs to finish
        jobs.forEach { it.join() }
      }

  private fun exitCommand() {
    exitProcess(0)
  }

  private suspend fun lsCommand(
      command: Command,
      programs: AvailablePrograms,
      stdOutput: SendChannel<String>,
      errOutput: SendChannel<String>
  ) {
    // If the user has not specified a directory, we use the current directory
    if (command.input.size < 3) {
      command.input = command.input + listOf(" ", currentDir.toString())
    }
    nonBuiltInCommand(command, programs, stdOutput, errOutput, null)
  }

  private suspend fun historyCommand(
      stdOutput: SendChannel<String>,
      errOutput: SendChannel<String>,
      command: Command
  ) {
    when (command.input.getOrNull(2)) {
      "-r" -> fileToHistory(command)
      "-w" -> historyToFile(command)
      "-a" -> appendHistoryToFile(command)
      else -> {
        val history = shell.getHistory
        val limit = command.input.getOrNull(2)?.toIntOrNull() ?: history.size
        val startIndex = (history.size - limit).coerceAtLeast(0)

        for (i in startIndex until history.size) {
          stdOutput.send("  ${i + 1}  ${history[i]}\n") // Output should be 1 indexed
        }
      }
    }

    closeChannels(stdOutput, errOutput)
  }

  private fun appendHistoryToFile(command: Command) {
    if (command.input.size < 4) return
    val historyFile = File(command.input[4])
    // Create the parent directories if they do not exist
    historyFile.parentFile?.mkdirs()
    // We need only this history since the last append command
    val lastAppend = shell.getHistory.drop(1).indexOfFirst { it.startsWith("history -a") }
    val history =
        shell.getHistory.also { if (lastAppend != -1) it.subList(lastAppend + 1, it.size) else it }
    // Append the history to the file
    historyFile.appendText(history.joinToString(""))
  }

  private fun historyToFile(command: Command) {
    if (command.input.size < 4) return
    val historyFile = File(command.input[4])
    // Create the parent directories if they do not exist
    historyFile.parentFile?.mkdirs()
    // Write the history to the file
    historyFile.printWriter().use { writer ->
      shell.getHistory.forEach { entry -> writer.println(entry) }
    }
  }

  private fun fileToHistory(command: Command) {
    if (command.input.size < 4) return
    val historyFile = File(command.input[4])
    if (!historyFile.exists()) return
    val lines = historyFile.readLines().filter { it.isNotBlank() }
    shell.appendToHistory(lines)
  }

  private suspend fun changeDirCommand(
      stdOutput: SendChannel<String>,
      errOutput: SendChannel<String>,
      command: Command
  ) {
    if (command.input.size < 3) {
      closeChannels(stdOutput, errOutput)
      return
    }
    // If the user input starts with a tilde (~), replace it with the user's home directory
    val homeDir = System.getenv("HOME") ?: "/"
    // Generate the new path the user has requested
    val userPath =
        when {
          command.input[2] == "~" -> Path.of(homeDir)
          command.input[2].startsWith("~/") -> Path.of(homeDir, command.input[2].substring(2))
          else -> Path.of(command.input[2])
        }

    val proposedPath = currentDir.resolve(userPath).normalize()
    // If the proposed path exists, change the current directory
    // Otherwise, print an error message
    if (proposedPath.exists()) {
      currentDir = proposedPath
      closeChannels(stdOutput, errOutput)
      return
    }
    val message = "cd: ${proposedPath}: No such file or directory\n"
    sendStandardMessage(message, stdOutput, errOutput)
  }

  private suspend fun echoCommand(
      command: Command,
      stdOut: SendChannel<String>,
      errOut: SendChannel<String>
  ) {
    val message = command.input.drop(2).joinToString("").trim() + "\n"
    sendStandardMessage(message, stdOut, errOut)
  }

  private suspend fun pwdCommand(stdOutput: SendChannel<String>, errOutput: SendChannel<String>) =
      sendStandardMessage("${currentDir}\n", stdOutput, errOutput)

  suspend fun nonBuiltInCommand(
      command: Command,
      programs: AvailablePrograms,
      stdOutput: SendChannel<String>,
      errOutput: SendChannel<String>,
      input: ReceiveChannel<String>?
  ) {
    if (command.input.isEmpty()) {
      closeChannels(stdOutput, errOutput)
      return
    }

    val program =
        programs.executables.getOrElse(command.input[0]) {
          sendStandardMessage("${command.input[0]}: command not found\n", stdOutput, errOutput)
          return
        }

    executeProgram(command, program, stdOutput, errOutput, input)
  }

  private suspend fun executeProgram(
      command: Command,
      program: File,
      stdOutput: SendChannel<String>,
      errOutput: SendChannel<String>,
      input: ReceiveChannel<String>? = null
  ) = coroutineScope {
    val args = command.input.drop(2).filter() { it.isNotBlank() } // Filter out empty arguments
    val process = ProcessBuilder(listOf("stdbuf", "-o0", program.name) + args).start()
    // Begin writing to the process's input stream
    input?.let { lines ->
      launch(Dispatchers.IO) {
        process.outputStream.use { stream ->
          for (chunk in lines) {
            // Split the chunk into actual lines and send each separately
            // We have to do this because functions such as head
            val actualLines = chunk.split('\n').filter { it.isNotEmpty() }
            for (actualLine in actualLines) {
              stream.write((actualLine).toByteArray() + "\n".toByteArray())
              stream.flush() // Required to ensure the data is sent immediately
            }
          }
        }
      }
    } ?: process.outputStream.close()
    // Begin writing to the std output channel
    launch(Dispatchers.IO) {
      process.inputStream.bufferedReader().useLines { lines ->
        try {
          for (line in lines) {
            stdOutput.send(line)
          }
        } finally {
          stdOutput.close()
        }
      }
    }
    // Begin writing to the error output channel
    launch(Dispatchers.IO) {
      process.errorStream.bufferedReader().useLines { lines ->
        try {
          for (line in lines) {
            errOutput.send(line)
          }
        } finally {
          errOutput.close()
        }
      }
    }
  }

  private suspend fun typeCommand(
      command: Command,
      programs: AvailablePrograms,
      stdOutput: SendChannel<String>,
      errOutput: SendChannel<String>
  ) {
    if (command.input.size < 3) {
      closeChannels(stdOutput, errOutput)
      return
    }
    val sub = command.input[2]
    val message =
        when (val subCommand = CommandType.fromInput(sub)) {
          CommandType.NOTBUILTIN -> {
            programs.executables[sub]?.let { "$sub is ${it.absolutePath}\n" } ?: "$sub: not found\n"
          }
          else -> "${subCommand.toString().lowercase()} is a shell builtin\n"
        }
    sendStandardMessage(message, stdOutput, errOutput)
  }

  private suspend fun writeOut(
      stdOutput: ReceiveChannel<String>,
      errOutput: ReceiveChannel<String>,
      command: Command
  ) = coroutineScope {
    // Function to write the output stream to the specified output file or stdout
    suspend fun writeToOutput(channel: ReceiveChannel<String>, output: Output?) = coroutineScope {
      // The default output is just the system out
      if (output == null) {
        for (line in channel) {
          print(line)
          if (!line.endsWith('\n')) print('\n') // Ensure we end with a newline
        }
      } else {
        output.outputFile.parentFile?.mkdirs()
        FileOutputStream(output.outputFile, output.appendMode).use { fileOutputStream ->
          for (line in channel) {
            fileOutputStream.write(line.toByteArray())
            if (!line.endsWith('\n'))
                fileOutputStream.write('\n'.code) // Ensure we end with a newline
          }
        }
      }
    }

    launch { writeToOutput(stdOutput, command.outputFile) }
    launch { writeToOutput(errOutput, command.errFile) }
  }

  private fun closeChannels(stdOutput: SendChannel<String>, errOutput: SendChannel<String>) {
    stdOutput.close()
    errOutput.close()
  }

  private suspend fun sendStandardMessage(
      message: String,
      stdOutput: SendChannel<String>,
      errOutput: SendChannel<String>
  ) {
    stdOutput.send(message)
    stdOutput.close()
    errOutput.close()
  }
}
