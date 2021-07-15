package org.cuongnv.subfinder.reactx

import io.reactivex.rxjava3.core.Scheduler

class MainScheduler : Scheduler() {
    override fun createWorker(): Worker = MainWorker()
}