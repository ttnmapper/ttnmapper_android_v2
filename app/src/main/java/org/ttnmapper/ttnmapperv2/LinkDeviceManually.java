package org.ttnmapper.ttnmapperv2;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

public class LinkDeviceManually extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_device_manually);


        MyApplication mApplication = (MyApplication)getApplicationContext();

        EditText applicationID = (EditText) findViewById(R.id.editTextApplicationID);
        EditText deviceID = (EditText) findViewById(R.id.editTextDeviceID);
        EditText accessKey = (EditText) findViewById(R.id.editTextAccessKey);
        EditText broker = (EditText) findViewById(R.id.editTextBroker);

        applicationID.setText(mApplication.getTtnApplicationId());
        deviceID.setText(mApplication.getTtnDeviceId());
        accessKey.setText(mApplication.getTtnAccessKey());
        broker.setText(mApplication.getTtnBroker());

    }

    public void onClickSave(View v) {
        //set values in myApplication
        MyApplication mApplication = (MyApplication)getApplicationContext();

        EditText applicationID = (EditText) findViewById(R.id.editTextApplicationID);
        EditText deviceID = (EditText) findViewById(R.id.editTextDeviceID);
        EditText accessKey = (EditText) findViewById(R.id.editTextAccessKey);
        EditText broker = (EditText) findViewById(R.id.editTextBroker);

        String applicationIDtext = applicationID.getText().toString();
        if (applicationIDtext.contains("+")) {
            Toast.makeText(this, "Your Application ID may not contain a + character.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (applicationIDtext.contains("#")) {
            Toast.makeText(this, "Your Application ID may not contain a # character.", Toast.LENGTH_SHORT).show();
            return;
        }

        String deviceIDtext = applicationID.getText().toString();
        if (!deviceIDtext.equals("+") && deviceIDtext.contains("+")) {
            Toast.makeText(this, "Your Device ID may not contain a + character.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (deviceIDtext.contains("#")) {
            Toast.makeText(this, "Your Device ID may not contain a # character.", Toast.LENGTH_SHORT).show();
            return;
        }

        mApplication.setTtnApplicationId(applicationID.getText().toString());
        mApplication.setTtnDeviceId(deviceID.getText().toString());
        mApplication.setTtnAccessKey(accessKey.getText().toString());
        mApplication.setTtnBroker(broker.getText().toString());

        Answers.getInstance().logCustom(new CustomEvent("Device configure").putCustomAttribute("method", "manually"));

        finish();
    }

    public void onClickCancel(View v) {
        finish();
    }
}
