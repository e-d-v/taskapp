package com.evanv.taskapp.ui.additem.recur;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.evanv.taskapp.R;
import com.evanv.taskapp.ui.additem.EventEntry;

import java.util.Arrays;
import java.util.Objects;

/**
 * Fragment that represents yearly recurrences
 *
 * @author Evan Voogd
 */
public class YearlyRecurFragment extends Fragment implements RecurInput {
    // Bundle Keys/Values
    // Value for a Bundle extra that represents yearly recurrence happening.
    public static final String EXTRA_VAL_TYPE = "com.evanv.taskapp.ui.additem.recur.YearlyRecurFragment.extra.val.TYPE";
    // Extra key for a value containing how many years between each recurrence of this event.
    public static final String EXTRA_INTERVAL = "com.evanv.taskapp.ui.additem.recur.YearlyRecurFragment.extra.INTERVAL";
    // Extra key for a value containing how the event will recur (the 18th, 3rd monday, 18/21st etc.)
    public static final String EXTRA_RECUR_TYPE = "com.evanv.taskapp.ui.additem.recur.YearlyRecurFragment.extra.RECUR_TYPE";
    // Value for a Bundle extra that represents an event occurring the same day every year.
    public static final String EXTRA_VAL_STATIC = "com.evanv.taskapp.ui.additem.recur.YearlyRecurFragment.extra.val.STATIC";
    // Value for a Bundle extra that represents an event occurring the same location every year
    // (e.g. 3rd monday of july).
    public static final String EXTRA_VAL_DYNAMIC = "com.evanv.taskapp.ui.additem.recur.YearlyRecurFragment.extra.val.DYNAMIC";
    // Value for a Bundle extra that represents an event occurring the same month/day every year
    public static final String EXTRA_VAL_MULTIPLE_STATIC = "com.evanv.taskapp.ui.additem.recur.YearlyRecurFragment.extra.val.MULTIPLE_STATIC";
    // Value for a Bundle extra that represents an event occurring the same location every year
    // (e.g. 3rd monday of july and august).
    public static final String EXTRA_VAL_MULTIPLE_DYNAMIC = "com.evanv.taskapp.ui.additem.recur.YearlyRecurFragment.extra.val.MULTIPLE_DYNAMIC";
    // Value for a Bundle extra that represents an event occurring specific days/months (e.g. the 20th
    // and 21st of May and October)
    public static final String EXTRA_VAL_SPECIFIC = "com.evanv.taskapp.ui.additem.recur.YearlyRecurFragment.extra.val.SPECIFIC";
    // Extra key for a value containing what days to recur on if specific is selected.
    public static final String EXTRA_DAYS = "com.evanv.taskapp.ui.additem.recur.YearlyRecurFragment.extra.DAYS";
    // Extra key for a value containing what months to recur on if specific is selected.
    public static final String EXTRA_MONTHS = "com.evanv.taskapp.ui.additem.recur.YearlyRecurFragment.extra.MONTHS";

    // Fields
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
        // Inflate the view
        View toReturn = inflater.inflate(R.layout.fragment_yearly_recur, container, false);

        // Get various views from the layout
        mIntervalET = Objects.requireNonNull(toReturn).findViewById(R.id.yearsBetween);
        mRecurTypes = Objects.requireNonNull(toReturn).findViewById(R.id.yearlyRadioGroup);
        mDaysET = Objects.requireNonNull(toReturn).findViewById(R.id.recurDaysEditText);
        mMonthsET = Objects.requireNonNull(toReturn).findViewById(R.id.recurMonthsEditText);

        // Load information from intent
        Intent intent = requireActivity().getIntent();
        String day = intent.getStringExtra(EventEntry.EXTRA_DAY);
        String desc = intent.getStringExtra(EventEntry.EXTRA_DESC);
        String month = intent.getStringExtra(EventEntry.EXTRA_MONTH);

        // Set text for the RadioButtons based on the date chosen by the user
        ((RadioButton) toReturn.findViewById(R.id.radioButtonStatic))
                .setText(String.format(getString(R.string.recur_on), month, day));
        ((RadioButton) toReturn.findViewById(R.id.radioButtonDynamic))
                .setText(String.format(getString(R.string.recur_on_of), desc, month));
        ((RadioButton) toReturn.findViewById(R.id.radioButtonMultipleStatic))
                .setText(String.format(getString(R.string.recur_specific_months), day));
        ((RadioButton) toReturn.findViewById(R.id.radioButtonMultipleDynamic))
                .setText(String.format(getString(R.string.recur_specific_months), desc));

