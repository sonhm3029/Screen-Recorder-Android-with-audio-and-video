package com.shariar99.screenrecorder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
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
    private static final int AUDIO_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize MediaRecorder
        mediaRecorder = new MediaRecorder();

        // Get references to UI elements
        startBtn = findViewById(R.id.btn_start_recording);
        stopBtn = findViewById(R.id.btn_stop_recording);

        // Get screen density
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenDensity = metrics.densityDpi;

        // Initialize media projection manager
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestRecordingPermissionsAndStart();
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
            }
        });
    }

    private void requestRecordingPermissionsAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            Log.i("DEBUGGGGGG", "vào");
            startRecording();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION_REQUEST_CODE);
        }
    }

    private void startRecording() {
        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);

        releaseMediaRecorder();

        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");
            String timestamp = now.format(formatter);
            File appSpecificDirectory = getExternalFilesDir(null);
            String videoSavePath = new File(appSpecificDirectory, "record_" + timestamp + ".mp4").getAbsolutePath();

            // Configure the MediaRecorder settings
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setVideoEncodingBitRate(512 * 1000);
            mediaRecorder.setVideoFrameRate(30);
            mediaRecorder.setVideoSize(720, 1280);
            mediaRecorder.setOutputFile(videoSavePath);

            // Prepare the MediaRecorder
            mediaRecorder.prepare();

            // Request permission to capture the screen
            // Inside MainActivity's startRecording method
            Intent serviceIntent = new Intent(this, MediaProjectionService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
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
        mediaProjection = null;

        virtualDisplay.release();
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Audio permission granted, you can start recording
                startRecording();
            } else {
                // Audio permission denied, handle accordingly
                Toast.makeText(this, "Audio permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CODE) {
            Log.e("MainActivity", "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
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
}

