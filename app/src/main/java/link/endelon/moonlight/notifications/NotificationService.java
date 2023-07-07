package link.endelon.moonlight.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import java.util.Locale;
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

        ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        var notification = createNotification();
        startForeground(1337, notification);
    }
    private Notification createNotification() {
        var language = Locale.getDefault().getLanguage();
        var description = language.contains("de") ? "Moonlight empfängt nun Benachrichtigungen, klicke hier zum deaktivieren dieser Benachrichtigung" : "Moonlight now listens for notifications, click here to disable this notification";
        var notificationChannelId = "Notification Service";
        var notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        var channel = new NotificationChannel(
                notificationChannelId,
                "Notification Service",
                NotificationManager.IMPORTANCE_HIGH
        );
        notificationManager.createNotificationChannel(channel);

        var builder = new Notification.Builder(
                this,
                notificationChannelId);
        Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getBaseContext().getPackageName())
                .putExtra(Settings.EXTRA_CHANNEL_ID, "Notification Service");
        return builder
                .setContentTitle("Moonlight")
                .setContentText(description)
                .setContentIntent(PendingIntent.getActivity(getBaseContext(), 0, intent, PendingIntent.FLAG_MUTABLE))
                .setDefaults(Notification.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
                .build();
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
