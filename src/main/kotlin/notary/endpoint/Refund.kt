package notary.endpoint

/**
 * Class responsible for creating refunds from Iroha to side chains
 *
 * @param Request type of custodian's request
 * @param NotaryResponse type of notary response
 */
interface Refund<Request, NotaryResponse> {

    /**
     * Perform rollback for side chain
     */
    fun performRefund(request: Request): NotaryResponse
}
