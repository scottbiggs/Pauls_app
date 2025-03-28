package com.sleepfuriously.paulsapp

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2LightColorGamut
import java.lang.Math.pow
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Holds general math functions.  Since kotlin doesn't like
 * vectors (matrices), I'm putting all that stuff here.
 */

//-------------------------------
//  functions
//-------------------------------

/**
 * Finds if a given point is within the specific triangle.
 *  from: https://stackoverflow.com/a/9755252/624814
 *  explanation: https://math.stackexchange.com/a/4624564/229631
 *
 * As far as I can tell, this uses a lot of cross products.  The idea
 * is that if the cross products of the vectors going from P to A, and then
 * traversing around the triangle in clockwise direction, then they should
 * all have the same sign for the third term (the cross products will point
 * in the same direction).  This is furthermore optimized by going backwards
 * from P to C, making sure that it's pointing in the opposite direction (since
 * we're now moving counter-clockwise).
 *
 * Note:
 *  There's another way to see if a point is in the triangle: find the area of the
 *  three triangles one can make with p and two of the other triangle's points.
 *  The area of the three triangles can equal the area of the original triangle iff
 *  the point p is within the triangle.
 *
 */
fun isPointInTriangle(
    p: MyPoint,
    a: MyPoint, b: MyPoint, c: MyPoint
) : Boolean {

    if (areThreePointsTriangle(a, b, c) == false) {
        Log.e(TAG, "isPointInTriangle() trying to work with three points that are not a triangle!")
        Log.e(TAG, "    ... returning false")
        return false
    }

    // Find vector AP (between points a and p)
    val ap_x = p.x - a.x
    val ap_y = p.y - a.y

    // (b.x - a.x), (b.y - a.y) is the side AB, and is clockwise.
    // We'll do just the third term of the crossproduce AB x AP and
    // keep only the direction (boolean: positive = true).
    val thirdTermABxAPisPositive = (b.x - a.x) * ap_y - (b.y - a.y) * ap_x > 0

    // similar for the other direction
    if ((c.x - a.x) * ap_y - (c.y - a.y) * ap_x > 0 == thirdTermABxAPisPositive)
        return false

    if ((c.x - b.x) * (p.y - b.y) - (c.y - b.y)*(p.x - b.x) > 0 != thirdTermABxAPisPositive)
        return false

    return true
}

/**
 * Determines if the given three points make a triangle.  Yes, if you think
 * about it, not every group of 3 points is actually a triangle!
 */
fun areThreePointsTriangle(
    a: MyPoint, b: MyPoint, c: MyPoint
) : Boolean {

    // All three points must be different
    if ((a == b) || (a == c) || (b == c)) {
        return false
    }

    // check for vertical lines (which will screw up the next step)
    if (a.x == b.x) {
        if (a.x == c.x) {
            // A & B are vertically stacked, but C is not
            return true
        }
        else {
            // all points are vertically stacked--a line, not a triangel
            return false
        }
    }
    if (a.x == c.x) {
        // A & C are vertically stacked, but B is not
        return true
    }
    if (b.x == c.x) {
        // B & C are vertically stacked, but A is not
        return true
    }

    // Are the points collinear?  Since two of the triangle side must pass
    // through B, if the slope of AB and the slope of BC are the same, then
    // the three points must be colinear.  And thus not a triangle.
    val slopeAB = (b.y - a.y) / (b.x - a.x)
    val slopeBC = (c.y - b.y) / (c.x - b.x)
    if (slopeAB == slopeBC) {
        return false
    }

    return true
}


/**
 * Given a line segment defined by two points a and b, find the point on
 * that segment that's closest to the given point (x, y).
 */
private fun findClosestPointInLineSegment(
    x: Double,
    y: Double,
    a: MyPoint,
    b: MyPoint
) : MyPoint {

    // we'll use t to represent the nearest point on the line (infinite length).
    // Project (x, y) onto line ab, computing parameterized position
    // with d(t) = a + t * (b - a)




    // todo: make this work properly.  For testing, I'm just returning the closest end point
    val distA = a.distance(MyPoint(x, y))
    val distB = b.distance(MyPoint(x, y))

    if (distA <= distB) {
        return a
    }
    return b
}


