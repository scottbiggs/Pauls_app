package com.sleepfuriously.paulsapp.model.philipshue.json

/**
 * Holds the various keys for the json used by the philips
 * hue api version 2
 */
const val ACTION = "action"
const val ACTIONS = "actions"
const val ACTIVE = "active"
const val API_VERSION = "apiversion"
const val APP_DATA = "appdata"
const val ARCHETYPE = "archetype"
const val AUTO_DYNAMIC = "auto_dynamic"
const val AUTO_INSTALL = "autoinstall"
const val BACKUP = "backup"
const val BLUE = "blue"
const val BRIDGE = "bridge"
const val BRIDGE_V2 = "bridge_v2"
const val BRIDGE_V3 = "bridge_v3"
const val BRIDGE_ID = "bridge_id"
const val BRIGHTNESS = "brightness"
const val CERTIFIED = "certified"
const val CHECK_FOR_UPDATE = "checkforupdate"
const val CHILDREN = "children"
const val CLIENTKEY = "clientkey"
const val COLOR = "color"
const val COLOR_TEMPERATURE = "color_temperature"
const val COMMUNICATION = "communication"
const val CONFIG = "config"
const val CREATE_DATE = "create date"
const val CREATION_TIME = "creationtime"
const val CONFIGURED = "configured"
const val DATA = "data"
const val DATA_STORE_VERSION = "datastoreversion"
const val DESCRIPTION = "description"
const val DEVICE = "device"
const val DEVICE_MODE = "device_mode"
const val DHCP = "dhcp"
const val DIMMING = "dimming"
const val DURATION = "duration"
const val DYNAMICS = "dynamics"
const val EFFECT = "effect"
const val EFFECTS = "effects"
const val EFFECT_VALUES = "effect_values"
const val EFFECTS_V2 = "effects_v2"
const val EMPTY_STRING = ""
const val ERROR = "error"
const val ERROR_CODE = "errorcode"
const val ERRORS = "errors"
const val ESTIMATED_END = "estimated_end"
const val FACTORY_NEW = "factorynew"
const val FIXED_MIRED = "fixed_mired"
const val FUNCTION = "function"
const val GAMUT = "gamut"
const val GAMUT_TYPE = "gamut_type"
const val GATEWAY = "gateway"
const val GRADIENT = "gradient"
const val GREEN = "green"
const val GROUP = "group"
const val HARDWARE_PLATFORM_TYPE = "hardware_platform_type"
const val ID = "id"
const val ID_V1 = "id_v1"
const val IMAGE = "image"
const val INCOMING = "incoming"
const val INTERNET = "internet"
const val INTERNET_SERVICES = "internetservices"
const val IP_ADDRESS = "ipaddress"
const val LAST_CHANGE = "lastchange"
const val LAST_INSTALL = "lastinstall"
const val LAST_RECALL = "last_recall"
const val LAST_USE_DATE = "last use date"
const val LIGHT = "light"
const val LIGHTS = "lights"
const val LINK_BUTTON = "linkbutton"
const val LOCAL_TIME = "localtime"
const val MAC = "mac"
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
const val NET_MASK = "netmask"
const val NONE = "none"
const val NO_ERR_SPECIFIED = "no error specified"
const val NO_SIGNAL = "no_signal"
const val ON = "on"
const val OTHER = "other"
const val OUTGOING = "outgoing"
const val OWNER = "owner"
const val PALETTE = "palette"
const val PARAMETERS = "parameters"
const val POINTS = "points"
const val PORTAL_SERVICES = "portalservices"
const val PORTAL_STATE = "portalstate"
const val POWERUP = "powerup"
const val PRESET = "preset"
const val PRODUCT_ARCHETYPE = "product_archetype"
const val PRODUCT_DATA = "product_data"
const val PRODUCT_NAME = "product_name"
const val PROXY_ADDRESS = "proxyaddress"
const val PROXY_PORT = "proxyport"
const val RED = "red"
const val REMOTE_ACCESS = "remoteaccess"
const val REPLACES_BRIDGE_ID = "replacesbridgeid"
const val RID = "rid"
const val ROOM = "room"
const val RTYPE = "rtype"
const val SAFETY = "safety"
const val SERVICES = "services"
const val SERVICE_ID = "service_id"
const val SIGNAL = "signal"
const val SIGNALING = "signaling"
const val SIGNAL_VALUES = "signal_values"
const val SIGNED_ON = "signedon"
const val SOFTWARE_VERSION = "software_version"
const val SPEED = "speed"
const val SPEED_VALID = "speed_valid"
const val STARTER_KIT_ID = "starterkitid"
const val STATE = "state"
const val STATUS = "status"
const val STATUS_VALUES = "status_values"
const val SUCCESS = "success"
const val SW_VERSION = "swversion"
const val SW_UPDATE = "swupdate"
const val SW_UPDATE2 = "swupdate2"
const val TARGET = "target"
const val TIME = "time"
const val TIME_ZONE = "time_zone"
const val TYPE = "type"
const val UPDATE_TIME = "updatetime"
const val USERNAME = "username"
const val UTC = "utc"
const val WHITELIST = "whitelist"
const val X = "x"
const val XY = "xy"
const val Y = "y"
const val ZIGBEE_CHANNEL = "zigbeechannel"

