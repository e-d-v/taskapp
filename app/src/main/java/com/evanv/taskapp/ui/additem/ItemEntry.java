package com.evanv.taskapp.ui.additem;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import com.evanv.taskapp.R;
import com.evanv.taskapp.ui.additem.recur.DailyRecurFragment;
import com.evanv.taskapp.ui.additem.recur.DatePickerFragment;
import com.evanv.taskapp.ui.additem.recur.MonthlyRecurFragment;
import com.evanv.taskapp.ui.additem.recur.WeeklyRecurFragment;
import com.evanv.taskapp.ui.additem.recur.YearlyRecurFragment;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.threeten.bp.LocalDate;
import org.threeten.bp.temporal.ChronoField;

/**
 * Contains shared functionality of entering Tasks and Events, such as recurrence handling and
 * submitListeners
 */
public abstract class ItemEntry extends BottomSheetDialogFragment {
    protected Bundle mRecur;
    protected View.OnClickListener mListener; // Listener for Submit Button
    protected long mEarlyDate;     // Holds the user selected early date
    // Key for the value representing the type of until statement (number or until)
    public static final String EXTRA_UNTIL_TYPE = "com.evanv.taskapp.ui.additem.recur.RecurActivity.extra.UNTIL_TYPE";
    // Value representing an event that recurs until a specific date.
    public static final String EXTRA_VAL_UNTIL = "com.evanv.taskapp.ui.additem.recur.RecurActivity.extra.val.UNTIL";
    // Value representing an event that recurs a set number of times.
    public static final String EXTRA_VAL_NUM = "com.evanv.taskapp.ui.additem.recur.RecurActivity.extra.val.NUM";
    // Key for the value representing the date the event stops recurring on / number of recurrences
    public static final String EXTRA_UNTIL = "com.evanv.taskapp.ui.additem.recur.RecurActivity.extra.UNTIL";
    private boolean mAllowEndDate; // True iff interval != 0

    /**
     * Launch a new intent to the RecurActivity, and give it the needed information
     */
    protected void intentRecur() {
        // Get the date information the user has entered
        LocalDate ecd;

        if (mEarlyDate == 0) {
            Toast.makeText(getContext(), "Please enter a start date first.", Toast.LENGTH_LONG).show();
            return;
        }

        ecd = LocalDate.ofEpochDay(mEarlyDate);

        // Get the day in month e.g. "31st"
        String day = EventEntry.getOrdinalDayInMonth(ecd);

        // Get the ordinal day of week e.g. "3rd Monday"
        String desc = EventEntry.getOrdinalDayInWeek(requireContext(), ecd);

        // Get the month e.g. "August"
        String month = getResources().getStringArray(R.array.months)
                [ecd.get(ChronoField.MONTH_OF_YEAR) - 1];

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Choose Recurrence Interval")
                .setItems(R.array.reoccur_names, ((dialogInterface, i) -> {
                    switch (i) {
                        case 0:
                            // Do nothing
                            break;
                        case 1:
                            // Daily recurrence
                            DailyRecurFragment daily = new DailyRecurFragment();
                            daily.addSubmitListener(view -> {
                                Bundle returned = daily.getRecurInfo();
                                if (returned != null) {
                                    daily.dismiss();
                                    mAllowEndDate =
                                            returned.getInt(DailyRecurFragment.EXTRA_INTERVAL) != 0;
                                    endInfo(returned);
                                }
                            });
                            daily.show(getParentFragmentManager(), "DAILY");
                            break;
                        case 2:
                            // Weekly recurrence
                            WeeklyRecurFragment weekly = new WeeklyRecurFragment();
                            weekly.addSubmitListener(view -> {
                                Bundle returned = weekly.getRecurInfo();
                                if (returned != null) {
                                    weekly.dismiss();
                                    mAllowEndDate =
                                            returned.getInt(WeeklyRecurFragment.EXTRA_INTERVAL) != 0;
                                    endInfo(returned);
                                }
                            });
                            weekly.show(getParentFragmentManager(), "WEEKLY");
                            break;
                        case 3:
                            // Monthly recurrence
                            MonthlyRecurFragment monthly = new MonthlyRecurFragment(day, desc);
                            monthly.addSubmitListener(view -> {
                                Bundle returned = monthly.getRecurInfo();
                                if (returned != null) {
                                    monthly.dismiss();
                                    mAllowEndDate =
                                            returned.getInt(MonthlyRecurFragment.EXTRA_INTERVAL) != 0;
                                    endInfo(returned);
                                }
                            });
                            monthly.show(getParentFragmentManager(), "MONTHLY");
                            break;
                        case 4:
                            // Yearly recurrence
                            YearlyRecurFragment yearly = new YearlyRecurFragment(day, desc, month);
                            yearly.addSubmitListener(view -> {
                                Bundle returned = yearly.getRecurInfo();
                                if (returned != null) {
                                    yearly.dismiss();
                                    mAllowEndDate =
                                            returned.getInt(YearlyRecurFragment.EXTRA_INTERVAL) != 0;
                                    endInfo(returned);
                                }
                            });
                            yearly.show(getParentFragmentManager(), "MONTHLY");
                            break;
                    }
                }));


        builder.create();
        builder.show();
    }

