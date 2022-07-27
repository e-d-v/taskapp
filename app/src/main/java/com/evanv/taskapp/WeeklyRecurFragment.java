package com.evanv.taskapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Objects;

/**
 * Fragment Representing a Weekly Recurrence
 *
 * @author Evan Voogd
 */
public class WeeklyRecurFragment extends Fragment implements RecurInput {
    // Value for a Bundle extra that represents weekly recurrence happening.
    public static final String EXTRA_VAL_TYPE = "com.evanv.taskapp.WeeklyRecurFragment.extra.val.TYPE";
    // Extra key for a value containing how many weeks between each recurrence of this event.
    public static final String EXTRA_INTERVAL = "com.evanv.taskapp.WeeklyRecurFragment.extra.INTERVAL";
    // Extra key for a value containing an array of booleans representing if events occur these days
    public static final String EXTRA_DAYS = "com.evanv.taskapp.WeeklyRecurFragment.extra.DAYS";
    private EditText mIntervalET;  // Edit text containing the interval value
    private CheckBox[] checkBoxes; // Array of the CheckBoxes representing the weeks

    /**
     * Required empty public constructor
     */
    public WeeklyRecurFragment() {
        // Required empty public constructor
    }

    /**
     * Creates a new fragment representing weekly recurrences
     *
     * @return A new instance of fragment WeeklyRecurFragment.
     */
    public static WeeklyRecurFragment newInstance() {
        WeeklyRecurFragment fragment = new WeeklyRecurFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Required empty onCreate method
     *
     * @param savedInstanceState not used
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Used to get the EditText and CheckBoxes to allow it to be read from.
     *
     * @param inflater not used
     * @param container Holds all the views in the fragment
     * @param savedInstanceState not used
     * @return not used
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Get the views that contain user data.
        View toReturn = inflater.inflate(R.layout.fragment_weekly_recur, container, false);

        mIntervalET = Objects.requireNonNull(toReturn).findViewById(R.id.weeksBetween);
        checkBoxes = new CheckBox[7];
        checkBoxes[0] = Objects.requireNonNull(toReturn).findViewById(R.id.sundayCheckBox);
        checkBoxes[1] = Objects.requireNonNull(toReturn).findViewById(R.id.mondayCheckBox);
        checkBoxes[2] = Objects.requireNonNull(toReturn).findViewById(R.id.tuesdayCheckBox);
        checkBoxes[3] = Objects.requireNonNull(toReturn).findViewById(R.id.wednesdayCheckBox);
        checkBoxes[4] = Objects.requireNonNull(toReturn).findViewById(R.id.thursdayCheckBox);
        checkBoxes[5] = Objects.requireNonNull(toReturn).findViewById(R.id.fridayCheckBox);
        checkBoxes[6] = Objects.requireNonNull(toReturn).findViewById(R.id.saturdayCheckBox);

        // Inflate the layout for this fragment
        return toReturn;
    }

    /**
     * Returns a bundle containing the information about this event's recurrence
     *
     * @return a bundle containing extras defining the user's recurrence choices.
     */
    @Override
    public Bundle getRecurInfo() {
        // Create a bundle and set it's type to this one
        Bundle toReturn = new Bundle();
        toReturn.putString(RecurInput.EXTRA_TYPE, EXTRA_VAL_TYPE);

        boolean flag = false; // If input is valid flag == false.

        // Load the interval field into the bundle if valid
        String input = mIntervalET.getText().toString();
        if (!input.equals("")) {
            int interval = Integer.parseInt(input);
            toReturn.putInt(EXTRA_INTERVAL, interval);
        }
        else {
            Toast.makeText(getActivity(),
                    String.format(getString(R.string.recur_format_event),
                            getString(R.string.weeks)),Toast.LENGTH_LONG).show();
            flag = true;
        }

        // If at least one day is checked, load the list of days to repeat on into the bundle
        boolean[] daysToRepeatOn = new boolean[7];
        daysToRepeatOn[0] = checkBoxes[0].isChecked();
        daysToRepeatOn[1] = checkBoxes[1].isChecked();
        daysToRepeatOn[2] = checkBoxes[2].isChecked();
        daysToRepeatOn[3] = checkBoxes[3].isChecked();
        daysToRepeatOn[4] = checkBoxes[4].isChecked();
        daysToRepeatOn[5] = checkBoxes[5].isChecked();
        daysToRepeatOn[6] = checkBoxes[6].isChecked();
        if (daysToRepeatOn[0] || daysToRepeatOn[1] || daysToRepeatOn[2] || daysToRepeatOn[3] ||
                daysToRepeatOn[4] || daysToRepeatOn[5] || daysToRepeatOn[6]) {
            toReturn.putBooleanArray(EXTRA_DAYS, daysToRepeatOn);
        }
        else {
            Toast.makeText(getActivity(), R.string.no_day_selected,
                    Toast.LENGTH_LONG).show();
            flag = true;
        }

        // If input was valid, return, if not return null to signal invalid input to RecurActivity
        return (!flag) ? toReturn : null;
    }
}