package org.anyrtc.p2p.activity;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.anyrtc.common.enums.AnyRTCP2PMediaType;
import org.anyrtc.common.enums.AnyRTCVideoMode;
import org.anyrtc.common.utils.AnyRTCAudioManager;
import org.anyrtc.p2p.P2PApplication;
import org.anyrtc.p2p.R;
import org.anyrtc.p2p.model.CallRecord;
import org.anyrtc.p2p.widgets.CircleImageView;
import org.anyrtc.p2p.widgets.RTCVideoView;
import org.anyrtc.rtp2pcall.AnyRTCP2PEngine;
import org.anyrtc.rtp2pcall.RTP2PCallHelper;
import org.anyrtc.rtp2pcall.RTP2PCallKit;
import org.webrtc.VideoRenderer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class P2PCallActivity extends BaseActivity implements Chronometer.OnChronometerTickListener,RTP2PCallHelper {
    private boolean misCalling = false;
    public static boolean IS_CALLING = false;

    private RTP2PCallKit mP2PKit;
    private RTCVideoView mVideoView;
    private String mUserId, mCallid;
    private boolean mIsCalled; //是否是被呼叫
    private AnyRTCP2PMediaType mP2PModel;
    MediaPlayer player;
    Chronometer chronometer;
    SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
    private long startTime;
    private String DisPlayTime="00:00";
    private AnyRTCAudioManager rtcAudioManager;
    private Button btnChangeMode;
    private ImageButton btn_camare, ibtn_accept, ibtn_audio, ibtn_hang_up, ibtn_video, ibtn_voice;
    private TextView tvUserid, tvType, tvTime, tvstate;
    private ImageView ivBg;
    private View space;
    private CircleImageView iv_icon;

    private int state=1;

    CallRecord callRecord=new CallRecord();
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (ibtn_audio.getVisibility() == View.GONE) {
                mP2PKit.endCall(mCallid);
                finishAnimActivity();
            } else {
                if (misCalling) {
                    mP2PKit.endCall(mCallid);
                }
                finish();
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /**
         * 停止播放铃音
         */
        stopRing();

        if (mP2PKit != null) {
            mVideoView.OnRtcRemoveLocalRender();
            mVideoView.OnRtcRemoveRemoteRender(mCallid);
            mP2PKit.endCall(mCallid);
        }
        if (chronometer != null) {
            chronometer.stop();
        }
        IS_CALLING = false;
        if (misCalling) {
            callRecord.setTime(DisPlayTime);
        }else {
            callRecord.setTime("00:00");
        }
        callRecord.setMode(mP2PModel.level);
        P2PApplication.the().getmDBDao().Add(callRecord);
        misCalling = false;
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_p2pcall;
    }

    @Override
    public void initView(Bundle savedInstanceState) {
        isCallActivity=true;
        space = findViewById(R.id.view_space);
        mImmersionBar.titleBar(space).init();
        chronometer = (Chronometer) findViewById(R.id.chronometer);
        btn_camare = (ImageButton) findViewById(R.id.btn_camare);
        ibtn_accept = (ImageButton) findViewById(R.id.ibtn_accept);
        ibtn_audio = (ImageButton) findViewById(R.id.ibtn_audio);
        ibtn_hang_up = (ImageButton) findViewById(R.id.ibtn_hang_up);
        ibtn_video = (ImageButton) findViewById(R.id.ibtn_video);
        tvUserid = (TextView) findViewById(R.id.tv_userid);
        tvType = (TextView) findViewById(R.id.tv_type);
        tvTime = (TextView) findViewById(R.id.tv_time);
        ivBg = (ImageView) findViewById(R.id.iv_background);
        iv_icon = (CircleImageView) findViewById(R.id.iv_icon);
        ibtn_voice = (ImageButton) findViewById(R.id.ibtn_voice);
        tvstate = (TextView) findViewById(R.id.tv_state);
        btnChangeMode= (Button) findViewById(R.id.btn_change_audio);
        ibtn_voice.setSelected(true);
        chronometer.setOnChronometerTickListener(this);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        startTime = System.currentTimeMillis();

        rtcAudioManager = AnyRTCAudioManager.create(getApplicationContext(), new Runnable() {
            @Override
            public void run() {
            }
        });
        rtcAudioManager.init();
        rtcAudioManager.setAudioDevice(AnyRTCAudioManager.AudioDevice.SPEAKER_PHONE);

        mUserId = getIntent().getExtras().getString("userid");
        mCallid = getIntent().getExtras().getString("callid");
        int p2p_mode = getIntent().getExtras().getInt("p2p_mode");
        mIsCalled = getIntent().getExtras().getBoolean("p2p_push");
        Log.d("p2pCallBack misPush", mIsCalled + "");
        tvUserid.setText(mCallid + "");
        if (mIsCalled) {
            startRing();
        }
        if (p2p_mode == 0) {
            //视频呼叫
            mP2PModel = AnyRTCP2PMediaType.RT_P2P_CALL_VIDEO.RT_P2P_CALL_VIDEO;
            if (mIsCalled) {
                beCalledByOther_Video();
            } else {
                callOther_Video();
            }
        } else if (p2p_mode == 1) {
            //视频PRO呼叫（被呼叫者可以先看到呼叫者的视频）
            mP2PModel =  AnyRTCP2PMediaType.RT_P2P_CALL_VIDEO.RT_P2P_CALL_VIDEO_PRO;
            if (mIsCalled) {
                beCalledByOther_Video_pre();
            } else {
                callOther_Video();
            }
        } else if (p2p_mode == 2) {
            //音频呼叫
            mP2PModel =  AnyRTCP2PMediaType.RT_P2P_CALL_VIDEO.RT_P2P_CALL_AUDIO;
            if (mIsCalled) {
                beCalledByOther_Audio();
            } else {
                callOther_Audio();
            }

        }else if (p2p_mode ==3){
            mP2PModel =  AnyRTCP2PMediaType.RT_P2P_CALL_VIDEO.RT_P2P_CALL_MONITOR;
            if (mIsCalled) {
                beCalledByOther_Watch();
            } else {
                callOther_Video();
            }

        }

        mP2PKit = P2PApplication.the().getmP2pKit();
        mP2PKit.setP2PCallHelper(this);
        mVideoView = new RTCVideoView((RelativeLayout) findViewById(R.id.rl_rtc_videos), this, AnyRTCP2PEngine.Inst().Egl());
        if (mP2PModel !=  AnyRTCP2PMediaType.RT_P2P_CALL_VIDEO.RT_P2P_CALL_AUDIO) {
//            if (mP2PModel == AnyRTCP2PMediaType.RT_P2P_CALL_MONITOR) {
////                if (mIsCalled) {
                    VideoRenderer render = mVideoView.OnRtcOpenLocalRender();
                    mP2PKit.setLocalVideoCapturer(render.GetRenderPointer(), true, AnyRTCVideoMode.AnyRTC_Video_HD);
//                }
//            } else {
//                VideoRenderer render = mVideoView.OnRtcOpenLocalRender();
//                mP2PKit.setLocalVideoCapturer(render.GetRenderPointer(), true, AnyRTCVideoMode.AnyRTC_Video_HD);
//            }

            }
            if (mIsCalled) {

            } else {
                if (mP2PKit != null) {
                    mP2PKit.makeCall(mCallid, mP2PModel, "{userid: " + mUserId + "}");
                }
                callRecord.setState(3);
            }
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日");
            Date curDate = new Date(System.currentTimeMillis());//获取当前时间
            String date = formatter.format(curDate);
            callRecord.setData(date);
            callRecord.setId(mCallid);
    }

    public void OnBtnClicked(View btn) {
        switch (btn.getId()) {
            case R.id.ibtn_hang_up:
                if (mP2PKit == null) {
                    return;
                }


                    if (misCalling) {//正在通话
                        mP2PKit.endCall(mCallid);
                        finishAnimActivity();
                    } else {//还未打电话    可分为主动打 （挂断）  被呼叫（拒绝）

                            if (mIsCalled){
                                mP2PKit.rejectCall(mCallid);
                                stopRing();
                                finishAnimActivity();
                                state=2;
                                callRecord.setState(state);
                            }else {
                                mP2PKit.endCall(mCallid);
                                finishAnimActivity();
                            }

                    }

                break;
            case R.id.ibtn_accept:
                state=0;
                callRecord.setState(state);
                mP2PKit.accpetCall(mCallid);
                if (mP2PModel == AnyRTCP2PMediaType.RT_P2P_CALL_AUDIO) {
                    startAudioCall();
                } else if (mP2PModel == AnyRTCP2PMediaType.RT_P2P_CALL_VIDEO) {
                    startVideoCall();
                } else if (mP2PModel== AnyRTCP2PMediaType.RT_P2P_CALL_VIDEO_PRO){
                    mVideoView.preView2Normol();
                    startVideoCall();
                }else if (mP2PModel== AnyRTCP2PMediaType.RT_P2P_CALL_MONITOR){
                    startWatchCall();
                }
                break;
            case R.id.ibtn_audio:
                if (ibtn_audio.isSelected()) {
                    ibtn_audio.setSelected(false);
                    mP2PKit.setLocalAudioEnable(true);
                } else {
                    ibtn_audio.setSelected(true);
                    mP2PKit.setLocalAudioEnable(false);
                }
                break;
            case R.id.ibtn_video:
                if (ibtn_video.isSelected()) {
                    mP2PKit.setLocalVideoEnable(true);
                    ibtn_video.setSelected(false);
                } else {
                    ibtn_video.setSelected(true);
                    mP2PKit.setLocalVideoEnable(false);
                }
                break;
            case R.id.btn_camare:
                mP2PKit.switchCamera();
                if (btn_camare.isSelected()) {
                    btn_camare.setSelected(false);
                } else {
                    btn_camare.setSelected(true);
                }
                break;
            case R.id.ibtn_voice:
                if (ibtn_voice.isSelected()) {
                    setSpeakerOn(false);
                    ibtn_voice.setSelected(false);
                } else {
                    ibtn_voice.setSelected(true);
                    setSpeakerOn(true);
                }
                break;
            case R.id.btn_change_audio:
                if (mP2PKit!=null){
                    mP2PKit.swtichToAudioMode();
                    startAudioCall();
                    mP2PModel= AnyRTCP2PMediaType.RT_P2P_CALL_AUDIO;
                    mVideoView.OnRtcRemoveLocalRender();
                }
                break;
        }
    }










    /**
     * 播放铃音
     */
    private void startRing() {
        try {
            player = MediaPlayer.create(this, R.raw.video_request);
            player.setLooping(true);//循环播放
            player.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止播放铃音
     */
    private void stopRing() {
        try {
            player.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onChronometerTick(Chronometer chronometer) {
        long tp = System.currentTimeMillis();
        DisPlayTime = sdf.format(new Date(tp - startTime));
        tvTime.setText(DisPlayTime);
    }


    public void callOther_Video() {//视频呼叫别人
        tvType.setText("等待对方接听");
        if (chronometer != null) {
            chronometer.start();
        }
    }

    public void beCalledByOther_Video() {//被别人视频频呼叫
        tvType.setText("视频来电");
        ibtn_accept.setVisibility(View.VISIBLE);
        tvTime.setVisibility(View.GONE);
    }

    public void beCalledByOther_Video_pre() {//被别人优先视频频呼叫
        tvType.setText("优先视频来电");
        iv_icon.setVisibility(View.GONE);
        ivBg.setVisibility(View.GONE);
        tvTime.setVisibility(View.GONE);
    }

    public void callOther_Audio() {//音频呼叫别人
        tvType.setText("等待对方接听");
        if (chronometer != null) {
            chronometer.start();
        }
    }

    public void beCalledByOther_Audio() {//被别人音频呼叫
        tvType.setText("音频来电");
        iv_icon.setVisibility(View.VISIBLE);
        ivBg.setVisibility(View.VISIBLE);
        ibtn_accept.setVisibility(View.VISIBLE);
        tvTime.setVisibility(View.GONE);
    }

    public void beCalledByOther_Watch() {//被别人监测模式呼叫
        tvType.setText("监看模式来电");
        ibtn_accept.setVisibility(View.VISIBLE);
        tvTime.setVisibility(View.GONE);
    }

    public void startAudioCall() {//开始音频通话
        misCalling = true;
        stopRing();
        iv_icon.setVisibility(View.VISIBLE);
        ivBg.setVisibility(View.VISIBLE);
        btnChangeMode.setVisibility(View.GONE);
        tvTime.setVisibility(View.VISIBLE);
        tvType.setVisibility(View.VISIBLE);
        tvType.setText("正在通话中...");
        startTime = System.currentTimeMillis();
        btn_camare.setVisibility(View.GONE);
        chronometer.start();
        ibtn_accept.setVisibility(View.GONE);
        ibtn_voice.setVisibility(View.VISIBLE);
        ibtn_video.setVisibility(View.GONE);
        ibtn_audio.setVisibility(View.VISIBLE);

    }

    public void startVideoCall() {//开始视频通话
        misCalling = true;
        stopRing();
        if (mIsCalled){
            btnChangeMode.setVisibility(View.GONE);
        }else {
            if (mP2PModel== AnyRTCP2PMediaType.RT_P2P_CALL_MONITOR){
                btnChangeMode.setVisibility(View.GONE);
            }else {
                btnChangeMode.setVisibility(View.VISIBLE);
            }
        }
        tvUserid.setVisibility(View.GONE);
        btn_camare.setVisibility(View.VISIBLE);
        iv_icon.setVisibility(View.GONE);
        ivBg.setVisibility(View.GONE);
        tvTime.setVisibility(View.GONE);
        tvType.setVisibility(View.GONE);
        ibtn_accept.setVisibility(View.GONE);
        ibtn_audio.setVisibility(View.VISIBLE);
        ibtn_video.setVisibility(View.VISIBLE);
        startTime = System.currentTimeMillis();
        if (chronometer != null) {
            chronometer.start();
        }
    }


    public void startWatchCall() {//开始监看通话
        if (mIsCalled) {
            mVideoView.removeLocalRenderBg();
        }
        misCalling = true;
        stopRing();
        if (mIsCalled){
            ibtn_audio.setVisibility(View.VISIBLE);
            ibtn_video.setVisibility(View.VISIBLE);
        }else {
            ibtn_audio.setVisibility(View.GONE);
            ibtn_video.setVisibility(View.GONE);
        }
        btnChangeMode.setVisibility(View.GONE);
        tvUserid.setVisibility(View.GONE);
        btn_camare.setVisibility(View.GONE);
        iv_icon.setVisibility(View.GONE);
        ivBg.setVisibility(View.GONE);
        tvTime.setVisibility(View.GONE);
        tvType.setVisibility(View.GONE);
        ibtn_accept.setVisibility(View.GONE);
        startTime = System.currentTimeMillis();
        if (chronometer != null) {
            chronometer.start();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        IS_CALLING = true;
    }

    /**
     * p2p连接成功
     */
    @Override
    public void onConnected() {
        P2PCallActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                Log.d("p2pCallBack", "OnConnected 连接成功");
                if (tvstate != null) {
                    tvstate.setText("连接成功");
                }
//                Toast.makeText(P2PCallActivity.this, "连接成功！", Toast.LENGTH_LONG).show();

            }
        });
    }

    /**
     * p2p断开连接
     * @param nErrCode
     */
    @Override
    public void onDisconnect(final int nErrCode) {
        P2PCallActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("p2pCallBack", "OnDisconnect 连接断开 Code=" + nErrCode);
                if (nErrCode == 0) {
                    Toast.makeText(P2PCallActivity.this, "连接断开", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(P2PCallActivity.this, "" + nErrCode, Toast.LENGTH_LONG).show();
                }
//                ((Button)findViewById(R.id.btn_p2p)).setText(R.string.str_p2p_call);
                finishAnimActivity();
            }
        });
    }

    /**
     * 其他人呼叫回掉
     * @param strPeerUserId 呼叫人ID
     * @param nCallMode 呼叫模式
     * @param strUserData 呼叫人自定义数据
     */
    @Override
    public void onRTCMakeCall(final String strPeerUserId, final int nCallMode, final String strUserData) {
        P2PCallActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("p2pCallBack", "OnRTCMakeCall strPeerUserId=" + strPeerUserId + "nCallMode=" + nCallMode + "strUserData=" + strUserData);
            }
        });
    }

    /**
     * 被呼叫人接收你的呼叫
     * @param strPeerUserId 被呼叫人ID
     */
    @Override
    public void onRTCAcceptCall(final String strPeerUserId) {
        P2PCallActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("p2pCallBack", "OnRTCAcceptCall strPeerUserId=" + strPeerUserId);
                if (mP2PModel == AnyRTCP2PMediaType.RT_P2P_CALL_AUDIO) {
                    startAudioCall();
                } else if (mP2PModel== AnyRTCP2PMediaType.RT_P2P_CALL_MONITOR){
                    startWatchCall();

                }else {
                    startVideoCall();
                }
            }
        });
    }

    /**
     * 被呼叫人拒绝你的呼叫请求
     * @param strPeerUserId 被呼叫人ID
     * @param errCode 状态码
     */
    @Override
    public void onRTCRejectCall(final String strPeerUserId, int errCode) {
        P2PCallActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("p2pCallBack", "OnRTCRejectCall strPeerUserId=" + strPeerUserId);
                Toast.makeText(P2PCallActivity.this, "对方已拒绝！", Toast.LENGTH_LONG).show();
                finish();
            }
        });

    }

    /**
     * 通话结束
     * @param strPeerUserId 对方ID
     * @param nErrCode 状态码
     */
    @Override
    public void onRTCEndCall(final String strPeerUserId, final int nErrCode) {
        P2PCallActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("p2pCallBack", "OnRTCEndCall strPeerUserId=" + strPeerUserId + "errCode=" + nErrCode);
                stopRing();
                String notification = "对方已挂断！";
                if (nErrCode == 800) {
                    notification = "对方正忙！";
                } else if (nErrCode == 801) {

                    notification = "对方不在线！";
                } else if (nErrCode == 802) {
                    notification = "不能呼叫自己！";

                } else if (nErrCode == 803) {
                    notification = "通话中对方意外掉线！";

                } else if (nErrCode == 804) {
                    notification = "对方异常导致(如：重复登录帐号将此前的帐号踢出)！";

                } else if (nErrCode == 805) {
                    notification = "呼叫超时！";
                }
                Toast.makeText(P2PCallActivity.this, notification, Toast.LENGTH_LONG).show();
                finishAnimActivity();
            }
        });
    }

    /**
     * 对方由视频切换至音频模式
     */
    @Override
    public void onRTCSwithToAudioMode() {
        P2PCallActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("p2pCallBack", "onRTCSwithToAudioMode");
                startAudioCall();
                mP2PModel= AnyRTCP2PMediaType.RT_P2P_CALL_AUDIO;
                mP2PKit.setRTCVideoRender(mCallid, 0);
                mVideoView.OnRtcRemoveLocalRender();
                mVideoView.OnRtcRemoveRemoteRender(mCallid);
            }
        });
    }

    /**
     * 收到对方消息
     * @param strPeerUserId
     * @param strMessage
     */
    @Override
    public void onRTCUserMessage(String strPeerUserId, String strMessage) {
        P2PCallActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("p2pCallBack", "onRTCUserMessage");
            }
        });
    }

    /**
     * 对方视频即将显示
     * @param strDevId
     */
    @Override
    public void onRTCOpenVideoRender(final String strDevId) {
        P2PCallActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("p2pCallBack", "OnRTCOpenVideoRender OnRTCOpenVideoRender=" + strDevId);
                VideoRenderer render = null;
                if (mP2PModel == AnyRTCP2PMediaType.RT_P2P_CALL_VIDEO) {
                    render = mVideoView.OnRtcOpenRemoteRender(strDevId);
                } else if (mP2PModel == AnyRTCP2PMediaType.RT_P2P_CALL_VIDEO_PRO) {
                    if (mIsCalled) {
                        render = mVideoView.OnRtcOpenPreViewRender(strDevId);
                    } else {
                        render = mVideoView.OnRtcOpenRemoteRender(strDevId);
                    }

                }else if (mP2PModel== AnyRTCP2PMediaType.RT_P2P_CALL_MONITOR){
                    render = mVideoView.OnRtcOpenRemoteRender(strDevId);
                }
                if (null != render) {
                    mP2PKit.setRTCVideoRender(strDevId, render.GetRenderPointer());
                }
                if (mP2PModel == AnyRTCP2PMediaType.RT_P2P_CALL_VIDEO_PRO){
                    if (mIsCalled) {
                        ibtn_accept.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
    }

    /**
     * 对方视频关闭
     * @param strDevId
     */
    @Override
    public void onRTCCloseVideoRender(final String strDevId) {
        P2PCallActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("p2pCallBack", "OnRTCCloseVideoRender strDevId=" + strDevId);
                if (null != mP2PKit) {
                    mP2PKit.setRTCVideoRender(strDevId, 0);
                    mVideoView.OnRtcRemoveRemoteRender(strDevId);
                }
                finish();
            }
        });
    }
    public void setSpeakerOn(boolean bOpen) {
        if (rtcAudioManager!=null) {
            if (bOpen) {
                rtcAudioManager.setAudioDevice(AnyRTCAudioManager.AudioDevice.SPEAKER_PHONE);
            } else {
                rtcAudioManager.setAudioDevice(AnyRTCAudioManager.AudioDevice.EARPIECE);
            }
        }
    }


}
