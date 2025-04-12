package com.sleepfuriously.paulsapp.model.philipshue

import android.util.Log
import com.sleepfuriously.paulsapp.getTime
import com.sleepfuriously.paulsapp.model.BODY_PEEK_SIZE
import com.sleepfuriously.paulsapp.model.OkHttpUtils.getAllTrustingSseClient
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceServerSentEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONException

/**
 * Controller and producer of server-sent events (sse) from the
 * philips hue bridges.
 *
 * The events will be consumed by [PhilipsHueRepository] which will
 * spit 'em out to whomever needs them.
 */
class PhilipsHueServerSentEvents(coroutineScope: CoroutineScope) {

    //---------------------------------
    //  flows
    //---------------------------------

    private val _openEvent = Channel<Pair<String, Boolean>>()
    /**
     * An open event is sent when a bridge is first opened or closed
     * to server-sent events.
     * The pair is <Bridge Id, open/closed>  where open = true.
     */
    val openEvent = _openEvent.receiveAsFlow()


    private val _serverSentEvent = Channel<Pair<String, PHv2ResourceServerSentEvent>>()
    /**
     * This channel sends events as they happen.  Event contains:
     *  String - Id of the bridge
     *  event - instance of [PHv2ResourceServerSentEvent]
     */
    val serverSentEvent = _serverSentEvent.receiveAsFlow()


    //---------------------------------
    //  private data
    //---------------------------------

    /**
     * Reference to Event Source--I use it to close the connections.
     * Each item in the list is a Pair:
     *      bridgeId : EventSource
     */
    private val eventSourceList = mutableListOf<Pair<String, EventSource>>()
//    /** reference to Event Source--I use it to close the connection */
//    private lateinit var myEventSource : EventSource

    //---------------------------------
    //  data objects
    //---------------------------------

