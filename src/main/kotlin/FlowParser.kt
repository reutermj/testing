import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.yield

interface Ast
//interface Empty: Ast

/*sealed interface AstOption<out T : Ast> : Ast
data class AstSome<out T: Ast>(val value: T): AstOption<T>
object AstNone: AstOption<Nothing>
*/

data class AstList<out T : Ast>(val nodes: List<T>): Ast
data class AstPair<out A : Ast, out B : Ast>(val a: A, val b: B): Ast

class ParserState private constructor(private val tokens: List<Token>, private val currentToken: Int, val isSuccessful: Boolean) {
    constructor(tokens: List<Token>) : this(tokens, 0, true)

    val token: Token
        get() = tokens.getOrElse(currentToken) { EOFToken }

    val remaining: Int
        get() = tokens.size - currentToken

    fun progress(): ParserState {
        if(isEOF()) return this

        var i = currentToken + 1
        while (tokens[i] is WhitespaceToken) {
            i++
        }
        return ParserState(tokens, i, isSuccessful)
    }

    fun fail() = ParserState(tokens, currentToken, false)

    fun isEOF() = token is EOFToken
}

data class ParserResult<out A : Ast>(val ast: A, val state: ParserState, val rating: Float)

abstract class FlowParser<out T : Ast>(val name: String) {
    abstract suspend fun expect(state: ParserState, rating: Float): Flow<ParserResult<T>>

