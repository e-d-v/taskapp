package com.evanv.taskapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Loading screen that appears while the task schedule is being optimized/loaded from file
 *
 * @author Evan Voogd
 */
public class OptimizingActivity extends AppCompatActivity {

    /**
     * Default onCreateMethod
     *
     * @param savedInstanceState not used
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_optimizing);
    }
}