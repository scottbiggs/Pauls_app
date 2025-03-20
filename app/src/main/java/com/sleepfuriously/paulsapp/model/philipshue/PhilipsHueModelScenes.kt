package com.sleepfuriously.paulsapp.model.philipshue

import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceScenesAll
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Scene
import kotlinx.coroutines.CoroutineScope
import android.util.Log
import com.sleepfuriously.paulsapp.model.philipshue.json.ROOM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * This part of the Philips Hue model just handles scenes.
 *
 * I'm experimenting with a little different way of organizing here.
 * The idea is this will hold all scene info.  When started, it'll
 * tap into the bridge (via [getAllScenesFromApi]) and find all the
 *
 * Note
 *      Each instance of this will handle scenes from just ONE bridge.
 *      The caller will need to keep track of the different bridges.
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
    suspend fun getAllScenesFromApi(bridge: PhilipsHueBridgeInfo) : List<PHv2Scene> {
        val newScenesList = mutableListOf<PHv2Scene>()
        val v2ScenesAll = PhilipsHueBridgeApi.getAllScenesFromApi(bridge)

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
        val scene = PhilipsHueBridgeApi.getSceneIndividualFromApi(sceneId, bridge)
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
     * @param   roomId      The v2 id for this room
     *
     * @param   bridge      The bridge in question.  It should have all its scenes
     *                      already loaded.
     *
     * preconditions
     *  - The bridge MUST have its scenes already loaded.  This does NOT poll the
     *  bridge to find the scenes directly.  Thus you need to call [getAllScenesFromApi]
     *  first, store that info into the bridge, then call this function.
     */
    fun getAllScenesForRoom(
        roomId: String,
        bridge: PhilipsHueBridgeInfo
    ) : List<PHv2Scene> {
        // Go through all the scenes in the bridge.  If any have a group that
        // matches the room id, we add it to the list.
        val sceneList = mutableListOf<PHv2Scene>()
        bridge.scenes.forEach { scene ->
            if ((scene.group.rtype == ROOM) && (scene.group.rid == roomId)) {
                sceneList.add(scene)
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