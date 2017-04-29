package krusli.solarity;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

import krusli.solarity.databinding.ActivityHomeBinding;

public class Home extends AppCompatActivity implements SensorEventListener {
    ActivityHomeBinding binding;

    private SensorManager mSensorManager;
    private Sensor mLight;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null) {
            List<Sensor> lightSensors = mSensorManager.getSensorList(Sensor.TYPE_LIGHT);
            for (int i = 0; i < lightSensors.size(); i++) {
                mLight = lightSensors.get(i);
            }

        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d("Event", "onSensorChanged");
        binding.mainText.setText(String.format("%f", event.values[0]));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

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
