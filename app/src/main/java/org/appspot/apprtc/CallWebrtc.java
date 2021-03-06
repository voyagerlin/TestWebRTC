/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.appspot.apprtc;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.appspot.apprtc.AppRTCClient.RoomConnectionParameters;
import org.appspot.apprtc.AppRTCClient.SignalingParameters;
import org.appspot.apprtc.PeerConnectionClient.PeerConnectionParameters;
import org.appspot.apprtc.util.LooperExecutor;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.RendererCommon.ScalingType;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

//adding socket io


/**
 * Activity for peer connection call setup, call waiting
 * and call view.
 */
public class CallWebrtc implements AppRTCClient.SignalingEvents,
        PeerConnectionClient.PeerConnectionEvents {

    private static CallWebrtc myInstance = new CallWebrtc();

    public static CallWebrtc getInstant() {
        return myInstance;
    }

    private CallWebrtc() {
    }

    private OnPeerConnectionNSignalingEvents mListener;

    private Socket socket;
    private Activity activity;

    public static final String EXTRA_ROOMID =
            "org.appspot.apprtc.ROOMID";
    public static final String EXTRA_LOOPBACK =
            "org.appspot.apprtc.LOOPBACK";
    public static final String EXTRA_VIDEO_CALL =
            "org.appspot.apprtc.VIDEO_CALL";
    public static final String EXTRA_VIDEO_WIDTH =
            "org.appspot.apprtc.VIDEO_WIDTH";
    public static final String EXTRA_VIDEO_HEIGHT =
            "org.appspot.apprtc.VIDEO_HEIGHT";
    public static final String EXTRA_VIDEO_FPS =
            "org.appspot.apprtc.VIDEO_FPS";
    public static final String EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED =
            "org.appsopt.apprtc.VIDEO_CAPTUREQUALITYSLIDER";
    public static final String EXTRA_VIDEO_BITRATE =
            "org.appspot.apprtc.VIDEO_BITRATE";
    public static final String EXTRA_VIDEOCODEC =
            "org.appspot.apprtc.VIDEOCODEC";
    public static final String EXTRA_HWCODEC_ENABLED =
            "org.appspot.apprtc.HWCODEC";
    public static final String EXTRA_CAPTURETOTEXTURE_ENABLED =
            "org.appspot.apprtc.CAPTURETOTEXTURE";
    public static final String EXTRA_AUDIO_BITRATE =
            "org.appspot.apprtc.AUDIO_BITRATE";
    public static final String EXTRA_AUDIOCODEC =
            "org.appspot.apprtc.AUDIOCODEC";
    public static final String EXTRA_NOAUDIOPROCESSING_ENABLED =
            "org.appspot.apprtc.NOAUDIOPROCESSING";
    public static final String EXTRA_AECDUMP_ENABLED =
            "org.appspot.apprtc.AECDUMP";
    public static final String EXTRA_OPENSLES_ENABLED =
            "org.appspot.apprtc.OPENSLES";
    public static final String EXTRA_DISPLAY_HUD =
            "org.appspot.apprtc.DISPLAY_HUD";
    public static final String EXTRA_TRACING = "org.appspot.apprtc.TRACING";
    public static final String EXTRA_CMDLINE =
            "org.appspot.apprtc.CMDLINE";
    public static final String EXTRA_RUNTIME =
            "org.appspot.apprtc.RUNTIME";
    private static final String TAG = "CallRTCClient";

    // List of mandatory application permissions.
    private static final String[] MANDATORY_PERMISSIONS = {
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET"
    };

    /**
     * Standard activity result: operation canceled.
     */
    public static final int RESULT_CANCELED = 0;
    /**
     * Standard activity result: operation succeeded.
     */
    public static final int RESULT_OK = -1;
    /**
     * Start of user-defined activity results.
     */
    public static final int RESULT_FIRST_USER = 1;

    // Peer connection statistics callback period in ms.
    private static final int STAT_CALLBACK_PERIOD = 1000;
    // Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 72;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;

    public PeerConnectionClient getPeerConnectionClient() {
        return peerConnectionClient;
    }

    private PeerConnectionClient peerConnectionClient = null;
    private AppRTCClient appRtcClient;
    private SignalingParameters signalingParameters;
    private AppRTCAudioManager audioManager = null;
    private EglBase rootEglBase;
    private SurfaceViewRenderer localRender;
    private SurfaceViewRenderer remoteRender;
    private PercentFrameLayout localRenderLayout;
    private PercentFrameLayout remoteRenderLayout;
    private ScalingType scalingType;
    //private Toast logToast;
    private boolean commandLineRun;
    private int runTimeMs;
    private boolean activityRunning;
    private RoomConnectionParameters roomConnectionParameters;
    private PeerConnectionParameters peerConnectionParameters;
    private boolean iceConnected;
    private boolean isError;
    private boolean callControlFragmentVisible = true;
    private long callStartedTimeMs = 0;

    // Controls
    CallFragment callFragment;
    HudFragment hudFragment;

    //roomId, callerId, calleeId
    private String roomId, callerId, calleeId;

    public static class RoomParameters {
        public String roomId;
        public String callerId;
        public String calleeId;

        public RoomParameters(String roomId, String callerId, String calleeId) {
            this.roomId = roomId;
            this.callerId = callerId;
            this.calleeId = calleeId;
        }
    }

    public void init(Activity activity, RoomParameters roomParam, SurfaceViewRenderer localSVRender, SurfaceViewRenderer remoteSVRender,
                     ViewGroup localViewGroup, ViewGroup remoteViewGroup) {

        this.activity = activity;
        this.mListener = (OnPeerConnectionNSignalingEvents) activity;
        iceConnected = false;
        signalingParameters = null;
        scalingType = ScalingType.SCALE_ASPECT_FILL;

        // Create UI controls.
        localRender = localSVRender;
        remoteRender = remoteSVRender;
        localRenderLayout = (PercentFrameLayout) localViewGroup;
        remoteRenderLayout = (PercentFrameLayout) remoteViewGroup;
        callFragment = new CallFragment();
        hudFragment = new HudFragment();

        // Show/hide call control fragment on view click.
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleCallControlFragmentVisibility();
            }
        };

        localRender.setOnClickListener(listener);
        remoteRender.setOnClickListener(listener);

        // Create video renderers.
        rootEglBase = EglBase.create();
        localRender.init(rootEglBase.getEglBaseContext(), null);
        remoteRender.init(rootEglBase.getEglBaseContext(), null);
        localRender.setZOrderMediaOverlay(true);
        updateVideoView();

        // Check for mandatory permissions.
