package org.ttnmapper.ttnmapperv2;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

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

        mApplication.setTtnApplicationId(applicationID.getText().toString());
        mApplication.setTtnDeviceId(deviceID.getText().toString());
        mApplication.setTtnAccessKey(accessKey.getText().toString());
        mApplication.setTtnBroker(broker.getText().toString());

        finish();
    }

    public void onClickCancel(View v) {
        finish();
    }
}