    private void endInfo(Bundle recurInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder((getContext()));
        builder.setTitle("Recur until date or number of times?")
                .setItems(R.array.recur_types, ((dialogInterface, i) -> {
                    switch (i) {
                        case 0:
                            recurNumTimes(recurInfo);
                            break;
                        case 1:
                            if (mAllowEndDate) {
                                recurEndDate(recurInfo);
                            }
                            else {
                                Toast.makeText(getContext(), R.string.error_recur_infinite,
                                        Toast.LENGTH_LONG).show();
                            }
                            break;
                    }
                }));
        builder.show();
    }

    private void recurNumTimes(Bundle recurInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edittext, null);
        builder.setView(view);
        EditText et = view.findViewById(R.id.editText);
        builder.setTitle("How many times to recur?")
                .setPositiveButton("OK", ((dialogInterface, i) -> {
                    if (et.getText().length() != 0) {
                        recurInfo.putString(EXTRA_UNTIL_TYPE, EXTRA_VAL_NUM);
                        recurInfo.putString(EXTRA_UNTIL, et.getText().toString());
                        mRecur = recurInfo;
                    }
                }));
        builder.show();
    }

    private void recurEndDate(Bundle recurInfo) {
        EditText fakeDdET = new EditText(getContext());
        fakeDdET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Do Nothing
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                recurInfo.putString(EXTRA_UNTIL_TYPE, EXTRA_VAL_UNTIL);
                recurInfo.putString(EXTRA_UNTIL, charSequence.toString());
                mRecur = recurInfo;
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Do Nothing
            }
        });
        // Set the max date so the early date can't be set as later than the due date
        LocalDate minDate = (mEarlyDate == 0) ? LocalDate.now() : LocalDate.ofEpochDay(mEarlyDate);

        // Generate and show the DatePicker
        DialogFragment newFragment = new DatePickerFragment(fakeDdET, getString(R.string.recur_until),
                minDate, null, minDate, 0, 0, false);
        newFragment.show(getParentFragmentManager(), "datePicker");
    }

    protected void setText(String toShow, TextView element, String formatString) {
        // Make starting text bold
        SpannableString dateText = new SpannableString(String.format
                (formatString, toShow));
        dateText.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                0, formatString.indexOf('\n'), 0);
        dateText.setSpan(new RelativeSizeSpan((float)0.75), formatString.indexOf('\n'),
                dateText.length(), 0);

        element.setText(dateText);
    }

    public void addSubmitListener(View.OnClickListener listener) {
        mListener = listener;
    }
}
