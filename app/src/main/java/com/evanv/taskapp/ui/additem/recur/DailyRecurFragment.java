package com.evanv.taskapp.ui.additem.recur;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import com.evanv.taskapp.R;

import java.util.Objects;

/**
 * Fragment that represents a daily recurrence.
 *
 * @author Evan Voogd
 */
public class DailyRecurFragment extends DialogFragment {
    private EditText mIntervalET;                 // Edit text containing the interval value
    private View.OnClickListener mSubmitListener; // Listener that is called when submit is pressed

    // Value for a Bundle extra that represents daily recurrence happening.
    public static final String EXTRA_VAL_TYPE = "com.evanv.taskapp.ui.additem.recur.DailyRecurFragment.extra.val.TYPE";
    // Extra key for a value containing how many days between each recurrence of this event.
    public static final String EXTRA_INTERVAL = "com.evanv.taskapp.ui.additem.recur.DailyRecurFragment.extra.INTERVAL";

    /**
     * Required empty public constructor
     */
    public DailyRecurFragment() {
    }

    /**
     * Add a listener that is called when the submit button is pressed.
     *
     * @param listener listener that will be called when the submit button is pressed.
     */
    public void addSubmitListener(View.OnClickListener listener) {
        mSubmitListener = listener;
    }

    /**
     * Creates a new fragment representing daily recurrences
     *
     * @return A new instance of fragment DailyRecurFragment.
     */
    @SuppressWarnings("unused")
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
        Objects.requireNonNull(getDialog()).setTitle("How many days between recurrences?");
        // Inflate the layout for this fragment
        View toReturn = inflater.inflate(R.layout.fragment_daily_recur, container, false);

        mIntervalET = Objects.requireNonNull(toReturn).findViewById(R.id.daysBetween);

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
            // Display a toast reminding the user to enter an interval of time.
            Toast.makeText(getActivity(), String.format(getString(R.string.recur_format_event),
                            getString(R.string.days)), Toast.LENGTH_LONG).show();

            return null;
        }
    }
}