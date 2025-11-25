package com.sleepfuriously.paulsapp.model.philipshue

import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Scene
import android.util.Log
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueRoomInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueZoneInfo
import com.sleepfuriously.paulsapp.model.philipshue.json.ROOM
import com.sleepfuriously.paulsapp.model.philipshue.json.ZONE

/**
 * This part of the Philips Hue model just handles scenes.
 */
object PhilipsHueModelScenes {

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
     * Returns all the scenes that this bridge knows about.  On error, empty
     * list is returned.
     */
    suspend fun getAllScenesFromBridgeApi(bridge: PhilipsHueBridgeInfo) : List<PHv2Scene> {
        val newScenesList = mutableListOf<PHv2Scene>()
        val v2ScenesAll = PhilipsHueApi.getAllScenesFromApi(
            bridgeIp = bridge.ipAddress,
            bridgeToken = bridge.token
        )

        if (v2ScenesAll.errors.isNotEmpty()) {
            Log.e(TAG, "Problem in getAllScenesFromApi()!")
            Log.e(TAG, "   ${v2ScenesAll.errors[0].description}")
            Log.e(TAG, "   Aborting!")
            return newScenesList
        }

        v2ScenesAll.data.forEach { scene ->
            newScenesList.add(scene)
        }
        return newScenesList
    }

    /**
     * Given the id of a scene (v2), find the scene data.  If error, then
     * null is returned.
     */
    suspend fun getIndividualSceneFromApi(sceneId: String, bridge: PhilipsHueBridgeInfo) : PHv2Scene? {
        val scene = PhilipsHueApi.getSceneIndividualFromApi(
            sceneId = sceneId,
            bridgeIp = bridge.ipAddress,
            bridgeToken = bridge.token
        )
        if (scene.errors.isNotEmpty()) {
            Log.e(TAG, "Problem in getSceneFromApi()!")
            Log.e(TAG, "   ${scene.errors[0].description}")
            Log.e(TAG, "   Aborting!")
            return null
        }
        return scene.data[0]
    }

    /**
     * Finds all the scenes used in a room.
     *
     * @param   room        Room info for this room
     *
     * @param   bridge      The bridge in question.  It should have all its scenes
     *                      already loaded.
     *
     * preconditions
     *  - The bridge MUST have its scenes already loaded.  This does NOT poll the
     *  bridge to find the scenes directly.  Thus you need to call [getAllScenesFromBridgeApi]
     *  first, store that info into the bridge, then call this function.
     */
    fun getAllScenesForRoom(
        room: PhilipsHueRoomInfo,
        bridge: PhilipsHueBridgeInfo
    ) : List<PHv2Scene> {

        Log.d(TAG, "getAllScenesForRoom()  room = ${room.name}")

        // Go through all the scenes in the bridge.  If any have a group that
        // matches the room id, we add it to the list.
        val sceneList = mutableListOf<PHv2Scene>()
        bridge.scenes.forEach { scene ->
            Log.d(TAG, "   scene = ${scene.metadata.name}")
            if ((scene.group.rtype == ROOM) && (scene.group.rid == room.v2Id)) {
                sceneList.add(scene)
                Log.d(TAG, "      added!")
            }
        }
        return sceneList
    }

    /**
     * Similar to [getAllScenesForRoom].
     */
    fun getAllScenesForZone(
        zone: PhilipsHueZoneInfo,
        bridge: PhilipsHueBridgeInfo
    ) : List<PHv2Scene> {
        Log.d(TAG, "getAllScenesForZone()  zone = ${zone.name}")

        val sceneList = mutableListOf<PHv2Scene>()
        bridge.scenes.forEach { scene ->
            Log.d(TAG, "                 scene = ${scene.metadata.name}")
            if ((scene.group.rtype == ZONE) && (scene.group.rid == zone.v2Id)) {
                sceneList.add(scene)
                Log.d(TAG, "                 added!")
            }
        }
        return sceneList
    }

    //----------------------------
    //  private functions
    //----------------------------

}

//----------------------------
//  constants
//----------------------------

private const val TAG = "PhilipsHueModelScenes"