package com.sleepfuriously.paulsapp.model.philipshue.json

import org.json.JSONObject

/**
 * Represents the json returned from a bridge GET about its zones.
 *
 *      https://<bridge_ip>/clip/v2/resource/zone
 */
data class PHv2ResourceZonesAll(
    /** Any errors should be explained here */
    val errors: List<PHv2Error> = listOf(),
    /** List of all the Zones this bridge knows about */
    val data: List<PHv2Zone> = listOf()
) {
    companion object {
        /**
         * Alternate constructor: this takes a json object representing
         * this class and converts it to a [PHv2ResourceZonesAll].
         */
        operator fun invoke(jsonObj: JSONObject) : PHv2ResourceZonesAll {
            // first the errors
            val errors = mutableListOf<PHv2Error>()
            val errorsJsonArray = jsonObj.optJSONArray(ERRORS)
            if (errorsJsonArray != null) {
                for (i in 0 until errorsJsonArray.length()) {
                    errors.add(PHv2Error(errorsJsonArray[i] as JSONObject))
                }
            }

            // similar for the data
            val data = mutableListOf<PHv2Zone>()
            val dataJsonArray = jsonObj.optJSONArray(DATA)
            if (dataJsonArray != null) {
                for (i in 0 until dataJsonArray.length()) {
                    data.add(PHv2Zone(dataJsonArray[i] as JSONObject))
                }
            }
            return PHv2ResourceZonesAll(errors, data)
        }

        /**
         * Another alternate constructor.  Takes a string representing the
         * json for this data.
         */
        operator fun invoke(jsonStr: String) : PHv2ResourceZonesAll {
            return PHv2ResourceZonesAll(JSONObject(jsonStr))
        }
    }
}

/**
 * Response when GETting:
 *      https://<bridge_ip>/clip/v2/resource/zone/<zone_id>
 *
 * note:
 *  This is EXACTLY the same you'd get for [PHv2ResourceZonesAll], except that the
 *  [data] array has just one (or no) items.
 */
data class PHv2ResourceZoneIndividual(
    val errors: List<PHv2Error> = listOf(),
    val data: List<PHv2Zone> = listOf()
) {
    companion object {
        /**
         * Alternate constructor: takes json object representing this data.
         */
        operator fun invoke(jsonObj: JSONObject) : PHv2ResourceZoneIndividual {
            val errors = mutableListOf<PHv2Error>()
            val errorsJsonArray = jsonObj.optJSONArray(ERRORS)
            if (errorsJsonArray != null) {
                for (i in 0 until errorsJsonArray.length()) {
                    errors.add(PHv2Error(errorsJsonArray[i] as JSONObject))
                }
            }

            // just one data is possible
            val data = mutableListOf<PHv2Zone>()
            val dataJsonArray = jsonObj.optJSONArray(DATA)
            if ((dataJsonArray != null) && (dataJsonArray.length() > 0)) {
                data.add(PHv2Zone(dataJsonArray[0] as JSONObject))
            }
            return PHv2ResourceZoneIndividual(errors, data)
        }
    }
}


/**
 * This is where the good stuff is!  All the real data is kept in this class.
 * Parent classes are really just dressing.
 */
data class PHv2Zone(
    val id: String,
    val idV1: String,
    /** must be "zone" */
    val type: String,
    /** All the devices (and possibly services?) aligned with this zone */
    val children: List<PHv2ItemInArray>,
    /**
     * from the docs--I didn't write this!!!
     *
     * "References all services aggregating control and state of children in the group.
     * This includes all services grouped in the group hierarchy given by child relation.
     * This includes all services of a device grouped in the group hierarchy given by child
     * relation Aggregation is per service type, ie every service type which can be grouped
     * has a corresponding definition of grouped type Supported types: –
     * grouped_light – grouped_motion – grouped_light_level"
     */
    val services: List<PHv2ItemInArray>,
    val metadata: PHv2RoomMetadata
) {
    companion object {
        /**
         * Alternate constructor - takes a json object that represents THIS data
         */
        operator fun invoke(jsonObj: JSONObject) : PHv2Zone {

            val children = mutableListOf<PHv2ItemInArray>()
            val childrenJsonArray = jsonObj.getJSONArray(CHILDREN)
            for (i in 0 until childrenJsonArray.length()) {
                children.add(PHv2ItemInArray(childrenJsonArray[i] as JSONObject))
            }

            val services = mutableListOf<PHv2ItemInArray>()
            val servicesJsonArray = jsonObj.getJSONArray(SERVICES)
            for (i in 0 until servicesJsonArray.length()) {
                services.add(PHv2ItemInArray(servicesJsonArray[i] as JSONObject))
            }

            return PHv2Zone(
                id = jsonObj.getString(ID),
                idV1 = jsonObj.optString(ID_V1, ""),
                type = jsonObj.optString(TYPE, "zone"),
                metadata = PHv2RoomMetadata(jsonObj.getJSONObject(METADATA)),
                children = children,
                services = services,
            )
        }
    }
}