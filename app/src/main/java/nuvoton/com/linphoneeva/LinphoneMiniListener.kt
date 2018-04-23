package nuvoton.com.linphoneeva

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.util.DisplayMetrics
import android.view.SurfaceView
import mu.KotlinLogging
import org.linphone.core.*
import java.io.IOException;
import java.util.Timer;

import org.linphone.core.LinphoneCall.State;
import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration;
import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration.AndroidCamera;
import kotlin.concurrent.timerTask
/**
 * Created by cchsu20 on 14/03/2018.
 */
private val logger = KotlinLogging.logger {}

class LinphoneMiniListener{

    interface LinphoneMiniListenerInterface {
        fun callAccepted()
        fun linphoneCallState(state: State, message: String)
    }

    private var mListener: LinphoneCoreListener = object : LinphoneCoreListenerBase() {
        override fun callState(lc: LinphoneCore?, call: LinphoneCall?, state: State?, message: String?) {
            mCall = call
            mInterface?.linphoneCallState(state ?: State.CallReleased, message ?: "No available call")
        }
    }

    companion object {
        lateinit var mInstance: LinphoneMiniListener
    }
    var mInterface: LinphoneMiniListenerInterface? = null
    var isIncomingCall = false
    var isOutgoingCall = false
    private var mContext: Context
    private var mCall: LinphoneCall? = null
    lateinit var mLinphoneCore: LinphoneCore
    private lateinit var mTimer: Timer
    lateinit var mAudioManager: AudioManager
    lateinit var videoView: SurfaceView
    lateinit var videoCaptureView: SurfaceView
    var ipAddress = "sip:192.168.8."

    init {

    }

    constructor(context: Context){
        this.mContext = context
        LinphoneCoreFactory.instance().setDebugMode(false, "Linphone Mini")

        try {
            val basePath = mContext.filesDir.absolutePath
            copyAssetsFromPackage(basePath)
            mLinphoneCore = LinphoneCoreFactory.instance().createLinphoneCore(mListener, basePath + "/.linphonerc", basePath + "linphonerc", null, mContext)
            mLinphoneCore.addListener(mListener)
            initLinphoneCoreValues(basePath)

            setUserAgent()
            setFrontCamAsDefault()
            startIterate()
            mInstance = this
            mLinphoneCore.isNetworkReachable = true
            mLinphoneCore.setSipNetworkReachable(true)
            mLinphoneCore.enableSpeaker(true)
            mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            mAudioManager.stopBluetoothSco()
            mAudioManager.isBluetoothScoOn = false
            logger.info { "bluetooth mode: ${mAudioManager.mode}" }
            for (codec in mLinphoneCore.videoCodecs){
                logger.info { "codec: ${codec.toString()}, mime: ${codec.mime.toString()}" }
            }
            mLinphoneCore.enableVideo(true, true)
        }catch (e: LinphoneCoreException){
            e.printStackTrace()
        }catch (e: IOException){
            e.printStackTrace()
        }
    }

    fun destroy() {
        try {
            mTimer.cancel()
            mLinphoneCore.destroy()
        }catch (e: RuntimeException){

        }
    }

    fun enableVideo() : Int{
        val param = mLinphoneCore.createCallParams(mCall)
        param.videoEnabled = true
        return mLinphoneCore.updateCall(mCall, param)
    }

    fun acceptEarlyMedia() : Boolean{
        isIncomingCall = true

        mAudioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        val params: LinphoneCallParams = mLinphoneCore.createCallParams(mCall)
        params.enableLowBandwidth(false)
        params.videoEnabled = true

        return mLinphoneCore.acceptEarlyMediaWithParams(mCall, params)
    }

    fun accept() {
        if (isIncomingCall){
            val calls = mLinphoneCore.calls
            for (call in calls){
                if (call.state == State.IncomingReceived || call.state == State.CallIncomingEarlyMedia) {
                    val params: LinphoneCallParams = mLinphoneCore.createCallParams(call)
                    params.enableLowBandwidth(false)
                    params.videoEnabled = true
                    checkCamera()
                    mLinphoneCore.acceptCallWithParams(call, params)
                    mLinphoneCore.enableSpeaker(true)
                    mAudioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                    mInterface?.callAccepted()
                    logger.info { "incoming received, is video enabled: ${call.currentParams.videoEnabled}" }
                }else {
                    mAudioManager.mode = AudioManager.MODE_NORMAL
                }
            }
        }else {
            logger.info { "There are no incoming calls" }
        }
    }

