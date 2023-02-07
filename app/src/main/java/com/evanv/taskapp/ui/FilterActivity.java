package com.evanv.taskapp.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.evanv.taskapp.R;
import com.evanv.taskapp.logic.LogicSubsystem;
import com.evanv.taskapp.logic.Task;
import com.evanv.taskapp.ui.additem.recur.DatePickerFragment;

import org.threeten.bp.LocalDate;

import java.util.ArrayList;

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
        findViewById(R.id.imageButtonLabels).setOnClickListener
                (new AddLabelsListener());

        // Add the PickProjectListener
        findViewById(R.id.imageButtonProject).setOnClickListener
                (new PickProjectListener());

        // Make starting text bold
        setText("None Chosen", findViewById(R.id.startDateLabel),
                getString(R.string.start_date_replace));
        setText("None Chosen", findViewById(R.id.endDateLabel),
                getString(R.string.end_date_replace));
        setText("None Chosen", findViewById(R.id.projectsLabel),
                getString(R.string.project_replace));
        setText("None", findViewById(R.id.labelsLabel), getString(R.string.labels_format));

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
                LocalDate toConvert = LocalDate.from(Task.dateFormat.parse(charSequence.toString()));
                mStartDate = toConvert.toEpochDay();

                // Update the UI
                TextView startDateLabel = findViewById(R.id.startDateLabel);
                String dateString = Task.dateFormat.format(toConvert);
                String formatString = getString(R.string.start_date_replace);
                setText(dateString, startDateLabel, formatString);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Do Nothing
            }
        });
        findViewById(R.id.startDateButton).setOnClickListener(view1 -> {
            // Set the max date so the early date can't be set as later than the due date
            LocalDate maxDate = (mEndDate == 0) ? null : LocalDate.ofEpochDay(mEndDate);
            fakeStartET.setText("");
            // Generate and show the DatePicker
            DialogFragment newFragment = new DatePickerFragment(fakeStartET, getString(R.string.start_date),
                    LocalDate.now(), maxDate, false);
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
                LocalDate toConvert = LocalDate.from(Task.dateFormat.parse(charSequence.toString()));
                mEndDate = toConvert.toEpochDay();

                // Update the UI
                TextView endDateLabel = findViewById(R.id.endDateLabel);

                // Show end date
                String dateString = Task.dateFormat.format(toConvert);
                String formatString = getString(R.string.end_date_replace);
                setText(dateString, endDateLabel, formatString);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Do Nothing
            }
        });
        findViewById(R.id.endDateButton).setOnClickListener(view1 -> {
            // Set the max date so the early date can't be set as later than the due date
            LocalDate minDate = (mStartDate == 0) ? LocalDate.now() :
                    LocalDate.ofEpochDay(mStartDate);
            fakeEndET.setText("");
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
        SeekBar seekBar = findViewById(R.id.seekBar);
        intent.putExtra(TaskListActivity.EXTRA_PRIORITY, seekBar.getProgress());

        // Get Completable
        CheckBox checkBox = findViewById(R.id.checkBox);
        intent.putExtra(TaskListActivity.EXTRA_COMPLETABLE, checkBox.isChecked());

        startActivity(intent);
    }

    private void setText(String toShow, TextView element, String formatString) {
        // Make starting text bold
        SpannableString dateText = new SpannableString(String.format
                (formatString, toShow));
        dateText.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                0, formatString.indexOf('\n'), 0);
        dateText.setSpan(new RelativeSizeSpan((float)0.75), formatString.indexOf('\n'),
                dateText.length(), 0);

        element.setText(dateText);
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
                                    selectedItems.remove(Integer.valueOf(index));
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

                                TextView labelsLabel = findViewById(R.id.labelsLabel);

                                // Get number of selected labels
                                int numLabels = mLabels.length;
                                setText(Integer.toString(numLabels), labelsLabel,
                                        getString(R.string.labels_format));
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
                                TextView projectLabel = findViewById(R.id.projectsLabel);

                                // Get name of selected project
                                String projectName = LogicSubsystem.getInstance()
                                        .getProjectName(mProject, mContext);
                                String formatString = getString(R.string.project_replace);
                                setText(projectName, projectLabel, formatString);
                            }));

            builder.create();
            builder.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.help_button_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_help) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.filter_tasks_url)));
            startActivity(browserIntent);
        }

        return super.onOptionsItemSelected(item);
    }
}