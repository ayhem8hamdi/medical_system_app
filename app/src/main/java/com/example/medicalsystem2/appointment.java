package com.example.medicalsystem2;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * APPOINTMENT ACTIVITY
 * Purpose: Allows users to book doctor appointments by selecting custom date and time
 * Features:
 * - User selects date (prevents past dates and Sundays)
 * - User selects time
 * - Appointment is saved to SharedPreferences
 * - Triggers reminder service and availability service
 * - Returns appointment data to Home activity
 */
public class appointment extends AppCompatActivity {

    // CLASS VARIABLES (Store appointment data)

    private TextView selectedDateTimeText;      // Shows selected date/time to user
    private MaterialButton customTimeButton;    // Button to open date/time picker

    private Calendar selectedCalendar;          // Stores the selected date and time
    private String selectedTimeSlot = "";       // Stores time as HH:mm format
    private boolean isCustomTime = false;       // Tracks if user chose custom time

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge display (use full screen including notch area)
        EdgeToEdge.enable(this);

        // Load the appointment layout XML file
        setContentView(R.layout.activity_appointment);

        // SETUP WINDOW INSETS (Handle notch and system bars)
        View rootView = findViewById(R.id.scrollView);
        if (rootView == null) {
            rootView = findViewById(android.R.id.content);
        }

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ═══════════════════════════════════════════════════════════════
        // INITIALIZE ACTIVITY
        // ═══════════════════════════════════════════════════════════════
        initializeViews();          // Find views from XML
        setupClickListeners();      // Setup button click handlers
        setupBackButton();          // Setup back navigation
    }

    /**
     * INITIALIZE VIEWS
     *
     * Purpose: Find views from the XML layout and store references
     * This method connects Java code to XML UI elements
     */
    private void initializeViews() {
        // Find the TextView that shows selected date/time
        selectedDateTimeText = findViewById(R.id.selectedDateTimeText);

        // Find the button user clicks to select date/time
        customTimeButton = findViewById(R.id.customTimeButton);

        // Initially hide the selected date/time text (show only after selection)
        if (selectedDateTimeText != null) {
            selectedDateTimeText.setVisibility(View.GONE);
        }
    }

    /**
     * SETUP BACK BUTTON
     *
     * Purpose: Handle back button clicks - user can go back without booking
     */
    private void setupBackButton() {
        ImageView backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                // Close this activity and return to Home
                finish();
            });
        }
    }

    /**
     * SETUP CLICK LISTENERS
     *
     * Purpose: Attach click handlers to buttons
     * When user clicks "Select Date and Time" button, open date picker
     */
    private void setupClickListeners() {
        if (customTimeButton != null) {
            customTimeButton.setOnClickListener(v -> {
                // User clicked "Select Date and Time" button
                showCustomDateTimePicker();
            });
        }
    }

    /**
     * SHOW CUSTOM DATE TIME PICKER
     *
     * Purpose: Show date picker dialog
     * User selects the appointment date
     *
     * Validations:
     * - Cannot select past dates
     * - Cannot select Sundays (clinic closed)
     */
    private void showCustomDateTimePicker() {
        // Get current date to use as default
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create and show date picker dialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // User selected a date - process it

                    // Create calendar object with selected date
                    selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay);

                    // ═══════════════════════════════════════════════════════════════
                    // VALIDATION 1: Check if Sunday (clinic closed on Sundays)
                    // ═══════════════════════════════════════════════════════════════
                    if (selectedCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                        // Show error message
                        Toast.makeText(this, "Sunday is not available for appointments", Toast.LENGTH_SHORT).show();
                        return; // Exit - don't proceed
                    }

                    // ═══════════════════════════════════════════════════════════════
                    // VALIDATION 2: Check if past date (user can't book past appointments)
                    // ═══════════════════════════════════════════════════════════════

                    // Get today's date (set time to 00:00:00 for comparison)
                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);

                    // Reset selected date time to 00:00:00 for comparison
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, 0);
                    selectedCalendar.set(Calendar.MINUTE, 0);
                    selectedCalendar.set(Calendar.SECOND, 0);
                    selectedCalendar.set(Calendar.MILLISECOND, 0);

                    // Check if selected date is in the past
                    if (selectedCalendar.before(today)) {
                        Toast.makeText(this, "Cannot select past dates", Toast.LENGTH_SHORT).show();
                        return; // Exit - don't proceed
                    }

                    // ═══════════════════════════════════════════════════════════════
                    // VALIDATION PASSED: Show time picker
                    // ═══════════════════════════════════════════════════════════════
                    showTimePicker(selectedYear, selectedMonth, selectedDay);
                },
                year, month, day
        );

        // Set minimum date to today (prevent selecting past dates)
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);

        // Display the date picker dialog
        datePickerDialog.show();
    }

    /**
     * SHOW TIME PICKER
     *
     * Purpose: Show time picker dialog after user selects date
     * User selects the appointment time
     *
     * Parameters:
     * - year, month, day: Date selected by user (from date picker)
     */
    private void showTimePicker(int year, int month, int day) {
        // Get current time to use as default
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // Create and show time picker dialog
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    // User selected time - process it

                    // Format time as HH:mm (e.g., "10:30")
                    selectedTimeSlot = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);

                    // Mark that user chose custom time (not preset slots)
                    isCustomTime = true;

                    // ═══════════════════════════════════════════════════════════════
                    // UPDATE CALENDAR WITH SELECTED TIME
                    // ═══════════════════════════════════════════════════════════════
                    // Now selectedCalendar has both date (from date picker) and time (from time picker)
                    selectedCalendar.set(year, month, day, selectedHour, selectedMinute, 0);

                    // ═══════════════════════════════════════════════════════════════
                    // SHOW SELECTED DATE/TIME TO USER
                    // ═══════════════════════════════════════════════════════════════
                    if (selectedDateTimeText != null) {
                        // Format: "Selected: Mon, Jan 15, 2024 at 10:30"
                        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());
                        String dateString = "Selected: " + sdf.format(selectedCalendar.getTime()) + " at " + selectedTimeSlot;

                        // Display the formatted text
                        selectedDateTimeText.setText(dateString);
                        selectedDateTimeText.setVisibility(View.VISIBLE);
                    }

                    // Show confirmation toast
                    Toast.makeText(this, "Custom time selected: " + selectedTimeSlot, Toast.LENGTH_SHORT).show();

                    // ═══════════════════════════════════════════════════════════════
                    // AUTO-CONFIRM APPOINTMENT
                    // ═══════════════════════════════════════════════════════════════
                    // After user selects time, automatically confirm the appointment
                    confirmAppointment();
                },
                hour,
                minute,
                true // Use 24-hour format (10:30 instead of 10:30 AM)
        );

        // Display the time picker dialog
        timePickerDialog.show();
    }

    /**
     * CONFIRM APPOINTMENT
     *
     * Purpose: Save appointment and return to Home activity
     *
     * Steps:
     * 1. Save appointment to SharedPreferences (local phone storage)
     * 2. Prepare result data to send back to Home
     * 3. Start AppointmentReminderService (notify user 1 min before)
     * 4. Start DoctorAvailabilityService (update doctor's status)
     * 5. Return to Home activity
     */
    private void confirmAppointment() {
        // Check if appointment data is valid
        if (selectedCalendar != null && !selectedTimeSlot.isEmpty()) {

            // ═══════════════════════════════════════════════════════════════
            // STEP 1: SAVE APPOINTMENT TO SHARED PREFERENCES
            // ═══════════════════════════════════════════════════════════════
            // This saves the appointment locally on the phone
            // Other parts of app can read this saved appointment
            saveAppointmentToPreferences();

            // ═══════════════════════════════════════════════════════════════
            // STEP 2: PREPARE RESULT DATA FOR HOME ACTIVITY
            // ═══════════════════════════════════════════════════════════════
            // When appointment activity closes, send appointment data back to Home
            Intent resultIntent = new Intent();

            // Put formatted appointment date/time string
            resultIntent.putExtra("appointment_datetime", getFormattedAppointmentDateTime());

            // Put flag indicating custom time was selected
            resultIntent.putExtra("is_custom_time", isCustomTime);

            // Set success result
            setResult(RESULT_OK, resultIntent);

            // ═══════════════════════════════════════════════════════════════
            // STEP 3: START REMINDER SERVICE
            // ═══════════════════════════════════════════════════════════════
            // AppointmentReminderService will:
            // - Monitor the appointment time
            // - Send notification 1 minute before appointment
            // - Play ringtone and vibrate phone
            startReminderService();

            // ═══════════════════════════════════════════════════════════════
            // STEP 4: START AVAILABILITY SERVICE
            // ═══════════════════════════════════════════════════════════════
            // DoctorAvailabilityService will:
            // - Monitor when appointment starts
            // - Update doctor's status to "IN CONSULTATION"
            // - Change doctor card color to red (red = busy)
            // - When appointment ends, change back to green (available)
            startAvailabilityService();

            // Show success message
            Toast.makeText(this,
                    "Appointment confirmed!\nYou'll receive a reminder 1 minute before!",
                    Toast.LENGTH_LONG).show();

            // ═══════════════════════════════════════════════════════════════
            // STEP 5: CLOSE THIS ACTIVITY AND RETURN TO HOME
            // ═══════════════════════════════════════════════════════════════
            finish();
        } else {
            // If validation fails (no date/time selected)
            Toast.makeText(this, "Please select a date and time", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * GET FORMATTED APPOINTMENT DATE TIME
     *
     * Purpose: Format the appointment date/time in a user-friendly way
     *
     * Format: "15-12-2024 at 10:30"
     *
     * Returns: String with formatted date and time
     */
    private String getFormattedAppointmentDateTime() {
        // Create date formatter with desired format
        SimpleDateFormat displaySdf = new SimpleDateFormat("dd-MM-yyyy 'at' HH:mm", Locale.getDefault());

        // Format the selected calendar to string
        String dateString = displaySdf.format(selectedCalendar.getTime());

        return dateString;
    }

    /**
     * SAVE APPOINTMENT TO PREFERENCES
     *
     * Purpose: Save appointment data to SharedPreferences (local phone storage)
     *
     * SharedPreferences is like a phone memory:
     * - Saves small data locally
     * - Data persists even after app closes
     * - Other activities can read this data
     *
     * What we save:
     * - "appointment_datetime": The appointment date/time (used by services)
     * - "is_custom_time": Boolean flag (custom time vs preset slots)
     */
    private void saveAppointmentToPreferences() {
        // Create date formatter with specific format (yyyy-MM-dd HH:mm)
        // This format must match what services expect
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        // Format selected calendar to string
        String dateTimeStr = sdf.format(selectedCalendar.getTime());

        // ═══════════════════════════════════════════════════════════════
        // ACCESS SHARED PREFERENCES
        // ═══════════════════════════════════════════════════════════════
        // Get SharedPreferences with name "AppointmentPrefs"
        // MODE_PRIVATE = only this app can access
        SharedPreferences prefs = getSharedPreferences("AppointmentPrefs", MODE_PRIVATE);

        // Get editor to modify preferences
        SharedPreferences.Editor editor = prefs.edit();

        // ═══════════════════════════════════════════════════════════════
        // SAVE APPOINTMENT DATA
        // ═══════════════════════════════════════════════════════════════
        // Save the appointment date/time string
        editor.putString("appointment_datetime", dateTimeStr);

        // Save whether custom time was used
        editor.putBoolean("is_custom_time", isCustomTime);

        // Apply changes (save to phone storage)
        editor.apply();

        // Log for debugging
        Log.d("AppointmentActivity", "Saved appointment: " + dateTimeStr);
    }

    /**
     * START REMINDER SERVICE
     *
     * Purpose: Start the AppointmentReminderService
     *
     * This is an UNBOUNDED SERVICE that:
     * - Runs in background independently
     * - Monitors appointment time
     * - Sends notification 1 minute before appointment
     * - Plays ringtone and vibrates phone
     *
     * It's UNBOUNDED because:
     * - Returns null in onBind()
     * - No direct connection to activity
     * - Runs independently
     * - Communication via notifications
     */
    private void startReminderService() {
        // Create intent to start AppointmentReminderService
        Intent serviceIntent = new Intent(this, AppointmentReminderService.class);

        // Start the service (unbounded - runs independently)
        startService(serviceIntent);

        // Log for debugging
        Log.d("AppointmentActivity", "Reminder service started!");
    }

    /**
     * START AVAILABILITY SERVICE
     *
     * Purpose: Start the DoctorAvailabilityService
     *
     * This is an UNBOUNDED SERVICE that:
     * - Runs in background independently
     * - Monitors appointment time
     * - Updates doctor's availability status
     * - Sends broadcasts when status changes
     *
     * Status updates:
     * - AVAILABLE (before and after appointment) = Green dot
     * - IN CONSULTATION (during appointment) = Red dot
     *
     * It's UNBOUNDED because:
     * - Returns null in onBind()
     * - No direct connection to activity
     * - Communication via Broadcast
     */
    private void startAvailabilityService() {
        // Create intent to start DoctorAvailabilityService
        Intent serviceIntent = new Intent(this, DoctorAvailabilityService.class);

        // Start the service (unbounded - runs independently)
        startService(serviceIntent);

        // Log for debugging
        Log.d("AppointmentActivity", "Availability service started!");
    }

    /**
     * ON DESTROY
     *
     * Purpose: Cleanup when activity is destroyed
     * Called automatically when user closes appointment activity
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Any cleanup code would go here if needed
    }
}