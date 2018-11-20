package wdrollbackservice

import com.github.kittinunf.result.Result
import io.reactivex.Observable

interface WdRollbackService {

    /**
     * Process internal events and form an output event describing rollback need
     */
    fun monitorWithdrawal(): Observable<Result<List<WdRollbackServiceOutputEvent>, Exception>>

    /**
     * Process an output event describing rollback need and perform a rollback
     */
    fun initiateRollback(wdRollbackServiceOutputEvent: WdRollbackServiceOutputEvent): Result<String, Exception>
}
