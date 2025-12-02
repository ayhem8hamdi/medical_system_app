package com.example.medicalsystem2;

import android.app.DatePickerDialog;
import android.os.Bundle;
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

    private TimeSlotsAdapter timeSlotsAdapter;
    private List<String> availableTimeSlots = new ArrayList<>();
    private Calendar selectedCalendar;
    private String selectedTimeSlot = "";

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

        if (confirmAppointmentButton != null) {
            confirmAppointmentButton.setOnClickListener(v -> {
                if (selectedCalendar != null && !selectedTimeSlot.isEmpty()) {
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());
                    String date = sdf.format(selectedCalendar.getTime());

                    Toast.makeText(this,
                            "Appointment confirmed for " + date + " at " + selectedTimeSlot,
                            Toast.LENGTH_LONG).show();

                    // Here you would typically save the appointment to your database
                    // and navigate to confirmation screen

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (availableTimeSlots != null) {
            availableTimeSlots.clear();
        }
    }
}