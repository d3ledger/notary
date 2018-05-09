package main

/**
 * Class incapsulates all configuration parameters for the application.
 */
object Configs {

    /** Port for refund REST API */
    val refundPort = 8080

    /** Path to public key */
    val pubkeyPath = "example/admin@test.pub"

    /** Path to private key */
    val privkeyPath = "example/admin@test.priv"

    /** Iroha account for creator txs */
    val irohaCreator = "admin@test"

    /** Iroha peer hsotname */
    val irohaHostname = "localhost"

    /** Iroha peer hsotname */
    val irohaPort = 50051

    //--------- Ethereum ---------
    /** Confirmation period */
    val ethConfirmationPeriod: Long = 6

    /** URL of Ethereum client */
    val ethConnectionUrl = "http://0.0.0.0:8180/#/auth?token=P8VD-nlD5-8AB5-ZIJv"
}
