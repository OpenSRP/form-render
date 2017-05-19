package org.opensrp.form.odk;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import org.odk.collect.android.activities.MainMenuActivity;

/**
 * Created by Maimoona on 5/19/2017.
 *
 * This class is main activity which is replacing org.odk.collect.android.activities.SplashScreenActivity
 * i.e. launcher of ODK. If code is upgraded make sure to setup everything here again which is done
 * in mentioned activity in ODK to make sure that nothing is missed out which is required for ODK-collect
*/
public class OdkLauncher extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // setup ODK whenever application is launched. This should cover everything ODK-collect does
        // in its init activities like main SplashScreenActivity, then MainActivity etc
        OdkUtils.setupODK(this);

        // todo https://github.com/OpenSRP/form-render/issues/17
        // download files and save those to odk-collect path and to odk-collect forms db as well
        // the code odk-collect should be used as much as possible to prevent possibility of deviation
        // incase odk-collect is upgraded
        // The org.odk.collect.android.tasks.DiskSyncTask dos it automatically i.e. sync data according
        // to form files existing in odk-collect forms folder. This may reduce work to manage FormDAO on our own
        
        // todo 'View Sent Form' option can be provided to users to help troubleshoot issues
        // though its not needed. Also user app should be able to decide whether to see the option to
        // view to delete forms after sending

        // todo the application should let user app decide how to manage finalized, editable forms

        // todo A blank form filled is identified by path+datetime. there is no oher id associated. the displayName field which
        // user can input. when launching/saving form this field can be utilized to manage editable/finlaized form life cycle

        // todo a way to change settings dynamically is to add settings to a file collect.settings which can be helpful in
        // changing these dynamically from web dashboard. how this file should look like

        // todo onListItemClick in org.odk.collect.android.activities.FormChooserList shows the way odk-collect
        // handle forms. when user clicks item it fetches form URI by form id and opens
        // org.odk.collect.android.activities.FormEntryActivity with extras
        // we need to fetch form by formName or identifier or display and get the form long id from db
        // and pass it to activity with content provider path or show error if none exists

        setContentView(R.layout.odk_launcher);

        final Activity this_ = this;

        ((Button)findViewById(R.id.button2)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    OdkUtils.launchODKForm("Birds", this_);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });

        // launch new activity and close splash screen
        // startActivity(new Intent(this, MainMenuActivity.class));
        // finish();
    }
}
