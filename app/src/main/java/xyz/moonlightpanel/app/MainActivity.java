package xyz.moonlightpanel.app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

public class MainActivity extends AppCompatActivity {
    private static GeckoRuntime sRuntime;
    private static GeckoSession session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GeckoView view = findViewById(R.id.firefox);

        if (sRuntime == null) {
            session = new GeckoSession();

// Workaround for Bug 1758212
            session.setContentDelegate(new GeckoSession.ContentDelegate() {});
            // GeckoRuntime can only be initialized once per process
            sRuntime = GeckoRuntime.create(this);

            session.getSettings().setUserAgentOverride(Consts.USER_AGENT);
            session.open(sRuntime);
            session.loadUri(Consts.APP_URL);
        }

        view.setSession(session); // Or any other URL...
    }

    @Override
    public void onBackPressed() {
        session.goBack();
    }
}