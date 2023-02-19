package com.evanv.taskapp.ui.additem;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import com.evanv.taskapp.R;
import com.evanv.taskapp.logic.Event;
import com.evanv.taskapp.logic.LogicSubsystem;
import com.evanv.taskapp.logic.Task;
import com.evanv.taskapp.ui.additem.recur.DatePickerFragment;
import com.evanv.taskapp.ui.additem.recur.NoRecurFragment;
import com.evanv.taskapp.ui.additem.recur.RecurInput;
import com.ibm.icu.text.RuleBasedNumberFormat;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.temporal.ChronoField;
import org.threeten.bp.temporal.ChronoUnit;

import java.util.Locale;

/**
 * The fragment that handles data entry for new events
 *
 * @author Evan Voogd
 */
public class EventEntry extends ItemEntry {
    // Fields
    private EditText mEditTextEventName; // EditText containing the name of the event
    private long mID = -1;               // ID of the Event to update, -1 if adding an event

    private LinearLayout mStartTimeLayout;
    private TextView mStartTimeLabel;
    private LinearLayout mEndTimeLayout;
    private TextView mEndTimeLabel;
    LocalDateTime mStartTime; // Start time for event
    LocalDateTime mEndTime;   // End time for event

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
        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogStyle);
    }

    /**
     * If no required fields are empty, pack user input into a bundle and return it.
     *
     * @return true if ran successfully, false otherwise.
     */
    @SuppressWarnings("unused")
    public boolean addItem() {
        // Get user input from views
        String eventName = mEditTextEventName.getText().toString();

        // Check if eventName is valid
        if (eventName.length() == 0) {
            Toast.makeText(getActivity(), R.string.name_error_event,
                    Toast.LENGTH_LONG).show();
            return false;
        }

        // Check if ECD is valid
        if (mStartTime == null) {
            Toast.makeText(getActivity(),
                    R.string.ecd_empty_event,
                    Toast.LENGTH_LONG).show();
            return false;
        }

        // Check if End Time is valid
        if (mEndTime == null) {
            Toast.makeText(getActivity(),
                    R.string.enter_end_time,
                    Toast.LENGTH_LONG).show();
            return false;
        }

        // Add/Edit the event in the LogicSubsystem
        LogicSubsystem.getInstance().editEvent(eventName, mStartTime, mEndTime, mRecur, mID,
                getContext());

        // Return true as item was added successfully.
        return true;
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
        mStartTimeLayout = view.findViewById(R.id.startDateLayout);
        mStartTimeLabel = view.findViewById(R.id.startDateLabel);
        mEndTimeLayout = view.findViewById(R.id.endDateLayout);
        mEndTimeLabel = view.findViewById(R.id.endDateLabel);

        // Make starting text bold
        setText("None Chosen", mStartTimeLabel, getString(R.string.start_time_format));
        setText("None Chosen", mEndTimeLabel, getString(R.string.end_time_format));

        // Add the default recurrence interval (none)
        mRecur = new Bundle();
        mRecur.putString(RecurInput.EXTRA_TYPE, NoRecurFragment.EXTRA_VAL_TYPE);

        // Add click handler to button
        Button button = view.findViewById(R.id.recurButton);
        button.setOnClickListener(v -> intentRecur());

        EditText fakeECDet = new EditText(getContext());
        mStartTimeLayout.setOnClickListener(v -> {
            // Clear End Time picker
            fakeECDet.setText("");

            LocalDate defaultDate = mStartTime != null ? mStartTime.toLocalDate() : LocalDate.now();

            int hourOfDay = mStartTime != null ? mStartTime.getHour() : 0;
            int minute = mStartTime != null ? mStartTime.getMinute() : 0;

            // Show a date picker fragment
            new DatePickerFragment(fakeECDet,
                    getString(R.string.start_time), LocalDate.now(), null, defaultDate, hourOfDay, minute,
                    true)
                    .show(getParentFragmentManager(), getTag());
        });
        fakeECDet.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Do Nothing
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    mStartTime = LocalDateTime.from(Event.dateFormat.parse(charSequence));
                    EventEntry.super.mEarlyDate = mStartTime.toLocalDate().toEpochDay();
                    setText(Event.dateFormat.format(mStartTime), mStartTimeLabel,
                            getString(R.string.start_time_format));
                    mEndTime = null;
                    setText("None Chosen", mEndTimeLabel, getString(R.string.end_time_format));
                } catch (Exception ignored) { }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Do Nothing
            }
        });
        EditText fakeDDet = new EditText(getContext());
        mEndTimeLayout.setOnClickListener(v -> {
            if (mStartTime != null) {
                int hourOfDay = mStartTime.getHour();
                int minute = mStartTime.getMinute();

                if (mEndTime != null) {
                    hourOfDay = mEndTime.getHour();
                    minute = mEndTime.getMinute();
                }

                fakeDDet.setText(Task.dateFormat.format(mStartTime.toLocalDate()));

                new TimePickerFragment(fakeDDet, "Choose end time", hourOfDay, minute)
                        .show(getParentFragmentManager(), getTag());
            }
            else {
                Toast.makeText(getContext(), getString(R.string.enter_start_time), Toast.LENGTH_LONG)
                        .show();
            }
        });
        fakeDDet.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Do Nothing
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    mEndTime = LocalDateTime.from(Event.dateFormat.parse(charSequence));
                    setText(Event.dateFormat.format(mEndTime), mEndTimeLabel,
                            getString(R.string.end_time_format));
                }
                catch (Exception ignored) { }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Do Nothing
            }
        });

        // Load id from intent to see if we're editing a task.
        if (mID != -1) {
            // Set the event name
            mEditTextEventName.setText(LogicSubsystem.getInstance().getEventName(mID));

            mStartTime = LogicSubsystem.getInstance().getEventECD(mID);
            EventEntry.super.mEarlyDate = mStartTime.toLocalDate().toEpochDay();
            setText(Event.dateFormat.format(mStartTime),
                    mStartTimeLabel, getString(R.string.start_time_format));
            mEndTime = LogicSubsystem.getInstance().getEventECD(mID).plus(
                    LogicSubsystem.getInstance().getEventTTC(mID), ChronoUnit.MINUTES);
            setText(Event.dateFormat.format(mEndTime), mEndTimeLabel,
                    getString(R.string.end_time_format));
        }

        if(mListener != null) {
            view.findViewById(R.id.submitButton).setOnClickListener(mListener);
        }

        view.findViewById(R.id.helpButton).setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.add_event_url)));
            startActivity(browserIntent);
        });

        // Inflate the layout for this fragment
        return view;
    }

    /**
     * Sets the ID of the event to edit.
     *
     * @param id the ID to edit.
     */
    public void setID(long id) {
        mID = id;
    }


    /**
     * Get the ordinal day in month for a specific Date (e.g. "31st")
     *
     * @param date The Date to build the ordinal date out of
     *
     * @return The ordinal day in month (e.g. "31st") of the given Date
     */
    public static String getOrdinalDayInMonth(LocalDate date) {
        // Format the number to ordinal (the "st" in "31st")
        RuleBasedNumberFormat formatter = new RuleBasedNumberFormat(Locale.US,
                RuleBasedNumberFormat.ORDINAL);

        // Return the day
        return formatter.format(date.get(ChronoField.DAY_OF_MONTH));
    }

    /**
     * Get the ordinal day in week for a specific Date (e.g. "3rd Monday")
     *
     * @param context The context of the call for resources
     *
     * @param date The Date to build the ordinal date out of
     * @return The ordinal day in week (e.g. "3rd Monday") of the given Date
     */
    public static String getOrdinalDayInWeek(Context context, LocalDate date) {
        // Get the day of week in month and day of week (e.g. the "3" and "Monday" in "3rd Monday")
        int dayOfWeekInMonth = date.get(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH);
        int weekday = date.get(ChronoField.DAY_OF_WEEK);

        // Format the number to ordinal (the "st" in "31st")
        RuleBasedNumberFormat formatter = new RuleBasedNumberFormat(Locale.US,
                RuleBasedNumberFormat.ORDINAL);

        // Get the description (formatted "3rd Monday)"
        String ordinalNumber = formatter.format(dayOfWeekInMonth);
        String weekdayString = context.getResources().getStringArray(R.array.weekdays)[weekday - 1];

        return ordinalNumber + " " + weekdayString;
    }
}