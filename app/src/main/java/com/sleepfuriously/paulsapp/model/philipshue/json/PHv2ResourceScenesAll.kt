package com.sleepfuriously.paulsapp.model.philipshue.json

import org.json.JSONObject

/**
 * Represents the json returned from a bridge GET about its scenes.
 *
 *      https://<bridge_ip>/clip/v2/resource/scene
 */
data class PHv2ResourceScenesAll(
    val errors: List<PHv2Error> = listOf(),
    val data: List<PHv2Scene> = listOf()
) {
    companion object {
        /**
         * Alternate constructor: this takes a json object representing
         * this class and converts it to a [PHv2ResourceScenesAll].
         */
        operator fun invoke(jsonObj: JSONObject) : PHv2ResourceScenesAll {
            // first the errors
            val errors = mutableListOf<PHv2Error>()
            val errorsJsonArray = jsonObj.optJSONArray(ERRORS)
            if (errorsJsonArray != null) {
                for (i in 0 until errorsJsonArray.length()) {
                    errors.add(PHv2Error(errorsJsonArray[i] as JSONObject))
                }
            }

            // similar for the data
            val data = mutableListOf<PHv2Scene>()
            val dataJsonArray = jsonObj.optJSONArray(DATA)
            if (dataJsonArray != null) {
                for (i in 0 until dataJsonArray.length()) {
                    data.add(PHv2Scene(dataJsonArray[i] as JSONObject))
                }
            }
            return PHv2ResourceScenesAll(errors, data)
        }

        /**
         * Another alternate constructor.  Takes a string representing the
         * json for this data.
         */
        operator fun invoke(jsonStr: String) : PHv2ResourceScenesAll {
            return PHv2ResourceScenesAll(JSONObject(jsonStr))
        }
    }
}

/**
 * Data returned from a bridge GET inquiry:
 *      https://<bridge_ip>/clip/v2/resource/scene/<scene_id>
 *
 * Note:
 *  This is EXACTLY like [PHv2ResourceScenesAll], except that the
 *  [data] list has just 1 item (the one that matches the id).
 */
data class PHv2ResourceSceneIndividual(
    val errors: List<PHv2Error> = listOf(),
    val data: List<PHv2Scene> = listOf()
) {
    companion object {
        /**
         * Alternate constructor: takes json object representing this data.
         */
        operator fun invoke(jsonObj: JSONObject) : PHv2ResourceSceneIndividual {
            val errors = mutableListOf<PHv2Error>()
            val errorsJsonArray = jsonObj.optJSONArray(ERRORS)
            if (errorsJsonArray != null) {
                for (i in 0 until errorsJsonArray.length()) {
                    errors.add(PHv2Error(errorsJsonArray[i] as JSONObject))
                }
            }

            // just one data is possible
            val data = mutableListOf<PHv2Scene>()
            val dataJsonArray = jsonObj.optJSONArray(DATA)
            if ((dataJsonArray != null) && (dataJsonArray.length() > 0)) {
                data.add(PHv2Scene(dataJsonArray[0] as JSONObject))
            }
            return PHv2ResourceSceneIndividual(errors, data)
        }
    }
}

/**
 * The meat--info for a scene is described here.
 */
