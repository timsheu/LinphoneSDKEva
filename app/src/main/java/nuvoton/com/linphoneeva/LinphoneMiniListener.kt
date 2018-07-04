package nuvoton.com.linphoneeva

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.SurfaceView
import org.linphone.core.*
import java.io.IOException;
import java.util.Timer;

import org.linphone.core.LinphoneCall.State;
import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration;
import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration.AndroidCamera;
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.concurrent.timerTask
/**
 * Created by cchsu20 on 14/03/2018.
 */

class LinphoneMiniListener(context: Context) {
    private val TAG = "LinphoneMiniListener"

    interface LinphoneMiniListenerInterface {
        fun callAccepted()
        fun callEnded()
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
    private var mContext: Context = context
    private var mCall: LinphoneCall? = null
    lateinit var mLinphoneCore: LinphoneCore
    private lateinit var mTimer: Timer
    lateinit var mAudioManager: AudioManager
    lateinit var videoView: SurfaceView
    lateinit var videoCaptureView: SurfaceView
    var ipAddress = "sip:192.168.8."
    var audioOnly = false
    private var accountCreator: LinphoneAccountCreator? = null

    fun destroy() {
        try {
            mTimer.cancel()
            mLinphoneCore.destroy()
        } catch (e: RuntimeException) {

        }
    }

    fun enableVideo(): Int {
        val param = mLinphoneCore.createCallParams(mCall)
        param.videoEnabled = true
        return mLinphoneCore.updateCall(mCall, param)
    }

    fun acceptEarlyMedia(): Boolean {
        isIncomingCall = true

        mAudioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        val params: LinphoneCallParams = mLinphoneCore.createCallParams(mCall)
        params.enableLowBandwidth(false)
        params.videoEnabled = true

        return mLinphoneCore.acceptEarlyMediaWithParams(mCall, params)
    }

    fun accept() {
        if (isIncomingCall) {
            val calls = mLinphoneCore.calls
            for (call in calls) {
                if (call.state == State.IncomingReceived || call.state == State.CallIncomingEarlyMedia) {
                    val params: LinphoneCallParams = mLinphoneCore.createCallParams(call)
                    params.enableLowBandwidth(false)
                    params.videoEnabled = true
                    checkCamera()
                    mLinphoneCore.acceptCallWithParams(call, params)
                    val isSpeaker = SettingManager.shared.settingMap[Category.EnableSpeaker.name]
                    val enableSpeaker = isSpeaker == "true"
                    mLinphoneCore.enableSpeaker(enableSpeaker)
                    mAudioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                    mInterface?.callAccepted()
                    NuvotonLogger.debugMessage(TAG, "incoming received, is video enabled: ${call.currentParams.videoEnabled}")
                } else {
                    mAudioManager.mode = AudioManager.MODE_NORMAL
                }
            }
        } else {
            NuvotonLogger.debugMessage(TAG, "There are no incoming calls")
        }
    }

    fun decline() {
        NuvotonLogger.debugMessage(TAG, "in: $isIncomingCall, out: $isOutgoingCall")
        if (isIncomingCall || isOutgoingCall) {
            mLinphoneCore.terminateCall(mCall)
        }
        isOutgoingCall = false
        isIncomingCall = false
        mCall = null
    }

