package krusli.solarity;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import krusli.solarity.databinding.ActivityCalculatorBinding;

import static krusli.solarity.Results.PANEL_EFFICIENCY;


public class Calculator extends AppCompatActivity {
    ActivityCalculatorBinding binding;
    float[] radiationByHour;
    LineChart chart;
    double kWhPerM2PerDay;

    HashMap<Integer, ArrayList<Float>> userAddedUsage;

    Spinner spinner;
    Spinner timeSpinner;
    Spinner durationSpinner;
    TextView savingsBlurb;

    float calculatekWh(float[] radiationList, double panelSize) {
        float sum = 0;
        for (int i=0; i<radiationList.length; i++) {
            sum += panelSize * radiationList[i] * PANEL_EFFICIENCY * 1; // 1 hour
        }
        return sum/1000;
    }

    float calculateM2PerkWh(float[] radiationList, float kWh) {
        return kWh / calculatekWh(radiationList, 1);
    }

    void updateSavingsBlurb() {
        /* calculate savings */
        int powerSaved = 0;

        if (spinner != null) {
            float panelMaxPower = 1;
            String panelMaxPowerStr = spinner.getSelectedItem().toString();
            panelMaxPowerStr = panelMaxPowerStr.substring(0, panelMaxPowerStr.length()-3);
            panelMaxPower = Float.parseFloat(panelMaxPowerStr);
            float panelSize = calculateM2PerkWh(radiationByHour, panelMaxPower);
            for (int i=0; i<23; i++) {
                double kWFromSolar = panelSize * radiationByHour[i] * PANEL_EFFICIENCY;
                double kWUsed = 0;
                ArrayList<Float> kWUsageForHour = userAddedUsage.get(i);
                if (kWUsageForHour != null) {
                    for (int j=0; j<kWUsageForHour.size(); j++) {
                        kWUsed += kWUsageForHour.get(j);
                    }
                }
//                Log.d(String.valueOf(i), String.format("Used: %f, generated: %f", kWUsed, kWFromSolar));
                double delta = kWUsed - kWFromSolar;
                if (delta > 0) {    // more energy used than generated
                    powerSaved += kWFromSolar;
                }
                if (delta < 0) {    // more energy generated than used
                    powerSaved += kWUsed;
                }
            }

            savingsBlurb.setText(String.format("Installing a %s solar panel can save you %d kWh of power every day.",
                    spinner.getSelectedItem().toString(), powerSaved));
        }
    }

