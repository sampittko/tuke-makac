package sk.tuke.smart.makac;

import android.content.Intent;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ZoomControls;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import sk.tuke.smart.makac.helpers.IntentHelper;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;

    private ArrayList<List<Location>> finalPositionList;

    private static final String TAG = "MapsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Intent intent = getIntent();
        parseIntent(intent);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_maps_workoutmap);
        mapFragment.getMapAsync(this);
    }

    private void parseIntent(Intent intent) {
        Bundle bundle = intent.getBundleExtra(IntentHelper.DATA_BUNDLE);
        finalPositionList = (ArrayList<List<Location>>) bundle.getSerializable(IntentHelper.DATA_POSITIONS);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        for (List<Location> locationList : finalPositionList) {
            PolylineOptions polylineOptions = new PolylineOptions().clickable(true);
            for (Location location : locationList)
                polylineOptions.add(new LatLng(location.getLatitude(), location.getLongitude()));
            mMap.addPolyline(polylineOptions);
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        LatLng startPoint = new LatLng(finalPositionList.get(0).get(0).getLatitude(), finalPositionList.get(0).get(0).getLongitude());

        List<Location> lastLocationList = finalPositionList.get(finalPositionList.size() - 1);
        LatLng endPoint = new LatLng(lastLocationList.get(lastLocationList.size() - 1).getLatitude(), lastLocationList.get(lastLocationList.size() - 1).getLongitude());

        builder.include(startPoint);
        builder.include(endPoint);

        LatLngBounds bounds = builder.build();

        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 100);
        googleMap.animateCamera(cu);
    }
}
