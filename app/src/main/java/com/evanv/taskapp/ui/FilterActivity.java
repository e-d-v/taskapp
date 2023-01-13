package com.evanv.taskapp.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.evanv.taskapp.R;
import com.evanv.taskapp.logic.Event;
import com.evanv.taskapp.logic.LogicSubsystem;
import com.evanv.taskapp.logic.Task;
import com.evanv.taskapp.ui.additem.TaskEntry;
import com.evanv.taskapp.ui.additem.recur.DatePickerFragment;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

/**
 * Search field for user to lookup tasks.
 */
public class FilterActivity extends AppCompatActivity {
    private long mStartDate;  // Holds the user selected start date
    private long mEndDate;    // Holds the user selected end date
    private long mProject;    // Holds the ID of the user selected project
    private long[] mLabels;   // Holds the IDs of user selected labels
    private Context mContext; // Context

    /**
     * Creates a FilterActivity
     *
     * @param savedInstanceState Not used
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        mContext = this;

        // Add the AddLabelsListener
        ((ImageButton) findViewById(R.id.imageButtonLabels)).setOnClickListener
                (new AddLabelsListener());

        // Add the PickProjectListener
        ((ImageButton) findViewById(R.id.imageButtonProject)).setOnClickListener
                (new PickProjectListener());

        // Make starting text bold
        String startString = getString(R.string.start_date_label);
        SpannableString startDateText = new SpannableString(startString);
        startDateText.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                0, startString.indexOf('\n'), 0);
        ((TextView) findViewById(R.id.startDateLabel)).setText(startDateText);

        String endString = getString(R.string.end_date_label);
        SpannableString endDateText = new SpannableString(endString);
        endDateText.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                0, endString.indexOf('\n'), 0);
        ((TextView) findViewById(R.id.endDateLabel)).setText(endDateText);

        String projectString = getString(R.string.project_label);
        SpannableString projectText = new SpannableString(projectString);
        projectText.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                0, projectString.indexOf('\n'), 0);
        ((TextView) findViewById(R.id.projectsLabel)).setText(projectText);

        String labelsString = getString(R.string.label_label);
        SpannableString labelsText = new SpannableString(labelsString);
        labelsText.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                0, labelsString.indexOf('\n'), 0);
        ((TextView) findViewById(R.id.labelsLabel)).setText(labelsText);

        mStartDate = 0;
        mEndDate = 0;
        mProject = -1;

        // Set up the Start Date picker UI
        EditText fakeStartET = new EditText(this);
        fakeStartET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Do Nothing
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    Date toConvert = Task.dateFormat.parse(charSequence.toString());
                    mStartDate = Objects.requireNonNull(toConvert).getTime();

                    // Update the UI
                    TextView startDateLabel = findViewById(R.id.startDateLabel);

                    // Get number of selected labels
                    String dateString = Task.dateFormat.format(toConvert);
                    String formatString = getString(R.string.start_date_replace);

                    // Make starting text bold
                    SpannableString startDateText = new SpannableString(String.format
                            (formatString, dateString));
                    startDateText.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                            0, formatString.indexOf('\n'), 0);

                    startDateLabel.setText(startDateText);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Do Nothing
            }
        });
        findViewById(R.id.startDateButton).setOnClickListener(view1 -> {
            // Set the max date so the early date can't be set as later than the due date
            Date maxDate = (mEndDate == 0) ? null : new Date(mEndDate);

            // Generate and show the DatePicker
            DialogFragment newFragment = new DatePickerFragment(fakeStartET, getString(R.string.start_date),
                    new Date(), maxDate, false);
            newFragment.show(getSupportFragmentManager(), "datePicker");
        });

        // Set up the End Date picker UI
        EditText fakeEndET = new EditText(this);
        fakeEndET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Do Nothing
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    Date toConvert = Task.dateFormat.parse(charSequence.toString());
                    mEndDate = Objects.requireNonNull(toConvert).getTime();

                    // Update the UI
                    TextView endDateLabel = findViewById(R.id.endDateLabel);

                    // Get number of selected labels
                    String dateString = Task.dateFormat.format(toConvert);
                    String formatString = getString(R.string.end_date_replace);

                    // Make starting text bold
                    SpannableString endDateText = new SpannableString(String.format
                            (formatString, dateString));
                    endDateText.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                            0, formatString.indexOf('\n'), 0);

                    endDateLabel.setText(endDateText);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Do Nothing
            }
        });
        findViewById(R.id.endDateButton).setOnClickListener(view1 -> {
            // Set the max date so the early date can't be set as later than the due date
            Date minDate = (mStartDate == 0) ? Task.clearDate(new Date()) : new Date(mStartDate);

            // Generate and show the DatePicker
            DialogFragment newFragment = new DatePickerFragment(fakeEndET, getString(R.string.start_date),
                    minDate, null, false);
            newFragment.show(getSupportFragmentManager(), "datePicker");
        });

        mLabels = null;
    }

    /**
     * Search for a given task and pull up the TaskListActivity displaying the results
     *
     * @param view Not used
     */
    public void search(View view) {
        Intent intent = new Intent(this, TaskListActivity.class);

        // Get name
        String name = ((EditText)findViewById(R.id.editTextName)).getText().toString();
        if (name.length() > 0) {
            intent.putExtra(TaskListActivity.EXTRA_NAME, name);
        }

        // Get Start Date
        intent.putExtra(TaskListActivity.EXTRA_START_DATE, mStartDate);

        // Get End Date
        intent.putExtra(TaskListActivity.EXTRA_END_DATE, mEndDate);

        // Get Project
        intent.putExtra(TaskListActivity.EXTRA_PROJECT, mProject);

        // Get Labels
        intent.putExtra(TaskListActivity.EXTRA_LABELS, mLabels);

        // Get Minimum TTC
        String minTTC = ((EditText)findViewById(R.id.editTextMinTime)).getText().toString();
        if (minTTC.length() > 0) {
            intent.putExtra(TaskListActivity.EXTRA_MIN_TIME, Integer.parseInt(minTTC));
        }

        // Get Maximum TTC
        String maxTTC = ((EditText)findViewById(R.id.editTextMaxTime)).getText().toString();
        if (maxTTC.length() > 0) {
            intent.putExtra(TaskListActivity.EXTRA_MAX_TIME, Integer.parseInt(maxTTC));
        }

        // Get Priority
        SeekBar seekBar = ((SeekBar) findViewById(R.id.seekBar));
        intent.putExtra(TaskListActivity.EXTRA_PRIORITY, seekBar.getProgress());

        // Get Completable
        CheckBox checkBox = findViewById(R.id.checkBox);
        intent.putExtra(TaskListActivity.EXTRA_COMPLETABLE, checkBox.isChecked());

        startActivity(intent);
    }