    fun dial() {
        NuvotonLogger.debugMessage(TAG, "isOutgoingCall: $isOutgoingCall")
        if (!isOutgoingCall && !isIncomingCall) {
            try {
                mCall = mLinphoneCore.invite(ipAddress)
//                val linphoneAddress = LinphoneCoreFactory.instance().createLinphoneAddress("cchsu20", "sip.linphhone.org", "cchsu20")
//                mCall = mLinphoneCore.invite(linphoneAddress)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            NuvotonLogger.debugMessage(TAG, "place call to $ipAddress")
            if (mCall == null) {
                NuvotonLogger.debugMessage(TAG, "could not place call to " + ipAddress)
            }
        } else {
            NuvotonLogger.debugMessage(TAG, "isIncomingCall=$isIncomingCall")
        }
        isOutgoingCall = true
    }

    fun holdAndResume(isHold: Boolean) {
        if (mCall != null) {
            var call = mCall
            if (isHold) {
                mLinphoneCore.resumeCall(call)
            } else {
                mLinphoneCore.pauseCall(call)
            }
        }
    }

    fun updateCall(params: LinphoneCallParams) {
        mLinphoneCore.updateCall(mCall, params)
    }

    fun loginServer(username: String, password: String) {
        mLinphoneCore.defaultProxyConfig
    }

    private fun copyAssetsFromPackage(basePath: String) {
        LinphoneMiniUtils.copyIfNotExist(mContext, R.raw.oldphone_mono, basePath + "/oldphone_mono.wav")
        LinphoneMiniUtils.copyIfNotExist(mContext, R.raw.ringback, basePath + "/ringback.wav")
        LinphoneMiniUtils.copyIfNotExist(mContext, R.raw.toy_mono, basePath + "/toy_mono.wav")
        LinphoneMiniUtils.copyIfNotExist(mContext, R.raw.linphonerc_default, basePath + "/.linphonerc")
//        val factoryFile = File(basePath + "/linphonerc")
        LinphoneMiniUtils.copyFromPackage(mContext, R.raw.linphonerc_factory, basePath + "/linphonerc")
        LinphoneMiniUtils.copyIfNotExist(mContext, R.raw.lpconfig, basePath + "/lpconfig.xsd")
        LinphoneMiniUtils.copyIfNotExist(mContext, R.raw.rootca, basePath + "/rootca.pem")
        LinphoneMiniUtils.copyIfNotExist(mContext, R.raw.test, basePath + "/test.mp3")
        LinphoneMiniUtils.copyIfNotExist(mContext, R.raw.assistant_create, basePath + "/assistant_create.rc")
    }

    private fun initLinphoneCoreValues(basePath: String) {
        mLinphoneCore.setContext(mContext)
        mLinphoneCore.setRing(null)
        mLinphoneCore.setRootCA(basePath + "/rootca.pem")
        mLinphoneCore.setPlayFile(basePath + "/toy_mono.wav")
        mLinphoneCore.setChatDatabasePath(basePath + "/linphone-history.db")
        val availableCores = Runtime.getRuntime().availableProcessors()
        mLinphoneCore.setCpuCount(availableCores)
    }

    private fun setUserAgent() {
        try {
            var versionName = mContext.packageManager.getPackageInfo(mContext.packageName, 0).versionName
            if (versionName == null) {
                versionName = mContext.packageManager.getPackageInfo(mContext.packageName, 0).versionCode.toString()
            }
            mLinphoneCore.setUserAgent("LinphoneMiniAndroid", versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun setFrontCamAsDefault() {
        var camID = 0
        var cams: Array<AndroidCamera> = AndroidCameraConfiguration.retrieveCameras()
        cams.filter { it.frontFacing }.forEach { camID = it.id }
        mLinphoneCore.videoDevice = camID
    }

    private fun startIterate() {
        mTimer = Timer("LinphoneMini scheduler")
        mTimer.schedule(timerTask { mLinphoneCore.iterate() }, 0, 20)
    }

    private fun checkCamera() {
        var videoDeviceId = mLinphoneCore.videoDevice
        videoDeviceId = if (AndroidCameraConfiguration.retrieveCameras().size == 1) { 0 } else { 1 }
        mLinphoneCore.videoDevice = videoDeviceId
    }

    init {
        LinphoneCoreFactory.instance().setDebugMode(false, "Linphone Mini")
        try {
            val basePath = mContext.filesDir.absolutePath
            val linphonercPath = basePath + "/.linphonerc"
            val linphoneFactoryConfigFile = basePath + "/linphonerc"
            copyAssetsFromPackage(basePath)
            mLinphoneCore = LinphoneCoreFactory.instance().createLinphoneCore(mListener, linphonercPath, linphoneFactoryConfigFile, null, mContext)
            mLinphoneCore.addListener(mListener)
            initLinphoneCoreValues(basePath)

            setUserAgent()
            setFrontCamAsDefault()
            startIterate()
            mInstance = this
            mLinphoneCore.isNetworkReachable = true
            mLinphoneCore.setSipNetworkReachable(true)
            val isSpeaker = SettingManager.shared.settingMap[Category.EnableSpeaker.name]
            val enableSpeaker = isSpeaker == "true"
            mLinphoneCore.enableSpeaker(enableSpeaker)
            mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            mAudioManager.stopBluetoothSco()
            mAudioManager.isBluetoothScoOn = false
            NuvotonLogger.debugMessage(TAG, "bluetooth mode: ${mAudioManager.mode}")
            for (codec in mLinphoneCore.videoCodecs) {
                NuvotonLogger.debugMessage(TAG, "codec: ${codec.toString()}, mime: ${codec.mime}")
            }
            mLinphoneCore.enableVideo(true, true)
//            setupAccountCreator(linphonercPath)
        } catch (e: LinphoneCoreException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(LinphoneCoreException::class)
    fun login(username: String, password: String, domain: String) {
        val identity = "sip:$username@$domain"
        mLinphoneCore.addAuthInfo(LinphoneCoreFactory.instance().createAuthInfo(username, password, null, domain))

        val proxyConfig = mLinphoneCore.createProxyConfig(identity, domain, null, true)
        proxyConfig.expires = 2000
        mLinphoneCore.addProxyConfig(proxyConfig)
        mLinphoneCore.defaultProxyConfig = proxyConfig

        mLinphoneCore.defaultProxyConfig.edit()
        mLinphoneCore.defaultProxyConfig.enableRegister(true)
        mLinphoneCore.defaultProxyConfig.done()
    }

}