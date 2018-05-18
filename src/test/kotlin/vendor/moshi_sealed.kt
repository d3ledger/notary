package vendor

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import org.junit.Assert
import org.junit.Test

/**
 * Code below provide example of JSON serialization/deserialization of sealed classed
 */


/**
 * Target sealed class
 */
sealed class Base {
    data class A(val first: Int) : Base()
    data class B(val second: String) : Base()
}

/**
 * Data layer type which contains union with all parameters of each class plus type marker
 */
data class BaseLayer(val type: BaseType, val first: Int? = null, val second: String? = null)


/**
 * Type marker of the type
 */
enum class BaseType {
    A, B
}

/**
 * Moshi adapter that contains trivial transformation from [Base] to [BaseLayer] and vice versa
 */
class BaseLayerAdapter {
    @FromJson
    fun fromJson(baseLayer: BaseLayer) = when (baseLayer.type) {
        BaseType.A -> Base.A(baseLayer.first!!)
        BaseType.B -> Base.B(baseLayer.second!!)
    }

    @ToJson
    fun toJson(base: Base) = when (base) {
        is Base.A -> BaseLayer(BaseType.A, first = base.first)
        is Base.B -> BaseLayer(BaseType.B, second = base.second)
    }
}

class TestSealedClasses {

    /**
     * @given initialized builder with target adapter
     * @when  [Base.A] and [Base.B] is serialized
     * @then  check deserialized values
     */
    @Test
    fun sealedDeSerialization() {
        val moshi = Moshi.Builder().add(BaseLayerAdapter()).build()
        val adapter = moshi.adapter(Base::class.java)

        val a = Base.A(1)
        val aJson = adapter.toJson(a)

        val b = Base.B("second")
        val bJson = adapter.toJson(b)

        println(aJson)
        println(bJson)

        Assert.assertEquals(a, adapter.fromJson(aJson))
        Assert.assertEquals(b, adapter.fromJson(bJson))
    }
}
