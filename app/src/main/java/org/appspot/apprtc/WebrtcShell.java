package org.appspot.apprtc;

/**
 * Created by c900 on 2016/4/22.
 */

import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class WebrtcShell implements AppRTCClient.SignalingEvents, PeerConnectionClient.PeerConnectionEvents{
    private Socket socket;
    private IO.Options opt;

    //private Toast logToast;
    private String TAG = "WebrtcShell";


    private PeerConnectionClient peerConnectionClient = null;
    private AppRTCClient appRtcClient;
    private AppRTCClient.SignalingParameters signalingParameters;
    private AppRTCAudioManager audioManager = null;
    private EglBase rootEglBase;
    private SurfaceViewRenderer localRender;
    private SurfaceViewRenderer remoteRender;


    private static WebrtcShell ourInstance = new WebrtcShell();

    public static WebrtcShell getInstance() {
        return ourInstance;
    }

    private WebrtcShell() {

    }

    public void init(String roomId, String callerId, String calleeId){
        opt = new IO.Options();
        opt.query = "RoomId=" + roomId + "&CallerId=" + callerId + "&CalleeId=" + calleeId;
        try {
            socket = IO.socket("http://54.169.150.82:3000", opt);
            socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    System.out.println("socket connect error:" + args[0].toString());
                }
            });
            socket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectionError);
            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void connect(){

    }

    public void disconnect(){

    }

    /*public void logAndToast(String msg) {
        Log.d(TAG, msg);
        if (logToast != null) {
            logToast.cancel();
        }
        //Looper.prepare();
        //logToast = Toast.makeText(this, msg, 0);
        //Looper.loop();
        logToast.show();
    }*/


    //adding socketio error handling
    private Emitter.Listener onConnectionError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

        }
    };

    public static interface PeerConnectionNSignalingEvents{

    }



    //PeerConnectionClient.PeerConnectionEvents
    @Override
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

    //AppRTCClient.SignalingEvents
    @Override
    public void onConnectedToRoom(final AppRTCClient.SignalingParameters params) {

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

    }
}
