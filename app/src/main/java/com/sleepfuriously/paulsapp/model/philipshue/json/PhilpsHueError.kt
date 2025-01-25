package com.sleepfuriously.paulsapp.model.philipshue.json

/**
 * This is the class that represents an error JSON object as returned
 * by a philps hue api request.
 *
 * It seems that this is pretty universal to all api errors, regardless
 * of the original call.
 *
 * @param   type        Error type number
 * @param   address     An address (not always supplied)
 * @param   description A readable description of the error
 */
@Deprecated("I think this is just api 1")
data class PhilpsHueError(
    val type: Int = -1,
    val address: String = "",
    val description: String = "no description"
)
