package xyz.moonlightpanel.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.StorageController;
import org.mozilla.geckoview.WebExtension;
import org.mozilla.geckoview.WebRequest;
import org.mozilla.geckoview.WebResponse;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import xyz.moonlightpanel.app.notifications.NotificationService;

public class MainActivity extends AppCompatActivity {
    private static GeckoRuntime sRuntime;
    private static GeckoSession session;
    public static MainActivity INSTANCE;
    public static String mlCookie = "";
    public static boolean needCookie = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        INSTANCE = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission("android.permission.POST_NOTIFICATIONS", 200);
        checkPermission("android.permission.FOREGROUND_SERVICE", 200);

        GeckoView view = findViewById(R.id.firefox);

        var li = getIntent();
        if (sRuntime == null) {
            session = new GeckoSession();

// Workaround for Bug 1758212
            session.setContentDelegate(new GeckoSession.ContentDelegate() {});
            // GeckoRuntime can only be initialized once per process
            sRuntime = GeckoRuntime.create(this);

            session.getSettings().setUserAgentOverride(Consts.USER_AGENT);
            session.open(sRuntime);
            if(li.hasExtra("url")) {
                var fullUrl = Consts.APP_URL + li.getStringExtra("url");
                session.loadUri(fullUrl);

                li.removeExtra("url");
            }
            else if(li.getCategories().contains("android.intent.category.BROWSABLE")){
                var url = Objects.requireNonNull(li.getData()).toString();
                Log.i("LIA", "Opening Moonlight Url: " + url);


                session.loadUri(url);
                li.removeCategory("android.intent.category.BROWSABLE");
            }
            else {
                session.loadUri(Consts.APP_URL);
            }
            session.setPromptDelegate(new GeckoSession.PromptDelegate() {
                @Nullable
                @Override
                public GeckoResult<PromptResponse> onAlertPrompt(@NonNull GeckoSession session, @NonNull AlertPrompt prompt) {
                    if(prompt.message.startsWith("COOKIES")) {
                        mlCookie = prompt.message.replace("COOKIES", "");
                        Log.i("NTS", "Alert: " + mlCookie);
                        if(mlCookie.contains("token="))
                            needCookie = false;
                        return null;
                    }
                    return GeckoSession.PromptDelegate.super.onAlertPrompt(session, prompt);
                }
            });
            session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
                @Override
                public void onLocationChange(@NonNull GeckoSession session, @Nullable String url, @NonNull List<GeckoSession.PermissionDelegate.ContentPermission> perms) {
                    if(needCookie)
                        session.loadUri("javascript:alert('COOKIES' + document.cookie)");
                    GeckoSession.NavigationDelegate.super.onLocationChange(session, url, perms);
                }

                @Nullable
                @Override
                public GeckoResult<GeckoSession> onNewSession(@NonNull GeckoSession session, @NonNull String uri) {
                    Log.i("NWT", uri);
                    CustomTabsIntent intent = new CustomTabsIntent.Builder()
                            .build();
                    intent.launchUrl(MainActivity.this, Uri.parse(uri));
                    return GeckoSession.NavigationDelegate.super.onNewSession(session, uri);
                }
            });
        }

        if(li.hasExtra("url")) {
            var fullUrl = Consts.APP_URL + li.getStringExtra("url");
            session.loadUri(fullUrl);

            li.removeExtra("url");
        }
        else if(li.getCategories().contains("android.intent.category.BROWSABLE")){
            var url = Objects.requireNonNull(li.getData()).toString();
            Log.i("LIA", "Opening Moonlight Url: " + url);


            session.loadUri(url);
            li.removeCategory("android.intent.category.BROWSABLE");
            li.removeCategory("android.intent.category.DEFAULT");
            li.addCategory("android.intent.category.LAUNCHER");
            li.setAction("android.intent.action.MAIN");
        }

        view.setSession(session);

        Intent intent = new Intent(this, NotificationService.class);
        startForegroundService(intent);
    }

    public void checkPermission(String permission, int requestCode)
    {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { permission }, requestCode);
        }
    }

    @Override
    public void onBackPressed() {
        session.goBack();
    }
}