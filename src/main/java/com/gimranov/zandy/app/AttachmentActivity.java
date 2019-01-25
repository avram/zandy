/*******************************************************************************
 * This file is part of Zandy.
 *
 * Zandy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Zandy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Zandy.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.gimranov.zandy.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import com.gimranov.zandy.app.data.Attachment;
import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.data.Item;
import com.gimranov.zandy.app.storage.StorageManager;
import com.gimranov.zandy.app.task.APIRequest;
import com.gimranov.zandy.app.task.ZoteroAPITask;
import com.gimranov.zandy.app.webdav.WebDavTrust;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This Activity handles displaying and editing attachments. It works almost the same as
 * ItemDataActivity and TagActivity, using a simple ArrayAdapter on Bundles with the creator info.
 * <p>
 * This currently operates by showing the attachments for a given item
 *
 * @author ajlyon
 */
public class AttachmentActivity extends ListActivity {

    private static final String TAG = AttachmentActivity.class.getSimpleName();

    static final int DIALOG_CONFIRM_NAVIGATE = 4;
    static final int DIALOG_FILE_PROGRESS = 6;
    static final int DIALOG_CONFIRM_DELETE = 5;
    static final int DIALOG_NOTE = 3;
    static final int DIALOG_NEW = 1;

    public Item item;
    private ProgressDialog mProgressDialog;
    private ProgressThread progressThread;
    private Database db;

    /**
     * For <= Android 2.1 (API 7), we can't pass bundles to showDialog(), so set this instead
     */
    private Bundle b = new Bundle();

    private ArrayList<File> tmpFiles;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tmpFiles = new ArrayList<File>();

        db = new Database(this);

        /* Get the incoming data from the calling activity */
        final String itemKey = getIntent().getStringExtra("com.gimranov.zandy.app.itemKey");
        Item item = Item.load(itemKey, db);
        this.item = item;

        if (item == null) {
            Log.e(TAG, "AttachmentActivity started without itemKey; finishing.");
            finish();
            return;
        }

        this.setTitle(getResources().getString(R.string.attachments_for_item, item.getTitle()));

        ArrayList<Attachment> rows = Attachment.forItem(item, db);

