package org.appspot.apprtc;

//import android.support.v7.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Toast;
import android.view.WindowManager.LayoutParams;

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

public class TestActivity extends Activity implements CallWebrtc.OnPeerConnectionNSignalingEvents,
        CallFragment.OnCallEvents
{
    private final static String TAG = "TestActivity";
    private Toast logToast;

    private SurfaceViewRenderer localRender;
    private SurfaceViewRenderer remoteRender;
    private PercentFrameLayout localRenderLayout;
    private PercentFrameLayout remoteRenderLayout;

    private CallWebrtc callWebrtc;

    private String roomId, callerId, calleeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(
                new UnhandledExceptionHandler(this));

        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                LayoutParams.FLAG_FULLSCREEN
                        | LayoutParams.FLAG_KEEP_SCREEN_ON
                        | LayoutParams.FLAG_DISMISS_KEYGUARD
                        | LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_call);

        final Intent intent = getIntent();
        roomId = intent.getStringExtra("room_id");
        callerId = intent.getStringExtra("caller_id");
        calleeId = intent.getStringExtra("callee_id");

        // Create UI controls.
        localRender = (SurfaceViewRenderer) findViewById(R.id.local_video_view);
        remoteRender = (SurfaceViewRenderer) findViewById(R.id.remote_video_view);
        localRenderLayout = (PercentFrameLayout) findViewById(R.id.local_video_layout);
        remoteRenderLayout = (PercentFrameLayout) findViewById(R.id.remote_video_layout);

        callWebrtc = CallWebrtc.getInstant();
        callWebrtc.init(this, new CallWebrtc.RoomParameters(roomId, callerId, calleeId), localRender, remoteRender, localRenderLayout, remoteRenderLayout);

    }


    @Override
    public void onPeerConnectionEventFired(String msg) {
        Log.d(TAG, "gogo" + msg);
        logAndToast("PeerConnectionEvent:" + msg);
    }

    @Override
    public void onSignalingEventFired(String msg) {
        logAndToast("SignalingEvent:" + msg);
    }

    @Override
    public void onSocketIOEventFired(String msg) {
        logAndToast("SocketIOEvent:" + msg);
    }

    /*@Override
    public void onLocalDescription(SessionDescription sdp) {

    }

    @Override
    public void onIceCandidate(IceCandidate candidate) {

    }

    @Override
    public void onIceConnected() {

    }

    @Override
    public void onIceDisconnected() {

    }

    @Override
    public void onPeerConnectionClosed() {

    }

    @Override
    public void onPeerConnectionStatsReady(StatsReport[] reports) {

    }

    @Override
    public void onPeerConnectionError(String description) {

    }

    @Override
    public void onConnectedToRoom(AppRTCClient.SignalingParameters params) {

    }

    @Override
    public void onRemoteDescription(SessionDescription sdp) {

    }

    @Override
    public void onRemoteIceCandidate(IceCandidate candidate) {

    }

    @Override
    public void onChannelClose() {

    }

    @Override
    public void onChannelError(String description) {

    }*/

    @Override
    public void onCallHangUp() {
        callWebrtc.disconnect();
    }

    @Override
    public void onCameraSwitch() {
        if (callWebrtc.getPeerConnectionClient() != null) {
            callWebrtc.getPeerConnectionClient().switchCamera();
        }
    }

    @Override
    public void onVideoScalingSwitch(RendererCommon.ScalingType scalingType) {
//        this.scalingType = scalingType;
//        updateVideoView();
    }

    @Override
    public void onCaptureFormatChange(int width, int height, int framerate) {
        if (callWebrtc.getPeerConnectionClient() != null) {
            callWebrtc.getPeerConnectionClient().changeCaptureFormat(width, height, framerate);
        }
    }

    // Log |msg| and Toast about it.
    private void logAndToast(String msg) {
        Log.d(TAG, msg);
        if (logToast != null) {
            logToast.cancel();
        }
        //Looper.prepare();
        logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        //Looper.loop();
        logToast.show();
    }
}
