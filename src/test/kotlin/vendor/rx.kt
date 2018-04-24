package vendor

import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toObservable
import org.junit.Assert
import org.junit.Test


class KotlinJunitTest {

    /**
     * Rx usage test
     */
    @Test
    fun rxUsage() {
        val list = listOf(1, 1, 1, 1)

        list.toObservable()
                .subscribeBy(  // named arguments for lambda Subscribers
                        onNext = { Assert.assertTrue(it == 1) }
                )
    }

}
