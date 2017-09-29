package krusli.solarity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
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
//import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import krusli.solarity.databinding.ActivityResultsBinding;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class Results extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    ActivityResultsBinding binding;
    private GoogleApiClient mGoogleApiClient;

//    LocationRequest mLocationRequest;
//    Location mLastLocation;

    Float lightIntensity;
    List<Float> radiationByHour;
    double kWhPerM2PerDay;

    LineChart chart;


    LocationManager mLocationManager;


    public static final double PANEL_EFFICIENCY = 0.14;

    double calculatekWh(List<Float> radiationList, double panelSize) {
        double sum = 0;
        for (int i = 0; i < radiationList.size(); i++) {
            sum += panelSize * radiationList.get(i) * PANEL_EFFICIENCY * 1; // 1 hour
        }
        return sum / 1000;
    }

    void drawChart() {
        List<Entry> entries = new ArrayList<>();
        // load to entries
        for (int i = 0; i < radiationByHour.size(); i++) {
            entries.add(new Entry(i, radiationByHour.get(i)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Power in W/m2");
        dataSet.setColor(ContextCompat.getColor(getBaseContext(), R.color.colorPrimaryDark));
        dataSet.setCircleColor(ContextCompat.getColor(getBaseContext(), R.color.colorPrimary));
//                            dataSet.setValueTextColor(R.color.colorPrimaryText);
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        Description description = new Description();
        description.setText("");
        chart.setDescription(description);
        chart.setDrawGridBackground(false);
        Legend l = chart.getLegend();
        l.setEnabled(false);
        chart.getAxisLeft().setEnabled(false);
        chart.getAxisLeft().setSpaceTop(40);
        chart.getAxisLeft().setSpaceBottom(40);
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setDrawAxisLine(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);

        chart.animateX(1000);

        chart.invalidate(); // refresh
    }

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            Log.d("onLocationChanged", "onLocationChanged");
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            Log.d("Latitude, longitude", String.format("%f, %f", latitude, longitude));

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            httpClient.addInterceptor(logging);

            /* make a request to our backend */
            Retrofit retrofit = new Retrofit.Builder()
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl("http://krusli.me:5000/")
                    .client(httpClient.build())
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
                            radiationByHour = apiResponse.getRadiationByHour();

                            drawChart();

                            binding.generatedPower.setText(
                                    Html.fromHtml(String.format("1 m<sup>2</sup> of solar panels here generates an estimated %.2f kWh of electricity every day.",
                                            calculatekWh(radiationByHour, 1))));
                            kWhPerM2PerDay = calculatekWh(radiationByHour, 1);
                        }
                    });
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        binding = DataBindingUtil.setContentView(this, R.layout.activity_results);

        chart = binding.chart;

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
        Log.d("onStart", "Connecting to Google Play Services");
        mGoogleApiClient.connect();



        binding.resultsBlurb.setText(Html.fromHtml("Estimated solar radiation in Watts per m<sup>2</sup>"));

        binding.calcButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (radiationByHour != null) {  // implies kWhPerM2PerDay != null
                    Intent calcIntent = new Intent(getBaseContext(), Calculator.class);
                    float[] radiationByHourP = new float[radiationByHour.size()];
                    for (int i=0; i<radiationByHour.size(); i++) {
                        radiationByHourP[i] = radiationByHour.get(i);
                    }
                    calcIntent.putExtra("RADIATION_BY_HOUR", radiationByHourP);
                    calcIntent.putExtra("KWH_PER_M2_PER_DAY", kWhPerM2PerDay);
                    startActivity(calcIntent);
                }
            }
        });
//        // set chart height
//        android.view.ViewGroup.LayoutParams params = binding.chart.getLayoutParams();
//        DisplayMetrics dm = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(dm);
//        params.height = (int) (dm.heightPixels * 0.3);
//        binding.chart.setLayoutParams(params);

    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
//        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("onConnected", "Connected to Google Play Services");
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            return;
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, (float) 0, mLocationListener);


    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }



}
