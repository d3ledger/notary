package util.functional

/**
 * Functional application of lambda to nullable type
 * @return application of lambda to value or null
 */
fun <T, R> T?.map(map: (value: T) -> R): R? {
    return if (this != null) {
        map(this)
    } else null
}

/**
 * Call different lambdas with respect to the input value
 * @return application of lambda to value if it exists
 */
fun <T, R> T?.end(value: (value: T) -> R?, empty: () -> R?): R? {
    return if (this != null) {
        value(this)
    } else empty()
}
