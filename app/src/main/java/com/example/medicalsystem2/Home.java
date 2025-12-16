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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

// ✅ IMPORTS FOR BOUNDED SERVICE
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.google.android.material.imageview.ShapeableImageView;

public class Home extends AppCompatActivity {

    // Request codes for identifying results from launched activities
    private static final int APPOINTMENT_REQUEST_CODE = 100;
    // Used to identify result from appointment booking activity
    private static final int GALLERY_REQUEST_CODE = 200;
    // Used to identify result from gallery image picker

    // ================= BOUNDED SERVICE VARIABLES =================
    private AppointmentBoundedService boundedService;
    // Reference to the bounded service instance
    private boolean isBoundedServiceConnected = false;
    // Tracks if the activity is currently bound to the service
    private ServiceConnection serviceConnection;
    // Manages the connection lifecycle between activity and service

    private ShapeableImageView avatarImage;
    // User avatar image view in the UI
    private BroadcastReceiver statusReceiver;
    // Receives updates about doctor availability from a background service

    private CardView reminderCard;
    // The container card that shows an appointment reminder
    private TextView appointmentReminderText;
    // Text inside the reminder card showing appointment date/time
    private ImageView closeReminderButton;
    // Button inside reminder card to dismiss/hide it

    private TextView boundedServiceStatusText;
    // TextView showing current appointment status from bounded service
    private TextView minutesUntilAppointmentText;
    // TextView showing remaining minutes until the appointment


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // show home activity content
        setContentView(R.layout.activity_home);


        // ================= FIND VIEWS =================
        // Find the TextView to display user's email
        TextView emailText = findViewById(R.id.emailText);

        // Find the avatar image view (for profile picture)
        avatarImage = findViewById(R.id.avatarImage);

        // ✅ Bounded service status views: show appointment status and minutes left
        boundedServiceStatusText = findViewById(R.id.boundedServiceStatusText);
        minutesUntilAppointmentText = findViewById(R.id.minutesUntilAppointmentText);

        //Get data from bundle
        // Get the Intent that started this activity
        Intent launchIntent = getIntent();
        // Extract any additional data (extras) from the Intent
        Bundle extras = launchIntent.getExtras();

        if (extras != null) {
            String email = extras.getString("user_email"); // Retrieve email from extras
            if (email != null && !email.isEmpty()) {
                emailText.setText(email); // Display email in the TextView
            }
        }

        // INITIALIZE REMINDER CARD
        // Setup the appointment reminder card views (text + close button)
        initializeReminderCard();

