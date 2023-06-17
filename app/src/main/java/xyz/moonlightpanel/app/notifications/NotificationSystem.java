package xyz.moonlightpanel.app.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import xyz.moonlightpanel.app.Consts;
import xyz.moonlightpanel.app.MainActivity;
import xyz.moonlightpanel.app.R;
import xyz.moonlightpanel.app.api.NotificationApi;
import xyz.moonlightpanel.app.api.models.ActionModel;
import xyz.moonlightpanel.app.api.models.ActionModelConverter;
import xyz.moonlightpanel.app.api.models.NotificationModelConverter;
import xyz.moonlightpanel.app.api.models.StatusModelConverter;

public class NotificationSystem extends WebSocketListener {
    private String currentToken;
    private boolean isInLogin = false;
    private boolean failureRestart = false;
    private NotificationApi api;

    public NotificationSystem(Context context) {
        api = new NotificationApi();
    }

    public void setup() {
        try {
            var dir = new File(MainActivity.INSTANCE.getBaseContext().getCacheDir().getPath() + "notifications");
            dir.mkdir();

            var tokenFile = new File(dir.getPath() + "token.bin");

            String token = "";
            if (tokenFile.exists()) {
                var reader = new BufferedReader(new FileReader(tokenFile));

                String s;
                while ((s = reader.readLine()) != null)
                    token += s;

                reader.close();
            } else {
                token = renewToken();
            }

            currentToken = token;
        } catch (Exception ignored) {

        }
    }

    public String renewToken() {
        try {
            var dir = new File(MainActivity.INSTANCE.getBaseContext().getCacheDir().getPath() + "notifications");
            var tokenFile = new File(dir.getPath() + "token.bin");

            Log.i("NTS", "Renewing token");
            var token = api.getNewToken();
            Log.w("NTS", "New Token: " + token);

            if (tokenFile.exists())
                tokenFile.delete();

            tokenFile.createNewFile();
            var writer = new FileWriter(tokenFile);
            writer.write(token);

            writer.close();

            return token;
        } catch (Exception e) {
            Log.e("NTS", e.toString());
            return null;
        }
    }

    public void resetToken(){
        try {
            var dir = new File(MainActivity.INSTANCE.getBaseContext().getCacheDir().getPath() + "notifications");
            var tokenFile = new File(dir.getPath() + "token.bin");

            if (tokenFile.exists())
                tokenFile.delete();
        }
        catch (Exception e){
            Log.e("NTSR", e.toString());
        }
    }

    public void run() {
        setup();

        var websocketUrl = Consts.APP_URL.replace("http", "ws") + "api/moonlight/notifications/listen";

        var client = new OkHttpClient.Builder().readTimeout(1, TimeUnit.DAYS).build();
        Request request = new Request.Builder()
                .url(websocketUrl)
                .build();

        Log.d("NTS", "Creating ws");
        client.newWebSocket(request, this);

        client.dispatcher().executorService().shutdown();

        if (failureRestart) {
            failureRestart = false;
            run();
        }
    }

    public void login(WebSocket webSocket) {
        var json = "{\"action\":\"login\",\"token\":\"" + currentToken + "\"}";
        Log.d("NTS", "Starting Login: " + json);
        webSocket.send(json);
        isInLogin = true;
    }

    @Override
    public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
        Log.d("NTS", "Opened");
        login(webSocket);
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
        Log.d("NTS", "Message: " + text);
        try {
            if (isInLogin) {
                var status = StatusModelConverter.fromJsonString(text).getStatus();
                if (!status) {
                    resetToken();
                    Log.d("NTS", "Login failed");
                    run();
                }
                else {
                    Log.d("NTS", "Logged in");
                    isInLogin = false;
                }
            } else {
                var action = ActionModelConverter.fromJsonString(text).getAction();

                if (Objects.equals(action, "notify")) {
                    sendNotification(webSocket, text);
                }
            }
        } catch (Exception ignored) {

        }
    }

    @Override
    public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
        super.onFailure(webSocket, t, response);
        run();
    }

    @Override
    public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        super.onClosed(webSocket, code, reason);
        run();
    }

    private void sendNotification(WebSocket webSocket, String json) {
        try {
            var notification = NotificationModelConverter.fromJsonString(json).getNotification();
            var chn = notification.getChannel().replace(" ", "_");

            try {
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel(chn, notification.getChannel(), importance);
                channel.setDescription(notification.getChannel());
                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                NotificationManager notificationManager = MainActivity.INSTANCE.getBaseContext().getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
            catch (Exception x){
                Log.e("NTS", Objects.requireNonNull(x.getMessage()));
            }

            Intent intent = new Intent(MainActivity.INSTANCE.getBaseContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("url", notification.getUrl());
            PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.INSTANCE.getBaseContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder n = new NotificationCompat.Builder(MainActivity.INSTANCE.getBaseContext(), chn)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(notification.getTitle())
                    .setContentText(notification.getContent())
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            var nm = NotificationManagerCompat.from(MainActivity.INSTANCE.getBaseContext());

            if (ActivityCompat.checkSelfPermission(MainActivity.INSTANCE.getBaseContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                var rand = new Random();
                Log.i("NTS", "Sending...");
                nm.notify(rand.nextInt(), n.build());
            }
        }
        catch (Exception e){
            Log.e("NTS", Objects.requireNonNull(e.getMessage()));
        }
    }
}
