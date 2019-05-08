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
@JsonPropertyOrder("assetName", "domainId", "precision")
class CreateAsset {

    @JsonProperty("assetName")
    @get:JsonProperty("assetName")
    @set:JsonProperty("assetName")
    var assetName: String? = null
    @JsonProperty("domainId")
    @get:JsonProperty("domainId")
    @set:JsonProperty("domainId")
    var domainId: String? = null
    @JsonProperty("precision")
    @get:JsonProperty("precision")
    @set:JsonProperty("precision")
    var precision: Int? = null
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
