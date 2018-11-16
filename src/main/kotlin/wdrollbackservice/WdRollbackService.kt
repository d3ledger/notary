package wdrollbackservice

import com.github.kittinunf.result.Result
import sidechain.SideChainEvent

interface WdRollbackService {

    /**
     * Process internal events and initiate rollback if needed
     */
    fun monitorWithdrawal(): Result<Unit, Exception>
}
