package xyz.moonlightpanel.app.notifications;

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

import xyz.moonlightpanel.app.MainActivity;
import xyz.moonlightpanel.app.R;

public class NotificationService extends Service {
    private Thread thread;
    private NotificationSystem notificationSystem;
    public NotificationService(){
        INSTANCE = this;
        notificationSystem = new NotificationSystem(MainActivity.INSTANCE.getBaseContext());
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
