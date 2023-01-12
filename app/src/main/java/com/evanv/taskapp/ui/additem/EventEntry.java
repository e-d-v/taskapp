package com.evanv.taskapp.ui.additem;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.evanv.taskapp.R;
import com.evanv.taskapp.logic.Event;
import com.evanv.taskapp.logic.LogicSubsystem;
import com.evanv.taskapp.logic.Task;
import com.evanv.taskapp.ui.additem.recur.DatePickerFragment;
import com.evanv.taskapp.ui.additem.recur.NoRecurFragment;
import com.evanv.taskapp.ui.additem.recur.RecurActivity;
import com.evanv.taskapp.ui.additem.recur.RecurInput;
import com.evanv.taskapp.ui.main.MainActivity;
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
    // Fields
    private Bundle mRecur;               // The value returned by the recur activity.
    private EditText mEditTextEventName; // EditText containing the name of the event
    private EditText mEditTextECD;       // EditText containing the date/time of the event
    private EditText mEditTextEndTime;    // EditText containing the length of the event
    // Allows data to be pulled from activity
    private ActivityResultLauncher<Intent> mStartForResult;

    // The day the user has entered (e.g. 18)
    public static final String EXTRA_DAY = "com.evanv.taskapp.ui.additem.EventEntry.extra.DAY";
    // The day the user has entered (e.g. 3rd monday)
    public static final String EXTRA_DESC = "com.evanv.taskapp.ui.additem.EventEntry.extra.DESC";
    // The month the user has entered
    public static final String EXTRA_MONTH = "com.evanv.taskapp.ui.additem.EventEntry.extra.MONTH";
    // The time the user has entered
    public static final String EXTRA_TIME = "com.evanv.taskapp.ui.additem.EventEntry.extra.TIME";

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

    /**
     * Function that is called when result is received from recurrence activity.
     *
     * @param resultCode Is Activity.RESULT_OK if ran successfully
     * @param data A bundle of data that describes the recurrence chosen
     */
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
        String endTime = mEditTextEndTime.getText().toString();

        // Check if eventName is valid
        if (eventName.length() == 0) {
            Toast.makeText(getActivity(), R.string.name_error_event,
                    Toast.LENGTH_LONG).show();
            return null;
        }

        Date startTime = null;

        // Check if ECD is valid
        if (ecd.length() == 0) {
            Toast.makeText(getActivity(),
                    R.string.ecd_empty_event,
                    Toast.LENGTH_LONG).show();
            return null;
        }
        else {
            try {
                startTime = Event.dateFormat.parse(ecd);
            } catch (ParseException e) {
                Toast.makeText(getActivity(), R.string.ecd_empty_event, Toast.LENGTH_LONG).show();
                return null;
            }
        }
        // Check if ECD is valid
        if (endTime.length() == 0) {
            Toast.makeText(getActivity(),
                    R.string.enter_end_time,
                    Toast.LENGTH_LONG).show();
            return null;
        }
        else {
            try {
                Date endTimeDate = Event.dateFormat.parse(endTime);
                assert endTimeDate != null;
                assert !endTimeDate.before(startTime);
            } catch (Throwable e) {
                Toast.makeText(getActivity(), R.string.enter_end_time, Toast.LENGTH_LONG).show();
                return null;
            }
        }

        // Put user input into a bundle
        Bundle bundle = new Bundle();
        bundle.putString(AddItem.EXTRA_TYPE, AddItem.EXTRA_VAL_EVENT);
        bundle.putString(AddItem.EXTRA_NAME, eventName);
        bundle.putString(AddItem.EXTRA_START, ecd);
        bundle.putString(AddItem.EXTRA_END, endTime);
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
        View view = inflater.inflate(R.layout.fragment_event_entry, container, false);

        // Get needed views
        mEditTextEventName = view.findViewById(R.id.editTextEventName);
        mEditTextECD = view.findViewById(R.id.editTextECD);
        mEditTextEndTime = view.findViewById(R.id.editTextEndTime);

        // Add the default recurrence interval (none)
        mRecur = new Bundle();
        mRecur.putString(RecurInput.EXTRA_TYPE, NoRecurFragment.EXTRA_VAL_TYPE);

        // Add click handler to button
        Button button = view.findViewById(R.id.recurButton);
        button.setOnClickListener(v -> intentRecur());

        mEditTextECD.setOnClickListener(v -> new DatePickerFragment(mEditTextECD,
                getString(R.string.start_time), new Date(), null, true)
                .show(getParentFragmentManager(), getTag()));
        mEditTextEndTime.setOnClickListener(v -> new DatePickerFragment(mEditTextEndTime,
                getString(R.string.end_time), new Date(), null, true)
                .show(getParentFragmentManager(), getTag()));

        // Initialize the information buttons to help the user understand the fields.
        ImageButton infoECD = view.findViewById(R.id.ecdInfoButton);
        ImageButton infoLength = view.findViewById(R.id.lengthInfoButton);
        infoECD.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.event_ecd_info);
            builder.setTitle(R.string.start_time);
            builder.show();
        });
        infoLength.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.event_length_info);
            builder.setTitle(R.string.length);
            builder.show();
        });

        // Load id from intent to see if we're editing a task.
        long id = requireActivity().getIntent().getLongExtra(MainActivity.EXTRA_ID, -1);
        String type = requireActivity().getIntent().getStringExtra(MainActivity.EXTRA_TYPE);

        if (type != null && type.equals(AddItem.EXTRA_VAL_EVENT) && id != -1) {
            // Set the event name
            mEditTextEventName.setText(LogicSubsystem.getInstance().getEventName(id));

            // Set the start time
            mEditTextECD.setText(Event.dateFormat.format(LogicSubsystem.getInstance().getEventECD(id)));

            // Set the end time
            Calendar calculateEndTime = Calendar.getInstance();
            calculateEndTime.setTime(LogicSubsystem.getInstance().getEventECD(id));
            calculateEndTime.add(Calendar.MINUTE, LogicSubsystem.getInstance().getEventTTC(id));

            mEditTextEndTime.setText(Event.dateFormat.format(calculateEndTime.getTime()));
        }

        // Inflate the layout for this fragment
        return view;
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

        // Get the day in month e.g. "31st"
        intent.putExtra(EXTRA_DAY, getOrdinalDayInMonth(ecdCal.getTime()));

        // Get the ordinal day of week e.g. "3rd Monday"
        intent.putExtra(EXTRA_DESC, getOrdinalDayInWeek(requireContext(), ecdCal.getTime()));

        // Get the month e.g. "August"
        intent.putExtra(EXTRA_MONTH,
                getResources().getStringArray(R.array.months)[ecdCal.get(Calendar.MONTH)]);

        // Get the time
        intent.putExtra(EXTRA_TIME, time);

        // Launch RecurActivity
        mStartForResult.launch(intent);
    }

    /**
     * Get the ordinal day in month for a specific Date (e.g. "31st")
     *
     * @param date The Date to build the ordinal date out of
     *
     * @return The ordinal day in month (e.g. "31st") of the given Date
     */
    public static String getOrdinalDayInMonth(Date date) {
        Calendar ecdCal = Calendar.getInstance();
        ecdCal.setTime(date);

        // Format the number to ordinal (the "st" in "31st")
        RuleBasedNumberFormat formatter = new RuleBasedNumberFormat(Locale.US,
                RuleBasedNumberFormat.ORDINAL);

        // Return the day
        return formatter.format(ecdCal.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * Get the ordinal day in week for a specific Date (e.g. "3rd Monday")
     *
     * @param date The Date to build the ordinal date out of
     * @param context The context of the call for resources
     *
     * @return The ordinal day in week (e.g. "3rd Monday") of the given Date
     */
    public static String getOrdinalDayInWeek(Context context, Date date) {
        Calendar ecdCal = Calendar.getInstance();
        ecdCal.setTime(date);

        // Get the day of week in month and day of week (e.g. the "3" and "Monday" in "3rd Monday")
        int dayOfWeekInMonth = ecdCal.get(Calendar.DAY_OF_WEEK_IN_MONTH);
        int weekday = ecdCal.get(Calendar.DAY_OF_WEEK);

        // Format the number to ordinal (the "st" in "31st")
        RuleBasedNumberFormat formatter = new RuleBasedNumberFormat(Locale.US,
                RuleBasedNumberFormat.ORDINAL);

        // Get the description (formatted "3rd Monday)"
        String ordinalNumber = formatter.format(dayOfWeekInMonth);
        String weekdayString = context.getResources().getStringArray(R.array.weekdays)[weekday - 1];

        return ordinalNumber + " " + weekdayString;
    }

}