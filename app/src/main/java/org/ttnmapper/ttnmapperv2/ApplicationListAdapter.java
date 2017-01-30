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

public class ApplicationListAdapter extends ArrayAdapter<TTNApplication> {

    Context mContext;
    int layoutResourceId;
    ArrayList<TTNApplication> data = null;

    public ApplicationListAdapter(Context mContext, int layoutResourceId, ArrayList<TTNApplication> data) {
        super(mContext, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.mContext = mContext;
        this.data = data;
    }



    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        TTNApplication currentApplication = data.get(position);

        /*
         * The convertView argument is essentially a "ScrapView" as described is Lucas post
         * http://lucasr.org/2012/04/05/performance-tips-for-androids-listview/
         * It will have a non-null value when ListView is asking you recycle the row layout.
         * So, when convertView is not null, you should simply update its contents instead of inflating a new row layout.
         */
        if(convertView==null){
            // inflate the layout
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            convertView = inflater.inflate(R.layout.list_applications_item, parent, false);
        }

        TextView appId = (TextView)convertView.findViewById(R.id.appID);
        TextView appDevices = (TextView)convertView.findViewById(R.id.appDevices);
        TextView appDescription = (TextView)convertView.findViewById(R.id.appDescription);
        TextView appHandler = (TextView)convertView.findViewById(R.id.appHandler);

        appId.setText(currentApplication.getId());
        String devices = ""+currentApplication.getDevices().size() + " devices";
        appDevices.setText(devices);
        appDescription.setText(currentApplication.getName());
        appHandler.setText(currentApplication.getHandler());

        return convertView;
    }

    public TTNApplication getItem(int i)
    {
        return data.get(i);
    }

}
