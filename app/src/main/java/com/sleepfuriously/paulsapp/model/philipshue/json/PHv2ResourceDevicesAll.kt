package com.sleepfuriously.paulsapp.model.philipshue.json

import org.json.JSONObject

/**
 * All the devices on the current bridge.  Yep, lots of stuff here.
 * You've been warned.
 *
 * Results from calling
 *      https://<bridge_id>/clip/v2/resource/device
 */
data class PHv2ResourceDevicesAll(
    val errors: List<PHv2Error> = listOf(),
    /** Holds info about this device.  Typically holds just one item */
    val data: List<PHv2Device> = listOf()
) {
    companion object {
        /**
         * Alternate constructor.  Takes the actual json string that's returned
         * from calling
         *      https://<ip>/clip/v2/resource/device
         *
         * @param   jsonObject  The json object that represents this class.
         *                      Unlike the other class's alternate constructors,
         *                      this IS the json, not its parent!!!
         *
         * @return  A [PHv2ResourceDevicesAll] instance.  Could be just two empty
         *          lists if the appropriate data can't be found.
         */
        operator fun invoke(jsonObject: JSONObject) : PHv2ResourceDevicesAll {
            val errors = mutableListOf<PHv2Error>()
            val data = mutableListOf<PHv2Device>()

            if (jsonObject.has(ERRORS)) {
                val errorsJsonArray = jsonObject.getJSONArray(ERRORS)
                for (i in 0 until errorsJsonArray.length()) {
                    errors.add(PHv2Error(errorsJsonArray.getJSONObject(i)))
                }
            }

            if (jsonObject.has(DATA)) {
                val dataJsonArray = jsonObject.getJSONArray(DATA)
                for (i in 0 until dataJsonArray.length()) {
                    data.add(PHv2Device(dataJsonArray.getJSONObject(i)))
                }
            }

            return PHv2ResourceDevicesAll(errors, data)
        }

        /**
         * Another constructor.  This one takes a String representation of the
         * json object (not the parent!).
         */
        operator fun invoke(jsonObjectStr: String) : PHv2ResourceDevicesAll {
            return PHv2ResourceDevicesAll(JSONObject(jsonObjectStr))
        }
    }
}

/**
 * Class representing the data returned from:
 *      https://<bridge_ip>/clip/v2/resource/device/<device_id>
 */
data class PHv2ResourceDeviceIndividual(
    val errors: List<PHv2Error> = listOf(),
    val data: List<PHv2Device> = listOf()
) {
    companion object {
        /**
         * Alternate constructor.  Takes the actual json string that's returned
         * from calling
         *      https://<bridge_ip>/clip/v2/resource/device/<device_id>
         *
         * @param   jsonObject  The json object that represents this class.
         *                      Unlike the other class's alternate constructors,
         *                      this IS the json, not its parent!!!
         *
         * @return  A [PHv2ResourceDeviceIndividual] instance.  Could be just two empty
         *          lists if the appropriate data can't be found.
         */
        operator fun invoke(jsonObject: JSONObject) : PHv2ResourceDeviceIndividual {
            val errors = mutableListOf<PHv2Error>()
            val data = mutableListOf<PHv2Device>()

            if (jsonObject.has(ERRORS)) {
                val errorsJsonArray = jsonObject.getJSONArray(ERRORS)
                for (i in 0 until errorsJsonArray.length()) {
                    errors.add(PHv2Error(errorsJsonArray.getJSONObject(i)))
                }
            }

            if (jsonObject.has(DATA)) {
                val dataJsonArray = jsonObject.getJSONArray(DATA)
                for (i in 0 until dataJsonArray.length()) {
                    data.add(PHv2Device(dataJsonArray.getJSONObject(i)))
                }
            }

            return PHv2ResourceDeviceIndividual(errors, data)
        }

        /**
         * Another constructor.  This one takes a String representation of the
         * json object (not the parent!).
         */
        operator fun invoke(jsonObjectStr: String) : PHv2ResourceDeviceIndividual {
            return PHv2ResourceDeviceIndividual(JSONObject(jsonObjectStr))
        }
    }
}

/**
 * Description of each of the devices within the data
 * list of [PHv2ResourceDevicesAll].
 */
