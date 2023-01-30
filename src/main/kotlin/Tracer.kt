import kotlin.math.*

object Tracer {
    private var indents = 0
    fun push() {
        indents++
    }

    fun pop() {
        indents = max(0, indents - 1)
    }

    fun enter(name: String) {
        printMessage("Enter $name")
    }

    fun success(name: String) {
        printMessage("Success $name")
    }

    fun fail(name: String) {
        printMessage("Failure $name")
    }

    fun recover(name: String) {
        printMessage("Recover $name")
    }

    fun beginRecovery(name: String) {
        printMessage("Begin recovery $name")
    }

    fun goOn(name: String) {
        printMessage("Continue $name")
    }

    private fun printMessage(message: String) {
        print("  ".repeat(indents))
        println(message)
    }
}