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
        room: PhilipsHueRoomInfo,
        bridge: PhilipsHueBridgeInfo
    ) : List<PHv2Scene> {

        Log.d(TAG, "getAllScenes() - room = ${room.name}")

        // Go through all the scenes in the bridge.  If any have a group that
        // matches the room id, we add it to the list.
        val sceneList = mutableListOf<PHv2Scene>()
        bridge.scenes.forEach { scene ->
            Log.d(TAG, "                 scene = ${scene.metadata.name}")
            if ((scene.group.rtype == ROOM) && (scene.group.rid == room.id)) {
                sceneList.add(scene)
                Log.d(TAG, "                 added!")
            }
        }
        return sceneList
    }

    /**
     * This tells the bridge to change the current settings for the given room
     * to now use the given scene.
     *
     * Note
     *  This will happen synchronously, but has no return value (so the caller
     *  can just put the call in a coroutine).  The results are reported by
     *  the bridge itself in the form of a server-sent event.
     *
     * @param   bridge      The bridge that controls the room
     *
     * @param   room        The room to change.
     *
     * @param   newScene    The scene that the room should now display.  This scene
     *                      should already be in the bridge's list of scenes.
     */
    suspend fun setRoomSceneToApi(
       bridge: PhilipsHueBridgeInfo,
       room: PhilipsHueRoomInfo,
       newScene: PHv2Scene,
    ) {
        // Figure out the body for the api PUT request.  The body (as far as I can figure)
        // is a json file that represents something...that tells the room to apply a
        // new scene.  Hmmm.
        //
        //  This tells a light to change its brightness to full-bright:
        //      {"dimming": {"brightness": 100}}
        //
        //  I think (from the docs?) it may look like this:
        //      {"children": [
        //          {
        //              "rid": "<scene_id>",
        //              "rtype": "scene"
        //          }
        //       ]
        //      }
        //
        //  let's see if this works
        //
        //  scene (Nightlight), id = 0d330862-2a8a-47dc-addb-c78da1adc21d
        //      nope, "invalid children"
        //


    }

    //----------------------------
    //  private functions
    //----------------------------

}

//----------------------------
//  constants
//----------------------------

private const val TAG = "PhilipsHueModelScenes"