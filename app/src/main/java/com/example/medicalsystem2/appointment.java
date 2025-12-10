package com.example.medicalsystem2;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class appointment extends AppCompatActivity {

    private TextView selectedDateText;
    private TextView availableTimeTitle;
    private RecyclerView timeSlotsRecyclerView;
    private MaterialButton confirmAppointmentButton;
    private MaterialButton openFullCalendarButton;
    private MaterialButton customTimeButton;

    private TimeSlotsAdapter timeSlotsAdapter;
    private List<String> availableTimeSlots = new ArrayList<>();
    private Calendar selectedCalendar;
    private String selectedTimeSlot = "";
    private boolean isCustomTime = false; // Track if user selected custom time

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_appointment);

        // Check if scrollView exists, otherwise use the root view
        View rootView = findViewById(R.id.scrollView);
        if (rootView == null) {
            rootView = findViewById(android.R.id.content);
        }

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupClickListeners();
        setupRecyclerView();
    }

    private void initializeViews() {
        selectedDateText = findViewById(R.id.selectedDateText);
        availableTimeTitle = findViewById(R.id.availableTimeTitle);
        timeSlotsRecyclerView = findViewById(R.id.timeSlotsRecyclerView);
        confirmAppointmentButton = findViewById(R.id.confirmAppointmentButton);
        openFullCalendarButton = findViewById(R.id.openFullCalendarButton);
        customTimeButton = findViewById(R.id.customTimeButton);

        // Make sure confirm button starts disabled and hidden
        if (confirmAppointmentButton != null) {
            confirmAppointmentButton.setEnabled(false);
            confirmAppointmentButton.setAlpha(0.5f);
            confirmAppointmentButton.setVisibility(View.GONE);
        }

        // Hide time slots section initially
        if (selectedDateText != null) {
            selectedDateText.setVisibility(View.GONE);
        }
        if (availableTimeTitle != null) {
            availableTimeTitle.setVisibility(View.GONE);
        }
        if (timeSlotsRecyclerView != null) {
            timeSlotsRecyclerView.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        if (openFullCalendarButton != null) {
            openFullCalendarButton.setOnClickListener(v -> showDatePickerDialog());
        }

        // NEW: Custom time button click listener
        if (customTimeButton != null) {
            customTimeButton.setOnClickListener(v -> showCustomDateTimePicker());
        }

        if (confirmAppointmentButton != null) {
            confirmAppointmentButton.setOnClickListener(v -> {
                if (selectedCalendar != null && !selectedTimeSlot.isEmpty()) {
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());
                    String date = sdf.format(selectedCalendar.getTime());

                    // Save appointment to SharedPreferences
                    saveAppointmentToPreferences();

                    // Start the reminder service
                    startReminderService();

                    // Start the availabilty service
                    startAvailabilityService();

                    Toast.makeText(this,
                            "Appointment confirmed for " + date + " at " + selectedTimeSlot + "\n" +
                                    "You'll receive a reminder 30 minutes before!",
                            Toast.LENGTH_LONG).show();

                    // For now, just show success and reset
                    resetSelection();

                } else {
                    Toast.makeText(this, "Please select a date and time slot", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupRecyclerView() {
        if (timeSlotsRecyclerView == null) return;

        // Use GridLayoutManager with 4 columns
        GridLayoutManager layoutManager = new GridLayoutManager(this, 4);
        timeSlotsRecyclerView.setLayoutManager(layoutManager);

        // Initialize the adapter with empty list
        timeSlotsAdapter = new TimeSlotsAdapter(new ArrayList<>(), timeSlot -> {
            selectedTimeSlot = timeSlot;
            isCustomTime = false; // Reset custom time flag
            if (timeSlotsAdapter != null) {
                timeSlotsAdapter.setSelectedTimeSlot(timeSlot);
            }

            // Enable confirm button when time is selected
            if (confirmAppointmentButton != null) {
                confirmAppointmentButton.setEnabled(true);
                confirmAppointmentButton.setAlpha(1f);
            }
        });

        timeSlotsRecyclerView.setAdapter(timeSlotsAdapter);
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay);

                    // Check if selected day is Sunday (Calendar.SUNDAY = 1)
                    if (selectedCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                        Toast.makeText(this, "Sunday is not available for appointments", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Check if selected date is in the past
                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);

                    selectedCalendar.set(Calendar.HOUR_OF_DAY, 0);
                    selectedCalendar.set(Calendar.MINUTE, 0);
                    selectedCalendar.set(Calendar.SECOND, 0);
                    selectedCalendar.set(Calendar.MILLISECOND, 0);

                    if (selectedCalendar.before(today)) {
                        Toast.makeText(this, "Cannot select past dates", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateSelectedDate(selectedYear, selectedMonth, selectedDay);
                    generateTimeSlots(selectedCalendar);

                },
                year, month, day
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    // NEW METHOD: Show custom date and time picker
    private void showCustomDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // First, show date picker
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay);

                    // Check if selected day is Sunday
                    if (selectedCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                        Toast.makeText(this, "Sunday is not available for appointments", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Check if selected date is in the past
                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);

                    selectedCalendar.set(Calendar.HOUR_OF_DAY, 0);
                    selectedCalendar.set(Calendar.MINUTE, 0);
                    selectedCalendar.set(Calendar.SECOND, 0);
                    selectedCalendar.set(Calendar.MILLISECOND, 0);

                    if (selectedCalendar.before(today)) {
                        Toast.makeText(this, "Cannot select past dates", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // After date is selected, show time picker
                    showTimePicker(selectedYear, selectedMonth, selectedDay);
                },
                year, month, day
        );

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    // NEW METHOD: Show time picker dialog
    private void showTimePicker(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    // Format the selected time
                    selectedTimeSlot = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                    isCustomTime = true;

                    // Update the selected date display
                    updateSelectedDate(year, month, day);

                    // Show selected time in the UI
                    if (selectedDateText != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());
                        selectedCalendar.set(year, month, day);
                        String dateString = "Selected: " + sdf.format(selectedCalendar.getTime()) + " at " + selectedTimeSlot;
                        selectedDateText.setText(dateString);
                        selectedDateText.setVisibility(View.VISIBLE);
                    }

                    // Hide time slots recycler view (not needed for custom time)
                    if (availableTimeTitle != null) {
                        availableTimeTitle.setVisibility(View.GONE);
                    }
                    if (timeSlotsRecyclerView != null) {
                        timeSlotsRecyclerView.setVisibility(View.GONE);
                    }

                    // Show and enable confirm button
                    if (confirmAppointmentButton != null) {
                        confirmAppointmentButton.setVisibility(View.VISIBLE);
                        confirmAppointmentButton.setEnabled(true);
                        confirmAppointmentButton.setAlpha(1f);
                    }

                    Toast.makeText(this, "Custom time selected: " + selectedTimeSlot, Toast.LENGTH_SHORT).show();
                },
                hour,
                minute,
                true // 24-hour format
        );

        timePickerDialog.show();
    }

    private void updateSelectedDate(int year, int month, int day) {
        if (selectedDateText == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);
        String dateString = "Selected Date: " + sdf.format(calendar.getTime());

        selectedDateText.setText(dateString);
        selectedDateText.setVisibility(View.VISIBLE);
    }

    private void generateTimeSlots(Calendar selectedDate) {
        if (availableTimeSlots == null) {
            availableTimeSlots = new ArrayList<>();
        }
        availableTimeSlots.clear();
        isCustomTime = false; // Reset when using predefined slots

        int dayOfWeek = selectedDate.get(Calendar.DAY_OF_WEEK);

        // Check if it's Friday (Calendar.FRIDAY = 6) or Saturday (Calendar.SATURDAY = 7)
        boolean isWeekend = (dayOfWeek == Calendar.FRIDAY || dayOfWeek == Calendar.SATURDAY);

        if (isWeekend) {
            // Weekend slots: 10:30 to 15:30 with 1-hour intervals
            for (int hour = 10; hour <= 15; hour++) {
                if (hour == 15) {
                    availableTimeSlots.add("15:30");
                    break;
                }
                availableTimeSlots.add(String.format(Locale.getDefault(), "%02d:30", hour));
            }
        } else {
            // Weekday slots: 10:30 to 17:30 with 1-hour intervals
            for (int hour = 10; hour <= 17; hour++) {
                if (hour == 17) {
                    availableTimeSlots.add("17:30");
                    break;
                }
                availableTimeSlots.add(String.format(Locale.getDefault(), "%02d:30", hour));
            }
        }

        // Show time slots section
        if (availableTimeTitle != null) {
            availableTimeTitle.setVisibility(View.VISIBLE);
        }
        if (timeSlotsRecyclerView != null) {
            timeSlotsRecyclerView.setVisibility(View.VISIBLE);
        }
        if (confirmAppointmentButton != null) {
            confirmAppointmentButton.setVisibility(View.VISIBLE);
        }

        // Update recycler view
        if (timeSlotsAdapter != null) {
            timeSlotsAdapter.setTimeSlots(availableTimeSlots);
        }
        selectedTimeSlot = ""; // Reset selected time

        if (confirmAppointmentButton != null) {
            confirmAppointmentButton.setEnabled(false);
            confirmAppointmentButton.setAlpha(0.5f);
        }
    }

    private void resetSelection() {
        // Reset selected time
        selectedTimeSlot = "";
        isCustomTime = false;

        // Reset UI state
        if (confirmAppointmentButton != null) {
            confirmAppointmentButton.setEnabled(false);
            confirmAppointmentButton.setAlpha(0.5f);
        }

        // Clear selection in adapter
        if (timeSlotsAdapter != null) {
            timeSlotsAdapter.setSelectedTimeSlot("");
            timeSlotsAdapter.notifyDataSetChanged();
        }
    }

    // Save appointment to SharedPreferences
    private void saveAppointmentToPreferences() {
        // Combine date and time into one string
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateStr = sdf.format(selectedCalendar.getTime());
        String dateTimeStr = dateStr + " " + selectedTimeSlot;

        // Save to SharedPreferences
        SharedPreferences prefs = getSharedPreferences("AppointmentPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("appointment_datetime", dateTimeStr);
        editor.apply();

        Log.d("AppointmentActivity", "Saved appointment: " + dateTimeStr + (isCustomTime ? " (Custom time)" : " (Predefined slot)"));
    }

    // Start the reminder service
    private void startReminderService() {
        Intent serviceIntent = new Intent(this, AppointmentReminderService.class);
        startService(serviceIntent);
        Log.d("AppointmentActivity", "Reminder service started!");
    }
    // Start the doctor availability service
    private void startAvailabilityService() {
        Intent serviceIntent = new Intent(this, DoctorAvailabilityService.class);
        startService(serviceIntent);
        Log.d("AppointmentActivity", "Availability service started!");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (availableTimeSlots != null) {
            availableTimeSlots.clear();
        }
    }
}