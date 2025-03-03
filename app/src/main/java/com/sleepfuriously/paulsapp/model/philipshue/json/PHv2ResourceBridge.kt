package com.sleepfuriously.paulsapp.model.philipshue.json

import org.json.JSONObject

/**
 * Handles the data returned from
 *      https://<ip>/clip/v2/resource/bridge
 *
 * or a call from
 *      https://<ip>/clip/v2/resource/bridge/<bridge_id>
 *
 * It seems that since a bridge is always unique, there is no difference.
 *
 * sample json:

{
    "errors": [],
    "data": [
        {
            "id": "96cc8d45-ead6-4a88-8c2b-598923846c66",
            "owner": {
                "rid": "8afed737-b415-4cf9-848f-a22b08a3c929",
                "rtype": "device"
            },
            "bridge_id": "ecb5fafffe11407e",
            "time_zone": {
                "time_zone": "America/Chicago"
            },
            "type": "bridge"
        }
    ]
}

 */
data class PHv2ResourceBridge(
    private val errors: List<PHv2Error> = listOf(),
    private val data: List<PHv2Bridge> = listOf()
) {
    /**
     * Simply tells if any data is in here or not.  If not,
     * then use [getError] to figure out why.
     */
    fun hasData() : Boolean {
        return data.isNotEmpty()
    }

    /**
     * Returns the error message for this class.  If no error
     * then empty string is returned.
     */
    fun getError() : String {
        return if (errors.isEmpty()) {
            ""
        } else {
            errors[0].description
        }
    }

    /**
     * Returns id.  If error, then empty string is returned.
     */
    fun getId() : String {
        return if (data.isEmpty()) {
            ""
        }
        else {
            data[0].id
        }
    }

    /**
     * Returns name as printed on the bridge.  Empty on error.
     */
    fun getName() : String {
        return if (data.isEmpty()) {
            ""
        }
        else {
            data[0].bridgeName
        }
    }

    /**
     * Returns the owner of this bridge.  Shouldn't be needed for our practices here,
     * but I'm including for completeness.
     *
     * Will be null if this instance is an error.
     */
    @Suppress("unused")
    fun getOwner() : PHv2ItemInArray? {
        return if (data.isEmpty()) {
            null
        }
        else {
            data[0].owner
        }
    }

    @Suppress("unused")
    fun getTimeZone() : String {
        return if (data.isEmpty()) {
            ""
        }
        else {
            data[0].timeZone.timeZone
        }
    }

    @Suppress("unused")
    fun getType() : String {
        return if (data.isEmpty()) {
            ""
        }
        else {
            data[0].type
        }
    }

    companion object {
        /**
         * Alternate constructor.  Takes the actual json string that's returned
         * from calling
         *      https://<ip>/clip/v2/resource/bridge
         *
         * @param   jsonObject  The json object that represents this class.
         *                      Unlike the other class's alternate constructors,
         *                      this IS the json, not its parent!!!
         *
         * @return  A [PHv2ResourceBridge] instance.  Could be just two empty
         *          lists if the appropriate data can't be found.
         */
        operator fun invoke(jsonObject: JSONObject) : PHv2ResourceBridge {
            val errors = mutableListOf<PHv2Error>()
            val data = mutableListOf<PHv2Bridge>()

            if (jsonObject.has(ERRORS)) {
                val errorsJsonArray = jsonObject.getJSONArray(ERRORS)
                for (i in 0 until errorsJsonArray.length()) {
                    errors.add(PHv2Error(errorsJsonArray.getJSONObject(i)))
                }
            }

            if (jsonObject.has(DATA)) {
                val dataJsonArray = jsonObject.getJSONArray(DATA)
                for (i in 0 until dataJsonArray.length()) {
                    val arrayObject = dataJsonArray[i] as JSONObject
                    val bridge = PHv2Bridge(arrayObject)
                    data.add(bridge)
                }
            }

            return PHv2ResourceBridge(errors, data)
        }

        /**
         * Takes a string representation instead of a json object of
         * this class.
         */
        operator fun invoke(jsonString: String) : PHv2ResourceBridge {
            val jsonObject = JSONObject(jsonString)
            return PHv2ResourceBridge(jsonObject)
        }
    }
}


data class PHv2Bridge(
    /** this should be "bridge" */
    val type: String = EMPTY_STRING,
    val id: String,
    val idV1: String = EMPTY_STRING,
    val owner: PHv2ItemInArray,
    /** id as printed on the device */
    val bridgeName: String,
    val timeZone: PHv2BridgeTimeZone
) {
    companion object {
        /**
         * Alternate constructor
         *
         * @param   bridgeJsonObject    A json object that IS represents a [PHv2Bridge]
         *                              class.
         *
         * @return  [PHv2Bridge] instance.  If not found, a basic instance is created.
         */
        operator fun invoke(bridgeJsonObject: JSONObject) : PHv2Bridge {
            val type = bridgeJsonObject.getString(TYPE)
            val id = bridgeJsonObject.getString(ID)
            val idV1 = if (bridgeJsonObject.has(ID_V1)) {
                bridgeJsonObject.getString(ID_V1)
            }
            else { EMPTY_STRING }

            val owner = PHv2ItemInArray(bridgeJsonObject.getJSONObject(OWNER))
            val name = bridgeJsonObject.getString(BRIDGE_ID)
            val timeZone = PHv2BridgeTimeZone(bridgeJsonObject)

            return PHv2Bridge(
                type = type,
                id = id,
                idV1 = idV1,
                owner = owner,
                bridgeName = name,
                timeZone = timeZone
            )
        }
    }
}


data class PHv2BridgeTimeZone(
    /** Time zone where the user's home is located (as Olson ID). */
    val timeZone: String
) {
    companion object {
        /**
         * Alternate constructor
         *
         * @param   parentJsonObject    A json object that CONTAINS a json object that is
         *                              the equivalent to this class.
         *
         * @return  [PHv2BridgeTimeZone] instance.  If not found, a basic instance is created.
         */
        operator fun invoke(parentJsonObject: JSONObject) : PHv2BridgeTimeZone {
            if (parentJsonObject.has(TIME_ZONE)) {
                val timeZoneJsonObject = parentJsonObject.getJSONObject(TIME_ZONE)
                return PHv2BridgeTimeZone(timeZoneJsonObject.getString(TIME_ZONE))
            }
            else {
                return PHv2BridgeTimeZone(timeZone = "")
            }
        }
    }
}
