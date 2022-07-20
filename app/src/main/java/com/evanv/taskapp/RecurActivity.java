package com.evanv.taskapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Spinner;

public class RecurActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    // Key for the value representing a bundle containing the user's input on recurrence
    public static final String EXTRA_RECUR = "com.evanv.taskapp.RecurActivity.extra.RECUR";
    // Tag for the currently displayed fragment.
    private static final String TAG_CURR_FRAG = "com.evanv.taskapp.RecurActivity.tag.CURR_FRAG";

    /**
     * Runs on activity creation. Initializes fragment, spinner, and their interaction.
     *
     * @param savedInstanceState Not used.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recur);

        // Set the displayed fragment to no recurrence
        Fragment fragment = new NoRecurFragment();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.replace(R.id.contentFragment, fragment);
        transaction.commit();

        // Set up the spinner to change fragments on user input
        Spinner spinner = findViewById(R.id.reoccurSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.reoccur_names, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        // Set up the button to submit information if clicked
        Button button = findViewById(R.id.submitButton);
        button.setOnClickListener(view -> submit());
    }

    private void submit() {
        FragmentManager fm = getSupportFragmentManager();
        RecurInput input = (RecurInput) getSupportFragmentManager().findFragmentByTag(TAG_CURR_FRAG);
        Bundle toReturn = input.getRecurInfo();

        if (toReturn != null) {
            Intent replyIntent = new Intent();
            replyIntent.putExtra(EXTRA_RECUR, toReturn);
            setResult(RESULT_OK, replyIntent);
            finish();
        }
    }

    /**
     * Changes displayed fragment when user changes interval of time to recur on
     *
     * @param parent Adapter for the Spinner
     * @param view The spinner itself
     * @param pos Position selected
     * @param id ID of the spinner
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        // No recurrence
        if (pos == 0) {
            Fragment fragment = new NoRecurFragment();
            transaction.replace(R.id.contentFragment, fragment, TAG_CURR_FRAG);
        }
        // Daily recurrence
        else if (pos == 1) {
            Fragment fragment = new DailyRecurFragment();
            transaction.replace(R.id.contentFragment, fragment, TAG_CURR_FRAG);
        }
        // Weekly recurrence
        else if (pos == 2) {
            Fragment fragment = new WeeklyRecurFragment();
            transaction.replace(R.id.contentFragment, fragment, TAG_CURR_FRAG);
        }
        // Monthly recurrence
        else if (pos == 3) {
            Fragment fragment = new MonthlyRecurFragment();
            transaction.replace(R.id.contentFragment, fragment, TAG_CURR_FRAG);
        }
        // Yearly recurrence
        else if (pos == 4) {
            Fragment fragment = new YearlyRecurFragment();
            transaction.replace(R.id.contentFragment, fragment, TAG_CURR_FRAG);
        }
        // Invalid call
        else {
            return;
        }
        transaction.commit();
    }

    /**
     * Required by AdapterView.OnItemSelectedListener Interface
     *
     * @param adapterView not used
     */
    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        // Do Nothing
    }
}