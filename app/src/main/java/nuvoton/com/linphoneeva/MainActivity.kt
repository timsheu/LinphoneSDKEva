package nuvoton.com.linphoneeva

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.karumi.dexter.Dexter
import android.Manifest.permission;
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.Surface
import android.view.SurfaceView
import android.widget.EditText
import android.widget.VideoView
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_main.*
import mu.KotlinLogging
import org.linphone.mediastream.video.display.GL2JNIView

private val logger = KotlinLogging.logger {}

class MainActivity : AppCompatActivity() , LinphoneMiniListener.LinphoneMiniListenerInterface{
    lateinit var linphoneMiniListener: LinphoneMiniListener
    var answerButton: Button? = null
    var declineButton: Button? = null
    var dialButton: Button? = null
    var holdButton: Button? = null
    var ipAddressText: EditText? = null
    var isHold = false
    val prefix = "sip:192.168.8."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        askPermissions()
        answerButton = findViewById(R.id.accept)
        declineButton = findViewById(R.id.decline)
        dialButton = findViewById(R.id.dial)
        holdButton = findViewById(R.id.hold)
        ipAddressText = findViewById(R.id.ip_address)
        ipAddressText?.addTextChangedListener( object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                linphoneMiniListener.ipAddress = prefix + editable.toString()
            }

            override fun beforeTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

        })
        linphoneMiniListener = LinphoneMiniListener(this)

        answerButton?.setOnClickListener {
            linphoneMiniListener.accept()
        }

        declineButton?.setOnClickListener {
            linphoneMiniListener.decline()
        }

        dialButton?.setOnClickListener {
            linphoneMiniListener.dial()
        }

        holdButton?.setOnClickListener {
            linphoneMiniListener.holdAndResume(isHold)
            isHold = !isHold
            if (isHold) {
                holdButton?.text = "Resume"
            }else {
                holdButton?.text = "Hold"
            }
        }
        linphoneMiniListener.mInterface = this
    }

    override fun onDestroy() {
        linphoneMiniListener.destroy()
        super.onDestroy()
    }

    fun askPermissions(){
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
                        if (report != null){
                            for (granted in report.grantedPermissionResponses){
                                logger.info { "granted permission: ${granted.permissionName}" }
                            }

                            for (denied in report.deniedPermissionResponses){
                                logger.info { "denied permission: ${denied.permissionName}" }
                            }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token: PermissionToken?) {
                        if (permissions != null) {
                            for (permission in permissions){
                                logger.info { "permission: $permission" }
                            }
                        }
                    }
                }).check()
    }

    override fun callAccepted() {
        val videoFragment = VideoCallFragment()
        videoFragment.mCore = linphoneMiniListener.mLinphoneCore
        addFragment(videoFragment, R.id.video_view)
    }
}
