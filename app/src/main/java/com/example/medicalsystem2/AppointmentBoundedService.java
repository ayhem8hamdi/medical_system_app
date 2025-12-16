package com.example.medicalsystem2;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * BOUNDED SERVICE - Provides appointment status to connected activities.
 * Activities can bind to this service and call its public methods directly.
 */
public class AppointmentBoundedService extends Service {

    // Tag for logging
    private static final String TAG = "AppointmentBounded";


    // This is the Binder object that is returned to clients (Activity)
    // It allows clients to access public methods of this service directly
    private final IBinder binder = new LocalBinder();

    // Holds the current appointment status
    private String appointmentStatus = "No Appointment";

    //  listener to notify UI components of real-time status changes ( state mangement)
    private AppointmentStatusListener statusListener;

    // INNER CLASS: LocalBinder

    /**
     * LocalBinder allows the client to get the service instance
     * so that it can call public methods directly.
     */
   // Binder, which is Android’s standard class to provide a
    // communication channel between activity and service.
    public class LocalBinder extends Binder {
        AppointmentBoundedService getService() {
            return AppointmentBoundedService.this;
        }
    }

    // ================================
    // SERVICE LIFECYCLE METHODS
    // ================================

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "========== SERVICE CREATED ==========");
        // Called when the service is first created
        // Initialization code can go here
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "========== SERVICE STARTED ==========");
        // Called when the service is started with startService()
        // START_STICKY ensures the service is restarted if killed by the system
        return START_STICKY;
    }


     // Called when an activity binds to this service.
     //Returns the Binder object which allows the activity to communicate with the service.

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "========== ACTIVITY BINDING ==========");
        return binder; // Provide the bridge object to the client
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "========== ACTIVITY UNBINDING ==========");
        // Called when all clients have unbound
        return false; // Return true if you want onRebind() to be called later
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "========== SERVICE DESTROYED ==========");
        // Cleanup resources here if needed
    }

    // PUBLIC METHODS FOR ACTIVITY
    // les methods qu'on peut acces from binder

    /**
     * Get current appointment status
     * Activities can call this directly because they are bound to the service.
     */
    public String getAppointmentStatus() {
        Log.d(TAG, ">>> Method Called: getAppointmentStatus()");

        // Step 1: Read appointment date-time from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("AppointmentPrefs", MODE_PRIVATE);
        String appointmentDateTime = prefs.getString("appointment_datetime", "");

        // Step 2: Check if an appointment exists
        if (appointmentDateTime.isEmpty()) {
            appointmentStatus = "No Appointment Scheduled";
            Log.d(TAG, "Result: " + appointmentStatus);
            return appointmentStatus;
        }

        try {
            // Step 3: Parse the saved appointment time string into a Calendar object
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Calendar appointmentTime = Calendar.getInstance();
            appointmentTime.setTime(sdf.parse(appointmentDateTime));

            // Step 4: Get current time
            Calendar now = Calendar.getInstance();

            // Step 5: Remove seconds for accurate minute-level comparison
            appointmentTime.set(Calendar.SECOND, 0);
            now.set(Calendar.SECOND, 0);

            // Step 6: Calculate time difference in minutes
            long diffInMillis = appointmentTime.getTimeInMillis() - now.getTimeInMillis();
            long diffInMinutes = diffInMillis / 60000;

            // Step 7: Set appointment status based on time difference
            if (diffInMinutes > 0) {
                appointmentStatus = "Appointment in " + diffInMinutes + " minutes";
            } else if (diffInMinutes < 0) {
                appointmentStatus = "Appointment Completed";
            } else {
                appointmentStatus = "Appointment Starting Now!";
            }

        } catch (Exception e) {
            appointmentStatus = "Error checking appointment";
            Log.e(TAG, "Error: " + e.getMessage());
        }

        Log.d(TAG, "Result: " + appointmentStatus);

        // Step 8: Notify any listener/UI component about status change
        if (statusListener != null) {
            statusListener.onStatusChanged(appointmentStatus);
        }

        return appointmentStatus;
    }

    /**
     * Get minutes remaining until appointment
     * Returns -1 if no appointment exists
     */
    public int getMinutesUntilAppointment() {
        Log.d(TAG, ">>> Method Called: getMinutesUntilAppointment()");

        SharedPreferences prefs = getSharedPreferences("AppointmentPrefs", MODE_PRIVATE);
        String appointmentDateTime = prefs.getString("appointment_datetime", "");

        if (appointmentDateTime.isEmpty()) {
            Log.d(TAG, "No appointment found");
            return -1;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Calendar appointmentTime = Calendar.getInstance();
            appointmentTime.setTime(sdf.parse(appointmentDateTime));

            Calendar now = Calendar.getInstance();
            long diffInMillis = appointmentTime.getTimeInMillis() - now.getTimeInMillis();
            int minutes = (int) (diffInMillis / 60000);

            Log.d(TAG, "Result: " + minutes + " minutes");
            return minutes;

        } catch (Exception e) {
            Log.e(TAG, "Error calculating minutes: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Get doctor name associated with appointment
     */
    public String getDoctorName() {
        Log.d(TAG, ">>> Method Called: getDoctorName()");
        return "Dr. Ahmed Hassan"; // Hardcoded for now, can be dynamic
    }

    /**
     * Check if there is any appointment scheduled
     */
    public boolean hasAppointment() {
        Log.d(TAG, ">>> Method Called: hasAppointment()");

        SharedPreferences prefs = getSharedPreferences("AppointmentPrefs", MODE_PRIVATE);
        String appointmentDateTime = prefs.getString("appointment_datetime", "");
        boolean hasAppointment = !appointmentDateTime.isEmpty();

        Log.d(TAG, "Result: " + hasAppointment);
        return hasAppointment;
    }

    // ================================
    // LISTENER INTERFACE FOR UI
    // ================================

    /**
     * Listener interface to notify activity/UI about status changes
     */
    public interface AppointmentStatusListener {
        void onStatusChanged(String newStatus);
    }

    /**
     * Set a listener for status updates
     */
    public void setStatusListener(AppointmentStatusListener listener) {
        Log.d(TAG, ">>> Listener Set");
        this.statusListener = listener;
    }
}
