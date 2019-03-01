package jp.co.soramitsu.bootstrap.dto.block

import java.util.HashMap
import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("commands", "createdTime", "quorum")
class ReducedPayload {

    @JsonProperty("commands")
    @get:JsonProperty("commands")
    @set:JsonProperty("commands")
    var commands: List<Command>? = null
    @JsonProperty("createdTime")
    @get:JsonProperty("createdTime")
    @set:JsonProperty("createdTime")
    var createdTime: String? = null
    @JsonProperty("quorum")
    @get:JsonProperty("quorum")
    @set:JsonProperty("quorum")
    var quorum: Int? = null
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