//        for (String permission : MANDATORY_PERMISSIONS) {
//            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
//                logAndToast("Permission " + permission + " is not granted");
//                setResult(RESULT_CANCELED);
//                finish();
//                return;
//            }
//        }

        // Get Intent parameters.
        final Intent intent = activity.getIntent();
        //Log.d(TAG, "testing:" + intent.toString());

        //Uri roomUri = intent.getData();
        //Uri roomUri = Uri.parse("https://apprtc.appspot.com");
        Uri roomUri = Uri.parse("http://54.169.150.82:3000");
        if (roomUri == null) {
            //logAndToast(getString(R.string.missing_url));
            Log.e(TAG, "Didn't get any URL in intent!");
//            setResult(RESULT_CANCELED);
//            finish();
            this.activity.setResult(RESULT_CANCELED);
            this.activity.finish();
            return;
        }

        this.roomId = roomParam.roomId;
        this.callerId = roomParam.callerId;
        this.calleeId = roomParam.calleeId;

        if (roomId == null || roomId.length() == 0) {
            //logAndToast(getString(R.string.missing_url));
            Log.e(TAG, "Incorrect room ID in intent!");
//            setResult(RESULT_CANCELED);
//            finish();
            this.activity.setResult(RESULT_CANCELED);
            this.activity.finish();
            return;
        }

        //adding socket io
        createSocketIo(roomId, callerId, calleeId);

        boolean loopback = intent.getBooleanExtra(EXTRA_LOOPBACK, false);
        boolean tracing = intent.getBooleanExtra(EXTRA_TRACING, false);
        peerConnectionParameters = new PeerConnectionParameters(
                intent.getBooleanExtra(EXTRA_VIDEO_CALL, true),
                loopback,
                tracing,
                intent.getIntExtra(EXTRA_VIDEO_WIDTH, 0),
                intent.getIntExtra(EXTRA_VIDEO_HEIGHT, 0),
                intent.getIntExtra(EXTRA_VIDEO_FPS, 0),
                intent.getIntExtra(EXTRA_VIDEO_BITRATE, 0),
                intent.getStringExtra(EXTRA_VIDEOCODEC),
                intent.getBooleanExtra(EXTRA_HWCODEC_ENABLED, true),
                intent.getBooleanExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, false),
                intent.getIntExtra(EXTRA_AUDIO_BITRATE, 0),
                intent.getStringExtra(EXTRA_AUDIOCODEC),
                intent.getBooleanExtra(EXTRA_NOAUDIOPROCESSING_ENABLED, false),
                intent.getBooleanExtra(EXTRA_AECDUMP_ENABLED, false),
                intent.getBooleanExtra(EXTRA_OPENSLES_ENABLED, false));
        commandLineRun = intent.getBooleanExtra(EXTRA_CMDLINE, false);
        runTimeMs = intent.getIntExtra(EXTRA_RUNTIME, 0);

        // Create connection client and connection parameters.
        appRtcClient = new WebSocketRTCClient(this, new LooperExecutor(), socket);
        roomConnectionParameters = new RoomConnectionParameters(
                roomUri.toString(), roomId, loopback, callerId, calleeId);

        // Send intent arguments to fragments.
        callFragment.setArguments(intent.getExtras());
        hudFragment.setArguments(intent.getExtras());
        // Activate call and HUD fragments and start the call.
        FragmentTransaction ft = activity.getFragmentManager().beginTransaction();
        ft.add(R.id.call_fragment_container, callFragment);
        ft.add(R.id.hud_fragment_container, hudFragment);
        ft.commit();
        startCall();

        // For command line execution run connection for <runTimeMs> and exit.
        if (commandLineRun && runTimeMs > 0) {
            (new Handler()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    disconnect();
                }
            }, runTimeMs);
        }

        peerConnectionClient = PeerConnectionClient.getInstance();
        peerConnectionClient.createPeerConnectionFactory(
                activity, peerConnectionParameters, this);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        //client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void createSocketIo(String roomId, String callerId, String calleeId) {
        try {
            IO.Options opt = new IO.Options();
            opt.query = "RoomId=" + roomId + "&CallerId=" + callerId + "&CalleeId=" + calleeId;
            socket = IO.socket("http://54.169.150.82:3000", opt);
            socket.on(Socket.EVENT_CONNECT_ERROR, onConnectionError);
            socket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectionError);
            socket.connect();
            socket.on("respDoCalling", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        JSONObject resp = new JSONObject(args[0].toString());
                        Log.d("my resp", resp.getString("params"));
                        appRtcClient.connectToRoomBySocket(roomConnectionParameters, args[0].toString());

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

            //response after SendInitMsg
            socket.on("resSavedMsg", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        JSONObject res = new JSONObject(args[0].toString());
                        Log.d("ResSavedMsg", res.getString("result"));

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

            //catch Error
            socket.on("gotError", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        JSONObject err = new JSONObject(args[0].toString());
                        mListener.onSocketIOEventFired("error:");

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (URISyntaxException ex) {
            //Toast.makeText(CallActivity.this, "uri ex", Toast.LENGTH_SHORT);
            mListener.onSocketIOEventFired("URISyntaxException: " + ex.getMessage());
        }
    }

    // Activity interfaces
    public void onPause() {
        //super.onPause();
        activityRunning = false;
        if (peerConnectionClient != null) {
            peerConnectionClient.stopVideoSource();
        }
    }


    public void onResume() {
        //super.onResume();
        activityRunning = true;
        if (peerConnectionClient != null) {
            peerConnectionClient.startVideoSource();
        }
    }

    protected void onDestroy() {
        disconnect();
//        if (logToast != null) {
//            logToast.cancel();
//        }
        activityRunning = false;
        rootEglBase.release();
        //super.onDestroy();
    }


    public interface OnPeerConnectionNSignalingEvents {
        public void onPeerConnectionEventFired(String msg);

        public void onSignalingEventFired(String msg);

        public void onSocketIOEventFired(String msg);
    }

    // CallFragment.OnCallEvents interface implementation.
    /*@Override
    public void onCallHangUp() {
        disconnect();
    }

    @Override
    public void onCameraSwitch() {
        if (peerConnectionClient != null) {
            peerConnectionClient.switchCamera();
        }
    }

    @Override
    public void onVideoScalingSwitch(ScalingType scalingType) {
        this.scalingType = scalingType;
        updateVideoView();
    }

    @Override
    public void onCaptureFormatChange(int width, int height, int framerate) {
        if (peerConnectionClient != null) {
            peerConnectionClient.changeCaptureFormat(width, height, framerate);
        }
    }*/

    // Helper functions.
    private void toggleCallControlFragmentVisibility() {
        if (!iceConnected || !callFragment.isAdded()) {
            return;
        }
        // Show/hide call control fragment
        callControlFragmentVisible = !callControlFragmentVisible;
        FragmentTransaction ft = this.activity.getFragmentManager().beginTransaction();
        if (callControlFragmentVisible) {
            ft.show(callFragment);
            ft.show(hudFragment);
        } else {
            ft.hide(callFragment);
            ft.hide(hudFragment);
        }
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    private void updateVideoView() {
        remoteRenderLayout.setPosition(REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT);
        remoteRender.setScalingType(scalingType);
        remoteRender.setMirror(false);

        if (iceConnected) {
            localRenderLayout.setPosition(
                    LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED, LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED);
            localRender.setScalingType(ScalingType.SCALE_ASPECT_FIT);
        } else {
            localRenderLayout.setPosition(
                    LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING);
            localRender.setScalingType(scalingType);
        }
        localRender.setMirror(true);

        localRender.requestLayout();
        remoteRender.requestLayout();
    }

    private void startCall() {
        if (appRtcClient == null) {
            Log.e(TAG, "AppRTC client is not allocated for a call.");
            return;
        }
        callStartedTimeMs = System.currentTimeMillis();

        // Start room connection.
//        logAndToast(getString(R.string.connecting_to,
//                roomConnectionParameters.roomUrl));
        mListener.onPeerConnectionEventFired(roomConnectionParameters.roomUrl);

        JSONObject data = new JSONObject();
        try {
            data.put("RoomId", this.roomConnectionParameters.roomId);
            data.put("CallerId", this.roomConnectionParameters.callerId);
            data.put("CalleeId", this.roomConnectionParameters.calleeId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit("doCalling", data.toString());

        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = AppRTCAudioManager.create(this.activity, new Runnable() {
                    // This method will be called each time the audio state (number and
                    // type of devices) has been changed.
                    @Override
                    public void run() {
                        onAudioManagerChangedState();
                    }
                }
        );
        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        Log.d(TAG, "Initializing the audio manager...");
        audioManager.init();
    }

    // Should be called from UI thread
    private void callConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        Log.i(TAG, "Call connected: delay=" + delta + "ms");
        if (peerConnectionClient == null || isError) {
            Log.w(TAG, "Call is connected in closed or error state");
            return;
        }
        // Update video view.
        updateVideoView();
        // Enable statistics callback.
        peerConnectionClient.enableStatsEvents(true, STAT_CALLBACK_PERIOD);
    }

    private void onAudioManagerChangedState() {
        // TODO(henrika): disable video if AppRTCAudioManager.AudioDevice.EARPIECE
        // is active.
    }

    // Disconnect from remote resources, dispose of local resources, and exit.
    public void disconnect() {
        socket.disconnect();
        activityRunning = false;
//        if (appRtcClient != null) {
//            appRtcClient.disconnectFromRoom();
//            appRtcClient = null;
//        }
        if (peerConnectionClient != null) {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }
        if (localRender != null) {
            localRender.release();
            localRender = null;
        }
        if (remoteRender != null) {
            remoteRender.release();
            remoteRender = null;
        }
        if (audioManager != null) {
            audioManager.close();
            audioManager = null;
        }
        if (iceConnected && !isError) {
            //setResult(RESULT_OK);
            this.activity.setResult(RESULT_OK);
        } else {
            //setResult(RESULT_CANCELED);
        }
        //finish();
    }

    private void disconnectWithErrorMessage(final String errorMessage) {
        if (commandLineRun || !activityRunning) {
            Log.e(TAG, "Critical error: " + errorMessage);
            disconnect();
        } else {
            disconnect();
//            new AlertDialog.Builder(this)
//                    .setTitle(getText(R.string.channel_error_title))
//                    .setMessage(errorMessage)
//                    .setCancelable(false)
//                    .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int id) {
//                            dialog.cancel();
//                            disconnect();
//                        }
//                    }).create().show();
        }
    }

    private void reportError(final String description) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
        if (!isError) {
            isError = true;
            disconnectWithErrorMessage(description);
        }
//            }
//        });
    }

    // -----Implementation of AppRTCClient.AppRTCSignalingEvents ---------------
    // All callbacks are invoked from websocket signaling looper thread and
    // are routed to UI thread.
    private void onConnectedToRoomInternal(final SignalingParameters params) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;

        signalingParameters = params;
        //logAndToast("Creating peer connection, delay=" + delta + "ms");
        mListener.onSignalingEventFired("Creating peer connection, delay=" + delta + "ms");
        Log.d(TAG, "Creating peer connection, delay=" + delta + "ms");
        peerConnectionClient.createPeerConnection(rootEglBase.getEglBaseContext(),
                localRender, remoteRender, signalingParameters);

        if (signalingParameters.initiator) {
            Log.d("is_initiator", "true");
            //logAndToast("Creating OFFER...");
            mListener.onSignalingEventFired("Creating OFFER...");
            Log.d(TAG, "Creating OFFER...");
            // Create offer. Offer SDP will be sent to answering client in
            // PeerConnectionEvents.onLocalDescription event.
            peerConnectionClient.createOffer();
        } else {
            Log.d("is_initiator", "false");
            if (params.offerSdp != null) {
                peerConnectionClient.setRemoteDescription(params.offerSdp);
                //logAndToast("Creating ANSWER...");
                mListener.onSignalingEventFired("Creating ANSWER...");
                Log.d(TAG, "Creating ANSWER...");
                // Create answer. Answer SDP will be sent to offering client in
                // PeerConnectionEvents.onLocalDescription event.
                peerConnectionClient.createAnswer();
            }
            if (params.iceCandidates != null) {
                // Add remote ICE candidates from room.
                for (IceCandidate iceCandidate : params.iceCandidates) {
                    peerConnectionClient.addRemoteIceCandidate(iceCandidate);
                }
            }
        }
    }

    @Override
    public void onConnectedToRoom(final SignalingParameters params) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
        onConnectedToRoomInternal(params);
//            }
//        });
    }

    @Override
    public void onRemoteDescription(final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
        if (peerConnectionClient == null) {
            Log.e(TAG, "Received remote SDP for non-initilized peer connection.");
            return;
        }
        //logAndToast("Received remote " + sdp.type + ", delay=" + delta + "ms");
        Log.d(TAG, "Received remote " + sdp.type + ", delay=" + delta + "ms");
        peerConnectionClient.setRemoteDescription(sdp);
        if (!signalingParameters.initiator) {
            //logAndToast("Creating ANSWER...");
            Log.d(TAG, "Creating ANSWER...");
            // Create answer. Answer SDP will be sent to offering client in
            // PeerConnectionEvents.onLocalDescription event.
            peerConnectionClient.createAnswer();
        }
//            }
//        });
    }

    @Override
    public void onRemoteIceCandidate(final IceCandidate candidate) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
        if (peerConnectionClient == null) {
            Log.e(TAG,
                    "Received ICE candidate for non-initilized peer connection.");
            return;
        }
        peerConnectionClient.addRemoteIceCandidate(candidate);
//            }
//        });
    }

    @Override
    public void onChannelClose() {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
        //logAndToast("Remote end hung up; dropping PeerConnection");
        Log.d(TAG, "Remote end hung up; dropping PeerConnection");
        disconnect();
//            }
//        });
    }

    @Override
    public void onChannelError(final String description) {
        reportError(description);
    }

    // -----Implementation of PeerConnectionClient.PeerConnectionEvents.---------
    // Send local peer connection SDP and ICE candidates to remote party.
    // All callbacks are invoked from peer connection client looper thread and
    // are routed to UI thread.
    @Override
    public void onLocalDescription(final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
        if (appRtcClient != null) {
            //logAndToast("Sending " + sdp.type + ", delay=" + delta + "ms");
            Log.d(TAG, "Sending " + sdp.type + ", delay=" + delta + "ms");
            if (signalingParameters.initiator) {
                appRtcClient.sendOfferSdp(sdp);
            } else {
                appRtcClient.sendAnswerSdp(sdp);
            }
        }
//            }
//        });
    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
        if (appRtcClient != null) {
            appRtcClient.sendLocalIceCandidate(candidate);
        }
//            }
//        });
    }

    @Override
    public void onIceConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        this.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //logAndToast("ICE connected, delay=" + delta + "ms");
                Log.d(TAG, "ICE connected, delay=" + delta + "ms");
                iceConnected = true;
                callConnected();
            }
        });
    }

    @Override
    public void onIceDisconnected() {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
        //logAndToast("ICE disconnected");
        Log.d(TAG, "ICE disconnected");
        iceConnected = false;
        disconnect();
//            }
//        });
    }

    @Override
    public void onPeerConnectionClosed() {
    }

    @Override
    public void onPeerConnectionStatsReady(final StatsReport[] reports) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
        if (!isError && iceConnected) {
            hudFragment.updateEncoderStatistics(reports);
        }
//            }
//        });
    }

    @Override
    public void onPeerConnectionError(final String description) {
        reportError(description);
    }


    //adding socketio error handling
    private Emitter.Listener onConnectionError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.e(TAG, args[0].toString());
        }
    };
}