data class PHv2Scene(
    val id: String,
    val idV1: String = "",
    /** optional, but should be there and it should be "scene" */
    val type: String = "scene",
    /** "List of actions to be executed synchronously on recall" */
    val actions: List<PHv2SceneAction>,
    /** can be null if no palettes are present */
    val palette: PHv2ScenePalette?,
    /** there is no documentation on this, except that it's required */
    val recall: Any? = null,
    val metadata: PHv2SceneMetadata,    // different from other metadata
    /**
     * Groups associated with this scene.  All services in the group are
     * part of this scene. If the group is changed the scene is updated.
     * (e.g. light added/removed)
     *
     * NOTE: this is a reference to the room (and presumably zone) that the
     * scene is part of.
     */
    val group: PHv2ItemInArray,
    /** range [0..1] */
    val speed: Float,
    /** Indicates whether to automatically start the scene dynamically on active recall */
    val autoDynamic: Boolean,
    val status: PHv2SceneStatus
) {
    /**
     * Returns the id of the room or zone (some sort of group) that this
     * scene is part of. Same as [getZone].
     */
    fun getRoom() : String {
        return group.rid
    }

    /** same as [getRoom] */
    fun getZone() : String {
        return group.rid
    }


    override fun equals(other: Any?): Boolean {
        val otherScene = other as PHv2Scene
        if (id != otherScene.id) return false
        if (idV1 != otherScene.idV1) return false
        if (type != otherScene.type) return false
        if (actions.size != otherScene.actions.size) return false
        actions.forEachIndexed { i, action ->
            if (action != otherScene.actions[i]) return false
        }
        if (palette != otherScene.palette) return false
        if (metadata != otherScene.metadata) return false
        if (group != otherScene.group) return false
        if (speed != otherScene.speed) return false
        if (autoDynamic != otherScene.autoDynamic) return false
        if (status != otherScene.status) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + idV1.hashCode()
        result = 31 * result + actions.hashCode()
        result = 31 * result + palette.hashCode()
        result = 31 * result + metadata.hashCode()
        result = 31 * result + group.hashCode()
        result = 31 * result + speed.hashCode()
        result = 31 * result + autoDynamic.hashCode()
        result = 31 * result + status.hashCode()
        return result
    }

    companion object {
        /**
         * Alternate constructor: takes a json object representing a PHv2Scene.
         */
        operator fun invoke(jsonObj: JSONObject) : PHv2Scene {

            val actions = mutableListOf<PHv2SceneAction>()
            val actionsJsonArray = jsonObj.getJSONArray(ACTIONS)
            for (i in 0 until actionsJsonArray.length()) {
                actions.add(PHv2SceneAction(actionsJsonArray[i] as JSONObject))
            }

            val palette = if (jsonObj.has(PALETTE)) {
                PHv2ScenePalette(jsonObj.getJSONObject(PALETTE))
            }
            else {
                null
            }

            return PHv2Scene(
                id = jsonObj.getString(ID),
                idV1 = jsonObj.optString(ID_V1, ""),
                type = jsonObj.optString(TYPE, ""),
                actions = actions,
                palette = palette,
                metadata = PHv2SceneMetadata(jsonObj.getJSONObject(METADATA)),
                group = PHv2ItemInArray(jsonObj.getJSONObject(GROUP)),
                speed = jsonObj.getDouble(SPEED).toFloat(),
                autoDynamic = jsonObj.getBoolean(AUTO_DYNAMIC),
                status = PHv2SceneStatus(jsonObj.getJSONObject(STATUS)),
            )
        }
    }
}

/**
 * Consists the information about the current status and last time it is recalled (used)
 */
data class PHv2SceneStatus(
    /** inactive, static, dynamic_palette */
    val active: String? = null,
    /** data / time */
    val lastRecall: String? = null
) {
    companion object {
        operator fun invoke(jsonObj: JSONObject) : PHv2SceneStatus {
            return PHv2SceneStatus(
                active = jsonObj.optString(ACTIVE),
                lastRecall = jsonObj.optString(LAST_RECALL)
            )
        }
    }
}

/**
 * Slightly different from other metadata
 */
data class PHv2SceneMetadata(
    /** human readable name--most important part of this data class */
    val name: String,
    /**
     * docs:  "Reference with unique identifier for the image representing the scene only
     * accepting “rtype”: “public_image” on creation"
     */
    val image: PHv2ItemInArray? = null,
    /** Application specific data. Free format string. Length [1..16] chars */
    val appdata: String? = null
) {
    companion object {
        operator fun invoke(jsonObj: JSONObject) : PHv2SceneMetadata {
            val imageReference = if (jsonObj.has(IMAGE)) {
                PHv2ItemInArray(jsonObj.getJSONObject(IMAGE))
            }
            else { null }

            return PHv2SceneMetadata(
                name = jsonObj.getString(NAME),
                image = imageReference,
                appdata = jsonObj.optString(APP_DATA)
            )
        }
    }
}

/**
 * Palette of colors used when playing dynamics.  Each array should have
 * the same number of items (which is range [0..9]).
 */
