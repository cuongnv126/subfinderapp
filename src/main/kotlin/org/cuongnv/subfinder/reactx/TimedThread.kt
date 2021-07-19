package org.cuongnv.subfinder.reactx

import java.util.concurrent.ConcurrentLinkedDeque

class TimedThread private constructor() : Thread() {
    companion object {
        const val SEQUENCE_TIME = 1000 / 60L

        private var instance: TimedThread? = null
        fun getInstance(): TimedThread {
            if (instance == null || instance!!.isStopped) {
                synchronized(TimedThread::class) {
                    if (instance == null || instance!!.isStopped) {
                        instance = TimedThread()
                    }
                }
            }
            return instance!!
        }
    }

    private val queue = ConcurrentLinkedDeque<TimedTask>()
    private var isStopped = false

    init {
        start()
    }

    fun stopThread() {
        try {
            isStopped = true
            queue.clear()
            interrupt()
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    fun dispose(id: Int) {
        queue.firstOrNull { it.hashCode() == id }?.isDisposed = true
    }

    fun disposeAll() {
        val iterator = queue.iterator()
        while (iterator.hasNext()) {
            iterator.next().isDisposed = true
            iterator.remove()
        }
    }

    fun submit(task: Runnable, time: Long): Int {
        val timedTask = TimedTask(task, time)
        queue.add(timedTask)
        return timedTask.hashCode()
    }

    fun submit(task: Runnable): Int {
        return submit(task, System.currentTimeMillis())
    }

    fun schedule(task: Runnable, spacingTime: Long): Int {
        submit(task)
        return submitDelay({ schedule(task, spacingTime) }, spacingTime)
    }

    fun submitDelay(task: Runnable, delay: Long): Int {
        return submit(task, System.currentTimeMillis() + delay)
    }

    override fun run() {
        var iterator: MutableIterator<TimedTask>
        var currentTask: TimedTask

        while (!isStopped) {
            iterator = queue.iterator()
            while (iterator.hasNext()) {
                currentTask = iterator.next()

                if (currentTask.isDisposed) {
                    iterator.remove()
                }

                if (System.currentTimeMillis() >= currentTask.time - SEQUENCE_TIME) {
                    currentTask.task.run()
                    iterator.remove()
                }
            }
            sleep(SEQUENCE_TIME)
        }
    }
}