package sk.tuke.smart.makac;

import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import sk.tuke.smart.makac.helpers.IntentHelper;
import sk.tuke.smart.makac.helpers.MainHelper;
import sk.tuke.smart.makac.model.GpsPoint;
import sk.tuke.smart.makac.model.config.DatabaseHelper;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, DatabaseConnection {
    private GoogleMap mMap;

    private Dao<GpsPoint, Long> gpsPointDao;

    private ArrayList<List<Location>> finalPositionList;

    private static final String TAG = "MapsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        databaseSetup();
        retrieveVariables();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_maps_map);
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
            long currentWorkoutId = getIntent().getLongExtra(IntentHelper.DATA_WORKOUT_ID, -1);
            List<GpsPoint> gpsPoints = gpsPointDao.queryForEq("workout_id", currentWorkoutId);
            finalPositionList = MainHelper.getFinalPositionList(gpsPoints);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        OpenHelperManager.releaseHelper();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        renderRoute();
        setCamera();
    }

    private void renderRoute() {
        for (List<Location> locationList : finalPositionList) {
            PolylineOptions polylineOptions = new PolylineOptions().clickable(true);
            for (Location location : locationList)
                polylineOptions.add(new LatLng(location.getLatitude(), location.getLongitude()));
            mMap.addPolyline(polylineOptions);
        }
        Log.i(TAG, "Route was rendered");
    }

    private void setCamera() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        LatLng startPoint = getStartPoint();
        LatLng endPoint = getEndPoint();

        builder.include(startPoint);
        builder.include(endPoint);

        LatLngBounds bounds = builder.build();

        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 100);
        mMap.animateCamera(cu);

        Log.i(TAG, "Camera setup successful");
    }

    private LatLng getStartPoint() {
        return new LatLng(finalPositionList.get(0).get(0).getLatitude(), finalPositionList.get(0).get(0).getLongitude());
    }

    private LatLng getEndPoint() {
        List<Location> lastLocationList = finalPositionList.get(finalPositionList.size() - 1);
        return new LatLng(lastLocationList.get(lastLocationList.size() - 1).getLatitude(), lastLocationList.get(lastLocationList.size() - 1).getLongitude());
    }
}
