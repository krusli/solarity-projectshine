package krusli.solarity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import krusli.solarity.databinding.ActivityResultsBinding;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class Results extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    ActivityResultsBinding binding;
    private GoogleApiClient mGoogleApiClient;

    LocationRequest mLocationRequest;
    com.google.android.gms.location.LocationListener mLocationListener;
    Location mLastLocation;

    Float lightIntensity;

    public static final double PANEL_EFFICIENCY = 0.2;

    double calculatekWh(List<Float> radiationList, int panelSize) {
        double sum = 0;
        for (int i=0; i<radiationList.size(); i++) {
            sum += panelSize * radiationList.get(i) * PANEL_EFFICIENCY * 1;
        }
        return sum/1000;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_results);

        /* handle intent */
        Intent intent = getIntent();
        lightIntensity = intent.getFloatExtra("LIGHT_VALUE", -1);    // -1 is not possible normally
        Log.d("avg @ Results.java", String.format("%f", lightIntensity));

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

//        // set chart height
//        android.view.ViewGroup.LayoutParams params = binding.chart.getLayoutParams();
//        DisplayMetrics dm = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(dm);
//        params.height = (int) (dm.heightPixels * 0.3);
//        binding.chart.setLayoutParams(params);

    }


    @Override
    protected void onStart() {
        Log.d("onStart", "Connecting to Google Play Services");
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("onConnected", "Connected to Google Play Services");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
        startLocationUpdates();
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation == null) {
            startLocationUpdates();
        }
        if (mLastLocation != null) {
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();

            /* make a request to our backend */
            Retrofit retrofit = new Retrofit.Builder()
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl("http://krusli.me:5000/")
                    .build();

            // get current month
            Date date = new Date();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            int month = calendar.get(Calendar.MONTH);

            PredictionsService predictionsService = retrofit.create(PredictionsService.class);
            Observable<ApiResponse> apiResponseObservable = predictionsService.getPredictionsData(latitude, longitude, month, lightIntensity );

            apiResponseObservable.subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<ApiResponse>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onNext(ApiResponse apiResponse) {
                            Log.d("API response", apiResponse.getRadiationByHour().toString());
                            List<Float> radiationByHour = apiResponse.getRadiationByHour();
                            LineChart chart = binding.chart;

                            // customise chart display
                            chart.setNoDataText("Loading...");  // TODO: does not work
//                            chart.setBackgroundColor(getResources().getColor(R.color.colorPrimary));

                            // styling
                            Description description = new Description(); description.setText("");
                            chart.setDescription(description);
                            chart.setDrawGridBackground(false);

//                            XAxis xAxis = chart.getXAxis();
//                            xAxis.setDrawGridLines(false);
//                            xAxis.setDrawAxisLine(false);
//
//                            YAxis leftAxis = chart.getAxisLeft();
//                            leftAxis.setDrawAxisLine(false);
//                            YAxis rightAxis = chart.getAxisRight();
//                            rightAxis.setDrawAxisLine(false);

//                            rightAxis.setGranularityEnabled(false);

                            List<Entry> entries = new ArrayList<>();

                            // load to entries
                            for (int i=0; i<radiationByHour.size(); i++) {
                                int hour = i + 1; // index 0 = 0100, index 1 = 0200 and so on
                                entries.add(new Entry(hour, radiationByHour.get(i)));
                            }

                            LineDataSet dataSet = new LineDataSet(entries, "Power in W/m2");
                            dataSet.setColor(ContextCompat.getColor(getBaseContext(), R.color.colorPrimaryDark));
                            dataSet.setCircleColor(ContextCompat.getColor(getBaseContext(), R.color.colorPrimary));
//                            dataSet.setValueTextColor(R.color.colorPrimaryText);
                            LineData lineData = new LineData(dataSet);
                            chart.setData(lineData);

                            Legend l = chart.getLegend();
                            l.setEnabled(false);

                            chart.getAxisLeft().setEnabled(false);
                            chart.getAxisLeft().setSpaceTop(40);
                            chart.getAxisLeft().setSpaceBottom(40);
                            chart.getAxisRight().setEnabled(false);

                            chart.getXAxis().setEnabled(false);

                            chart.animateX(1000);

                            chart.invalidate(); // refresh
                            Log.d("calculate", String.valueOf(calculatekWh(radiationByHour, 72)));
                            binding.generatedPower.setText(String.format("With a solar panel of size %dm2, you can generate %f kWh over a day", 72, calculatekWh(radiationByHour, 72)));

                        }
                    });


        } else {
            Toast.makeText(this, "Cannot get location. Did you turn off your location services?", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
    }

    protected void startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(30 * 1000)
                .setFastestInterval(5 * 1000);

        // Request location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }


}
