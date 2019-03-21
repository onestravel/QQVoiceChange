package cn.onestravel.ndk.qqvoicechange;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.fmod.core.EffectUtils;
import org.fmod.core.FmodUtils;

import cn.onestravel.ndk.qqvoicechange.recordutils.AudioRecordUtils;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener, AudioRecordUtils.OnAudioStatusUpdateListener {
    private static final int RECORD_START = 0;
    private static final int RECORD_FINISH = 1;
    private static final int RECORD_CANCEL = 2;
    private static final int RECORD_ERROR = 3;
    private FmodUtils fmodUtils = FmodUtils.getInstance();
    private ImageButton recordBtn;
    private LinearLayout recordLl;
    private TextView recordTimeTv;
    private AudioRecordUtils audioRecordUtils;
    private EffectUtils effectUtils;
    private MyHandler handler = new MyHandler();

    private boolean recording = false;
    private String recordFilePath;
    private long mTime = 0;
    private boolean permission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        org.fmod.FMOD.init(this);
        audioRecordUtils = AudioRecordUtils.getInstance();
        audioRecordUtils.setOnAudioStatusUpdateListener(this);
        recordBtn = findViewById(R.id.recordBtn);
        recordLl = findViewById(R.id.recordLl);
        recordTimeTv = findViewById(R.id.recordTimeTv);
        recordBtn.setOnTouchListener(this);
        effectUtils = new EffectUtils();
        requestPermission();
    }

    /**
     * 获取权限
     */
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] perms = {"android.permission.RECORD_AUDIO", "android.permission.WRITE_EXTERNAL_STORAGE"};
            if (checkSelfPermission(perms[0]) == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(perms[1]) == PackageManager.PERMISSION_DENIED) {
                permission = false;
                requestPermissions(perms, 200);
            } else {
                permission = true;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        org.fmod.FMOD.close();
        handler = null;
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                permission = true;
            }
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (permission) {
                    handler.sendEmptyMessage(RECORD_START);
                } else {
                    Toast.makeText(this, "请先同意录音和存储功能权限后使用", Toast.LENGTH_SHORT).show();
                    requestPermission();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (recording) {
                    if (mTime < 1000) {
                        handler.sendEmptyMessage(RECORD_CANCEL);
                    }

                    handler.sendEmptyMessage(RECORD_FINISH);
                } else {
                    super.onTouchEvent(motionEvent);
                }
                break;
        }
        return true;
    }


    /**
     * 按钮点击事件
     *
     * @param view
     */
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.reRecordTv:
                recordTimeTv.setText("长按开始录音");
                recordLl.setVisibility(View.VISIBLE);
                break;
            case R.id.normalLL:
                effectUtils.effect(recordFilePath, EffectUtils.MODE_NORMAL);
                break;
            case R.id.luoliLL:
                effectUtils.effect(recordFilePath, EffectUtils.MODE_LUOLI);
                break;
            case R.id.dashuLL:
                effectUtils.effect(recordFilePath, EffectUtils.MODE_DASHU);
                break;
            case R.id.gaoguaiLL:
                effectUtils.effect(recordFilePath, EffectUtils.MODE_GAOGUAI);
                break;
            case R.id.konglingLL:
                effectUtils.effect(recordFilePath, EffectUtils.MODE_KONGLING);
                break;
            case R.id.jingsongLL:
                effectUtils.effect(recordFilePath, EffectUtils.MODE_JINGSONG);
                break;
        }
    }

    @Override
    public void onUpdate(double db, AudioRecordUtils.RecordTimes times) {
        this.mTime = times.getTime();
        recordTimeTv.setText("... " + times.getFormatTime("mm:ss") + " ...");
    }

    @Override
    public void onFinish(float seconds, String filePath) {
        recordFilePath = filePath;
        recordLl.setVisibility(View.GONE);
    }


    public class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case RECORD_START:
                    try {
                        audioRecordUtils.startRecordAndFile();
                        recording = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        recording = false;
                        requestPermission();
                    }
                    break;
                case RECORD_FINISH:
                    recording = false;
                    audioRecordUtils.stopRecordAndFile();
                    break;
                case RECORD_CANCEL:
                    audioRecordUtils.stopRecordAndFile();
                    recording = false;
                    break;
            }
        }
    }

}
