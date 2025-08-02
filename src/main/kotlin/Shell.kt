import java.util.logging.*
import org.jline.reader.*
import org.jline.reader.impl.DefaultParser
import org.jline.reader.impl.completer.StringsCompleter
import org.jline.terminal.*

class Shell(prompt: String = "$ ", completions: List<String> = listOf()) {
  private val lineReader: LineReader
  private val prompt: String

  init {
    // Disable JLine logging to prevent output interference
    Logger.getLogger("org.jline").level = Level.OFF

    this.prompt = prompt
    lineReader =
        LineReaderBuilder.builder()
            .terminal(TerminalBuilder.builder().system(true).build())
            .completer(StringsCompleter(completions))
            .parser(DefaultParser().apply { escapeChars = charArrayOf() }) // We use a custom parser
            .build()
  }

  fun readLine(): String {
    val line = lineReader.readLine(prompt)
    return line
  }
}
