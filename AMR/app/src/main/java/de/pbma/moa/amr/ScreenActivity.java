package de.pbma.moa.amr;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ScreenActivity extends AppCompatActivity {
    private static final int CHUNK_SIZE = 1024;
    private DatagramSocket socket;
    private byte[] frameBytes;
    private boolean isStreaming;
    private ImageView imageView;
    private ScreenHelper screenHelper;
    private Button startButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stream_layout);
        isStreaming = false;
        screenHelper = new ScreenHelper();

        startButton = findViewById(R.id.start_button);
        imageView = findViewById(R.id.image_view);

        startButton.setOnClickListener(listener);
    }


    private final View.OnClickListener listener = v -> start();
    private void start(){
        if(isStreaming){
            isStreaming = false;
            startButton.setText("start");
            if(socket!=null){
                socket.close();
            }
        }else {
            isStreaming = true;
            startButton.setText("stop");
            new Thread(()->{
                receiveVideo();
            }).start();
        }
    }

    private void receiveVideo(){
        try {
            socket = new DatagramSocket(5000);
            byte[] buffer = new byte[CHUNK_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            frameBytes = new byte[0];

            while (isStreaming) {
                socket.receive(packet);
                byte[] chunk = packet.getData();
                frameBytes = screenHelper.concatenateByteArrays(frameBytes, chunk);

                if (screenHelper.containsDelimiter(frameBytes)) {
                    byte[][] frameChunks = screenHelper.splitFrameBytes(frameBytes);
                    Mat frame = decodeFrame(frameChunks[0]);
                    showFrame(frame);
                    if (frame != null) {
                        frame.release();
                    }
                    frameBytes = frameChunks[1];
                }
            }
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Mat decodeFrame(byte[] frameChunk) {
        try {
            MatOfByte matOfByte = new MatOfByte(frameChunk);
            return Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_UNCHANGED);
        } catch (CvException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showFrame(final Mat frame){
        if(frame != null && !frame.empty()){
            runOnUiThread(()->{
                Bitmap bitmap = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888);
                imageView.setImageBitmap(bitmap);
                frame.release();
            });
        }
    }
}
