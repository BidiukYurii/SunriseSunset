package com.bidyuk.tasks.sunrisesunset;

import android.*;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {

    GoogleApiClient googleApiClient;
    TextView sunset_tv, surise_tv, place_tv;
    private final int PLACE_REQUEST = 1;
    LatLng latLngToParse;
    DownloadTask downloadTask;
    enum Modes{ AFTER_GETTING_PLACE, AFTER_PARSING, TRY_AGAIN };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        downloadTask = new DownloadTask();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(googleApiClient != null)
            googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if(googleApiClient != null && googleApiClient.isConnected())
            googleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == PLACE_REQUEST && resultCode== RESULT_OK)
            displayPlace(PlacePicker.getPlace(data,this));
    }

    private void initView() {
        sunset_tv = (TextView) findViewById(R.id.sunset_tv);
        surise_tv = (TextView) findViewById(R.id.sunrise_tv);
        place_tv = (TextView) findViewById(R.id.got_place_tv);
    }

    public void getPlaceListener(View view) {
        if(googleApiClient != null && !googleApiClient.isConnected())
            return;

        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

        try {
            startActivityForResult(builder.build(this), PLACE_REQUEST);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    private void displayPlace(Place place) {
        if(place == null)
            return;
        StringBuilder stringBuilder = new StringBuilder();

        if(!TextUtils.isEmpty(place.getName()))
            stringBuilder.append(place.getName());

        latLngToParse = place.getLatLng();

        place_tv.setText(stringBuilder);

        setVisbilityViews(Modes.AFTER_GETTING_PLACE);
    }

    public void showSunriseListener(View view) {
        downloadTask.execute("https://api.sunrise-sunset.org/json?lat=" + latLngToParse.latitude
                + "&lng=" + latLngToParse.longitude);
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
    }

    /**
     *Needed to change layouts
     */
    private void setVisbilityViews(Modes mode){
        switch (mode) {
            case AFTER_GETTING_PLACE:
                //after getting place
                findViewById(R.id.launching_layout).setVisibility(View.INVISIBLE);
                findViewById(R.id.result_layout).setVisibility(View.VISIBLE);
                findViewById(R.id.try_again_btn).setVisibility(View.INVISIBLE);
                break;
            case AFTER_PARSING:
                //after parsing json
                findViewById(R.id.before_getting_JSON_layout).setVisibility(View.INVISIBLE);
                findViewById(R.id.linear_layout).setVisibility(View.VISIBLE);
                findViewById(R.id.try_again_btn).setVisibility(View.VISIBLE);
                break;
            case TRY_AGAIN:
                //after try again
                findViewById(R.id.before_getting_JSON_layout).setVisibility(View.VISIBLE);
                findViewById(R.id.linear_layout).setVisibility(View.INVISIBLE);
                findViewById(R.id.launching_layout).setVisibility(View.VISIBLE);
                findViewById(R.id.result_layout).setVisibility(View.INVISIBLE);
                break;
        }
    }


    public void tryAgainListener(View view) {
        setVisbilityViews(Modes.TRY_AGAIN);
        downloadTask = new DownloadTask();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION )
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Please allow ACCESS_FINE_LOCATION persmission.",
                    Toast.LENGTH_LONG).show();
            return;
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    /**
     * DownloadTask is needed for parse
     */
    private class DownloadTask extends AsyncTask<String,Void,String>
    {

        @Override
        protected String doInBackground(String... strings) {

            try {
                URL url = new URL(strings[0]);
                URLConnection urlConnection = url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                int data = inputStreamReader.read();
                String result ="";
                while (data != -1)
                {
                    result += (char) data;
                    data = inputStreamReader.read();
                }
                return result;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result)
        {
            String sunset = "", sunrise = "";
            try {
                JSONObject jsonObject = new JSONObject(result);
                jsonObject = jsonObject.getJSONObject("results");
                sunset = jsonObject.getString("sunset");
                sunrise = jsonObject.getString("sunrise");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            ((TextView)findViewById(R.id.sunrise_tv)).setText("Sunrise\n" + sunrise);
            ((TextView)findViewById(R.id.sunset_tv)).setText("Sunset\n"+sunset);

            setVisbilityViews(Modes.AFTER_PARSING);

            findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);

        }
    }
}