data class PHv2ScenePalette(
    val color: List<PHv2SceneColor>,
    val dimming: List<PHv2SceneDimming>,
    val colorTemperature: List<PHv2SceneColorTemperature>,
//    @Deprecated("use effectsV2")
//    val effects: List<Any>?,
    val effectsV2: List<PHv2ScenePaletteEffects2>
) {
    companion object {
        operator fun invoke(jsonObj: JSONObject) : PHv2ScenePalette {
            val colors = mutableListOf<PHv2SceneColor>()
            val colorJsonArray = jsonObj.getJSONArray(COLOR)
            for (i in 0 until colorJsonArray.length()) {
                colors.add(PHv2SceneColor(colorJsonArray.getJSONObject(i)))
            }

            val dimmingList = mutableListOf<PHv2SceneDimming>()
            val dimmingJsonArray = jsonObj.getJSONArray(DIMMING)
            for (i in 0 until dimmingJsonArray.length()) {
                dimmingList.add(PHv2SceneDimming(dimmingJsonArray.getJSONObject(i)))
            }


            val colorTemps = mutableListOf<PHv2SceneColorTemperature>()
            val colorTempJsonArray = jsonObj.getJSONArray(COLOR_TEMPERATURE)
            for (i in 0 until colorTempJsonArray.length()) {
                colorTemps.add(PHv2SceneColorTemperature(colorTempJsonArray.getJSONObject(i)))
            }

            val effectsV2 = mutableListOf<PHv2ScenePaletteEffects2>()
            val effectsV2JsonArray = jsonObj.getJSONArray(EFFECTS_V2)
            for (i in 0 until effectsV2JsonArray.length()) {
                effectsV2.add(PHv2ScenePaletteEffects2(effectsV2JsonArray.getJSONObject(i)))
            }

            return PHv2ScenePalette(
                color = colors,
                dimming = dimmingList,
                colorTemperature = colorTemps,
                effectsV2 = effectsV2
            )
        }
    }
}

data class PHv2SceneColor(
    val color: PHv2LightColorXY,
    val dimming: PHv2SceneDimming
) {
    companion object {
        operator fun invoke(jsonObj: JSONObject) : PHv2SceneColor {
            return PHv2SceneColor(
                color = PHv2LightColorXY(jsonObj.getJSONObject(COLOR)),
                dimming = PHv2SceneDimming(jsonObj.getJSONObject(DIMMING))
            )
        }
    }
}

data class PHv2SceneDimming(
    /** percentage brightness. range = [0..100] */
    val brightness: Int
) {
    companion object {
        operator fun invoke(jsonObj: JSONObject) : PHv2SceneDimming {
            return PHv2SceneDimming(
                jsonObj.getInt(BRIGHTNESS)
            )
        }
    }
}

data class PHv2ScenePaletteEffects2 (
    val action: PHv2ScenePaletteEffects2Action
) {
    companion object {
        operator fun invoke(jsonObj: JSONObject) : PHv2ScenePaletteEffects2 {
            return PHv2ScenePaletteEffects2(
                PHv2ScenePaletteEffects2Action(jsonObj.getJSONObject(ACTION))
            )
        }
    }
}

data class PHv2ScenePaletteEffects2Action(
    /** prism, opal, glisten, sparkle, fire, candle, underwater, cosmos, sunbeam, enchant, no_effect */
    val effect: String,
    val parameters: PHv2ScenePaletteEffects2ActionParameters? = null
) {
    companion object {
        operator fun invoke(jsonObj: JSONObject) : PHv2ScenePaletteEffects2Action {
            val parameters = if (jsonObj.has(PARAMETERS)) {
                PHv2ScenePaletteEffects2ActionParameters(jsonObj.getJSONObject(PARAMETERS))
            }
            else { null }

            return PHv2ScenePaletteEffects2Action(
                effect = jsonObj.getString(EFFECT),
                parameters = parameters
            )
        }
    }
}

data class PHv2ScenePaletteEffects2ActionParameters(
    val color: PHv2LightColor? = null,
    val colorTemperature: PHv2SceneColorTemperature? = null,
    /** range [0..1] */
    val speed: Float = 0f
) {
    companion object {
        operator fun invoke(jsonObj: JSONObject) : PHv2ScenePaletteEffects2ActionParameters {
            val color = if (jsonObj.has(COLOR)) {
                PHv2LightColor(jsonObj.getJSONObject(COLOR))
            }
            else { null }

            val colorTemp = if (jsonObj.has(COLOR_TEMPERATURE)) {
                PHv2SceneColorTemperature(jsonObj.getJSONObject(COLOR_TEMPERATURE))
            }
            else { null }

            return PHv2ScenePaletteEffects2ActionParameters(
                color = color,
                colorTemperature = colorTemp,
                speed = jsonObj.optDouble(SPEED, 0.0).toFloat()
            )
        }
    }
}

/**
 * Describes the action that's to be executed on recall.
 * Since scenes usually have more than one light, each light
 * may (and probably does) have different actions.
 */
