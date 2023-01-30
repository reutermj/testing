/*
sealed interface RValue
data class RFnNode(val boundId: List<String>, val body: RFnBody): RValue
data class RClosure(val closedValues: Map<String, RValue>, val boundId: List<String>, val body: RFnBody): RValue

val baseContext = mutableMapOf<String, RValue>()

sealed interface RFnBody {
    fun execute(context: Map<String, RValue>): RValue
}
data class RLetNode(val boundId: String, val expression: RExpression, val next: RFnBody): RFnBody {
    override fun execute(context: Map<String, RValue>): RValue {
        val v = expression.execute(context)
        return next.execute(context + (boundId to v))
    }
}

data class RRetNode(val expression: RExpression): RFnBody {
    override fun execute(context: Map<String, RValue>): RValue {
        return expression.execute(context)
    }
}

data class RPrintNode(val value: ValueNode, val next: RFnBody): RFnBody {
    override fun execute(context: Map<String, RValue>): RValue {
        println(value.symbol.symbol)
        return next.execute(context)
    }
}

data class RCloseValues(val names: Set<String>, val fnName: String, val boundId: List<String>, val body: RFnBody, val next: RFnBody): RFnBody {
    override fun execute(context: Map<String, RValue>): RValue {
        val closure = RClosure(names.associateWith { context[it]!! }, boundId, body)
        return next.execute(context + (fnName to closure))
    }
}

object Noop: RFnBody {
    override fun execute(context: Map<String, RValue>): RValue {
        throw Exception("This should never be called")
    }
}

sealed interface RExpression {
    fun execute(context: Map<String, RValue>): RValue
}

data class RFnCallNode(val fnId: String, val argId: List<String>): RExpression {
    override fun execute(context: Map<String, RValue>): RValue {
        return when(val fn = context[fnId]!!) {
            is RFnNode -> {
                val newContext = fn.boundId.zip(argId) { bid, aid -> bid to context[aid]!! }.toMap()
                fn.body.execute(baseContext + newContext)
            }
            is RClosure -> {
                val newContext = fn.boundId.zip(argId) { bid, aid -> bid to context[aid]!! }.toMap()
                fn.body.execute(baseContext + newContext + fn.closedValues)
            }
        }
    }
}

data class RValueNode(val valId: String): RExpression {
    override fun execute(context: Map<String, RValue>): RValue {
        return context[valId]!!
    }
}

fun lowerFnNode(fn: FunctionNode) {
    val args = fn.args.args.map { it.symbol.symbol }
    val fnNode = RFnNode(args, lowerFnBody(fn.body.expressions, args.toSet()))
    baseContext[fn.name.symbol.symbol] = fnNode
}

fun lowerFnBody(body: List<FunctionBody>, localNames: Set<String>): RFnBody {
    return if(body.isEmpty()) Noop
           else when (val expr = body[0]) {
               is LetNode -> {
                   val name = expr.name.symbol.symbol
                   RLetNode(name, lowerExpression(expr.value), lowerFnBody(body.drop(1), localNames + name))
               }
               is ReturnNode -> RRetNode(lowerExpression(expr.value))
               is PrintNode -> RPrintNode(expr.value, lowerFnBody(body.drop(1), localNames))
               is FunctionNode -> {
                   val args = expr.args.args.map { it.symbol.symbol }
                   RCloseValues(localNames.toSet(), expr.name.symbol.symbol, args, lowerFnBody(expr.body.expressions, localNames.toSet()), lowerFnBody(body.drop(1), localNames))
               }
           }

}

fun lowerExpression(expr: Expression): RExpression =
    when(expr) {
        is FunctionApplicationNode -> RFnCallNode(expr.name.symbol.symbol, expr.args.args.map { it.symbol.symbol })
        is ValueNode -> RValueNode(expr.symbol.symbol)
    }
*/
