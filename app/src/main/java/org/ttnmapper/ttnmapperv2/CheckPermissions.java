package org.ttnmapper.ttnmapperv2;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class CheckPermissions extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    /**
     * Id to identify a camera permission request.
     */
    public static final String TAG = "TTNMapsPermission";

    private static final int REQUEST_COARSE_PERMISSION = 0;
    private static final int REQUEST_FINE_PERMISSION = 1;
    private static final int REQUEST_FILE_PERMISSION = 2;

    private CoordinatorLayout coordinatorLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_permissions);

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        if (checkAndSetPermissions()) {
            onContinueClicked(null);
        }
    }

    public boolean checkAndSetPermissions() {
        boolean allPermissionsGranted = true;

        SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            allPermissionsGranted = false;
            TextView textView = (TextView) findViewById(R.id.textViewPermissionCoarse);
            textView.setText("No");
            Button button = (Button) findViewById(R.id.buttonPermissionCoarse);
            button.setEnabled(true);
        } else {
            TextView textView = (TextView) findViewById(R.id.textViewPermissionCoarse);
            textView.setText("Yes");
            Button button = (Button) findViewById(R.id.buttonPermissionCoarse);
            button.setEnabled(false);
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            allPermissionsGranted = false;
            TextView textView = (TextView) findViewById(R.id.textViewPermissionFine);
            textView.setText("No");
            Button button = (Button) findViewById(R.id.buttonPermissionFine);
            button.setEnabled(true);
        } else {
            TextView textView = (TextView) findViewById(R.id.textViewPermissionFine);
            textView.setText("Yes");
            Button button = (Button) findViewById(R.id.buttonPermissionFine);
            button.setEnabled(false);
        }

        if (myPrefs.getBoolean("savefile", true) && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            allPermissionsGranted = false;
            TextView textView = (TextView) findViewById(R.id.textViewPermissionFile);
            textView.setText("No");
            Button button = (Button) findViewById(R.id.buttonPermissionFile);
            button.setEnabled(true);
        } else {
            TextView textView = (TextView) findViewById(R.id.textViewPermissionFile);
            textView.setText("Yes");
            Button button = (Button) findViewById(R.id.buttonPermissionFile);
            button.setEnabled(false);
        }

        Button button = (Button) findViewById(R.id.buttonPermissionsContinue);
        button.setEnabled(allPermissionsGranted);

        return allPermissionsGranted;
    }

    public void requestFilePermission(View view) {
        Log.i(TAG, "FILE permission has NOT been granted. Requesting permission.");

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Log.i(TAG, "Displaying file permission rationale to provide additional context.");
            Snackbar.make(coordinatorLayout, R.string.permission_file_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(CheckPermissions.this,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    REQUEST_FILE_PERMISSION);
                        }
                    })
                    .show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_FILE_PERMISSION);
        }
    }


    public void requestCoarsePermission(View view) {
        Log.d(TAG, "Requesting coarse location");
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            Snackbar.make(coordinatorLayout, R.string.permission_location_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(CheckPermissions.this,
                                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                    REQUEST_COARSE_PERMISSION);
                        }
                    })
                    .show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_COARSE_PERMISSION);
        }
    }

    public void requestFinePermission(View view) {
        Log.d(TAG, "Requesting fine location");
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Snackbar.make(coordinatorLayout, R.string.permission_location_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(CheckPermissions.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_FINE_PERMISSION);
                        }
                    })
                    .show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_FINE_PERMISSION);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        checkAndSetPermissions();
    }

    public void onContinueClicked(View view) {
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
        finish();
    }
}
