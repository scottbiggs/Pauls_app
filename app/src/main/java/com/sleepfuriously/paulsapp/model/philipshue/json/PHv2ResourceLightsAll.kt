package com.sleepfuriously.paulsapp.model.philipshue.json

import android.util.Log
import org.json.JSONObject

/**
 * All the classes used when GETting light data.  The structures are
 * defined in the Philips Hue API v2.
 *
 * The url is:
 *      https://<ip>/clip/v2/resource/light
 *
 *  where <ip> is the ip of the bridge.  As usual you'll also need:
 *      header key = hue-application-key
 *      header body = <token/username>
 */

/**
 * This is data returned by a GET to
 *      https://<ip>/clip/v2/resource/light
 *
 * Should hold all the lights on a bridge.
 *
 * You have a light in all its complexity, including network errors
 * and sub-parts of the light.
 *
 * Note: as always (in v2) the header has the correct token/username
 */
data class PHv2ResourceLightsAll(
    /** will be empty if no error */
    val errors: List<PHv2Error> = listOf(),
    /** Holds all the lights in all their data. Empty if error. */
    val data: List<PHv2Light> = listOf()
) {
    companion object {
        /**
         * Alternate constructor.  Takes the actual json string that's returned
         * from calling
         *      https://<ip>/clip/v2/resource/light
         *
         * @param   jsonObject  The json object that represents this class.
         *                      Unlike the other class's alternate constructors,
         *                      this IS the json, not its parent!!!
         *
         * @return  A [PHv2ResourceLightsAll] instance.  Could be just two empty
         *          lists if the appropriate data can't be found.
         */
        operator fun invoke(jsonObject: JSONObject) : PHv2ResourceLightsAll {
            val errors = mutableListOf<PHv2Error>()
            val data = mutableListOf<PHv2Light>()

            if (jsonObject.has(ERRORS)) {
                val errorsJsonArray = jsonObject.getJSONArray(ERRORS)
                for (i in 0 until errorsJsonArray.length()) {
                    errors.add(PHv2Error(errorsJsonArray.getJSONObject(i)))
                }
            }

            if (jsonObject.has(DATA)) {
                val dataJsonArray = jsonObject.getJSONArray(DATA)
                for (i in 0 until dataJsonArray.length()) {
                    data.add(PHv2Light(dataJsonArray.getJSONObject(i)))
                }
            }
            return PHv2ResourceLightsAll(errors, data)
        }

        /**
         * Another constructor.  This takes a string representation of a
         * json object (NOT the parent).
         */
        operator fun invoke(jsonObjectStr: String) : PHv2ResourceLightsAll {
            return PHv2ResourceLightsAll(JSONObject(jsonObjectStr))
        }
    }
}

/**
 * This is the result from calling
 *      https://<ip>/clip/v2/resource/light/<light_id>
 */
data class PHv2LightIndividual(
    /** Will be empty unless there is an error.  If error, only 1 element. */
    val errors: List<PHv2Error> = listOf(),
    /** afaik, this will have just one element (unless error: then it's empty) */
    val data: List<PHv2Light> = listOf()
) {
    companion object {
        /**
         * Alternate constructor.  Takes the actual json string that's returned
         * from calling
         *      https://<ip>/clip/v2/resource/light/<light_id>
         *
         * @param   jsonObject  The json object that represents this class.
         *                      Unlike the other class's alternate constructors,
         *                      this IS the json, not its parent!!!
         *
         * @return  A [PHv2LightIndividual] instance.  Could be just two empty
         *          lists if the appropriate data can't be found.
         */
        operator fun invoke(jsonObject: JSONObject) : PHv2LightIndividual {
            val errors = mutableListOf<PHv2Error>()
            val data = mutableListOf<PHv2Light>()

            if (jsonObject.has(ERRORS)) {
                val errorsJsonArray = jsonObject.getJSONArray(ERRORS)
                for (i in 0 until errorsJsonArray.length()) {
                    errors.add(PHv2Error(errorsJsonArray.getJSONObject(i)))
                }
            }

            if (jsonObject.has(DATA)) {
                val dataJsonArray = jsonObject.getJSONArray(DATA)
                for (i in 0 until dataJsonArray.length()) {
                    data.add(PHv2Light(dataJsonArray.getJSONObject(i)))
                }
            }
            return PHv2LightIndividual(errors, data)
        }

        /**
         * Another constructor.  This takes a string representation of a
         * json object (NOT the parent).
         */
        operator fun invoke(jsonObjectStr: String) : PHv2LightIndividual {
            return PHv2LightIndividual(JSONObject(jsonObjectStr))
        }

    }
}

