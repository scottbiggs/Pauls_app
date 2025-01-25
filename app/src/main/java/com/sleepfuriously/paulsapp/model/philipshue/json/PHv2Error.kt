package com.sleepfuriously.paulsapp.model.philipshue.json

import org.json.JSONObject

/**
 * Error data for bad GET call
 */
data class PHv2Error(
    val description: String = NO_ERR_SPECIFIED
) {
    companion object {
        /**
         * Unlike most of these companion object alternate constructors, this
         * USES THE JSON THAT HOLDS THE DATA, **NOT the PARENT**!!!
         *
         * @return  A [PHv2Error] instance that corresponds to the
         */
        operator fun invoke(errorJsonObject: JSONObject) : PHv2Error {
            if (errorJsonObject.has(DESCRIPTION)) {
                val desc = errorJsonObject.getString(DESCRIPTION)
                return PHv2Error(desc)
            }
            else {
                return PHv2Error()
            }
        }
    }
}

