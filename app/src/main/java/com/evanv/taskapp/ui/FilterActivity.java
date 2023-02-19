package com.evanv.taskapp.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.ui.text.android.InternalPlatformTextApi;
import androidx.fragment.app.DialogFragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.evanv.taskapp.R;
import com.evanv.taskapp.logic.LogicSubsystem;
import com.evanv.taskapp.logic.Task;
import com.evanv.taskapp.ui.additem.LabelEntry;
import com.evanv.taskapp.ui.additem.ProjectEntry;
import com.evanv.taskapp.ui.additem.TaskEntry;
import com.evanv.taskapp.ui.additem.recur.DatePickerFragment;
import com.evanv.taskapp.ui.main.MainActivity;
import com.google.android.material.chip.Chip;

import org.threeten.bp.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@InternalPlatformTextApi /**
 * Search field for user to lookup tasks.
 */
public class FilterActivity extends AppCompatActivity {
    private long mStartDate;  // Holds the user selected start date
    private long mEndDate;    // Holds the user selected end date
    private long mProject;    // Holds the ID of the user selected project
    private List<Long> mLabels;   // Holds the IDs of user selected labels
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

        setSupportActionBar(findViewById(R.id.toolbar));

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        mContext = this;

        // Add the AddLabelsListener
        findViewById(R.id.labelsLayout).setOnClickListener
                (new AddLabelsListener());

        // Add the PickProjectListener
        findViewById(R.id.projectLayout).setOnClickListener
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
        mLabels = new ArrayList<>();

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
                    LocalDate toConvert = LocalDate.from(Task.dateFormat.parse(charSequence.toString()));
                    mStartDate = toConvert.toEpochDay();

