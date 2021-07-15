package org.cuongnv.subfinder.reactx

data class TimedTask(
    val task: Runnable,
    val time: Long,
    var isDisposed: Boolean = false
)