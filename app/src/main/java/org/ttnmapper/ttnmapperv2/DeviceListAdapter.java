package org.ttnmapper.ttnmapperv2;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by jpmeijers on 30-1-17.
 */

public class DeviceListAdapter extends ArrayAdapter<String> {

    Context mContext;
    int layoutResourceId;
    String[] data = null;

    public DeviceListAdapter(Context mContext, int layoutResourceId, String[] data) {
        super(mContext, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.mContext = mContext;
        this.data = data;
    }



    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        /*
         * The convertView argument is essentially a "ScrapView" as described is Lucas post
         * http://lucasr.org/2012/04/05/performance-tips-for-androids-listview/
         * It will have a non-null value when ListView is asking you recycle the row layout.
         * So, when convertView is not null, you should simply update its contents instead of inflating a new row layout.
         */
        if(convertView==null){
            // inflate the layout
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            convertView = inflater.inflate(R.layout.list_devices_item, parent, false);
        }

        TextView deviceId = (TextView)convertView.findViewById(R.id.deviceID);

        deviceId.setText(data[position]);

        return convertView;
    }

    public String getItem(int i)
    {
        String value = data[i];
        if(value.equals("All devices"))
        {
            value = "+";
        }
        return value;
    }
}
