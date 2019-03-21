package cn.onestravel.ndk.qqvoicechange.recordutils;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * 录音
 * Created by fan on 2016/6/23.
 */
public class AudioRecordUtils {
    private static AudioRecordUtils mInstance;
    // 缓冲区字节大小
    private int bufferSizeInBytes = 0;

    //AudioName裸音频数据文件 ，麦克风
    private String AudioName = "";

    //NewAudioName可播放的音频文件
    private String newAudioPath = "";

    private AudioRecord audioRecord;
    private boolean isRecord = false;// 设置正在录制的状态
    private RecordTimes recordTimes;
    private Thread thread;
    private OnAudioStatusUpdateListener onAudioStatusUpdateListener;
    private static final long SPACE = 1000;
    private double volume;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {

            }
        }
    };


    private AudioRecordUtils() {
        newAudioPath = AudioFileUtils.getWavFilePath();
    }

    private AudioRecordUtils(String newAudioPath) {
        this.newAudioPath = AudioFileUtils.getWavFilePath();
    }

    public synchronized static AudioRecordUtils getInstance() {
        if (mInstance == null)
            mInstance = new AudioRecordUtils();
        return mInstance;
    }

    public synchronized static AudioRecordUtils getInstance(String newAudioPath) {
        if (mInstance == null)
            mInstance = new AudioRecordUtils(newAudioPath);
        return mInstance;
    }

    public int startRecordAndFile() {
        //判断是否有外部存储设备sdcard
        if (AudioFileUtils.isSdcardExit()) {
            if (isRecord) {
                return ErrorCode.E_STATE_RECODING;
            } else {
                if (audioRecord == null) {
                    creatAudioRecord();
                }
                recordTimes = new RecordTimes(0);
                volume = 0;
                bufferSizeInBytes = 0;
                audioRecord.startRecording();
                // 让录制状态为true
                isRecord = true;
                // 开启音频文件写入线程
                thread = new Thread(new AudioRecordRunnable());
                thread.start();
                mHandler.post(new TimesRunnable());
                return ErrorCode.SUCCESS;
            }

        } else {
            return ErrorCode.E_NOSDCARD;
        }

    }

    public void stopRecordAndFile() {
        close();
    }


    public long getRecordFileSize() {
        return AudioFileUtils.getFileSize(newAudioPath);
    }

    public String getRecordFilePath() {
        return newAudioPath;
    }

    public void setOnAudioStatusUpdateListener(OnAudioStatusUpdateListener onAudioStatusUpdateListener) {
        this.onAudioStatusUpdateListener = onAudioStatusUpdateListener;
    }

    private void close() {
        if (recordTimes.getTime() < 1000) {
            AudioFileUtils.deleteFile(AudioFileUtils.getAMRFilePath());
            AudioFileUtils.deleteFile(AudioFileUtils.getWavFilePath());
            AudioFileUtils.deleteFile(AudioFileUtils.getRawFilePath());
        } else {
            if (onAudioStatusUpdateListener != null) {
                onAudioStatusUpdateListener.onFinish(recordTimes.time / 1000, AudioFileUtils.getWavFilePath());
            }
        }
        if (thread != null) {
            thread = null;
        }
        if (audioRecord != null) {
            System.out.println("stopRecord");
            isRecord = false;//停止文件写入
            recordTimes = null;
            volume = 0;
            bufferSizeInBytes = 0;
            audioRecord.stop();
            audioRecord.release();//释放资源
            audioRecord = null;
        }
    }


    private void creatAudioRecord() {
        // 获取音频文件路径
        AudioName = AudioFileUtils.getRawFilePath();


        // 获得缓冲区字节大小
        bufferSizeInBytes = AudioRecord.getMinBufferSize(AudioFileUtils.AUDIO_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);

        // 创建AudioRecord对象
        audioRecord = new AudioRecord(AudioFileUtils.AUDIO_INPUT, AudioFileUtils.AUDIO_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes);
    }


    class AudioRecordRunnable implements Runnable {

        @Override
        public void run() {
            writeDateTOFile();//往文件中写入裸数据
            copyWaveFile(AudioName, newAudioPath);//给裸数据加上头文件

        }
    }

    /**
     * 这里将数据写入文件，但是并不能播放，因为AudioRecord获得的音频是原始的裸音频，
     * 如果需要播放就必须加入一些格式或者编码的头信息。但是这样的好处就是你可以对音频的 裸数据进行处理，比如你要做一个爱说话的TOM
     * 猫在这里就进行音频的处理，然后重新封装 所以说这样得到的音频比较容易做一些音频的处理。
     */
    private void writeDateTOFile() {
        // new一个byte数组用来存一些字节数据，大小为缓冲区大小
        byte[] audiodata = new byte[bufferSizeInBytes];
        FileOutputStream fos = null;
        int readsize = 0;
        try {
            File pathFile = new File(AudioFileUtils.getFileBasePath());
            if (!pathFile.exists()) {
                pathFile.mkdirs();
            }
            File file = new File(AudioName);
            if (file.exists()) {
                file.delete();
            }
            fos = new FileOutputStream(file);// 建立一个可存取字节的文件
        } catch (Exception e) {
            e.printStackTrace();
        }
        long startTime = System.currentTimeMillis();
        while (isRecord == true) {
            readsize = audioRecord.read(audiodata, 0, bufferSizeInBytes);
            if (AudioRecord.ERROR_INVALID_OPERATION != readsize && fos != null) {
                try {
                    fos.write(audiodata);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            long v = 0;

            // 将 buffer 内容取出，进行平方和运算
            for (int i = 0; i < audiodata.length; i++) {
                v += audiodata[i] * audiodata[i];
            }

            // 平方和除以数据总长度，得到音量大小。
            double mean = v / (double) readsize;
            volume = 10 * Math.log10(mean);
            long currentTime = System.currentTimeMillis();
            long time = currentTime - startTime;
            recordTimes.setTime(time);
            mHandler.postDelayed(new TimesRunnable(), SPACE);
        }
        try {
            if (fos != null)
                fos.close();// 关闭写入流
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 这里得到可播放的音频文件
    private void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = AudioFileUtils.AUDIO_SAMPLE_RATE;
        int channels = 2;
        long byteRate = 16 * AudioFileUtils.AUDIO_SAMPLE_RATE * channels / 8;
        byte[] data = new byte[bufferSizeInBytes];
        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 这里提供一个头信息。插入这些信息就可以得到可以播放的文件。
     * 为我为啥插入这44个字节，这个还真没深入研究，不过你随便打开一个wav
     * 音频的文件，可以发现前面的头文件可以说基本一样哦。每种格式的文件都有
     * 自己特有的头文件。
     */
    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }


    public class TimesRunnable implements Runnable {
        @Override
        public void run() {
            if (onAudioStatusUpdateListener != null && recordTimes != null) {
                onAudioStatusUpdateListener.onUpdate(volume, recordTimes);
            }
        }
    }

    public static class AudioFile {
        private String rawFilepath;
        private String wavFilepath;
        private String amrFilepath;

        public String getRawFilepath() {
            return rawFilepath;
        }

        public void setRawFilepath(String rawFilepath) {
            this.rawFilepath = rawFilepath;
        }

        public String getWavFilepath() {
            return wavFilepath;
        }

        public void setWavFilepath(String wavFilepath) {
            this.wavFilepath = wavFilepath;
        }

        public String getAmrFilepath() {
            return amrFilepath;
        }

        public void setAmrFilepath(String amrFilepath) {
            this.amrFilepath = amrFilepath;
        }
    }

    public static class RecordTimes {
        private long time;

        public RecordTimes(long time) {
            this.time = time;

        }

        public void setTime(long time) {
            this.time = time;
            Log.e("TIMES", "fff   " + time);
            Log.e("TIMES", "fff   " + format("mm:ss"));
        }

        public long getTime() {
            return time;
        }

        public String getFormatTime(String pattern) {
            return format(pattern);
        }

        private String format(String pattern) {
            SimpleDateFormat formatter = new SimpleDateFormat(pattern);
            formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
            String hms = formatter.format(time);
            return hms;
        }
    }

    public interface OnAudioStatusUpdateListener {
        void onUpdate(double db, RecordTimes times);

        void onFinish(float seconds, String filePath);
    }
}
