package org.ttnmapper.ttnmapperv2;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

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