data class PHv2Device(
    /** should be "device" */
    val type: String,
    val id: String,
    val idV1: String = EMPTY_STRING,
    val productData: PHv2DeviceProductData,
    val metadata: PHv2DeviceMetadata,
//    val usertest: PHv2DeviceUsertest,     todo if necessary
    val deviceMode: PHv2DeviceMode,
    val services: List<PHv2ResourceIdentifier>
) {
    companion object {
        /**
         * Alternate constructor.
         *
         * @param   jsonObject  Data respresenting the [PHv2Device],
         *                      NOT the parent!!!  This is often from a
         *                      json array, so the parent doesn't make sense.
         */
        operator fun invoke(jsonObject: JSONObject) : PHv2Device {
            // create the service list
            val serviceList = mutableListOf<PHv2ResourceIdentifier>()
            if (jsonObject.has(SERVICES)) {
                val serviceJsonArray = jsonObject.getJSONArray(SERVICES)
                for (i in 0 until serviceJsonArray.length()) {
                    serviceList.add(PHv2ResourceIdentifier(serviceJsonArray.getJSONObject(i)))
                }
            }

            return PHv2Device(
                type = jsonObject.optString(TYPE),
                id = jsonObject.getString(ID),
                idV1 = jsonObject.optString(ID_V1),
                productData = PHv2DeviceProductData(jsonObject),
                metadata = PHv2DeviceMetadata(jsonObject),
                deviceMode = PHv2DeviceMode(jsonObject),
                services = serviceList
            )
        }

        /**
         * Alternate constructor
         *
         * @param   jsonStr     A string representation of the json that is
         *                      this actual device (NOT the parent!).
         */
        operator fun invoke(jsonStr: String) : PHv2Device {
            val jsonObject = JSONObject(jsonStr)
            return PHv2Device(jsonObject)
        }
    }
}

data class PHv2DeviceProductData(
    val modelId: String,
    val manufacturerName: String,
    val productName: String,
    val productArchetype: String,
    val certified: Boolean,
    val softwareVersion: String,
    val hardwarePlatformType: String
) {
    companion object {
        /**
         * Alternate Constructor
         *
         * @param   parentJsonObject    The json object that is the
         *                              parent.  It is assumed that
         *                              the parent DOES include this object!
         */
        operator fun invoke(parentJsonObject: JSONObject) : PHv2DeviceProductData {
            val productDataJsonObject = parentJsonObject.getJSONObject(PRODUCT_DATA)

            return PHv2DeviceProductData(
                modelId = productDataJsonObject.getString(MODEL_ID),
                manufacturerName = productDataJsonObject.getString(MANUFACTURER_NAME),
                productName = productDataJsonObject.getString(PRODUCT_NAME),
                productArchetype = productDataJsonObject.getString(PRODUCT_ARCHETYPE),
                certified = productDataJsonObject.getBoolean(CERTIFIED),
                softwareVersion = productDataJsonObject.getString(SOFTWARE_VERSION),
                hardwarePlatformType = productDataJsonObject.getString(HARDWARE_PLATFORM_TYPE)
            )
        }
    }
}

data class PHv2DeviceMetadata(
    /** human readable name for this resource */
    val name: String,
    val archetype: String
) {
    companion object {
        /**
         * Alternate constructor.
         *
         * Takes the [JSONObject] of the PARENT as input.
         */
        operator fun invoke(parentJsonObject: JSONObject) : PHv2DeviceMetadata {
            if (parentJsonObject.has(METADATA)) {
                val metadataJsonObject = parentJsonObject.getJSONObject(METADATA)
                val name = metadataJsonObject.getString(NAME)
                val archetype = metadataJsonObject.getString(ARCHETYPE)

                return PHv2DeviceMetadata(name = name, archetype = archetype)
            }
            else {
                return PHv2DeviceMetadata(EMPTY_STRING, EMPTY_STRING)
            }
        }
    }
}

data class PHv2DeviceMode(
    val status: String,
    val mode: String,
    val modeValues: List<String>
) {
    companion object {
        operator fun invoke(parentJsonObject: JSONObject) : PHv2DeviceMode {
            if (parentJsonObject.has(DEVICE_MODE)) {
                val deviceJsonObject = parentJsonObject.getJSONObject(DEVICE_MODE)
                val status = deviceJsonObject.getString(STATUS)
                val mode = deviceJsonObject.getString(MODE)

                val modeList = mutableListOf<String>()
                val modeJsonArray = deviceJsonObject.getJSONArray(MODE_VALUES)
                for (i in 0 until modeJsonArray.length()) {
                    modeList.add(modeJsonArray.getString(i))
                }

                return PHv2DeviceMode(
                    status = status,
                    mode = mode,
                    modeValues = modeList
                )
            }
            else {
                return PHv2DeviceMode(
                    status = EMPTY_STRING,
                    mode = EMPTY_STRING,
                    modeValues = listOf()
                )
            }
        }
    }
}