/**
 * Finds the point in a triangle that's closest to a given point, (x, y).
 * If p is within the triangle, then p is returned.
 *
 * @param   x, y            the starting point
 *
 * @param   gamutTriangle   a way to define a triangle
 *
 * @return  - p if it's within the triangle
 *          - The point in the triangle that's closest to p (will be along
 *          an edge)
 */
private fun findClosestPointToTriangle(
    x: Double,
    y: Double,
    gamutTriangle: PHv2LightColorGamut
) : MyPoint {

    // the easy case
    if (isPointInGamutTriangle(x, y, gamutTriangle)) {
        return MyPoint(x, y)
    }

    // convert each edge of the triangle to a line


    // todo: do this right.  For now I'm just returning the closest endpoint of the gamut triangle
    val p = MyPoint(x, y)
    val dist1 = p.distance(MyPoint(gamutTriangle.red))
    val dist2 = p.distance(MyPoint(gamutTriangle.green))
    val dist3 = p.distance(MyPoint(gamutTriangle.blue))

    if (dist1 == minOf(dist1, dist2, dist3)) {
        return MyPoint(gamutTriangle.red)
    }
    else if (dist2 < dist3) {
        return MyPoint(gamutTriangle.green)
    }
    return MyPoint(gamutTriangle.blue)
}


/**
 * Is the given point within the gamut triangle?
 */
private fun isPointInGamutTriangle(
    x: Double, y: Double,
    gamutTriangle: PHv2LightColorGamut
) : Boolean {

    // convert to MyPoints
    val s = MyPoint(x = x, y = y)
    val a = MyPoint(x = gamutTriangle.red.first, y = gamutTriangle.red.second)
    val b = MyPoint(x = gamutTriangle.green.first, y = gamutTriangle.green.second)
    val c = MyPoint(x = gamutTriangle.blue.first, y = gamutTriangle.blue.second)

    return isPointInTriangle(s, a, b, c)
}

/**
 * Converts colors from the CIE xy coordinates to RGB.
 * todo: not sure I want to abandon all this yet
 */
fun convertCIEtoRGB(x: Double, y: Double, bri: Int, gamut: PHv2LightColorGamut) : Color {

    Log.d(TAG, "convertCIEtoRGB() x = $x, y = $y, bri = $bri")

    // use this from now on
    val p = findClosestPointToTriangle(x, y, gamut)


    // from https://developers.meethue.com/develop/application-design-guidance/color-conversion-formulas-rgb-to-xy-and-back/
    // calc XYZ values
    val z = 1.0 - p.x - p.y
    Log.d(TAG, "convertCIEtoRGB() p.x = ${p.x}, p.y = ${p.y}, z = $z")

//    val bigY = bri.toDouble() / 254.0 * 100.0
    val bigY = bri.toDouble() / 100.0       // put in range 0..100
    val bigX = (bigY / p.y) * p.x
    val bigZ = (bigY / p.y) * z

//    val bigY = min(bri2, 1.0) // can't be bigger than 1
//    val bigX = min((bigY / y) * x, 1.0)
//    val bigZ = min((bigY / y) * z, 1.0)

    Log.d(TAG, "convertCIEtoRGB() bigX = $bigX, bigY = $bigY, bigZ = $bigZ")


    // convert to rgb using Wide RGB D65 conversion -todo double check to make sure this is right
    var r = bigX * 1.656492 - bigY * 0.354851 - bigZ * 0.255038
    var g = -bigX * 0.707196 + bigY * 1.655397 + bigZ * 0.036152
    var b = bigX * 0.051713 - bigY * 0.121364 + bigZ * 1.011530

    Log.d(TAG, "convertCIEtoRGB() part 1: r = $r, g = $g, b = $b")

    // Apply reverse gamma correction
//    r = if (r <= 0.0031308) 12.92 * r else (1.0 + 0.055) * r.pow((1.0 / 2.4)) - 0.055
//    g = if (g <= 0.0031308) 12.92 * g else (1.0 + 0.055) * g.pow(((1.0 / 2.4))) - 0.055
//    b = if (b <= 0.0031308) 12.92 * b else (1.0 + 0.055) * b.pow(((1.0 / 2.4))) - 0.055
//    Log.d(TAG, "convertCIEtoRGB() part 2: r = $r, g = $g, b = $b")

    // convert to 0..255
    r *= 255.0
    g *= 255.0
    b *= 255.0

    Log.d(TAG, "convertCIEtoRGB() part 3: r = $r, g = $g, b = $b")

    val red = r.toInt()
    val green = g.toInt()
    val blue = b.toInt()
    Log.d(TAG, "convertCIEtoRGB() part 4: r = $red, g = $green, b = $blue")

    return Color(red, green, blue)
}


