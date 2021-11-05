package nl.dvbritsem.weatherapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static com.google.android.gms.location.LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;

public class MainActivity extends AppCompatActivity {

    // Constants
    public static final String TAG = "WeatherApp-Log";

    // Constants for preferences
    public final static String USE_GPS = "use_gps_location";
    public final static boolean USE_GPS_DEFAULT = false;
    public static boolean GPS_PREF;
    public final static String LOCATION_PREF = "list_preference_location";
    public final static String LOCATION_PREF_DEFAULT = "Utrecht";

    // Constants for Location
    public static Location LOCATION;
    public static String LOCATION_CITY;
    public static double LOCATION_LAT;
    public static double LOCATION_LON;
    public static CancellationToken CANCEL_TOKEN;
    public final static String API_KEY = "edbd952116f8c6093d32faba3bb3e237";

    public static String URI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        getLocation();
    }

    // Get location
    public void getLocation() {
        GPS_PREF = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(USE_GPS, USE_GPS_DEFAULT);

        // Use other location
        if (!GPS_PREF) {
            LOCATION_CITY = PreferenceManager.getDefaultSharedPreferences(this).getString(LOCATION_PREF, LOCATION_PREF_DEFAULT);

            // Load the weather info
            getWeather();
        }
        // Use GPS location
        else {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // User granted permission
                fusedLocationClient.getCurrentLocation(PRIORITY_BALANCED_POWER_ACCURACY, CANCEL_TOKEN)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                LOCATION = task.getResult();
                                if (LOCATION != null) {
                                    LOCATION_LAT = LOCATION.getLatitude();
                                    LOCATION_LON = LOCATION.getLongitude();

                                    // Load the weather info
                                    getWeather();
                                } else {
                                    Log.d(TAG, "getLocation: No location");
                                    noLocationHandler();
                                }
                                
                            } else {
                                Exception exception = task.getException();
                                Log.d(TAG, "Error: " + exception);
                                noLocationHandler();
                            }
                        });
            } else {
                // User denied permission
                Log.d(TAG, "Permission denied");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }
        }
    }

    //
    // Get weather info from the internet
    //
    public void getWeather() {
        // Hide no location warning
        TextView locationWarning = (TextView) findViewById(R.id.location_warning);
        locationWarning.setVisibility(View.GONE);

        RequestQueue queue = Volley.newRequestQueue(this);

        // Use GPS location
        if (GPS_PREF && LOCATION_LON != 0 && LOCATION_LAT != 0) {
            URI = "https://api.openweathermap.org/data/2.5/weather?lat=" + LOCATION_LAT + "&lon=" + LOCATION_LON + "&appid=" + API_KEY + "&units=metric";
        }
        else {
            URI = "https://api.openweathermap.org/data/2.5/weather?q=" + LOCATION_CITY + "&appid=" + API_KEY + "&units=metric";
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, URI, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "Succes" + response);

                        // Hide no internet warning
                        TextView internetWarning = (TextView) findViewById(R.id.internet_warning);
                        internetWarning.setVisibility(View.GONE);

                        updateWeather(response);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "Error" + error);
                        noInternetHandler();
                    }
                });

        queue.add(jsonObjectRequest);

        Log.d(TAG, "getWeather: " + URI);
    }

    public void updateWeather(JSONObject data) {
        try {
            // Update temperature
            JSONObject main = (JSONObject) data.get("main");
            double temp = (double) main.get("temp");
            temp = Math.round(temp * 10.0) / 10.0;

            Log.d(TAG, "updateTemp: " + temp);
            TextView Temp = (TextView) findViewById(R.id.Temp);
            Temp.setText(temp + "°C");

            // Update weatherIcon
            JSONArray weather = (JSONArray) data.get("weather");
            JSONObject weatherInfo = (JSONObject) weather.get(0);
            String iconCode = (String) weatherInfo.get("icon");

            ImageView icon = (ImageView) findViewById(R.id.weatherIcon);
            int resourceID = getResources().getIdentifier("_"+iconCode, "drawable", getPackageName());
            icon.setImageResource(resourceID);
            Log.d(TAG, "updateTemp: " + resourceID +"_"+iconCode+".png");

            // Update location
            String saveLocation = "";
            if (GPS_PREF) {
                // Show GPS location on screen
                String cityName = (String) data.get("name");

                TextView CURRENT_LOCATION = (TextView) findViewById(R.id.current_location);
                CURRENT_LOCATION.setText(cityName);
                saveLocation = cityName;

                ImageView locationType = findViewById(R.id.location_type);
                locationType.setImageResource(R.drawable.ic_baseline_gps_fixed_24);
            }
            else {
                // Show location on screen
                TextView CURRENT_LOCATION = (TextView) findViewById(R.id.current_location);
                CURRENT_LOCATION.setText(LOCATION_CITY);
                saveLocation = LOCATION_CITY;

                ImageView locationType = findViewById(R.id.location_type);
                locationType.setImageResource(R.drawable.ic_baseline_location_on_24);
            }

            // Update time
            Date currentTime = Calendar.getInstance().getTime();
            DateFormat currentTimeFormat = new SimpleDateFormat(" dd/MM HH:mm");
            currentTimeFormat.setTimeZone(TimeZone.getDefault());
            String time = currentTimeFormat.format(currentTime);

            TextView last_updated_time = (TextView) findViewById(R.id.last_updated_time);
            String last_updated_time_String = getString(R.string.last_updated_time);
            last_updated_time.setText(last_updated_time_String + time);

            // Save data for later
            SharedPreferences.Editor editor = getSharedPreferences("saved_data", MODE_PRIVATE).edit();
            editor.putString("saved_temp", String.valueOf(temp));
            editor.putString("saved_icon", iconCode);
            editor.putString("saved_location", saveLocation);
            editor.putString("saved_time", time);
            editor.apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onClick(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    public void onRefresh(View view) {
        getLocation();
    }

    public void noInternetHandler() {
        // Show no internet warning
        TextView internetWarning = (TextView) findViewById(R.id.internet_warning);
        internetWarning.setVisibility(View.VISIBLE);

        // Get saved data
        SharedPreferences saved_data = getSharedPreferences("saved_data", MODE_PRIVATE);
        String temp = saved_data.getString("saved_temp", "No internet");
        String iconCode = saved_data.getString("saved_icon", "03d");
        String location = saved_data.getString("saved_location", "No internet");
        String time = saved_data.getString("saved_time", "Never");

        // Update temperature
        TextView Temp = (TextView) findViewById(R.id.Temp);
        Temp.setText(temp + "°C");

        // Update weatherIcon
        ImageView icon = (ImageView) findViewById(R.id.weatherIcon);
        int resourceID = getResources().getIdentifier("_"+iconCode, "drawable", getPackageName());
        icon.setImageResource(resourceID);

        // Update location
        TextView CURRENT_LOCATION = (TextView) findViewById(R.id.current_location);
        CURRENT_LOCATION.setText(location);

        // Update time
        TextView last_updated_time = (TextView) findViewById(R.id.last_updated_time);
        String last_updated_time_String = getString(R.string.last_updated_time);
        last_updated_time.setText(last_updated_time_String + time);
    }

    public void noLocationHandler() {
        // Hide no internet warning
        TextView internetWarning = (TextView) findViewById(R.id.internet_warning);
        internetWarning.setVisibility(View.GONE);

        // Show no location warning
        TextView locationWarning = (TextView) findViewById(R.id.location_warning);
        locationWarning.setVisibility(View.VISIBLE);

        // Get saved data
        SharedPreferences saved_data = getSharedPreferences("saved_data", MODE_PRIVATE);
        String temp = saved_data.getString("saved_temp", "No location");
        String iconCode = saved_data.getString("saved_icon", "03d");
        String location = saved_data.getString("saved_location", "No location");
        String time = saved_data.getString("saved_time", "Never");

        // Update temperature
        TextView Temp = (TextView) findViewById(R.id.Temp);
        Temp.setText(temp + "°C");

        // Update weatherIcon
        ImageView icon = (ImageView) findViewById(R.id.weatherIcon);
        int resourceID = getResources().getIdentifier("_"+iconCode, "drawable", getPackageName());
        icon.setImageResource(resourceID);

        // Update location
        TextView CURRENT_LOCATION = (TextView) findViewById(R.id.current_location);
        CURRENT_LOCATION.setText(location);

        // Update time
        TextView last_updated_time = (TextView) findViewById(R.id.last_updated_time);
        String last_updated_time_String = getString(R.string.last_updated_time);
        last_updated_time.setText(last_updated_time_String + time);
    }
}

