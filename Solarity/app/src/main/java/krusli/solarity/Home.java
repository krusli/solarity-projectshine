package krusli.solarity;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.github.lzyzsd.circleprogress.CircleProgress;
import com.github.lzyzsd.circleprogress.DonutProgress;

import java.util.ArrayList;
import java.util.List;

import krusli.solarity.databinding.ActivityHomeBinding;

public class Home extends AppCompatActivity implements SensorEventListener {
    ActivityHomeBinding binding;

    private SensorManager mSensorManager;
    private Sensor mLight;

    private float sensorValue = 0;
    private ArrayList<Float> sensorSamples = new ArrayList<>();

    private boolean measuring = false;
    private Handler handler = new Handler();
    private Intent resultsIntent;

    private Runnable doneMeasuring = new Runnable() {
        @Override
        public void run() {
            if (measuring) {
                measuring = false;
                binding.startStopMeasuring.setText("Measure");
            }

            /* calculate average value */
            // TODO: if standard deviation > 100, re-measure
            float sum = 0;
            for (int i=0; i<sensorSamples.size(); i++) {
                sum += sensorSamples.get(i);
            }
            float avg = sum/sensorSamples.size();

            /* start Results activity */
            resultsIntent.putExtra("LIGHT_VALUE", avg);
            startActivity(resultsIntent);
        }
    };
    private Runnable sampleSensorData = new Runnable() {
        @Override
        public void run() {
            if (measuring) {
                sensorSamples.add(sensorValue);
//                Log.d("sensorSamples", sensorSamples.toString());
                handler.postDelayed(this, 100); // sample again 5 seconds later
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home);

        /* load light sensor to an instance */
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null) {
            List<Sensor> lightSensors = mSensorManager.getSensorList(Sensor.TYPE_LIGHT);
            for (int i = 0; i < lightSensors.size(); i++) {
                mLight = lightSensors.get(i);
            }
        }

        resultsIntent = new Intent(this, Results.class);

        final DonutProgress donutProgress = binding.donutProgress;

        donutProgress.setShowText(false);
//        donutProgress.setUnfinishedStrokeColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        donutProgress.setFinishedStrokeColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
//        donutProgress.setDonut_progress("100");



        /* when button clicked, start sampling from the light sensor */
        binding.startStopMeasuring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int timeToCountDownMilis = 5000;
                donutProgress.setMax(timeToCountDownMilis);

                // Initiates and starts the countdowntimer which gradually increases the "progress" in the progress bar
                new CountDownTimer(timeToCountDownMilis, 50) {

                    @Override
                    public void onTick(long millisUntilFinished) {
                        donutProgress.setProgress(timeToCountDownMilis - (int) millisUntilFinished);
                    }

                    @Override
                    public void onFinish() {

                    }

                }.start();

                binding.startStopMeasuring.setText("Measuring...");
                sensorSamples.clear();  // clear out old samples
                measuring = true;
                handler.post(sampleSensorData); // start sampling
                handler.postDelayed(doneMeasuring, 5000);   // stop measuring after 5 seconds
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        sensorValue = event.values[0];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
}
