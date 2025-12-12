package com.example.medicalsystem2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.imageview.ShapeableImageView;

public class Home extends AppCompatActivity {

    private static final int APPOINTMENT_REQUEST_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 200;

    private ShapeableImageView avatarImage;
    private BroadcastReceiver statusReceiver;

    // Reminder card views
    private CardView reminderCard;
    private TextView appointmentReminderText;
    private ImageView closeReminderButton;

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

        // Initialize reminder card views
        initializeReminderCard();

        // Set onClickListener to open gallery
        avatarImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, GALLERY_REQUEST_CODE);
        });

        // Setup click listener for Book Now button
        setupBookNowButton();

        // Register broadcast receiver for doctor status updates
        setupStatusReceiver();

        // Load and display saved appointment if it exists
        loadSavedAppointment();

        // Start the availability service
        startAvailabilityService();
    }

    private void initializeReminderCard() {
        reminderCard = findViewById(R.id.reminderCard);
        appointmentReminderText = findViewById(R.id.appointmentReminderText);
        closeReminderButton = findViewById(R.id.closeReminderButton);

        if (closeReminderButton != null) {
            closeReminderButton.setOnClickListener(v -> hideReminderCard());
        }
    }

    private void setupBookNowButton() {
        try {
            // Find the doctor card frame layout first
            View doctorCardFrame = findViewById(R.id.doctorCard1);

            if (doctorCardFrame != null) {
                // Find the book button inside the included layout
                View bookButton = doctorCardFrame.findViewById(R.id.bookButton);

                if (bookButton != null) {
                    bookButton.setOnClickListener(v -> {
                        Log.d("HomeActivity", "Book button clicked");
                        Intent intent = new Intent(Home.this, appointment.class);
                        startActivityForResult(intent, APPOINTMENT_REQUEST_CODE);
                    });
                    Log.d("HomeActivity", "Book button listener set successfully");
                } else {
                    Log.e("HomeActivity", "Book button not found in doctor card");
                }
            } else {
                Log.e("HomeActivity", "Doctor card frame not found");
            }
        } catch (Exception e) {
            Log.e("HomeActivity", "Error setting up book button: " + e.getMessage());
        }
    }

    // ⭐ NEW METHOD - Handle results from launched activities
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Handle appointment booking result
        if (requestCode == APPOINTMENT_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String appointmentDateTime = data.getStringExtra("appointment_datetime");
            boolean isCustomTime = data.getBooleanExtra("is_custom_time", false);

            if (appointmentDateTime != null && !appointmentDateTime.isEmpty()) {
                displayAppointmentReminder(appointmentDateTime);
                Log.d("HomeActivity", "Appointment set: " + appointmentDateTime + " (Custom: " + isCustomTime + ")");
            }
        }

        // Handle gallery image result
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                avatarImage.setImageURI(imageUri);
            }
        }
    }

    private void displayAppointmentReminder(String appointmentDateTime) {
        if (appointmentReminderText != null && reminderCard != null) {
            // Format the appointment text
            String reminderText = "You have an appointment on " + appointmentDateTime;
            appointmentReminderText.setText(reminderText);

            // Show the reminder card
            reminderCard.setVisibility(View.VISIBLE);
        }
    }

    private void hideReminderCard() {
        if (reminderCard != null) {
            reminderCard.setVisibility(View.GONE);
        }
    }

    private void loadSavedAppointment() {
        // Load appointment from SharedPreferences if it exists
        SharedPreferences prefs = getSharedPreferences("AppointmentPrefs", MODE_PRIVATE);
        String savedAppointment = prefs.getString("appointment_datetime", "");

        if (!savedAppointment.isEmpty()) {
            displayAppointmentReminder(savedAppointment);
        }
    }

    // Setup broadcast receiver to listen for status changes
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
        try {
            registerReceiver(statusReceiver, filter);
            Log.d("HomeActivity", "BroadcastReceiver registered");
        } catch (Exception e) {
            Log.e("HomeActivity", "Error registering receiver: " + e.getMessage());
        }
    }

    // Update doctor status in UI
    private void updateDoctorStatus(String status) {
        Log.d("HomeActivity", "Updating doctor status to: " + status);

        try {
            // Find the doctor card
            View doctorCardFrame = findViewById(R.id.doctorCard1);

            if (doctorCardFrame != null) {
                TextView statusText = doctorCardFrame.findViewById(R.id.statusText);
                View statusDot = doctorCardFrame.findViewById(R.id.statusDot);

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
                Log.e("HomeActivity", "Doctor card not found!");
            }
        } catch (Exception e) {
            Log.e("HomeActivity", "Error updating doctor status: " + e.getMessage());
        }
    }

    // Start availability service
    private void startAvailabilityService() {
        try {
            Intent serviceIntent = new Intent(this, DoctorAvailabilityService.class);
            startService(serviceIntent);
            Log.d("HomeActivity", "Availability service started");
        } catch (Exception e) {
            Log.e("HomeActivity", "Error starting availability service: " + e.getMessage());
        }
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