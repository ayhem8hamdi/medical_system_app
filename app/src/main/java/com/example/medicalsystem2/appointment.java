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
    private boolean isCustomTime = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_appointment);

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
        setupBackButton();
    }

    private void initializeViews() {
        selectedDateText = findViewById(R.id.selectedDateText);
        availableTimeTitle = findViewById(R.id.availableTimeTitle);
        timeSlotsRecyclerView = findViewById(R.id.timeSlotsRecyclerView);
        confirmAppointmentButton = findViewById(R.id.confirmAppointmentButton);
        openFullCalendarButton = findViewById(R.id.openFullCalendarButton);
        customTimeButton = findViewById(R.id.customTimeButton);

        if (confirmAppointmentButton != null) {
            confirmAppointmentButton.setEnabled(false);
            confirmAppointmentButton.setAlpha(0.5f);
            confirmAppointmentButton.setVisibility(View.GONE);
        }

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

    private void setupBackButton() {
        ImageView backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
    }

    private void setupClickListeners() {
        if (openFullCalendarButton != null) {
            openFullCalendarButton.setOnClickListener(v -> showDatePickerDialog());
        }

        if (customTimeButton != null) {
            customTimeButton.setOnClickListener(v -> showCustomDateTimePicker());
        }

        if (confirmAppointmentButton != null) {
            confirmAppointmentButton.setOnClickListener(v -> confirmAppointment());
        }
    }

    private void confirmAppointment() {
        if (selectedCalendar != null && !selectedTimeSlot.isEmpty()) {
            // Save appointment to SharedPreferences
            saveAppointmentToPreferences();

            // Prepare result intent with appointment data
            Intent resultIntent = new Intent();
            resultIntent.putExtra("appointment_datetime", getFormattedAppointmentDateTime());
            resultIntent.putExtra("is_custom_time", isCustomTime);
            setResult(RESULT_OK, resultIntent);

            // Start the reminder service
            startReminderService();

            // Start the availability service
            startAvailabilityService();

            Toast.makeText(this,
                    "Appointment confirmed!\nYou'll receive a reminder 30 minutes before!",
                    Toast.LENGTH_LONG).show();

            // Finish activity and return to home
            finish();
        } else {
            Toast.makeText(this, "Please select a date and time slot", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFormattedAppointmentDateTime() {
        SimpleDateFormat displaySdf = new SimpleDateFormat("dd-MM-yyyy 'at' HH:mm", Locale.getDefault());
        String dateString = displaySdf.format(selectedCalendar.getTime());
        return dateString;
    }

    private void setupRecyclerView() {
        if (timeSlotsRecyclerView == null) return;

        GridLayoutManager layoutManager = new GridLayoutManager(this, 4);
        timeSlotsRecyclerView.setLayoutManager(layoutManager);

        timeSlotsAdapter = new TimeSlotsAdapter(new ArrayList<>(), timeSlot -> {
            selectedTimeSlot = timeSlot;
            isCustomTime = false;
            if (timeSlotsAdapter != null) {
                timeSlotsAdapter.setSelectedTimeSlot(timeSlot);
            }

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

                    if (selectedCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                        Toast.makeText(this, "Sunday is not available for appointments", Toast.LENGTH_SHORT).show();
                        return;
                    }

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

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showCustomDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay);

                    if (selectedCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                        Toast.makeText(this, "Sunday is not available for appointments", Toast.LENGTH_SHORT).show();
                        return;
                    }

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

                    showTimePicker(selectedYear, selectedMonth, selectedDay);
                },
                year, month, day
        );

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePicker(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    selectedTimeSlot = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                    isCustomTime = true;

                    // IMPORTANT: Update selectedCalendar with both date AND time
                    selectedCalendar.set(year, month, day, selectedHour, selectedMinute, 0);

                    if (selectedDateText != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());
                        String dateString = "Selected: " + sdf.format(selectedCalendar.getTime()) + " at " + selectedTimeSlot;
                        selectedDateText.setText(dateString);
                        selectedDateText.setVisibility(View.VISIBLE);
                    }

                    if (availableTimeTitle != null) {
                        availableTimeTitle.setVisibility(View.GONE);
                    }
                    if (timeSlotsRecyclerView != null) {
                        timeSlotsRecyclerView.setVisibility(View.GONE);
                    }

                    if (confirmAppointmentButton != null) {
                        confirmAppointmentButton.setVisibility(View.VISIBLE);
                        confirmAppointmentButton.setEnabled(true);
                        confirmAppointmentButton.setAlpha(1f);
                    }

                    Toast.makeText(this, "Custom time selected: " + selectedTimeSlot, Toast.LENGTH_SHORT).show();
                },
                hour,
                minute,
                true
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
        isCustomTime = false;

        int dayOfWeek = selectedDate.get(Calendar.DAY_OF_WEEK);
        boolean isWeekend = (dayOfWeek == Calendar.FRIDAY || dayOfWeek == Calendar.SATURDAY);

        if (isWeekend) {
            for (int hour = 10; hour <= 15; hour++) {
                if (hour == 15) {
                    availableTimeSlots.add("15:30");
                    break;
                }
                availableTimeSlots.add(String.format(Locale.getDefault(), "%02d:30", hour));
            }
        } else {
            for (int hour = 10; hour <= 17; hour++) {
                if (hour == 17) {
                    availableTimeSlots.add("17:30");
                    break;
                }
                availableTimeSlots.add(String.format(Locale.getDefault(), "%02d:30", hour));
            }
        }

        if (availableTimeTitle != null) {
            availableTimeTitle.setVisibility(View.VISIBLE);
        }
        if (timeSlotsRecyclerView != null) {
            timeSlotsRecyclerView.setVisibility(View.VISIBLE);
        }
        if (confirmAppointmentButton != null) {
            confirmAppointmentButton.setVisibility(View.VISIBLE);
        }

        if (timeSlotsAdapter != null) {
            timeSlotsAdapter.setTimeSlots(availableTimeSlots);
        }
        selectedTimeSlot = "";

        if (confirmAppointmentButton != null) {
            confirmAppointmentButton.setEnabled(false);
            confirmAppointmentButton.setAlpha(0.5f);
        }
    }

    private void saveAppointmentToPreferences() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String dateTimeStr = sdf.format(selectedCalendar.getTime());

        SharedPreferences prefs = getSharedPreferences("AppointmentPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("appointment_datetime", dateTimeStr);
        editor.putBoolean("is_custom_time", isCustomTime);
        editor.apply();

        Log.d("AppointmentActivity", "Saved appointment: " + dateTimeStr);
    }

    private void startReminderService() {
        Intent serviceIntent = new Intent(this, AppointmentReminderService.class);
        startService(serviceIntent);
        Log.d("AppointmentActivity", "Reminder service started!");
    }

    private void startAvailabilityService() {
        Intent serviceIntent = new Intent(this, DoctorAvailabilityService.class);
        startService(serviceIntent);
        Log.d("AppointmentActivity", "Availability service started!");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (availableTimeSlots != null) {
            availableTimeSlots.clear();
        }
    }
}