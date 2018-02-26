package com.email.mocks

import com.email.bgworker.BackgroundWorker
import com.email.bgworker.WorkRunner
import org.amshove.kluent.`should equal`
import java.util.*

/**
 * Created by gabriel on 2/21/18.
 */

class MockedWorkRunner: WorkRunner {
    private val pendingWork: LinkedList<BackgroundWorker<*>> = LinkedList()
    var totalRuns = 0
        private set

    fun getTotalWorkersInQueue() = pendingWork.size

    fun discardPendingWork() {
        pendingWork.clear()
    }

    fun _work() {
        if (pendingWork.isNotEmpty()) {
            val worker = pendingWork.pop()
            val task = MockedAsyncTask(worker)
            task.execute()
        }
    }

    fun assertPendingWork(expectedPendingWork : List<Class<*>>) {
        pendingWork.map { it.javaClass } `should equal` expectedPendingWork
    }

    override fun workInBackground(worker: BackgroundWorker<*>) {
        totalRuns += 1
        pendingWork.add(worker)
    }

    private class MockedAsyncTask<U>(private val worker: BackgroundWorker<U>) {

        fun execute() {
            val result = try { worker.work() } catch (ex: Exception) {
                worker.catchException(ex)
            }

            if (result != null)
                worker.publishFn(result)
        }
    }
}