    private val defaultListener = object : EventSourceListener() {
        override fun onOpen(eventSource: EventSource, response: Response) {
            super.onOpen(eventSource, response)
            Log.d(TAG, "EventSourceListener: Connection Opened")
            Log.i(TAG, "   time = ${getTime()}")

            if (response.isSuccessful) {
                val eventJsonString = response.body?.string()
                if (eventJsonString == null) {
                    Log.e(TAG, "Unable to get json string from response body in onOpen()!  Aborting!")
                    return
                }
                Log.d(TAG, "sse - onOpen: json = $eventJsonString")

                val bridgeId = getBridgeIdFromEventSource(eventSource)
                if (bridgeId == null) {
                    Log.e(TAG, "Unable to get bridge id in listener.onOpen()!  Nothing to update!")
                    return
                }

                coroutineScope.launch {
                    Log.d(TAG, "onOpen() sending _openEvent: true")
                    _openEvent.send(Pair(bridgeId, true))
                }
/*
            todo: this code needs to be put in the receiver of the flow!!!

                // trying a new way to update a Set. Hope it works.
                _bridgeFlowSet.update {
                    val currentBridgeFlowSet = bridgeFlowSet.value.toMutableSet()
                    currentBridgeFlowSet.remove(bridge)
                    bridge.connected = true         // set connect status
                    currentBridgeFlowSet.add(bridge)
                    currentBridgeFlowSet
                }
 */
            }
            else {
                Log.e(TAG, "problem with response in defaultListener.onOpen()!")
                Log.e(TAG, "   code = ${response.code}")
                Log.e(TAG, "   message = ${response.message}")
                Log.e(TAG, "   request = ${response.request}")
                Log.e(TAG, "   time = ${getTime()}")
            }
            response.body?.close()
        }

        override fun onClosed(eventSource: EventSource) {
            super.onClosed(eventSource)
            Log.d(TAG, "sse - onClosed() for eventSource ${eventSource.toString()}")
            Log.i(TAG, "   time = ${getTime()}")

            val bridgeId = getBridgeIdFromEventSource(eventSource)
            if (bridgeId == null) {
                Log.e(TAG, "Unable to get bridge id in listener.onClosed()!")
                return
            }

            coroutineScope.launch {
                Log.d(TAG, "onClosed() sending _openEvent: false")
                _openEvent.send(Pair(bridgeId, false))
            }
        }

        /**
         * This is the event that the server sends us!!!!  YAY!
         */
        override fun onEvent(
            eventSource: EventSource,
            id: String?,
            type: String?,
            data: String
        ) {
//            super.onEvent(eventSource, id, type, data)
            Log.d(TAG, "EventSourceListener: On Event Received!")
            Log.d(TAG, "   eventSource = ${eventSource.toString()}")
            Log.d(TAG, "   id = $id")
            Log.d(TAG, "   type = $type")
            Log.d(TAG, "   data = $data")
            Log.i(TAG, "   time = ${getTime()}")

            val bridge = getBridgeIdFromEventSource(eventSource)
            if (bridge == null) {
                Log.e(TAG, "Unable to find bridge in onEvent()!")
                return
            }

            try {
                val eventJsonArray = JSONArray(data)

                coroutineScope.launch {
                    // process each event
                    for (i in 0 until eventJsonArray.length()) {
                        val eventJsonObj = eventJsonArray.getJSONObject(i)
                        val v2Event = PHv2ResourceServerSentEvent(eventJsonObj)
                        _serverSentEvent.send(Pair(bridge, v2Event))
                    }
                }
            }
            catch (e: JSONException) {
                Log.e(TAG, "Unable to parse event into json object: $data")
                e.printStackTrace()
                return
            }
        }

        override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
            super.onFailure(eventSource, t, response)

            Log.e(TAG, "EventSourceListener: On Failure():")
            Log.e(TAG, "   response.isSuccessful = ${response?.isSuccessful}")
            Log.e(TAG, "   response.code = ${response?.code}")
            Log.e(TAG, "   response.request = ${response?.request.toString()}")
            Log.e(TAG, "   response.body = ${response?.peekBody(BODY_PEEK_SIZE)?.string()}")
            Log.e(TAG, "   time = ${getTime()}")

            val bridgeId = getBridgeIdFromEventSource(eventSource)
            if (bridgeId == null) {
                Log.e(TAG, "Unable to get bridge in listener.onFailure()!")
            }
            else {
                coroutineScope.launch {
                    Log.d(TAG, "onFailure() sending openEvent: false")
                    _openEvent.send(Pair(bridgeId, false))
                }

            }
            response?.body?.close()
        }
    }

    /**
     * Grabs the id of thebridge that uses this given eventSource.  If none can be found, this
     * returns null.
     */
    private fun getBridgeIdFromEventSource(eventSource: EventSource) : String? {
        // The id of the bridge is conveniently stashed in the tag.
        val bridgeId = eventSource.request().tag()
        if (bridgeId == null) {
            Log.e(TAG, "Unable to find tag in getBridgeIdFromEventSource()!  Where is it???")
            return null
        }
        return bridgeId as String
    }



    //---------------------------------
    //  functions
    //---------------------------------

    /**
     * Begins connecing to the given bridge for server-sent events.
     */
    fun startSse(bridge: PhilipsHueBridgeInfo) {
        Log.d(TAG, "startSse()")

        val request = Request.Builder()
            .url("https://${bridge.ipAddress}/eventstream/clip/v2")
            .header("Accept", "text/event-stream")
            .addHeader("hue-application-key", bridge.token)
            .tag(bridge.id)     // identifies this request (within the EventSource)
            .build()

        // Check to see if this bridge is already on our list.
        // if so, remove it (we'll make a new one).
        val foundEventSource: Pair<String, EventSource>? = null
        for(i in 0 until eventSourceList.size) {
            if (eventSourceList[i].first == bridge.id) {
                eventSourceList.removeAt(i)
                break
            }
        }

        // making new eventsource
        val newEventSource = Pair(bridge.id,
            EventSources.createFactory(getAllTrustingSseClient())   // todo: make secure
                .newEventSource(
                    request = request,
                    listener = defaultListener
                ))

        // add this to the list
        eventSourceList.add(newEventSource)

        // note: the connection is not complete yet; we just made an attempt at connecting.
        // we'll know that the connection is successful in the onOpen() call in whatever
        // is listening to server-sent events.
    }

    /**
     * Stop receive sse events for the given bridge.
     */
    fun cancelSSE(bridgeId: String) {
        // find the right event source and cancel it.
        eventSourceList.forEach { eventSourcePair ->
            if (eventSourcePair.first == bridgeId) {
                Log.d(TAG, "Canceling sse for bridge id = $bridgeId")
                eventSourcePair.second.cancel()
                // should I remove this eventsource? hmmm
                return
            }
        }
    }

}

//---------------------------------
//  constants
//---------------------------------

private const val TAG = "PhilipsHueServerSentEvents"