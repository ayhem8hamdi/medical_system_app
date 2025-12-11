package com.example.medicalsystem2;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DoctorAvailabilityService extends Service {

    private static final String TAG = "DoctorAvailability";
    public static final String ACTION_STATUS_CHANGED = "com.example.medicalsystem2.DOCTOR_STATUS_CHANGED";
    public static final String EXTRA_STATUS = "status";
    public static final String STATUS_AVAILABLE = "Available";
    public static final String STATUS_IN_CONSULTATION = "In Consultation";

    private Handler handler;
    private Runnable checkStatusRunnable;
    private String currentStatus = STATUS_AVAILABLE;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service Created!");
        handler = new Handler();

        // Check status every 5 seconds for real-time updates
        checkStatusRunnable = new Runnable() {
            @Override
            public void run() {
                checkDoctorStatus();
                handler.postDelayed(this, 5000); // Check every 5 seconds
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service Started!");
        handler.post(checkStatusRunnable);
        return START_STICKY;
    }

    private void checkDoctorStatus() {
        SharedPreferences prefs = getSharedPreferences("AppointmentPrefs", MODE_PRIVATE);
        String appointmentDateTime = prefs.getString("appointment_datetime", "");

        if (appointmentDateTime.isEmpty()) {
            Log.d(TAG, "No appointment scheduled");
            updateStatus(STATUS_AVAILABLE);
            return;
        }

        try {
            // Match the format used in appointment.java: yyyy-MM-dd HH:mm
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Calendar appointmentTime = Calendar.getInstance();

            // Parse the saved appointment time
            appointmentTime.setTime(sdf.parse(appointmentDateTime));

            Calendar now = Calendar.getInstance();

            // Set appointment time with seconds to 0 for accurate comparison
            appointmentTime.set(Calendar.SECOND, 0);
            now.set(Calendar.SECOND, 0);

            // Calculate consultation end time (appointment + 1 minute for testing)
            Calendar consultationEndTime = (Calendar) appointmentTime.clone();
            consultationEndTime.add(Calendar.MINUTE, 1);

            Log.d(TAG, "Appointment time: " + sdf.format(appointmentTime.getTime()));
            Log.d(TAG, "Current time: " + sdf.format(now.getTime()));
            Log.d(TAG, "Consultation ends: " + sdf.format(consultationEndTime.getTime()));

            // Check if we're currently in consultation period
            if (now.getTimeInMillis() >= appointmentTime.getTimeInMillis() &&
                    now.getTimeInMillis() < consultationEndTime.getTimeInMillis()) {
                // Doctor is IN CONSULTATION
                Log.d(TAG, "Doctor is IN CONSULTATION");
                updateStatus(STATUS_IN_CONSULTATION);
            } else if (now.getTimeInMillis() >= consultationEndTime.getTimeInMillis()) {
                // Consultation ended, doctor is AVAILABLE again
                Log.d(TAG, "Consultation ended, Doctor is AVAILABLE");
                updateStatus(STATUS_AVAILABLE);
                // Clear the appointment since it's done
                prefs.edit().remove("appointment_datetime").apply();
            } else {
                // Appointment is in the future
                Log.d(TAG, "Appointment not started yet, Doctor is AVAILABLE");
                updateStatus(STATUS_AVAILABLE);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error checking status: " + e.getMessage(), e);
            updateStatus(STATUS_AVAILABLE);
        }
    }

    private void updateStatus(String newStatus) {
        if (!currentStatus.equals(newStatus)) {
            currentStatus = newStatus;
            Log.d(TAG, "Status changed to: " + newStatus);

            // Broadcast the status change to update UI
            Intent intent = new Intent(ACTION_STATUS_CHANGED);
            intent.putExtra(EXTRA_STATUS, newStatus);
            sendBroadcast(intent);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service Destroyed!");
        if (handler != null) {
            handler.removeCallbacks(checkStatusRunnable);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}