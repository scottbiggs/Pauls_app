package com.sleepfuriously.paulsapp.model.philipshue.json

/**
 * Used to reference other resources within a resource.
 */
data class PHv2ResourceIdentifier(
    /** The unique id of the referenced resource */
    val rid: String,
    /** defined the type of the referenced resource */
    val rtype: String
)