data class PHv2SceneAction(
    /** identifier of the light to execute the action on */
    val target: PHv2ItemInArray,
    /** the action to be executed */
    val action: PHv2SceneActionDetail
) {
    companion object {
        /**
         * Alternate constructor.  Takes a json object representing this
         * data.
         */
        operator fun invoke(jsonObj: JSONObject) : PHv2SceneAction {
            return PHv2SceneAction(
                target = PHv2ItemInArray(jsonObj.getJSONObject(TARGET)),
                action = PHv2SceneActionDetail(jsonObj.getJSONObject(ACTION))
            )
        }
    }
}

/**
 * The specifics of an Action.  See the parent class [PHv2SceneAction].
 */
data class PHv2SceneActionDetail(
    val on: PHv2SceneActionDetailOn? = null,
    val dimming: PHv2SceneDimming? = null,
    val color: PHv2SceneActionDetailColor? = null,
    val colorTemperature: PHv2SceneColorTemperature? = null,
    val gradient: PHv2SceneActionDetailGradient? = null,
    /** prism, opal, glisten, sparkle, fire, candle, underwater, cosmos, sunbeam, enchant, no_effect */
    val effects: String? = null,
    val evectsV2: PHv2SceneActionDetailEffectsV2? = null,
    val dynamics: PHv2SceneActionDetailDynamics? = null
) {
    companion object {
        /**
         * Alternate constructor: takes json object representing this data
         */
        operator fun invoke(jsonObj: JSONObject) : PHv2SceneActionDetail {
            val on = if (jsonObj.has(ON)) {
                PHv2SceneActionDetailOn(jsonObj.getJSONObject(ON))
            }
            else { null }

            val dimming = if (jsonObj.has(DIMMING)) {
                PHv2SceneDimming(jsonObj.getJSONObject(DIMMING))
            }
            else { null }

            val color = if (jsonObj.has(COLOR)) {
                PHv2SceneActionDetailColor(jsonObj.getJSONObject(COLOR))
            }
            else { null }

            val colorTemp = if (jsonObj.has(COLOR_TEMPERATURE)) {
                PHv2SceneColorTemperature(jsonObj.getJSONObject(COLOR_TEMPERATURE))
            }
            else { null }

            val gradient = if (jsonObj.has(GRADIENT)) {
                PHv2SceneActionDetailGradient(jsonObj.getJSONObject(GRADIENT))
            }
            else { null }

            val effects2 = if (jsonObj.has(EFFECTS)) {
                PHv2SceneActionDetailEffectsV2(jsonObj.getJSONObject(EFFECTS))
            }
            else { null }

            val dynamics = if (jsonObj.has(DYNAMICS)) {
                PHv2SceneActionDetailDynamics(jsonObj.getJSONObject(DYNAMICS))
            }
            else { null }

            return PHv2SceneActionDetail(
                on = on,
                dimming = dimming,
                color = color,
                colorTemperature = colorTemp,
                gradient = gradient,
                effects = null,         // deprecated
                evectsV2 = effects2,
                dynamics = dynamics
            )
        }
    }
}

data class PHv2SceneActionDetailOn(
    val on: Boolean
) {
    companion object {
        operator fun invoke(jsonObj: JSONObject) : PHv2SceneActionDetailOn {
            return PHv2SceneActionDetailOn(on = jsonObj.getBoolean(ON))
        }
    }
}

data class PHv2SceneActionDetailColor(
    val xy: PHv2SceneCieXyGamutPosition
) {
    companion object {
        operator fun invoke(jsonObj: JSONObject) : PHv2SceneActionDetailColor {
            return PHv2SceneActionDetailColor(
                xy = PHv2SceneCieXyGamutPosition(jsonObj.getJSONObject(XY))
            )
        }
    }
}

/**
 * Gradient properties
 */
data class PHv2SceneActionDetailGradient(
    /**
     * from docs:  "Collection of gradients points. For control of the
     * gradient points through a PUT a minimum of 2 points need to be provided."
     */
    val points: List<PHv2SceneActionDetailGradientPoint>,
    /** interpolated_palette, interpolated_palette_mirrored, random_pixelated */
    val mode: String
) {
    companion object {
        operator fun invoke(jsonObj: JSONObject) : PHv2SceneActionDetailGradient {

            val points = mutableListOf<PHv2SceneActionDetailGradientPoint>()
            val pointsJsonArray = jsonObj.getJSONArray(POINTS)
            for (i in 0 until pointsJsonArray.length()) {
                points.add(PHv2SceneActionDetailGradientPoint(pointsJsonArray.getJSONObject(i)))
            }

            return PHv2SceneActionDetailGradient(
                points = points,
                mode = jsonObj.getString(MODE)
            )
        }
    }
}

