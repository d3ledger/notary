package endpoint

/**
 * Class responsible for creating rollbacks from Iroha to side chains
 *
 * @param Request type of custodian's request
 * @param NotaryResponse type of notary response
 */
interface RollbackCreator<Request, NotaryResponse> {

    /**
     * Perform rollback for side chain
     */
    fun rollbackEndPoint(request: Request): NotaryResponse
}
