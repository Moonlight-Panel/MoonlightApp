package xyz.moonlightpanel.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        checkPermission("android.permission.READ_MEDIA_VIDEO", 200);
        checkPermission("android.permission.READ_MEDIA_AUDIO", 200);
        checkPermission("android.permission.READ_MEDIA_IMAGES", 200);
        checkPermission("android.permission.READ_EXTERNAL_STORAGE", 200);
        checkPermission("android.permission.WRITE_EXTERNAL_STORAGE", 200);
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
            session.setPromptDelegate(new MoonlightPrompt());
            sRuntime.setActivityDelegate(
                    pendingIntent -> {
                        final GeckoResult<Intent> result = new GeckoResult<>();
                        try {
                            final int code = mNextActivityResultCode++;
                            mPendingActivityResult.put(code, result);
                            MainActivity.this.startIntentSenderForResult(
                                    pendingIntent.getIntentSender(), code, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            result.completeExceptionally(e);
                        }
                        return result;
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
            session.setContentDelegate(new GeckoSession.ContentDelegate() {
                @Override
                public void onExternalResponse(@NonNull GeckoSession session, @NonNull WebResponse response) {
                    downloadFile(response);
                }
            });
        }

        if(li.hasExtra("url")) {
            var fullUrl = Consts.APP_URL + li.getStringExtra("url");
            session.loadUri(fullUrl);

            li.removeExtra("url");
        }
        else if(li.getCategories() != null) {
            if (li.getCategories().contains("android.intent.category.BROWSABLE")) {
                var url = Objects.requireNonNull(li.getData()).toString();
                Log.i("LIA", "Opening Moonlight Url: " + url);


                session.loadUri(url);
                li.removeCategory("android.intent.category.BROWSABLE");/*
            li.removeCategory("android.intent.category.DEFAULT");
            li.addCategory("android.intent.category.LAUNCHER");
            li.setAction("android.intent.action.MAIN");*/
            }
        }

        view.setSession(session);

        Intent intent = new Intent(this, NotificationService.class);
        startForegroundService(intent);
    }

    private int mNextActivityResultCode = 10;
    public void checkPermission(String permission, int requestCode)
    {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { permission }, requestCode);
        }
    }

    private static final int REQUEST_FILE_PICKER = 1;
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == REQUEST_FILE_PICKER) {
            final MoonlightPrompt prompt =
                    (MoonlightPrompt) session.getPromptDelegate();
            prompt.onFileCallbackResult(resultCode, data);
        } else if (mPendingActivityResult.containsKey(requestCode)) {
            final GeckoResult<Intent> result = mPendingActivityResult.remove(requestCode);

            if (resultCode == Activity.RESULT_OK) {
                result.complete(data);
            } else {
                result.completeExceptionally(new RuntimeException("Unknown error"));
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private HashMap<Integer, GeckoResult<Intent>> mPendingActivityResult = new HashMap<>();

    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 3;
    @Override
    public void onBackPressed() {
        session.goBack();
    }

    private String getFileName(final WebResponse response) {
        String filename;
        String contentDispositionHeader;
        if (response.headers.containsKey("content-disposition")) {
            contentDispositionHeader = response.headers.get("content-disposition");
        } else {
            contentDispositionHeader =
                    response.headers.getOrDefault("Content-Disposition", "default filename=moonlight_download");
        }
        Pattern pattern = Pattern.compile("(filename=\"?)(.+)(\"?)");
        Matcher matcher = pattern.matcher(contentDispositionHeader);
        if (matcher.find()) {
            filename = matcher.group(2).replaceAll("\\s", "%20");
        } else {
            filename = "moonlight_download";
        }

        return filename;
    }

    private LinkedList<WebResponse> mPendingDownloads = new LinkedList<>();
    private void downloadFile(final WebResponse response) {

        if (response.body == null) {
            return;
        }

        if (ContextCompat.checkSelfPermission(
                MainActivity.this, "android.permission.WRITE_EXTERNAL_STORAGE")
                != PackageManager.PERMISSION_GRANTED) {
            mPendingDownloads.add(response);
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[] {"android.permission.WRITE_EXTERNAL_STORAGE"},
                    REQUEST_WRITE_EXTERNAL_STORAGE);
            //return;
        }

        String filename = getFileName(response);

        filename = filename.replaceAll("\"", "");

        try {
            String downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            .getAbsolutePath()
                            + "/"
                            + filename;

            Log.i("LOGTAG", "Downloading to: " + downloadsPath);
            int bufferSize = 1024; // to read in 1Mb increments
            byte[] buffer = new byte[bufferSize];
            try (OutputStream out = new BufferedOutputStream(getFileStream(filename))) {
                int len;
                while ((len = response.body.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                session.loadUri("javascript:window.moonlight.toasts.info(\"Download completed: Saved as " + downloadsPath + "\")");
            } catch (Throwable e) {
                Log.i("LOGTAG", e.toString());
            }
        } catch (Throwable e) {
            Log.i("LOGTAG", e.toString());
        }
    }

    private OutputStream getFileStream(String fileName) throws FileNotFoundException {
        OutputStream fos;
        var context = getBaseContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            ContentValues values = new ContentValues();

            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);       //file name
            //values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");        //file extension, will automatically add to file
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);     //end "/" is not mandatory

            Uri uri = context.getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);      //important!

            fos = context.getContentResolver().openOutputStream(uri);
        } else {
            String docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
            File file = new File(docsDir, fileName);
            fos = new FileOutputStream(file);
        }
        return fos;
    }
}