/**
 * Each light in [PHv2ResourceLightsAll].  This is the real meat
 * of the data.
 *
 * api ref:
 *      https://developers.meethue.com/develop/hue-api-v2/api-reference/#resource_light_get
 *
 * Required params won't have a default.
 */
data class PHv2Light(
    val type: String = LIGHT,
    val id: String,
    /** id for the old API */
    val idV1: String = EMPTY_STRING,
    val owner: PHv2LightOwner,
    val metadata: PHv2LightMetadata,
    val productData: PHv2LightProductData,
    val serviceId: Int,
    val on: PHv2LightOn,
    val dimming: PHv2LightDimming,
    val colorTemperature: PHv2LightColorTemperature,
    val color: PHv2LightColor,
    val dynamics: PHv2LightDynamics,
    /** this may need its own type, but there's no doc about it */
//    val alert: List<String>,                          todo
    /** various signaling properties */
    val signaling: PHv2LightSignaling,
    /** normal or streaming */
    val mode: String,
//    val gradient: PHv2Gradient = PHv2Gradient(        todo if needed later
//        points = listOf(),
//        mode = EMPTY_STRING,
//        pointsCapable = 1,
//        modeValues = listOf(),
//        pixelCount = 1
//    ),
    val effects: PHv2LightEffects = PHv2LightEffects(listOf(), EMPTY_STRING, listOf()),
//    val timedEffect: PHv2TimedEffects = PHv2TimedEffects(listOf(), "no_effect", listOf()),    todo if needed
    val powerup: PHv2LightPowerup = PHv2LightPowerup(
        EMPTY_STRING, false, PHv2LightPowerupOnMode(
            PHv2LightOn(false)
        )
    ),
) {
    companion object {
        /**
         * Alternate constructor.
         *
         * UNLIKE most alternate constructors, this takes the json that IS the representation
         * of this data class (not the parent, as most of the classes here).  I've done this
         * because the parent is probably a json array, which should be parsed before calling
         * this.
         */
        operator fun invoke(lightDataJsonObject: JSONObject) : PHv2Light {
            val idVer2 = lightDataJsonObject.getString(ID)
            val idVer1 = if (lightDataJsonObject.has(ID_V1)) {
                lightDataJsonObject.getString(ID_V1)
            }
            else { EMPTY_STRING }

            val typeData = if (lightDataJsonObject.has(TYPE)) {
                lightDataJsonObject.getString(TYPE)
            }
            else { LIGHT }

            val own = PHv2LightOwner(lightDataJsonObject)
            val metaD = PHv2LightMetadata(lightDataJsonObject)

            val prodData = PHv2LightProductData(lightDataJsonObject)
            val servId = lightDataJsonObject.getInt(SERVICE_ID)

            val onData = PHv2LightOn(lightDataJsonObject)
            val dim = PHv2LightDimming(lightDataJsonObject)

            val colorTemp = PHv2LightColorTemperature(lightDataJsonObject)
            val col = PHv2LightColor(lightDataJsonObject)

            val dyn = PHv2LightDynamics(lightDataJsonObject)
            val signalingData = PHv2LightSignaling(lightDataJsonObject)

            val modeData = lightDataJsonObject.getString(MODE)
            val eff = PHv2LightEffects(lightDataJsonObject)
            val pow = PHv2LightPowerup(lightDataJsonObject)

            return PHv2Light(
                id = idVer2,
                idV1 = idVer1,
                type = typeData,
                owner = own,
                metadata = metaD,
                productData = prodData,
                serviceId = servId,
                on = onData,
                dimming = dim,
                colorTemperature = colorTemp,
                color = col,
                dynamics = dyn,
                signaling = signalingData,
                mode = modeData,
                effects = eff,
                powerup = pow
            )
        }

        /**
         * Another alternate constructor.  This takes a JSON string that
         * represents the json data (NOT the parent data!).
         */
        operator fun invoke(jsonObjectString : String) : PHv2Light {
            val jsonObject = JSONObject(jsonObjectString)
            return PHv2Light(jsonObject)
        }
    }
}

/**
 * Describes the owner of the service.
 */
