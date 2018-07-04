package nuvoton.com.linphoneeva

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage

/**
 * Created by cchsu20 on 2018/6/11.
 */
class MQTTService : Service() {
    private val TAG = "MQTTService"
    private var count = 0
    private var isStarted = false

    inner class MQTTServiceBinder: Binder() {
        fun getService(): MQTTService? {
            return this@MQTTService
        }
    }

    override fun onCreate() {
        NuvotonLogger.context = this
        NuvotonLogger.debugMessage(TAG, "onCreate")
        showNotification("Background service started")
        NuvotonLogger.debugMessageInPref("$TAG, onCreate", "onCreate")
        initMqttClient()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val from = intent?.extras?.get("from").toString()
        val message = "start command from $from"
        NuvotonLogger.debugMessage(TAG, message)
        NuvotonLogger.context = this
        NuvotonLogger.debugMessageInPref("$TAG, from", message)
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder {
        return MQTTServiceBinder()
    }

    private fun showNotification(message: String) {
        NuvotonLogger.debugMessageInPref("$TAG, prepareNoti", "showNotification")
        val channelId = "foreground_channel_id"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                notificationManager.getNotificationChannel(channelId) == null) {
            val name = "MQTT TEST Notification"
            val channel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_DEFAULT)
            channel.enableVibration(true)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.action = Intent.ACTION_MAIN
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
        }else {
            notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationCompat.Builder(this, channelId)
        }else {
            NotificationCompat.Builder(this)
        }

        notificationBuilder
                .setSmallIcon(R.drawable.ic_launcher_round)
                .setContentTitle("MQTT Message")
                .setContentText(message)
                .setTicker(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC)
        }

        if (isStarted) {
            notificationManager.notify(count++, notificationBuilder.build())
        }else{
            isStarted = true
            startForeground(count++, notificationBuilder.build())
        }

    }

    private fun initMqttClient() {
        val client = MQTTClient.shared
        client.initClient(applicationContext)
        client.setMqttCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                NuvotonLogger.debugMessage(TAG, "connectComplete, reconnect=$reconnect, serverURI=$serverURI")
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                NuvotonLogger.debugMessage(TAG, "messageArrived, topic=$topic, message=${message.toString()}")
                showNotification(message.toString())
            }

            override fun connectionLost(cause: Throwable?) {
                NuvotonLogger.debugMessage(TAG, "connectionLost, cause=${cause.toString()}")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                NuvotonLogger.debugMessage(TAG, "deliveryComplete, token=${token.toString()}")
            }
        })
    }
}