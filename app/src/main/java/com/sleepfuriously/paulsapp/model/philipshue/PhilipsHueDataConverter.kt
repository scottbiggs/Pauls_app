package com.sleepfuriously.paulsapp.model.philipshue

import android.util.Log
import com.sleepfuriously.paulsapp.model.philipshue.json.DEVICE
import com.sleepfuriously.paulsapp.model.philipshue.json.EMPTY_STRING
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ItemInArray
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Room
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2RoomIndividual
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_DEVICE
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_GROUP_LIGHT
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_LIGHT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use this to convert between different data types (primarily
 * between PHv2... and PhilipsHue...
 */
object PhilipsHueDataConverter {

// TODO:

}

private const val TAG = "PhilipsHueDataConverter"