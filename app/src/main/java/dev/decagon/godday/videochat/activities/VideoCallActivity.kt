package dev.decagon.godday.videochat.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import dev.decagon.godday.videochat.R
import dev.decagon.godday.videochat.model.User
import dev.decagon.godday.videochat.databinding.ActivityVideoCallBinding
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration

class VideoCallActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideoCallBinding
    private lateinit var user: User
    private lateinit var target: String
    private lateinit var channelName: String

    private lateinit var remoteContainer: RelativeLayout
    private lateinit var localContainer: FrameLayout
    private var localVideo: VideoCanvas? = null
    private var remoteVideo: VideoCanvas? = null
    private lateinit var callBtn: ImageView
    private lateinit var muteBtn: ImageView
    private lateinit var switchCameraBtn: ImageView

    private lateinit var rtcEngine: RtcEngine
    private var callEnd: Boolean = false
    private var muted: Boolean = false


    private val mRtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                logIt("Join channel success, uid: " + (uid and 0xFFFFFFFFL.toInt()))
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                logIt("First remote video decoded, uid: " + (uid and 0xFFFFFFFFL.toInt()))
                setupRemoteVideo(uid)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                logIt("User offline, uid: " + (uid and 0xFFFFFFFFL.toInt()))
                onRemoteUserLeft(uid)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()

        binding = ActivityVideoCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()

        // Get all extras from intent
        user = intent.getParcelableExtra(USER)!!
        target = intent.getStringExtra(TARGET)!!
        channelName = intent.getStringExtra(CHANNEL_NAME)!!

        checkPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!callEnd) {
            leaveChannel()
        }
        RtcEngine.destroy()
    }

    private fun checkPermissions() {
        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQUEST_ID) &&
            checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQUEST_ID) &&
            checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQUEST_ID)) {
            initEngineAndJoinChannel()
        }
    }

    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(this, permission) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, REQUESTED_PERMISSIONS, requestCode
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_ID) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[1] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[2] != PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(binding.root, "Can't make video chat without these permissions",
                    Snackbar.LENGTH_LONG).show()
                finish()
                return
            }
            initEngineAndJoinChannel()
        }
    }

    private fun logIt(message: String) {
       Log.d("VideoCallActivity", message)
    }

    private fun initUI() {
        binding.apply {
            localContainer = localVideoViewContainer
            remoteContainer = remoteVideoViewContainer
            callBtn = btnCall
            muteBtn = btnMute
            switchCameraBtn = btnSwitchCamera
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        var parent: ViewGroup = remoteContainer
        if (parent.indexOfChild(localVideo?.view) > - 1) {
            parent = localContainer
        }

        if (remoteVideo != null) return

        val view = RtcEngine.CreateRendererView(baseContext)
        view.setZOrderMediaOverlay(parent == localContainer)
        parent.addView(view)

        remoteVideo = VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, uid)
        rtcEngine.setupRemoteVideo(remoteVideo)
    }

    private fun onRemoteUserLeft(uid: Int) {
        if (remoteVideo != null && remoteVideo!!.uid == uid) {
            removeFromParent(remoteVideo)
            remoteVideo = null
        }
    }

    private fun initEngineAndJoinChannel() {
        initializeEngine()
        setupVideoConfig()
        setupLocalVideo()
        joinChannel()
    }

    private fun initializeEngine() {
        try {
            rtcEngine = RtcEngine.create(baseContext, getString(R.string.agora_app_id), mRtcEventHandler)
        } catch (e: Exception) {
            logIt(Log.getStackTraceString(e))
            throw RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e))
        }
    }

    private fun setupVideoConfig() {
        rtcEngine.enableVideo()
        rtcEngine.setVideoEncoderConfiguration( VideoEncoderConfiguration())
    }

    private fun setupLocalVideo() {
        val view = RtcEngine.CreateRendererView(baseContext)
        view.setZOrderMediaOverlay(true)
        localContainer.addView(view)

        localVideo = VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, 0)
        rtcEngine.setupLocalVideo(localVideo)
    }

    private fun joinChannel() {
        rtcEngine.joinChannel(null, channelName, "Extra Optional Data", 0)
    }

    private fun leaveChannel() {
        rtcEngine.leaveChannel()
    }

    fun onLocalAudioMuteClicked(view: View) {
        muted = !muted
        rtcEngine.muteLocalAudioStream(muted)
        val res = if (muted) R.drawable.btn_mute else R.drawable.btn_unmute
        muteBtn.setImageResource(res)
    }

    fun onSwitchCameraClicked(view: View) {
        rtcEngine.switchCamera()
    }

    fun onCallClicked(view: View) {
        if (callEnd) {
            startCall()
            callEnd = false
            callBtn.setImageResource(R.drawable.btn_endcall)
        } else {
            endCall()
            callEnd = true
            callBtn.setImageResource(R.drawable.btn_startcall)
        }
        showButtons(!callEnd)
    }

    private fun startCall() {
        setupLocalVideo()
        joinChannel()
    }

    private fun endCall() {
        removeFromParent(localVideo)
        localVideo = null
        removeFromParent(remoteVideo)
        remoteVideo = null
        leaveChannel()
        finish()
    }

    private fun showButtons(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        muteBtn.visibility = visibility
        switchCameraBtn.visibility = visibility
    }

    private fun removeFromParent(canvas: VideoCanvas?): ViewGroup? {
        if (canvas != null) {
            val parent: ViewParent = canvas.view.parent
            if (parent != null) {
                val group: ViewGroup = parent as ViewGroup
                group.removeView(canvas.view)
                return group
            }
        }
        return null
    }

    private fun switchView(canvas: VideoCanvas?) {
        val parent: ViewGroup? = removeFromParent(canvas)
        if (parent == localContainer) {
            if (canvas?.view is SurfaceView) {
                canvas.view.setZOrderMediaOverlay(false)
            }
            remoteContainer.addView(canvas?.view)
        } else if (parent == remoteContainer) {
            if (canvas?.view is SurfaceView) {
                canvas.view.setZOrderMediaOverlay(true)
            }
            localContainer.addView(canvas?.view)
        }
    }

    fun onLocalContainerClick(view: View) {
        switchView(localVideo)
        switchView(remoteVideo)
    }

    companion object {
        private const val USER = "user"
        private const val CHANNEL_NAME = "channelName"
        private const val TARGET = "target"

        private const val PERMISSION_REQUEST_ID = 22
        private val REQUESTED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        // Define the logic of navigating to this activity and a means to
        // get the needed data for the creation of the activity.
        fun newIntent(context: Context, user: User, channelName: String, target: String): Intent {
            return Intent(context, VideoCallActivity::class.java).also {
                it.putExtra(USER, user)
                it.putExtra(CHANNEL_NAME, channelName)
                it.putExtra(TARGET, target)
            }
        }
    }
}