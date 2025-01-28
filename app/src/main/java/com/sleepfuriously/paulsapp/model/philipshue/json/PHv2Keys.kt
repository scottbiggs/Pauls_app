package com.sleepfuriously.paulsapp.model.philipshue.json

/**
 * Holds the various keys for the json used by the philips
 * hue api version 2
 */

const val ARCHETYPE = "archetype"
const val BLUE = "blue"
const val BRIDGE_ID = "bridge_id"
const val BRIGHTNESS = "brightness"
const val CERTIFIED = "certified"
const val CHILDREN = "children"
const val CLIENTKEY = "clientkey"
const val COLOR = "color"
const val COLOR_TEMPERATURE = "color_temperature"
const val CONFIGURED = "configured"
const val DATA = "data"
const val DESCRIPTION = "description"
const val DEVICE_MODE = "device_mode"
const val DIMMING = "dimming"
const val DYNAMICS = "dynamics"
const val EFFECTS = "effects"
const val EFFECT_VALUES = "effect_values"
const val EMPTY_STRING = ""
const val ERROR = "error"
const val ERRORS = "errors"
const val ESTIMATED_END = "estimated_end"
const val FIXED_MIRED = "fixed_mired"
const val FUNCTION = "function"
const val GAMUT = "gamut"
const val GAMUT_TYPE = "gamut_type"
const val GREEN = "green"
const val HARDWARE_PLATFORM_TYPE = "hardware_platform_type"
const val ID = "id"
const val ID_V1 = "id_v1"
const val LIGHT = "light"
const val MANUFACTURER_NAME = "manufacturer_name"
const val METADATA = "metadata"
const val MIN_DIM_LEVEL = "min_dim_level"
const val MIREK = "mirek"
const val MIREK_MAXIMUM = "mirek_maximum"
const val MIREK_MINIMUM = "mirek_minimum"
const val MIREK_SCHEMA = "mirek_schema"
const val MIREK_VALID = "mirek_valid"
const val MODE = "mode"
const val MODEL_ID = "model_id"
const val MODE_VALUES = "mode_values"
const val NAME = "name"
const val NONE = "none"
const val NO_ERR_SPECIFIED = "no error specified"
const val NO_SIGNAL = "no_signal"
const val ON = "on"
const val OTHER = "other"
const val OWNER = "owner"
const val POWERUP = "powerup"
const val PRESET = "preset"
const val PRODUCT_ARCHETYPE = "product_archetype"
const val PRODUCT_DATA = "product_data"
const val PRODUCT_NAME = "product_name"
const val RED = "red"
const val RID = "rid"
const val RTYPE = "rtype"
const val SAFETY = "safety"
const val SERVICES = "services"
const val SERVICE_ID = "service_id"
const val SIGNAL = "signal"
const val SIGNALING = "signaling"
const val SIGNAL_VALUES = "signal_values"
const val SOFTWARE_VERSION = "software_version"
const val SPEED = "speed"
const val SPEED_VALID = "speed_valid"
const val STATUS = "status"
const val STATUS_VALUES = "status_values"
const val SUCCESS = "success"
const val TIME_ZONE = "time_zone"
const val TYPE = "type"
const val USERNAME = "username"
const val X = "x"
const val XY = "xy"
const val Y = "y"

//----------------------------
//  RTYPES
//----------------------------

/** Identifies an [RTYPE] as a device instead of a service or something (could be a light!) */
const val RTYPE_DEVICE = "device"

/** This one is actually a light! */
const val RTYPE_LIGHT = "light"

/** Denotes that this is a group of lights */
const val RTYPE_GROUP_LIGHT = "grouped_light"

/** Identifies an [TYPE] as a device */
const val TYPE_DEVICE = "device"

//----------------------------
//  TYPES
//----------------------------

const val ROOM = "room"