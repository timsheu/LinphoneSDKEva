package nuvoton.com.linphoneeva

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.karumi.dexter.Dexter
import android.Manifest.permission;
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.Surface
import android.view.SurfaceView
import android.widget.EditText
import android.widget.Toast
import android.widget.VideoView
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_main.*
import mu.KotlinLogging
import org.linphone.core.LinphoneCall
import org.linphone.mediastream.video.display.GL2JNIView
import kotlin.concurrent.thread
import kotlin.concurrent.schedule
import java.util.Timer

private val logger = KotlinLogging.logger {}

class MainActivity : AppCompatActivity() , LinphoneMiniListener.LinphoneMiniListenerInterface {
    lateinit var linphoneMiniListener: LinphoneMiniListener
    var answerButton: Button? = null
    var declineButton: Button? = null
    var dialButton: Button? = null
    var holdButton: Button? = null
    var ipAddressText: EditText? = null
    var isHold = false
    val noti = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        askPermissions()
        mediaPlayer = MediaPlayer.create(applicationContext, noti)
        answerButton = findViewById(R.id.accept)
        declineButton = findViewById(R.id.decline)
        dialButton = findViewById(R.id.dial)
        holdButton = findViewById(R.id.hold)
        ipAddressText = findViewById(R.id.ip_address)
        SettingManager.shared.mContext = this
        val ip = SettingManager.shared.readIPAddress()
        ipAddressText?.setText(ip)
        ipAddressText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                logger.info { "ipaddress: ${editable.toString()}" }
                linphoneMiniListener.ipAddress = editable.toString()
                val result = SettingManager.shared.updateIPAddress(editable.toString())
                logger.info { "update result: $result" }
            }

            override fun beforeTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

        })
        linphoneMiniListener = LinphoneMiniListener(this)
        linphoneMiniListener.mInterface = this

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

    override fun linphoneCallState(state: LinphoneCall.State, message: String) {
        when (state) {
            LinphoneCall.State.IncomingReceived -> {
                runOnUiThread(Runnable { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() })
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
            else -> {
                runOnUiThread(Runnable { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() })
                logger.info { "state: ${state.toString()} message: $message" }
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
                        permission.CHANGE_WIFI_MULTICAST_STATE
                ).withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report != null) {
                            for (granted in report.grantedPermissionResponses) {
                                logger.info { "granted permission: ${granted.permissionName}" }
                            }

                            for (denied in report.deniedPermissionResponses) {
                                logger.info { "denied permission: ${denied.permissionName}" }
                            }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token: PermissionToken?) {
                        if (permissions != null) {
                            for (permission in permissions) {
                                logger.info { "permission: $permission" }
                            }
                        }
                    }
                }).check()
    }

    /*
    Bind video stream to video fragment
     */
    fun bindVideoStream() {
        val videoFragment = VideoCallFragment()
        videoFragment.mCore = linphoneMiniListener.mLinphoneCore
        addFragment(videoFragment, R.id.video_view)
    }

    private fun playRingTone() {
        mediaPlayer.start()
    }

    private fun stopRingTone() {
        mediaPlayer.stop()
    }

}
