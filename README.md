[![progress-banner](https://backend.codecrafters.io/progress/shell/4ac3b061-d791-487c-a4a8-f206755d52b1)](https://app.codecrafters.io/users/codecrafters-bot?r=2qF)

This a solution to the build your own shell challenge on Codecrafters using Kotlin >>
["Build Your Own Shell" Challenge](https://app.codecrafters.io/courses/shell/overview).

This is a POSIX compliant shell that's capable of
interpreting shell commands, running external programs and builtin commands like
cd, pwd, echo and more. Along the way, you'll learn about shell command parsing,
REPLs, builtin commands, and more.

As a challenge, no external libraries were used to implement the shell. The code is written in pure Kotlin and uses the
standard library only.

Features include:

- Command & quote parsing
- Builtin commands (cd, pwd, echo, exit, history, type, etc.)
- Loads all programs from the PATH environment variable
- Run external programs
- Pipe internal and external programs seamlessly. This solution does not rely on OS pipes, but instead implements its
  own using Kotlin's coroutines
  piping mechanism to connect the output of one command to the input of another.
- Redirection of input and output (both stdout and stderr)
- Text prediction (implemented using a Trie)
- Command history which can be navigated using the up and down arrow keys
- Ability to read and write to a history file by setting the HISTFILE environment variable



