data class LetKeywordNode(val letKeyword: LetToken, val discardedTokens: List<Token>): Ast {
    constructor(letKeyword: LetToken): this(letKeyword, listOf())
    constructor(line: Int, column: Int): this(LetToken(line, column), listOf())
    companion object {
        val parser = FlowParser.expect("LetKeywordNode", true, ::LetKeywordNode, ::LetKeywordNode, ::LetKeywordNode)
    }
}

data class ReturnKeywordNode(val returnKeyword: ReturnToken, val discardedTokens: List<Token>): Ast {
    constructor(returnKeyword: ReturnToken): this(returnKeyword, listOf())
    constructor(line: Int, column: Int): this(ReturnToken(line, column), listOf())
    companion object {
        val parser = FlowParser.expect("ReturnKeywordNode", true, ::ReturnKeywordNode, ::ReturnKeywordNode, ::ReturnKeywordNode)
    }
}

data class PrintKeywordNode(val printKeyword: PrintToken, val discardedTokens: List<Token>): Ast {
    constructor(printKeyword: PrintToken): this(printKeyword, listOf())
    constructor(line: Int, column: Int): this(PrintToken(line, column), listOf())
    companion object {
        val parser = FlowParser.expect("PrintKeywordNode", true, ::PrintKeywordNode, ::PrintKeywordNode, ::PrintKeywordNode)
    }
}

data class ValueNode(val symbol: SymbolToken, val discardedTokens: List<Token>): Expression {
    constructor(printKeyword: SymbolToken): this(printKeyword, listOf())
    constructor(line: Int, column: Int): this(SymbolToken("ArtificialSymbol", line, column), listOf())
    companion object {
        val parser = FlowParser.expect("ValueNode", true, ::ValueNode, ::ValueNode, ::ValueNode)
    }
}

data class EqualSignNode(val equalSign: EqualSignToken, val discardedTokens: List<Token>): Ast {
    constructor(equalSign: EqualSignToken): this(equalSign, listOf())
    constructor(line: Int, column: Int): this(EqualSignToken(line, column), listOf())
    companion object {
        val parser = FlowParser.expect("EqualSignNode", true, ::EqualSignNode, ::EqualSignNode, ::EqualSignNode)
    }
}

data class OpenCurlyNode(val openCurly: OpenCurlyToken, val discardedTokens: List<Token>): Ast {
    constructor(openCurly: OpenCurlyToken): this(openCurly, listOf())
    constructor(line: Int, column: Int): this(OpenCurlyToken(line, column), listOf())
    companion object {
        val parser = FlowParser.expect("OpenCurlyNode", true, ::OpenCurlyNode, ::OpenCurlyNode, ::OpenCurlyNode)
    }
}

data class CloseCurlyNode(val closeCurly: CloseCurlyToken, val discardedTokens: List<Token>): Ast, FunctionBodyListNode {
    constructor(closeCurly: CloseCurlyToken): this(closeCurly, listOf())
    constructor(line: Int, column: Int): this(CloseCurlyToken(line, column), listOf())
    companion object {
        val parser = FlowParser.expect("CloseCurlyNode", true, ::CloseCurlyNode, ::CloseCurlyNode, ::CloseCurlyNode)
    }
}

data class OpenParenNode(val openParen: OpenParenToken, val discardedTokens: List<Token>): Ast {
    constructor(openParen: OpenParenToken): this(openParen, listOf())
    constructor(line: Int, column: Int): this(OpenParenToken(line, column), listOf())
    companion object {
        val parser = FlowParser.expect("OpenParenNode", true, ::OpenParenNode, ::OpenParenNode, ::OpenParenNode)
    }
}

data class CloseParenNode(val closeParen: CloseParenToken, val discardedTokens: List<Token>): Ast, FunctionArgsListNode {
    constructor(closeParen: CloseParenToken): this(closeParen, listOf())
    constructor(line: Int, column: Int): this(CloseParenToken(line, column), listOf())
    companion object {
        val parser = FlowParser.expect("CloseParenNode", true, ::CloseParenNode, ::CloseParenNode, ::CloseParenNode)
    }
}