                    // Update the UI
                    TextView startDateLabel = findViewById(R.id.startDateLabel);
                    String dateString = Task.dateFormat.format(toConvert);
                    String formatString = getString(R.string.start_date_replace);
                    setText(dateString, startDateLabel, formatString);
                } catch (Exception ignored) {}
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Do Nothing
            }
        });
        findViewById(R.id.startDateLayout).setOnClickListener(view1 -> {
            // Set the max date so the early date can't be set as later than the due date
            LocalDate maxDate = (mEndDate == 0) ? null : LocalDate.ofEpochDay(mEndDate);
            fakeStartET.setText("");

            LocalDate defaultDate = mStartDate != 0 ? LocalDate.ofEpochDay(mStartDate) :
                    LocalDate.now();

            // Generate and show the DatePicker
            DialogFragment newFragment = new DatePickerFragment(fakeStartET, getString(R.string.start_date),
                    LocalDate.now(), maxDate, defaultDate, 0, 0, false);
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
                    LocalDate toConvert = LocalDate.from(Task.dateFormat.parse(charSequence.toString()));
                    mEndDate = toConvert.toEpochDay();

                    // Update the UI
                    TextView endDateLabel = findViewById(R.id.endDateLabel);

                    // Show end date
                    String dateString = Task.dateFormat.format(toConvert);
                    String formatString = getString(R.string.end_date_replace);
                    setText(dateString, endDateLabel, formatString);
                } catch (Exception ignored) {}
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Do Nothing
            }
        });
        findViewById(R.id.endDateLayout).setOnClickListener(view1 -> {
            // Set the max date so the early date can't be set as later than the due date
            LocalDate minDate = (mStartDate == 0) ? LocalDate.now() :
                    LocalDate.ofEpochDay(mStartDate);
            fakeEndET.setText("");

            LocalDate defaultDate = mEndDate == 0 ? minDate : LocalDate.ofEpochDay(mEndDate);

            // Generate and show the DatePicker
            DialogFragment newFragment = new DatePickerFragment(fakeEndET, getString(R.string.start_date),
                    minDate, null, defaultDate, 0, 0,false);
            newFragment.show(getSupportFragmentManager(), "datePicker");
        });
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
        long[] labels = mLabels.size() != 0 ? convertLongListToArray(mLabels) : null;
        intent.putExtra(TaskListActivity.EXTRA_LABELS, labels);

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

    /**
     * Converts an ArrayList of Strings to an array of Strings.
     *
     * @param list The list to convert to an array
     *
     * @return An array with the same items as the ArrayList
     */
    private long[] convertLongListToArray(List<Long> list) {
        long[] toReturn = new long[list.size()];
        Object[] toReturnObjs = list.toArray();
        for (int i = 0; i < toReturnObjs.length; i++) {
            toReturn[i] = (long) toReturnObjs[i];
        }

        return toReturn;
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
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle(R.string.pick_labels)
                    .setAdapter(new LabelChipAdapter<>(mContext, R.layout.chip_item),
                            (dialogInterface, i) -> {
                                // Do Nothing
                            })
                    .setPositiveButton(R.string.ok,
                            ((dialogInterface, unused) -> setText(Integer.toString(mLabels.size()),
                                    findViewById(R.id.labelsLabel), getString(R.string.labels_format))))
                    .setNeutralButton(getString(R.string.add_label),
                            ((dialogInterface, i) -> {
                                // Open Label Entry dialog
                                LabelEntry labelEntry = new LabelEntry();
                                labelEntry.show(getSupportFragmentManager(), "LABELENTRY");
                            }));


            AlertDialog diag = builder.create();

            ListView listView = diag.getListView();
            listView.setAdapter(new LabelChipAdapter<>(mContext, R.layout.chip_item));

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
                    .setAdapter(new ProjectChipAdapter<>(mContext, R.layout.chip_item),
                            (dialogInterface, i) -> {
                                // Do Nothing
                            })
                    .setPositiveButton(R.string.ok,
                            ((dialogInterface, unused) -> {
                                // Get name of selected project
                                String projectName = LogicSubsystem.getInstance()
                                        .getProjectName(mProject, mContext);
                                String formatString = getString(R.string.project_replace);

                                setText(projectName, findViewById(R.id.projectsLabel), formatString);
                            }))
                    .setNeutralButton(getString(R.string.add_project), ((dialogInterface, i) -> {
                        // Open Project Entry dialog
                        ProjectEntry projectEntry = new ProjectEntry();
                        projectEntry.show(getSupportFragmentManager(), "PROJECTENTRY");
                    }));


            AlertDialog diag = builder.create();

            ListView listView = diag.getListView();
            listView.setAdapter(new ProjectChipAdapter<>(mContext, R.layout.chip_item));

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
        else if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class ProjectChipAdapter<T> extends ArrayAdapter<T> {
        private ImageView mCurrentSelected;

        public ProjectChipAdapter(@NonNull Context context, int resource, @NonNull T[] objects) {
            super(context, resource, objects);
        }

        public ProjectChipAdapter(Context context, int chip_item) {
            super(context, chip_item);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                convertView = inflater.inflate(R.layout.chip_item, parent, false);
            }

            Chip chip = convertView.findViewById(R.id.chip);
            long projectID = LogicSubsystem.getInstance().getProjectID(position);

            chip.setText(LogicSubsystem.getInstance().getProjectName(projectID, mContext));

            // Set project color
            int[] colors = {R.color.pale_blue,
                    R.color.blue,
                    R.color.pale_green,
                    R.color.green,
                    R.color.pink,
                    R.color.red,
                    R.color.pale_orange,
                    R.color.orange,
                    R.color.lavender,
                    R.color.purple,
                    R.color.yellow,
                    R.color.gray};

            // What color is most readable on the background
            int[] textColors = { Color.BLACK,
                    Color.WHITE,
                    Color.BLACK,
                    Color.BLACK,
                    Color.BLACK,
                    Color.WHITE,
                    Color.BLACK,
                    Color.BLACK,
                    Color.BLACK,
                    Color.WHITE,
                    Color.BLACK,
                    Color.BLACK};

            // Set colors
            int projectColor = LogicSubsystem.getInstance().getProjectColor(projectID);
            chip.setChipBackgroundColorResource(colors[projectColor]);
            chip.setTextColor(textColors[projectColor]);

            View finalConvertView = convertView;
            chip.setOnClickListener(v -> {
                mProject = LogicSubsystem.getInstance().getProjectID(position);
                if (mCurrentSelected != null) {
                    mCurrentSelected.setVisibility(View.INVISIBLE);
                }
                mCurrentSelected = finalConvertView.findViewById(R.id.selectIndicator);
                mCurrentSelected.setVisibility(View.VISIBLE);
            });

            if (mProject == LogicSubsystem.getInstance().getProjectID(position)) {
                if (mCurrentSelected != null) {
                    mCurrentSelected.setVisibility(View.INVISIBLE);
                }
                mCurrentSelected = finalConvertView.findViewById(R.id.selectIndicator);
                mCurrentSelected.setVisibility(View.VISIBLE);
            }
            else {
                ImageView view = finalConvertView.findViewById(R.id.selectIndicator);
                view.setVisibility(View.INVISIBLE);
            }

            return convertView;
        }

        @Override
        public int getCount() {
            return LogicSubsystem.getInstance().getProjectNames().size();
        }
    }

    private class LabelChipAdapter<T> extends ArrayAdapter<T> {
        public LabelChipAdapter(@NonNull Context context, int resource, @NonNull T[] objects) {
            super(context, resource, objects);
        }

        public LabelChipAdapter(Context context, int chip_item) {
            super(context, chip_item);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                convertView = inflater.inflate(R.layout.chip_item, parent, false);
            }

            Chip chip = convertView.findViewById(R.id.chip);
            ImageView checkmark = convertView.findViewById(R.id.selectIndicator);
            long labelID = LogicSubsystem.getInstance().getLabelID(position);

            chip.setText(LogicSubsystem.getInstance().getLabelName(labelID));

            // Set project color
            int[] colors = {R.color.pale_blue,
                    R.color.blue,
                    R.color.pale_green,
                    R.color.green,
                    R.color.pink,
                    R.color.red,
                    R.color.pale_orange,
                    R.color.orange,
                    R.color.lavender,
                    R.color.purple,
                    R.color.yellow,
                    R.color.gray};

            // What color is most readable on the background
            int[] textColors = { Color.BLACK,
                    Color.WHITE,
                    Color.BLACK,
                    Color.BLACK,
                    Color.BLACK,
                    Color.WHITE,
                    Color.BLACK,
                    Color.BLACK,
                    Color.BLACK,
                    Color.WHITE,
                    Color.BLACK,
                    Color.BLACK};

            // Set colors
            int labelColor = LogicSubsystem.getInstance().getLabelColor(labelID);
            chip.setChipBackgroundColorResource(colors[labelColor]);
            chip.setTextColor(textColors[labelColor]);

            if (mLabels.contains(labelID)) {
                checkmark.setVisibility(View.VISIBLE);
            }
            else {
                checkmark.setVisibility(View.INVISIBLE);
            }

            chip.setOnClickListener(v -> {
                if (!mLabels.contains(labelID)) {
                    checkmark.setVisibility(View.VISIBLE);
                    mLabels.add(labelID);
                }
                else {
                    checkmark.setVisibility(View.INVISIBLE);
                    mLabels.remove(labelID);
                }
            });


            return convertView;
        }

        @Override
        public int getCount() {
            return LogicSubsystem.getInstance().getLabelNames().size();
        }
    }

    /**
     * Updates todayTime in SharedPreferences
     */
    @Override
    protected void onPause() {
        // Update todayTime in SharedPreferences
        SharedPreferences sp = getSharedPreferences(MainActivity.PREF_FILE, MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        edit.putLong(MainActivity.PREF_DAY, LogicSubsystem.getInstance().getStartDate().toEpochDay());
        edit.putInt(MainActivity.PREF_TIME, LogicSubsystem.getInstance().getTodayTime());
        edit.putLong(MainActivity.PREF_TIMED_TASK, LogicSubsystem.getInstance().getTimedID());
        edit.putLong(MainActivity.PREF_TIMER, LogicSubsystem.getInstance().getTimerStart());

        edit.apply();
        super.onPause();
    }
}