    /**
     * Converts an ArrayList of Strings to an array of Strings.
     *
     * @param list The list to convert to an array
     *
     * @return An array with the same items as the ArrayList
     */
    private String[] convertListToArray(ArrayList<String> list) {
        String[] toReturn = new String[list.size()];
        Object[] toReturnObjs = list.toArray();
        for (int i = 0; i < toReturnObjs.length; i++) {
            toReturn[i] = (String) toReturnObjs[i];
        }

        return toReturn;
    }

    /**
     * Handles the pick labels dialog
     */
    private class AddLabelsListener implements View.OnClickListener {
        /**
         * Opens a dialog allowing the user to set labels for the task
         *
         * @param view the button
         */
        @Override
        public void onClick(View view) {
            // Get the list of labels for the dialog
            ArrayList<String> labelNames = LogicSubsystem.getInstance().getLabelNames();

            showPickerDialog(convertListToArray(labelNames));
        }

        /**
         * Builds and shows a picker dialog based on a list of label names.
         *
         * @param labelNamesArr List of names of labels
         */
        private void showPickerDialog(String[] labelNamesArr) {
            ArrayList<Integer> selectedItems = new ArrayList<>();
            // Define the dialog used to pick parent tasks
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle(R.string.pick_labels)
                    .setMultiChoiceItems(labelNamesArr, null,
                            ((dialogInterface, index, isChecked) -> {
                                // If checked, add to list of Tasks to be added as parents
                                if (isChecked) {
                                    selectedItems.add(index);
                                }
                                // If unchecked, remove form list of Tasks to be added as
                                // parents
                                else if (selectedItems.contains(index)) {
                                    selectedItems.remove(index);
                                }
                            })).setPositiveButton(R.string.ok,
                            ((dialogInterface, unused) -> {
                                // Convert selectedItems to array of label IDs
                                mLabels = new long[selectedItems.size()];

                                // For each label selected by the user, add it to the list
                                for (int i = 0; i < mLabels.length; i++) {
                                    mLabels[i] = LogicSubsystem.getInstance()
                                            .getLabelID(selectedItems.get(i));
                                }

                                TextView labelsLabel = (TextView)findViewById(R.id.labelsLabel);

                                // Get number of selected labels
                                int numLabels = mLabels.length;
                                String formatString = getString(R.string.label_replace);

                                // Make starting text bold
                                SpannableString labelsText = new SpannableString(String.format
                                        (formatString, numLabels));
                                labelsText.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                                        0, formatString.indexOf('\n'), 0);

                                labelsLabel.setText(labelsText);

                            }));

            builder.create();
            builder.show();
        }
    }

    /**
     * Handles the pick project dialog
     */
    private class PickProjectListener implements View.OnClickListener {
        /**
         * Opens a dialog allowing the user to set labels for the task
         *
         * @param view the button
         */
        @Override
        public void onClick(View view) {
            // Get the list of labels for the dialog
            ArrayList<String> projectNames = LogicSubsystem.getInstance().getProjectNames();

            showPickerDialog(convertListToArray(projectNames));
        }

        /**
         * Builds and shows a picker dialog based on a list of label names.
         *
         * @param projectNamesArr List of names of labels
         */
        private void showPickerDialog(String[] projectNamesArr) {
            // Define the dialog used to pick parent tasks
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle(R.string.pick_project)
                    .setSingleChoiceItems(projectNamesArr, -1, (dialogInterface, i) ->
                            mProject = LogicSubsystem.getInstance().getProjectID(i))
                    .setPositiveButton(R.string.ok,
                            ((dialogInterface, unused) -> {
                                TextView projectLabel = (TextView)findViewById(R.id.projectsLabel);

                                // Get name of selected project
                                String projectName = LogicSubsystem.getInstance()
                                        .getProjectName(mProject, mContext);
                                String formatString = getString(R.string.project_replace);

                                // Make starting text bold
                                SpannableString projectText = new SpannableString(String.format
                                        (formatString, projectName));
                                projectText.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                                        0, formatString.indexOf('\n'), 0);

                                projectLabel.setText(projectText);
                            }));

            builder.create();
            builder.show();
        }
    }
}