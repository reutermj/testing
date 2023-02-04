import kotlinx.coroutines.channels.*
import kotlinx.coroutines.coroutineScope

abstract class ChannelParser<out T : Ast>(val name: String) {
    abstract suspend fun expect(state: ParserState, rating: Float): ReceiveChannel<ParserResult<T>>

    companion object {
        inline fun <reified T : Token, A : Ast> expect(
            name: String,
            shouldSynthesize: Boolean,
            crossinline f: (T) -> A,
            crossinline fd: (T, List<Token>) -> A,
            crossinline r: (Int, Int) -> A
        ) =
            object : ChannelParser<A>(name) {
                override suspend fun expect(state: ParserState, rating: Float): ReceiveChannel<ParserResult<A>> {
                    return coroutineScope {
                        produce {
                            when (val a = state.token) {
                                is T -> send(ParserResult(f(a), state.progress(), rating))
                                else ->
                                    if (!state.isEOF()) {
                                        var state = state.fail()
                                        val synth = ParserResult(r(state.token.line, state.token.column), state, rating + 1.5f)
                                        var del: ParserResult<A>? = null

                                        val droppedTokens = mutableListOf<Token>()
                                        for (i in 1..3) {
                                            droppedTokens.add(state.token)
                                            state = state.progress()
                                            val t = state.token
                                            if (t is T) {
                                                del = ParserResult(fd(t, droppedTokens), state.progress(), rating + i.toFloat())
                                                break
                                            }
                                        }

                                        if(del == null) send(synth)
                                        else if(synth.rating > del.rating) {
                                            send(del)
                                            send(synth)
                                        } else {
                                            send(synth)
                                            send(del)
                                        }
                                    }
                            }
                        }
                    }
                }
            }

        fun <T : Ast, A : Ast, B : Ast> compose(name: String, a: () -> ChannelParser<A>, b: () -> ChannelParser<B>, toAst: (A, B) -> T) =
            object : ChannelParser<T>(name) {
                val aa: ChannelParser<A> by lazy(a)
                val bb: ChannelParser<B> by lazy(b)

                override suspend fun expect(state: ParserState, rating: Float) =
                    coroutineScope {
                        produce {
                            val ar = aa.expect(state, rating)
                            val a1 = ar.receive()

                            val a1br = bb.expect(a1.state, a1.rating)
                            val a1b1 = a1br.receive()
                            send(ParserResult(toAst(a1.ast, a1b1.ast), a1b1.state, a1b1.rating))

                            val a1b2 = a1br.receive()

                            val a2 = ar.receive()
                            val a2br = bb.expect(a2.state, a2.rating)
                            var a2b1 = a2br.receive()

                            val buffer = mutableListOf<Pair<ReceiveChannel<ParserResult<B>>, ParserResult<T>>>()
                            if(a1b2.rating < a2b1.rating) {
                                buffer.add(Pair(a2br, ParserResult(toAst(a2.ast, a2b1.ast), a2b1.state, a2b1.rating)))
                                send(ParserResult(toAst(a1.ast, a1b2.ast), a1b2.state, a1b2.rating))
                            }
                        }
                    }
                    /*aa.expect(state, rating).flatMapConcat { (a, state, rating) ->
                        bb.expect(state, rating).map { (b, state, rating) ->
                            ParserResult(toAst(a, b), state, rating)
                        }
                    }*/
            }
    }
}