data class PHv2SceneActionDetailGradientPoint(
    val color: PHv2SceneActionDetailColor
) {
    companion object {
        operator fun invoke(jsonObj: JSONObject) : PHv2SceneActionDetailGradientPoint {

            return PHv2SceneActionDetailGradientPoint(
                PHv2SceneActionDetailColor(jsonObj.getJSONObject(COLOR))
            )
        }
    }
}

data class PHv2SceneCieXyGamutPosition(
    /** range [0..1] */
    val x: Float,
    /** range [0..1] */
    val y: Float
) {
    companion object {
        operator fun invoke(jsonObj: JSONObject) : PHv2SceneCieXyGamutPosition {
            return PHv2SceneCieXyGamutPosition(
                x = jsonObj.getDouble(X).toFloat(),
                y = jsonObj.getDouble(Y).toFloat(),
            )
        }
    }
}

data class PHv2SceneActionDetailEffectsV2(
    val action: PHv2SceneActionDetailEffectsV2Action
) {
    companion object {
        operator fun invoke(jsonObj: JSONObject) : PHv2SceneActionDetailEffectsV2 {
            return PHv2SceneActionDetailEffectsV2(
                action = PHv2SceneActionDetailEffectsV2Action(jsonObj.getJSONObject(ACTION))
            )
        }
    }
}

data class PHv2SceneActionDetailEffectsV2Action(
    /** prism, opal, glisten, sparkle, fire, candle, underwater, cosmos, sunbeam, enchant, no_effect */
    val effect: String,
    val parameters: PHv2SceneActionDetailEffectsV2ActionParameters? = null,
) {
    companion object {
        operator fun invoke(jsonObj: JSONObject) : PHv2SceneActionDetailEffectsV2Action {
            val params = if (jsonObj.has(PARAMETERS)) {
                PHv2SceneActionDetailEffectsV2ActionParameters(jsonObj.getJSONObject(PARAMETERS))
            }
            else { null }

            return PHv2SceneActionDetailEffectsV2Action(
                effect = jsonObj.getString(EFFECT),
                parameters = params
            )
        }
    }
}

data class PHv2SceneActionDetailEffectsV2ActionParameters(
    val color: PHv2LightColorXY? = null,
    val colorTemperature: PHv2SceneColorTemperature? = null,
    /** range [0..1] */
    val speed: Float = 0f
) {
    companion object {
        operator fun invoke(jsonObj: JSONObject) : PHv2SceneActionDetailEffectsV2ActionParameters {
            val color = if (jsonObj.has(COLOR)) {
                PHv2LightColorXY(jsonObj.getJSONObject(COLOR))
            }
            else { null }

            val colorTemp = if (jsonObj.has(COLOR_TEMPERATURE)) {
                PHv2SceneColorTemperature(jsonObj.getJSONObject(COLOR_TEMPERATURE))
            }
            else { null }

            return PHv2SceneActionDetailEffectsV2ActionParameters(
                color = color,
                colorTemperature = colorTemp,
                speed = jsonObj.optDouble(SPEED, 0.0).toFloat()
            )
        }
    }
}


/**
 * Duration of a light transition or timed effect.
 */
data class PHv2SceneActionDetailDynamics(
    /** duration of effect in ms */
    val duration: Int
) {
    companion object {
        /**
         * Alternate constructor: takes json object representing this data
         */
        operator fun invoke(jsonObj: JSONObject) : PHv2SceneActionDetailDynamics {

            return if (jsonObj.has(DURATION)) {
                PHv2SceneActionDetailDynamics(jsonObj.getJSONObject(DURATION))
            } else {
                PHv2SceneActionDetailDynamics(0)
            }
        }
    }
}

data class PHv2SceneColorTemperature(
    /** range [153..500] or null if color is not in ct spectrum */
    val mirek: Int? = null
) {
    companion object {
        operator fun invoke(jsonObj: JSONObject) : PHv2SceneColorTemperature {
            return PHv2SceneColorTemperature(
                jsonObj.optInt(MIREK)
            )
        }
    }
}