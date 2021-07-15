package org.cuongnv.subfinder.reactx

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

class MainWorker : Scheduler.Worker() {
    private val executor = TimedThread()

    @Volatile
    private var disposed = false

    override fun dispose() {
        if (!disposed) {
            disposed = true
            executor.disposeAll()
        }
    }

    override fun isDisposed(): Boolean = disposed

    override fun schedule(run: Runnable?, delay: Long, unit: TimeUnit): Disposable {
        return TimedDisposable(
            id = executor.submitDelay(
                {
                    if (!disposed) {
                        SwingUtilities.invokeLater(run)
                    }
                },
                unit.convert(delay, TimeUnit.MILLISECONDS)
            ),
            timedThread = executor
        )
    }
}