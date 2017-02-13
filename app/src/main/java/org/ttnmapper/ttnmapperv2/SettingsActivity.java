package org.ttnmapper.ttnmapperv2;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class SettingsActivity extends AppCompatActivity {

    private final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MyApplication mApplication = (MyApplication) getApplicationContext();
        RadioButton rbUpload = (RadioButton) findViewById(R.id.radioButtonUploadGlobal);
        RadioButton rbExperiment = (RadioButton) findViewById(R.id.radioButtonUploadExperiment);
        RadioButton rbNoUpload = (RadioButton) findViewById(R.id.radioButtonNoUpload);
        EditText etExperimentName = (EditText) findViewById(R.id.editTextExperimentName);
        EditText etSaveToFile = (EditText) findViewById(R.id.editTextFilename);
        CheckBox cbSaveToFile = (CheckBox) findViewById(R.id.checkBoxSaveFile);
        TextView versionText = (TextView) findViewById(R.id.textViewVersion);

        // version
        try {
            //set the app instance ID (https://developers.google.com/instance-id/)
            final PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            final String version = pInfo.versionName;
            int verCode = pInfo.versionCode;
            versionText.setText("App version number: " + verCode + "\nBuild date: " + version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // save to file
        etSaveToFile.setText(mApplication.getFileName());
        etSaveToFile.setEnabled(false);
        cbSaveToFile.setChecked(mApplication.isSaveToFile());


        // upload type
        etExperimentName.setText(mApplication.getExperimentName());

        if (mApplication.isExperiment() && mApplication.isShouldUpload()) {
            rbExperiment.setChecked(true);
            etExperimentName.setEnabled(true);
        } else if (mApplication.isShouldUpload()) {
            rbUpload.setChecked(true);
            etExperimentName.setEnabled(false);
        } else {
            rbNoUpload.setChecked(true);
            etExperimentName.setEnabled(false);
        }


        // and listen for changes
        RadioGroup rg = (RadioGroup) findViewById(R.id.radioGroupUpload);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Radio group changed");

                if (checkedId == R.id.radioButtonUploadExperiment) {
                    Log.d(TAG, "experiment checked");
                    EditText etExperimentName = (EditText) findViewById(R.id.editTextExperimentName);
                    etExperimentName.setEnabled(true);

                } else {
                    Log.d(TAG, "not experiment");
                    EditText etExperimentName = (EditText) findViewById(R.id.editTextExperimentName);
                    etExperimentName.setEnabled(false);
                }

            }
        });

        // set text view to say if a device was linked yet or not
        TextView textView = (TextView) findViewById(R.id.textViewLinekdDevice);
        if (mApplication.isConfigured()) {
            textView.setText("Already configured.\nUsing device with ID " + mApplication.getTtnDeviceId());
        } else {
            textView.setText("No device linked yet");
        }

    }

    public void onLinkDeviceClicked(View v) {
        Intent intent = new Intent(this, LinkDevice.class);
        startActivity(intent);
    }

    public void onBackClicked(View v) {
        finish();
    }

    public void onSaveClicked(View v) {
        Log.d(TAG, "Save clicked");

        MyApplication mApplication = (MyApplication) getApplicationContext();
        RadioButton rbUpload = (RadioButton) findViewById(R.id.radioButtonUploadGlobal);
        RadioButton rbExperiment = (RadioButton) findViewById(R.id.radioButtonUploadExperiment);
        EditText etExperimentName = (EditText) findViewById(R.id.editTextExperimentName);

        //first enable upload, then test experiment
        if (rbUpload.isChecked()) {
            Log.d(TAG, "global upload");
            mApplication.setShouldUpload(true);
            mApplication.setExperiment(false);
        } else if (rbExperiment.isChecked()) {
            Log.d(TAG, "experiment upload");
            mApplication.setShouldUpload(true);
            mApplication.setExperiment(true);
            mApplication.setExperimentName(etExperimentName.getText().toString());
        } else {
            Log.d(TAG, "no upload");
            mApplication.setShouldUpload(false);
            mApplication.setExperiment(false);
        }

        //save to file
        CheckBox cbSaveToFile = (CheckBox) findViewById(R.id.checkBoxSaveFile);
        mApplication.setSaveToFile(cbSaveToFile.isChecked());

        finish();
    }
}
