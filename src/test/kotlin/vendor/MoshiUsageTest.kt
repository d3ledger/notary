package vendor

import com.squareup.moshi.Moshi
import mu.KLogging
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

data class Engine(val temperature: Int)

data class Passenger(val name: String)

data class Car(val engine: Engine, val passengers: List<Passenger>)

class MoshiUsageTest {
    @Test
    fun usageTest() {
        val moshi = Moshi.Builder().build()
        val jsonAdapter = moshi.adapter(Car::class.java)

        val car = Car(Engine(452), listOf(Passenger("Joker"), Passenger("Hartman")))

        val jsonRepr = jsonAdapter.toJson(car)
        logger.info { "JSON representation:$jsonRepr" }

        val jsonCar = jsonAdapter.fromJson(jsonRepr)

        assertEquals(car, jsonCar)
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
