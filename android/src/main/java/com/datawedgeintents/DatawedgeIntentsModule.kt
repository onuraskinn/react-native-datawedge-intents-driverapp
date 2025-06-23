package com.datawedgeintents

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeArray
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Observable
import java.util.Observer

class DatawedgeIntentsModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(
        reactContext
    ),
    Observer,
    LifecycleEventListener {

    override fun getName(): String {
        return "DatawedgeIntents"
    }

    init {
        reactContext.addLifecycleEventListener(this)
        Log.v(TAG, "Constructing React native DataWedge intents module")

        //  Register a broadcast receiver to return data back to the application
        ObservableObject.instance.addObserver(this)
    }

    //  The previously registered receiver (if any)
    private var registeredAction: String? = null
    private var registeredCategory: String? = null

    //  Broadcast receiver for the response to the Enumerate Scanner API
    //  THIS METHOD IS DEPRECATED, you should enumerate scanners as shown in https://github.com/darryncampbell/DataWedgeReactNative/blob/master/App.js
    var myEnumerateScannersBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.v(TAG, "Received Broadcast from DataWedge API - Enumerate Scanners")
            ObservableObject.instance.updateValue(intent)
        }
    }

    //  Broadcast receiver for the DataWedge intent being sent from Datawedge.
    //  Note: DW must be configured to send broadcast intents
    //  THIS METHOD IS DEPRECATED, you should enumerate scanners as shown in https://github.com/darryncampbell/DataWedgeReactNative/blob/master/App.js
    var scannedDataBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.v(TAG, "Received Broadcast from DataWedge API - Scanner")
            ObservableObject.instance.updateValue(intent)
        }
    }

    override fun onHostResume() {
        val filter = IntentFilter()
        filter.addAction(ACTION_ENUMERATEDLISET)
        handleReceiver(myEnumerateScannersBroadcastReceiver, filter)
        if (this.registeredAction != null) registerReceiver(
            this.registeredAction,
            this.registeredCategory
        )
    }

    override fun onHostPause() {
        unregisterReceivers()
    }

    override fun onHostDestroy() {
        Log.v(TAG, "Host Destroy")
    }

    override fun getConstants(): Map<String, Any> {
        val constants: MutableMap<String, Any> = HashMap()
        //  These are the constants available to the caller
        //  CONSTANTS HAVE BEEN DEPRECATED and will not stay current with the latest DW API
        constants["ACTION_SOFTSCANTRIGGER"] = ACTION_SOFTSCANTRIGGER
        constants["ACTION_SCANNERINPUTPLUGIN"] = ACTION_SCANNERINPUTPLUGIN
        constants["ACTION_ENUMERATESCANNERS"] = ACTION_ENUMERATESCANNERS
        constants["ACTION_SETDEFAULTPROFILE"] = ACTION_SETDEFAULTPROFILE
        constants["ACTION_RESETDEFAULTPROFILE"] = ACTION_RESETDEFAULTPROFILE
        constants["ACTION_SWITCHTOPROFILE"] = ACTION_SWITCHTOPROFILE
        constants["START_SCANNING"] = START_SCANNING
        constants["STOP_SCANNING"] = STOP_SCANNING
        constants["TOGGLE_SCANNING"] = TOGGLE_SCANNING
        constants["ENABLE_PLUGIN"] = ENABLE_PLUGIN
        constants["DISABLE_PLUGIN"] = DISABLE_PLUGIN
        return constants
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun handleReceiver(broadcast: BroadcastReceiver, filter: IntentFilter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            reactContext.registerReceiver(
                broadcast, filter,
                Context.RECEIVER_EXPORTED
            )
        } else {
            reactContext.registerReceiver(broadcast, filter)
        }
    }

    @ReactMethod
    fun sendIntent(action: String, parameterValue: String?) {
        //  THIS METHOD IS DEPRECATED, use SendBroadcastWithExtras
        Log.v(
            TAG,
            "Sending Intent with action: $action, parameter: [$parameterValue]"
        )
        //  Some DW API calls use a different paramter name, abstract this from the caller.
        var parameterKey = EXTRA_PARAMETER
        if (action.equals(ACTION_SETDEFAULTPROFILE, ignoreCase = true) ||
            action.equals(ACTION_RESETDEFAULTPROFILE, ignoreCase = true) ||
            action.equals(ACTION_SWITCHTOPROFILE, ignoreCase = true)
        ) parameterKey = EXTRA_PROFILENAME

        val dwIntent = Intent()
        dwIntent.setAction(action)
        if (parameterValue != null && parameterValue.length > 0) dwIntent.putExtra(
            parameterKey,
            parameterValue
        )
        reactContext.sendBroadcast(dwIntent)
    }

    @ReactMethod
    @Throws(JSONException::class)
    fun sendBroadcastWithExtras(obj: ReadableMap) {
        //  Implementation note: Whilst this function will probably be able to deconstruct many ReadableMap objects
        //  (originally JSON objects) to intents, no effort has been made to make this function generic beyond
        //  support for the DataWedge API.
        val action = if (obj.hasKey("action")) obj.getString("action") else null
        val i = Intent()
        if (action != null) i.setAction(action)

        val intentMap = recursivelyDeconstructReadableMap(obj)
        var extrasMap: Map<String?, Any?>? = null
        if (intentMap.containsKey("extras") && intentMap["extras"] != null &&
            intentMap["extras"] is Map<*, *>
        ) extrasMap = intentMap["extras"] as Map<String?, Any?>?

        for (key in extrasMap!!.keys) {
            val value = extrasMap[key]
            val valueStr = value.toString()
            // If type is text html, the extra text must sent as HTML
            if (value is Boolean) {
                i.putExtra(key, valueStr.toBoolean())
            } else if (value is Int) {
                i.putExtra(key, valueStr.toInt())
            } else if (value is Long) {
                i.putExtra(key, valueStr.toLong())
            } else if (value is Double) {
                i.putExtra(key, valueStr.toDouble())
            } else if (valueStr.startsWith("{")) {
                //  UI has passed a JSON object
                val bundle = toBundle(JSONObject(valueStr))
                i.putExtra(key, bundle)
            } else {
                i.putExtra(key, valueStr)
            }
        }
        reactContext.sendBroadcast(i)
    }

    @ReactMethod
    fun registerReceiver(action: String?, category: String?) {
        //  THIS METHOD IS DEPRECATED, use registerBroadcastReceiver
        Log.d(
            TAG,
            "Registering an Intent filter for action: $action"
        )
        this.registeredAction = action
        this.registeredCategory = category
        //  User has specified the intent action and category that DataWedge will be reporting
        unregisterReceiver(scannedDataBroadcastReceiver)
        val filter = IntentFilter()
        filter.addAction(action)
        if (category != null && category.length > 0) filter.addCategory(category)
        handleReceiver(scannedDataBroadcastReceiver, filter)
    }

    @ReactMethod
    fun registerBroadcastReceiver(filterObj: ReadableMap) {
        unregisterReceiver(genericReceiver)
        val filter = IntentFilter()
        if (filterObj.hasKey("filterActions")) {
            val type = filterObj.getType("filterActions")
            if (type == ReadableType.Array) {
                val actionsArray = filterObj.getArray("filterActions")
                for (i in 0 until actionsArray!!.size()) {
                    filter.addAction(actionsArray.getString(i))
                }
            }
        }
        if (filterObj.hasKey("filterCategories")) {
            val type = filterObj.getType("filterCategories")
            if (type == ReadableType.Array) {
                val categoriesArray = filterObj.getArray("filterCategories")
                for (i in 0 until categoriesArray!!.size()) {
                    filter.addCategory(categoriesArray.getString(i))
                }
            }
        }
        handleReceiver(genericReceiver, filter)
    }

    //  Credit: https://github.com/facebook/react-native/issues/4655
    private fun recursivelyDeconstructReadableMap(readableMap: ReadableMap?): Map<String, Any?> {
        val iterator = readableMap!!.keySetIterator()
        val deconstructedMap: MutableMap<String, Any?> = HashMap()
        while (iterator.hasNextKey()) {
            val key = iterator.nextKey()
            val type = readableMap.getType(key)
            when (type) {
                ReadableType.Null -> deconstructedMap[key] = null
                ReadableType.Boolean -> deconstructedMap[key] = readableMap.getBoolean(key)
                ReadableType.Number -> deconstructedMap[key] = readableMap.getDouble(key)
                ReadableType.String -> deconstructedMap[key] = readableMap.getString(key)
                ReadableType.Map -> deconstructedMap[key] = recursivelyDeconstructReadableMap(
                    readableMap.getMap(key)
                )

                ReadableType.Array -> deconstructedMap[key] = recursivelyDeconstructReadableArray(
                    readableMap.getArray(key)
                )

                else -> throw IllegalArgumentException("Could not convert object with key: $key.")
            }
        }
        return deconstructedMap
    }

    //  Credit: https://github.com/facebook/react-native/issues/4655
    private fun recursivelyDeconstructReadableArray(readableArray: ReadableArray?): List<Any?> {
        val deconstructedList = mutableListOf<Any?>()
        if (readableArray == null) {
            return deconstructedList
        }
        for (i in 0 until readableArray.size()) {
            val indexType = readableArray.getType(i)
            when (indexType) {
                ReadableType.Null -> deconstructedList.add(i, null)
                ReadableType.Boolean -> deconstructedList.add(i, readableArray.getBoolean(i))
                ReadableType.Number -> deconstructedList.add(i, readableArray.getDouble(i))
                ReadableType.String -> deconstructedList.add(i, readableArray.getString(i))
                ReadableType.Map -> deconstructedList.add(
                    i, recursivelyDeconstructReadableMap(
                        readableArray.getMap(i)
                    )
                )

                ReadableType.Array -> deconstructedList.add(
                    i, recursivelyDeconstructReadableArray(
                        readableArray.getArray(i)
                    )
                )

                else -> throw IllegalArgumentException("Could not convert object at index $i.")
            }
        }
        return deconstructedList
    }

    //  https://github.com/darryncampbell/darryncampbell-cordova-plugin-intent/blob/master/src/android/IntentShim.java
    private fun toBundle(obj: JSONObject?): Bundle? {
        val returnBundle = Bundle()
        if (obj == null) {
            return null
        }
        try {
            val keys: Iterator<*> = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next() as String
                val compare = obj[key]
                if (obj[key] is String) returnBundle.putString(key, obj.getString(key))
                else if (key.equals(
                        "keystroke_output_enabled",
                        ignoreCase = true
                    )
                ) returnBundle.putString(
                    key,
                    obj.getString(key)
                )
                else if (obj[key] is Boolean) returnBundle.putBoolean(key, obj.getBoolean(key))
                else if (obj[key] is Int) returnBundle.putInt(key, obj.getInt(key))
                else if (obj[key] is Long) returnBundle.putLong(key, obj.getLong(key))
                else if (obj[key] is Double) returnBundle.putDouble(key, obj.getDouble(key))
                else if (obj[key].javaClass.isArray || obj[key] is JSONArray) {
                    val jsonArray = obj.getJSONArray(key)
                    val length = jsonArray.length()
                    if (jsonArray[0] is String) {
                        val stringArray = arrayOfNulls<String>(length)
                        for (j in 0 until length) stringArray[j] = jsonArray.getString(j)
                        returnBundle.putStringArray(key, stringArray)
                        //returnBundle.putParcelableArray(key, obj.get);
                    } else if (jsonArray[0] is Double) {
                        val intArray = IntArray(length)
                        for (j in 0 until length) intArray[j] = jsonArray.getInt(j)
                        returnBundle.putIntArray(key, intArray)
                    } else {
                        val bundleArray = arrayOfNulls<Bundle>(length)
                        for (k in 0 until length) bundleArray[k] =
                            toBundle(jsonArray.getJSONObject(k))
                        returnBundle.putParcelableArray(key, bundleArray)
                    }
                } else if (obj[key] is JSONObject) returnBundle.putBundle(
                    key,
                    toBundle(obj[key] as JSONObject)
                )
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return returnBundle
    }

    private fun unregisterReceivers() {
        unregisterReceiver(myEnumerateScannersBroadcastReceiver)
        unregisterReceiver(scannedDataBroadcastReceiver)
    }

    private fun unregisterReceiver(receiver: BroadcastReceiver) {
        try {
            reactContext.unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            //  Expected behaviour if there was not a previously registered receiver.
        }
    }

    //  Sending events to JavaScript as defined in the native-modules documentation.
    //  Note: Callbacks can only be invoked a single time so are not a suitable interface for barcode scans.
    private fun sendEvent(
        reactContext: ReactContext,
        eventName: String,
        params: WritableMap
    ) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    //  Credit: http://stackoverflow.com/questions/28083430/communication-between-broadcastreceiver-and-activity-android#30964385
    override fun update(observable: Observable, data: Any) {
        val intent = data as Intent

        if (intent.hasExtra("v2API")) {
            val intentBundle = intent.extras

            // Remove arrays (fb converter cannot cope with byte arrays)
            for (key in ArrayList(intentBundle!!.keySet())) {
                val extraValue = intentBundle[key]
                if (extraValue is ByteArray || extraValue is ArrayList<*> || extraValue is ArrayList<*>) {
                    intentBundle.remove(key)
                }
            }

            val map = Arguments.fromBundle(intentBundle)
            sendEvent(this.reactContext, "datawedge_broadcast_intent", map)
        }

        val action = intent.action
        if (action == ACTION_ENUMERATEDLISET) {
            val b = intent.extras
            val scanner_list = b!!.getStringArray(KEY_ENUMERATEDSCANNERLIST)
            val userFriendlyScanners: WritableArray = WritableNativeArray()
            for (i in scanner_list!!.indices) {
                userFriendlyScanners.pushString(scanner_list[i])
            }
            try {
                val enumeratedScannersObj: WritableMap = WritableNativeMap()
                enumeratedScannersObj.putArray("Scanners", userFriendlyScanners)
                sendEvent(this.reactContext, "enumerated_scanners", enumeratedScannersObj)
            } catch (e: Exception) {
                Toast.makeText(this.reactContext, "Error returning scanners", Toast.LENGTH_LONG)
                    .show()
                e.printStackTrace()
            }
        } else {
            //  Intent from the scanner (barcode has been scanned)
            val decodedSource = intent.getStringExtra(RECEIVED_SCAN_SOURCE)
            val decodedData = intent.getStringExtra(RECEIVED_SCAN_DATA)
            val decodedLabelType = intent.getStringExtra(RECEIVED_SCAN_TYPE)

            val scanData: WritableMap = WritableNativeMap()
            scanData.putString("source", decodedSource)
            scanData.putString("data", decodedData)
            scanData.putString("labelType", decodedLabelType)
            sendEvent(this.reactContext, "barcode_scan", scanData)
        }
    }

    companion object {
        private val TAG: String = DatawedgeIntentsModule::class.java.simpleName

        //  THESE ACTIONS ARE DEPRECATED, PLEASE SPECIFY THE ACTION AS PART OF THE CALL TO sendBroadcastWithExtras
        private const val ACTION_SOFTSCANTRIGGER = "com.symbol.datawedge.api.ACTION_SOFTSCANTRIGGER"
        private const val ACTION_SCANNERINPUTPLUGIN =
            "com.symbol.datawedge.api.ACTION_SCANNERINPUTPLUGIN"
        private const val ACTION_ENUMERATESCANNERS =
            "com.symbol.datawedge.api.ACTION_ENUMERATESCANNERS"
        private const val ACTION_SETDEFAULTPROFILE =
            "com.symbol.datawedge.api.ACTION_SETDEFAULTPROFILE"
        private const val ACTION_RESETDEFAULTPROFILE =
            "com.symbol.datawedge.api.ACTION_RESETDEFAULTPROFILE"
        private const val ACTION_SWITCHTOPROFILE = "com.symbol.datawedge.api.ACTION_SWITCHTOPROFILE"
        private const val EXTRA_PARAMETER = "com.symbol.datawedge.api.EXTRA_PARAMETER"
        private const val EXTRA_PROFILENAME = "com.symbol.datawedge.api.EXTRA_PROFILENAME"

        //  Intent extra parameters
        private const val START_SCANNING = "START_SCANNING"
        private const val STOP_SCANNING = "STOP_SCANNING"
        private const val TOGGLE_SCANNING = "TOGGLE_SCANNING"
        private const val ENABLE_PLUGIN = "ENABLE_PLUGIN"
        private const val DISABLE_PLUGIN = "DISABLE_PLUGIN"

        //  Enumerated Scanner receiver
        private const val ACTION_ENUMERATEDLISET =
            "com.symbol.datawedge.api.ACTION_ENUMERATEDSCANNERLIST"
        private const val KEY_ENUMERATEDSCANNERLIST = "DWAPI_KEY_ENUMERATEDSCANNERLIST"

        //  END DEPRECATED PROPERTIES
        //  Scan data receiver - These strings are only used by registerReceiver, a deprecated method
        private const val RECEIVED_SCAN_SOURCE = "com.symbol.datawedge.source"
        private const val RECEIVED_SCAN_DATA = "com.symbol.datawedge.data_string"
        private const val RECEIVED_SCAN_TYPE = "com.symbol.datawedge.label_type"
        var genericReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.v(TAG, "Received Broadcast from DataWedge")
                intent.putExtra("v2API", true)
                ObservableObject.instance.updateValue(intent)
            }
        }
    }
}
