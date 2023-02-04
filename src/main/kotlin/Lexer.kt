import java.io.Reader

sealed class Token {
    abstract val line: Int
    abstract val column: Int
}
data class SymbolToken(val symbol: String, override val line: Int, override val column: Int) : Token()
data class FunToken(override val line: Int, override val column: Int) : Token()
data class LetToken(override val line: Int, override val column: Int) : Token()
data class ReturnToken(override val line: Int, override val column: Int) : Token()
data class PrintToken(override val line: Int, override val column: Int) : Token()
data class SwichToken(override val line: Int, override val column: Int) : Token()
data class WhitespaceToken(val whitespace: String, override val line: Int, override val column: Int) : Token()
data class BadToken(val badChars: String, override val line: Int, override val column: Int) : Token()
data class OpenParenToken(override val line: Int, override val column: Int) : Token()
data class CloseParenToken(override val line: Int, override val column: Int) : Token()
data class OpenCurlyToken(override val line: Int, override val column: Int) : Token()
data class CloseCurlyToken(override val line: Int, override val column: Int) : Token()
data class EqualSignToken(override val line: Int, override val column: Int) : Token()
object EOFToken : Token() {
    override val line = -1
    override val column = -1
}

val acceptableChars = ('a'..'z').toSet() + ('A'..'Z').toSet()
val whiteSpace = setOf('\n', '\r', '\t', ' ')

enum class TokenizerState {
    Initial,
    Symbol,
    WhiteSpace,
    Bad
}

fun tokenize(lines: List<String>): List<Token> {
    val tokens = mutableListOf<Token>()
    val buffer = StringBuilder()
    var state = TokenizerState.Initial

    var line = 0
    var column = 0

    for(ln in 0 until lines.size) {
        val l = lines[ln]
        for(cn in 0 .. l.length) {
            val c =
                if(cn == l.length) '\n'
                else l[cn]

            val newState =
                when (c) {
                    in acceptableChars -> TokenizerState.Symbol
                    in whiteSpace -> TokenizerState.WhiteSpace
                    '(' -> TokenizerState.Initial
                    ')' -> TokenizerState.Initial
                    '{' -> TokenizerState.Initial
                    '}' -> TokenizerState.Initial
                    '=' -> TokenizerState.Initial
                    else -> TokenizerState.Bad
                }

            if (newState != state) {
                val token = buffer.toString()
                buffer.clear()

                when (state) {
                    TokenizerState.Symbol -> {
                        tokens.add(
                            when (token) {
                                "fun" -> FunToken(line, column)
                                "let" -> LetToken(line, column)
                                "switch" -> SwichToken(line, column)
                                "return" -> ReturnToken(line, column)
                                "print" -> PrintToken(line, column)
                                else -> SymbolToken(token, line, column)
                            }
                        )
                    }
                    TokenizerState.WhiteSpace -> tokens.add(WhitespaceToken(token, line, column))
                    TokenizerState.Bad -> tokens.add(BadToken(token, line, column))
                    TokenizerState.Initial -> {}
                }

                state = newState
                line = ln
                column = cn
            }

            when (c) {
                '(' -> tokens.add(OpenParenToken(line, column))
                ')' -> tokens.add(CloseParenToken(line, column))
                '{' -> tokens.add(OpenCurlyToken(line, column))
                '}' -> tokens.add(CloseCurlyToken(line, column))
                '=' -> tokens.add(EqualSignToken(line, column))
                else -> buffer.append(c)
            }
        }
    }

    val token = buffer.toString()
    buffer.clear()

    when (state) {
        TokenizerState.Symbol -> {
            tokens.add(
                when (token) {
                    "fun" -> FunToken(line, column)
                    "let" -> LetToken(line, column)
                    "switch" -> SwichToken(line, column)
                    "return" -> ReturnToken(line, column)
                    else -> SymbolToken(token, line, column)
                }
            )
        }
        TokenizerState.WhiteSpace -> tokens.add(WhitespaceToken(token, line, column))
        TokenizerState.Bad -> tokens.add(BadToken(token, line, column))
        TokenizerState.Initial -> {}
    }

    tokens.add(EOFToken)

    return tokens.toList()
}