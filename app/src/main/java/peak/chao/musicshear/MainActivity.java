package peak.chao.musicshear;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //AudioEditUtil.cutAudio(Audio.createAudioFromFile(new File(getSaveLocation() + "/zf.mp3")), 53, 100);
                    String fenLiData = CaoZuoMp3Utils.fenLiData(getSaveLocation() + "/zf.mp3");
                    final List<Integer> list = CaoZuoMp3Utils.initMP3Frame(fenLiData);
                    final String path = CaoZuoMp3Utils.CutingMp3(fenLiData, getSaveLocation() + "/jq.mp3", list, 53, 58);
                    final File file = new File(fenLiData);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //clip(getSaveLocation() + "/zf.mp3", getSaveLocation() + "/jq.mp3", 53, 100);
            }
        }).start();
    }

    private String getSaveLocation() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    //适当的调整SAMPLE_SIZE可以更加精确的裁剪音乐
    private static final int SAMPLE_SIZE = 1024 * 200;

    public static boolean clip(String inputPath, String outputPath, int start, int end) {
        MediaExtractor extractor = null;
        BufferedOutputStream outputStream = null;
        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(inputPath);
            int track = getAudioTrack(extractor);
            if (track < 0) {
                return false;
            }
            //选择音频轨道
            extractor.selectTrack(track);
            outputStream = new BufferedOutputStream(
                    new FileOutputStream(outputPath), SAMPLE_SIZE);
            start = start * 1000;
            end = end * 1000;
            //跳至开始裁剪位置
            extractor.seekTo(start, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            while (true) {
                ByteBuffer buffer = ByteBuffer.allocate(SAMPLE_SIZE);
                int sampleSize = extractor.readSampleData(buffer, 0);
                long timeStamp = extractor.getSampleTime();
                // >= 1000000是要裁剪停止和指定的裁剪结尾不小于1秒，否则可能产生需要9秒音频
                //裁剪到只有8.6秒，大多数音乐播放器是向下取整，这样对于播放器变成了8秒，
                // 所以要裁剪比9秒多一秒的边界
                if (timeStamp > end && timeStamp - end >= 1000000) {
                    break;
                }
                if (sampleSize <= 0) {
                    break;
                }
                byte[] buf = new byte[sampleSize];
                buffer.get(buf, 0, sampleSize);
                //写入文件
                outputStream.write(buf);
                //音轨数据往前读
                extractor.advance();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (extractor != null) {
                extractor.release();
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    /**
     * 获取音频数据轨道
     *
     * @param extractor
     * @return
     */
    private static int getAudioTrack(MediaExtractor extractor) {
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio")) {
                return i;
            }
        }
        return -1;
    }

    private static void printMusicFormat(String musicPath) {
        try {
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(musicPath);
            MediaFormat format = extractor.getTrackFormat(getAudioTrack(extractor));
            Log.e("music", "码率：" + format.getInteger(MediaFormat.KEY_BIT_RATE));
            Log.e("music", "轨道数:" + format.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
            Log.e("music", "采样率：" + format.getInteger(MediaFormat.KEY_SAMPLE_RATE));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
