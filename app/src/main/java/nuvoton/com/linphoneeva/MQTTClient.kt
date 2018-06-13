package nuvoton.com.linphoneeva

import android.content.Context
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

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
    val clientId = "cchsu20MQTT"
    val port = "19316"
    val sslPort = "29316"
    val webSocketPort = "39316"
    val serverUri = "tcp://m13.cloudmqtt.com:$port"
    val username = "frrsxmuk"
    val password = "2nU6Nmahng9g"
    val subscriptionTopic = "sensor/+"

    fun initClient(context: Context) {
        client = MqttAndroidClient(context, serverUri, clientId)
        val callback = client?.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                NuvotonLogger.debugMessage(TAG, "messageArrived=${message.toString()}")
            }

            override fun connectionLost(cause: Throwable?) {
                NuvotonLogger.debugMessage(TAG, "connectionLost=${cause.toString()}")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                NuvotonLogger.debugMessage(TAG, "deliveryComplete=${token.toString()}")
            }
        })

        connect()
    }

    fun setMqttCallback(callback: MqttCallbackExtended) {
        client?.setCallback(callback)
    }

    private fun subscribeToTopic() {
        try {
            client?.subscribe(subscriptionTopic, 0, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    NuvotonLogger.debugMessage(TAG, "subscribe onSuccess=${asyncActionToken.toString()}")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    NuvotonLogger.debugMessage(TAG, "subscribe onFailure=${asyncActionToken.toString()}")
                }
            })
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun connect() {
        val options = MqttConnectOptions()
        options.isAutomaticReconnect = true
        options.isCleanSession = false
        options.userName = username
        options.password = password.toCharArray()

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
}