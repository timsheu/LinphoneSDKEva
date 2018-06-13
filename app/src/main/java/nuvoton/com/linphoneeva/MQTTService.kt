package nuvoton.com.linphoneeva

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.support.v4.app.NotificationCompat

/**
 * Created by cchsu20 on 2018/6/11.
 */
class MQTTService : Service() {
    private val TAG = "MQTTService"

    override fun onCreate() {
        NuvotonLogger.debugMessage(TAG, "onCreate")
        startForeground(MQTTConstants.START_SERVICE.value, prepareNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val from = intent?.extras?.get("from").toString()
        NuvotonLogger.debugMessage(TAG, "start command from $from")
        return START_NOT_STICKY
    }

    override fun onBind(p0: Intent?): IBinder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun prepareNotification() : Notification {
        val channelId = "foreground_channel_id"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                notificationManager.getNotificationChannel(channelId) == null) {
            val name = "MQTT TEST Notification"
            val channel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_DEFAULT)
            channel.enableVibration(false)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.action = MQTTConstants.START_SERVICE.name

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }else {
            notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationCompat.Builder(this, channelId)
        }else {
            NotificationCompat.Builder(this)
        }

        notificationBuilder.setSmallIcon(R.drawable.ic_launcher_round)
                .setContentTitle("TEST MQTT title")
                .setContentText("TEST MQTT text")
                .setTicker("TEST MQTT Ticker")
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC)
        }
        return notificationBuilder.build()
    }
}