    companion object {
        /*fun <T : Empty>empty(e: T, name: String) =
            object : FlowParser<T>(name) {
                override suspend fun expect(state: ParserState, rating: Float) = flow {
                    emit(ParserResult(e, state, rating))
                }
            }*/

        inline fun <reified T : Token, A : Ast> expect(
            name: String,
            shouldSynthesize: Boolean,
            crossinline f: (T) -> A,
            crossinline fd: (T, List<Token>) -> A,
            crossinline r: (Int, Int) -> A
        ) =
            object : FlowParser<A>(name) {
                override suspend fun expect(state: ParserState, rating: Float) = flow {
                    when (val a = state.token) {
                        is T -> emit(ParserResult(f(a), state.progress(), rating))
                        else ->
                            if (!state.isEOF()) {
                                //any(...) cases will try all branches at once and even on valid programs it will hit this case a lot
                                //yield() to try to not spend too much time following recovery cases unless necessary
                                yield()

                                var state = state.fail()
                                if(shouldSynthesize && rating + 1.5f < 10f) emit(ParserResult(r(state.token.line, state.token.column), state, rating + 1.5f))

                                val droppedTokens = mutableListOf<Token>()
                                for (i in 1..3) {
                                    droppedTokens.add(state.token)
                                    state = state.progress()
                                    val t = state.token
                                    if (t is T) {
                                        if(rating + i.toFloat() < 10f) emit(ParserResult(fd(t, droppedTokens), state.progress(), rating + i.toFloat()))
                                        break
                                    }
                                }
                            }
                    }
                }
            }

        //the sub-parsers need to be passed in lazily to allow for mutually recursive sub-parsers to be defined.
        fun <T : Ast, A : Ast, B : Ast> compose(name: String, a: () -> FlowParser<A>, b: () -> FlowParser<B>, toAst: (A, B) -> T) =
            object : FlowParser<T>(name) {
                val aa: FlowParser<A> by lazy(a)
                val bb: FlowParser<B> by lazy(b)

                override suspend fun expect(state: ParserState, rating: Float) =
                    aa.expect(state, rating).flatMapConcat { (a, state, rating) ->
                        bb.expect(state, rating).map { (b, state, rating) ->
                            ParserResult(toAst(a, b), state, rating)
                        }
                    }
            }

        fun <T : Ast, A : Ast, B : Ast, C : Ast> compose(name: String, a: () -> FlowParser<A>, b: () -> FlowParser<B>, c: () -> FlowParser<C>, toAst: (A, B, C) -> T) =
            object : FlowParser<T>(name) {
                val aa: FlowParser<A> by lazy(a)
                val bb: FlowParser<B> by lazy(b)
                val cc: FlowParser<C> by lazy(c)

                override suspend fun expect(state: ParserState, rating: Float) =
                    aa.expect(state, rating).flatMapMerge { (a, state, rating) ->
                        bb.expect(state, rating).flatMapMerge { (b, state, rating) ->
                            cc.expect(state, rating).map { (c, state, rating) ->
                                ParserResult(toAst(a, b, c), state, rating)
                            }
                        }
                    }
            }

        fun <T : Ast, A : Ast, B : Ast, C : Ast, D : Ast> compose(name: String, a: () -> FlowParser<A>, b: () -> FlowParser<B>, c: () -> FlowParser<C>, d: () -> FlowParser<D>, toAst: (A, B, C, D) -> T) =
            object : FlowParser<T>(name) {
                val aa: FlowParser<A> by lazy(a)
                val bb: FlowParser<B> by lazy(b)
                val cc: FlowParser<C> by lazy(c)
                val dd: FlowParser<D> by lazy(d)

                override suspend fun expect(state: ParserState, rating: Float) =
                    aa.expect(state, rating).flatMapMerge { (a, state, rating) ->
                        bb.expect(state, rating).flatMapMerge { (b, state, rating) ->
                            cc.expect(state, rating).flatMapMerge { (c, state, rating) ->
                                dd.expect(state, rating).map { (d, state, rating) ->
                                    ParserResult(toAst(a, b, c, d), state, rating)
                                }
                            }
                        }
                    }
            }

        fun <T : Ast, A : Ast, B : Ast, C : Ast, D : Ast, E : Ast> compose(name: String, a: () -> FlowParser<A>, b: () -> FlowParser<B>, c: () -> FlowParser<C>, d: () -> FlowParser<D>, e: () -> FlowParser<E>, toAst: (A, B, C, D, E) -> T) =
            object : FlowParser<T>(name) {
                val aa: FlowParser<A> by lazy(a)
                val bb: FlowParser<B> by lazy(b)
                val cc: FlowParser<C> by lazy(c)
                val dd: FlowParser<D> by lazy(d)
                val ee: FlowParser<E> by lazy(e)

                override suspend fun expect(state: ParserState, rating: Float) =
                    aa.expect(state, rating).flatMapMerge { (a, state, rating) ->
                        bb.expect(state, rating).flatMapMerge { (b, state, rating) ->
                            cc.expect(state, rating).flatMapMerge { (c, state, rating) ->
                                dd.expect(state, rating).flatMapMerge { (d, state, rating) ->
                                    ee.expect(state, rating).map { (e, state, rating) ->
                                        ParserResult(toAst(a, b, c, d, e), state, rating)
                                    }
                                }
                            }
                        }
                    }
            }

        fun <T : Ast, A : T, B : T> any(name: String, a: () -> FlowParser<A>, b: () -> FlowParser<B>) =
            object : FlowParser<T>(name) {
                val aa: FlowParser<A> by lazy(a)
                val bb: FlowParser<B> by lazy(b)

                override suspend fun expect(state: ParserState, rating: Float): Flow<ParserResult<T>> {
                    val a = aa.expect(state, rating)
                    val b = bb.expect(state, rating)

                    val z = listOf(a, b).asFlow().flattenMerge()
                    val zp = z.filter { it.rating == rating }
                    return if(zp.firstOrNull() == null) z
                    else zp
                }
            }

        fun <T : Ast, A : T, B : T, C : T> any(name: String, a: () -> FlowParser<A>, b: () -> FlowParser<B>, c: () -> FlowParser<C>) =
            object : FlowParser<T>(name) {
                val aa: FlowParser<A> by lazy(a)
                val bb: FlowParser<B> by lazy(b)
                val cc: FlowParser<C> by lazy(c)

                override suspend fun expect(state: ParserState, rating: Float): Flow<ParserResult<T>> {
                    val a = aa.expect(state, rating)
                    val b = bb.expect(state, rating)
                    val c = cc.expect(state, rating)
                    val z = listOf(a, b, c).asFlow().flattenMerge()
                    val zp = z.filter { it.rating == rating }
                    return if(zp.firstOrNull() == null) z
                    else zp
                }
            }

        fun <T : Ast, A : T, B : T, C : T, D : T> any(name: String, a: () -> FlowParser<A>, b: () -> FlowParser<B>, c: () -> FlowParser<C>, d: () -> FlowParser<D>) =
            object : FlowParser<T>(name) {
                val aa: FlowParser<A> by lazy(a)
                val bb: FlowParser<B> by lazy(b)
                val cc: FlowParser<C> by lazy(c)
                val dd: FlowParser<D> by lazy(d)

                override suspend fun expect(state: ParserState, rating: Float): Flow<ParserResult<T>> {
                    val a = aa.expect(state, rating)
                    val b = bb.expect(state, rating)
                    val c = cc.expect(state, rating)
                    val d = dd.expect(state, rating)

                    val z = listOf(a, b, c, d).asFlow().flattenMerge()
                    val zp = z.filter { it.rating == rating }
                    return if(zp.firstOrNull() == null) z
                    else zp
                }
            }

        fun <T : Ast, A : T, B : T, C : T, D : T, E : T> any(name: String, a: () -> FlowParser<A>, b: () -> FlowParser<B>, c: () -> FlowParser<C>, d: () -> FlowParser<D>, e: () -> FlowParser<E>) =
            object : FlowParser<T>(name) {
                val aa: FlowParser<A> by lazy(a)
                val bb: FlowParser<B> by lazy(b)
                val cc: FlowParser<C> by lazy(c)
                val dd: FlowParser<D> by lazy(d)
                val ee: FlowParser<E> by lazy(e)

                override suspend fun expect(state: ParserState, rating: Float): Flow<ParserResult<T>> {
                    val a = aa.expect(state, rating)
                    val b = bb.expect(state, rating)
                    val c = cc.expect(state, rating)
                    val d = dd.expect(state, rating)
                    val e = ee.expect(state, rating)

                    val z = listOf(a, b, c, d, e).asFlow().flattenMerge()
                    val zp = z.filter { it.rating == rating }
                    return if(zp.firstOrNull() == null) z
                    else zp
                }
            }
    }

    fun <S : Ast>star(next: () -> FlowParser<S>): FlowParser<AstPair<AstList<T>, S>> {
        val t = this
        object : FlowParser<AstPair<AstList<T>, S>>("${t.name}.star") {
            val nn: FlowParser<S> by lazy(next)

            override suspend fun expect(state: ParserState, rating: Float): Flow<ParserResult<AstPair<AstList<T>, S>>> {

            }
        }
    }
}