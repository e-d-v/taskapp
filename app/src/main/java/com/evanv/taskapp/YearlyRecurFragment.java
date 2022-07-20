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
 * Fragment that represents yearly recurrences
 *
 * @author Evan Voogd
 */
public class YearlyRecurFragment extends Fragment implements RecurInput {
    // Value for a Bundle extra that represents yearly recurrence happening.
    public static final String EXTRA_VAL_TYPE = "com.evanv.taskapp.YearlyRecurFragment.extra.val.TYPE";
    // Extra key for a value containing how many years between each recurrence of this event.
    public static final String EXTRA_INTERVAL = "com.evanv.taskapp.YearlyRecurFragment.extra.INTERVAL";
    // Extra key for a value containing how the event will recur (the 18th, 3rd monday, 18/21st etc.)
    public static final String EXTRA_RECUR_TYPE = "com.evanv.taskapp.YearlyRecurFragment.extra.RECUR_TYPE";
    // Value for a Bundle extra that represents an event occurring the same day every year.
    public static final String EXTRA_VAL_STATIC = "com.evanv.taskapp.YearlyRecurFragment.extra.val.STATIC";
    // Value for a Bundle extra that represents an event occurring the same location every year
    // (e.g. 3rd monday of july).
    public static final String EXTRA_VAL_DYNAMIC = "com.evanv.taskapp.YearlyRecurFragment.extra.val.DYNAMIC";
    // Value for a Bundle extra that represents an event occurring the same month/day every year
    public static final String EXTRA_VAL_MULTIPLE_STATIC = "com.evanv.taskapp.YearlyRecurFragment.extra.val.MULTIPLE_STATIC";
    // Value for a Bundle extra that represents an event occurring the same location every year
    // (e.g. 3rd monday of july and august).
    public static final String EXTRA_VAL_MULTIPLE_DYNAMIC = "com.evanv.taskapp.YearlyRecurFragment.extra.val.MULTIPLE_DYNAMIC";
    // Value for a Bundle extra that represents an event occurring specific days/months (e.g. the 20th
    // and 21st of May and October)
    public static final String EXTRA_VAL_SPECIFIC = "com.evanv.taskapp.YearlyRecurFragment.extra.val.SPECIFIC";
    // Extra key for a value containing what days to recur on if specific is selected.
    public static final String EXTRA_DAYS = "com.evanv.taskapp.YearlyRecurFragment.extra.DAYS";
    // Extra key for a value containing what months to recur on if specific is selected.
    public static final String EXTRA_MONTHS = "com.evanv.taskapp.YearlyRecurFragment.extra.MONTHS";
    private EditText mIntervalET;   // Edit text containing the interval value
    private RadioGroup mRecurTypes; // RadioGroup representing the type of recurrence the user chooses
    private EditText mDaysET;       // EditText containing what days to increment on
    private EditText mMonthsET;     // EditText containing what months to increment on
    private int currSelection;      // Currently selected radio button index

    /**
     * Required empty public constructor
     */
    public YearlyRecurFragment() {
        // Required empty public constructor
    }

