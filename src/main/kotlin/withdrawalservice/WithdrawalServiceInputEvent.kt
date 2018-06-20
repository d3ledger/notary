package withdrawalservice


/**
 * All events that [WithdrawalService] is interested in
 */
sealed class WithdrawalServiceInputEvent {

    /**
     * All Iroha chain events that [WithdrawalService] is interested in
     */
    sealed class IrohaInputEvent : WithdrawalServiceInputEvent() {

        /**
         * Initiate rollback case
         */
        class Rollback() : IrohaInputEvent()

        /**
         * Initiate withdrawal case
         */
        class Withdrawal() : IrohaInputEvent()
    }
}
