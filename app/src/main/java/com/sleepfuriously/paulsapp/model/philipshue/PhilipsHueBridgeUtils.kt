package com.sleepfuriously.paulsapp.model.philipshue

import android.content.Context

/**
 * Utilities associated with the Philips Hue bridge device.
 *
 * Some of these functions may take an indeterminate time to
 * complete and should be done within a coroutine on a thread
 * different from the main ui.
 *
 * DETAILS
 *
 * The Philips Hue bridge is the central device of the Philips Hue
 * lights.  It contains all the information about the light system
 * and is the sole point of contact for this app.
 *
 * The initalization process requires knowing the local
 */

/**
 * Checks to see if the Philips Hue Bridge has been initialized.
 */
fun isBridgeInitialized(ctx: Context) : Boolean {
    return false
}

/**
 * Checks to see if the philips hue bridge is currently responding
 */
fun isBridgeConnected(ctx: Context) : Boolean {
    return false
}

/**
 * Finds the current "name" for the philips hue bridge.  The name is
 * actually a token necessary for any real communication with the
 * bridge.
 *
 * If the bridge has not been initialized, then this name won't
 * exists (returns null).
 *
 * NOTE:    This does care if the bridge is currently active or
 *          not.  This simply returns any token that has been
 *          recorded for this app.
 *
 * NOTE:    Does not check that the token is correct.  This can
 *          happen if connected to a different bridge or the
 *          bridge has forgotten this token somehow.
 *
 * @return      The token (String) needed to access the bridge.
 *              Null if no token has been assigned.
 */
fun getBridgeToken(ctx: Context) : String? {
    return null
}