    /**
     * Creates a new fragment representing yearly recurrences
     *
     * @return A new instance of fragment YearlyRecurFragment.
     */
    public static YearlyRecurFragment newInstance() {
        YearlyRecurFragment fragment = new YearlyRecurFragment();
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
        View toReturn = inflater.inflate(R.layout.fragment_yearly_recur, container, false);

        mIntervalET = Objects.requireNonNull(toReturn).findViewById(R.id.yearsBetween);
        mRecurTypes = Objects.requireNonNull(toReturn).findViewById(R.id.yearlyRadioGroup);
        mDaysET = Objects.requireNonNull(toReturn).findViewById(R.id.recurDaysEditText);
        mMonthsET = Objects.requireNonNull(toReturn).findViewById(R.id.recurMonthsEditText);

        Intent intent = requireActivity().getIntent();
        String day = intent.getStringExtra(EventEntry.EXTRA_DAY);
        String desc = intent.getStringExtra(EventEntry.EXTRA_DESC);
        String month = intent.getStringExtra(EventEntry.EXTRA_MONTH);

        RadioButton rbStatic = toReturn.findViewById(R.id.radioButtonStatic);
        RadioButton rbDynamic = toReturn.findViewById(R.id.radioButtonDynamic);
        RadioButton rbMS = toReturn.findViewById(R.id.radioButtonMultipleStatic);
        RadioButton rbMD = toReturn.findViewById(R.id.radioButtonMultipleDynamic);

        rbStatic.setText(String.format(getString(R.string.recur_on), month, day));
        rbDynamic.setText(String.format(getString(R.string.recur_on_of), desc, month));
        rbMS.setText(String.format(getString(R.string.recur_specific_months), day));
        rbMD.setText(String.format(getString(R.string.recur_specific_months), desc));

        RadioGroup rg = toReturn.findViewById(R.id.yearlyRadioGroup);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                int index = radioGroup.indexOfChild(toReturn.findViewById(i));
                if (currSelection == 2 || currSelection == 3 || currSelection == 4) {
                    LinearLayout ll = toReturn.findViewById(R.id.recurMonthsLayout);
                    ll.setVisibility(View.INVISIBLE);
                }

                if (currSelection == 4) {
                    LinearLayout ll = toReturn.findViewById(R.id.recurDaysLayout);
                    ll.setVisibility(View.INVISIBLE);
                }

                if (index == 2 || index == 3 || index == 4) {
                    LinearLayout ll = toReturn.findViewById(R.id.recurMonthsLayout);
                    ll.setVisibility(View.VISIBLE);
                }

                if (index == 4) {
                    LinearLayout ll = toReturn.findViewById(R.id.recurDaysLayout);
                    ll.setVisibility(View.VISIBLE);
                }

                currSelection = index;
            }
        });

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
        // Create the bundle and set it's type to this
        Bundle toReturn = new Bundle();
        toReturn.putString(RecurInput.EXTRA_TYPE, EXTRA_VAL_TYPE);

        boolean flag = false; // If input is valid flag == false.

        // If valid, add the number of years between events to the bundle
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

        // Get the type of recurrence chosen
        int radioButtonID = mRecurTypes.getCheckedRadioButtonId();
        View radioButton = mRecurTypes.findViewById(radioButtonID);
        int idx = mRecurTypes.indexOfChild(radioButton);

        // If the user chose to recur on the same day every year
        if (idx == 0) {
            toReturn.putString(EXTRA_RECUR_TYPE, EXTRA_VAL_STATIC);
        }
        // If the user chose to recur on the xth weekday of month every year
        else if (idx == 1) {
            toReturn.putString(EXTRA_RECUR_TYPE, EXTRA_VAL_DYNAMIC);
        }
        // If the user chose to recur on specific months
        else if (idx == 2 || idx == 3 || idx == 4) {
            String userInput = mMonthsET.getText().toString();

            if (!userInput.equals("")) {
                toReturn.putString(EXTRA_MONTHS, userInput);

                String[] strs = userInput.split(",");

                for (String str : strs) {
                    if (!str.equals("Jan") && !str.equals("Feb") && !str.equals("Mar") &&
                            !str.equals("Apr") && !str.equals("May") && !str.equals("Jun") &&
                            !str.equals("Jul") && !str.equals("Aug") && !str.equals("Sep") &&
                            !str.equals("Oct") && !str.equals("Nov") && !str.equals("Dec")) {
                        flag = true;
                        Toast.makeText(getActivity(),
                                R.string.months_format,
                                Toast.LENGTH_LONG).show();
                        break;
                    }
                }
            }
            else {
                Toast.makeText(getActivity(),
                        R.string.months_format,
                        Toast.LENGTH_LONG).show();
                flag = true;
            }
        }

        // If the user chose to recur on the xth day of specific months
        if (idx == 2) {
            toReturn.putString(EXTRA_RECUR_TYPE, EXTRA_VAL_MULTIPLE_STATIC);
        }
        // If the user chose to recur on the xth weekday of specific months
        else if (idx == 3) {
            toReturn.putString(EXTRA_RECUR_TYPE, EXTRA_VAL_MULTIPLE_DYNAMIC);
        }
        // If the user chose to recur on specific days of specific months
        else if (idx == 4) {
            toReturn.putString(EXTRA_RECUR_TYPE, EXTRA_VAL_SPECIFIC);

            String userInput = mDaysET.getText().toString();

            // If valid, load the days the user entered to recur on into the bundle
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

        return (!flag) ? toReturn : null;
    }
}