    fun decline() {
        logger.info { "in: $isIncomingCall, out: $isOutgoingCall" }
        if (isIncomingCall || isOutgoingCall) {
            mLinphoneCore.terminateCall(mCall)
        }
        isOutgoingCall = false
        isIncomingCall = false
        mCall = null
    }

    fun dial(){
        logger.info { "isOutgoingCall: $isOutgoingCall" }
        isOutgoingCall = true
        if (!isIncomingCall){
            try {
                mCall = mLinphoneCore.invite(ipAddress)
            }catch (e: Exception){
                e.printStackTrace()
            }
            logger.info { "place call to $ipAddress" }
            if (mCall == null){
                logger.info { "could not place call to " + ipAddress }
            }
        }else {
            logger.info { "isIncomingCall=$isIncomingCall" }
        }
    }

    fun holdAndResume(isHold: Boolean){
        if (mCall != null){
            var call = mCall
            if (isHold){
                mLinphoneCore.resumeCall(call)
            }else {
                mLinphoneCore.pauseCall(call)
            }
        }
    }

    fun updateCall(params: LinphoneCallParams){
        mLinphoneCore.updateCall(mCall, params)
    }

    private fun copyAssetsFromPackage(basePath: String) {
        LinphoneMiniUtils.copyIfNotExist(mContext, R.raw.oldphone_mono, basePath + "/oldphone_mono.wav")
        LinphoneMiniUtils.copyIfNotExist(mContext, R.raw.ringback, basePath + "ringback.wav")
        LinphoneMiniUtils.copyIfNotExist(mContext, R.raw.toy_mono, basePath + "toy_mono.wav")
        LinphoneMiniUtils.copyIfNotExist(mContext, R.raw.linphonerc_default, basePath + "/.linphonerc")
//        val factoryFile = File(basePath + "/linphonerc")
        LinphoneMiniUtils.copyFromPackage(mContext, R.raw.linphonerc_factory, basePath + "/linphonerc")
        LinphoneMiniUtils.copyIfNotExist(mContext, R.raw.lpconfig, basePath + "/lpconfig.xsd")
        LinphoneMiniUtils.copyIfNotExist(mContext, R.raw.rootca, basePath + "/rootca.pem")
        LinphoneMiniUtils.copyIfNotExist(mContext, R.raw.test, basePath + "/test.mp3")
    }
    private fun initLinphoneCoreValues(basePath: String) {
        mLinphoneCore.setContext(mContext)
        mLinphoneCore.setRing(null)
        mLinphoneCore.setRootCA(basePath + "/rootca.pem")
        mLinphoneCore.setPlayFile(basePath + "toy_mono.wav")
        mLinphoneCore.setChatDatabasePath(basePath + "linphone-history.db")
        val availableCores = Runtime.getRuntime().availableProcessors()
        mLinphoneCore.setCpuCount(availableCores)
    }

    private fun setUserAgent(){
        try {
            var versionName = mContext.packageManager.getPackageInfo(mContext.packageName, 0).versionName
            if (versionName == null){
                versionName = mContext.packageManager.getPackageInfo(mContext.packageName, 0).versionCode.toString()
            }
            mLinphoneCore.setUserAgent("LinphoneMiniAndroid", versionName)
        }catch (e: PackageManager.NameNotFoundException){
            e.printStackTrace()
        }
    }

    private fun setFrontCamAsDefault(){
        var camID = 0
        var cams: Array<AndroidCamera> = AndroidCameraConfiguration.retrieveCameras()
        for (cam in cams){
            if (cam.frontFacing) {
                camID = cam.id
            }
        }
        mLinphoneCore.videoDevice = camID
    }

    private fun startIterate(){
        mTimer = Timer("LinphoneMini scheduler")
        mTimer.schedule(timerTask { mLinphoneCore.iterate() }, 0, 20)
    }

    private fun checkCamera() {
        var videoDeviceId = mLinphoneCore.videoDevice
        if (AndroidCameraConfiguration.retrieveCameras().size == 1){
            videoDeviceId = 0
        }else {
            videoDeviceId = 1
        }
        mLinphoneCore.videoDevice = videoDeviceId
    }
}