    void drawChart() {
        updateSavingsBlurb();

        List<Entry> entries = new ArrayList<>();

        ArrayList<ILineDataSet> datasets = new ArrayList<>();

        // load to entries
        for (int i=0; i<radiationByHour.length; i++) {
            float panelMaxPower = 1;
            if (spinner != null) {
                String panelMaxPowerStr = spinner.getSelectedItem().toString();
                panelMaxPowerStr = panelMaxPowerStr.substring(0, panelMaxPowerStr.length()-3);
                panelMaxPower = Float.parseFloat(panelMaxPowerStr);
            }

            entries.add(new Entry(i, radiationByHour[i] * calculateM2PerkWh(radiationByHour, panelMaxPower)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Solar power");
        dataSet.setColor(ContextCompat.getColor(getBaseContext(), R.color.colorPrimaryDark));
        dataSet.setCircleColor(ContextCompat.getColor(getBaseContext(), R.color.colorPrimary));
//                            dataSet.setValueTextColor(R.color.colorPrimaryText);
//        LineData lineData = new LineData(dataSet);
        datasets.add(dataSet);
//        chart.setData(lineData);

        /* load user added entries */
        List<Entry> userEntries = new ArrayList<>();
        // load to userEntries
        for (int i=0; i<24; i++) {
            int totalPower = 0;
            ArrayList<Float> value = userAddedUsage.get(i);
            if (value != null) {
                for (int j=0; j<value.size(); j++) {
                    totalPower += value.get(j);
                }
            }
            userEntries.add(new Entry(i, totalPower));
        }

        if (userEntries.size() > 0) {
            LineDataSet userDataSet = new LineDataSet(userEntries, "Power usage");
            userDataSet.setColor(ContextCompat.getColor(getBaseContext(), R.color.primary));
            userDataSet.setCircleColor(ContextCompat.getColor(getBaseContext(), R.color.primary_light));
            datasets.add(userDataSet);
        }

        LineData data = new LineData(datasets);
        chart.setData(data);


        Description description = new Description();
        description.setText("");
        chart.setDescription(description);
        chart.setDrawGridBackground(false);
//        Legend l = chart.getLegend();
//        l.setEnabled(false);
        chart.getAxisLeft().setEnabled(false);
        chart.getAxisLeft().setSpaceTop(40);
        chart.getAxisLeft().setSpaceBottom(40);
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setDrawAxisLine(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);

        chart.animateX(1000);

        chart.invalidate(); // refresh
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_calculator);

        userAddedUsage = new HashMap<>();

        binding.calcBlurb.setText("Energy by hour (W)");
        savingsBlurb = binding.savingsBlurb;

        /* handle intent */
        Intent intent = getIntent();
        radiationByHour = intent.getFloatArrayExtra("RADIATION_BY_HOUR");
        kWhPerM2PerDay = intent.getDoubleExtra("KWH_PER_M2_PER_DAY", -1);   // -1 not normal, TODO: handle

        chart = binding.calcChart;
        drawChart();

        spinner = binding.spinner;
        /* spinner: load values */
        List<String> list = new ArrayList<>();
        list.add("1 kW"); list.add("1.5 kW"); list.add("2 kW"); list.add("3 kW"); list.add("4 kW");
        list.add("5 kW"); list.add("7 kW"); list.add("8 kW"); list.add("9 kW"); list.add("10 kW");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        if (spinner != null) {  // TODO: handle resume
            spinner.setAdapter(dataAdapter);

            /* spinner: setOnItemClickListener */
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (view != null) {
                        drawChart();
//                        Toast.makeText(view.getContext(), String.valueOf(spinner.getSelectedItem()), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }

        timeSpinner = binding.timePickerSpinner;
        List<String> time = new ArrayList<>();
        for (int i=0; i<24; i++) {
            if (i/13 == 0) {    // AM
                if (i==0) {
                    time.add(String.format("%d %s", 12, "AM"));
                }
                else {
                    time.add(String.format("%d %s", i, "AM"));
                }
            }
            if (i/13 == 1) {    // PM
                time.add(String.format("%d %s", i-12, "PM"));
            }
        }
        ArrayAdapter<String> timeDataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, time);
        timeDataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (timeSpinner != null) {
            timeSpinner.setAdapter(timeDataAdapter);
        }

        durationSpinner = binding.durationSpinner;
        List<String> duration = new ArrayList<>();
        for (int i=0; i<23; i++) {
            if (i==0) {
                duration.add(String.format("%d hour", i+1));
            }
            else {
                duration.add(String.format("%d hours", i+1));
            }

        }
        ArrayAdapter<String> durationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, duration);
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (durationAdapter != null) {
            durationSpinner.setAdapter(durationAdapter);
        }

        final SeekBar wattageSelector = binding.wattageSelector;
        if (wattageSelector != null) {
            binding.wattage.setText(String.format("%d W", wattageSelector.getProgress()));

            wattageSelector.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    binding.wattage.setText(String.format("%d W", progress));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }


        Button addUsageBtn = binding.addUsage;
        addUsageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timeSpinner != null && wattageSelector != null && durationSpinner != null) {
                    int timeIndex = timeSpinner.getSelectedItemPosition();
                    int power = wattageSelector.getProgress();
                    int duration = durationSpinner.getSelectedItemPosition() + 1;

                    for (int i=0; timeIndex+i<24 && i<=duration; i++) {
                        ArrayList<Float> usageForTime = userAddedUsage.get(timeIndex+i);
                        if (usageForTime == null) {
                            usageForTime = new ArrayList<>();
                        }
                        usageForTime.add((float) power);
                        userAddedUsage.put(timeIndex+i, usageForTime);
                    }

                    drawChart();
                }
            }
        });

        Button typicalUsageBtn = binding.loadHouseholdData;
        typicalUsageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timeSpinner != null && wattageSelector != null && durationSpinner != null) {
                    userAddedUsage = new HashMap<Integer, ArrayList<Float>>();
                    ArrayList<Float> list;
                    list = new ArrayList<>(); list.add((float) 400); userAddedUsage.put(0, list);
                    list = new ArrayList<>(); list.add((float) 300); userAddedUsage.put(1, list);
                    list = new ArrayList<>(); list.add((float) 250); userAddedUsage.put(2, list);
                    list = new ArrayList<>(); list.add((float) 200); userAddedUsage.put(3, list);
                    list = new ArrayList<>(); list.add((float) 200); userAddedUsage.put(4, list);
                    list = new ArrayList<>(); list.add((float) 250); userAddedUsage.put(5, list);
                    list = new ArrayList<>(); list.add((float) 550); userAddedUsage.put(6, list);
                    list = new ArrayList<>(); list.add((float) 800); userAddedUsage.put(7, list);
                    list = new ArrayList<>(); list.add((float) 1000); userAddedUsage.put(8, list);
                    list = new ArrayList<>(); list.add((float) 1200); userAddedUsage.put(9, list);
                    list = new ArrayList<>(); list.add((float) 850); userAddedUsage.put(10, list);
                    list = new ArrayList<>(); list.add((float) 600); userAddedUsage.put(11, list);
                    list = new ArrayList<>(); list.add((float) 500); userAddedUsage.put(12, list);
                    list = new ArrayList<>(); list.add((float) 450); userAddedUsage.put(13, list);
                    list = new ArrayList<>(); list.add((float) 425); userAddedUsage.put(14, list);
                    list = new ArrayList<>(); list.add((float) 450); userAddedUsage.put(15, list);
                    list = new ArrayList<>(); list.add((float) 550); userAddedUsage.put(16, list);
                    list = new ArrayList<>(); list.add((float) 700); userAddedUsage.put(17, list);
                    list = new ArrayList<>(); list.add((float) 900); userAddedUsage.put(18, list);
                    list = new ArrayList<>(); list.add((float) 1150); userAddedUsage.put(19, list);
                    list = new ArrayList<>(); list.add((float) 1500); userAddedUsage.put(20, list);
                    list = new ArrayList<>(); list.add((float) 1300); userAddedUsage.put(21, list);
                    list = new ArrayList<>(); list.add((float) 900); userAddedUsage.put(22, list);
                    list = new ArrayList<>(); list.add((float) 500); userAddedUsage.put(23, list);
                    drawChart();
                }
            }
        });


    }
}
