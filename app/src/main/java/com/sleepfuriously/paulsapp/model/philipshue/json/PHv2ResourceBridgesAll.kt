package com.sleepfuriously.paulsapp.model.philipshue.json

import org.json.JSONObject

/**
 * Handles the data returned from
 *      https://<ip>/clip/v2/resource/bridge
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
data class PHv2ResourceBridgesAll(
    val errors: List<PHv2Error> = listOf(),
    val data: List<PHv2Bridge> = listOf()
) {
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
         * @return  A [PHv2ResourceBridgesAll] instance.  Could be just two empty
         *          lists if the appropriate data can't be found.
         */
        operator fun invoke(jsonObject: JSONObject) : PHv2ResourceBridgesAll {
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

            return PHv2ResourceBridgesAll(errors, data)
        }

        /**
         * Takes a string representation instead of a json object of
         * this class.
         */
        operator fun invoke(jsonString: String) : PHv2ResourceBridgesAll {
            val jsonObject = JSONObject(jsonString)
            return PHv2ResourceBridgesAll(jsonObject)
        }
    }
}

/**
 * Results from:
 *      https://<ip>/clip/v2/resource/bridge/<bridge_id>
 */
data class PHv2ResourceBridgeIndividual(
    val errors: List<PHv2Error> = listOf(),
    val data: List<PHv2Bridge> = listOf()
) {
    companion object {
        /**
         * Alternate constructor.  Takes the actual json string that's returned
         * from calling
         *      https://<ip>/clip/v2/resource/bridge/<bridge_id>
         *
         * @param   jsonObject  The json object that represents this class.
         *                      Unlike the other class's alternate constructors,
         *                      this IS the json, not its parent!!!
         *
         * @return  A [PHv2ResourceBridgeIndividual] instance.  Could be just two empty
         *          lists if the appropriate data can't be found.
         */
        operator fun invoke(jsonObject: JSONObject) : PHv2ResourceBridgeIndividual {
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
            return PHv2ResourceBridgeIndividual(errors, data)
        }

        /**
         * Takes a string representation instead of a json object of
         * this class.
         */
        operator fun invoke(jsonString: String) : PHv2ResourceBridgeIndividual {
            val jsonObject = JSONObject(jsonString)
            return PHv2ResourceBridgeIndividual(jsonObject)
        }
    }
}

data class PHv2Bridge(
    val type: String = EMPTY_STRING,
    val id: String,
    val idV1: String,
    val owner: PHv2ItemInArray = PHv2ItemInArray(EMPTY_STRING, EMPTY_STRING),
    val children: List<PHv2ItemInArray> = listOf(),
    val services: List<PHv2ItemInArray> = listOf(),
    val metadata: PHv2BridgeMetadata = PHv2BridgeMetadata(JSONObject())
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

            val childrenList = mutableListOf<PHv2ItemInArray>()
            if (bridgeJsonObject.has(CHILDREN)) {
                val childrenJsonArray = bridgeJsonObject.getJSONArray(CHILDREN)
                for (i in 0 until childrenJsonArray.length()) {
                    val childJsonObject = childrenJsonArray[i] as JSONObject
                    val child = PHv2ItemInArray(childJsonObject)
                    childrenList.add(child)
                }
            }

            val servicesList = mutableListOf<PHv2ItemInArray>()
            if (bridgeJsonObject.has(SERVICES)) {
                val servicesJsonArray = bridgeJsonObject.getJSONArray(SERVICES)
                for (i in 0 until servicesJsonArray.length()) {
                    val serviceJsonObject = servicesJsonArray[i] as JSONObject
                    val service = PHv2ItemInArray(serviceJsonObject)
                    servicesList.add(service)
                }
            }

            val metadata = PHv2BridgeMetadata(bridgeJsonObject)

            return PHv2Bridge(
                type = type,
                id = id,
                idV1 = idV1,
                owner = owner,
                children = childrenList,
                services = servicesList,
                metadata = metadata
            )
        }
    }
}


data class PHv2BridgeMetadata(
    val name: String,
    val archetype: String
) {
    companion object {
        /**
         * Alternate constructor
         *
         * @param   parentJsonObject  A json object that CONTAINS a json object that is
         *                      the equivalent to this class.
         *
         * @return  [PHv2BridgeMetadata] instance.  If not found, a basic instance is created.
         */
        operator fun invoke(parentJsonObject: JSONObject) : PHv2BridgeMetadata {
            if (parentJsonObject.has(METADATA)) {
                val metadataJsonObject = parentJsonObject.getJSONObject(METADATA)

                val name = if (metadataJsonObject.has(NAME)) {
                    metadataJsonObject.getString(NAME)
                }
                else { EMPTY_STRING }

                val archetype = if (metadataJsonObject.has(ARCHETYPE)) {
                    metadataJsonObject.getString(ARCHETYPE)
                }
                else { EMPTY_STRING }

                return PHv2BridgeMetadata(name, archetype)
            }
            else {
                return PHv2BridgeMetadata(EMPTY_STRING, EMPTY_STRING)
            }
        }
    }
}
