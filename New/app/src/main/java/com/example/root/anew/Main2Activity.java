package com.example.root.anew;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Arrays;

public class Main2Activity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    static long UPDATE_INTERVAL_IN_MILLISECONDS = 12000;
    static long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";
    protected LocationRequest mLocationRequest;
    protected Location mCurrentLocation;
    protected Button startTrek;
    protected Button backTrek;
    protected Button stopTrek;
    protected GoogleApiClient mGoogleApiClient;
    protected TextView mLatitudeTextView;
    protected TextView mLongitudeTextView;
    protected Boolean mRequestingLocationUpdates;
    protected String mLastUpdateTime;
    protected ListView lv;
    private final static int REQUEST_LOCATION_FOR_THIS_APP = 500;

    public SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        startTrek = (Button) findViewById(R.id.button3);
        backTrek = (Button) findViewById(R.id.back_trek);
        stopTrek = (Button) findViewById(R.id.start_trek);

        final Animation translate = AnimationUtils.loadAnimation(this,R.anim.anim_translate);
        final Animation rotate = AnimationUtils.loadAnimation(this,R.anim.anim_rotate);
        final Animation translate2 = AnimationUtils.loadAnimation(this,R.anim.anim_translate2);
        final Animation scale= AnimationUtils.loadAnimation(this,R.anim.anim_scale);
        startTrek.startAnimation(translate2);
        backTrek.startAnimation(scale);
        stopTrek.startAnimation(translate);

        db = openOrCreateDatabase("Coordinate", MODE_PRIVATE, null);
        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";

        setValues();
        buildGoogleApiClient();
        updateValuesFromBundle(savedInstanceState);

        try
        {
            db.execSQL("create table Coordinate(latitude varchar(100), longitude varchar(100))");
            Toast.makeText(this, "Database Created", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e)
        {

        }

        backTrek.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Main2Activity.this, MapsActivity.class);
                startActivity(intent);
            }
        });

        backTrek.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if ( v == backTrek)
                {
                    if (event.getAction() == MotionEvent.ACTION_DOWN)
                    {
                        v.setAlpha(0.1f);
                    }
                    else
                    {
                        v.setAlpha(0.9f);
                    }
                }
                return false;
            }
        });
    }

    public void setValues()
    {
        CharSequence INTERVAL[] = new CharSequence[]{"3 min", "5 min", "10 min", "Default"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Update Interval");
        builder.setCancelable(false);
        builder.setItems(INTERVAL, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                    switch (which)
                    {
                        case 0 : UPDATE_INTERVAL_IN_MILLISECONDS = 3 * 60 * 1000;
                                FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
                            break;
                        case 1 :  UPDATE_INTERVAL_IN_MILLISECONDS = 5 * 60 * 1000;
                                  FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
                            break;
                        case 2 : UPDATE_INTERVAL_IN_MILLISECONDS = 10 * 60 * 1000;
                                FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
                            break;
                        case 3 :
                            break;
                    }

            }
        });
        builder.show();
     }

    private void updateValuesFromBundle(Bundle savedInstanceState) {

        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
                setButtonsEnabledState();
            }

            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }
        }

    }

    private void setButtonsEnabledState() {

        if (mRequestingLocationUpdates) {
            startTrek.setEnabled(false);
            startTrek.getBackground().setAlpha(128);
            stopTrek.setEnabled(true);
            stopTrek.getBackground().setAlpha(255);
        } else {
            startTrek.setEnabled(true);
            stopTrek.setEnabled(false);
            stopTrek.getBackground().setAlpha(128);
            startTrek.getBackground().setAlpha(255);
        }

    }

    protected synchronized void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();

    }

    protected void createLocationRequest() {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }


    public void startUpdatesButtonHandler(View view) {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
            setButtonsEnabledState();
            startLocationUpdates();
        }
        Toast.makeText(this, "Interval : "+UPDATE_INTERVAL_IN_MILLISECONDS+"", Toast.LENGTH_LONG).show();

    }

    private void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_LOCATION_FOR_THIS_APP);

            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);

    }

    public void stopUpdatesButtonHandler(View view) {
        if (mRequestingLocationUpdates) {
            mRequestingLocationUpdates = false;
            setButtonsEnabledState();
            stopLocationUpdates();
        }
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (mCurrentLocation == null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_LOCATION_FOR_THIS_APP);
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

        mGoogleApiClient.connect();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location)
    {
        mCurrentLocation = location;
        String lat = String.valueOf(mCurrentLocation.getLatitude());
        String lon = String.valueOf(mCurrentLocation.getLongitude());
        try
        {
            db.execSQL("insert into Coordinate values('"+lat+"','"+lon+"')");
        }
        catch (Exception e)
        {

        }
        Toast.makeText(this,"Location Marked",Toast.LENGTH_SHORT).show();
    }

    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }
}