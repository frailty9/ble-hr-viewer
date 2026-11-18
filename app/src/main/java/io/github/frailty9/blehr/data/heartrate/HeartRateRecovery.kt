package io.github.frailty9.blehr.data.heartrate

internal const val MAX_RECONNECT_ATTEMPTS = 3

/** Runs on the same thread as BLE events. Owns at most one deadline or retry. */
internal class HeartRateRecovery(
    private val nowMillis: () -> Long,
    private val schedule: (Runnable, Long) -> Unit,
    private val cancel: (Runnable) -> Unit,
    private val timeoutMillis: () -> Long,
    private val onDataTimeout: () -> Boolean,
    private val reconnect: (attempt: Int) -> Boolean,
    private val onRecoveryFailed: () -> Unit,
) {
    private var lastActivityMillis: Long? = null
    private var attempts = 0
    val isRecovering: Boolean get() = attempts > 0
    val isMonitoring: Boolean get() = lastActivityMillis != null

    private val deadline = Runnable { checkDeadline() }
    private val retry = Runnable { startAttempt() }

    fun subscribed() {
        lastActivityMillis = nowMillis()
        checkDeadline()
    }

    fun receivedReading() {
        attempts = 0
        subscribed()
    }

    fun checkDeadline() {
        cancel(deadline)
        val lastActivity = lastActivityMillis ?: return
        val remaining = timeoutMillis() - (nowMillis() - lastActivity)
        if (remaining > 0) {
            schedule(deadline, remaining)
            return
        }
        lastActivityMillis = null
        if (!onDataTimeout()) {
            reset()
        } else if (isRecovering) {
            retryAfterFailure()
        } else {
            startAttempt()
        }
    }

    /** Returns true when an existing recovery cycle handles the connection failure. */
    fun connectionEnded(): Boolean {
        lastActivityMillis = null
        cancel(deadline)
        if (!isRecovering) return false
        retryAfterFailure()
        return true
    }

    fun reset() {
        lastActivityMillis = null
        attempts = 0
        cancel(deadline)
        cancel(retry)
    }

    private fun retryAfterFailure() {
        cancel(retry)
        if (attempts >= MAX_RECONNECT_ATTEMPTS) {
            failRecovery()
        } else {
            schedule(retry, 1_000)
        }
    }

    private fun startAttempt() {
        attempts++
        // False means the adapter or target cannot be used. GATT failures arrive as events.
        if (!reconnect(attempts)) failRecovery()
    }

    private fun failRecovery() {
        reset()
        onRecoveryFailed()
    }
}
