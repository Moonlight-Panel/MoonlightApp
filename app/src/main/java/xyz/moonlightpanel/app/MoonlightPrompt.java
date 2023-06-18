package xyz.moonlightpanel.app;

import static xyz.moonlightpanel.app.MainActivity.mlCookie;
import static xyz.moonlightpanel.app.MainActivity.needCookie;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;

import java.util.ArrayList;
import java.util.Locale;

public class MoonlightPrompt implements GeckoSession.PromptDelegate {
    @Nullable
    @Override
    public GeckoResult<GeckoSession.PromptDelegate.PromptResponse> onAlertPrompt(@NonNull GeckoSession session, @NonNull GeckoSession.PromptDelegate.AlertPrompt prompt) {
        if(prompt.message.startsWith("COOKIES")) {
            mlCookie = prompt.message.replace("COOKIES", "");
            Log.i("NTS", "Alert: " + mlCookie);
            if(mlCookie.contains("token="))
                needCookie = false;
            return null;
        }
        else if (prompt.message.startsWith("MLCMDreload")){
            session.reload();
        }
        return GeckoSession.PromptDelegate.super.onAlertPrompt(session, prompt);
    }

    @Override
    public GeckoResult<GeckoSession.PromptDelegate.PromptResponse> onFilePrompt(GeckoSession session, GeckoSession.PromptDelegate.FilePrompt prompt) {
        final Activity activity = MainActivity.INSTANCE;
        if (activity == null) {
            return GeckoResult.fromValue(prompt.dismiss());
        }

        // Merge all given MIME types into one, using wildcard if needed.
        String mimeType = null;
        String mimeSubtype = null;
        if (prompt.mimeTypes != null) {
            for (final String rawType : prompt.mimeTypes) {
                final String normalizedType = rawType.trim().toLowerCase(Locale.ROOT);
                final int len = normalizedType.length();
                int slash = normalizedType.indexOf('/');
                if (slash < 0) {
                    slash = len;
                }
                final String newType = normalizedType.substring(0, slash);
                final String newSubtype = normalizedType.substring(Math.min(slash + 1, len));
                if (mimeType == null) {
                    mimeType = newType;
                } else if (!mimeType.equals(newType)) {
                    mimeType = "*";
                }
                if (mimeSubtype == null) {
                    mimeSubtype = newSubtype;
                } else if (!mimeSubtype.equals(newSubtype)) {
                    mimeSubtype = "*";
                }
            }
        }

        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(
                (mimeType != null ? mimeType : "*") + '/' + (mimeSubtype != null ? mimeSubtype : "*"));
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        if (prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        if (prompt.mimeTypes.length > 0) {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, prompt.mimeTypes);
        }

        GeckoResult<GeckoSession.PromptDelegate.PromptResponse> res = new GeckoResult<GeckoSession.PromptDelegate.PromptResponse>();

        try {
            mFileResponse = res;
            mFilePrompt = prompt;
            activity.startActivityForResult(intent, filePickerRequestCode);
        } catch (final ActivityNotFoundException e) {
            Log.e("LOGTAG", "Cannot launch activity", e);
            return GeckoResult.fromValue(prompt.dismiss());
        }

        return res;
    }

    public void onFileCallbackResult(final int resultCode, final Intent data) {
        if (mFileResponse == null) {
            return;
        }

        final GeckoResult<GeckoSession.PromptDelegate.PromptResponse> res = mFileResponse;
        mFileResponse = null;

        final GeckoSession.PromptDelegate.FilePrompt prompt = mFilePrompt;
        mFilePrompt = null;

        if (resultCode != Activity.RESULT_OK || data == null) {
            res.complete(prompt.dismiss());
            return;
        }

        final Uri uri = data.getData();
        final ClipData clip = data.getClipData();

        if (prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.SINGLE
                || (prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE && clip == null)) {
            res.complete(prompt.confirm(MainActivity.INSTANCE, uri));
        } else if (prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE) {
            if (clip == null) {
                Log.w("LOGTAG", "No selected file");
                res.complete(prompt.dismiss());
                return;
            }
            final int count = clip.getItemCount();
            final ArrayList<Uri> uris = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                uris.add(clip.getItemAt(i).getUri());
            }
            res.complete(prompt.confirm(MainActivity.INSTANCE, uris.toArray(new Uri[uris.size()])));
        }
    }

    private GeckoResult<GeckoSession.PromptDelegate.PromptResponse> mFileResponse;
    private GeckoSession.PromptDelegate.FilePrompt mFilePrompt;
    public int filePickerRequestCode = 1;
}
