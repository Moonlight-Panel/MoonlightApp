package link.endelon.moonlight;

import static link.endelon.moonlight.MainActivity.mlCookie;
import static link.endelon.moonlight.MainActivity.needCookie;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mozilla.geckoview.AllowOrDeny;
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
        else {
            final Activity activity = MainActivity.INSTANCE;
            if (activity == null) {
                return GeckoResult.fromValue(prompt.dismiss());
            }
            final AlertDialog.Builder builder =
                    new AlertDialog.Builder(activity)
                            .setTitle(prompt.title)
                            .setMessage(prompt.message)
                            .setPositiveButton(android.R.string.ok, /* onClickListener */ null);
            GeckoResult<PromptResponse> res = new GeckoResult<PromptResponse>();
            createStandardDialog(builder, prompt, res).show();
            return res;
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
    @Override
    public GeckoResult<PromptResponse> onButtonPrompt(
            final GeckoSession session, final ButtonPrompt prompt) {
        final Activity activity = MainActivity.INSTANCE;
        if (activity == null) {
            return GeckoResult.fromValue(prompt.dismiss());
        }
        final AlertDialog.Builder builder =
                new AlertDialog.Builder(activity).setTitle(prompt.title).setMessage(prompt.message);

        GeckoResult<PromptResponse> res = new GeckoResult<PromptResponse>();

        final DialogInterface.OnClickListener listener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            res.complete(prompt.confirm(ButtonPrompt.Type.POSITIVE));
                        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                            res.complete(prompt.confirm(ButtonPrompt.Type.NEGATIVE));
                        } else {
                            res.complete(prompt.dismiss());
                        }
                    }
                };

        builder.setPositiveButton(android.R.string.ok, listener);
        builder.setNegativeButton(android.R.string.cancel, listener);

        createStandardDialog(builder, prompt, res).show();
        return res;
    }

    @Nullable
    @Override
    public GeckoResult<PromptResponse> onRepostConfirmPrompt(
            final GeckoSession session, final RepostConfirmPrompt prompt) {
        final Activity activity = MainActivity.INSTANCE;
        if (activity == null) {
            return GeckoResult.fromValue(prompt.dismiss());
        }
        final AlertDialog.Builder builder =
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.repost_confirm_title)
                        .setMessage(R.string.repost_confirm_message);

        GeckoResult<PromptResponse> res = new GeckoResult<>();

        final DialogInterface.OnClickListener listener =
                (dialog, which) -> {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        res.complete(prompt.confirm(AllowOrDeny.ALLOW));
                    } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                        res.complete(prompt.confirm(AllowOrDeny.DENY));
                    } else {
                        res.complete(prompt.dismiss());
                    }
                };

        builder.setPositiveButton(R.string.repost_confirm_resend, listener);
        builder.setNegativeButton(R.string.repost_confirm_cancel, listener);

        createStandardDialog(builder, prompt, res).show();
        return res;
    }
    @Nullable
    @Override
    public GeckoResult<PromptResponse> onBeforeUnloadPrompt(
            final GeckoSession session, final BeforeUnloadPrompt prompt) {
        final Activity activity = MainActivity.INSTANCE;
        if (activity == null) {
            return GeckoResult.fromValue(prompt.dismiss());
        }
        final AlertDialog.Builder builder =
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.before_unload_title)
                        .setMessage(R.string.before_unload_message);

        GeckoResult<PromptResponse> res = new GeckoResult<>();

        final DialogInterface.OnClickListener listener =
                (dialog, which) -> {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        res.complete(prompt.confirm(AllowOrDeny.ALLOW));
                    } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                        res.complete(prompt.confirm(AllowOrDeny.DENY));
                    } else {
                        res.complete(prompt.dismiss());
                    }
                };

        builder.setPositiveButton(R.string.before_unload_leave_page, listener);
        builder.setNegativeButton(R.string.before_unload_stay, listener);

        createStandardDialog(builder, prompt, res).show();
        return res;
    }

    private AlertDialog createStandardDialog(
            final AlertDialog.Builder builder,
            final BasePrompt prompt,
            final GeckoResult<PromptResponse> response) {
        final AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(
                new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(final DialogInterface dialog) {
                        if (!prompt.isComplete()) {
                            response.complete(prompt.dismiss());
                        }
                    }
                });
        return dialog;
    }

    private int getViewPadding(final AlertDialog.Builder builder) {
        final TypedArray attr =
                builder
                        .getContext()
                        .obtainStyledAttributes(new int[] {android.R.attr.listPreferredItemPaddingLeft});
        final int padding = attr.getDimensionPixelSize(0, 1);
        attr.recycle();
        return padding;
    }
    private LinearLayout addStandardLayout(
            final AlertDialog.Builder builder, final String title, final String msg) {
        final ScrollView scrollView = new ScrollView(builder.getContext());
        final LinearLayout container = new LinearLayout(builder.getContext());
        final int horizontalPadding = getViewPadding(builder);
        final int verticalPadding = (msg == null || msg.isEmpty()) ? horizontalPadding : 0;
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(
                /* left */ horizontalPadding, /* top */ verticalPadding,
                /* right */ horizontalPadding, /* bottom */ verticalPadding);
        scrollView.addView(container);
        builder.setTitle(title).setMessage(msg).setView(scrollView);
        return container;
    }
    @Override
    public GeckoResult<PromptResponse> onTextPrompt(
            final GeckoSession session, final TextPrompt prompt) {
        final Activity activity = MainActivity.INSTANCE;
        if (activity == null) {
            return GeckoResult.fromValue(prompt.dismiss());
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final LinearLayout container = addStandardLayout(builder, prompt.title, prompt.message);
        final EditText editText = new EditText(builder.getContext());
        editText.setText(prompt.defaultValue);
        container.addView(editText);

        GeckoResult<PromptResponse> res = new GeckoResult<PromptResponse>();

        builder
                .setNegativeButton(android.R.string.cancel, /* listener */ null)
                .setPositiveButton(
                        android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                res.complete(prompt.confirm(editText.getText().toString()));
                            }
                        });

        createStandardDialog(builder, prompt, res).show();
        return res;
    }
    @Override
    public GeckoResult<PromptResponse> onAuthPrompt(
            final GeckoSession session, final AuthPrompt prompt) {
        final Activity activity = MainActivity.INSTANCE;
        if (activity == null) {
            return GeckoResult.fromValue(prompt.dismiss());
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final LinearLayout container = addStandardLayout(builder, prompt.title, prompt.message);

        final int flags = prompt.authOptions.flags;
        final int level = prompt.authOptions.level;
        final EditText username;
        if ((flags & AuthPrompt.AuthOptions.Flags.ONLY_PASSWORD) == 0) {
            username = new EditText(builder.getContext());
            username.setHint(R.string.username);
            username.setText(prompt.authOptions.username);
            container.addView(username);
        } else {
            username = null;
        }

        final EditText password = new EditText(builder.getContext());
        password.setHint(R.string.password);
        password.setText(prompt.authOptions.password);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(password);

        if (level != AuthPrompt.AuthOptions.Level.NONE) {
            final ImageView secure = new ImageView(builder.getContext());
            secure.setImageResource(android.R.drawable.ic_lock_lock);
            container.addView(secure);
        }

        GeckoResult<PromptResponse> res = new GeckoResult<PromptResponse>();

        builder
                .setNegativeButton(android.R.string.cancel, /* listener */ null)
                .setPositiveButton(
                        android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                if ((flags & AuthPrompt.AuthOptions.Flags.ONLY_PASSWORD) == 0) {
                                    res.complete(
                                            prompt.confirm(username.getText().toString(), password.getText().toString()));

                                } else {
                                    res.complete(prompt.confirm(password.getText().toString()));
                                }
                            }
                        });
        createStandardDialog(builder, prompt, res).show();

        return res;
    }

    private static class ModifiableChoice {
        public boolean modifiableSelected;
        public String modifiableLabel;
        public final ChoicePrompt.Choice choice;

        public ModifiableChoice(ChoicePrompt.Choice c) {
            choice = c;
            modifiableSelected = choice.selected;
            modifiableLabel = choice.label;
        }
    }
    private void addChoiceItems(
            final int type,
            final ArrayAdapter<ModifiableChoice> list,
            final ChoicePrompt.Choice[] items,
            final String indent) {
        if (type == ChoicePrompt.Type.MENU) {
            for (final ChoicePrompt.Choice item : items) {
                list.add(new ModifiableChoice(item));
            }
            return;
        }

        for (final ChoicePrompt.Choice item : items) {
            final ModifiableChoice modItem = new ModifiableChoice(item);

            final ChoicePrompt.Choice[] children = item.items;

            if (indent != null && children == null) {
                modItem.modifiableLabel = indent + modItem.modifiableLabel;
            }
            list.add(modItem);

            if (children != null) {
                final String newIndent;
                if (type == ChoicePrompt.Type.SINGLE || type == ChoicePrompt.Type.MULTIPLE) {
                    newIndent = (indent != null) ? indent + '\t' : "\t";
                } else {
                    newIndent = null;
                }
                addChoiceItems(type, list, children, newIndent);
            }
        }
    }
    private void onChoicePromptImpl(
            final GeckoSession session,
            final String title,
            final String message,
            final int type,
            final ChoicePrompt.Choice[] choices,
            final ChoicePrompt prompt,
            final GeckoResult<PromptResponse> res) {
        final Activity activity = MainActivity.INSTANCE;
        if (activity == null) {
            res.complete(prompt.dismiss());
            return;
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        addStandardLayout(builder, title, message);

        final ListView list = new ListView(builder.getContext());
        if (type == ChoicePrompt.Type.MULTIPLE) {
            list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        }

        final ArrayAdapter<ModifiableChoice> adapter =
                new ArrayAdapter<ModifiableChoice>(
                        builder.getContext(), android.R.layout.simple_list_item_1) {
                    private static final int TYPE_MENU_ITEM = 0;
                    private static final int TYPE_MENU_CHECK = 1;
                    private static final int TYPE_SEPARATOR = 2;
                    private static final int TYPE_GROUP = 3;
                    private static final int TYPE_SINGLE = 4;
                    private static final int TYPE_MULTIPLE = 5;
                    private static final int TYPE_COUNT = 6;

                    private LayoutInflater mInflater;
                    private View mSeparator;

                    @Override
                    public int getViewTypeCount() {
                        return TYPE_COUNT;
                    }

                    @Override
                    public int getItemViewType(final int position) {
                        final ModifiableChoice item = getItem(position);
                        if (item.choice.separator) {
                            return TYPE_SEPARATOR;
                        } else if (type == ChoicePrompt.Type.MENU) {
                            return item.modifiableSelected ? TYPE_MENU_CHECK : TYPE_MENU_ITEM;
                        } else if (item.choice.items != null) {
                            return TYPE_GROUP;
                        } else if (type == ChoicePrompt.Type.SINGLE) {
                            return TYPE_SINGLE;
                        } else if (type == ChoicePrompt.Type.MULTIPLE) {
                            return TYPE_MULTIPLE;
                        } else {
                            throw new UnsupportedOperationException();
                        }
                    }

                    @Override
                    public boolean isEnabled(final int position) {
                        final ModifiableChoice item = getItem(position);
                        return !item.choice.separator
                                && !item.choice.disabled
                                && ((type != ChoicePrompt.Type.SINGLE && type != ChoicePrompt.Type.MULTIPLE)
                                || item.choice.items == null);
                    }

                    @Override
                    public View getView(final int position, View view, final ViewGroup parent) {
                        final int itemType = getItemViewType(position);
                        final int layoutId;
                        if (itemType == TYPE_SEPARATOR) {
                            if (mSeparator == null) {
                                mSeparator = new View(getContext());
                                mSeparator.setLayoutParams(
                                        new ListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2, itemType));
                                final TypedArray attr =
                                        getContext().obtainStyledAttributes(new int[] {android.R.attr.listDivider});
                                mSeparator.setBackgroundResource(attr.getResourceId(0, 0));
                                attr.recycle();
                            }
                            return mSeparator;
                        } else if (itemType == TYPE_MENU_ITEM) {
                            layoutId = android.R.layout.simple_list_item_1;
                        } else if (itemType == TYPE_MENU_CHECK) {
                            layoutId = android.R.layout.simple_list_item_checked;
                        } else if (itemType == TYPE_GROUP) {
                            layoutId = android.R.layout.preference_category;
                        } else if (itemType == TYPE_SINGLE) {
                            layoutId = android.R.layout.simple_list_item_single_choice;
                        } else if (itemType == TYPE_MULTIPLE) {
                            layoutId = android.R.layout.simple_list_item_multiple_choice;
                        } else {
                            throw new UnsupportedOperationException();
                        }

                        if (view == null) {
                            if (mInflater == null) {
                                mInflater = LayoutInflater.from(builder.getContext());
                            }
                            view = mInflater.inflate(layoutId, parent, false);
                        }

                        final ModifiableChoice item = getItem(position);
                        final TextView text = (TextView) view;
                        text.setEnabled(!item.choice.disabled);
                        text.setText(item.modifiableLabel);
                        if (view instanceof CheckedTextView) {
                            final boolean selected = item.modifiableSelected;
                            if (itemType == TYPE_MULTIPLE) {
                                list.setItemChecked(position, selected);
                            } else {
                                ((CheckedTextView) view).setChecked(selected);
                            }
                        }
                        return view;
                    }
                };
        addChoiceItems(type, adapter, choices, /* indent */ null);

        list.setAdapter(adapter);
        builder.setView(list);

        final AlertDialog dialog;
        if (type == ChoicePrompt.Type.SINGLE || type == ChoicePrompt.Type.MENU) {
            dialog = createStandardDialog(builder, prompt, res);
            list.setOnItemClickListener(
                    new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(
                                final AdapterView<?> parent, final View v, final int position, final long id) {
                            final ModifiableChoice item = adapter.getItem(position);
                            if (type == ChoicePrompt.Type.MENU) {
                                final ChoicePrompt.Choice[] children = item.choice.items;
                                if (children != null) {
                                    // Show sub-menu.
                                    dialog.setOnDismissListener(null);
                                    dialog.dismiss();
                                    onChoicePromptImpl(
                                            session, item.modifiableLabel, /* msg */ null, type, children, prompt, res);
                                    return;
                                }
                            }
                            res.complete(prompt.confirm(item.choice));
                            dialog.dismiss();
                        }
                    });
        } else if (type == ChoicePrompt.Type.MULTIPLE) {
            list.setOnItemClickListener(
                    new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(
                                final AdapterView<?> parent, final View v, final int position, final long id) {
                            final ModifiableChoice item = adapter.getItem(position);
                            item.modifiableSelected = ((CheckedTextView) v).isChecked();
                        }
                    });
            builder
                    .setNegativeButton(android.R.string.cancel, /* listener */ null)
                    .setPositiveButton(
                            android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(final DialogInterface dialog, final int which) {
                                    final int len = adapter.getCount();
                                    ArrayList<String> items = new ArrayList<>(len);
                                    for (int i = 0; i < len; i++) {
                                        final ModifiableChoice item = adapter.getItem(i);
                                        if (item.modifiableSelected) {
                                            items.add(item.choice.id);
                                        }
                                    }
                                    res.complete(prompt.confirm(items.toArray(new String[items.size()])));
                                }
                            });
            dialog = createStandardDialog(builder, prompt, res);
        } else {
            throw new UnsupportedOperationException();
        }
        dialog.show();

        prompt.setDelegate(
                new PromptInstanceDelegate() {
                    @Override
                    public void onPromptDismiss(final BasePrompt prompt) {
                        dialog.dismiss();
                    }

                    @Override
                    public void onPromptUpdate(final BasePrompt prompt) {
                        dialog.setOnDismissListener(null);
                        dialog.dismiss();
                        final ChoicePrompt newPrompt = (ChoicePrompt) prompt;
                        onChoicePromptImpl(
                                session,
                                newPrompt.title,
                                newPrompt.message,
                                newPrompt.type,
                                newPrompt.choices,
                                newPrompt,
                                res);
                    }
                });
    }

    @Override
    public GeckoResult<PromptResponse> onChoicePrompt(
            final GeckoSession session, final ChoicePrompt prompt) {
        final GeckoResult<PromptResponse> res = new GeckoResult<PromptResponse>();
        onChoicePromptImpl(
                session, prompt.title, prompt.message, prompt.type, prompt.choices, prompt, res);
        return res;
    }

}