//----------------------------
//  RTYPES
//----------------------------

const val RTYPE_AUTH_V1 = "auth_v1"
const val RTYPE_BEHAVIOR_INSTANCE = "behavior_instance"
const val RTYPE_BEHAVIOR_SCRIPT = "behavior_script"
const val RTYPE_BRIDGE = "bridge"
/**
 * API says:  Homes group rooms as well as devices not assigned to a room.
 * My interpetation: They call it a Bridge Home.  It's the bridge itself and
 * things attached to it, like [RTYPE_GROUP_LIGHT], [RTYPE_DEVICE],
 * [RTYPE_ROOM].
 *
 * Contains:
 *  List of children (could be devices, rooms, etc)
 *      rid, rtype
 *  List of services (possibly grouped lights, etc)
 *      rid, rtype
 */
const val RTYPE_BRIDGE_HOME = "bridge_home"
const val RTYPE_BUTTON = "button"
const val RTYPE_CAMERA_MOTION = "camera_motion"
const val RTYPE_CONTACT = "contact"

/** Identifies an [RTYPE] as a device instead of a service or something (could be a light!) */
const val RTYPE_DEVICE = "device"
const val RTYPE_DEVICE_POWER = "device_power"
const val RTYPE_DEVICE_SOFTWARE_UPDATE = "device_software_update"
const val RTYPE_ENTERTAINMENT = "entertainment"
const val RTYPE_ENTERTAINMENT_CONFIGURATION = "entertainment_configuration"
const val RTYPE_GEOFENCE_CLIENT = "geofence_client"
const val RTYPE_GEOLOCATION = "geolocation"
/** Denotes that this is a group of lights */
const val RTYPE_GROUP_LIGHT = "grouped_light"
const val RTYPE_GROUPED_LIGHT_LEVEL = "grouped_light_level"
const val RTYPE_GROUPED_MOTION = "grouped_motion"
const val RTYPE_HOMEKIT = "homekit"

/** This one is actually a light! */
const val RTYPE_LIGHT = "light"

const val RTYPE_LIGHT_LEVEL = "light_level"
const val RTYPE_MATTER = "matter"
const val RTYPE_MATTER_FABRIC = "matter_fabric"
const val RTYPE_MOTION = "motion"

/** Another new type of group--don't know what to make of it yet */
const val RTYPE_PRIVATE_GROUP = "private_group"

const val RTYPE_PUBLIC_IMAGE = "public_image"
const val RTYPE_RELATIVE_ROTARY = "relative_rotary"
/** This one is obvious (finally something I can work with). */
const val RTYPE_ROOM = "room"

const val RTYPE_SCENE = "scene"
const val RTYPE_SERVICE_GROUP = "service_group"
const val RTYPE_SMART_SCENE = "smart_scene"
const val RTYPE_TAMPER = "tamper"
const val RTYPE_TEMPERATURE = "temperature"
const val RTYPE_ZGP_CONNECTIVITY = "zgp_connectivity"
const val RTYPE_ZIGBEE_CONNECTIVITY = "zigbee_connectivity"
const val RTYPE_ZIGBEE_DEVICE_DISCOVERY = "zigbee_device_discovery"
const val RTYPE_ZONE = "zone"


//----------------------------
//  SSE Types
//----------------------------

/** Signals this event is an ADD */
const val EVENT_ADD = "add"

/** Signals this event is an UPDATE (the most common) */
const val EVENT_UPDATE = "update"

/** Signals this event is a DELETE */
const val EVENT_DELETE = "delete"

/** Signals this event is an ERROR--don't know what to do with these */
const val EVENT_ERROR = "error"
