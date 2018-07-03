package nuvoton.com.linphoneeva

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import android.hardware.usb.UsbDevice.getDeviceId
import android.os.Build
import android.support.v4.app.ActivityCompat
import android.telephony.TelephonyManager


/**
 * Created by cchsu20 on 2018/6/6.
 */
class MQTTClient {
    private val TAG = "MQTTClient"
    private object Holder { val INSTANCE = MQTTClient() }
    companion object {
        val shared: MQTTClient by lazy { Holder.INSTANCE }
    }
    var client: MqttAndroidClient? = null
    val port = "1883"
    val serverUri = "tcp://rak.adacomm.io:$port"
    var topicControl = ""
    var topicStatus = ""
    var productId = "36AM17"
    var deviceId = ""
    set(value) {
        topicControl = "$productId/$value/control"
        topicStatus = "$productId/$value/status"
    }

    val content = "LIVEVIEW"

    fun initClient(context: Context) {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val isPermitted = ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
        if (isPermitted == PackageManager.PERMISSION_GRANTED) {
            deviceId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telephonyManager.imei
            } else {
                telephonyManager.deviceId
            }
        }
        NuvotonLogger.debugMessage(TAG, "imei=$deviceId")
        client = MqttAndroidClient(context, serverUri, deviceId)
        connect()
    }

    fun setMqttCallback(callback: MqttCallbackExtended) {
        client?.setCallback(callback)
    }

    private fun subscribeToTopic() {
        try {
            client?.subscribe(topicStatus, 0, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    NuvotonLogger.debugMessage(TAG, "$topicStatus subscribe onSuccess=${asyncActionToken.toString()}")
                    publish(MQTTClient.shared.topicStatus)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    NuvotonLogger.debugMessage(TAG, "$topicStatus subscribe onFailure=${asyncActionToken.toString()}")
                }
            })

            client?.subscribe(topicControl, 0, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    NuvotonLogger.debugMessage(TAG, "$topicControl subscribe onSuccess=${asyncActionToken.toString()}")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    NuvotonLogger.debugMessage(TAG, "$topicControl subscribe onFailure=${asyncActionToken.toString()}")
                }
            })
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun connect() {
        val options = MqttConnectOptions()
        options.isAutomaticReconnect = true
        // If isCleanSession is set to false, the connecting process to RAK server will fail
//        options.isCleanSession = false
//        options.userName = username
//        options.password = password.toCharArray()

        try {
            client?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    NuvotonLogger.debugMessage(TAG, "connect onSuccess=${asyncActionToken.toString()}")
                    val disconnectOptions = DisconnectedBufferOptions()
                    disconnectOptions.isBufferEnabled = true
                    disconnectOptions.bufferSize = 100
                    disconnectOptions.isPersistBuffer = false
                    disconnectOptions.isDeleteOldestMessages = false
                    client?.setBufferOpts(disconnectOptions)
                    subscribeToTopic()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    NuvotonLogger.debugMessage(TAG, "connect onFailure=${asyncActionToken.toString()}")
                }
            })
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun publish(message: String) {
        NuvotonLogger.debugMessage(TAG, message)
        client?.publish(message, MqttMessage("test".toByteArray()))
    }
}