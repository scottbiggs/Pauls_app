package com.sleepfuriously.paulsapp.model.philipshue.json

import org.json.JSONObject

/**
 * Data for the json object that holds information on a
 * group of lights.  Rooms and such are popular light groups
 * and are pretty common in the Philips Hue world.
 *
 * There are LOTS of different group_light thingies!  Many have
 * nothing to do with lights too.  I'm most interested in
 * "room", "light", and "grouped_light".  But I don't even know
 * where the complete documentation is for these things, sigh.
 *
 * But it looks like "grouped_light" is what we really want--it's
 * a service of Rooms and allows us to find out (and make changes)
 * to all the lights in a room at the same time.
 *
 * This is the top level data structure, and is the result from:
 *      https://<bridge_ip>/clip/v2/resource/grouped_light
 */
data class PHv2ResourceGroupedLightsAll(
    val errors: List<PHv2Error> = listOf(),
    /** Holds info about this group.  Typically holds just one item */
    val data: List<PHv2GroupedLight> = listOf()
) {
    companion object {
        /**
         * Alternate constructor: takes a json equivalent.
         */
        operator fun invoke(jsonObject: JSONObject) : PHv2ResourceGroupedLightsAll {
            val errorsList = mutableListOf<PHv2Error>()
            val dataList = mutableListOf<PHv2GroupedLight>()

            if (jsonObject.has(ERRORS)) {
                val errorsJsonArray = jsonObject.getJSONArray(ERRORS)
                for (i in 0 until errorsJsonArray.length()) {
                    errorsList.add(PHv2Error(errorsJsonArray.getJSONObject(i)))
                }
            }

            if (jsonObject.has(DATA)) {
                val dataJsonArray = jsonObject.getJSONArray(DATA)
                for (i in 0 until dataJsonArray.length()) {
                    dataList.add(PHv2GroupedLight(dataJsonArray.getJSONObject(i)))
                }
            }

            return PHv2ResourceGroupedLightsAll(errorsList, dataList)
        }
    }
}

/**
 * Result from calling:
 *      https://<bridge_ip>/clip/v2/resource/grouped_light<grouped_light_id>
 */
data class PHv2GroupedLightIndividual(
    val errors: List<PHv2Error> = listOf(),
    /** Holds info about this group.  Typically holds just one item */
    val data: List<PHv2GroupedLight> = listOf()
) {
    companion object {
        /**
         * Alternate constructor.  This takes the json object that represents
         * this exact data, NOT the parent.
         */
        operator fun invoke(jsonObject: JSONObject) : PHv2GroupedLightIndividual {
            val errorsList = mutableListOf<PHv2Error>()
            val dataList = mutableListOf<PHv2GroupedLight>()

            if (jsonObject.has(ERRORS)) {
                val errorsJsonArray = jsonObject.getJSONArray(ERRORS)
                for (i in 0 until errorsJsonArray.length()) {
                    errorsList.add(PHv2Error(errorsJsonArray.getJSONObject(i)))
                }
            }

            if (jsonObject.has(DATA)) {
                val dataJsonArray = jsonObject.getJSONArray(DATA)
                for (i in 0 until dataJsonArray.length()) {
                    dataList.add(PHv2GroupedLight(dataJsonArray.getJSONObject(i)))
                }
            }
            return PHv2GroupedLightIndividual(errorsList, dataList)
        }
    }
}

/**
 * Class that's the equivalent of a GroupedLightGet.  This is
 * where the real info is about a group.
 */
data class PHv2GroupedLight(
    /** should be "grouped_light" */
    val type: String,
    val id: String,
    val idV1: String,
    val owner: PHv2LightOwner,
    val on: PHv2LightOn,
    val dimming: PHv2LightDimming,
//    val alert: todo
    val signaling: PHv2LightSignaling
) {
    companion object {
        /**
         * Alternate constructor.
         *
         * @param   jsonObject  Json representation of the actual grouped_light
         *                      (not the parent).
         */
        operator fun invoke(jsonObject: JSONObject) : PHv2GroupedLight {
            val type = if (jsonObject.has(TYPE)) {
                jsonObject.getString(TYPE)
            }
            else { EMPTY_STRING }

            val id = jsonObject.getString(ID)

            val idV1 = if (jsonObject.has(ID_V1)) {
                jsonObject.getString(ID_V1)
            }
            else { EMPTY_STRING }

            val owner = PHv2LightOwner(jsonObject)
            val on = PHv2LightOn(jsonObject)
            val dimming = PHv2LightDimming(jsonObject)
            val signaling = PHv2LightSignaling(jsonObject)

            return PHv2GroupedLight(
                type = type,
                id = id,
                idV1 = idV1,
                owner = owner,
                on = on,
                dimming = dimming,
                signaling = signaling
            )
        }

    }
}
