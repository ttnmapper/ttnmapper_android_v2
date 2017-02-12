package org.ttnmapper.ttnmapperv2;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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

        if (mApplication.getTtnApplicationId().equals("")) {
            textViewApplicationID.setText("<not configured>");
        } else {
            textViewApplicationID.setText(mApplication.getTtnApplicationId());
        }

        if (mApplication.getTtnAccessKey().equals(""))
        {
            textViewAccessKey.setText("<not configured>");
        } else {
            textViewAccessKey.setText(mApplication.getTtnAccessKey());
        }

        if (mApplication.getTtnBroker().equals("")) {
            textViewBroker.setText("<not configured>");
        } else {
            textViewBroker.setText(mApplication.getTtnBroker());
        }

        if (mApplication.getTtnDeviceId().equals("")) {
            textViewDeviceID.setText("<not configured>");
        } else if (mApplication.getTtnDeviceId().equals("+")) {
            textViewDeviceID.setText("All devices in application");
        } else {
            textViewDeviceID.setText(mApplication.getTtnDeviceId());
        }
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
