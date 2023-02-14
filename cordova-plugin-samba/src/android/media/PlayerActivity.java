package net.cloudseat.smbova;

import android.app.Activity;
import android.content.res.Configuration;
import android.util.TypedValue;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

import android.media.MediaDataSource;
import android.media.MediaPlayer;
import android.media.TimedText;

import android.os.Bundle;
import android.os.Handler;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import net.cloudseat.smbova.R;

public class PlayerActivity extends Activity {

    // 接收外部传来的媒体数据源
    public static MediaDataSource dataSource;
    public static String timedTextFile;
    public static final boolean DEBUG = false;

    // 布局控件
    private View mainLayout;
    private ProgressBar loading;
    private SurfaceView surfaceView;
    private TableLayout controls;
    private ImageView audioDisc;
    private ImageButton playBtn;
    private SeekBar seekBar;
    private TextView position;
    private TextView duration;
    private TextView subtitle;
    private GestureDetector gestureDetector;

    // 媒体播放器
    private MediaPlayer mediaPlayer;
    private boolean restartOnResume;
    private boolean isSeeking;

    // 每隔 100ms 更新进度条
    private Handler progressHandler = new Handler();
    private Runnable progressThread = new Runnable() {
        @Override
        public void run() {
            if (!isSeeking) {
                int pos = mediaPlayer.getCurrentPosition();
                seekBar.setProgress(pos);
                position.setText(formatDuration(pos));
            }
            progressHandler.postDelayed(this, 100);
        }
    };