        /*
         * We use the standard ArrayAdapter, passing in our data as a Attachment.
         * Since it's no longer a simple TextView, we need to override getView, but
         * we can do that anonymously.
         */
        setListAdapter(new ArrayAdapter<Attachment>(this, R.layout.list_attach, rows) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row;

                // We are reusing views, but we need to initialize it if null
                if (null == convertView) {
                    LayoutInflater inflater = getLayoutInflater();
                    row = inflater.inflate(R.layout.list_attach, null);
                } else {
                    row = convertView;
                }

                ImageView tvType = row.findViewById(R.id.attachment_type);
                TextView tvSummary = row.findViewById(R.id.attachment_summary);

                Attachment att = getItem(position);
                Log.d(TAG, "Have an attachment: " + att.title + " fn:" + att.filename + " status:" + att.status);

                tvType.setImageResource(Item.resourceForType(att.getType()));

                try {
                    Log.d(TAG, att.content.toString(4));
                } catch (JSONException e) {
                    Log.e(TAG, "JSON parse exception when reading attachment content", e);
                }

                if (att.getType().equals("note")) {
                    String note = att.content.optString("note", "");
                    if (note.length() > 40) {
                        note = note.substring(0, 40);
                    }
                    tvSummary.setText(note);
                } else {
                    StringBuilder status = new StringBuilder(getResources().getString(R.string.status));
                    if (att.status == Attachment.AVAILABLE)
                        status.append(getResources().getString(R.string.attachment_zfs_available));
                    else if (att.status == Attachment.LOCAL)
                        status.append(getResources().getString(R.string.attachment_zfs_local));
                    else
                        status.append(getResources().getString(R.string.attachment_unknown));
                    tvSummary.setText(att.title + " " + status.toString());
                }
                return row;
            }
        });

        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        lv.setOnItemLongClickListener(new OnItemLongClickListener() {
            // Warning here because Eclipse can't tell whether my ArrayAdapter is
            // being used with the correct parametrization.
            @SuppressWarnings("unchecked")
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // If we have a click on an entry, show its note
                ArrayAdapter<Attachment> adapter = (ArrayAdapter<Attachment>) parent.getAdapter();
                Attachment row = adapter.getItem(position);

                if (row.content.has("note")) {
                    Log.d(TAG, "Trying to start note view activity for: " + row.key);
                    Intent i = new Intent(getBaseContext(), NoteActivity.class);
                    i.putExtra("com.gimranov.zandy.app.attKey", row.key);//row.content.optString("note", ""));
                    startActivity(i);
                }
                return true;
            }
        });
        lv.setOnItemClickListener(new OnItemClickListener() {
            // Warning here because Eclipse can't tell whether my ArrayAdapter is
            // being used with the correct parametrization.
            @SuppressWarnings("unchecked")
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // If we have a long click on an entry, do something...
                ArrayAdapter<Attachment> adapter = (ArrayAdapter<Attachment>) parent.getAdapter();
                Attachment row = adapter.getItem(position);
                String url = (row.url != null && !row.url.equals("")) ?
                        row.url : row.content.optString("url");

                if (!row.getType().equals("note")) {
                    Bundle b = new Bundle();
                    b.putString("title", row.title);
                    b.putString("attachmentKey", row.key);
                    b.putString("content", url);
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

                    if (settings.getBoolean("webdav_enabled", false))
                        b.putString("mode", "webdav");
                    else
                        b.putString("mode", "zfs");

                    if (row.isDownloadable()) {
                        loadFileAttachment(b);
                    } else {
                        AttachmentActivity.this.b = b;
                        showDialog(DIALOG_CONFIRM_NAVIGATE);
                    }
                }

                if (row.getType().equals("note")) {
                    Bundle b = new Bundle();
                    b.putString("attachmentKey", row.key);
                    b.putString("itemKey", itemKey);
                    b.putString("content", row.content.optString("note", ""));
                    removeDialog(DIALOG_NOTE);
                    AttachmentActivity.this.b = b;
                    showDialog(DIALOG_NOTE);
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        if (db != null) db.close();

        if (tmpFiles != null) {
            for (File f : tmpFiles) {
                if (!f.delete()) {
                    Log.e(TAG, "Failed to delete temporary file on activity close.");
                }
            }

            tmpFiles.clear();
        }

        super.onDestroy();
    }

    @Override
    public void onResume() {
        if (db == null) db = new Database(this);
        super.onResume();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final String attachmentKey = b.getString("attachmentKey");
        final String itemKey = b.getString("itemKey");
        final String content = b.getString("content");
        final String mode = b.getString("mode");
        AlertDialog dialog;
        switch (id) {
            case DIALOG_CONFIRM_NAVIGATE:
                dialog = new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.view_online_warning))
                        .setPositiveButton(getResources().getString(R.string.view), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // The behavior for invalid URIs might be nasty, but
                                // we'll cross that bridge if we come to it.
                                try {
                                    Uri uri = Uri.parse(content);
                                    startActivity(new Intent(Intent.ACTION_VIEW)
                                            .setData(uri));
                                } catch (ActivityNotFoundException e) {
                                    // There can be exceptions here; not sure what would prompt us to have
                                    // URIs that the browser can't load, but it apparently happens.
                                    Toast.makeText(getApplicationContext(),
                                            getResources().getString(R.string.attachment_intent_failed_for_uri, content),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).setNeutralButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // do nothing
                            }
                        }).create();
                return dialog;
            case DIALOG_CONFIRM_DELETE:
                dialog = new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.attachment_delete_confirm))
                        .setPositiveButton(getResources().getString(R.string.menu_delete), new DialogInterface.OnClickListener() {
                            @SuppressWarnings("unchecked")
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Attachment a = Attachment.load(attachmentKey, db);
                                a.delete(db);
                                ArrayAdapter<Attachment> la = (ArrayAdapter<Attachment>) getListAdapter();
                                la.clear();
                                for (Attachment at : Attachment.forItem(Item.load(itemKey, db), db)) {
                                    la.add(at);
                                }
                            }
                        }).setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // do nothing
                            }
                        }).create();
                return dialog;
            case DIALOG_NOTE:
                final EditText input = new EditText(this);
                input.setText(content, BufferType.EDITABLE);

                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.note))
                        .setView(input)
                        .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @SuppressWarnings("unchecked")
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Editable value = input.getText();
                                String fixed = value.toString().replaceAll("\n\n", "\n<br>");
                                if (mode != null && mode.equals("new")) {
                                    Log.d(TAG, "Attachment created with parent key: " + itemKey);
                                    Attachment att = new Attachment("note", itemKey);
                                    att.setNoteText(fixed);
                                    att.dirty = APIRequest.API_NEW;
                                    att.save(db);
                                } else {
                                    Attachment att = Attachment.load(attachmentKey, db);
                                    att.setNoteText(fixed);
                                    att.dirty = APIRequest.API_DIRTY;
                                    att.save(db);
                                }
                                ArrayAdapter<Attachment> la = (ArrayAdapter<Attachment>) getListAdapter();
                                la.clear();
                                for (Attachment a : Attachment.forItem(Item.load(itemKey, db), db)) {
                                    la.add(a);
                                }
                                la.notifyDataSetChanged();
                            }
                        }).setNeutralButton(getResources().getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // do nothing
                                    }
                                });
                // We only want the delete option when this isn't a new note
                if (mode == null || !"new".equals(mode)) {
                    builder = builder.setNegativeButton(getResources().getString(R.string.menu_delete), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Bundle b = new Bundle();
                            b.putString("attachmentKey", attachmentKey);
                            b.putString("itemKey", itemKey);
                            removeDialog(DIALOG_CONFIRM_DELETE);
                            AttachmentActivity.this.b = b;
                            showDialog(DIALOG_CONFIRM_DELETE);
                        }
                    });
                }
                dialog = builder.create();

                //noinspection ConstantConditions
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

                return dialog;
            case DIALOG_FILE_PROGRESS:
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.setMessage(getResources().getString(R.string.attachment_downloading, b.getString("title")));
                mProgressDialog.setIndeterminate(true);
                return mProgressDialog;
            default:
                Log.e(TAG, "Invalid dialog requested");
                return null;
        }
    }

    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case DIALOG_FILE_PROGRESS:
                mProgressDialog.setMessage(getResources().getString(R.string.attachment_downloading, b.getString("title")));
                progressThread = new ProgressThread(handler, b);
                progressThread.start();
        }
    }

    private void showAttachment(Attachment att) {
        if (att.status == Attachment.LOCAL) {
            Log.d(TAG, "Starting to display local attachment " + att.title);
            Uri uri = Uri.fromFile(new File(att.filename));
            String contentType = att.getContentType();
            try {
                startActivity(new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, contentType));
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "No activity for intent", e);
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.attachment_intent_failed, contentType),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * This mainly is to move the logic out of the onClick callback above
     * Decides whether to download or view, and launches the appropriate action
     *
     * @param b
     */
    private void loadFileAttachment(Bundle b) {
        Attachment att = Attachment.load(b.getString("attachmentKey"), db);

        File attFile = new File(att.filename);

        if (att.status == Attachment.AVAILABLE || attFile.length() == 0) {
            Log.d(TAG, "Starting to try and download attachment (status: " + att.status + ", fn: " + att.filename + ")");
            this.b = b;
            showDialog(DIALOG_FILE_PROGRESS);
        } else showAttachment(att);
    }

    /**
     * Refreshes the current list adapter
     */
    @SuppressWarnings("unchecked")
    private void refreshView() {
        ArrayAdapter<Attachment> la = (ArrayAdapter<Attachment>) getListAdapter();
        la.clear();
        for (Attachment at : Attachment.forItem(item, db)) {
            la.add(at);
        }
    }

    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.arg2) {
                case ProgressThread.STATE_DONE:
                    if (mProgressDialog.isShowing())
                        dismissDialog(DIALOG_FILE_PROGRESS);
                    refreshView();
                    if (null != msg.obj)
                        showAttachment((Attachment) msg.obj);
                    break;
                case ProgressThread.STATE_FAILED:
                    // Notify that we failed to get anything
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.attachment_no_download_url),
                            Toast.LENGTH_SHORT).show();

                    if (mProgressDialog.isShowing())
                        dismissDialog(DIALOG_FILE_PROGRESS);

                    // Let's try to fall back on an online version
                    AttachmentActivity.this.b = msg.getData();
                    showDialog(DIALOG_CONFIRM_NAVIGATE);

                    refreshView();
                    break;
                case ProgressThread.STATE_UNZIPPING:
                    mProgressDialog.setMax(msg.arg1);
                    mProgressDialog.setProgress(0);
                    mProgressDialog.setMessage(getResources().getString(R.string.attachment_unzipping));
                    break;
                case ProgressThread.STATE_RUNNING:
                    mProgressDialog.setMax(msg.arg1);
                    mProgressDialog.setProgress(0);
                    mProgressDialog.setIndeterminate(false);
                    break;
                default:
                    mProgressDialog.setProgress(msg.arg1);
                    break;

            }
        }
    };

    private class ProgressThread extends Thread {
        Handler mHandler;
        Bundle arguments;
        final static int STATE_DONE = 5;
        final static int STATE_FAILED = 3;
        final static int STATE_RUNNING = 1;
        final static int STATE_UNZIPPING = 6;

        ProgressThread(Handler h, Bundle b) {
            mHandler = h;
            arguments = b;
        }

        @SuppressWarnings("unchecked")
        public void run() {

            // Setup
            final String attachmentKey = arguments.getString("attachmentKey");

            // Can't fetch if we have nothing to fetch
            if (attachmentKey == null) return;

            final String mode = arguments.getString("mode");
            URL url;
            File file;
            String urlstring;
            Attachment att = Attachment.load(attachmentKey, db);

            String sanitized = att.getCanonicalStorageName();

            file = new File(StorageManager.getDocumentsDirectory(AttachmentActivity.this), sanitized);

            final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

            if ("webdav".equals(mode)) {
                urlstring = att.getDownloadUrlWebDav(settings);

                Authenticator.setDefault(new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(settings.getString("webdav_username", ""),
                                settings.getString("webdav_password", "").toCharArray());
                    }
                });

                if (settings.getBoolean("webdav_ssl_override", false)) {
                    WebDavTrust.installAllTrustingCertificate();
                }
            } else {
                urlstring = att.getDownloadUrlZfs(settings);
            }

            try {
                try {
                    url = new URL(urlstring);
                } catch (MalformedURLException e) {
                    // Alert that we don't have a valid download URL and return
                    Message msg = mHandler.obtainMessage();
                    msg.arg2 = STATE_FAILED;
                    msg.setData(arguments);
                    mHandler.sendMessage(msg);

                    Log.e(TAG, "Download URL not valid: " + urlstring, e);
                    return;
                }
                //this is the downloader method
                long startTime = System.currentTimeMillis();
                Log.d(TAG, "download beginning");
                Log.d(TAG, "download url:" + url.toString());
                Log.d(TAG, "downloaded file name:" + file.getPath());

                /* Open a connection to that URL. */
                URLConnection ucon = url.openConnection();
                ucon.setRequestProperty("User-Agent", "Mozilla/5.0 ( compatible ) ");
                ucon.setRequestProperty("Accept", "*/*");
                Message msg = mHandler.obtainMessage();
                msg.arg1 = ucon.getContentLength();
                msg.arg2 = STATE_RUNNING;
                mHandler.sendMessage(msg);

                /*
                 * Define InputStreams to read from the URLConnection.
                 */
                InputStream is = null;
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(file);

                    final AtomicInteger counter = new AtomicInteger();

                    OutputStream outputStream = new CountingOutputStream(fos) {
                        @Override
                        protected void afterWrite(int n) throws IOException {
                            super.afterWrite(n);
                            if (n > 0) {
                                int completed = counter.addAndGet(n);
                                Message message = mHandler.obtainMessage();
                                message.arg1 = completed;
                                mHandler.sendMessage(message);
                            }
                        }
                    };
                    is = ucon.getInputStream();

                    IOUtils.copy(is, outputStream);
                } finally {
                    if (is != null) {
                        is.close();
                    }

                    if (fos != null) {
                        fos.close();
                    }
                }

                /* Save to temporary directory for WebDAV */
                if ("webdav".equals(mode)) {
                    File tmpFile = File.createTempFile("zandy", ".zip", StorageManager.getCacheDirectory(AttachmentActivity.this));

                    FileUtils.copyFile(file, tmpFile);
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();

                    // Keep track of temp files that we've created.
                    if (tmpFiles == null) tmpFiles = new ArrayList<>();
                    tmpFiles.add(tmpFile);

                    ZipFile zf = new ZipFile(tmpFile);

                    Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zf.entries();
                    do {
                        ZipEntry entry = entries.nextElement();
                        // Change the message to reflect that we're unzipping now
                        msg = mHandler.obtainMessage();
                        msg.arg1 = (int) entry.getSize();
                        msg.arg2 = STATE_UNZIPPING;
                        mHandler.sendMessage(msg);

                        String name64 = entry.getName();
                        try {
                            byte[] byteName = Base64.decode(name64.getBytes(), 0, name64.length() - 5, Base64.DEFAULT);
                            String name = new String(byteName);
                            Log.d(TAG, "Found file " + name + " from encoded " + name64);
                            // If the linkMode is not an imported URL (snapshot) and the MIME type isn't text/html,
                            // then we unzip it and we're happy. If either of the preceding is true, we skip the file
                            // unless the filename includes .htm (covering .html automatically)
                            if ((!att.getType().equals("text/html")) || name.contains(".htm")) {
                                FileOutputStream fos2 = new FileOutputStream(file);
                                InputStream entryStream = zf.getInputStream(entry);

                                final AtomicInteger counter = new AtomicInteger();

                                OutputStream outputStream = new CountingOutputStream(fos2) {
                                    @Override
                                    protected void afterWrite(int n) throws IOException {
                                        super.afterWrite(n);
                                        if (n > 0) {
                                            int completed = counter.addAndGet(n);
                                            Message message = mHandler.obtainMessage();
                                            message.arg1 = completed;
                                            mHandler.sendMessage(message);
                                        }
                                    }
                                };

                                IOUtils.copy(entryStream, outputStream);

                                fos2.close();
                                entryStream.close();
                                Log.d(TAG, "Finished reading file");
                            } else {
                                Log.d(TAG, "Skipping file: " + name);
                            }
                        } catch (IllegalArgumentException | NegativeArraySizeException e) {
                            Log.e(TAG, "b64 " + name64, e);
                        }
                    } while (entries.hasMoreElements());

                    zf.close();

                    // We remove the file from the ArrayList if deletion succeeded;
                    // otherwise deletion is put off until the activity exits.
                    if (tmpFile.delete()) {
                        tmpFiles.remove(tmpFile);
                    }
                }

                Log.d(TAG, "download ready in "
                        + ((System.currentTimeMillis() - startTime) / 1000)
                        + " sec");
            } catch (IOException e) {
                Log.e(TAG, "Error: ", e);
                toastError(R.string.attachment_download_failed, e.getMessage());
            }

            att.filename = file.getPath();
            File newFile = new File(att.filename);
            Message msg = mHandler.obtainMessage();
            if (newFile.length() > 0) {
                att.status = Attachment.LOCAL;
                Log.d(TAG, "File downloaded: " + att.filename);
                msg.obj = att;
            } else {
                Log.d(TAG, "File not downloaded: " + att.filename);
                att.status = Attachment.AVAILABLE;
                msg.obj = null;
            }
            att.save(db);
            msg.arg2 = STATE_DONE;
            mHandler.sendMessage(msg);
        }
    }

    private void toastError(final int resource, final String detail) {
        AttachmentActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(AttachmentActivity.this,
                        AttachmentActivity.this.getString(resource)
                                + "\n " + detail,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.zotero_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Bundle b = new Bundle();
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.do_sync:
                if (!ServerCredentials.check(getApplicationContext())) {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.sync_log_in_first),
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                Log.d(TAG, "Preparing sync requests, starting with present item");
                new ZoteroAPITask(getBaseContext()).execute(APIRequest.update(this.item));
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.sync_started),
                        Toast.LENGTH_SHORT).show();

                return true;
            case R.id.do_new:
                b.putString("itemKey", this.item.getKey());
                b.putString("mode", "new");
                AttachmentActivity.this.b = b;
                removeDialog(DIALOG_NOTE);
                showDialog(DIALOG_NOTE);
                return true;
            case R.id.do_prefs:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
