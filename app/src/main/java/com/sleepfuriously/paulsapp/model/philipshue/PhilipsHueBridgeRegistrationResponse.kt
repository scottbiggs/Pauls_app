package com.sleepfuriously.paulsapp.model.philipshue

/**
 * Responses from the philips hue bridge when registering this
 * app with it.
 *
 * Note that it's possible to have both a success and an error.
 */
class PhilipsHueBridgeRegistrationResponse : ArrayList<PhilipsHueBridgeRegistrationResponseItem>()

data class PhilipsHueBridgeRegistrationResponseItem(
    /** indicates some sort of error */
    val error: Error?,
    /** Indicates some sort of success */
    val success: Success?
) {

    data class Error(
        val type: Int,
        val address: String,
        val description: String
    )

    data class Success(
        /** this is the whole point of registering--the username IS the token! */
        val username: String
    )


}


