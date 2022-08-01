package com.evanv.taskapp;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;
import java.util.Date;

/**
 * Class that creates a DatePicker to launch when setting a date for easier Date entering
 * for the user.
 *
 * @author Evan Voogd
 */
public class DatePickerFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {
    private final EditText mET;  // The EditText this date will be placed in.
    private final String mTitle; // The text for the title of the dialog
    private final Date mMinDate; // The earliest date user can choose
    private final Date mMaxDate; // The latest date user can choose
    // True if time is needed, false if not.
    private final boolean mTP;

    /**
     * Creates a DatePickerFragment for a dialog that puts its output in the given EditText,
     * with the given title.
     *
     * @param et The EditText this date will be placed in.
     * @param title The text for the title of the dialog.
     * @param minDate The minimum date to allow user to select
     * @param maxDate The latest date to allow user to select, null if not needed
     * @param tp true if timepicker should be shown
     */
    public DatePickerFragment(EditText et, String title, Date minDate, Date maxDate, boolean tp) {
        mET = et;
        mTitle = title;
        mMinDate = minDate;
        mMaxDate = maxDate;
        mTP = tp;
    }

    /**
     * Sets the default value of the date the user will select.
     *
     * @param savedInstanceState not used
     * @return A dialog to pick dates.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog and return it
        DatePickerDialog dp =
                new DatePickerDialog(requireContext(), this, year, month, day);
        dp.setMessage(mTitle);
        dp.getDatePicker().setMinDate(mMinDate.getTime());
        // If no max date is necessary skip this step
        if (mMaxDate != null) {
            dp.getDatePicker().setMaxDate(mMaxDate.getTime());
        }
        return dp;
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        // Do something with the date chosen by the user

        mET.setText(String.format(getString(R.string.generic_date), month + 1, day, year - 2000));

        if (mTP) {
            new TimePickerFragment(mET, "Choose start time").show(getParentFragmentManager(),
                    getTag());
        }
    }
}