data class PHv2LightOwner(
    /** unique id of the owner */
    val rid: String,
    /** descirbes the type of device of the owner */
    val rtype: String
) {
    companion object {
        /**
         * Alternate constructor.  This takes a Json object that should include
         * an object WITHIN it that responds to "owner".
         *
         * Generates an empty class if data can't be found.
         */
        operator fun invoke(parentJsonObject: JSONObject) : PHv2LightOwner {
            if (parentJsonObject.has(OWNER)) {
                val ownerJSONObject = parentJsonObject.getJSONObject(OWNER)

                val rid = ownerJSONObject.get(RID) as String
                val rtype = ownerJSONObject.get(RTYPE) as String

                return PHv2LightOwner(
                    // these are required according to docs--no need to check for existence
                    rid = rid,
                    rtype = rtype
                )
            }
            Log.e(TAG, "Unable to find 'owner' in PHv2Owner secondary constructor!")
            return PHv2LightOwner(EMPTY_STRING, EMPTY_STRING)
        }
    }
}

/**
 * Lots of good stuff about the device is stored here!
 */
data class PHv2LightMetadata(
    val name: String,
    /** see docs for list */
    val archetype: String,
    /** mired value of the white lamp (???) default is max */
    val fixedMired: Int = 500,
    /** function of this light: functional, decorative, mixed, or unknown */
    val function: String
) {
    companion object {
        /**
         * Alternate constructor using JSON object.
         *
         * @param   parentJsonObject    Object that CONTAINS a json object that
         *                              corresponds to this class.
         */
        operator fun invoke(parentJsonObject: JSONObject) : PHv2LightMetadata {

            if (parentJsonObject.has(METADATA)) {
                val metadataJsonObject = parentJsonObject.getJSONObject(METADATA)

                // no checking for these: required by docs
                val name = metadataJsonObject.get(NAME) as String
                val archetype = metadataJsonObject.get(ARCHETYPE) as String
                val function = metadataJsonObject.get(FUNCTION) as String

                // optional param
                val fixedMired = if (metadataJsonObject.has(FIXED_MIRED)) {
                    metadataJsonObject.get(FIXED_MIRED) as Int
                }
                else { 500 }

                return PHv2LightMetadata(
                    name = name,
                    archetype = archetype,
                    fixedMired = fixedMired,
                    function = function
                )
            }
            else {
                // return empty object
                return PHv2LightMetadata(
                    name = EMPTY_STRING,
                    archetype = EMPTY_STRING,
                    function = EMPTY_STRING
                )
            }
        }
    }
}

/**
 * factory defaults of product data (???)
 */
data class PHv2LightProductData(
    /** only available for multiple lightservices */
    val name: String = EMPTY_STRING,
    /** see docs for list */
    val archetype: String = EMPTY_STRING,
    /** same as [PHv2LightMetadata.function] */
    val function: String
) {
    companion object {
        /**
         * Alternate constructor that uses a json object that CONTAINS (or should
         * contain) a json object that corresponds to PHv2ProductionData.
         */
        operator fun invoke(parentJsonObject: JSONObject) : PHv2LightProductData {
            if (parentJsonObject.has(PRODUCT_DATA)) {
                val productDataJsonObject = parentJsonObject.get(PRODUCT_DATA) as JSONObject

                val name = if (productDataJsonObject.has(NAME)) {
                    productDataJsonObject.get(NAME) as String
                }
                else { EMPTY_STRING }

                val archetype = if (productDataJsonObject.has(ARCHETYPE)) {
                    productDataJsonObject.get(ARCHETYPE) as String
                }
                else { EMPTY_STRING }

                val function = productDataJsonObject.get(FUNCTION) as String

                return PHv2LightProductData(
                    name = name,
                    archetype = archetype,
                    function = function
                )
            }
            else {
                // doesn't have it! return empty version
                return PHv2LightProductData(function = EMPTY_STRING)
            }
        }
    }
}

/**
 * Very simply class that just tells if something is on or not.
 * Made more complicated by the duplicate use of "on"
 */
data class PHv2LightOn(
    val on: Boolean
) {
    companion object {
        /**
         * Alternate constructor: takes a json object that CONTAINS
         * a json object that is the equivalent to this class.
         *
         * If the param doesn't have this, then an empty PHv2On is
         * created (will be off).
         */
        operator fun invoke(parentJsonObject: JSONObject) : PHv2LightOn {
            if (parentJsonObject.has (ON)) {
                val onJSONObject = parentJsonObject.getJSONObject(ON)
                val onData = onJSONObject.getBoolean(ON)
                return PHv2LightOn(onData)
            }
            else {
                return PHv2LightOn(false)
            }
        }
    }
}

