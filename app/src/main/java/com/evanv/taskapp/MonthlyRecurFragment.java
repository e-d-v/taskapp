package com.evanv.taskapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.Objects;

/**
 * Fragment that represents a monthly recurrence.
 *
 * @author Evan Voogd
 */
public class MonthlyRecurFragment extends Fragment implements RecurInput {
    // Value for a Bundle extra that represents monthly recurrence happening.
    public static final String EXTRA_VAL_TYPE = "com.evanv.taskapp.MonthlyRecurFragment.extra.val.TYPE";
    // Extra key for a value containing how many months between each recurrence of this event.
    public static final String EXTRA_INTERVAL = "com.evanv.taskapp.MonthlyRecurFragment.extra.INTERVAL";
    // Extra key for a value containing how the event will recur (the 18th, 3rd monday, 18/21st etc.)
    public static final String EXTRA_RECUR_TYPE = "com.evanv.taskapp.MonthlyRecurFragment.extra.RECUR_TYPE";
    // Value for a Bundle extra that represents an event occurring the same day every month.
    public static final String EXTRA_VAL_STATIC = "com.evanv.taskapp.MonthlyRecurFragment.extra.val.STATIC";
    // Value for a Bundle extra that represents an event occurring the same location every month
    // (e.g. 3rd monday).
    public static final String EXTRA_VAL_DYNAMIC = "com.evanv.taskapp.MonthlyRecurFragment.extra.val.DYNAMIC";
    // Value for a Bundle extra that represents an event occurring on specific days every month
    public static final String EXTRA_VAL_SPECIFIC = "com.evanv.taskapp.MonthlyRecurFragment.extra.val.SPECIFIC";
    // Extra key for a value containing what days to recur on if specific is selected.
    public static final String EXTRA_DAYS = "com.evanv.taskapp.MonthlyRecurFragment.extra.DAYS";
    private EditText mIntervalET;   // Edit text containing the interval value
    private RadioGroup mRecurTypes; // RadioGroup representing the type of recurrence the user chooses
    private EditText mDaysET;       // EditText containing what days to increment on
    private int currSelection;      // Representing the index of the currently selected radio item

    /**
     * Required empty public constructor
     */
    public MonthlyRecurFragment() {
        // Required empty public constructor
    }

    /**
     * Creates a new fragment representing monthly recurrences
     *
     * @return A new instance of fragment MonthlyRecurFragment.
     */
    public static MonthlyRecurFragment newInstance(String param1, String param2) {
        MonthlyRecurFragment fragment = new MonthlyRecurFragment();
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
     * Used to get the EditTexts and RadioGroup to allow it to be read from.
     *
     * @param inflater not used
     * @param container Holds all the views in the fragment
     * @param savedInstanceState not used
     * @return not used
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View toReturn = inflater.inflate(R.layout.fragment_monthly_recur, container, false);

        mIntervalET = Objects.requireNonNull(toReturn).findViewById(R.id.monthsBetween);
        mRecurTypes = Objects.requireNonNull(toReturn).findViewById(R.id.monthlyRadioGroup);
        mDaysET = Objects.requireNonNull(toReturn).findViewById(R.id.recurDaysEditText);

        Intent intent = getActivity().getIntent();
        String day = intent.getStringExtra(EventEntry.EXTRA_DAY);
        String desc = intent.getStringExtra(EventEntry.EXTRA_DESC);

        RadioButton rbStatic = toReturn.findViewById(R.id.radioButtonStatic);
        RadioButton rbDynamic = toReturn.findViewById(R.id.radioButtonDynamic);

        rbStatic.setText(String.format(getString(R.string.recur_on_the), day));
        rbDynamic.setText(String.format(getString(R.string.recur_on_the), desc));

        currSelection = 0;

        RadioGroup rg = toReturn.findViewById(R.id.monthlyRadioGroup);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (currSelection == 2) {
                    LinearLayout ll = toReturn.findViewById(R.id.recurDaysLayout);
                    ll.setVisibility(View.INVISIBLE);
                    currSelection = i;
                }

                if (radioGroup.indexOfChild(toReturn.findViewById(i)) == 2) {
                    LinearLayout ll = toReturn.findViewById(R.id.recurDaysLayout);
                    ll.setVisibility(View.VISIBLE);
                    currSelection = 2;
                }
            }
        });

        return toReturn;
    }

    /**
     * Returns a bundle containing the information about this event's recurrence
     *
     * @return a bundle containing extras defining the user's recurrence choices.
     */
    @Override
    public Bundle getRecurInfo() {
        // Create a bundle and set it's type to this
        Bundle toReturn = new Bundle();
        toReturn.putString(RecurInput.EXTRA_TYPE, EXTRA_VAL_TYPE);

        boolean flag = false; // If input is valid flag == false.

        // If valid, load the interval into the bundle
        String input = mIntervalET.getText().toString();
        if (!input.equals("")) {
            int interval = Integer.parseInt(input);
            toReturn.putInt(EXTRA_INTERVAL, interval);
        }
        else {
            Toast.makeText(getActivity(),
                    String.format(getString(R.string.recur_format_event),
                            getString(R.string.months)),Toast.LENGTH_LONG).show();
            flag = true;
        }

        // Get the recurrence type chosen
        int radioButtonID = mRecurTypes.getCheckedRadioButtonId();
        View radioButton = mRecurTypes.findViewById(radioButtonID);
        int idx = mRecurTypes.indexOfChild(radioButton);

        // User chose to recur on the xth day of every month
        if (idx == 0) {
            toReturn.putString(EXTRA_RECUR_TYPE, EXTRA_VAL_STATIC);
        }
        // User chose to recur on the xth weekday of every month
        else if (idx == 1) {
            toReturn.putString(EXTRA_RECUR_TYPE, EXTRA_VAL_DYNAMIC);
        }
        // User chose to recur on multiple days of every month
        else if (idx == 2) {
            toReturn.putString(EXTRA_RECUR_TYPE, EXTRA_VAL_SPECIFIC);

            String userInput = mDaysET.getText().toString();

            // If the list of days is valid, load it into the bundle
            if (!userInput.equals("")) {
                toReturn.putString(EXTRA_DAYS, userInput);

                try {
                    String[] strs = userInput.split(",");

                    for (String str : strs) {
                        Integer.parseInt(str);
                    }
                }
                catch (Exception e) {
                    Toast.makeText(getActivity(), R.string.date_list_format,
                            Toast.LENGTH_LONG).show();
                    flag = true;
                }
            }
            else {
                Toast.makeText(getActivity(), R.string.no_days,
                        Toast.LENGTH_LONG).show();
                flag = true;
            }
        }

        // If valid return the bundle, if not, send null to the calling activity to inform it
        return (!flag) ? toReturn : null;
    }
}