/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.bootstrap.dto.block

import java.util.HashMap
import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("transactions", "txNumber", "height", "createdTime")
class Payload {

    @JsonProperty("transactions")
    @get:JsonProperty("transactions")
    @set:JsonProperty("transactions")
    var transactions: List<Transaction>? = null
    @JsonProperty("txNumber")
    @get:JsonProperty("txNumber")
    @set:JsonProperty("txNumber")
    var txNumber: Int? = null
    @JsonProperty("height")
    @get:JsonProperty("height")
    @set:JsonProperty("height")
    var height: String? = null
    @JsonProperty("createdTime")
    @get:JsonProperty("createdTime")
    @set:JsonProperty("createdTime")
    var createdTime: String? = null
    @JsonIgnore
    private val additionalProperties = HashMap<String, Any>()

    @JsonAnyGetter
    fun getAdditionalProperties(): Map<String, Any> {
        return this.additionalProperties
    }

    @JsonAnySetter
    fun setAdditionalProperty(name: String, value: Any) {
        this.additionalProperties[name] = value
    }

}