    ///////////////////////////////////////////////////////
    // Activity override methods
    ///////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        debug("Activity onCreate");
        initLayoutView();
        initMediaPlayer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        debug("Activity onPause");
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            restartOnResume = true;
        } else {
            restartOnResume = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        debug("Activity onResume");
        if (restartOnResume) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        debug("Activity onDestroy");
        progressHandler.removeCallbacks(progressThread);
        mediaPlayer.stop();
        mediaPlayer.release();
    }

    /**
     * 如果使用 GestureDetector 处理双击事件就必须重写 Activity 的 onTouchEvent 方法，否则 GestureDetector 中监听的方法都无效
     */
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return gestureDetector.onTouchEvent(e);
    }

    /**
     * 转屏回调
     * 在 plugin.xml 中增加 android:configChanges="orientation|screenSize" 配置
     * 可以防止转屏时 activity 重建，改为调用当前方法。
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        adjustViewSize();

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        } else {
            subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        }
    }

    ///////////////////////////////////////////////////////
    // Private methods
    ///////////////////////////////////////////////////////

    /**
     * 初始化布局控件
     * or: setContentView(getResources().getIdentifier("media_player", "layout", getPackageName()));
     * or: findViewById(getResources().getIdentifier("id_in_xml", "id", getPackageName()));
     */
    private void initLayoutView() {
        setContentView(R.layout.media_player);
        mainLayout = (View) findViewById(R.id.main_layout);
        loading = (ProgressBar) findViewById(R.id.loading);
        surfaceView = (SurfaceView) findViewById(R.id.video_view);
        controls = (TableLayout) findViewById(R.id.controls);
        playBtn = (ImageButton) findViewById(R.id.play_btn);
        seekBar = (SeekBar) findViewById(R.id.seek_bar);
        position = (TextView) findViewById(R.id.position);
        duration = (TextView) findViewById(R.id.duration);
        subtitle = (TextView) findViewById(R.id.subtitle);
        audioDisc = (ImageView) findViewById(R.id.audio_disc);

        // 默认隐藏控件
        controls.setVisibility(View.INVISIBLE);
        audioDisc.setVisibility(View.INVISIBLE);

        // 使用 GestureDetector 监听单击和双击事件
        gestureDetector = new GestureDetector(PlayerActivity.this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleControls();
                return true; // 必须返回 true，否则双击穿透
            }
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                togglePlayOrPause();
                return true;
            }
        });

        // 主屏幕触摸事件交给 GestureDetector 处理
        mainLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                return gestureDetector.onTouchEvent(e);
            }
        });

        // 播放/暂停按钮点击事件
        playBtn.setImageResource(R.drawable.play);
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlayOrPause();
            }
        });

        // 进度条拖动事件
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                position.setText(formatDuration(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeeking = true;
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeeking = false;
                mediaPlayer.seekTo(seekBar.getProgress());
                loading.setVisibility(View.VISIBLE);
                audioDisc.setVisibility(View.INVISIBLE);
            }
        });

        // 设置视频播放容器
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                debug("surfaceCreated");
                mediaPlayer.setDisplay(surfaceHolder);
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                debug("surfaceChanged: " + format + " | " + width + ", " + height);
            }
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                debug("surfaceDestroyed");
                mediaPlayer.setDisplay(null);
            }
        });
    }

    /**
     * 初始化媒体播放器
     */
    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setScreenOnWhilePlaying(true);
        mediaPlayer.setDataSource(dataSource);

        try {
            if (timedTextFile != null)
            mediaPlayer.addTimedTextSource(timedTextFile, MediaPlayer.MEDIA_MIMETYPE_TEXT_SUBRIP);
        } catch (IOException e) {
            console("Load timed text error.");
        }

        mediaPlayer.prepareAsync();
        // 数据预加载完成后回调
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
                progressHandler.post(progressThread);

                loading.setVisibility(View.INVISIBLE);
                audioDisc.setVisibility(View.VISIBLE);
                playBtn.setImageResource(R.drawable.pause);
                seekBar.setMax(mediaPlayer.getDuration());
                duration.setText(formatDuration(mediaPlayer.getDuration()));

                // 根据媒体信息或加载字幕或显示音频图标
                boolean hasVideo = false;
                MediaPlayer.TrackInfo[] trackInfos = mediaPlayer.getTrackInfo();
                for (int i = 0; i < trackInfos.length; i++) {
                    MediaPlayer.TrackInfo info = trackInfos[i];
                    if (info.getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_VIDEO) {
                        hasVideo = true;
                    } else
                    if (info.getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT) {
                        mediaPlayer.selectTrack(i);
                    }
                }
                if (!hasVideo) {
                    initAudioDisc();
                }
            }
        });
        // 字幕同步回调
        mediaPlayer.setOnTimedTextListener(new MediaPlayer.OnTimedTextListener() {
            @Override
            public void onTimedText(MediaPlayer mediaPlayer, TimedText text) {
                String textString = "";
                if (text != null) {
                    try {
                        byte[] bytes = text.getText().getBytes("GBK");
                        textString = new String(bytes, "utf-8");
                    } catch (Exception e) {
                        textString = text.getText();
                    }
                }
                subtitle.setText(textString);
            }
        });
        // 视频尺寸改变后回调
        mediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mediaPlayer, int width, int height) {
                debug("VideoSizeChanged: " + width + ", " + height);
                adjustViewSize();
            }
        });
        // 拖动进度寻轨完成后回调
        mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mediaPlayer) {
                loading.setVisibility(View.INVISIBLE);
                audioDisc.setVisibility(View.VISIBLE);
            }
        });
        // 播放完成后回调
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                playBtn.setImageResource(R.drawable.play);
            }
        });
        // 发生错误时回调
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                console("Media player error.");
                mediaPlayer.reset();
                return false;
            }
        });
    }

    /**
     * 初始化音频图标旋转动画
     */
    private void initAudioDisc() {
        audioDisc.setImageResource(R.drawable.disc);
        RotateAnimation rotate = new RotateAnimation(0, 360,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(3000);
        rotate.setFillAfter(true); // 结束后停止再最后一帧
        rotate.setInterpolator(new LinearInterpolator()); // 插值：线性
        rotate.setRepeatMode(Animation.RESTART); // 重复模式：从头开始
        rotate.setRepeatCount(Animation.INFINITE); // 重复次数：无限
        audioDisc.startAnimation(rotate);
        audioDisc.setVisibility(View.VISIBLE);
    }

    /**
     * 播放/暂停
     */
    private void togglePlayOrPause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playBtn.setImageResource(R.drawable.play);
        } else {
            mediaPlayer.start();
            playBtn.setImageResource(R.drawable.pause);
        }
    }

    /**
     * 显示/隐藏 播放控制面板
     */
    private void toggleControls() {
        if (controls.getVisibility() == View.INVISIBLE) {
            controls.setAlpha(0f);
            controls.setVisibility(View.VISIBLE);
            controls.animate().alpha(1f).setDuration(300).setListener(null);
        } else {
            controls.animate().alpha(0f).setDuration(300).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    controls.setVisibility(View.INVISIBLE);
                }
            });
        }
    }

    /**
     * 调整视频容器大小以自适应屏幕
     */
    private void adjustViewSize() {
        WindowManager wm = getWindowManager();
        int screenWidth = wm.getDefaultDisplay().getWidth();
        int screenHeight = wm.getDefaultDisplay().getHeight();
        int videoWidth = mediaPlayer.getVideoWidth();
        int videoHeight = mediaPlayer.getVideoHeight();
        double videoRatio = (float) videoWidth / videoHeight;
        double screenRatio = (float) screenWidth / screenHeight;

        ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();
        if (videoRatio > screenRatio) {
            lp.width = screenWidth;
            lp.height = (int)((double)lp.width / videoRatio);
        } else {
            lp.height = screenHeight;
            lp.width = (int)((double)lp.height * videoRatio);
        }
        surfaceView.setLayoutParams(lp);
    }

    ///////////////////////////////////////////////////////
    // Utils
    ///////////////////////////////////////////////////////

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        String m = String.format("%02d", minutes %= 60);
        String s = String.format("%02d", seconds %= 60);
        if (hours > 0) {
            return hours + ":" + m + ":" + s;
        }
        return m + ":" + s;
    }

    private void debug(String text) {
        if (DEBUG) console(text);
    }

    private void console(String text) {
        Toast.makeText(PlayerActivity.this, text, Toast.LENGTH_LONG).show();
    }

}
