package org.ttnmapper.ttnmapperv2;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

public class DevicesList extends AppCompatActivity implements AdapterView.OnItemClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices_list);

        MyApplication mApplication = (MyApplication)getApplicationContext();
        ArrayList<String> devices = mApplication.chosenTtnApplication.getDevices();
        String[] devicesCopy = new String[devices.size()+1];
        for(int i=0; i<devices.size(); i++)
        {
            devicesCopy[i] = devices.get(i);
        }
        devicesCopy[devicesCopy.length-1] = "All devices";

        ListView listView = (ListView)findViewById(R.id.listViewDevices);
        final DeviceListAdapter adapter = new DeviceListAdapter(this, 0, devicesCopy);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String selectedDevice = (String) adapterView.getAdapter().getItem(i);
        MyApplication mApplication = (MyApplication)getApplicationContext();

        mApplication.setTtnDeviceId(selectedDevice);

        finish();
    }
}
