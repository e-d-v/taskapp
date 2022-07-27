package com.evanv.taskapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Objects;

/**
 * Fragment that represents a daily recurrence.
 *
 * @author Evan Voogd
 */
public class DailyRecurFragment extends Fragment implements RecurInput {
    // Value for a Bundle extra that represents daily recurrence happening.
    public static final String EXTRA_VAL_TYPE = "com.evanv.taskapp.DailyRecurFragment.extra.val.TYPE";
    // Extra key for a value containing how many days between each recurrence of this event.
    public static final String EXTRA_INTERVAL = "com.evanv.taskapp.DailyRecurFragment.extra.INTERVAL";
    private EditText mIntervalET; // Edit text containing the interval value

    /**
     * Required empty public constructor
     */
    public DailyRecurFragment() {
    }

    /**
     * Creates a new fragment representing daily recurrences
     *
     * @return A new instance of fragment DailyRecurFragment.
     */
    public static DailyRecurFragment newInstance() {
        DailyRecurFragment fragment = new DailyRecurFragment();
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
     * Used to get the EditText to allow it to be read from.
     *
     * @param inflater not used
     * @param container Holds all the views in the fragment
     * @param savedInstanceState not used
     * @return not used
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View toReturn = inflater.inflate(R.layout.fragment_daily_recur, container, false);

        mIntervalET = Objects.requireNonNull(toReturn).findViewById(R.id.daysBetween);

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
        // Create a Bundle and set it's type to be Daily Recurrence
        Bundle toReturn = new Bundle();
        toReturn.putString(RecurInput.EXTRA_TYPE, EXTRA_VAL_TYPE);

        // Load the interval field into the Bundle if valid
        String input = mIntervalET.getText().toString();
        if (!input.equals("")) {
            int interval = Integer.parseInt(input);

            toReturn.putInt(EXTRA_INTERVAL, interval);

            return toReturn;
        }
        else {
            Toast.makeText(getActivity(),
                    String.format(getString(R.string.recur_format_event), getString(R.string.days)),Toast.LENGTH_LONG)
                    .show();

            return null;
        }
    }
}