data class PHv2LightDimming(
    /** value [1..100] */
    val brightness: Int,
    /** same range as above */
    val minDimLevel: Int = 1
) {
    companion object {
        /**
         * Alternate constructor: takes a json object that CONTAINS
         * a json object that is the equivalent of this class.
         *
         * If the param doesn't have this, then an empty PHv2Dimming is
         * created.
         */
        operator fun invoke (parentJsonObject: JSONObject) : PHv2LightDimming {
            if (parentJsonObject.has(DIMMING)) {
                val phv2DimmingJSONObject = parentJsonObject.getJSONObject(DIMMING)

                val brightness = phv2DimmingJSONObject.getInt(BRIGHTNESS)
                val minDimLevel = if (phv2DimmingJSONObject.has(MIN_DIM_LEVEL)) {
                    phv2DimmingJSONObject.getInt(MIN_DIM_LEVEL)
                } else { 1 }

                return PHv2LightDimming(
                    brightness = brightness,
                    minDimLevel = minDimLevel
                )
            }
            else {
                return PHv2LightDimming(100)
            }
        }
    }
}

data class PHv2LightColorTemperature(
    /** range of [153 .. 500] or null when not in spectrum */
    val mirek: Int?,
    /** tells if [mirek] is valid */
    val mirekValid: Boolean,
    val mirekSchema: PHv2MirekSchema
) {
    companion object {
        /**
         * Alternate constructor.  Takes a json object that contains
         * a json object that represents a [PHv2LightColorTemperature].
         *
         * If not found, returns empty instance.
         */
        operator fun invoke(parentJsonObject: JSONObject) : PHv2LightColorTemperature {
            if (parentJsonObject.has(COLOR_TEMPERATURE)) {
                val phv2colorTempJsonObject = parentJsonObject.getJSONObject(COLOR_TEMPERATURE)


                val mirek = phv2colorTempJsonObject.optInt(MIREK)
                val mirekValid = phv2colorTempJsonObject.optBoolean(MIREK_VALID)
                val mirekSchema = PHv2MirekSchema(phv2colorTempJsonObject)

                return PHv2LightColorTemperature(
                    mirek = mirek,
                    mirekValid = mirekValid,
                    mirekSchema = mirekSchema
                )
            }
            else {
                return PHv2LightColorTemperature(null, false, PHv2MirekSchema(parentJsonObject))
            }
        }
    }
}

data class PHv2MirekSchema(
    /** [153..500] */
    val mirekMinimum: Int,
    /** [153..500] */
    val mirekMaximum: Int
) {
    companion object {
        /**
         * alternate constructor: Takes a json object that INCLUDES a json
         * object that is equivalent to a [PHv2MirekSchema].  If not found
         * returns an empty instance.
         */
        operator fun invoke(parentJsonObject: JSONObject) : PHv2MirekSchema {
            if (parentJsonObject.has(MIREK_SCHEMA)) {
                val mirekSchemaJSONObject = parentJsonObject.getJSONObject(MIREK_SCHEMA)

                return PHv2MirekSchema(
                    mirekMinimum = mirekSchemaJSONObject.getInt(MIREK_MINIMUM),
                    mirekMaximum = mirekSchemaJSONObject.getInt(MIREK_MAXIMUM),
                )
            }
            else {
                return PHv2MirekSchema(500, 500)
            }
        }
    }
}

data class PHv2LightColor(
    /** CIE XY gamut position.  ranges [0..1] */
    val xy: PHv2LightColorXY,
    val gamut: PHv2LightColorGamut = PHv2LightColorGamut(Pair(1.0, 1.0), Pair(1.0, 1.0), Pair(1.0, 1.0)),
    /** A, B, C or other. Used by older devices. */
    val gamutType: String
) {
    companion object {
        /**
         * Alternate constructor.
         *
         * @param   parentJsonObject  A json object that CONTAINS a json object that is
         *                      the equivalent of this class.
         *
         * @return  [PHv2LightColor] instance.  If not found, a basic instance is created.
         */
        operator fun invoke(parentJsonObject: JSONObject) : PHv2LightColor {
            if (parentJsonObject.has(COLOR)) {
                val colorJSONObject = parentJsonObject.getJSONObject(COLOR)

                val xyJSONObject = colorJSONObject.getJSONObject(XY)
                val x = xyJSONObject.getDouble(X).toFloat()
                val y = xyJSONObject.getDouble(Y).toFloat()

                val gamut = PHv2LightColorGamut(colorJSONObject)
                val gamutType = colorJSONObject.getString(GAMUT_TYPE)

                return PHv2LightColor(PHv2LightColorXY(x, y), gamut, gamutType)
            }
            else {
                return PHv2LightColor(PHv2LightColorXY(1f, 1f), PHv2LightColorGamut(parentJsonObject), OTHER)
            }
        }
    }
}

