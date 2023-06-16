package xyz.moonlightpanel.app.notifications;

import android.os.Binder;

public class NotificationServiceBinder extends Binder {
    public NotificationService getService() {
        return NotificationService.INSTANCE;
    }
}