data class FunKeywordNode(val funKeyword: FunToken, val discardedTokens: List<Token>): Ast {
    constructor(funKeyword: FunToken): this(funKeyword, listOf())
    constructor(line: Int, column: Int): this(FunToken(line, column), listOf())
    companion object {
        val parser = FlowParser.expect("FunKeywordNode", false, ::FunKeywordNode, ::FunKeywordNode, ::FunKeywordNode)
    }
}

sealed interface FunctionBody: Ast {
    companion object {
        val parser = FlowParser.any("FunctionBody", /*{ FunctionNode.parser }, */{ LetNode.parser }, { ReturnNode.parser }/*, { PrintNode.parser }*/)
    }
}

data class LetNode(val letKeyword: LetKeywordNode, val name: ValueNode, val equalSign: EqualSignNode, val value: Expression): FunctionBody {
    companion object {
        val parser = FlowParser.compose("LetNode", { LetKeywordNode.parser }, { ValueNode.parser }, { EqualSignNode.parser }, { Expression.parser }, ::LetNode)
    }
}

data class ReturnNode(val returnKeyword: ReturnKeywordNode, val value: Expression): FunctionBody {
    companion object {
        val parser = FlowParser.compose("ReturnNode", { ReturnKeywordNode.parser }, { Expression.parser }, ::ReturnNode)
    }
}

data class PrintNode(val printKeyword: PrintKeywordNode, val openParen: OpenParenNode, val value: ValueNode, val closeParen: CloseParenNode): FunctionBody {
    companion object {
        val parser = FlowParser.compose("PrintNode", { PrintKeywordNode.parser }, { OpenParenNode.parser }, { ValueNode.parser }, { CloseParenNode.parser }, ::PrintNode)
    }
}

sealed interface FunctionArgsListNode : Ast {
    companion object {
        val parser = FlowParser.any("FunctionArgsListNode", { FunctionArgsConsNode.parser }, { CloseParenNode.parser })
    }
}
data class FunctionArgsConsNode(val arg: ValueNode, val next: FunctionArgsListNode) : FunctionArgsListNode {
    companion object {
        val parser: FlowParser<FunctionArgsConsNode> = FlowParser.compose("FunctionArgsConsNode", { ValueNode.parser }, { FunctionArgsListNode.parser }, ::FunctionArgsConsNode)
    }
}

data class FunctionArgsNode(val openParen: OpenParenNode, val args: FunctionArgsListNode): Ast {
    companion object {
        val parser = FlowParser.compose("FunctionArgsNode", { OpenParenNode.parser }, { FunctionArgsListNode.parser }, ::FunctionArgsNode)
    }
}

sealed interface FunctionBodyListNode : Ast {
    companion object {
        val parser = FlowParser.any("FunctionBodyListNode", { FunctionBodyConsNode.parser }, { CloseCurlyNode.parser })
    }
}
data class FunctionBodyConsNode(val arg: FunctionBody, val next: FunctionBodyListNode) : FunctionBodyListNode {
    companion object {
        val parser: FlowParser<FunctionBodyConsNode> = FlowParser.compose("FunctionBodyConsNode", { FunctionBody.parser }, { FunctionBodyListNode.parser }, ::FunctionBodyConsNode)
    }
}
data class FunctionBodyNode(val openCurly: OpenCurlyNode, val body: FunctionBodyListNode): Ast {
    companion object {
        val parser = FlowParser.compose("FunctionBodyNode", { OpenCurlyNode.parser }, { FunctionBodyListNode.parser }, ::FunctionBodyNode)
    }
}

data class FunctionNode(val funKeywordNode: FunKeywordNode, val name: ValueNode, val args: FunctionArgsNode, val body: FunctionBodyNode): FunctionBody {
    companion object {
        val parser: FlowParser<FunctionNode> = FlowParser.compose("FunctionNode", { FunKeywordNode.parser }, { ValueNode.parser }, { FunctionArgsNode.parser }, { FunctionBodyNode.parser }, ::FunctionNode)
    }
}

sealed interface Expression: Ast {
    companion object {
        val parser = ValueNode.parser
    }
}

/*data class FunctionApplicationNode(val name: ValueNode, val args: FunctionArgsNode): Expression {
    companion object {
        val parser = ValueNode.parser.optionally("FunctionApplicationNode", { FunctionArgsNode.parser }, ::FunctionApplicationNode)
    }
}*/
