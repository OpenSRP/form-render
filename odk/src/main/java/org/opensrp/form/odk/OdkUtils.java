package org.opensrp.form.odk;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;

import org.odk.collect.android.application.Collect;
import org.odk.collect.android.dao.FormsDao;
import org.odk.collect.android.listeners.DiskSyncListener;
import org.odk.collect.android.preferences.AdminPreferencesActivity;
import org.odk.collect.android.preferences.PreferenceKeys;
import org.odk.collect.android.provider.FormsProviderAPI;
import org.odk.collect.android.tasks.DiskSyncTask;
import org.odk.collect.android.utilities.ApplicationConstants;
import org.odk.collect.android.utilities.ToastUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by Maimoona on 5/19/2017.
 */
public class OdkUtils {

    // todo override some preferences
    // setup ODK whenever application is launched. This should cover everything ODK does in its init Activities
    public static void setupODK(Activity context){
        // must be at the beginning of any activity that can be called from an external intent
        Collect.createODKDirs();

        // get the shared preferences object
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Collect.getInstance());
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // get the package info object with version number
        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            Timber.e(e, "Unable to get package info");
        }

        // if you've increased version code, then update the version number and set firstRun to true
        if (sharedPreferences.getLong(PreferenceKeys.KEY_LAST_VERSION, 0) < packageInfo.versionCode) {
            editor.putLong(PreferenceKeys.KEY_LAST_VERSION, packageInfo.versionCode);
            editor.apply();
        }

        // do all the first run things
        boolean firstRun = sharedPreferences.getBoolean(PreferenceKeys.KEY_FIRST_RUN, true);
        if (firstRun) {
            editor.putBoolean(PreferenceKeys.KEY_FIRST_RUN, false);
            editor.commit();

            // don`t allow deleting forms (submitted/downloaded/blank)
            boolean delete_saved = sharedPreferences.getBoolean("delete_saved", true);
            if (delete_saved) {
                editor.putBoolean("delete_saved", false);
                editor.commit();
            }
        }

        // as specified in MainActivity of ODK-collect the settings should be updatable via collect.settings
        // to override settings again, this file should be resent and pasted.
        // This code after loading and updating settings deletes this file.
        File f = new File(Collect.ODK_ROOT + "/collect.settings");
        if (f.exists()) {
            boolean success = loadSharedPreferencesFromFile(f);
            if (success) {
                ToastUtils.showLongToast(org.odk.collect.android.R.string.settings_successfully_loaded_file_notification);
                f.delete();
            } else {
                ToastUtils.showLongToast(org.odk.collect.android.R.string.corrupt_settings_file_notification);
            }
        }

        // This flag must be set each time the app starts up
        setupGoogleAnalytics();

        // must run DiskFormLoader
        DiskSyncTask mDiskSyncTask = (DiskSyncTask) context.getLastNonConfigurationInstance();
        if (mDiskSyncTask == null) {
            Timber.i("Starting new disk sync task");
            mDiskSyncTask = new DiskSyncTask();

            // todo see where and how it update app about sync; Also every time forms are updated it should run itself;
            // or if app runs first time
            mDiskSyncTask.setDiskSyncListener(new DiskSyncListener() {
                @Override
                public void syncComplete(String result) {
                    Log.v(getClass().getName(), "DiskSync COMPLETED "+result);
                }
            });
            mDiskSyncTask.execute((Void[]) null);
        }
    }

    // onListItemClick of org.odk.collect.android.activities.FormChooserList explains how form is loaded
    public static void launchODKForm(long id, Activity context){
        Uri formUri = ContentUris.withAppendedId(FormsProviderAPI.FormsColumns.CONTENT_URI, id);

        Log.v(OdkUtils.class.getName(), "Launching URI "+formUri);

        Intent intent = new Intent(Intent.ACTION_EDIT, formUri);
        intent.putExtra(ApplicationConstants.BundleKeys.FORM_MODE, ApplicationConstants.FormModes.EDIT_SAVED);
        context.startActivity(intent);
    }

    public static void launchODKForm(String formId, Activity context) throws IllegalAccessException {
        Cursor formcursor = new FormsDao().getFormsCursorForFormId(formId);
        try {
            if (formcursor.getCount() > 0) {
                formcursor.moveToFirst();

                launchODKForm(formcursor.getLong(formcursor.getColumnIndex(FormsProviderAPI.FormsColumns._ID)), context);
            }
            else throw new IllegalAccessException("Form with given id ("+formId+") not exists. Make sure application life cycle is managed properly");
        }
        finally {
            formcursor.close();
        }
    }

    private static boolean loadSharedPreferencesFromFile(File src) {
        // this should probably be in a thread if it ever gets big
        boolean res = false;
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(new FileInputStream(src));
            SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(Collect.getInstance()).edit();
            prefEdit.clear();
            // first object is preferences
            Map<String, ?> entries = (Map<String, ?>) input.readObject();
            for (Map.Entry<String, ?> entry : entries.entrySet()) {
                Object v = entry.getValue();
                String key = entry.getKey();

                if (v instanceof Boolean) {
                    prefEdit.putBoolean(key, ((Boolean) v).booleanValue());
                } else if (v instanceof Float) {
                    prefEdit.putFloat(key, ((Float) v).floatValue());
                } else if (v instanceof Integer) {
                    prefEdit.putInt(key, ((Integer) v).intValue());
                } else if (v instanceof Long) {
                    prefEdit.putLong(key, ((Long) v).longValue());
                } else if (v instanceof String) {
                    prefEdit.putString(key, ((String) v));
                }
            }
            prefEdit.apply();

            // second object is admin options
            SharedPreferences.Editor adminEdit = Collect.getInstance().getSharedPreferences(AdminPreferencesActivity.ADMIN_PREFERENCES,
                    0).edit();
            adminEdit.clear();
            // first object is preferences
            Map<String, ?> adminEntries = (Map<String, ?>) input.readObject();
            for (Map.Entry<String, ?> entry : adminEntries.entrySet()) {
                Object v = entry.getValue();
                String key = entry.getKey();

                if (v instanceof Boolean) {
                    adminEdit.putBoolean(key, ((Boolean) v).booleanValue());
                } else if (v instanceof Float) {
                    adminEdit.putFloat(key, ((Float) v).floatValue());
                } else if (v instanceof Integer) {
                    adminEdit.putInt(key, ((Integer) v).intValue());
                } else if (v instanceof Long) {
                    adminEdit.putLong(key, ((Long) v).longValue());
                } else if (v instanceof String) {
                    adminEdit.putString(key, ((String) v));
                }
            }
            adminEdit.apply();

            res = true;
        } catch (IOException | ClassNotFoundException e) {
            Timber.e(e, "Exception while loading preferences from file due to : %s ", e.getMessage());
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ex) {
                Timber.e(ex, "Exception thrown while closing an input stream due to: %s ", ex.getMessage());
            }
        }
        return res;
    }

    // This flag must be set each time the app starts up
    private static void setupGoogleAnalytics() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Collect
                .getInstance());
        boolean isAnalyticsEnabled = settings.getBoolean(PreferenceKeys.KEY_ANALYTICS, true);
        GoogleAnalytics googleAnalytics = GoogleAnalytics.getInstance(Collect.getInstance());
        googleAnalytics.setAppOptOut(!isAnalyticsEnabled);
    }
}
