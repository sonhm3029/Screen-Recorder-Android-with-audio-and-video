package com.shariar99.screenrecorder;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1000;
    private int screenDensity;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaRecorder mediaRecorder;
    private Button startBtn;
    private Button stopBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize MediaRecorder
        mediaRecorder = new MediaRecorder();

        //Get references to UI elements
        startBtn = findViewById(R.id.btn_start_recording);
        stopBtn = findViewById(R.id.btn_stop_recording);


        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();
            }
        });
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
            }
        });

        //get screen density
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenDensity = metrics.densityDpi;

        //initialize media projection manager
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CODE) {
            Log.e("MainActivity", "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this,
                    "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            return;
        }
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        virtualDisplay = createVirtualDisplay();
        mediaRecorder.start();
    }

    private VirtualDisplay createVirtualDisplay() {
        return mediaProjection.createVirtualDisplay("MainActivity",
                720,1280, screenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mediaRecorder.getSurface(), null /*Callbacks*/, null
                    /*Handler*/);
        }
    private void startRecording() {
        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");
            String timestamp = now.format(formatter);
//            String videoSavePath = Environment.getExternalStorageDirectory() + "/record_" + timestamp +".mp4";
            File appSpecificDirectory = getExternalFilesDir(null);
            String videoSavePath = new File(appSpecificDirectory, "record_" + timestamp + ".mp4").getAbsolutePath();

            // Configure the MediaRecorder settings
//            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setVideoEncodingBitRate(512 * 1000);
            mediaRecorder.setVideoFrameRate(30);
            mediaRecorder.setVideoSize(720, 1280);
            mediaRecorder.setOutputFile(videoSavePath);

            // Prepare the MediaRecorder
            mediaRecorder.prepare();

            // Request permission to capture the screen
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        mediaRecorder.stop();
        mediaRecorder.reset();
        mediaProjection.stop();

        // Release the media recorder
        mediaRecorder.release();
    }
}

