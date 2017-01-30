package org.ttnmapper.ttnmapperv2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class ApplicationList extends AppCompatActivity implements AdapterView.OnItemClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_application_list);

        MyApplication mApplication = (MyApplication)getApplicationContext();

        ListView listView = (ListView)findViewById(R.id.listViewApplications);
        final ApplicationListAdapter adapter = new ApplicationListAdapter(this, 0, mApplication.ttnApplications);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        TTNApplication selectedApp = (TTNApplication) adapterView.getAdapter().getItem(i);
        MyApplication mApplication = (MyApplication)getApplicationContext();

        mApplication.setTtnApplicationId(selectedApp.getId());
        mApplication.setTtnBroker(selectedApp.getMqttAddress());
        mApplication.setTtnAccessKey(selectedApp.getAccessKey());
        mApplication.chosenTtnApplication = selectedApp;

        Intent intent = new Intent(this, DevicesList.class);
        startActivity(intent);

        finish();
    }
}
