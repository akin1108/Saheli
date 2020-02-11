package com.example.geofencing_1;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GeoQueryEventListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener{

    private GoogleMap mMap;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Marker currentUser;
    double latitude;
    double longitude;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    private DatabaseReference myLocationRef;
    private GeoFire geoFire;
    private int PROXIMITY_RADIUS = 10000;
    private List<LatLng> danger_zone;
    GoogleApiClient mGoogleApiClient;
    Button logout;
    String phn;

    int count = 0;

    FirebaseAuth firebaseAuth;
    DatabaseReference dbrefer;

    private static final String CHANNEL_ID = "Akin_coding";
    private static final String CHANNEL_NAME = "Akin coding";
    private static final String CHANNEL_DESC = " ANDROID LVL 26+";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        //DB
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser hUser = firebaseAuth.getCurrentUser();
        final String uId = hUser.getUid();
        dbrefer = FirebaseDatabase.getInstance().getReference().child("User").child(uId);
        dbrefer.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String contact = dataSnapshot.child("eContact").getValue().toString();
              phn = contact;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        logout = (Button)findViewById(R.id.signout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseAuth.signOut();
                startActivity(new Intent(getApplicationContext(),Login.class));
            }
        });

        //Check if Google Play Services Available or not
        if (!CheckGooglePlayServices()) {
            Log.d("onCreate", "Finishing test case since Google Play Services are not available");
            finish();
        }
        else {
            Log.d("onCreate","Google Play Services available.");
        }


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CHANNEL_DESC);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }


        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {

                        buildLocationRequest();
                        buildLocationCallBack();
                        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapsActivity.this);
                        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                                .findFragmentById(R.id.map);
                        mapFragment.getMapAsync(MapsActivity.this);

                        initArea();
                        settingGeofire();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MapsActivity.this, "Location Permission must be ENABLED", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();

    }

    private boolean CheckGooglePlayServices() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if(result != ConnectionResult.SUCCESS) {
            if(googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result,
                        0).show();
            }
            return false;
        }
        return true;
    }

    private void initArea() {
        danger_zone = new ArrayList<>();
        danger_zone.add(new LatLng(28.546317,77.333785));
        danger_zone.add(new LatLng(28.544394,77.334198));
        danger_zone.add(new LatLng(28.544312,77.332741));
        danger_zone.add(new LatLng(28.544578,77.331607));

    }

    private void settingGeofire() {
        myLocationRef = FirebaseDatabase.getInstance().getReference("MyLocation");
        geoFire = new GeoFire(myLocationRef);
    }

    private void buildLocationCallBack() {
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(final LocationResult locationResult) {

                    geoFire.setLocation("You", new GeoLocation(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude()),
                            new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                    if (mMap != null) {
                                        if (currentUser != null) currentUser.remove();
                                        currentUser = mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude()))
                                                .title("You"));
                                        MarkerOptions markerOption = new MarkerOptions();
                                     markerOption.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentUser.getPosition(), 12.0f));

                                    }
                                }
                    });
                }

        };
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setSmallestDisplacement(10f);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        // --------------- Blue dot ------------------//
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        }
        else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

        // -------------------------------------//

        mMap.getUiSettings().setZoomControlsEnabled(true);

        if(fusedLocationProviderClient != null)
            fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper());

        //Add circle for geofence
        for(LatLng latLng:danger_zone){
            mMap.addCircle(new CircleOptions().center(latLng)
            .radius(50)
            .strokeColor(Color.RED)
            .fillColor(0x22c93030)
            .strokeWidth(5.0f));

            //create geoquerry
            GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(latLng.latitude,latLng.longitude),0.05f);
            geoQuery.addGeoQueryEventListener(MapsActivity.this);
        }

        Button btnHospital = (Button) findViewById(R.id.btnHospital);
        btnHospital.setOnClickListener(new View.OnClickListener() {
            String Hospital = "hospital";
            @Override
            public void onClick(View v) {
                Log.d("onClick", "Button is Clicked");
//                mMap.clear();
                String url = getUrl(latitude, longitude, Hospital);
                Object[] DataTransfer = new Object[2];
                DataTransfer[0] = mMap;
                DataTransfer[1] = url;
                Log.d("onClick", url);
                GetNearbyPlacesData getNearbyPlacesData = new GetNearbyPlacesData();
                getNearbyPlacesData.execute(DataTransfer);
                Toast.makeText(MapsActivity.this,"Nearby Hospitals", Toast.LENGTH_LONG).show();
            }
        });

        Button btnpolice = (Button) findViewById(R.id.btnPolice);
        btnpolice.setOnClickListener(new View.OnClickListener() {
            String Police = "police";
            @Override
            public void onClick(View v) {
                Log.d("onClick", "Button is Clicked");
                mMap.clear();
                if (mCurrLocationMarker != null) {
                    mCurrLocationMarker.remove();
                }
                String url = getUrl(latitude, longitude, Police);
                Object[] DataTransfer = new Object[2];
                DataTransfer[0] = mMap;
                DataTransfer[1] = url;
                Log.d("onClick", url);
                GetNearbyPlacesData getNearbyPlacesData = new GetNearbyPlacesData();
                getNearbyPlacesData.execute(DataTransfer);
                Toast.makeText(MapsActivity.this,"Nearby Thana", Toast.LENGTH_LONG).show();
            }
        });
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }
    @Override
    protected void onStop(){
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onStop();
    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        sendNotification("Akin",String.format("%s Entered Dangerous area ",key));
    }

    private void sendNotification(String title, String content) {
        Intent intent = new Intent(this,MapsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,0);

        Intent phoneCall = new Intent(Intent.ACTION_CALL);
        phoneCall.setData(Uri.parse("tel:9110113248"));
        PendingIntent phoneCallIntent = PendingIntent.getActivity(this, 0, phoneCall, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this,CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_warning_red_24dp)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_call_green_24dp,"CALL POLICE",phoneCallIntent);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(1,mBuilder.build());
    }

    @Override
    public void onKeyExited(String key) {
        sendNotification("Akin",String.format("%s Left Dangerous area ",key));
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
      //  sendNotification("Akin",String.format("%s In Dangerous area ",key));
    }

    @Override
    public void onGeoQueryReady() {

    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        Toast.makeText(this, ""+error.getMessage(), Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
        }
    }

    private String getUrl(double latitude, double longitude, String nearbyPlace) {

        StringBuilder googlePlacesUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location=" + latitude + "," + longitude);
        googlePlacesUrl.append("&radius=" + PROXIMITY_RADIUS);
        googlePlacesUrl.append("&type=" + nearbyPlace);
        googlePlacesUrl.append("&sensor=false");
        googlePlacesUrl.append("&key=" + "AIzaSyATuUiZUkEc_UgHuqsBJa1oqaODI-3mLs0");
        Log.d("getUrl", googlePlacesUrl.toString());
        return (googlePlacesUrl.toString());
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("onLocationChanged", "entered");

        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        //Place current location marker
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        mCurrLocationMarker = mMap.addMarker(markerOptions);

        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
        Toast.makeText(MapsActivity.this,"Your Current Location", Toast.LENGTH_LONG).show();

        Log.d("onLocationChanged", String.format("latitude:%.3f longitude:%.3f",latitude,longitude));

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            Log.d("onLocationChanged", "Removing Location Updates");
        }
        Log.d("onLocationChanged", "Exit");

    }


    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action, keycode;
//        String fullAddress = currentLoc;
//        String mobile = phn;
        action = event.getAction();
        keycode = event.getKeyCode();


        switch(keycode){
            case KeyEvent.KEYCODE_VOLUME_UP:
            {
                if(KeyEvent.ACTION_UP==action){
                    count++;
                    if(count>0){
                        final MediaPlayer siren = MediaPlayer.create(this,R.raw.policebuzzer);
                        siren.start();
                    }

                }
                break;
            }

            case KeyEvent.KEYCODE_VOLUME_DOWN:
            {
                if(KeyEvent.ACTION_DOWN==action){

//                    count++;
//                    if (count > 0) {
//
//                        try{
//                            SmsManager sms=SmsManager.getDefault();
//                            sms.sendTextMessage(mobile, null, "HELP !\n"+fullAddress, null, null);
//                            Toast.makeText(MapsActivity.this, "SMS Sent Successfully", Toast.LENGTH_SHORT).show();
//                        }
//                        catch (Exception e){
//                            Toast.makeText(MapsActivity.this, "SMS Failed to Send, Please try again", Toast.LENGTH_SHORT).show();
//                        }
//
//                    }
                    Toast.makeText(this, "Press volume Up", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
