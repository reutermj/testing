import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.broadcast
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.suspendCoroutine

class AwaitableMap<K, V> {
    private val backer = ConcurrentHashMap<K, MutableSharedFlow<V>>()

    private fun get(key: K) =
        backer.getOrPut(key) {
            MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        }

    suspend fun await(key: K) = get(key).first()
    suspend fun emit(key: K, value: V) = get(key).emit(value)
}

suspend fun bla() = flow {
    emit(1)
    emit(2)
    emit(3)
}

suspend fun foo() = flow {
    emit(4)
    kotlinx.coroutines.delay(100)
    emit(5)
}

fun main() = runBlocking {
    /*runBlocking {
        val f = bla().flatMapMerge { x ->
            foo().map { y ->
                "$x $y"
            }
        }
    }*/

    val lines = File("./program.upl").bufferedReader().readLines()
    val tokens = tokenize(lines)
    val state = ParserState(tokens)
    val let = FunctionNode.parser.expect(state, 0f)
    let.toList().map{ ParserResult(it.ast, it.state, it.rating + it.state.remaining) }.sortedBy { it.rating }.forEach {
        println(it)
    }
//    println(.map{ ParserResult(it.ast, it.state, it.rating + it.state.remaining) }.sortedBy { it.rating }.first())
}