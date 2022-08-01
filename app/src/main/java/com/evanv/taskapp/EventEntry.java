package com.evanv.taskapp;

import static com.evanv.taskapp.Task.clearDate;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.ibm.icu.text.RuleBasedNumberFormat;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;


/**
 * The fragment that handles data entry for new events
 *
 * @author Evan Voogd
 */
public class EventEntry extends Fragment implements ItemEntry {
    private Bundle mRecur;        // The value returned by the recur activity.
    private EditText mEditTextEventName; // EditText containing the name of the event
    private EditText mEditTextECD;       // EditText containing the date/time of the event
    private EditText mEditTextLength;    // EditText containing the length of the event
    // The day the user has entered (e.g. 18)
    public static final String EXTRA_DAY = "com.evanv.taskapp.EventEntry.extra.DAY";
    // The day the user has entered (e.g. 3rd monday)
    public static final String EXTRA_DESC = "com.evanv.taskapp.EventEntry.extra.DESC";
    // The month the user has entered
    public static final String EXTRA_MONTH = "com.evanv.taskapp.EventEntry.extra.MONTH";
    // The time the user has entered
    public static final String EXTRA_TIME = "com.evanv.taskapp.EventEntry.extra.TIME";
    // Allows data to be pulled from activity
    private ActivityResultLauncher<Intent> mStartForResult;

    /**
     * Required empty public constructor
     */
    public EventEntry() {
    }

    /**
     * Required empty onCreate method
     *
     * @param savedInstanceState not used
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStartForResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleRecurInput(result.getResultCode(),
                        result.getData()));

    }

    private void handleRecurInput(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            mRecur = data.getBundleExtra(RecurActivity.EXTRA_RECUR);
        }
    }

    /**
     * If no required fields are empty, pack user input into a bundle and return it.
     *
     * @return a Bundle containing user input if all fields are filled, null otherwise
     */
    @SuppressWarnings("unused")
    public Bundle getItem() {

        // Get user input from views
        String eventName = mEditTextEventName.getText().toString();
        String ecd = mEditTextECD.getText().toString();
        String length = mEditTextLength.getText().toString();

        boolean flag = false; // Allows us to Toast multiple errors at once.

        // Check if eventName is valid
        if (eventName.length() == 0) {
            Toast.makeText(getActivity(), R.string.name_error_event,
                    Toast.LENGTH_LONG).show();
            flag = true;
        }
        // Check if ECD is valid
        if (ecd.length() == 0) {
            Toast.makeText(getActivity(),
                    R.string.ecd_empty_event,
                    Toast.LENGTH_LONG).show();
            flag = true;
        }
        else {
            String[] fullTokens = ecd.split(" ");

            boolean ecdFlag = false; // true if there is an issue with ecd input

            // Check if ecd follows format MM/DD/YY HH:MM AM/PM
            if (fullTokens.length == 3) {
                String[] dateTokens = fullTokens[0].split("/");
                String[] timeTokens = fullTokens[1].split(":");

                if (dateTokens.length == 3 && timeTokens.length == 2) {
                    // Check if everything that's supposed to be a number is an integer
                    try {
                        int month = Integer.parseInt(dateTokens[0]);
                        int day = Integer.parseInt(dateTokens[1]);
                        int year = Integer.parseInt(dateTokens[2]);
                        int hour = Integer.parseInt(timeTokens[0]);
                        int minute = Integer.parseInt(timeTokens[1]);

                        // Make sure input makes sense
                        if (month > 12 || month < 1 || day > 31 || day < 1 || year < 0 ||
                                hour < 1 || hour > 12 || minute < 0 || minute > 59) {
                            ecdFlag = true;
                        }

                        // Make sure we're not scheduling an event for before today.
                        // Note: we allow a start time any time in the current date in case user is
                        // attempting to inform optimizer of a previous meeting they forgot to add
                        // to the calendar.
                        Date thisDay = clearDate(new Date());
                        Calendar userCal = Calendar.getInstance();
                        userCal.set(2000+year, month - 1, day);
                        Date userDay = clearDate(userCal.getTime());

                        if (userDay.before(thisDay)) {
                            Toast.makeText(getActivity(),
                                    R.string.ecd_format_event,
                                    Toast.LENGTH_LONG).show();
                            flag = true;
                        }
                    }
                    catch (Exception e) {
                        ecdFlag = true;
                    }

                    if (!fullTokens[2].equals(getString(R.string.am)) &&
                            !fullTokens[2].equals(getString(R.string.pm))) {
                        ecdFlag = true;
                    }
                }
            }
            else {
                ecdFlag = true;
            }

            if (ecdFlag) {
                Toast.makeText(getActivity(),
                        R.string.ecd_help_text_format,
                        Toast.LENGTH_LONG).show();
                flag = true;
            }
        }
        // Check if length is valid
        if (length.length() == 0) {
            Toast.makeText(getActivity(), R.string.ttc_error_empty, Toast.LENGTH_LONG).show();
            flag = true;
        }
        else {
            try {
                Integer.parseInt(length);
            }
            catch (Exception e) {
                Toast.makeText(getActivity(),
                        R.string.ttc_format_event,
                        Toast.LENGTH_LONG).show();
                flag = true;
            }
        }

        // If any required views are empty, return null to signify invalid input
        if (flag) {
            return null;
        }

        // Put user input into a bundle
        Bundle bundle = new Bundle();
        bundle.putString(AddItem.EXTRA_TYPE, AddItem.EXTRA_VAL_EVENT);
        bundle.putString(AddItem.EXTRA_NAME, eventName);
        bundle.putString(AddItem.EXTRA_START, ecd);
        bundle.putString(AddItem.EXTRA_TTC, length);
        bundle.putBundle(AddItem.EXTRA_RECUR, mRecur);

        // Return bundle containing user input
        return bundle;
    }

