package com.sleepfuriously.paulsapp.model.philipshue.json

import org.json.JSONObject

/**
 * Json data received when a server-sent event occurs.
 * Could be just about anything.
 */
data class PHv2ResourceServerSentEvent(
    /** id of the event for this specific group of data, not a device, group, or service */
    val eventId: String,
    /** will be ‘update’, ‘add’, ‘delete’, or ‘error’ */
    val type: String,
    val createtime: String,
    /** list of all the things that changed */
    val data: List<PHv2SseData> = listOf()
) {
    /**
     * Alternate constructor that uses THE ACTUAL json, not the parent!
     */
    companion object {
        operator fun invoke(jsonObject: JSONObject) : PHv2ResourceServerSentEvent {
            val id = jsonObject.getString(ID)
            val type = jsonObject.getString(TYPE)
            val createtime = jsonObject.getString(CREATION_TIME)

            val data = mutableListOf<PHv2SseData>()
            val dataJsonArray = jsonObject.getJSONArray(DATA)
            for (i in 0 until dataJsonArray.length()) {
                val arrayObject = dataJsonArray[i] as JSONObject
                val datum = PHv2SseData(arrayObject)
                data.add(datum)
            }

            return PHv2ResourceServerSentEvent(
                eventId = id,
                type = type,
                createtime = createtime,
                data = data
            )
        }
    }
}

/**
 * All the different things that the server can describe in
 * its broadcast.
 *
 * NOTE
 *  This is probably incomplete (as are the docs!!!).
 */
data class PHv2SseData(
    /** The id of the event. What changed is in [owner]. */
    val eventId: String,
    /** Old version--seems to refer to the [owner]. */
    val idV1: String = "",
    /** the type of thing that we're talking about: device, light, group, etc */
    val type: String,
    /** when not null, this will hold a change in the dimming aspect */
    val dimming: PHv2LightDimming? = null,
    /** when not null, this will hold a change in the on/off aspect */
    val on: PHv2LightOn? = null,
    /** when not null, this will hold reference to the owner (which is probably the device or service) */
    val owner: PHv2ResourceIdentifier? = null
) {
    companion object {
        /**
         * As this is from a json array, this needs THE ACTUAL data,
         * not the parent data to work.
         */
        operator fun invoke(jsonObject: JSONObject) : PHv2SseData {
            // the required components
            val id = jsonObject.getString(ID)
            val idv1 = jsonObject.getString(ID_V1)
            val type = jsonObject.getString(TYPE)

            // optional properties

            val dimming = if (jsonObject.has(DIMMING)) {
                PHv2LightDimming(jsonObject.getJSONObject(DIMMING))
            }
            else { null }

            val on = if (jsonObject.has(ON)) {
                PHv2LightOn(jsonObject.getJSONObject(ON))
            }
            else { null }

            val owner = if (jsonObject.has(OWNER)) {
                PHv2ResourceIdentifier(jsonObject.getJSONObject(OWNER))
            }
            else { null }

            return PHv2SseData(
                eventId = id,
                idV1 = idv1,
                type = type,
                dimming = dimming,
                on = on,
                owner = owner
            )
        }
    }
}
