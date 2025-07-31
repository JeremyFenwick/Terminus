import kotlin.system.exitProcess

object CommandParser {
  fun parse(input: String) {
    when (input) {
      "exit 0" -> exitProcess(0)
      else -> println("${input}: command not found")
    }
  }
}
