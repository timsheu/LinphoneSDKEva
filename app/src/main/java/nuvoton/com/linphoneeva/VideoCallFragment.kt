package nuvoton.com.linphoneeva

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.DisplayMetrics
import android.view.*
import org.linphone.core.LinphoneCore
import org.linphone.mediastream.video.AndroidVideoWindowImpl

/**
 * Created by cchsu20 on 2018/4/11.
 */
class VideoCallFragment : Fragment() {
    private val TAG = "VideoCallFragment"
    private var mVideoView: SurfaceView? = null
    private var mCaptureView: SurfaceView? = null
    private var androidVideoWindowImpl: AndroidVideoWindowImpl? = null
    private val mZoomFactor = 1.0f
    private var mZoomCenterX = 0.0f
    private var mZoomCenterY = 0.0f
    private var previewX = 0
    private var previewY = 0
    var mCore: LinphoneCore? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view: View = if (mCore?.hasCrappyOpenGL() == true){
            inflater.inflate(R.layout.video_no_opengl, container, false)
        }else {
            inflater.inflate(R.layout.video, container, false)
        }

        mVideoView = view.findViewById(R.id.videoSurface)
        mCaptureView = view.findViewById(R.id.videoCaptureSurface)
//        @Deprecated
//        mCaptureView.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        fixZOrder(mVideoView!!, mCaptureView!!)

        androidVideoWindowImpl = AndroidVideoWindowImpl(mVideoView, mCaptureView, object : AndroidVideoWindowImpl.VideoWindowListener {
            override fun onVideoRenderingSurfaceReady(p0: AndroidVideoWindowImpl?, p1: SurfaceView?) {
                NuvotonLogger.debugMessage(TAG, "video rendering surface ready")
                mVideoView = p1
                mCore?.setVideoWindow(p0)
            }

            override fun onVideoPreviewSurfaceDestroyed(p0: AndroidVideoWindowImpl?) {
                NuvotonLogger.debugMessage(TAG, "video preview surface destroyed")
            }

            override fun onVideoPreviewSurfaceReady(p0: AndroidVideoWindowImpl?, p1: SurfaceView?) {
                NuvotonLogger.debugMessage(TAG, "video preview surface ready")
                mCaptureView = p1
                mCore!!.setPreviewWindow(mCaptureView)
                resizePreview()
            }

            override fun onVideoRenderingSurfaceDestroyed(p0: AndroidVideoWindowImpl?) {
                NuvotonLogger.debugMessage(TAG, "video rendering surface destroyed")
            }
        })
        return view
    }


    private fun fixZOrder(video : SurfaceView, preview: SurfaceView){
        video.setZOrderOnTop(false)
        preview.setZOrderOnTop(true)
        preview.setZOrderMediaOverlay(true)
    }

    private fun resizePreview(){
        val core = mCore!!
        if (core.callsNb > 0){
            var call = core.currentCall
            if (call == null){
                call = core.calls[0]
            }
            if (call == null) {
                return
            }

            val metrics = DisplayMetrics()
            this.activity?.windowManager?.defaultDisplay?.getMetrics(metrics)
            val screenHeight = metrics.heightPixels
            val maxHeight = screenHeight / 4

            val videoSize = call.currentParams.sentVideoSize
            var width = if (videoSize.width == 0) 320 else videoSize.width
            var height = if (videoSize.height == 0) 240 else videoSize.height
            NuvotonLogger.debugMessage(TAG, "Video height is $height, width is $width")
            width = width * maxHeight / height
            height = maxHeight
            mCaptureView!!.holder.setFixedSize(width, height)
            NuvotonLogger.debugMessage(TAG, "Video preview size is set to $width x $height")
        }
    }
}