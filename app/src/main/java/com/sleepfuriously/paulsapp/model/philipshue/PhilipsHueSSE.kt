package com.sleepfuriously.paulsapp.model.philipshue

import android.util.Log
import com.sleepfuriously.paulsapp.getTime
import com.sleepfuriously.paulsapp.model.BODY_PEEK_SIZE
import com.sleepfuriously.paulsapp.model.OkHttpUtils.getAllTrustingSseClient
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceServerSentEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONException


/**
 * This is a more encapsulated version of collecting server-sent events (sse)
 * than my first try.  This class only deals with one bridge, but can be
 * instantiated as many times as needed.
 */
@OptIn(ExperimentalStdlibApi::class)
class PhilipsHueSSE(
    private val bridgeId: String,
    private val bridgeIpAddress: String,
    private val bridgeToken: String,
    coroutineScope: CoroutineScope
) : AutoCloseable {

    //---------------------------------
    //  flows
    //---------------------------------

//    private val _openEvent = Channel<Boolean>()
    private val _openEvent = MutableStateFlow(false)
    /**
     * An open event is sent when the bridge's sse is first opened or closed.
     * True means that the bridge is connected and receiving server-sent events.
     * Falst means that the bridge is not connected and no sse are flowing.
     */
//    val openEvent = _openEvent.receiveAsFlow()
    val openEvent = _openEvent.asStateFlow()


    private val _serverSentEvent = Channel<PHv2ResourceServerSentEvent>()
    /**
     * This channel sends events as they happen.  Each event contains
     * an instance of [PHv2ResourceServerSentEvent].
     */
    val serverSentEvent = _serverSentEvent.receiveAsFlow()


    //---------------------------------
    //  private data & data objects
    //---------------------------------

    /**
     * Reference to the [EventSource]--I use it to close the connections.
     * It'll be null if not in use.
     */
    private var eventSource : EventSource? = null

    /**
     * This is THE thing that listens for server-sent events.  It'll spit out
     * the events as the bridge sends them (once it's set up).
     */
    private val defaultListener = object : EventSourceListener() {

        override fun onOpen(eventSource: EventSource, response: Response) {
            super.onOpen(eventSource, response)
            Log.d(TAG, "EventSourceListener: Connection Opened")
            Log.d(TAG, "   time = ${getTime()}")

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
                    _openEvent.update { true }
//                    _openEvent.send(true)
                }
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
            Log.d(TAG, "sse - onClosed() for eventSource $eventSource")
            Log.d(TAG, "   time = ${getTime()}")

            val bridgeId = getBridgeIdFromEventSource(eventSource)
            if (bridgeId == null) {
                Log.e(TAG, "Unable to get bridge id in listener.onClosed()!")
                return
            }

            coroutineScope.launch {
                Log.d(TAG, "onClosed() sending _openEvent: false")
                _openEvent.update { false }
//                _openEvent.send(false)
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
            Log.d(TAG, "EventSourceListener: On Event Received!")
            Log.d(TAG, "   eventSource:")
            Log.d(TAG, "      id = $id")
            Log.d(TAG, "      type = $type")
            Log.d(TAG, "      data = $data")
            Log.d(TAG, "      time = ${getTime()}")

            val bridgeId = getBridgeIdFromEventSource(eventSource)
            if (bridgeId == null) {
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
                        _serverSentEvent.send(v2Event)
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
                    _openEvent.update { false }
//                    _openEvent.send(false)
                }

            }
            response?.body?.close()
        }
    }

    //---------------------------------
    //  constructors & initializations
    //---------------------------------

//    init {
//        startSse()
//    }


    override fun close() {
        // This is run whenever this instance goes out of scope (no longer is needed).
        // I use this to clean up.
        stopSSE()
    }

    //---------------------------------
    //  public functions
    //---------------------------------

    /**
     * Begins connecting to the bridge for server-sent events
     */
    fun startSse() {
        Log.d(TAG, "startSse()")

        val request = Request.Builder()
            .url("https://${bridgeIpAddress}/eventstream/clip/v2")
            .header("Accept", "text/event-stream")
            .addHeader("hue-application-key", bridgeToken)
            .tag(bridgeId)     // identifies this request (within the EventSource)
            .build()

        // making new eventsource
        eventSource = EventSources.createFactory(getAllTrustingSseClient())     // todo: make secure
            .newEventSource(
                request = request,
                listener = defaultListener
            )

        // note: the connection is not complete yet; we just made an attempt at connecting.
        // we'll know that the connection is successful in the onOpen() call in whatever
        // is listening to server-sent events.
    }


    /**
     * Stop receive sse events for the given bridge.
     *
     * side effects
     *  - [eventSource] is stopped (if it exists) and set to null
     */
    fun stopSSE() {
        eventSource?.cancel()
        eventSource = null
    }

    /**
     * Tells caller if sse is running
     */
    fun isSseRunning() : Boolean {
        return eventSource != null
    }

    //---------------------------------
    //  private functions
    //---------------------------------

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
    //  statics
    //---------------------------------

    companion object {


        // todo: all stuff that's similar to all instances will go here


    }


}

private const val TAG = "PhilipsHueSSE"