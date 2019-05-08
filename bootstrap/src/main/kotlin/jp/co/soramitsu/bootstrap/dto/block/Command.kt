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
@JsonPropertyOrder("addPeer", "createRole", "createDomain", "createAsset", "createAccount")
class Command {

    @JsonProperty("addPeer")
    @get:JsonProperty("addPeer")
    @set:JsonProperty("addPeer")
    var addPeer: AddPeer? = null
    @JsonProperty("createRole")
    @get:JsonProperty("createRole")
    @set:JsonProperty("createRole")
    var createRole: CreateRole? = null
    @JsonProperty("createDomain")
    @get:JsonProperty("createDomain")
    @set:JsonProperty("createDomain")
    var createDomain: CreateDomain? = null
    @JsonProperty("createAsset")
    @get:JsonProperty("createAsset")
    @set:JsonProperty("createAsset")
    var createAsset: CreateAsset? = null
    @JsonProperty("createAccount")
    @get:JsonProperty("createAccount")
    @set:JsonProperty("createAccount")
    var createAccount: CreateAccount? = null
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