/**
 * This is some code I found on redit
 *
 * Triple returned is r, g, b
 */
fun xyToRgbWithBrightness(x: Float, y: Float, brightness: Int) : Triple<Int, Int, Int> {
    Log.d(TAG, "xyToRgbWithBrightness(): x = $x, y = $y, bri = $brightness")
//    val Y = brightness / 255f
//    val Y = brightness
    val Y = brightness / 100f
    val X =(Y / y) * x
    val Z =(Y / y) * (1f - x - y)
    Log.d(TAG, "xyToRgbWithBrightness(): X = $X, Y = $Y, Z = $Z")

    // wide d65 color space conversion coefficients
    var r = X * 1.656492f - Y * 0.354851f - Z * 0.255038f
    var g = -X * 0.707196f + Y * 1.655397f + Z * 0.036152f
    var b = X * 0.051713f - Y * 0.121364f + Z * 1.011530f
    Log.d(TAG, "xyToRgbWithBrightness(): part1   r = $r, g = $g, b = $b")

    // Adjust the values to be within the RGB color space
    //  -- but why?  Why do we have to constrain the range to [0..255]?
    r = constrain(r, 0.0f, 1.0f) * 255f
    g = constrain(g, 0.0f, 1.0f) * 255f
    b = constrain(b, 0.0f, 1.0f) * 255f
    Log.d(TAG, "xyToRgbWithBrightness(): part2   r = $r, g = $g, b = $b")

    return Triple(r.toInt(), g.toInt(), b.toInt())
}


/**
 * Finds the luminance of the given RGB values.  Uses Rec. 709 spec.
 */
//fun getRGBLuminance(r: Double, g: Double, b: Double) : Double {
//    return 0.2126 * r + 0.7152 * g + 0.0722 * b
//}



/** makes sure that n is in the given range */
fun constrain(n: Float, min: Float, max: Float) : Float {
    return when {
        n < min -> {
            min
        }
        n > max -> {
            max
        }
        else -> {
            n
        }
    }
}

//-------------------------------
//  extension functions
//-------------------------------

/**
 * Finds the dot product of two []DoubleArrays.  Note that the infix means
 * you can use it like this:
 *
 * val array1 = doubleArrayOf(3.0, 2.0, 1.0)
 * val array2 = doubleArrayOf(4.0, 2.0, -1.0)
 * val dotProduct = array1 dot array2
 *
 * Note: there's a specialized version of this designed just
 * for [MyPoint] as an operator function of that class.
 */
infix fun DoubleArray.dot(other: DoubleArray): Double {
    // insures that the arrays are the same length
    require(this.size == other.size)

    var out = 0.0
    for (i in indices) out += this[i] * other[i]
    return out
}


//-------------------------------
//  classes
//-------------------------------

data class MyPoint(
    var x: Double,
    var y: Double
) {
    /**
     * Returns the distance between this and another point
     */
    fun distance(p: MyPoint) : Double {
        return sqrt((x - p.x).pow(2.0) + pow(y - p.y, 2.0))
    }

    /**
     * Finds the dot product of two [MyPoint]s.
     */
    infix fun dot(other: MyPoint) : Double {
        // use our general dot product
        val array1 = doubleArrayOf(x, y)
        val array2 = doubleArrayOf(other.x, other.y)
        return array1 dot array2
    }

    companion object {
        /**
         * Alternate constructor.
         */
        operator fun invoke(pair: Pair<Double, Double>) : MyPoint {
            return MyPoint(pair.first, pair.second)
        }
    }

}

private const val TAG = "MathUtils"