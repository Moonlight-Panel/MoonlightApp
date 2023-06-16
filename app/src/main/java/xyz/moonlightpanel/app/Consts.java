package xyz.moonlightpanel.app;

import android.app.Application;

import org.mozilla.geckoview.BuildConfig;

public class Consts {
    public static final String VERSION = "1.0";
    public static final String APP_URL = "https://my.endelon-hosting.de/";
    public static final String USER_AGENT = String.format("Moonlight.App/{0} using Gecko/{1}", VERSION, BuildConfig.MOZILLA_VERSION);
}
