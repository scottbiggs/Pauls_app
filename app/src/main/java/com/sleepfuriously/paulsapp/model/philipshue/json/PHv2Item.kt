package com.sleepfuriously.paulsapp.model.philipshue.json

import org.json.JSONObject

/**
 * A common data used in the Philips Hue API version 2 is
 * what they often call an Item.  Looks like this.
 *
 * This version is the kind used within a json ARRAY.
 */
data class PHv2ItemInArray(
    /** id reference to whatever this is */
    val rid: String,
    /** lets us know what kind of thing this is */
    val rtype: String
) {
    companion object {
        /**
         * Alternate constructor.
         *
         * @param   jsonObject      Unlike most of these helpers, this take
         *                          THE JSON THAT REPRESENTS THIS CLASS, not
         *                          the parent.  That's because this is often
         *                          found in a json array, not an object.
         *
         * NOTE
         *      This'll throw an exception if the required items are not found!!!
         */
        operator fun invoke(jsonObject: JSONObject) : PHv2ItemInArray {
            val rid = jsonObject.getString(RID)
            val rtype = jsonObject.getString(RTYPE)

            return PHv2ItemInArray(rid, rtype)
        }
    }
}


private const val TAG = "PHv2Item"