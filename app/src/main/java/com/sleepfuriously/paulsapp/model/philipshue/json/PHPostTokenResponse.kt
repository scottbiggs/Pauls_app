package com.sleepfuriously.paulsapp.model.philipshue.json

/*
 * Here you'll find data classes related to trying to get a token from
 * a Philips Hue bridge.
 *
 * Getting a token from a philips hue bridge
 *
 *  1) send a POST
 *      a) url = https://{url_for_bridge}/api
 *      b) body = {"devicetype":"app_name#instance_name", "generateclientkey":true}
 *
 *  2) process the response
 *      a) it'll be a JSON array with just one element (either "error" or "success",
 *          never both).  For creating the class properly, both possibilities
 *          are listed.
 *
        [
          {
            "error":{
              "type":101,
              "address":"",
              "description":"link button not pressed"
            }
          },
          {
            "success":{
              "username":"kAaab-q58sxmaz-jeHoOQ7C0uOxTYWO8ZXjcqdyc",
              "clientkey":"D4286DD214864A705E423FA50E6D4213"
            }
          }
        ]
 */

//---------------------------------------------
//  RECEIVE json data
//---------------------------------------------

/**
 * This is the Main Class for a response from the body of a request
 * to get a username (token) from the bridge.  Top Level--yeeha!
 *
 * The POST to get a username (token) will be a json array.  It will
 * have just one item though (and that item will be either a
 * [PhilpsHueError] or a [PHPostTokenResponseSuccessItem].
 */
class PHBridgePostTokenResponse : ArrayList<PHPostTokenResponseItem>()


/**
 * The response from a POST to get a username (token) from a bridge
 * will consist of an array of these (actually an array of exactly
 * one of these).  And Each of these will will either be an error
 * or a success--never both.  That's the way they designed it.
 *
 * @param   error       This is a json array of [PhilpsHueError] objects
 *
 * @param   success     A json array of [PHPostTokenResponseSuccessItem] objects
 */
data class PHPostTokenResponseItem(
    val error: PhilpsHueError?,
    val success: PHPostTokenResponseSuccessItem?
)


data class PHPostTokenResponseSuccessItem(
    val username: String,
    val clientkey: String
)


//---------------------------------------------
//  SEND json data
//---------------------------------------------
/**
 * When requesting a token, this must be in the Body of the PUT
 * request.
 *
 * Here is the JSON:
 *      {"devicetype":"app_name#instance_name", "generateclientkey":true}
 *
 *
 * @param   devicetype      The name of the device (that's this program).
 *                          I don't think the bridge needs it later, but
 *                          supposedly it'll identify this app vs other
 *                          apps and devices that access the bridge.  It
 *                          should be unique
 *
 * @param   generateclientkey   Defaults to true, which is what you want.
 *                              Experiments show that it doesn't need to
 *                              be here, but I'm doing this in case there
 *                              are changes later (it's in the docs).
 */
data class PhilipsHueTokenRequestBody(
    val devicetype: String,
    val generateclientkey: Boolean = true
)
