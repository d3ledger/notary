package wdrollbackservice

import com.github.kittinunf.result.Result
import io.reactivex.Observable

interface WithdrawalRollbackService {

    /**
     * Process internal events and form an output event describing rollback need
     */
    fun monitorWithdrawal(): Observable<Result<WithdrawalRollbackServiceOutputEvent, Exception>>

    /**
     * Process an output event describing rollback need and perform a rollback
     */
    fun initiateRollback(withdrawalRollbackServiceOutputEvent: WithdrawalRollbackServiceOutputEvent): Result<String, Exception>
}
