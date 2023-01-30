////fun distance(s1 String, s2 String) Int {
////    val dist = Array[I32,I32](length(s1), length(s2)) { i, j ->
////        switch(i, j) {
////            case 0, _ -> j
////            case _, 0 -> i
////            where s2(j-1) == s1(i-1) -> dist(j-1)(i-1)
////            else -> min(dist(j-1)(i), dist(j)(i-1), dist(j-1)(i-1))
////        }
////    }
////    return dist
////}
//

//
//sealed interface ParseResult<out A : Ast> {
//    fun <B : Ast> flatMap(f: (ParserState, A, Float) -> ParseResult<B>): ParseResult<B>
//    fun <B : Ast> result(f: (ParserState, A, Float, (ParserState, B, Float) -> ParseResult<B>) -> ParseResult<B>): ParseResult<B>
//    fun force(shouldForce: Boolean): ParseResult<A>
//}
//
//data class Accepted<out A : Ast>(val state: ParserState, val node: A, val rating: Float) : ParseResult<A> {
//    override fun <B : Ast> flatMap(f: (ParserState, A, Float) -> ParseResult<B>) = f(state, node, rating)
//    override fun <B : Ast> result(f: (ParserState, A, Float, (ParserState, B, Float) -> ParseResult<B>) -> ParseResult<B>) =
//        f(state, node, rating, ::Accepted)
//    override fun force(shouldForce: Boolean) = this
//}
//
//data class Recovery<A : Ast>(val baseRating: Float, val recoveryResults: () -> List<RecoverResult<A>>) : ParseResult<A> {
//    override fun <B : Ast> flatMap(f: (ParserState, A, Float) -> ParseResult<B>) =
//        Recovery(baseRating) {
//            recoveryResults().map { (state, node, rating) ->
//                Tracer.push()
//                val ret =
//                    if(rating > 10) Failure
//                    else f(state, node, rating)
//                Tracer.pop()
//                ret
//            }.flatten()
//        }
//
//    override fun <B : Ast> result(f: (ParserState, A, Float, (ParserState, B, Float) -> ParseResult<B>) -> ParseResult<B>) =
//        Recovery(baseRating) {
//            recoveryResults().map { (state, node, rating) ->
//                Tracer.push()
//                val ret =
//                    if(rating > 10) Failure
//                    else f(state, node, rating) {s, n, r -> Recovery(r) { listOf(RecoverResult(s, n, r)) }}
//                Tracer.pop()
//                ret
//            }.flatten()
//        }
//
//    override fun force(shouldForce: Boolean): ParseResult<A> {
//        if (shouldForce) {
//            val (state, node, rating) = recoveryResults().filter { !it.node.isSynthetic }.minByOrNull { it.rating } ?: return Failure
//            return Accepted(state, node, rating)
//        }
//
//        return this
//    }
//}
//
//data class RecoverResult<A : Ast>(val state: ParserState, val node: A, val rating: Float)
//object Failure : ParseResult<Nothing> {
//    override fun <B : Ast> flatMap(f: (ParserState, Nothing, Float) -> ParseResult<B>) = Failure
//    override fun <B : Ast> result(f: (ParserState, Nothing, Float, (ParserState, B, Float) -> ParseResult<B>) -> ParseResult<B>) = Failure
//    override fun force(shouldForce: Boolean) = Failure
//}
//
//fun <A : Ast> List<ParseResult<A>>.flatten(): List<RecoverResult<A>> {
//    val r = mutableListOf<RecoverResult<A>>()
//    for (res in this) {
//        when (res) {
//            is Failure -> {}
//            is Accepted -> r.add(RecoverResult(res.state, res.node, res.rating))
//            is Recovery ->
//                for (n in res.recoveryResults()) {
//                    r.add(n)
//                }
//        }
//    }
//
//    return r
//}
//
//abstract class Parser<T : Ast>(val name: String) {
//    companion object {
//        inline fun <reified T : Token, A : Ast> expect(
//            name: String,
//            crossinline f: (T) -> A,
//            crossinline fd: (T, List<Token>) -> A,
//            crossinline r: (Int, Int) -> A
//        ) =
//            object : Parser<A>(name) {
//                override fun expect(state: ParserState, rating: Float): ParseResult<A> {
//                    Tracer.enter(this.name)
//                    return when (val a = state.token) {
//                        is T -> {
//                            Tracer.success(this.name)
//                            Accepted(state.progress(), f(a), rating)
//                        }
//                        else ->
//                            if (state.isEOF()) {
//                                Tracer.fail(this.name)
//                                Failure
//                            } else {
//                                Tracer.recover(this.name)
//                                Recovery(rating) { recover(state, rating) }
//                            }
//                    }
//                }
//
//                fun recover(state: ParserState, rating: Float): List<RecoverResult<A>> {
//                    Tracer.beginRecovery(this.name)
//                    var state = state.fail()
//
//                    //create a fake token
//                    val possibleRecovers = mutableListOf(RecoverResult(state, r(state.token.line, state.token.column), rating + 1.5f))
//
//                    //delete up to 5 tokens and see if a matching token found
//                    val droppedTokens = mutableListOf<Token>()
//                    for (i in 1..5) {
//                        droppedTokens.add(state.token)
//                        state = state.progress()
//                        val t = state.token
//                        if (t is T) {
//                            possibleRecovers.add(RecoverResult(state.progress(), fd(t, droppedTokens), rating + i.toFloat()))
//                            break
//                        }
//                    }
//
//                    return possibleRecovers
//                }
//            }
//
//        //the sub-parsers need to be passed in lazily to allow for mutually recursive sub-parsers to be defined.
//        fun <T : Ast, A : Ast, B : Ast> compose(name: String, a: () -> Parser<A>, b: () -> Parser<B>, toAst: (A, B, Boolean) -> T, shouldForce: Boolean) =
//            object : Parser<T>(name) {
//                val aa: Parser<A> by lazy(a)
//                val bb: Parser<B> by lazy(b)
//
//                override fun expect(state: ParserState, rating: Float): ParseResult<T> {
//                    Tracer.enter(this.name)
//                    Tracer.push()
//                    val ret = aa.expect(state, rating).flatMap { state, a, rating ->
//                        bb.expect(state, rating).result<T> { state, b, rating, toResult ->
//                            toResult(state, toAst(a, b, a.isSynthetic && b.isSynthetic), rating)
//                        }
//                    }.force(shouldForce)
//                    Tracer.pop()
//                    when(ret) {
//                        is Accepted -> Tracer.success(this.name)
//                        is Recovery -> Tracer.recover(this.name)
//                        is Failure -> Tracer.fail(this.name)
//                    }
//                    return ret
//                }
//
//            }
//
//        fun <T : Ast, A : Ast, B : Ast, C : Ast> compose(
//            name: String,
//            a: () -> Parser<A>,
//            b: () -> Parser<B>,
//            c: () -> Parser<C>,
//            toAst: (A, B, C, Boolean) -> T,
//            shouldForce: Boolean
//        ) =
//            object : Parser<T>(name) {
//                val aa: Parser<A> by lazy(a)
//                val bb: Parser<B> by lazy(b)
//                val cc: Parser<C> by lazy(c)
//
//                override fun expect(state: ParserState, rating: Float): ParseResult<T> {
//                    Tracer.enter(this.name)
//                    Tracer.push()
//                    val ret =  aa.expect(state, rating).flatMap { state, a, rating ->
//                        bb.expect(state, rating).flatMap { state, b, rating ->
//                            cc.expect(state, rating).result<T> { state, c, rating, toResult ->
//                                toResult(state, toAst(a, b, c, a.isSynthetic && b.isSynthetic && c.isSynthetic), rating)
//                            }
//                        }
//                    }.force(shouldForce)
//                    Tracer.pop()
//                    when(ret) {
//                        is Accepted -> Tracer.success(this.name)
//                        is Recovery -> Tracer.recover(this.name)
//                        is Failure -> Tracer.fail(this.name)
//                    }
//                    return ret
//                }
//            }
//
//        fun <T : Ast, A : Ast, B : Ast, C : Ast, D : Ast> compose(
//            name: String,
//            a: () -> Parser<A>,
//            b: () -> Parser<B>,
//            c: () -> Parser<C>,
//            d: () -> Parser<D>,
//            toAst: (A, B, C, D, Boolean) -> T,
//            shouldForce: Boolean
//        ) =
//            object : Parser<T>(name) {
//                val aa: Parser<A> by lazy(a)
//                val bb: Parser<B> by lazy(b)
//                val cc: Parser<C> by lazy(c)
//                val dd: Parser<D> by lazy(d)
//
//                override fun expect(state: ParserState, rating: Float): ParseResult<T> {
//                    Tracer.enter(this.name)
//                    Tracer.push()
//                    val ret = aa.expect(state, rating).flatMap { state, a, rating ->
//                        bb.expect(state, rating).flatMap { state, b, rating ->
//                            cc.expect(state, rating).flatMap { state, c, rating ->
//                                dd.expect(state, rating).result<T> { state, d, rating, toResult ->
//                                    toResult(state, toAst(a, b, c, d, a.isSynthetic && b.isSynthetic && c.isSynthetic && d.isSynthetic), rating)
//                                }
//                            }
//                        }
//                    }.force(shouldForce)
//                    Tracer.pop()
//                    when(ret) {
//                        is Accepted -> Tracer.success(this.name)
//                        is Recovery -> Tracer.recover(this.name)
//                        is Failure -> Tracer.fail(this.name)
//                    }
//                    return ret
//                }
//            }
//
//        fun <T : Ast, A : Ast, B : Ast, C : Ast, D : Ast, E : Ast> compose(
//            name: String,
//            a: () -> Parser<A>,
//            b: () -> Parser<B>,
//            c: () -> Parser<C>,
//            d: () -> Parser<D>,
//            e: () -> Parser<E>,
//            toAst: (A, B, C, D, E, Boolean) -> T,
//            shouldForce: Boolean
//        ) =
//            object : Parser<T>(name) {
//                val aa: Parser<A> by lazy(a)
//                val bb: Parser<B> by lazy(b)
//                val cc: Parser<C> by lazy(c)
//                val dd: Parser<D> by lazy(d)
//                val ee: Parser<E> by lazy(e)
//
//                override fun expect(state: ParserState, rating: Float): ParseResult<T> {
//                    Tracer.enter(this.name)
//                    Tracer.push()
//                    val ret = aa.expect(state, rating).flatMap { state, a, rating ->
//                        bb.expect(state, rating).flatMap { state, b, rating ->
//                            cc.expect(state, rating).flatMap { state, c, rating ->
//                                dd.expect(state, rating).flatMap { state, d, rating ->
//                                    ee.expect(state, rating).result<T> { state, e, rating, toResult ->
//                                        toResult(
//                                            state,
//                                            toAst(a, b, c, d, e, a.isSynthetic && b.isSynthetic && c.isSynthetic && d.isSynthetic && e.isSynthetic),
//                                            rating
//                                        )
//                                    }
//                                }
//                            }
//                        }
//                    }.force(shouldForce)
//                    Tracer.pop()
//                    when(ret) {
//                        is Accepted -> Tracer.success(this.name)
//                        is Recovery -> Tracer.recover(this.name)
//                        is Failure -> Tracer.fail(this.name)
//                    }
//                    return ret
//                }
//            }
//
//        fun <T : Ast, A : T, B : T> any(name: String, a: () -> Parser<A>, b: () -> Parser<B>, shouldForce: Boolean) =
//            object : Parser<T>(name) {
//                val aa: Parser<A> by lazy(a)
//                val bb: Parser<B> by lazy(b)
//
//                override fun expect(state: ParserState, rating: Float): ParseResult<T> {
//                    Tracer.enter(this.name)
//                    Tracer.push()
//                    val a1 = aa.expect(state, rating)
//                    if (a1 is Accepted) {
//                        Tracer.pop()
//                        Tracer.success(this.name)
//                        return a1
//                    }
//
//                    val b1 = bb.expect(state, rating)
//                    if (b1 is Accepted) {
//                        Tracer.pop()
//                        Tracer.success(this.name)
//                        return b1
//                    }
//
//                    Tracer.pop()
//                    Tracer.recover(this.name)
//                    return Recovery(rating) { listOf(a1, b1).flatten() }.force(shouldForce)
//                }
//            }
//
//        fun <T : Ast, A : T, B : T, C : T> any(
//            name: String,
//            a: () -> Parser<A>,
//            b: () -> Parser<B>,
//            c: () -> Parser<C>,
//            shouldForce: Boolean
//        ) =
//            object : Parser<T>(name) {
//                val aa: Parser<A> by lazy(a)
//                val bb: Parser<B> by lazy(b)
//                val cc: Parser<C> by lazy(c)
//
//                override fun expect(state: ParserState, rating: Float): ParseResult<T> {
//                    Tracer.enter(this.name)
//                    Tracer.push()
//                    val a1 = aa.expect(state, rating)
//                    if (a1 is Accepted) {
//                        Tracer.pop()
//                        Tracer.success(this.name)
//                        return a1
//                    }
//
//                    val b1 = bb.expect(state, rating)
//                    if (b1 is Accepted) {
//                        Tracer.pop()
//                        Tracer.success(this.name)
//                        return b1
//                    }
//
//                    val c1 = cc.expect(state, rating)
//                    if (c1 is Accepted) {
//                        Tracer.pop()
//                        Tracer.success(this.name)
//                        return c1
//                    }
//
//                    Tracer.pop()
//                    Tracer.recover(this.name)
//                    return Recovery(rating) { listOf(a1, b1, c1).flatten() }.force(shouldForce)
//                }
//            }
//
//        fun <T : Ast, A : T, B : T, C : T, D : T> any(
//            name: String,
//            a: () -> Parser<A>,
//            b: () -> Parser<B>,
//            c: () -> Parser<C>,
//            d: () -> Parser<D>,
//            shouldForce: Boolean
//        ) =
//            object : Parser<T>(name) {
//                val aa: Parser<A> by lazy(a)
//                val bb: Parser<B> by lazy(b)
//                val cc: Parser<C> by lazy(c)
//                val dd: Parser<D> by lazy(d)
//
//                override fun expect(state: ParserState, rating: Float): ParseResult<T> {
//                    Tracer.enter(this.name)
//                    Tracer.push()
//                    val a1 = aa.expect(state, rating)
//                    if (a1 is Accepted) {
//                        Tracer.pop()
//                        Tracer.success(this.name)
//                        return a1
//                    }
//
//                    val b1 = bb.expect(state, rating)
//                    if (b1 is Accepted) {
//                        Tracer.pop()
//                        Tracer.success(this.name)
//                        return b1
//                    }
//
//                    val c1 = cc.expect(state, rating)
//                    if (c1 is Accepted) {
//                        Tracer.pop()
//                        Tracer.success(this.name)
//                        return c1
//                    }
//
//                    val d1 = dd.expect(state, rating)
//                    if (d1 is Accepted) {
//                        Tracer.pop()
//                        Tracer.success(this.name)
//                        return d1
//                    }
//
//                    Tracer.pop()
//                    Tracer.recover(this.name)
//                    return Recovery(rating) { listOf(a1, b1, c1, d1).flatten() }.force(shouldForce)
//                }
//            }
//
//        fun <T : Ast, A : T, B : T, C : T, D : T, E : T> any(
//            name: String,
//            a: () -> Parser<A>,
//            b: () -> Parser<B>,
//            c: () -> Parser<C>,
//            d: () -> Parser<D>,
//            e: () -> Parser<E>,
//            shouldForce: Boolean
//        ) =
//            object : Parser<T>(name) {
//                val aa: Parser<A> by lazy(a)
//                val bb: Parser<B> by lazy(b)
//                val cc: Parser<C> by lazy(c)
//                val dd: Parser<D> by lazy(d)
//                val ee: Parser<E> by lazy(e)
//
//                override fun expect(state: ParserState, rating: Float): ParseResult<T> {
//                    Tracer.enter(this.name)
//                    Tracer.push()
//                    val a1 = aa.expect(state, rating)
//                    if (a1 is Accepted) {
//                        Tracer.pop()
//                        Tracer.success(this.name)
//                        return a1
//                    }
//
//                    val b1 = bb.expect(state, rating)
//                    if (b1 is Accepted) {
//                        Tracer.pop()
//                        Tracer.success(this.name)
//                        return b1
//                    }
//
//                    val c1 = cc.expect(state, rating)
//                    if (c1 is Accepted) {
//                        Tracer.pop()
//                        Tracer.success(this.name)
//                        return c1
//                    }
//
//                    val d1 = dd.expect(state, rating)
//                    if (d1 is Accepted) {
//                        Tracer.pop()
//                        Tracer.success(this.name)
//                        return d1
//                    }
//
//                    val e1 = dd.expect(state, rating)
//                    if (e1 is Accepted) {
//                        Tracer.pop()
//                        Tracer.success(this.name)
//                        return e1
//                    }
//
//                    Tracer.pop()
//                    Tracer.success(this.name)
//                    return Recovery(rating) { listOf(a1, b1, c1, d1, e1).flatten() }.force(shouldForce)
//                }
//            }
//    }
//
//    fun optional(name: String): Parser<AstOption<T>> {
//        val t = this
//        return object : Parser<AstOption<T>>(name) {
//            override fun expect(state: ParserState, rating: Float): ParseResult<AstOption<T>> {
//                val res = t.expect(state, rating)
//                return if (res is Accepted) Accepted(res.state, AstSome(res.node), res.rating)
//                else Accepted(state, AstNone, rating)
//            }
//        }
//    }
//
//    inline fun <reified S : Token>star(): Parser<AstOption<AstList<T>>> {
//        val t = this
//        return object : Parser<AstOption<AstList<T>>>("${t.name}.star") {
//            override fun expect(state: ParserState, rating: Float): ParseResult<AstOption<AstList<T>>> {
//                Tracer.enter(this.name)
//                Tracer.push()
//                var state = state
//                var r = rating
//
//                val asts = mutableListOf<T>()
//                //TODO is this still relevant? ... probably
//                //TODO possible infinite loop S**; S** = S*
//                //TODO (S* T*)* might lead to a more difficult to solve infinite loop
//                while (true) {
//                    if(state.token is S) break
//                    val res = t.expect(state, r)
//                    if (res is Accepted && !res.node.isSynthetic) {
//                        state = res.state
//                        r += res.rating
//                        asts.add(res.node)
//                    } else break
//                    Tracer.pop()
//                    Tracer.goOn(this.name)
//                    Tracer.push()
//                }
//
//                Tracer.pop()
//                Tracer.success(this.name)
//                return if (asts.size == 0) Accepted(state, AstNone, rating)
//                else Accepted(state, AstSome(AstList(asts)), r)
//            }
//        }
//    }
//
//    /*val plus: Parser<List<T>> by lazy {
//        val t = this
//        object : Parser<List<T>>() {
//            override suspend fun accept(state: ParserState): List<T>? {
//                val asts = mutableListOf<T>()
//
//                asts.add(t.accept(state) ?: return null)
//
//                //TODO can get in an infinite loop with something like S*+; S*+ = S*; does S+* = S*?
//                while (true) asts.add(t.accept(state) ?: break)
//
//                return asts
//            }
//        }
//    }*/
//
//    abstract fun expect(state: ParserState, rating: Float): ParseResult<T>
//}
//
//fun <T : Ast, A : T, B : Ast> Parser<A>.optionally(name: String, b: () -> Parser<B>, toAst: (A, B, Boolean) -> T, shouldForce: Boolean): Parser<T> {
//    val t = this
//    return object : Parser<T>(name) {
//        val bb: Parser<B> by lazy(b)
//
//        override fun expect(state: ParserState, rating: Float): ParseResult<T> =
//            t.expect(state, rating).flatMap { state, a, rating ->
//                //todo the synthetic thing might be unnecessary?
//                when(val b1 = bb.expect(state, rating)) {
//                    is Accepted -> Accepted(b1.state, toAst(a, b1.node, a.isSynthetic && b1.node.isSynthetic), b1.rating)
//                    is Recovery -> {
//                        //these in theory should be the same, but the type checker is hating it
//                        /*b1.result { state, b, rating, toResult ->
//                            toResult(state, toAst(a, b, a.isSynthetic && b.isSynthetic), rating)
//                        }*/
//                        Recovery(b1.baseRating) {
//                            b1.recoveryResults().map { (state, b, rating) ->
//                                if(rating > 10) Failure
//                                else Recovery(rating) {
//                                    listOf(RecoverResult(state, toAst(a, b, b.isSynthetic), rating))
//                                }
//                            }.flatten()
//                        }.force(true)
//                    }
//                    else -> Accepted(state, a, rating)
//                }
//            }.force(shouldForce)
//    }
//}
