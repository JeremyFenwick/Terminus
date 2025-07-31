import kotlin.system.exitProcess

object Commander {
  fun execute(command: String) {
    when {
      command == "exit 0" -> exitProcess(0)
      command.startsWith("echo ") -> println(command.removePrefix("echo "))
      else -> println("${command}: command not found")
    }
  }
}
