package com.sleepfuriously.paulsapp.model.philipshue

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * This is the communicator between all the Philips Hue components
 * and the viewmodel.  This middle layer will receive flows and pass
 * them along.
 */
class PhilipsHueRepository(
    /**
     * Anything that takes a while should be done within this scope.
     */
    private val coroutineScope: CoroutineScope
) {

    //-------------------------------
    //  flows to observe
    //-------------------------------

    private val _bridgesSet = MutableStateFlow<Set<PhilipsHueBridgeInfo>>(setOf())
    /** the complete list of all the bridges and associated data */
    val bridgesSet = _bridgesSet.asStateFlow()


    //-------------------------------
    //  class variables
    //-------------------------------

    /** Accessor to the Philips Hue Model */
    private val model = PhilipsHueModel(coroutineScope = coroutineScope)

    //-------------------------------
    //  init
    //

    init {
        // start consuming bridge flow from Model
        coroutineScope.launch {
            model.bridgeFlowSet.collectLatest {
                Log.d(TAG, "collecting bridgeFlowSet from bridgeModel. change = $it, hash = ${System.identityHashCode(it)}")

                // rebuilding a copy of the bridge set
                val newBridgeSet = mutableSetOf<PhilipsHueBridgeInfo>()
                it.forEach { bridge ->
                    newBridgeSet.add(bridge)
                    Log.d(TAG, "Setting bridge for flow:")
                    bridge.rooms.forEach { room ->
                        Log.d(TAG, "   room ${room.name}, on = ${room.on}, bri = ${room.brightness}")
                    }
                }
                // producing flow
                _bridgesSet.update { newBridgeSet }
            }
        }

    }

}

private const val TAG = "PhilipsHueRepository"