    /**
     * Initializes recurrence button/recurrence related information.
     *
     * @param inflater not used
     * @param container not used
     * @param savedInstanceState not used
     * @return not used
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View toReturn = inflater.inflate(R.layout.fragment_event_entry, container, false);

        // Get needed views
        mEditTextEventName = toReturn.findViewById(R.id.editTextEventName);
        mEditTextECD = toReturn.findViewById(R.id.editTextECD);
        mEditTextLength = toReturn.findViewById(R.id.editTextLength);

        // Add the default recurrence interval (none)
        mRecur = new Bundle();
        mRecur.putString(RecurInput.EXTRA_TYPE, NoRecurFragment.EXTRA_VAL_TYPE);

        // Add click handler to button
        Button button = toReturn.findViewById(R.id.recurButton);
        button.setOnClickListener(view -> intentRecur());

        mEditTextECD.setOnClickListener(v -> {
            new DatePickerFragment(mEditTextECD, getString(R.string.start_time), new Date(),
                    null, true).show(getParentFragmentManager(), getTag());
        });

        // Inflate the layout for this fragment
        return toReturn;
    }

    /**
     * Launch a new intent to the RecurActivity, and give it the needed information
     */
    private void intentRecur() {
        // Create a new intent
        Intent intent = new Intent(getActivity(), RecurActivity.class);

        // Get the date information the user has entered
        Calendar ecdCal = Calendar.getInstance();
        long time;
        try {
            String ecdText = mEditTextECD.getText().toString();
            Date ecd = Event.dateFormat.parse(ecdText);
            time = Objects.requireNonNull(ecd).getTime();
            ecdCal.setTime(Objects.requireNonNull(ecd));
        } catch (ParseException e) {
            Toast.makeText(getActivity(),
                    R.string.ecd_help_text_format,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Pull the month/day/week
        // Gets how many times this weekday has appeared in the month. (e.g. if it is the 3rd
        // monday of february, this says "3".
        int dayOfWeekInMonth = ecdCal.get(Calendar.DAY_OF_WEEK_IN_MONTH);
        int weekday = ecdCal.get(Calendar.DAY_OF_WEEK);
        int month = ecdCal.get(Calendar.MONTH);
        int day = ecdCal.get(Calendar.DAY_OF_MONTH);

        RuleBasedNumberFormat formatter = new RuleBasedNumberFormat(Locale.US, RuleBasedNumberFormat.ORDINAL);

        // Get the day
        String dayString = formatter.format(day);
        intent.putExtra(EXTRA_DAY, dayString);

        // Get the description (formatted "3rd Monday)"
        String ordinalNumber = formatter.format(dayOfWeekInMonth);
        String weekdayString = getResources().getStringArray(R.array.weekdays)[weekday - 1];
        intent.putExtra(EXTRA_DESC, ordinalNumber + " " + weekdayString);

        // Get the month
        String monthString = getResources().getStringArray(R.array.months)[month];
        intent.putExtra(EXTRA_MONTH, monthString);

        // Get the time
        intent.putExtra(EXTRA_TIME, time);

        // Launch RecurActivity
        mStartForResult.launch(intent);
    }

}