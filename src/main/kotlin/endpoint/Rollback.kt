package endpoint

/**
 * Class responsible for creating rollbacks from Iroha to side chains
 *
 * @param Request type of custodian's request
 * @param NotaryResponse type of notary response
 */
abstract class Rollback<Request, NotaryResponse> {

    /**
     * Perform rollback for side chain
     */
    abstract fun rollbackEndPoint(request: Request) : NotaryResponse
}
