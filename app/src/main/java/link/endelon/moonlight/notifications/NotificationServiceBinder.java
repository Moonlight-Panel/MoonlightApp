package link.endelon.moonlight.notifications;

import android.os.Binder;

public class NotificationServiceBinder extends Binder {
    public NotificationService getService() {
        return NotificationService.INSTANCE;
    }
}
