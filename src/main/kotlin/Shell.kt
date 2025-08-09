import java.io.BufferedReader
import java.io.InputStreamReader

class Shell(private val prompt: String = "$ ", words: List<String>) {
  private val trie = WordTrie(words)
  private val history = mutableListOf<String>()

  fun readLine(): String {
    val reader = BufferedReader(InputStreamReader(System.`in`))
    val buffer = StringBuilder()
    var justHitTab = false
    var historyIndex = history.size

    setTerminalRawMode()
    print(prompt)

    try {
      while (true) {
        val currentChar = readKeyPress(reader)

        when {
          isEnterKey(currentChar) -> {
            println()
            break
          }
          isUpArrow(currentChar) -> {
            justHitTab = false
            if (historyIndex > 0) historyIndex--
            printHistory(historyIndex, buffer)
          }
          isDownArrow(currentChar) -> {
            justHitTab = false
            if (historyIndex < history.size - 1) historyIndex++
            printHistory(historyIndex, buffer)
          }
          isBackspace(currentChar) -> {
            justHitTab = false
            handleBackspace(buffer)
          }
          isTab(currentChar) -> {
            justHitTab = handleTab(buffer, justHitTab)
          }
          else -> {
            justHitTab = false
            handleRegularChar(currentChar, buffer)
          }
        }
      }
    } finally {
      cleanupAndAddToHistory(buffer)
    }

    return buffer.toString().trim()
  }

  private fun readKeyPress(reader: BufferedReader): Int {
    val nextChar = reader.read()
    if (nextChar != 27) return nextChar // No escape key sequence
    reader.mark(2) // We need to read two more characters
    val secondChar = reader.read()
    val thirdChar = reader.read()
    return when {
      secondChar == 91 && thirdChar == 65 -> -1 // Up arrow
      secondChar == 91 && thirdChar == 66 -> -2 // Down arrow
      else -> {
        reader.reset() // Not an arrow key, reset the reader
        nextChar // Return the first character (27)
      }
    }
  }

  private fun isEnterKey(char: Int): Boolean = char == '\n'.code || char == '\r'.code

  private fun isBackspace(char: Int): Boolean = char == 127 || char == 8

  private fun isTab(char: Int): Boolean = char == '\t'.code

  private fun isUpArrow(char: Int): Boolean = char == -1

  private fun isDownArrow(char: Int): Boolean = char == -2

  private fun handleBackspace(buffer: StringBuilder) {
    if (buffer.isNotEmpty()) {
      buffer.deleteCharAt(buffer.length - 1)
      print("\b \b") // Move cursor back, print space, move cursor back again
    }
  }

  private fun printHistory(index: Int, buffer: StringBuilder) {
    if (index >= 0 && index < history.size) {
      // Clear the current line and print the history entry
      print("\r\u001B[2K") // Carriage return + clear line
      print(prompt + history[index])
      buffer.clear() // Clear the buffer to match the history entry
      buffer.append(history[index])
    }
  }

  private fun handleRegularChar(char: Int, buffer: StringBuilder) {
    val character = char.toChar()
    print(character)
    buffer.append(character)
  }

  private fun handleTab(buffer: StringBuilder, justHitTab: Boolean): Boolean {
    val completions = trie.getChildren(buffer.toString())

    return when {
      completions.size == 1 -> {
        executeSingleCompletion(buffer, completions[0])
        false
      }
      completions.isNotEmpty() && !justHitTab -> {
        handleMultipleCompletionsFirstTime(buffer)
      }
      completions.isNotEmpty() -> {
        showAllCompletions(buffer, completions)
        false
      }
      else -> {
        ringBell()
        false
      }
    }
  }

  private fun executeSingleCompletion(buffer: StringBuilder, completion: String) {
    val remainingText = completion.substring(buffer.length) + " "
    print(remainingText)
    buffer.append(remainingText)
  }

  private fun handleMultipleCompletionsFirstTime(buffer: StringBuilder): Boolean {
    val longestPrefix = trie.getLongestCommonChildPrefix(buffer.toString())
    val additionalText = longestPrefix.substring(buffer.length)

    if (additionalText.isEmpty()) {
      ringBell()
      return true // Signal that tab was just hit
    }

    print(additionalText)
    buffer.append(additionalText)
    return false
  }

  private fun showAllCompletions(buffer: StringBuilder, completions: List<String>) {
    print("\r\n")
    completions.forEachIndexed { index, completion ->
      print(completion)
      if (index < completions.size - 1) print("  ")
    }
    print("\r\n")
    print(prompt + buffer.toString())
  }

  private fun ringBell() {
    print("\u0007") // ASCII Bell character
  }

  private fun cleanupAndAddToHistory(buffer: StringBuilder) {
    print("\r\u001B[2K") // Carriage return + clear line (ANSI escape code)
    resetTerminalMode()
    history.add(buffer.toString())
    if (history.size > HISTORYLIMIT) history.removeAt(0) // Limit history size
  }

  private fun setTerminalRawMode() {
    Runtime.getRuntime().exec(arrayOf("sh", "-c", "stty raw -echo </dev/tty")).waitFor()
  }

  private fun resetTerminalMode() {
    Runtime.getRuntime().exec(arrayOf("sh", "-c", "stty sane </dev/tty")).waitFor()
  }

  class WordTrie(words: List<String>) {
    private val root: Node = Node()
    private var lastQuery: String? = null
    private var lastResults: List<String> = emptyList()

    data class Node(
        val children: MutableMap<Char, Node> = mutableMapOf(),
        var isWord: Boolean = false
    )

    init {
      buildTrie(words)
    }

    fun buildTrie(words: List<String>) {
      for (word in words) {
        var currentNode = root
        for (char in word) {
          currentNode = currentNode.children.getOrPut(char) { Node() }
        }
        currentNode.isWord = true // Mark the end of the word
      }
    }

    fun getChildren(prefix: String): List<String> {
      if (prefix == lastQuery) return lastResults
      var currentNode = root
      for (char in prefix) {
        currentNode = currentNode.children[char] ?: return emptyList()
      }
      // The current node is the end of the prefix, now we need to collect all words
      val words = mutableListOf<String>()
      collectWords(currentNode, prefix, words)
      words.sort() // Sort the words for consistent output
      // Cache the results
      lastQuery = prefix
      lastResults = words
      return words
    }

    fun getLongestCommonChildPrefix(prefix: String): String {
      // Update the cache if required
      if (prefix != lastQuery) getChildren(prefix)
      if (lastResults.isEmpty()) return prefix
      var index = prefix.length - 1
      // By definition the longest common prefix is the shortest child
      val shortestWord = lastResults.minBy { it.length }
      while (index < shortestWord.length) {
        for (word in lastResults) {
          if (word[index] != shortestWord[index]) {
            return shortestWord.substring(0, index)
          }
        }
        index++
      }
      return shortestWord
    }

    fun collectWords(node: Node, prefix: String, words: MutableList<String>) {
      if (node.isWord) {
        words.add(prefix)
      }
      for ((char, childNode) in node.children) {
        collectWords(childNode, prefix + char, words)
      }
    }
  }
}
