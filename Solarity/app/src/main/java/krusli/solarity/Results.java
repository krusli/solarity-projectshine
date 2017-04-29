package krusli.solarity;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import krusli.solarity.databinding.ActivityResultsBinding;

public class Results extends AppCompatActivity {
    ActivityResultsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_results);

        /* handle intent */
        Intent intent = getIntent();
        Float avg = intent.getFloatExtra("LIGHT_VALUE", -1);    // -1 is not possible normally

        Log.d("avg @ Results.java", String.format("%f", avg));


    }
}
