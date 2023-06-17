package xyz.moonlightpanel.app.api;

import android.util.Log;

import java.io.IOException;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.moonlightpanel.app.Consts;
import xyz.moonlightpanel.app.MainActivity;
import xyz.moonlightpanel.app.api.models.TokenResponseModelConverter;

public class NotificationApi {
    private OkHttpClient client;
    private String cookie = "";
    public NotificationApi(){
        client = new OkHttpClient.Builder().build();
    }

    public String getMoonlightCookie(){
        if(cookie.contains("token="))
            return cookie;

        while (!MainActivity.mlCookie.contains("token="))
        {
            try {
                Thread.sleep(100);
            }
            catch (Exception ignored){

            }
        }

        cookie = MainActivity.mlCookie;
        return cookie;
    }

    public String getNewToken() {
        try {
            var url = Consts.APP_URL + "api/moonlight/notifications/register";
            Log.i("NTS","Url: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .header("Cookie", getMoonlightCookie())
                    .build();
            Log.d("NTS", "Starting Token Request");
            Response response = client.newCall(request).execute();
            var json = response.body().string();
            Log.i("NTS", "Response: " + json);

            return TokenResponseModelConverter.fromJsonString(json).getToken();
        } catch (IOException e) {
            Log.e("NTS", Objects.requireNonNull(e.getMessage()));
            return null;
        }
    }
}
