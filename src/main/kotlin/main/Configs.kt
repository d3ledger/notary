package main

/**
 * Class incapsulates all configuration parameters for the application.
 */
object Configs {

    /** Port for refund REST API */
    var refundPort = 8080

    /** Path to public key */
    var pubkeyPath = "example/admin@test.pub"

    /** Path to private key */
    var privkeyPath = "example/admin@test.priv"

    /** Iroha account for creator txs */
    var irohaCreator = "admin@test"

    /** Iroha peer hsotname */
    var irohaHostname = "localhost"

    /** Iroha peer hsotname */
    var irohaPort = 50051
}