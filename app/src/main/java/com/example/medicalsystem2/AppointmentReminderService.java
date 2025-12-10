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
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AppointmentReminderService extends Service {

    private static final String TAG = "AppointmentReminder";
    private static final String CHANNEL_ID = "AppointmentReminderChannel";
    private static final int NOTIFICATION_ID = 1001;

    private Handler handler;
    private Runnable checkAppointmentRunnable;
    private Ringtone ringtone;
    private boolean notificationShown = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service Created!");

        createNotificationChannel();
        handler = new Handler();

        // Check appointments every 1 minute
        checkAppointmentRunnable = new Runnable() {
            @Override
            public void run() {
                checkForUpcomingAppointments();
                handler.postDelayed(this, 60000); // Check every 60 seconds
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service Started!");

        // Start checking for appointments
        handler.post(checkAppointmentRunnable);

        return START_STICKY; // Service will restart if killed by system
    }

    private void checkForUpcomingAppointments() {
        SharedPreferences prefs = getSharedPreferences("AppointmentPrefs", MODE_PRIVATE);
        String appointmentDateTime = prefs.getString("appointment_datetime", "");

        if (appointmentDateTime.isEmpty()) {
            Log.d(TAG, "No appointment scheduled");
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Calendar appointmentTime = Calendar.getInstance();
            appointmentTime.setTime(sdf.parse(appointmentDateTime));

            Calendar now = Calendar.getInstance();

            // Calculate difference in minutes
            long diffInMillis = appointmentTime.getTimeInMillis() - now.getTimeInMillis();
            long diffInMinutes = diffInMillis / (60 * 1000);

            Log.d(TAG, "Minutes until appointment: " + diffInMinutes);

            // If appointment is in 30 minutes (between 29 and 31 to catch it)
            if (diffInMinutes >= 29 && diffInMinutes <= 31 && !notificationShown) {
                showNotificationWithRingtone();
                notificationShown = true;

                // Clear appointment after notification
                prefs.edit().remove("appointment_datetime").apply();
            }

            // If appointment time has passed, clear it
            if (diffInMinutes < 0) {
                prefs.edit().remove("appointment_datetime").apply();
                notificationShown = false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error checking appointment: " + e.getMessage());
        }
    }

    private void showNotificationWithRingtone() {
        Log.d(TAG, "Showing notification with ringtone!");

        // Create intent to open appointment activity when notification is tapped
        Intent intent = new Intent(this, appointment.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Appointment Reminder 🏥")
                .setContentText("Your appointment is in 30 minutes!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 1000, 500, 1000}); // Vibration pattern

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());

        // Play ringtone
        playRingtone();

        // Vibrate
        vibratePhone();
    }

    private void playRingtone() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
            ringtone.play();

            Log.d(TAG, "Ringtone playing!");

            // Stop ringtone after 30 seconds automatically
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopRingtone();
                }
            }, 30000);

        } catch (Exception e) {
            Log.e(TAG, "Error playing ringtone: " + e.getMessage());
        }
    }

    private void stopRingtone() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
            Log.d(TAG, "Ringtone stopped!");
        }
    }

    private void vibratePhone() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 1000, 500, 1000, 500, 1000};
                vibrator.vibrate(pattern, -1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error vibrating: " + e.getMessage());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Appointment Reminders",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for upcoming appointments");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service Destroyed!");

        // Stop handler
        if (handler != null) {
            handler.removeCallbacks(checkAppointmentRunnable);
        }

        // Stop ringtone
        stopRingtone();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}