package com.example.medicalsystem2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.imageview.ShapeableImageView;

public class Home extends AppCompatActivity {

    private ShapeableImageView avatarImage;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private BroadcastReceiver statusReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Find views
        TextView emailText = findViewById(R.id.emailText);
        avatarImage = findViewById(R.id.avatarImage);

        // Display email from bundle
        String email = getIntent().getStringExtra("user_email");
        if (email != null && !email.isEmpty()) {
            emailText.setText(email);
        }

        // Initialize ActivityResultLauncher for picking image
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            avatarImage.setImageURI(imageUri);
                        }
                    }
                }
        );

        // Set onClickListener to open gallery
        avatarImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        // Setup click listeners for Book Now buttons
        setupBookNowButtons();

        // Register broadcast receiver for doctor status updates
        setupStatusReceiver();

        // Start the availability service
        startAvailabilityService();
    }

    private void setupBookNowButtons() {
        // Find the doctors container
        ViewGroup doctorsContainer = findViewById(R.id.doctorsContainer);
        if (doctorsContainer == null) {
            // Try to find it in the horizontal scroll view
            HorizontalScrollView scrollView = findViewById(R.id.doctorsScrollView);
            if (scrollView != null) {
                doctorsContainer = (ViewGroup) scrollView.getChildAt(0);
            }
        }

        if (doctorsContainer != null) {
            // Loop through all doctor cards in the container
            for (int i = 0; i < doctorsContainer.getChildCount(); i++) {
                View card = doctorsContainer.getChildAt(i);
                Button bookButton = card.findViewById(R.id.bookButton);
                if (bookButton != null) {
                    bookButton.setOnClickListener(v -> openAppointmentScreen());
                }
            }
        }
    }

    private void openAppointmentScreen() {
        // Simple intent to open appointment screen
        Intent intent = new Intent(Home.this, appointment.class);
        startActivity(intent);
    }

    // NEW METHOD: Setup broadcast receiver to listen for status changes
    private void setupStatusReceiver() {
        statusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String status = intent.getStringExtra(DoctorAvailabilityService.EXTRA_STATUS);
                if (status != null) {
                    Log.d("HomeActivity", "Received status update: " + status);
                    updateDoctorStatus(status);
                }
            }
        };

        // Register the receiver
        IntentFilter filter = new IntentFilter(DoctorAvailabilityService.ACTION_STATUS_CHANGED);
        registerReceiver(statusReceiver, filter);
        Log.d("HomeActivity", "BroadcastReceiver registered");
    }

    // NEW METHOD: Update doctor status in UI
    private void updateDoctorStatus(String status) {
        Log.d("HomeActivity", "Updating doctor status to: " + status);

        // Find the first doctor card (you can modify this to update specific doctors)
        ViewGroup doctorsContainer = findViewById(R.id.doctorsContainer);
        if (doctorsContainer == null) {
            HorizontalScrollView scrollView = findViewById(R.id.doctorsScrollView);
            if (scrollView != null) {
                doctorsContainer = (ViewGroup) scrollView.getChildAt(0);
            }
        }

        if (doctorsContainer != null && doctorsContainer.getChildCount() > 0) {
            // Update the FIRST doctor card
            View firstDoctorCard = doctorsContainer.getChildAt(0);

            TextView statusText = firstDoctorCard.findViewById(R.id.statusText);
            View statusDot = firstDoctorCard.findViewById(R.id.statusDot);

            if (statusText != null && statusDot != null) {
                statusText.setText(status);

                if (status.equals(DoctorAvailabilityService.STATUS_AVAILABLE)) {
                    // Green for available
                    statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    statusDot.setBackgroundResource(R.drawable.status_dot_green);
                    Log.d("HomeActivity", "Status updated to AVAILABLE (GREEN)");
                } else {
                    // Red for in consultation
                    statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    statusDot.setBackgroundResource(R.drawable.status_dot_red);
                    Log.d("HomeActivity", "Status updated to IN CONSULTATION (RED)");
                }
            } else {
                Log.e("HomeActivity", "Could not find statusText or statusDot views!");
            }
        } else {
            Log.e("HomeActivity", "Doctor container is null or empty!");
        }
    }

    // NEW METHOD: Start availability service
    private void startAvailabilityService() {
        Intent serviceIntent = new Intent(this, DoctorAvailabilityService.class);
        startService(serviceIntent);
        Log.d("HomeActivity", "Availability service started");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister receiver to prevent memory leaks
        if (statusReceiver != null) {
            try {
                unregisterReceiver(statusReceiver);
                Log.d("HomeActivity", "BroadcastReceiver unregistered");
            } catch (IllegalArgumentException e) {
                Log.e("HomeActivity", "Receiver was not registered: " + e.getMessage());
            }
        }
    }
}