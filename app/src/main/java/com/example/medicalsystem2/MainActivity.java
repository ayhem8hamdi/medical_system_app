package com.example.medicalsystem2;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private int progressStatus = 0;
    private Handler handler = new Handler();
    private boolean isRunning = true;
    private Thread progressThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);

        startProgress();
    }

    private void startProgress() {
        progressThread = new Thread(() -> {
            while (progressStatus < 100 && isRunning) {
                progressStatus++;
                handler.post(() -> progressBar.setProgress(progressStatus));
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (progressStatus >= 100) {
                // Navigate to the NEXT activity (replace NextActivity.class with your target)
                startActivity(new Intent(MainActivity.this, Login.class));
                finish(); // close current activity
            }
        });
        progressThread.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isRunning = false; // pause thread if app goes to background
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        isRunning = true; // resume progress
        startProgress();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
    }
}
