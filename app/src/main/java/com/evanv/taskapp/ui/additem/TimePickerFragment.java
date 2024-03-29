package com.evanv.taskapp.ui.additem;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.evanv.taskapp.R;
import com.evanv.taskapp.logic.Event;

import org.threeten.bp.LocalTime;

/**
 * Class that creates a DatePicker to launch when setting a date for easier Date entering
 * for the user.
 *
 * @author Evan Voogd
 */
public class TimePickerFragment extends DialogFragment
        implements TimePickerDialog.OnTimeSetListener {
    private final EditText mET;   // The EditText this date will be placed in.
    private final String mTitle;  // The text for the title of the dialog
    private final int mHourOfDay; // Hour of day to default to
    private final int mMinute;    // Minute of hour to default to

    /**
     * Creates a TimePickerFragment for a dialog that puts its output in the given EditText,
     * with the given title.
     *
     * @param et The EditText this date will be placed in.
     * @param title The text for the title of the dialog.
     * @param hourOfDay Hour of day to default to
     * @param minute Minute of hour to default to
     */
    public TimePickerFragment(EditText et, String title, int hourOfDay, int minute) {
        mET = et;
        mTitle = title;
        mHourOfDay = hourOfDay;
        mMinute = minute;
    }

    /**
     * Creates a timepicker dialog
     *
     * @param savedInstanceState not used
     * @return A dialog to pick dates.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Create a new instance of DatePickerDialog and return it
        TimePickerDialog tp =
                new TimePickerDialog(requireContext(), this, mHourOfDay, mMinute,
                        false);
        tp.setMessage(mTitle);
        // If no max date is necessary skip this step
        return tp;
    }

    /**
     * Called when user selects time. Adds the time to the end of the EditText
     *
     * @param timePicker The TimePicker the user used
     * @param hour The hour the user selected
     * @param minute The minute the user selected
     */
    @Override
    public void onTimeSet(TimePicker timePicker, int hour, int minute) {
        String curr = mET.getText().toString();

        LocalTime time = LocalTime.of(hour, minute);
        String timeStr = Event.timeFormat.format(time);

        mET.setText(String.format(getString(R.string.two_words), curr, timeStr));
    }
}