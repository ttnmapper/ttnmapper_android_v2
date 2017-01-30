package org.ttnmapper.ttnmapperv2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class LinkDevice extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_device);
    }

    @Override
    protected void onResume() {
        super.onResume();

        TextView textViewApplicationID = (TextView) findViewById(R.id.textViewApplicationID);
        TextView textViewDeviceID = (TextView) findViewById(R.id.textViewDeviceID);
        TextView textViewAccessKey = (TextView) findViewById(R.id.textViewAccessKey);
        TextView textViewBroker = (TextView) findViewById(R.id.textViewBroker);

        MyApplication mApplication = (MyApplication)getApplicationContext();

        textViewApplicationID.setText(mApplication.getTtnApplicationId());
        textViewAccessKey.setText(mApplication.getTtnAccessKey());
        textViewBroker.setText(mApplication.getTtnBroker());

        String deviceID = mApplication.getTtnDeviceId();
        if(deviceID.equals("+"))
        {
            deviceID = "All devices in application";
        }
        textViewDeviceID.setText(deviceID);
    }

    public void onClickLogIn(View v) {
        Intent intent = new Intent(this, LogInToTTN.class);
        startActivity(intent);
    }

    public void onClickManualConfigure(View v) {
        Intent intent = new Intent(this, LinkDeviceManually.class);
        startActivity(intent);
    }

    public void onClickBack(View v) {
        finish();
    }
}
