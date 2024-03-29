package com.evanv.taskapp.ui.additem.recur;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import com.evanv.taskapp.R;

import java.util.Objects;

/**
 * Fragment that represents a monthly recurrence.
 *
 * @author Evan Voogd
 */
public class MonthlyRecurFragment extends DialogFragment {
    // Extras for storing monthly recurrence information
    // Value for a Bundle extra that represents monthly recurrence happening.
    public static final String EXTRA_VAL_TYPE = "com.evanv.taskapp.ui.additem.recur.MonthlyRecurFragment.extra.val.TYPE";
    // Extra key for a value containing how many months between each recurrence of this event.
    public static final String EXTRA_INTERVAL = "com.evanv.taskapp.ui.additem.recur.MonthlyRecurFragment.extra.INTERVAL";
    // Extra key for a value containing how the event will recur (the 18th, 3rd monday, 18/21st etc.)
    public static final String EXTRA_RECUR_TYPE = "com.evanv.taskapp.ui.additem.recur.MonthlyRecurFragment.extra.RECUR_TYPE";
    // Value for a Bundle extra that represents an event occurring the same day every month.
    public static final String EXTRA_VAL_STATIC = "com.evanv.taskapp.ui.additem.recur.MonthlyRecurFragment.extra.val.STATIC";
    // Value for a Bundle extra that represents an event occurring the same location every month
    // (e.g. 3rd monday).
    public static final String EXTRA_VAL_DYNAMIC = "com.evanv.taskapp.ui.additem.recur.MonthlyRecurFragment.extra.val.DYNAMIC";
    // Value for a Bundle extra that represents an event occurring on specific days every month
    public static final String EXTRA_VAL_SPECIFIC = "com.evanv.taskapp.ui.additem.recur.MonthlyRecurFragment.extra.val.SPECIFIC";
    // Extra key for a value containing what days to recur on if specific is selected.
    public static final String EXTRA_DAYS = "com.evanv.taskapp.ui.additem.recur.MonthlyRecurFragment.extra.DAYS";
    private View.OnClickListener mSubmitListener;

    private EditText mIntervalET; // Edit text containing the interval value
    private EditText mDaysET;     // EditText containing what days to increment on
    private int currSelection;    // Representing the index of the currently selected radio item
    private final String mDay;    // The day in the month the user has chosen
    private final String mDesc;   // A description of the day (e.g. "3rd Monday")

    /**
     * Sets day information for UI.
     *
     * @param day The day in the month the user has chosen
     * @param desc A description of the day (e.g. "3rd Monday")
     */
    public MonthlyRecurFragment(String day, String desc) {
        mDay = day;
        mDesc = desc;
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
        View toReturn = inflater.inflate(R.layout.fragment_monthly_recur, container,
                false);

        // Get the various views of this layout
        mIntervalET = Objects.requireNonNull(toReturn).findViewById(R.id.monthsBetween);
        mDaysET = Objects.requireNonNull(toReturn).findViewById(R.id.recurDaysEditText);

        // Set the text for this layout's RadioButtons based on user input in AddEvent screen
        ((RadioButton) toReturn.findViewById(R.id.radioButtonStatic))
                .setText(String.format(getString(R.string.recur_on_the), mDay));
        ((RadioButton) toReturn.findViewById(R.id.radioButtonDynamic))
                .setText(String.format(getString(R.string.recur_on_the), mDesc));

        // The initially selected radiobutton is index 0
        currSelection = 0;

        // Set the onClickListener for the radio buttons to hide/show fields
        ((RadioGroup) toReturn.findViewById(R.id.monthlyRadioGroup))
                .setOnCheckedChangeListener((radioGroup, id) -> {
                    LinearLayout ll = toReturn.findViewById(R.id.recurDaysLayout);

                    if (currSelection == 2) {
                        ll.setVisibility(View.INVISIBLE);
                    }

                    if (radioGroup.indexOfChild(toReturn.findViewById(id)) == 2) {
                        ll.setVisibility(View.VISIBLE);
                    }

                    currSelection = radioGroup.indexOfChild(toReturn.findViewById(id));
        });

        if (mSubmitListener != null) {
            toReturn.findViewById(R.id.submitButton).setOnClickListener(mSubmitListener);
        }

        return toReturn;
    }

    /**
     * Returns a bundle containing the information about this event's recurrence
     *
     * @return a bundle containing extras defining the user's recurrence choices.
     */
    public Bundle getRecurInfo() {
        // Create a bundle and set it's type to this
        Bundle toReturn = new Bundle();
        toReturn.putString(RecurInput.EXTRA_TYPE, EXTRA_VAL_TYPE);

        // If valid, load the interval into the bundle
        String input = mIntervalET.getText().toString();
        if (!input.equals("")) {
            toReturn.putInt(EXTRA_INTERVAL, Integer.parseInt(input));
        }
        else {
            Toast.makeText(getActivity(), String.format(getString(R.string.recur_format_event),
                            getString(R.string.months)),Toast.LENGTH_LONG).show();
            return null;
        }

        // User chose to recur on the xth day of every month
        if (currSelection == 0) {
            toReturn.putString(EXTRA_RECUR_TYPE, EXTRA_VAL_STATIC);
        }
        // User chose to recur on the xth weekday of every month
        else if (currSelection == 1) {
            toReturn.putString(EXTRA_RECUR_TYPE, EXTRA_VAL_DYNAMIC);
        }
        // User chose to recur on multiple days of every month
        else if (currSelection == 2) {
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
                    return null;
                }
            }
            else {
                Toast.makeText(getActivity(), R.string.no_days,
                        Toast.LENGTH_LONG).show();
                return null;
            }
        }

        return toReturn;
    }

    public void addSubmitListener(View.OnClickListener listener) {
        mSubmitListener = listener;
    }

}