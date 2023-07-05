package de.pbma.moa.amr;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ScreenActivity extends AppCompatActivity {
    private static final int CHUNK_SIZE = 1024;
    private DatagramSocket socket;
    private byte[] frameBytes;
    private boolean isStreaming;
    private ScreenHelper screenHelper;
    private Button startButton;
    private Button settingsButton;
    private SurfaceView surfaceView;
    private SurfaceView surfaceView2;
    private SurfaceHolder surfaceHolder;
    private SurfaceHolder surfaceHolder2;
    private SensorHelper sensorHelper;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stream_layout);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        isStreaming = false;
        screenHelper = new ScreenHelper();
        //startButton = findViewById(R.id.start_button);
       // settingsButton = findViewById(R.id.settings_button);

       /* settingsButton.setOnClickListener(v ->{
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });*/

        surfaceView = findViewById(R.id.surfaceView);
        surfaceView2 = findViewById(R.id.surfaceView2);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder2 = surfaceView2.getHolder();

        surfaceView.setOnLongClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        });
        surfaceView2.setOnLongClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        });
        surfaceView.setOnClickListener(listener);
        surfaceView2.setOnClickListener(listener);

        //startButton.setOnClickListener(listener);

        sensorHelper = new SensorHelper((SensorManager) getSystemService(Context.SENSOR_SERVICE), this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(sensorHelper!=null){
            sensorHelper.unregisterSensorListener();
        }
    }

    private final View.OnClickListener listener = v -> start();

    private void start() {
        if (isStreaming) {
            Toast.makeText(this, "stop", Toast.LENGTH_LONG).show();
            isStreaming = false;
            //startButton.setText("start");
            if(sensorHelper!=null){
                sensorHelper.unregisterSensorListener();
            }
            if (socket != null) {
                socket.close();
            }
        } else {
            isStreaming = true;
            //startButton.setText("stop");
            if(sensorHelper!=null){
                sensorHelper.registerSensorListener();
            }
            new Thread(() -> {
                this.runOnUiThread(() -> {
                    Toast.makeText(this, "start", Toast.LENGTH_SHORT).show();
                });
                receiveVideo();
            }).start();
        }
    }

    private void receiveVideo() {
        try {
            socket = new DatagramSocket(5000);
            byte[] buffer = new byte[CHUNK_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            frameBytes = new byte[0];
            while (isStreaming) {
                socket.receive(packet);
                byte[] chunk = packet.getData();
                try {
                    frameBytes = screenHelper.concatenateByteArrays(frameBytes, chunk);
                } catch (IOException ignored) {
                }
                int indexDel = screenHelper.containsDelimiter(frameBytes);
                if (indexDel > -1) {
                    ScreenHelper.SplitResult frameChunks = ScreenHelper.getArrays(frameBytes, indexDel);
                    showVideoFrameSec(frameChunks.getFirstPart());
                    frameBytes = new byte[0];
                }
            }
            socket.close();
        } catch (Exception e) {
           // this.runOnUiThread(() -> {
           //     Toast.makeText(this, "fail", Toast.LENGTH_LONG).show();
           // });
            e.printStackTrace();
        }
    }

    private void showVideoFrameSec(byte[] array) {
        int surfaceViewWidth = surfaceView.getWidth();
        int surfaceViewHeight = surfaceView.getHeight();
        runOnUiThread(() -> {
            Canvas canvas = surfaceHolder.lockCanvas();
            Canvas canvas2 = surfaceHolder2.lockCanvas();
            if(canvas!=null){
                try {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(array, 0, array.length);
                    bitmap = Bitmap.createScaledBitmap(bitmap, surfaceViewWidth, surfaceViewHeight, true);
                    if (bitmap != null) {
                        canvas.drawBitmap(bitmap, 0, 0, null);
                        canvas2.drawBitmap(bitmap, 0, 0, null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                    surfaceHolder2.unlockCanvasAndPost(canvas2);
                }
            }
        });
    }
}
