package com.d3.notifications.push

import com.github.kittinunf.result.Result

/**
 * Interface of service that is used to send web push notifications using Push API
 */
interface WebPushAPIService {

    /**
     * Sends push notification
     * @param accountId - account id of client that will receive push notification
     * @param message - message of notification
     */
    fun push(accountId: String, message: String): Result<Unit, Exception>

}
