package nuvoton.com.linphoneeva

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.karumi.dexter.Dexter
import android.Manifest.permission;
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.support.v4.app.NotificationCompat
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.linphone.core.LinphoneCall
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() , LinphoneMiniListener.LinphoneMiniListenerInterface {
    private val TAG = "MainActivity"
    lateinit var linphoneMiniListener: LinphoneMiniListener
    var answerButton: Button? = null
    var declineButton: Button? = null
    var dialButton: Button? = null
    var holdButton: Button? = null
    var loginButton: Button? = null
    var settingButton: Button? = null
    var ipAddressText: EditText? = null

    var isHold = false
    val noti = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    lateinit var mediaPlayer: MediaPlayer
    var videoFragment: VideoCallFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        NuvotonLogger.DEBUG = true
        askPermissions()
        mediaPlayer = MediaPlayer.create(applicationContext, noti)
        answerButton = findViewById(R.id.accept)
        declineButton = findViewById(R.id.decline)
        dialButton = findViewById(R.id.dial)
        holdButton = findViewById(R.id.hold)
        loginButton = findViewById(R.id.login)
        settingButton = findViewById(R.id.setting)
        ipAddressText = findViewById(R.id.ip_address)
        SettingManager.shared.mContext = this

        initTextViews()

        linphoneMiniListener = LinphoneMiniListener(this)
        linphoneMiniListener.mInterface = this

        initButtons()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        val intent = Intent(this, MQTTService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        }else {
            startService(intent)
        }
    }

    override fun onDestroy() {
        if (ipAddressText != null) {
            val ip = ipAddressText!!.text.toString()
            SettingManager.shared.settingMap[Category.DeviceIpAddress.name] = ip
        }
        linphoneMiniListener.destroy()
        super.onDestroy()
    }

    override fun onPause() {
        SettingManager.shared.updateSettingsToPref()
        super.onPause()
    }

    override fun callAccepted() {
        bindVideoStream()
    }

    override fun callEnded() {
        resetVideoStream()
    }

    override fun linphoneCallState(state: LinphoneCall.State, message: String) {
        when (state) {
            LinphoneCall.State.IncomingReceived -> {
                runOnUiThread({ Toast.makeText(this, message, Toast.LENGTH_SHORT).show() })
                if (linphoneMiniListener.acceptEarlyMedia()) {
                    bindVideoStream()
                }
                playRingTone()
            }
            LinphoneCall.State.CallReleased -> {
                runOnUiThread(Runnable { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() })
                stopRingTone()
                linphoneMiniListener.isIncomingCall = false
                linphoneMiniListener.isOutgoingCall = false
            }
            LinphoneCall.State.Connected -> {
                runOnUiThread(Runnable { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() })
                if (!linphoneMiniListener.audioOnly) {
                    thread {
                        while (linphoneMiniListener.enableVideo() == -1){
                            Thread.sleep(50)
                        }
                        Thread.sleep(500)
                        runOnUiThread{
                            bindVideoStream()
                        }
                    }
                }
            }
            else -> {
                runOnUiThread(Runnable { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() })
                NuvotonLogger.debugMessage(TAG, "state: ${state.toString()} message: $message")
            }
        }
    }

    /*
    Use Dexter library to check permissions
     */
    private fun askPermissions() {
        Dexter.withActivity(this)
                .withPermissions(permission.RECORD_AUDIO,
                        permission.READ_CONTACTS,
                        permission.MODIFY_AUDIO_SETTINGS,
                        permission.CALL_PHONE,
                        permission.READ_PHONE_STATE,
                        permission.ACCESS_NETWORK_STATE,
                        permission.PROCESS_OUTGOING_CALLS,
                        permission.VIBRATE,
                        permission.BLUETOOTH,
                        permission.ACCESS_WIFI_STATE,
                        permission.CHANGE_WIFI_MULTICAST_STATE,
                        permission.RECEIVE_BOOT_COMPLETED,
                        permission.READ_PHONE_STATE
                ).withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report != null) {
                            for (granted in report.grantedPermissionResponses) {
                                NuvotonLogger.debugMessage(TAG, "granted permission: ${granted.permissionName}")
                            }

                            for (denied in report.deniedPermissionResponses) {
                                NuvotonLogger.debugMessage(TAG, "denied permission: ${denied.permissionName}")
                            }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token: PermissionToken?) {
                        if (permissions != null) {
                            for (permission in permissions) {
                                NuvotonLogger.debugMessage(TAG, "permission: $permission")
                            }
                        }
                    }
                }).check()
    }

    /*
    Bind video stream to video fragment
     */
    fun bindVideoStream() {
        if (videoFragment == null)
            videoFragment = VideoCallFragment()

        if (videoFragment != null) {
            videoFragment!!.mCore = linphoneMiniListener.mLinphoneCore
            if (!videoFragment!!.isAdded) {
                addFragment(videoFragment!!, R.id.video_view)
            }
        }
    }

    private fun resetVideoStream() {
        if (videoFragment != null && videoFragment!!.isAdded) {
            removeFragment(videoFragment!!)

        }
    }

    private fun playRingTone() {
        mediaPlayer.start()
    }

    private fun stopRingTone() {
        mediaPlayer.stop()
    }

    /**
     * init text views
     */

    private fun initTextViews() {
        val ip = SettingManager.shared.settingMap[Category.DeviceIpAddress.name] as String
        ipAddressText?.setText(ip)
        ipAddressText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                NuvotonLogger.debugMessage(TAG, "ipaddress: ${editable.toString()}")
                linphoneMiniListener.ipAddress = editable.toString()
                val key = Category.DeviceIpAddress.name
                val value = editable.toString()
                SettingManager.shared.settingMap[key] = value
                SettingManager.shared.updateSettingToPref(key, value)
            }

            override fun beforeTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

        })
    }

    private fun initButtons() {
        answerButton?.setOnClickListener {
            linphoneMiniListener.accept()
        }

        declineButton?.setOnClickListener {
            linphoneMiniListener.decline()
        }

        dialButton?.setOnClickListener {
            linphoneMiniListener.ipAddress = SettingManager.shared.settingMap[Category.DeviceIpAddress.name] as String
            linphoneMiniListener.dial()
        }

        holdButton?.setOnClickListener {
            linphoneMiniListener.holdAndResume(isHold)
            isHold = !isHold
            if (isHold) {
                holdButton?.text = "Resume"
            } else {
                holdButton?.text = "Hold"
            }
        }

        loginButton?.setOnClickListener {
            val username = SettingManager.shared.settingMap["LoginUsername"]
            val password = SettingManager.shared.settingMap["LoginPassword"]
            val domain = SettingManager.shared.settingMap["LoginDomain"]
            Log.d(TAG, "username=$username, password=$password, domain=$domain")
            linphoneMiniListener.login(username ?: "cchsu20", password ?: "48694062", domain ?: "sip.linphone.org")
        }

        settingButton?.setOnClickListener {
            SettingDialog.create(this)
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
