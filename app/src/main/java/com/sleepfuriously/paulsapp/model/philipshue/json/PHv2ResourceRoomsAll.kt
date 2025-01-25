package com.sleepfuriously.paulsapp.model.philipshue.json

import org.json.JSONObject

/**
 * Handles the data returned from
 *      https://<ip>/clip/v2/resource/room
 *
 * sample json:

{
    "errors": [],
    "data": [
    {
        "id": "624edadb-82e3-4bb8-bcde-acba757e6624",
        "id_v1": "/groups/81",
        "children": [
        {
            "rid": "b23dc287-34e7-480d-a232-bea15a5a3557",
            "rtype": "device"
        },
        {
            "rid": "bcaea8c1-c476-4eb1-a641-a6129145517c",
            "rtype": "device"
        },
        {
            "rid": "ef1681c2-6c08-4422-b53a-ba8af33ccf3e",
            "rtype": "device"
        }
        ],
        "services": [
        {
            "rid": "b4775662-ab9a-43a8-9a63-2f7dcfab3224",
            "rtype": "grouped_light"
        }
        ],
        "metadata": {
        "name": "Den",
        "archetype": "office"
    },
        "type": "room"
    }
    ]
}

*/
data class PHv2ResourceRoomsAll(
    val errors: List<PHv2Error> = listOf(),
    val data: List<PHv2Room> = listOf()
) {

    /**
     * Returns the number of rooms in this resource
     */
    fun size() : Int {
        return data.size
    }

    companion object {
        /**
         * Alternate constructor.  Takes the actual json string that's returned
         * from calling
         *      https://<ip>/clip/v2/resource/room
         *
         * @param   jsonObject  The json object that represents this class.
         *                      Unlike the other class's alternate constructors,
         *                      this IS the json, not its parent!!!
         *
         * @return  A [PHv2ResourceRoomsAll] instance.  Could be just two empty
         *          lists if the appropriate data can't be found.
         */
        operator fun invoke(jsonObject: JSONObject) : PHv2ResourceRoomsAll {
            val errors = mutableListOf<PHv2Error>()
            val data = mutableListOf<PHv2Room>()

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
                    val room = PHv2Room(arrayObject)
                    data.add(room)
                }
            }

            return PHv2ResourceRoomsAll(errors, data)
        }

        /**
         * Another constructor.  This takes a string that represents the
         * actual json data that represents this class.  Whew!
         */
        operator fun invoke(jsonObjectString: String) : PHv2ResourceRoomsAll {
            val jsonObject = JSONObject(jsonObjectString)
            return PHv2ResourceRoomsAll(jsonObject)
        }
    }
}

/**
 * This class represents data as returned by a call to a bridge:
 *      https://<bridge_ip>/clip/v2/resource/room/<room_id>
 */
data class PHv2RoomIndividual(
    val errors: List<PHv2Error> = listOf(),
    val data: List<PHv2Room> = listOf()
) {
    companion object {
        /**
         * Alternate constructor.  Takes the actual json string that's returned
         * from calling
         *      https://<ip>/clip/v2/resource/room/<room_id>
         *
         * @param   jsonObject  The json object that represents this class.
         *                      Unlike the other class's alternate constructors,
         *                      this IS the json, not its parent!!!
         *
         * @return  A [PHv2RoomIndividual] instance.  Could be just two empty
         *          lists if the appropriate data can't be found.
         */
        operator fun invoke(jsonObject: JSONObject) : PHv2RoomIndividual {
            val errors = mutableListOf<PHv2Error>()
            val data = mutableListOf<PHv2Room>()

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
                    val room = PHv2Room(arrayObject)
                    data.add(room)
                }
            }

            return PHv2RoomIndividual(errors, data)
        }

        /**
         * Another constructor.  This takes a string that represents the
         * actual json data that represents this class.  Whew!
         */
        operator fun invoke(jsonObjectString: String) : PHv2RoomIndividual {
            val jsonObject = JSONObject(jsonObjectString)
            return PHv2RoomIndividual(jsonObject)
        }

    }
}


/**
 * Room data structure.  This is generally one of many within a json array.
 * Identified by the [type] parameter rather than a key.
 *
 */
data class PHv2Room(
    /** really needs to be "room".  Anything else is just wrong. */
    val type: String,
    val id: String,
    val idV1: String,
    /** all the devices (things) in this room (could be services too) */
    val children: List<PHv2ItemInArray> = listOf(),
    /** Services that use this room whole or in part. Very broad. */
    val services: List<PHv2ItemInArray> = listOf(),
    /** Inludes human-readable info about this room */
    val metadata: PHv2RoomMetadata
) {
    companion object {
        /**
         * Alternate constructor. Takes a json object that represents THIS ROOM
         * (not a parent json object).
         */
        operator fun invoke(roomJsonObject: JSONObject) : PHv2Room {
            val type = roomJsonObject.getString(TYPE)

            val id = roomJsonObject.getString(ID)
            val idV1 = roomJsonObject.getString(ID_V1)

            val childrenJsonArray = roomJsonObject.getJSONArray(CHILDREN)
            val childrenList = mutableListOf<PHv2ItemInArray>()
            for (i in 0 until childrenJsonArray.length()) {
                val childJsonObject = childrenJsonArray.getJSONObject(i)
                val child = PHv2ItemInArray(childJsonObject)
                childrenList.add(child)
            }

            val servicesJsonArray = roomJsonObject.getJSONArray(SERVICES)
            val servicesList = mutableListOf<PHv2ItemInArray>()
            for (i in 0 until servicesJsonArray.length()) {
                val serviceJsonObject = servicesJsonArray.getJSONObject(i)
                val service = PHv2ItemInArray(serviceJsonObject)
                servicesList.add(service)
            }

            val metadata = PHv2RoomMetadata(roomJsonObject)

            return PHv2Room(
                type = type,
                id = id,
                idV1 = idV1,
                children = childrenList,
                services = servicesList,
                metadata = metadata
            )
        }

        /**
         * Another constructor.  This one uses a String that represents this
         * json object (not the parent).
         */
        operator fun invoke(jsonObjectString: String) : PHv2Room {
            val jsonObject = JSONObject(jsonObjectString)
            return PHv2Room(jsonObject)
        }
    }
}

/**
 * Configuration object for a room (taken from docs).
 *
 * Looks like this info is used to make the object human-readable.
 */
data class PHv2RoomMetadata(
    /** Display name of this room */
    val name: String,
    /** general category for the room */
    val archetype: String
) {
    companion object {
        operator fun invoke(parentJsonObject: JSONObject) : PHv2RoomMetadata {
            if (parentJsonObject.has(METADATA)) {
                val metadataJsonObject = parentJsonObject.getJSONObject(METADATA)
                val name = metadataJsonObject.getString(NAME)
                val archetype = metadataJsonObject.getString(ARCHETYPE)

                return PHv2RoomMetadata(name = name, archetype = archetype)
            }
            else {
                return PHv2RoomMetadata(EMPTY_STRING, EMPTY_STRING)
            }
        }
    }
}
