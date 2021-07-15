package org.cuongnv.subfinder.reactx

import io.reactivex.rxjava3.disposables.Disposable

class TimedDisposable(
    private val id: Int,
    private val timedThread: TimedThread
) : Disposable {
    @Volatile
    private var disposed = false

    override fun dispose() {
        if (!disposed) {
            timedThread.dispose(id)
            disposed = true
        }
    }

    override fun isDisposed(): Boolean {
        return disposed
    }
}