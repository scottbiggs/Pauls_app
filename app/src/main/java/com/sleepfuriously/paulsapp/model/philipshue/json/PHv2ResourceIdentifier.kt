package com.sleepfuriously.paulsapp.model.philipshue.json

import org.json.JSONObject

/**
 * Used to reference other resources within a resource.
 */
data class PHv2ResourceIdentifier(
    /** The unique id of the referenced resource */
    val rid: String,
    /** defined the type of the referenced resource */
    val rtype: String
) {
    companion object {

        /**
         * Alternate constructor: takes a json object that represents
         * THIS DATA (not the parent) and turns it into [PHv2ResourceIdentifier].
         */
        operator fun invoke(jsonObject: JSONObject) : PHv2ResourceIdentifier {
            val rid = jsonObject.getString(RID)
            val rtype = jsonObject.getString(RTYPE)
            return PHv2ResourceIdentifier(rid, rtype)
        }
    }
}
