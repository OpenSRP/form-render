package org.opensrp.form.odk;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import org.odk.collect.android.application.Collect;
import org.odk.collect.android.preferences.PreferenceKeys;

import timber.log.Timber;

/**
 * Created by Maimoona on 5/19/2017.
 */

public class OdkUtils {

    // todo override some preferences
    // setup ODK whenever application is launched. This should cover everything ODK does in its init Activities
    public static void setupODK(Context context){
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
        }
    }
}
