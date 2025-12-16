package com.example.medicalsystem2;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ProgressBar progressBar;       // ProgressBar view in layout
    private int progressStatus = 0;        // Current progress value (0-100)
    private Handler handler = new Handler(); // Handler to update UI from background thread
    private boolean isRunning = true;      // Flag to control thread execution
    private Thread progressThread;         // Background thread to update progress

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Connect the ProgressBar from layout
        progressBar = findViewById(R.id.progressBar);

        // Start the progress update
        startProgress();
    }

    /**
     * Start a background thread to increment the progress bar
     */
    private void startProgress() {
        progressThread = new Thread(() -> {
            // Loop until progress reaches 100 or the activity is stopped
            while (progressStatus < 100 && isRunning) {
                progressStatus++; // Increase progress

                // Post the update to the UI thread using Handler
                // Update UI from background thread

                handler.post(() -> progressBar.setProgress(progressStatus));

                try {
                    // Delay for 50 milliseconds to create a smooth animation
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // When progress reaches 100, navigate to the next activity (Login)
            if (progressStatus >= 100) {
                startActivity(new Intent(MainActivity.this, Login.class));
                finish(); // Close MainActivity so user can't go back to it
            }
        });

        // Start the background thread
        progressThread.start();
    }

    /**
     * Pause the progress if the app goes to background
     */
    @Override
    protected void onStop() {
        super.onStop();
        isRunning = false; // Stop the thread
    }

    /**
     * Resume progress if the app comes back to foreground
     */
    @Override
    protected void onRestart() {
        super.onRestart();
        isRunning = true; // Resume the thread
        startProgress();  // Start progress again
    }

    /**
     * Ensure the thread stops when activity is destroyed
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false; // Stop the thread
    }
}
