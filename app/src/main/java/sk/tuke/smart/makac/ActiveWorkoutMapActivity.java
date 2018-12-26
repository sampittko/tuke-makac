package sk.tuke.smart.makac;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;
import sk.tuke.smart.makac.helpers.IntentHelper;
import sk.tuke.smart.makac.helpers.MainHelper;
import sk.tuke.smart.makac.model.GpsPoint;
import sk.tuke.smart.makac.model.config.DatabaseHelper;

public class ActiveWorkoutMapActivity extends FragmentActivity implements OnMapReadyCallback, DatabaseConnection {
    private static final String TAG = "ActiveWorkoutMapA";
    private final float ZOOM_LEVEL = 17.0f;
    int lastRenderBeforeSeconds = 0;

    private ArrayList<List<Location>> finalPositionList = new ArrayList<>();
    private ArrayList<Location> locationList = new ArrayList<>();

    private GoogleMap mMap;

    private Marker currentMarker;

    private LatLng initialLatLng;

    private Dao<GpsPoint, Long> gpsPointDao;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent broadcastIntent) {
            try {
                lastRenderBeforeSeconds++;
                if (broadcastIntent.getAction().equals(IntentHelper.ACTION_TICK)) {
                    Location newestLocation = broadcastIntent.getParcelableExtra(IntentHelper.DATA_LOCATION);
                    updateMapLocation(newestLocation);
                    if (lastRenderBeforeSeconds == 15)
                        performRouteRender();
                }
                else
                    Log.e(TAG, "Broadcast intent does not contain accepted action.");
            }
            catch(NullPointerException e) {
                Log.e(TAG, "Broadcast intent does not have any action inside.");
            }
        }
    };

    private void setupMarkerAndCamera() {
        if (initialLatLng == null) {
            getInitialLatLng();
            if (initialLatLng == null) {
                Log.w(TAG, "Initial latitude and longitude is null.");
                return;
            }
        }
        currentMarker = mMap.addMarker(new MarkerOptions().position(initialLatLng));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(initialLatLng, ZOOM_LEVEL));
        performRouteRender();
        Log.i(TAG, "Marker and camera set up.");
    }

    private void updateMapLocation(Location newestLocation) {
        if (newestLocation != null) {
            Log.i(TAG, "Location changed.");
            handleFinalPositionList(newestLocation);
            if (currentMarker != null) {
                currentMarker.remove();
                Log.i(TAG, "Marker removed");
            }
            LatLng latLng = new LatLng(newestLocation.getLatitude(), newestLocation.getLongitude());
            currentMarker = mMap.addMarker(new MarkerOptions().position(latLng));
            Log.i(TAG, "New marker placed");
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, ZOOM_LEVEL));
            Log.i(TAG, "Camera moved.");
            Log.i(TAG, "Location updated.");
        }
        else {
            Log.i(TAG, "Location not changed.");
        }
    }

    private void handleFinalPositionList(Location newestLocation) {
        if (finalPositionList.size() == 0) {
            Log.w(TAG, "Position list is empty. Creating new list with newest location inside.");
            locationList.add(newestLocation);
            finalPositionList.add(locationList);
        }
        else {
            finalPositionList.get(finalPositionList.size() - 1).add(newestLocation);
            Log.i(TAG, "Newest location inserted into the list.");
        }
    }

    private void performRouteRender() {
        mMap.clear();
        MarkerOptions markerOptions = new MarkerOptions().position(currentMarker.getPosition());
        currentMarker = mMap.addMarker(markerOptions);
        for (List<Location> locationList : finalPositionList) {
            PolylineOptions polylineOptions = new PolylineOptions().clickable(true);
            for (Location location : locationList)
                polylineOptions.add(new LatLng(location.getLatitude(), location.getLongitude()));
            mMap.addPolyline(polylineOptions);
        }
        lastRenderBeforeSeconds = 0;
        Log.i(TAG, "Map rendered.");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_workout_map);
        ButterKnife.bind(this);
        databaseSetup();
        retrieveVariables();
        setupBroadcastReceiver();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_activeworkoutmap_map);
        mapFragment.getMapAsync(this);
    }

    public void databaseSetup() {
        try {
            DatabaseHelper databaseHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
            gpsPointDao = databaseHelper.gpsPointDao();
            Log.i(TAG, "Local database is ready");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void retrieveVariables() {
        try {
            long currentWorkoutId = getIntent().getLongExtra(IntentHelper.DATA_WORKOUT, -1);
            List<GpsPoint> gpsPoints = gpsPointDao.queryForEq("workout_id", currentWorkoutId);
            finalPositionList = MainHelper.getFinalPositionList(gpsPoints);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(IntentHelper.ACTION_TICK);
        registerReceiver(broadcastReceiver, intentFilter);
        Log.i(TAG, "Receiver registered.");
    }

    private void getInitialLatLng() {
        try {
            List<Location> lastLocationList = finalPositionList.get(finalPositionList.size() - 1);
            initialLatLng = new LatLng(lastLocationList.get(lastLocationList.size() - 1).getLatitude(), lastLocationList.get(lastLocationList.size() - 1).getLongitude());
            Log.i(TAG, "Initial LatLng set");
        }
        catch (IndexOutOfBoundsException e) {
            Log.i(TAG, "Cannot set initial LatLng");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        OpenHelperManager.releaseHelper();
        unregisterReceiver(broadcastReceiver);
        Log.i(TAG, "Receiver unregistered.");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (initialLatLng == null) {
            getInitialLatLng();
            if (initialLatLng == null) {
                Log.w(TAG, "Initial latitude and longitude is null.");
                return;
            }
        }
        setupMarkerAndCamera();
        mMap.animateCamera(CameraUpdateFactory.newLatLng(initialLatLng));
    }

    @OnClick(R.id.button_activeworkoutmap_back)
    public void triggerBackAction(View view) {
        finish();
    }
}