        // Set onClickListener that will show/hide views based on in if they're necessary for the
        // user's selection
        ((RadioGroup) toReturn.findViewById(R.id.yearlyRadioGroup)).setOnCheckedChangeListener(
                new RecurrenceTypeOnChangedCheckListener(toReturn));

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

        // If valid, add the number of years between events to the bundle
        String input = mIntervalET.getText().toString();
        if (!input.equals("")) {
            toReturn.putInt(EXTRA_INTERVAL, Integer.parseInt(input));
        }
        else {
            Toast.makeText(getActivity(),
                    String.format(getString(R.string.recur_format_event),
                            getString(R.string.months)),Toast.LENGTH_LONG).show();
            return null;
        }

        // Get the type of recurrence chosen
        int idx = mRecurTypes.indexOfChild(
                mRecurTypes.findViewById(mRecurTypes.getCheckedRadioButtonId()));

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

            if (!isListOfMonths(userInput)) {
                return null;
            }

            toReturn.putString(EXTRA_MONTHS, userInput);
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

            if (!isListOfDays(userInput)) {
                return null;
            }

            toReturn.putString(EXTRA_DAYS, userInput);
        }

        // If there was no issue, return the bundle, if not, return null to signify the error
        return toReturn;
    }

    /**
     * Checks if String contains a comma delimited list of three-letter abbreviations of months
     *
     * @param userInput A string to check
     *
     * @return true if userInput is a comma delimited list of months, false otherwise
     */
    private boolean isListOfMonths(String userInput) {
        // Make sure the user entered a month if they chose an option that requires a month
        if (!userInput.equals("")) {
            String[] strs = userInput.split(",");

            // Make sure the selected month is a correct Date.
            for (String str : strs) {
                if (!Arrays.asList(new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul",
                        "Aug", "Sep", "Oct", "Nov", "Dec"}).contains(str)) {
                    Toast.makeText(getActivity(),
                            R.string.months_format,
                            Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        }
        else {
            Toast.makeText(getActivity(),
                    R.string.months_format,
                    Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    /**
     * Checks if String contains a comma delimited list of days
     *
     * @param userInput A string to check
     *
     * @return true if userInput is a comma delimited list of days, false otherwise
     */
    private boolean isListOfDays(String userInput) {
        // If valid, load the days the user entered to recur on into the bundle
        if (!userInput.equals("")) {

            // Make sure all the user-entered days are valid.
            try {
                String[] strs = userInput.split(",");

                for (String str : strs) {
                    Integer.parseInt(str);
                }
            }
            catch (Exception e) {
                Toast.makeText(getActivity(), R.string.date_list_format,
                        Toast.LENGTH_LONG).show();
                return false;
            }
        }
        else {
            Toast.makeText(getActivity(), R.string.no_days,
                    Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    /**
     * OnChangedCheckListener that updates the UI when the user chooses a different type of yearly
     * recurrence.
     */
    private class RecurrenceTypeOnChangedCheckListener implements RadioGroup.OnCheckedChangeListener {
        private final View layout;

        /**
         * Constructs a listener that will update the UI when a different type of yearly recurrence
         * is chosen.
         *
         * @param inflatedLayout The inflated layout for the YearlyRecurFragment.
         */
        public RecurrenceTypeOnChangedCheckListener(View inflatedLayout) {
            this.layout = inflatedLayout;
        }

        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int i) {
            int index = radioGroup.indexOfChild(layout.findViewById(i));
            LinearLayout ll;
            // If the "Select months to recur on" EditText is currently displayed, hide it
            if (currSelection == 2 || currSelection == 3 || currSelection == 4) {
                ll = layout.findViewById(R.id.recurMonthsLayout);
                ll.setVisibility(View.INVISIBLE);
            }

            // If the "Select days to recur on" EditText  is currently displayed, hide it
            if (currSelection == 4) {
                ll = layout.findViewById(R.id.recurDaysLayout);
                ll.setVisibility(View.INVISIBLE);
            }

            // If the "Select months to recur on" EditText needs to be displayed, show it
            if (index == 2 || index == 3 || index == 4) {
                ll = layout.findViewById(R.id.recurMonthsLayout);
                ll.setVisibility(View.VISIBLE);
            }

            // If the "Select days to recur on" EditText needs to be displayed, show it
            if (index == 4) {
                ll = layout.findViewById(R.id.recurDaysLayout);
                ll.setVisibility(View.VISIBLE);
            }

            currSelection = index;
        }
    }
}