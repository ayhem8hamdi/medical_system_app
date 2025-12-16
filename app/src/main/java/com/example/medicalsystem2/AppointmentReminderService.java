package com.example.medicalsystem2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AppointmentReminderService extends Service {

    private static final String TAG = "AppointmentReminder";
    //A notification channel is registered to allow notifications to be displayed
    // on Android 8.0+ and to define their importance and behavior.
    // (without it android won't let u display ur notification)
    private static final String CHANNEL_ID = "AppointmentReminderChannel";
    private static final int NOTIFICATION_ID = 1001;

    //The Handler is used to schedule and repeatedly execute a
    // task every 5 seconds in the background without blocking the main thread.
    private Handler handler;

    // We find in it the logic that will executed in handler
    private Runnable checkAppointmentRunnable;
    //java object : The Ringtone class is used to play the system’s default sound
    // sound as an audible alert when an appointment reminder is triggered.
    private Ringtone ringtone;
    private boolean notificationShown = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "========== Service Created! ==========");


        createNotificationChannel();
        handler = new Handler();

        // Check appointments every 5 secs
        checkAppointmentRunnable = new Runnable() {
            @Override
            public void run() {
                checkForUpcomingAppointments();
                handler.postDelayed(this, 5000); // Check every 5 seconds : re execute itself
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "========== Service Started! ==========");

        // Start checking for appointments
        handler.post(checkAppointmentRunnable);

        return START_STICKY; // Service will restart if killed by system because of cpu overuse etc
    }

    private void checkForUpcomingAppointments() {
        //MODE_PRIVATE ensures that the SharedPreferences data is accessible only to the current
        // application and cannot be read or modified by other apps (malware etc)
        SharedPreferences prefs = getSharedPreferences("AppointmentPrefs", MODE_PRIVATE);

        // second default value
        String appointmentDateTime = prefs.getString("appointment_datetime", "");

        if (appointmentDateTime.isEmpty()) {
            Log.d(TAG, "No appointment scheduled");
            return; //complete this method and wait 5 sec to repeat the process
        }

        //This code converts the appointment time from a string into a calendar object,
        // gets the current time, normalizes both by removing seconds,
        // and calculates the remaining time until the appointment in
        // milliseconds, seconds, and minutes for accurate comparison.
        try {
            //Defines how date and time text is written ex: "2025-03-14 16:45"
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            //creates a calendar object :By default, it is initialized
            // with the current date and time according to the device’s clock.
            Calendar appointmentTime = Calendar.getInstance();
            //sdf.parse() converts the String into a Date . format sdf : yyyy-mm-dd ...
            appointmentTime.setTime(sdf.parse(appointmentDateTime));

            Calendar now = Calendar.getInstance();

            // Set seconds to 0 for accurate comparison
            appointmentTime.set(Calendar.SECOND, 0);
            now.set(Calendar.SECOND, 0);

            // Calculate difference in seconds
            long diffInMillis = appointmentTime.getTimeInMillis() - now.getTimeInMillis();
            long diffInSeconds = diffInMillis / 1000;
            long diffInMinutes = diffInSeconds / 60;

            Log.d(TAG, "📅 Appointment scheduled for: " + sdf.format(appointmentTime.getTime()));
            Log.d(TAG, "🕐 Current time: " + sdf.format(now.getTime()));
            Log.d(TAG, "⏱️ Minutes until appointment: " + diffInMinutes);
            Log.d(TAG, "⏱️ Seconds until appointment: " + diffInSeconds);

            // If appointment is in 1 minute (between 50 and 70 seconds to catch it reliably)
            if (diffInSeconds >= 50 && diffInSeconds <= 70 && !notificationShown) {
                Log.d(TAG, "🔔 REMINDER TRIGGERED - Appointment in 1 minute!");
                Log.d(TAG, "⏰ NOTIFICATION WILL BE SHOWN NOW");
                showNotificationWithRingtone();
                notificationShown = true;
            } else if (diffInSeconds >= 50 && diffInSeconds <= 70 && notificationShown) {
                // juste clarification
                Log.d(TAG, "⚠️ Reminder already shown, waiting for appointment");
            } else if (diffInSeconds > 70) {
                Log.d(TAG, "⏳ Appointment is still " + diffInSeconds + " seconds away (need to wait until 50-70 seconds)");
            } else if (diffInSeconds < 50) {
                Log.d(TAG, "⚡ Appointment is very soon! " + diffInSeconds + " seconds remaining");
            }
            // tous les else if pour logging ctt

            // If appointment time has passed, clear it
            // pour que sharedprefs stay empty
            if (diffInSeconds < 0) {
                Log.d(TAG, "✅ Appointment time has passed, clearing from preferences");
                prefs.edit().remove("appointment_datetime").apply();
                notificationShown = false;
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error checking appointment: " + e.getMessage(), e);
        }
    }


    // This method creates and posts notification to alert the user of an upcoming appointment.
    private void showNotificationWithRingtone() {
        // Log a debug message to indicate that the notification method has started
        Log.d(TAG, "📢 Showing notification (non-interactive)");

        // Create a NotificationCompat.Builder to build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                // Set the small icon for the notification (mandatory)
                .setContentTitle("Appointment Reminder 🏥")
                // Set the title text of the notification
                .setContentText("Your appointment is in 1 minute!")
                // Set the main content/message
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // Set the priority so it can pop up as heads-up on newer Android versions
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                // Categorize this notification as an alarm (affects behavior)
                // to become same behaviour as alarm
                .setAutoCancel(true)
                // Dismiss the notification automatically when user taps it
                .setVibrate(new long[]{0, 1000, 500, 1000});
        // Vibration pattern: wait 0ms, vibrate 1s, pause 0.5s, vibrate 1s

        // Get the NotificationManager system service to actually post notifications
        // NotificationManager : c'est qui qui handle every notif in android
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Post the notification with a unique ID so it can be updated or removed later
        // trigger notification
        notificationManager.notify(NOTIFICATION_ID, builder.build());

        // Log a message indicating the notification was successfully posted
        Log.d(TAG, "✅ Non-interactive notification posted");

        // Play a custom ringtone (method defined elsewhere)
        playRingtone();

        // Vibrate the phone manually (method defined elsewhere)
        vibratePhone();
    }


    //This method retrieves the system’s default ringtone,
    // plays it immediately, and automatically stops
    // it after 30 seconds using a Handler,
    // while logging errors if playback fails.
    private void playRingtone() {
        try {
            // Get the default ringtone URI for the phone
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

            // Create a Ringtone object using the application context and the ringtone URI
            // This object allows us to play, stop, and control the ringtone
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);

            // Start playing the ringtone immediately
            ringtone.play();

            // its only for debugging
            Log.d(TAG, "🔊 Ringtone is playing!");

            // Schedule a task to automatically stop the ringtone after 30 seconds
            // Using the Handler to post a delayed Runnable ensures it runs on the main thread after 30,000 milliseconds
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Call stopRingtone() to stop the sound
                    stopRingtone();
                }
            }, 30000); // 30,000 milliseconds = 30 seconds

        } catch (Exception e) {
            // Catch any exceptions (e.g., ringtone file not found or playback issues)
            // Log the error for debugging purposes
            Log.e(TAG, " Error playing ringtone: " + e.getMessage(), e);
        }
    }

    private void stopRingtone() {
        // Check if the ringtone object exists and is currently playing
        if (ringtone != null && ringtone.isPlaying()) {
            // Stop the ringtone playback immediately
            ringtone.stop();
            Log.d(TAG, "🔇 Ringtone stopped!");
        }
    }

    //This method makes the phone vibrate in a
    // defined pattern using vibration hardware, handles
    // differences between Android versions,
    // and logs whether vibration was successfully started or not.
    private void vibratePhone() {
        try {
            // Get the Vibrator system service to control device vibration
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

            // Check if the vibrator exists and the device actually has vibration hardware
            if (vibrator != null && vibrator.hasVibrator()) {

                // Define a vibration pattern in milliseconds
                // Pattern: wait 0ms, vibrate 1s, pause 0.5s, vibrate 1s, pause 0.5s, vibrate 1s
                long[] pattern = {0, 1000, 500, 1000, 500, 1000};

                // For Android 13 (API level 33 / TIRAMISU) and above
                // check if we have android 13 and above
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Create a vibration waveform with audio attributes for alarms
                    vibrator.vibrate(
                            VibrationEffect.createWaveform(pattern, -1), // -1 = do not repeat
                            new android.media.AudioAttributes.Builder() // vibrator builder
                                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                                    // Treat as alarm
                                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    // Sound-related vibration
                                    .build()
                    );
                } else {
                    // For older Android versions, use the deprecated simple vibrate method
                    vibrator.vibrate(pattern, -1); // -1 = do not repeat
                }

                Log.d(TAG, "📳 Phone is vibrating!");
            } else {
                Log.w(TAG, "⚠️ Device does not have vibrator");
            }

        } catch (Exception e) {
            // Catch and log any exceptions related to vibration
            Log.e(TAG, "❌ Error vibrating: " + e.getMessage(), e);
        }
    }



    private void createNotificationChannel() {
        // Only create a notification channel on Android 8.0 (Oreo / API 26) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Create a new NotificationChannel object
            // Parameters:
            // 1. CHANNEL_ID → unique string identifier for this channel
            // 2. "Appointment Reminders" → visible name for users in system settings
            // 3. IMPORTANCE_HIGH → ensures notifications are prominent, make sound, and can pop up as heads-up
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Appointment Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );

            // Set a description visible in system notification settings
            // existe dans any android fl parameter where users can view and control notifications for each app.
            channel.setDescription("Notifications for upcoming appointments");

            // Enable vibration for notifications in this channel
            channel.enableVibration(true);

            // Set a custom vibration pattern: wait 0ms, vibrate 1s, pause 0.5s, vibrate 1s
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});

            // Get the system NotificationManager to register the channel
            NotificationManager manager = getSystemService(NotificationManager.class);

            // Register the channel with the system
            // If the channel already exists, Android will ignore this call
            manager.createNotificationChannel(channel);

            // Log a debug message indicating the channel was successfully created
            Log.d(TAG, "✅ Notification channel created");
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop handler
        if (handler != null) {
            handler.removeCallbacks(checkAppointmentRunnable);
        }

        // Stop ringtone
        stopRingtone();
    }
    // unbounded just start it and not call the methods directly
    //onBind() returns null, meaning no clients can bind to
    // the service, making it an unbounded (started) service
    // that runs independently in the background.
    //client: A component (like an Activity)

    @Override
    public IBinder onBind(Intent intent) {

        // Returning null means that **binding is not allowed**
        // No client can connect to this service to interact with it.
        return null;
    }

}