        //AVATAR IMAGE CLICK
        // When avatar is clicked, open gallery to pick an image
        avatarImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            // Default behaviour to open gallery and choose image
            intent.setType("image/*"); // Filter for images only
            startActivityForResult(intent, GALLERY_REQUEST_CODE); // Launch gallery
        });

        // ================= BOOK NOW BUTTON =================
        // Setup the click listener for booking an appointment
        setupBookNowButton();

        // ================= BROADCAST RECEIVER =================
        // Register receiver to get updates about doctor's availability
        setupStatusReceiver();

        // ================= LOAD SAVED APPOINTMENT =================
        // Check SharedPreferences and display any existing appointment
        loadSavedAppointment();

        // ================= START BACKGROUND SERVICE =================
        // Start the DoctorAvailabilityService to keep track of doctor status
        startAvailabilityService();

        // ================= BOUNDED SERVICE CONNECTION =================
        // Bind to the AppointmentBoundedService to get live appointment data
        setupBoundedServiceConnection();
    }


    private void initializeReminderCard() {
        reminderCard = findViewById(R.id.reminderCard);
        appointmentReminderText = findViewById(R.id.appointmentReminderText);
        closeReminderButton = findViewById(R.id.closeReminderButton);

        // it will be found only when the user already picked a date
        if (closeReminderButton != null) {
            closeReminderButton.setOnClickListener(v -> hideReminderCard());
        }
    }

    private void setupBookNowButton() {
        try {
            // Step 1: Find the container layout for the doctor card
            View doctorCardFrame = findViewById(R.id.doctorCard1);

            if (doctorCardFrame != null) {
                // Step 2: Inside the doctor card, find the "Book Now" button
                View bookButton = doctorCardFrame.findViewById(R.id.bookButton);

                if (bookButton != null) {
                    // Step 3: Set a click listener for the book button
                    bookButton.setOnClickListener(v -> {
                        Log.d("HomeActivity", "Book button clicked");

                        // Launch the appointment activity when button is clicked
                        Intent intent = new Intent(Home.this, appointment.class);

                        // Start appointment activity expecting a result (appointment details)
                        startActivityForResult(intent, APPOINTMENT_REQUEST_CODE);
                    });

                    // Debug log to confirm listener was successfully attached
                    Log.d("HomeActivity", "Book button listener set successfully");
                } else {
                    // Log error if book button could not be found inside doctor card
                    Log.e("HomeActivity", "Book button not found in doctor card");
                }
            } else {
                // Log error if doctor card frame itself is not found in the layout
                Log.e("HomeActivity", "Doctor card frame not found");
            }
        } catch (Exception e) {
            // Catch any exceptions during setup and log them
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

                // ✅ NEW: Check bounded service status after appointment booked
                if (isBoundedServiceConnected && boundedService != null) {
                    String status = boundedService.getAppointmentStatus();
                    updateBoundedServiceUI(status);
                }
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

    // ✅ NEW: BOUNDED SERVICE SETUP METHOD
    /**
     * Setup the connection between Home activity and AppointmentBoundedService
     */
    private void setupBoundedServiceConnection() {
        Log.d("HomeActivity", "========== SETTING UP BOUNDED SERVICE ==========");

        serviceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d("HomeActivity", "✅ onServiceConnected() called");
                Log.d("HomeActivity", "Service: " + name.getClassName());

                // Get the Binder and cast it
                AppointmentBoundedService.LocalBinder binder =
                        (AppointmentBoundedService.LocalBinder) service;

                // Get service reference
                boundedService = binder.getService();
                isBoundedServiceConnected = true;

                Log.d("HomeActivity", "========== SERVICE CONNECTED ==========");
                Log.d("HomeActivity", "✅ Can now call service methods!");

                // Set up listener for status updates
                boundedService.setStatusListener(newStatus -> {
                    Log.d("HomeActivity", "📢 Bounded Service Status Update: " + newStatus);
                    updateBoundedServiceUI(newStatus);
                });

                // Get initial status
                String initialStatus = boundedService.getAppointmentStatus();
                int minutesLeft = boundedService.getMinutesUntilAppointment();
                boolean hasAppointment = boundedService.hasAppointment();

                Log.d("HomeActivity", "Initial Status: " + initialStatus);
                Log.d("HomeActivity", "Minutes Left: " + minutesLeft);
                Log.d("HomeActivity", "Has Appointment: " + hasAppointment);

                // Update UI with initial status
                updateBoundedServiceUI(initialStatus);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d("HomeActivity", "❌ onServiceDisconnected() called");
                isBoundedServiceConnected = false;
                boundedService = null;
                Log.d("HomeActivity", "========== SERVICE DISCONNECTED ==========");
            }
        };

        Log.d("HomeActivity", "Attempting to bind to AppointmentBoundedService...");

        Intent intent = new Intent(this, AppointmentBoundedService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);

        Log.d("HomeActivity", "bindService() called");
    }

    // ✅ NEW: Update UI with bounded service status
    private void updateBoundedServiceUI(String status) {
        Log.d("HomeActivity", "📱 Updating UI with bounded service status: " + status);

        if (boundedServiceStatusText != null) {
            boundedServiceStatusText.setText(status);
            boundedServiceStatusText.setVisibility(View.VISIBLE);
        }

        if (isBoundedServiceConnected && boundedService != null) {
            int minutesLeft = boundedService.getMinutesUntilAppointment();

            if (minutesUntilAppointmentText != null) {
                if (minutesLeft >= 0) {
                    minutesUntilAppointmentText.setText("⏱️ " + minutesLeft + " minutes left");
                } else {
                    minutesUntilAppointmentText.setText("⏱️ Appointment passed");
                }
                minutesUntilAppointmentText.setVisibility(View.VISIBLE);
            }
        }
    }

    // ✅ NEW: Check appointment status manually (optional)
    private void checkAppointmentWithBoundedService() {
        if (isBoundedServiceConnected && boundedService != null) {
            Log.d("HomeActivity", "========== MANUAL STATUS CHECK ==========");

            String status = boundedService.getAppointmentStatus();
            int minutesLeft = boundedService.getMinutesUntilAppointment();
            boolean hasAppointment = boundedService.hasAppointment();
            String doctorName = boundedService.getDoctorName();

            Log.d("HomeActivity", "Status: " + status);
            Log.d("HomeActivity", "Minutes: " + minutesLeft);
            Log.d("HomeActivity", "Has Appointment: " + hasAppointment);
            Log.d("HomeActivity", "Doctor: " + doctorName);

            Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
            updateBoundedServiceUI(status);
        } else {
            Log.e("HomeActivity", "❌ Service not connected!");
            Toast.makeText(this, "Service not connected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("HomeActivity", "========== ACTIVITY DESTROYING ==========");

        // Unregister broadcast receiver
        if (statusReceiver != null) {
            try {
                unregisterReceiver(statusReceiver);
                Log.d("HomeActivity", "✅ BroadcastReceiver unregistered");
            } catch (IllegalArgumentException e) {
                Log.e("HomeActivity", "Receiver was not registered: " + e.getMessage());
            }
        }

        // ✅ UNBIND BOUNDED SERVICE
        if (isBoundedServiceConnected) {
            Log.d("HomeActivity", "Unbinding from bounded service...");

            unbindService(serviceConnection);

            isBoundedServiceConnected = false;
            boundedService = null;

            Log.d("HomeActivity", "✅ Bounded Service Unbound");
        }

        Log.d("HomeActivity", "========== ACTIVITY DESTROYED ==========");
    }
}