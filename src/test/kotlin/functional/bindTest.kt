package functional

import org.junit.Assert
import org.junit.Test
import util.functional.end
import util.functional.map

class bindTest {
    @Test
    fun mapUsage() {
        val a: Int? = 1
        val binded = a.map {
            "1"
        }.map {
            it + it
        }
        Assert.assertEquals("11", binded)
    }

    @Test
    fun endUsage() {
        val a: Int? = 1
        a.end({
            Assert.assertTrue(true)
        }, {
            Assert.assertTrue(false)
        })
    }
}