/**
 * CIE XY gamut position
 */
data class PHv2LightColorXY(
    /** range [0..1] */
    val x: Float,
    /** range [0..1] */
    val y: Float
)

data class PHv2LightColorGamut(
    val red: Pair<Double, Double>,
    val green: Pair<Double, Double>,
    val blue: Pair<Double, Double>,
) {
    companion object {
        /**
         * Alternate constructor
         *
         * @param   parentJsonObject  A json object that CONTAINS a json object that is
         *                      the equivalent of this class.
         *
         * @return  [PHv2LightColorGamut] instance.  If not found, a basic instance is created.
         */
        operator fun invoke(parentJsonObject: JSONObject) : PHv2LightColorGamut {
            if (parentJsonObject.has(GAMUT)) {
                val gamutJSONObject = parentJsonObject.getJSONObject(GAMUT)

                val redJSONObject = gamutJSONObject.getJSONObject(RED)
                val redX = redJSONObject.getDouble(X)
                val redY = redJSONObject.getDouble(Y)
                val redPair = Pair(redX, redY)

                val greenJSONObject = gamutJSONObject.getJSONObject(GREEN)
                val greenX = greenJSONObject.getDouble(X)
                val greenY = greenJSONObject.getDouble(Y)
                val greenPair = Pair(greenX, greenY)

                val blueJSONObject = gamutJSONObject.getJSONObject(BLUE)
                val blueX = blueJSONObject.getDouble(X)
                val blueY = blueJSONObject.getDouble(Y)
                val bluePair = Pair(blueX, blueY)

                return PHv2LightColorGamut(redPair, greenPair, bluePair)
            }
            else {
                return PHv2LightColorGamut(Pair(1.0, 1.0),Pair(1.0, 1.0),Pair(1.0, 1.0))
            }
        }
    }
}

data class PHv2LightDynamics(
    /** dynamic_palette or none */
    val status: String,
    /** todo */
//    val statusValues: List<PHv2SupportedDynamicStatus>,
    /** range [0..1] */
    val speed: Double,
    /** indicates if [speed] is valid */
    val speedValid: Boolean
) {
    companion object {
        /**
         * Alternate constructor
         *
         * @param   parentJsonObject  A json object that CONTAINS a json object that is
         *                      the equivalent of this class.
         *
         * @return  [PHv2LightDynamics] instance.  If not found, a basic instance is created.
         */
        operator fun invoke(parentJsonObject: JSONObject) : PHv2LightDynamics {
            if (parentJsonObject.has(DYNAMICS)) {
                val dynamicsJSONObject = parentJsonObject.getJSONObject(DYNAMICS)

                val status = dynamicsJSONObject.getString(STATUS)
//                val statusValues = mutableListOf<PHv2SupportedDynamicStatus>()  todo (too complicated to bother with now)

                val speed = dynamicsJSONObject.getDouble(SPEED)
                val speedValid = dynamicsJSONObject.getBoolean(SPEED_VALID)

                return PHv2LightDynamics(status, speed, speedValid)
            }
            else {
                return PHv2LightDynamics(NONE, 1.0, false)
            }
        }
    }
}

data class PHv2SupportedDynamicStatus(
    val dynamicPalette: String,
    val supportedDynamicStatus: String
)

/** signaling properties */
data class PHv2LightSignaling(
    /** all the signal that this light supports */
    val signalValues: List<String>,
    val status: PHv2LightStatus,
) {
    companion object {
        /**
         * Alternate constructor
         *
         * @param   parentJsonObject  A json object that CONTAINS a json object that is
         *                      the equivalent of this class.
         *
         * @return  [PHv2LightSignaling] instance.  If not found, a basic instance is created.
         */
        operator fun invoke(parentJsonObject: JSONObject) : PHv2LightSignaling {
            if (parentJsonObject.has(SIGNALING)) {
                val signalingJSONObject = parentJsonObject.getJSONObject(SIGNALING)

                val signalValuesJSONArray = signalingJSONObject.getJSONArray(SIGNAL_VALUES)
                val signalValues = mutableListOf<String>()
                for (i in 0 until signalValuesJSONArray.length()) {
                    signalValues.add(signalValuesJSONArray[i] as String)
                }

                val status = PHv2LightStatus(signalingJSONObject)

                return PHv2LightSignaling(
                    signalValues = signalValues,
                    status = status
                )
            }
            else {
                return PHv2LightSignaling(listOf(), PHv2LightStatus(parentJsonObject))
            }
        }
    }
}

