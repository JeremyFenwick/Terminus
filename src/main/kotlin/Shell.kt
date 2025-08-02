import java.io.InputStreamReader

class Shell(private val prompt: String = "$ ", words: List<String>) {
  private val trie = WordTrie(words)

  fun setTerminalRawMode() {
    Runtime.getRuntime().exec(arrayOf("sh", "-c", "stty raw -echo </dev/tty")).waitFor()
  }

  fun resetTerminalMode() {
    Runtime.getRuntime().exec(arrayOf("sh", "-c", "stty sane </dev/tty")).waitFor()
  }

  fun readLine(): String {
    val reader = InputStreamReader(System.`in`)
    val buffer = StringBuilder()
    var justHitTab = false

    setTerminalRawMode()

    fun handleTab(justHitTab: Boolean): Boolean {
      val completions = trie.getChildren(buffer.toString())
      // Execute the completion
      if (completions.size == 1) {
        print(completions[0].substring(buffer.length) + " ")
        buffer.append(completions[0].substring(buffer.length) + " ")
      }
      // Case where we have multiple completions and the user just hit tab
      else if (completions.isNotEmpty() && !justHitTab) {
        // Ring the bell
        print("\u0007") // ASCII Bell character
        return true //
        // Case where we have multiple completions and the user has already hit tab
      } else if (completions.isNotEmpty()) {
        print("\r\n")
        for ((index, completion) in completions.withIndex()) {
          print(completion)
          if (index < completions.size - 1) print("  ")
        }
        print("\r\n")
        print(prompt + buffer.toString())
      } else {
        // If there are no completions, we just ring the bell
        print("\u0007") // ASCII Bell character
      }
      return false
    }
    // begin reading input
    print(prompt)
    while (true) {
      val currentChar = reader.read()
      // Case where the user has hit enter
      if (currentChar == '\n'.code || currentChar == '\r'.code) {
        println()
        break
      }
      // Case where user has entered a backspace
      if (currentChar == 127 || currentChar == 8) {
        if (buffer.isNotEmpty()) {
          buffer.deleteCharAt(buffer.length - 1)
          print("\b \b") // Move cursor back, print space, move cursor back again
        }
        continue
      }
      // Case where user has entered a tab
      if (currentChar == '\t'.code) {
        justHitTab = handleTab(justHitTab) // Returns true if the user just hit tab
        continue
      }
      // Append the character to the buffer
      print(currentChar.toChar())
      buffer.append(currentChar.toChar())
    }
    print("\r\u001B[2K") // Carriage return + clear line (ANSI escape code)

    resetTerminalMode()
    return buffer.toString().trim()
  }

  class WordTrie(words: List<String>) {
    private val root: Node = Node()

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
      var currentNode = root
      for (char in prefix) {
        currentNode = currentNode.children[char] ?: return emptyList()
      }
      // The current node is the end of the prefix, now we need to collect all words
      val words = mutableListOf<String>()
      collectWords(currentNode, prefix, words)
      words.sort() // Sort the words for consistent output
      return words
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
