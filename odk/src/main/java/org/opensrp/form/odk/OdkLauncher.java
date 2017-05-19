package org.opensrp.form.odk;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import org.odk.collect.android.activities.MainMenuActivity;

// This class is main activity which is replacing
// org.odk.collect.android.activities.SplashScreenActivity i.e. launcher of ODK
// If code is upgraded make sure to setup everything here again which is done in mentioned activity in ODK
// to make sure that nothing is missed out which is required for ODK
public class OdkLauncher extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // setup ODK whenever application is launched. This should cover everything ODK does in its init Activities
        OdkUtils.setupODK(this);

        setContentView(R.layout.odk_launcher);

        // launch new activity and close splash screen
        startActivity(new Intent(this, MainMenuActivity.class));
        finish();
    }
}
