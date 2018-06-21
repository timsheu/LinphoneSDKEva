package nuvoton.com.linphoneeva

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.karumi.dexter.Dexter
import android.Manifest.permission;
import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.support.v4.app.NotificationCompat
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
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
        ipAddressText = findViewById(R.id.ip_address)
        SettingManager.shared.mContext = this

        initTextViews()

        linphoneMiniListener = LinphoneMiniListener(this)
        linphoneMiniListener.mInterface = this

        initButtons()
        initMqttClient()

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
            SettingManager.shared.updateIPAddress(ip)
        }
        linphoneMiniListener.destroy()
        super.onDestroy()
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
                        permission.RECEIVE_BOOT_COMPLETED
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
        val ip = SettingManager.shared.readIPAddress()
        ipAddressText?.setText(ip)
        ipAddressText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                NuvotonLogger.debugMessage(TAG, "ipaddress: ${editable.toString()}")
                linphoneMiniListener.ipAddress = editable.toString()
                val result = SettingManager.shared.updateIPAddress(editable.toString())
                NuvotonLogger.debugMessage(TAG, "update result: $result")
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
            linphoneMiniListener.ipAddress = SettingManager.shared.readIPAddress()
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
            linphoneMiniListener.login("timsheu", "48694062", "sip.linphone.org")
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
