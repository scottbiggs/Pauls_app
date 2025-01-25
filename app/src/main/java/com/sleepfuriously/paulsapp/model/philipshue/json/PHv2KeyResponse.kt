package com.sleepfuriously.paulsapp.model.philipshue.json

import org.json.JSONArray
import org.json.JSONObject

/**
 * This class represents the data returned by a bridge when
 * there's a POST request to fetch a token/password.
 *
 *      https://<ip>/api
 *      body = {"devicetype":"app_name#instance_name", "generateclientkey":true}
 *
 * NOTE:
 *      This works both version 1 and version 2 (according to docs!)
 */
data class PHv2KeyResponse(
    val error: List<PHKeyError> = listOf(),
    val success: List<PHKeySuccess> = listOf()
) {
    companion object {
        /**
         * @param   jsonArrayString     The string representing the
         *                              json array that contains the
         *                              data we're interested in. (For
         *                              some reason, it's returned as
         *                              an array, not an object.)
         */
        operator fun invoke(jsonArrayString: String) : PHv2KeyResponse {
            val parentJsonArray = JSONArray(jsonArrayString)
            val errorList = mutableListOf<PHKeyError>()
            val successList = mutableListOf<PHKeySuccess>()

            for (i in 0 until parentJsonArray.length()) {
                val jsonObject = parentJsonArray.getJSONObject(i)
                if (jsonObject.has(ERROR)) {
                    val error = PHKeyError(jsonObject)
                    errorList.add(error)
                }

                if (jsonObject.has(SUCCESS)) {
                    val success = PHKeySuccess(jsonObject)
                    successList.add(success)
                }
            }

            // only the first items are ever used
            return PHv2KeyResponse(error = errorList, success = successList)
        }
    }
}


data class PHKeyError(
    val type: Int,
    val description: String
) {
    /**
     * Takes a json object that represents the error (NOT the parent!).
     */
    companion object {
        operator fun invoke(jsonObject: JSONObject) : PHKeyError {
            var type = 0
            if (jsonObject.has(TYPE)) {
                type = jsonObject.getInt(TYPE)
            }

            var desc = "no description"
            if (jsonObject.has(DESCRIPTION)) {
                desc = jsonObject.getString(DESCRIPTION)
            }

            return PHKeyError(type = type, description = desc)
        }
    }
}

/**
 * Holds the data returned on a successful POST.
 */
data class PHKeySuccess(
    val username: String,
    val clientkey: String
) {
    companion object {
        /**
         * This one needs the PARENT object.
         */
        operator fun invoke(parentJsonObject: JSONObject) : PHKeySuccess {
            if (parentJsonObject.has(SUCCESS)) {
                val successJsonObject = parentJsonObject.getJSONObject(SUCCESS)

                val user = successJsonObject.getString(USERNAME)
                val client = successJsonObject.getString(CLIENTKEY)

                return PHKeySuccess(username = user, clientkey = client)
            }
            else {
                return PHKeySuccess(EMPTY_STRING, EMPTY_STRING)
            }
        }
    }
}