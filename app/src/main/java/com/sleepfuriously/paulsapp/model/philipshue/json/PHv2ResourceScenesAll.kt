package com.sleepfuriously.paulsapp.model.philipshue.json

/**
 * Represents the json returned from a bridge GET about its scenes.
 *
 *      https://<bridge_ip>/clip/v2/resource/scene
 */
data class PHv2ResourceScenesAll(
    val errors: List<PHv2Error> = listOf(),
    val data: List<PHv2Scene> = listOf()
)

/**
 * Data returned from a bridge GET inquiry:
 *      https://<bridge_ip>/clip/v2/resource/scene/<scene_id>
 *
 * Note:
 *  This is EXACTLY like [PHv2ResourceScenesAll], except that the
 *  [data] list has just 1 item (the one that's id matches).
 */
data class PHv2ResourceSceneIndividual(
    val errors: List<PHv2Error> = listOf(),
    val data: List<PHv2Scene> = listOf()
)

/**
 * The meat--info for a scene is described here.
 */
data class PHv2Scene(
    val type: String,
    val id: String,
    val idV1: String,
    /** "List of actions to be executed synchronously on recall" */
    val actions: List<PHv2SceneAction>,
    val palette: PHv2ScenePalette,
    /** there is no documentation on this, except that it's required */
    val recall: Any,
    val metadata: PHv2SceneMetadata,    // different from other metadata
    /**
     * Groups associated with this scene.  All services in the group are
     * part of this scene. If the group is changed the scene is update
     * (e.g. light added/removed)
     */
    val group: PHv2ItemInArray,
    /** range [0..1] */
    val speed: Float,
    /** Indicates whether to automatically start the scene dynamically on active recall */
    val autoDynamic: Boolean,
    val status: PHv2SceneStatus
)

/**
 * Consists the information about the current status and last time it is recalled (used)
 */
data class PHv2SceneStatus(
    /** inactive, static, dynamic_palette */
    val active: String,
    /** data / time */
    val lastRecall: String
)

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
    val image: PHv2ItemInArray,
    /** Application specific data. Free format string. Length [1..16] chars */
    val appdata: String
)

/**
 * Palette of colors used when playing dynamics.  Each array should have
 * the same number of items (which is range [0..9]).
 */
data class PHv2ScenePalette(
    val color: List<PHv2SceneColor>,
    val dimming: List<PHv2SceneDimming>,
    val colorTemperature: List<PHv2SceneColorTemperature>,
    @Deprecated("use effectsV2")
    val effects: List<Any>?,
    val effectsV2: List<PHv2ScenePaletteEffects2>
)

data class PHv2SceneColor(
    val color: PHv2LightColorXY,
    val dimming: PHv2SceneDimming
)

data class PHv2SceneDimming(
    /** percentage brightness. range = [0..100] */
    val brightness: Int
)

data class PHv2ScenePaletteEffects2 (
    val action: PHv2ScenePaletteEffects2Action
)

data class PHv2ScenePaletteEffects2Action(
    /** prism, opal, glisten, sparkle, fire, candle, underwater, cosmos, sunbeam, enchant, no_effect */
    val effect: String,
    val parameters: PHv2ScenePaletteEffects2ActionParameters
)

data class PHv2ScenePaletteEffects2ActionParameters(
    val color: PHv2LightColor,
    val colorTemperature: PHv2SceneColorTemperature,
    /** range [0..1] */
    val speed: Float
)

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
)

/**
 * The specifics of an Action.  See the parent class [PHv2SceneAction].
 */
data class PHv2SceneActionDetail(
    val on: PHv2SceneActionDetailOn,
    val dimming: PHv2SceneDimming,
    val color: PHv2SceneActionDetailColor,
    val colorTemperature: PHv2SceneColorTemperature,
    val gradient: PHv2SceneActionDetailGradient? = null,
    /** prism, opal, glisten, sparkle, fire, candle, underwater, cosmos, sunbeam, enchant, no_effect */
    val effects: String,
    val evectsV2: PHv2SceneActionDetailEffectsV2,
    val dynamics: PHv2SceneActionDetailDynamics
)

data class PHv2SceneActionDetailOn(
    val on: PHv2LightOn
)

data class PHv2SceneActionDetailColor(
    val xy: PHv2SceneCieXyGamutPosition
)

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
)

data class PHv2SceneActionDetailGradientPoint(
    val color: PHv2SceneActionDetailColor
)

data class PHv2SceneCieXyGamutPosition(
    /** range [0..1] */
    val x: Float,
    /** range [0..1] */
    val y: Float
)

data class PHv2SceneActionDetailEffectsV2(
    val action: PHv2SceneActionDetailEffectsV2Action
)

data class PHv2SceneActionDetailEffectsV2Action(
    /** prism, opal, glisten, sparkle, fire, candle, underwater, cosmos, sunbeam, enchant, no_effect */
    val effect: String,
    val parameters: PHv2SceneActionDetailEffectsV2ActionParameters,
)

data class PHv2SceneActionDetailEffectsV2ActionParameters(
    val color: PHv2LightColorXY,
    val colorTemperature: PHv2SceneColorTemperature,
    /** range [0..1] */
    val speed: Float
)


/**
 * Duration of a light transition or timed effect.
 */
data class PHv2SceneActionDetailDynamics(
    /** duration of effect in ms */
    val duration: Int
)

data class PHv2SceneColorTemperature(
    /** range [153..500] or null if color is not in ct spectrum */
    val mirek: Int?
)