/** indicates status of active signal. not available when inactive */
data class PHv2LightStatus(
    /** no_signal, on_off, on_off_color, alternating */
    val signal: String,
    /** datetime */
    val estimatedEnd: String,
    /** Colors provided for active effect, all in CIE XY gamut position. */
//    val colors: List<Pair<Double, Double>>   todo
) {
    companion object {
        /**
         * Alternate constructor
         *
         * @param   parentJsonObject  A json object that CONTAINS a json object that is
         *                      the equivalent of this class.
         *
         * @return  [PHv2LightStatus] instance.  If not found, a basic instance is created.
         */
        operator fun invoke(parentJsonObject: JSONObject) : PHv2LightStatus {
            if (parentJsonObject.has(STATUS)) {
                val statusJSONObject = parentJsonObject.getJSONObject(STATUS)

                val signal = statusJSONObject.getString(SIGNAL)
                val estimatedEnd = statusJSONObject.getString(ESTIMATED_END)

//                val colorsJSONArray = statusJSONObject.getJSONArray("colors")     todo
//                for (i in 0 until colorsJSONArray.length()) {
//                    val
//                }

                return PHv2LightStatus(signal, estimatedEnd)
            }
            else {
                return PHv2LightStatus(NO_SIGNAL, EMPTY_STRING)
            }
        }
    }
}

/*   todo
/**
 * Basic feature containing gradient properties.
 */
data class PHv2Gradient(
    /** the x y values. in range [0..1] */
    val points: List<Pair<Double, Double>>,
    /** interpolated_palette, interpolated_palette_mirrored, or random_pixelated */
    val mode: String,
    /** number of color points that this light can show with gradient */
    val pointsCapable: Int,
    /** modes a gradient device can deploy */
    val modeValues: List<String>,
    /** pixels in the device */
    val pixelCount: Int
) {
    companion object {
        /**
         * Alternate constructor
         *
         * @param   jsonObject  A json object that CONTAINS a json object that is
         *                      the equivalent of this class.
         *
         * @return  [PHv2Gradient] instance.  If not found, a basic instance is created.
         */
        operator fun invoke(jsonObject: JSONObject) : PHv2Gradient {
            if (jsonObject.has("gradient")) {
                val gradientJSONObject = jsonObject.getJSONObject("gradient")
                val pointsJSONArray = gradientJSONObject.getJSONArray("points")

                val pointsList = mutableListOf<Pair<Double, Double>>()
                for (i in 0 until pointsJSONArray.length()) {
                    val point = sigh, this is getting complicated and probably not needed!
                }
            }
        }
    }
}
*/

data class PHv2LightEffects(
    /** Possible status values in which a light could be when playing an effect */
    val statusValues: List<String>,
    /** Current status values the light is in regarding effects */
    val status: String,
    /** Possible effect values you can set in a light */
    val effectValues: List<String>
) {
    companion object {
        /**
         * Alternate constructor
         *
         * @param   parentJsonObject    A json object that CONTAINS a json object that is
         *                              the equivalent of this class.
         *
         * @return  [PHv2LightEffects] instance.  If not found, a basic instance is created.
         */
        operator fun invoke(parentJsonObject: JSONObject) : PHv2LightEffects {
            if (parentJsonObject.has(EFFECTS)) {
                val effectsJSONObject = parentJsonObject.getJSONObject(EFFECTS)

                val statusValuesJSONArray = effectsJSONObject.getJSONArray(STATUS_VALUES)
                val statusValues = mutableListOf<String>()
                for (i in 0 until statusValuesJSONArray.length()) {
                    statusValues.add(statusValuesJSONArray[i] as String)
                }

                val status = effectsJSONObject.getString(STATUS)

                val effectValuesJSONArray = effectsJSONObject.getJSONArray(EFFECT_VALUES)
                val effectValues = mutableListOf<String>()
                for (i in 0 until effectValuesJSONArray.length()) {
                    effectValues.add(effectValuesJSONArray[i] as String)
                }

                return PHv2LightEffects(
                    statusValues = statusValues,
                    status = status,
                    effectValues = effectValues
                )
            }
            else {
                return PHv2LightEffects(listOf<String>(), EMPTY_STRING, listOf<String>())
            }
        }
    }
}

