package com.sleepfuriously.paulsapp.model.philipshue

import android.util.Log
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2GroupedLight
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Zone
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_GROUP_LIGHT
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_LIGHT

/**
 * This is the place to call all the Zone related stuff for the Philips
 * Hue light system.  It's a part of the [PhilipsHueModel], but separated
 * to keep things modularized (and reasonably sized).
 */
object PhilipsHueModelZones {

    //----------------------------
    //  flows
    //----------------------------

    //----------------------------
    //  init
    //----------------------------

    //----------------------------
    //  public functions
    //----------------------------

    /**
     * Uses a GET from the bridge to find all the zones that the bridge knows
     * about.
     *
     * @return  List of all the zones.  On error, we'll get an empty list (which
     *          could be a normal result as well--not all bridges have zones).
     */
    suspend fun getAllZonesFromApi(bridge: PhilipsHueBridgeInfo) : List<PHv2Zone> {
        val zoneList = mutableListOf<PHv2Zone>()
        val v2ZonesAll = PhilipsHueBridgeApi.getAllZonesFromApi(
            bridgeIp = bridge.ipAddress,
            bridgeToken = bridge.token
        )

        if (v2ZonesAll.errors.isNotEmpty()) {
            Log.e(TAG, "Problem in getAllZonesFromApi()!")
            Log.e(TAG, "   ${v2ZonesAll.errors[0].description}")
            Log.e(TAG, "   Aborting!")
            return zoneList
        }

        v2ZonesAll.data.forEach { zone ->
            zoneList.add(zone)
        }
        return zoneList
    }


    /**
     * Given the id of a zone (v2), returns the zone data.  If error, then
     * null is returned.
     */
    suspend fun getIndividualZoneFromApi(zoneId: String, bridge: PhilipsHueBridgeInfo) : PHv2Zone? {
        val zone = PhilipsHueBridgeApi.getZoneIndividualFromApi(
            zoneId = zoneId,
            bridgeIp = bridge.ipAddress,
            bridgeToken = bridge.token
        )
        if (zone.errors.isNotEmpty()) {
            Log.e(TAG, "Problem in getZoneFromApi()!")
            Log.e(TAG, "   ${zone.errors[0].description}")
            Log.e(TAG, "   Aborting!")
            return null
        }
        return zone.data[0]
    }

    /**
     * Finds all the lights that a zone uses.
     *
     * @param   zone    the zone that we're concerned about
     *
     * @param   bridge  The bridge MUST have its zones already loaded.
     *                  This does NOT poll the the bridge through the
     *                  API, but goes through the data already stored
     *                  in this bridge.
     */
    suspend fun getAllLightsForZone(
        zone: PHv2Zone,
        bridge: PhilipsHueBridgeInfo
    ) : List<PhilipsHueLightInfo> {

        Log.d(TAG, "getAllLightsForZone() - zone = ${zone.id}")

        // go through all the children and see if any are lights.
        val lightList = mutableListOf<PhilipsHueLightInfo>()
        zone.children.forEach { child ->
            if (child.rtype == RTYPE_LIGHT) {
                val lightIndividual = PhilipsHueBridgeApi.getLightInfoFromApi(
                    lightId = child.rid,
                    bridgeIp = bridge.ipAddress,
                    bridgeToken = bridge.token
                )
                if (lightIndividual != null) {

                    if ((lightIndividual.errors.isNotEmpty()) || (lightIndividual.data.isEmpty())) {
                        // oooh, the bridge reported something wrong
                        Log.e(TAG, "Error getting light in getAllLightsForZone (lightId = ${child.rid})")
                        Log.e(TAG, "   description = ${lightIndividual.errors[0].description}")
                    }
                    else {
                        // so far so good. Convert and add it
                        val light = PhilipsHueDataConverter.convertV2Light(lightIndividual.data[0])
                        lightList.add(light)
                    }
                }
                else {
                    // there was a problem getting the light. Report to log and continue
                    Log.e(TAG, "Unable to get light (id = ${child.rid}) in getAllLightsForZone (bridge problem?). skipping")
                }
            }
        }
        return lightList
    }


    /**
     * Gets the Grouped_light(s) from a zone.
     *
     * I think there may only be one grouped_light per zone, but one can't be certain!
     *
     * @return      A list of all the grouped_light things in a zone that are found int
     *              the zone's services list.  If an error, then none are returned.
     */
    suspend fun getAllGroupedLightsForZone(
        zone: PHv2Zone,
        bridge: PhilipsHueBridgeInfo
    ) : List<PHv2GroupedLight> {

        Log.d(TAG, "getAllGroupsForZone() zoneId = ${zone.id}")
        val groupedLights = mutableListOf<PHv2GroupedLight>()

        // For efficiency (so we hit the bridge only once) get all the grouped
        // lights for this bridge
        val v2GroupedLights = PhilipsHueBridgeApi.getAllGroupedLightsFromApi(
            bridgeIp = bridge.ipAddress,
            bridgeToken = bridge.token
        )

        // check for errors (no point continuing otherwise!)
        if (v2GroupedLights == null) {
            Log.e(TAG, "Unable to contact bridge in getAllGroupsForZone(). Aborting!")
            return groupedLights
        }
        if (v2GroupedLights.errors.isNotEmpty() || (v2GroupedLights.data.isEmpty())) {
            // errors reported by bridge
            Log.e(TAG, "Bridge had problems in getAllGroupsForZone()")
            Log.e(TAG, "   description = ${v2GroupedLights.errors[0].description}")
            return groupedLights
        }

        // Go through the services for this zone and see if there are any grouped lights.
        // Match that with the data we just downloaded from the bridge and add that to
        // our list.
        zone.services.forEach { service ->
            if (service.rtype == RTYPE_GROUP_LIGHT) {
                // find this in our grouped light list
                for (i in 0 until v2GroupedLights.data.size) {
                    if (v2GroupedLights.data[i].id == service.rid) {
                        // Found it! Add it and break out of this inner loop
                        groupedLights.add(v2GroupedLights.data[i])
                        break
                    }
                }
            }
        }
        return groupedLights
    }


}

private const val TAG = "PhilipsHueModelZones"