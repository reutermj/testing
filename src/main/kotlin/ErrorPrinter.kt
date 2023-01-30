//import java.io.BufferedReader
//
//fun printErrors(ast: Ast, lines: List<String>) {
//    when(ast) {
//        is LetKeywordNode ->
//            if(ast.isSynthetic) printExpected("let", ast.letKeyword.line, ast.letKeyword.column, lines)
//            else for(token in ast.discardedTokens) printExpected("let", token.line, token.column, lines)
//        is ReturnKeywordNode ->
//            if(ast.isSynthetic) printExpected("return", ast.returnKeyword.line, ast.returnKeyword.column, lines)
//            else for(token in ast.discardedTokens) printExpected("return", token.line, token.column, lines)
//        is PrintKeywordNode ->
//            if(ast.isSynthetic) printExpected("print", ast.printKeyword.line, ast.printKeyword.column, lines)
//            else for(token in ast.discardedTokens) printExpected("print", token.line, token.column, lines)
//        is ValueNode ->
//            if(ast.isSynthetic) printExpected("Symbol", ast.symbol.line, ast.symbol.column, lines)
//            else for(token in ast.discardedTokens) printExpected("Symbol", token.line, token.column, lines)
//        is EqualSignNode ->
//            if(ast.isSynthetic) printExpected("=", ast.equalSign.line, ast.equalSign.column, lines)
//            else for(token in ast.discardedTokens) printExpected("=", token.line, token.column, lines)
//        is OpenCurlyNode ->
//            if(ast.isSynthetic) printExpected("{", ast.openCurly.line, ast.openCurly.column, lines)
//            else for(token in ast.discardedTokens) printExpected("{", token.line, token.column, lines)
//        is CloseCurlyNode ->
//            if(ast.isSynthetic) printExpected("}", ast.closeCurly.line, ast.closeCurly.column, lines)
//            else for(token in ast.discardedTokens) printExpected("}", token.line, token.column, lines)
//        is OpenParenNode ->
//            if(ast.isSynthetic) printExpected("(", ast.openParen.line, ast.openParen.column, lines)
//            else for(token in ast.discardedTokens) printExpected("(", token.line, token.column, lines)
//        is CloseParenNode ->
//            if(ast.isSynthetic) printExpected(")", ast.closeParen.line, ast.closeParen.column, lines)
//            else for(token in ast.discardedTokens) printExpected(")", token.line, token.column, lines)
//        is FunKeywordNode ->
//            if(ast.isSynthetic) printExpected("fun", ast.funKeyword.line, ast.funKeyword.column, lines)
//            else for(token in ast.discardedTokens) printExpected("fun", token.line, token.column, lines)
//        is LetNode -> {
//            printErrors(ast.letKeyword, lines)
//            printErrors(ast.name, lines)
//            printErrors(ast.equalSign, lines)
//            printErrors(ast.value, lines)
//        }
//        is ReturnNode -> {
//            printErrors(ast.returnKeyword, lines)
//            printErrors(ast.value, lines)
//        }
//        is PrintNode -> {
//            printErrors(ast.printKeyword, lines)
//            printErrors(ast.openParen, lines)
//            printErrors(ast.value, lines)
//            printErrors(ast.closeParen, lines)
//        }
//        is FunctionBodyNode -> {
//            printErrors(ast.openCurly, lines)
//            printErrors(ast.body, lines)
//            printErrors(ast.closeCurlyNode, lines)
//        }
//        is FunctionArgsNode -> {
//            printErrors(ast.openParen, lines)
//            printErrors(ast.args, lines)
//            printErrors(ast.closeParenNode, lines)
//        }
//        is FunctionNode -> {
//            printErrors(ast.funKeywordNode, lines)
//            printErrors(ast.name, lines)
//            printErrors(ast.args, lines)
//            printErrors(ast.body, lines)
//        }
//        is FunctionApplicationNode -> {
//            printErrors(ast.name, lines)
//            printErrors(ast.args, lines)
//        }
//        is AstSome<*> -> printErrors(ast.value, lines)
//        is AstNone -> {}
//        is AstList<*> -> for(n in ast.nodes) printErrors(n, lines)
//        else -> throw Exception("hmmmmm")
//    }
//}
//
//fun printExpected(expected: String, l: Int, c: Int, lines: List<String>) {
//    val line = lines[l]
//    val pretoken = line.substring(0, c).trimStart().replace("\t", "  ")
//    val postToken = line.substring(c).replace("\t", "  ")
//
//    println("Found:")
//    print(pretoken)
//    println(postToken)
//    print(" ".repeat(pretoken.length))
//    println("^")
//    println("Expected: $expected")
//}