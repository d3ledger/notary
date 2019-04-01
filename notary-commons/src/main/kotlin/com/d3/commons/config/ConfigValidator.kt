package com.d3.commons.config

interface ConfigValidationRule<T> {
    /* Validation rule itself. Must throw IllegalArgumentException in case of validation violations*/
    fun validate(config: T)
}