/*
todo
data class PHv2TimedEffects(
    val statusValues: List<String>,
    val status: String,
    val effectValues: List<String>
) {
    companion object {
        /**
         * Alternate constructor
         *
         * @param   jsonObject  A json object that CONTAINS a json object that is
         *                      the equivalent of this class.
         *
         * @return  [PHv2TimedEffects] instance.  If not found, a basic instance is created.
         */
        operator fun invoke(jsonObject: JSONObject) : PHv2TimedEffects {
            if (jsonObject.has("timed_effects")) {
                val timedEffectsJSONObject = jsonObject.getJSONObject("timed_effects")

                val statusValuesJSONArray = timedEffectsJSONObject.getJSONArray("status_values")
                val statusValuesList = mutableListOf<String>()
                for (i in 0 until statusValuesJSONArray.length()) {
                    val statVal = statusValuesJSONArray[i].toString()
                    statusValuesList.add(statVal)
                }


            }
        }
    }
}
*/

data class PHv2LightPowerup(
    /** one of safety, powerfail, last_on_state, or custom */
    val preset: String,
    /** true if this light has been configured */
    val configured: Boolean,
    val on: PHv2LightPowerupOnMode,
    val dimming: PHv2LightPowerupDimming = PHv2LightPowerupDimming(JSONObject()),
    val color: PHv2LightPowerupColor = PHv2LightPowerupColor(JSONObject())
) {
    companion object {
        /**
         * Alternate constructor
         *
         * @param   parentJsonObject  A json object that CONTAINS a json object that is
         *                      the equivalent of this class.
         *
         * @return  [PHv2LightPowerup] instance.  If not found, a basic instance is created.
         */
        operator fun invoke(parentJsonObject: JSONObject) : PHv2LightPowerup {
            if (parentJsonObject.has(POWERUP)) {
                val powerUpJSONObject = parentJsonObject.getJSONObject(POWERUP)

                val presetStr = powerUpJSONObject.getString(PRESET)
                val configuredBool = powerUpJSONObject.getBoolean(CONFIGURED)
                val onData = PHv2LightPowerupOnMode(powerUpJSONObject)
                val dimmingData = PHv2LightPowerupDimming(powerUpJSONObject)
                val colorData = PHv2LightPowerupColor(powerUpJSONObject)

                return PHv2LightPowerup(
                    preset = presetStr,
                    configured = configuredBool,
                    on = onData,
                    dimming = dimmingData,
                    color = colorData
                )
            }
            else {
                return PHv2LightPowerup(
                    preset = SAFETY,
                    configured = false,
                    on = PHv2LightPowerupOnMode(JSONObject()),      // send in an empty object
                    dimming = PHv2LightPowerupDimming(JSONObject()),
                    color = PHv2LightPowerupColor(JSONObject())
                )
            }
        }
    }
}

/**
 * This is an On object with mode.  [PHv2LightOn] is a property of
 * this object.
 */
data class PHv2LightPowerupOnMode(
    val on: PHv2LightOn,
    /** on, toggle, previous */
    val mode: String = EMPTY_STRING
) {
    companion object {
        /**
         * Alternate constructor: takes a json object that CONTAINS
         * a json object that is the equivalent of this class.
         *
         * If the param doesn't have this, then an empty PHv2Dimming is
         * created.
         */
        operator fun invoke(parentJsonObject: JSONObject): PHv2LightPowerupOnMode {
            if (parentJsonObject.has(ON)) {
                val onModeJSONObject = parentJsonObject.getJSONObject(ON)
                val onBool = PHv2LightOn(onModeJSONObject)
                val modeBool = if (onModeJSONObject.has(MODE)) {
                    onModeJSONObject.getString(MODE)
                }
                else { EMPTY_STRING }
                return PHv2LightPowerupOnMode(onBool, modeBool)
            } else {
                return PHv2LightPowerupOnMode(PHv2LightOn(false), EMPTY_STRING)
            }
        }
    }
}


