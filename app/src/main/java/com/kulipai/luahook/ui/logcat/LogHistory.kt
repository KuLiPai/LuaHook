package com.kulipai.luahook.ui.logcat

/** Keeps displayed logs even when logcat's ring buffer discards older lines. */
class LogHistory {
    private val logs = mutableListOf<String>()
    val entries: List<String> get() = logs

    fun appendSnapshot(snapshot: List<String>): List<String> {
        if (snapshot.isEmpty()) return emptyList()

        // Match the history's suffix against the snapshot's prefix, including repeated lines.
        val prefix = IntArray(snapshot.size)
        for (i in 1 until snapshot.size) {
            var matched = prefix[i - 1]
            while (matched > 0 && snapshot[i] != snapshot[matched]) {
                matched = prefix[matched - 1]
            }
            if (snapshot[i] == snapshot[matched]) matched++
            prefix[i] = matched
        }

        var overlap = 0
        for (i in maxOf(0, logs.size - snapshot.size) until logs.size) {
            if (overlap == snapshot.size) overlap = prefix[overlap - 1]
            while (overlap > 0 && logs[i] != snapshot[overlap]) {
                overlap = prefix[overlap - 1]
            }
            if (logs[i] == snapshot[overlap]) overlap++
        }

        val appended = snapshot.drop(overlap)
        logs.addAll(appended)
        return appended
    }

    fun clear() {
        logs.clear()
    }
}
