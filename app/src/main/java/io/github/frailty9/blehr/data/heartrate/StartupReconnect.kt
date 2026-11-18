package io.github.frailty9.blehr.data.heartrate

/** One foreground startup attempt per process. Manual actions close the opportunity. */
internal class StartupReconnect {
    private enum class State { PENDING, CONNECTING, DONE }

    private var state = State.PENDING

    fun tryOnce(start: () -> Boolean) {
        if (state != State.PENDING) return
        state = State.CONNECTING
        if (!start()) finish()
    }

    /** Returns whether the attempt still owns a connection in progress. */
    fun finish(): Boolean {
        val wasConnecting = state == State.CONNECTING
        state = State.DONE
        return wasConnecting
    }
}
