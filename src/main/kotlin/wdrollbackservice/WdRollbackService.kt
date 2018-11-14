package wdrollbackservice

import com.github.kittinunf.result.Result
import sidechain.SideChainEvent

interface WdRollbackService {

    fun monitorWithdrawal(irohaEvent: SideChainEvent.IrohaEvent): Result<Unit, Exception>
}
