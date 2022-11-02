package com.evanv.taskapp.ui.additem.recur;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.evanv.taskapp.R;
import com.evanv.taskapp.logic.Task;
import com.evanv.taskapp.ui.additem.EventEntry;

import java.text.ParseException;
import java.util.Date;
import java.util.Objects;

/**
 * Activity that users enter information about event recurrence into. Uses an extremely similar
 * method of Bundles/abstraction to pass data as AddItem.
 *
 * @author Evan Voogd
 */
public class RecurActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    // Key for the value representing a bundle containing the user's input on recurrence
    public static final String EXTRA_RECUR = "com.evanv.taskapp.ui.additem.recur.RecurActivity.extra.RECUR";
    // Tag for the currently displayed fragment.
    private static final String TAG_CURR_FRAG = "com.evanv.taskapp.ui.additem.recur.RecurActivity.tag.CURR_FRAG";
    boolean inputShown; // true if recur_n_times, false if recur_until
    // Key for the value representing the type of until statement (number or until)
    public static final String EXTRA_UNTIL_TYPE = "com.evanv.taskapp.ui.additem.recur.RecurActivity.extra.UNTIL_TYPE";
    // Value representing an event that recurs until a specific date.
    public static final String EXTRA_VAL_UNTIL = "com.evanv.taskapp.ui.additem.recur.RecurActivity.extra.val.UNTIL";
    // Value representing an event that recurs a set number of times.
    public static final String EXTRA_VAL_NUM = "com.evanv.taskapp.ui.additem.recur.RecurActivity.extra.val.NUM";
    // Key for the value representing the date the event stops recurring on / number of recurrences
    public static final String EXTRA_UNTIL = "com.evanv.taskapp.ui.additem.recur.RecurActivity.extra.UNTIL";

    private long time; // The time the user has entered

    /**
     * Runs on activity creation. Initializes fragment, spinner, and their interaction.
     *
     * @param savedInstanceState Not used.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recur);

        time = getIntent().getLongExtra(EventEntry.EXTRA_TIME, (new Date()).getTime());

        // Set the displayed fragment to no recurrence
        Fragment fragment = new NoRecurFragment();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.replace(R.id.contentFragment, fragment);
        transaction.commit();

        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.set_recur_interval);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set up the spinner to change fragments on user input
        Spinner spinner = findViewById(R.id.reoccurSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.reoccur_names, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
        spinner = findViewById(R.id.reoccurSpanSpinner);
        adapter = ArrayAdapter.createFromResource(this, R.array.timespans,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        // Set up the button to submit information if clicked
        Button button = findViewById(R.id.submitButton);
        button.setOnClickListener(view -> submit());

        inputShown = true;
    }

    /**
     * Submit the recurrence information back to the EventEntry fragment
     */
    private void submit() {
        // Get the information from the displayed fragment
        RecurInput input = (RecurInput) getSupportFragmentManager().findFragmentByTag(TAG_CURR_FRAG);
        Bundle toReturn = Objects.requireNonNull(input).getRecurInfo();

        boolean flag = toReturn != null; // true if input is valid

        // If the user input in the fragment was valid, and recurrence was chosen, load number
        // of recurrences into the bundle
        if (toReturn != null &&
                !toReturn.get(RecurInput.EXTRA_TYPE).equals(NoRecurFragment.EXTRA_VAL_TYPE)) {
            EditText timespan = findViewById(R.id.recurTSET);
            String timespanStr = timespan.getText().toString();

            // If valid, load the number of recurrences into the bundle
            if (!timespanStr.equals("")) {
                // The user chose to recur until a specific date
                if (!inputShown) {
                    try {
                        Task.dateFormat.parse(timespanStr);
                        toReturn.putString(EXTRA_UNTIL_TYPE, EXTRA_VAL_UNTIL);
                    } catch (ParseException e) {
                        Toast.makeText(this, R.string.date_format_task, Toast.LENGTH_LONG)
                                .show();
                        flag = false;
                    }
                }
                // The user chose to recur a specific number of times
                else {
                    toReturn.putString(EXTRA_UNTIL_TYPE, EXTRA_VAL_NUM);
                }

                Objects.requireNonNull(toReturn).putString(EXTRA_UNTIL,timespanStr);
            }
            else {
                Toast.makeText(this,
                        "Make sure to enter a recurrence interval.",Toast.LENGTH_LONG).show();
                flag = false;
            }
        }


        // If valid, send the bundle containing all recurrence information to EventEntry
        if (flag) {
            Intent replyIntent = new Intent();
            replyIntent.putExtra(EXTRA_RECUR, toReturn);
            setResult(RESULT_OK, replyIntent);
            finish();
        }
    }

    /**
     * Dispatches to various helper functions when a different item is selected in this layout.
     *
     * @param parent Adapter for the Spinner
     * @param view The item in the spinner itself
     * @param pos Position selected
     * @param id ID of the spinner
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // If the type of the recurrence is changed
        if (parent.getId() == R.id.reoccurSpinner) {
            changeRecurrenceType(pos);
        }
        // If the type of timespan is changed, show the different UI
        else if (parent.getId() == R.id.reoccurSpanSpinner) {
            changeTimespanType(pos);
        }
    }

    /**
     * Update UI when recurrence type is changed.
     *
     * @param pos Which recurrence type was chosen (index into spinner)
     */
    private void changeRecurrenceType(int pos) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment;
        EditText et = findViewById(R.id.recurTSET);

        // No recurrence
        if (pos == 0) {
            fragment = new NoRecurFragment();
            transaction.replace(R.id.contentFragment, fragment, TAG_CURR_FRAG);

            et.setVisibility(View.INVISIBLE);
        }
        else {
            et.setVisibility(View.VISIBLE);

            // Daily recurrence
            if (pos == 1) {
                fragment = new DailyRecurFragment();
                transaction.replace(R.id.contentFragment, fragment, TAG_CURR_FRAG);
            }
            // Weekly recurrence
            else if (pos == 2) {
                fragment = new WeeklyRecurFragment();
                transaction.replace(R.id.contentFragment, fragment, TAG_CURR_FRAG);
            }
            // Monthly recurrence
            else if (pos == 3) {
                fragment = new MonthlyRecurFragment();
                transaction.replace(R.id.contentFragment, fragment, TAG_CURR_FRAG);
            }
            // Yearly recurrence
            else if (pos == 4) {
                fragment = new YearlyRecurFragment();
                transaction.replace(R.id.contentFragment, fragment, TAG_CURR_FRAG);
            }
            // Invalid call
            else {
                return;
            }
        }
        transaction.commit();
    }

    /**
     * Update UI when recurrence interval is changed.
     *
     * @param pos Which recurrence interval was chosen (index into spinner)
     */
    private void changeTimespanType(int pos) {
        EditText et = findViewById(R.id.recurTSET);

        // Recur n times
        if (pos == 0) {
            et.setHint(R.string.recur_times);
            inputShown = true;

            et.setCursorVisible(true);
            et.setFocusable(true);
            et.setInputType(InputType.TYPE_CLASS_NUMBER);
            et.setOnClickListener(null);
            et.getText().clear();
        }
        // Recur until x date
        else if (pos == 1) {
            et.setHint(getString(R.string.recur_until));
            inputShown = false;

            et.setCursorVisible(false);
            et.setFocusable(false);
            et.setInputType(InputType.TYPE_NULL);
            et.getText().clear();
            et.setOnClickListener(v -> {
                Date minDate = new Date(time);

                DialogFragment newFragment = new DatePickerFragment(et,
                        getString(R.string.recur_until), minDate, null, false);
                newFragment.show(getSupportFragmentManager(), "datePicker");
            });
        }
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