data class PHv2LightPowerupDimming(
    val mode: String,
    val dimming: PHv2LightPowerupDimmingDimming = PHv2LightPowerupDimmingDimming(JSONObject())
) {
    companion object {
        /**
         * Alternate constructor
         *
         * @param   parentJsonObject  A json object that CONTAINS a json object that is
         *                      the equivalent of this class.
         *
         * @return  [PHv2LightPowerupDimming] instance.  If not found, a basic instance is created.
         */
        operator fun invoke(parentJsonObject: JSONObject) : PHv2LightPowerupDimming {
            if (parentJsonObject.has(DIMMING)) {
                val dimmingJSONObject = parentJsonObject.getJSONObject(DIMMING)

                val mode = dimmingJSONObject.getString(MODE)
                val dimming = PHv2LightPowerupDimmingDimming(dimmingJSONObject)

                return PHv2LightPowerupDimming(mode, dimming)
            }
            else {
                return PHv2LightPowerupDimming(EMPTY_STRING, PHv2LightPowerupDimmingDimming(JSONObject()))
            }
        }
    }
}

data class PHv2LightPowerupDimmingDimming(
    /** range [0..100], see docs for details */
    val brightness: Int
) {
    companion object {
        operator fun invoke(parentJsonObject: JSONObject) : PHv2LightPowerupDimmingDimming {
            if (parentJsonObject.has(DIMMING)) {
                val dimmingDimmingJSONObject = parentJsonObject.getJSONObject(DIMMING)
                val brightness = dimmingDimmingJSONObject.getInt(BRIGHTNESS)
                return PHv2LightPowerupDimmingDimming(brightness)
            }
            else {
                return PHv2LightPowerupDimmingDimming(100)
            }
        }
    }
}


data class PHv2LightPowerupColor(
    val mode: String,
    val colorTemp: PHv2LightPowerupColorTemperature = PHv2LightPowerupColorTemperature(JSONObject()),
    val color: PHv2LightPowerupColorColor = PHv2LightPowerupColorColor(JSONObject())
) {
    companion object {
        operator fun invoke(parentJsonObject: JSONObject) : PHv2LightPowerupColor {
            if (parentJsonObject.has(COLOR)) {
                val colorJSONObject = parentJsonObject.getJSONObject(COLOR)
                val mode = colorJSONObject.getString(MODE)
                val colorTemperature = PHv2LightPowerupColorTemperature(colorJSONObject)
                val color = PHv2LightPowerupColorColor(colorJSONObject)

                return PHv2LightPowerupColor(
                    mode = mode,
                    colorTemp = colorTemperature,
                    color = color
                )
            }
            else {
                return PHv2LightPowerupColor(
                    mode = EMPTY_STRING,
                    colorTemp = PHv2LightPowerupColorTemperature(JSONObject()),
                    color = PHv2LightPowerupColorColor(JSONObject())
                )
            }
        }
    }
}

/**
 * From docs:
 *      color temperature in mirek or null when the light color is not in the ct spectrum
 */
data class PHv2LightPowerupColorTemperature(
    /** range [153 .. 500] or null */
    val mirek: Int?
) {
    companion object {
        operator fun invoke(parentJsonObject: JSONObject) : PHv2LightPowerupColorTemperature {
            if (parentJsonObject.has(COLOR_TEMPERATURE)) {
                val mirekJSONObject = parentJsonObject.getJSONObject(COLOR_TEMPERATURE)
                val mirek = mirekJSONObject.getInt(MIREK)
                return PHv2LightPowerupColorTemperature(mirek)
            }
            else {
                return PHv2LightPowerupColorTemperature(null)
            }
        }
    }
}

data class PHv2LightPowerupColorColor(
    /** CIE XY gamut position */
    val xy: Pair<Double, Double>
) {
    companion object {
        /**
         * Alternate constructor
         *
         * @param   parentJsonObject  A json object that CONTAINS a json object that is
         *                      the equivalent to this class.
         *
         * @return  [PHv2LightPowerupColorColor] instance.  If not found, a basic instance is created.
         */
        operator fun invoke(parentJsonObject: JSONObject) : PHv2LightPowerupColorColor {
            if (parentJsonObject.has(COLOR)) {
                val colorColorJSONObject = parentJsonObject.getJSONObject(COLOR)
                val xyJSONObject = colorColorJSONObject.getJSONObject(XY)
                val x = xyJSONObject.getDouble(X)
                val y = xyJSONObject.getDouble(Y)

                return PHv2LightPowerupColorColor(Pair(x, y))
            }
            else {
                return PHv2LightPowerupColorColor(Pair(1.0, 1.0))
            }
        }
    }
}

//---------------------
//  constants
//---------------------

private const val TAG = "PHv2ResourceLight"


