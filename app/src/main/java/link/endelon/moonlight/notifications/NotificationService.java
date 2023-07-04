package link.endelon.moonlight.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.util.Locale;

import link.endelon.moonlight.MainActivity;
import link.endelon.moonlight.R;

public class NotificationService extends Service {
    private Thread thread;
    private NotificationSystem notificationSystem;
    public NotificationService(){
        INSTANCE = this;
        notificationSystem = new NotificationSystem(getBaseContext());
        Log.d("NTS", "Starting Notification Servcie");
    }
    public static NotificationService INSTANCE;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        thread = runService();
        thread.start();
        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        var notification = createNotification();
        startForeground(1, notification);
    }
    private Notification createNotification() {
        var language = Locale.getDefault().getLanguage();
        var description = language.contains("de") ? "Moonlight empfängt nun Benachrichtigungen" : "Moonlight now listens for notifications";
        var notificationChannelId = "Notification Service";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            var channel = new NotificationChannel(
                    notificationChannelId,
                    "Notification Service",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        var builder = new Notification.Builder(
                this,
                notificationChannelId);

        return builder
                .setContentTitle("Moonlight")
                .setContentText(description).setDefaults(Notification.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
                .build();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new NotificationServiceBinder();
    }

    private Thread runService(){
        return new Thread(() -> {
            Log.d("NTS", "Thread running");
                notificationSystem.run();
        });
    }
}
