package com.evanv.taskapp.ui.additem;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.evanv.taskapp.R;
import com.evanv.taskapp.logic.LogicSubsystem;
import com.evanv.taskapp.logic.Task;
import com.evanv.taskapp.ui.additem.recur.NoRecurFragment;
import com.evanv.taskapp.ui.additem.recur.RecurInput;
import com.evanv.taskapp.ui.additem.recur.DatePickerFragment;
import com.google.android.material.chip.Chip;

import org.threeten.bp.LocalDate;

import java.util.ArrayList;
import java.util.List;

/**
 * The fragment that handles data entry for new tasks
 *
 * @author Evan Voogd
 */
public class TaskEntry extends ItemEntry {
    private long mDueDate;       // Holds the user selected due date
    private long mProject;       // Holds the ID of the user selected project
    private List<Long> mLabels;      // Array of labels added to this task.
    private List<Long> mParents; // Array of selected parents
    private long mID = -1;       // ID of the edited task (or -1 if adding a task)

    private EditText mNameET;
    private TextView mECDLabel;
    private TextView mDDLabel;
    private TextView mProjectLabel;
    private TextView mLabelsLabel;
    private EditText mTtcET;
    private TextView mParentsLabel;
    private SeekBar mPrioritySeekbar;


    /**
     * Required empty public constructor, creates new TaskEntry fragment
     */
    public TaskEntry() {
        // Required empty public constructor
    }

    /**
     * Other than default Android behavior, sets the currentParents field to -1 to signify no
     * prerequisite tasks (the default)
     * @param savedInstanceState not used currently
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogStyle);

        // null signifies that no parents are added.
        mParents = null;

        mLabels = new ArrayList<>();
    }


    /**
     * Initializes important views. Most importantly it defines the dialog that shows up when
     * the "add parents" button is clicked.
     *
     * @param inflater not used other than default behavior
     * @param container contains the views we need
     * @param savedInstanceState not used
     * @return the layout of the fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_entry, container, false);

        // Add the default recurrence interval (none)
        mRecur = new Bundle();
        mRecur.putString(RecurInput.EXTRA_TYPE, NoRecurFragment.EXTRA_VAL_TYPE);

        mNameET = view.findViewById(R.id.editTextTaskName);
        mECDLabel = view.findViewById(R.id.startDateLabel);
        mDDLabel = view.findViewById(R.id.endDateLabel);
        mProjectLabel = view.findViewById(R.id.projectsLabel);
        mLabelsLabel = view.findViewById(R.id.labelsLabel);
        mTtcET = view.findViewById(R.id.editTextTTC);
        mParentsLabel = view.findViewById(R.id.parentsLabel);
        mPrioritySeekbar = view.findViewById(R.id.seekBar);
        Button mRecurButton = view.findViewById(R.id.recurButton);

        // Sets the onClick behavior to the button to creating a dialog asking what parents the user
        // wants to give the new task
        mParentsLabel.setOnClickListener(new AddParentsListener());

        // Make starting text bold
        setText("None Chosen", mECDLabel, getString(R.string.early_date_format));
        setText("None Chosen", mDDLabel, getString(R.string.due_date_format));
        setText("None Chosen", mProjectLabel, getString(R.string.project_replace));
        setText("None", mLabelsLabel, getString(R.string.labels_format));
        setText("None", mParentsLabel, getString(R.string.parent_tasks_format));

        mEarlyDate = 0;
        mDueDate = 0;
        mProject = 0;

        // Set up the Early Date picker UI
        EditText fakeEcdEt = new EditText(getContext());
        fakeEcdEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Do Nothing
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                LocalDate toDisplay = LocalDate.from(Task.dateFormat.parse(charSequence.toString()));
                mEarlyDate = toDisplay.toEpochDay();
                setText(Task.dateFormat.format(toDisplay), mECDLabel,
                        getString(R.string.early_date_format));
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Do Nothing
            }
        });
        mECDLabel.setOnClickListener(view1 -> {
            // Set the max date so the early date can't be set as later than the due date
            LocalDate maxDate = (mDueDate == 0) ? null : LocalDate.ofEpochDay(mDueDate);

            LocalDate defaultDate = mEarlyDate == 0 ? LocalDate.now() :
                    LocalDate.ofEpochDay(mEarlyDate);

            // Generate and show the DatePicker
            DialogFragment newFragment = new DatePickerFragment(fakeEcdEt, getString(R.string.ecd),
                    LocalDate.now(), maxDate, defaultDate, 0, 0, false);
            newFragment.show(getParentFragmentManager(), "datePicker");
        });

        // Set up the End Date picker UI
        EditText fakeDdET = new EditText(getContext());
        fakeDdET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Do Nothing
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                LocalDate toDisplay = LocalDate.from(Task.dateFormat.parse(charSequence.toString()));
                mDueDate = toDisplay.toEpochDay();
                setText(Task.dateFormat.format(toDisplay), mDDLabel,
                        getString(R.string.due_date_format));
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Do Nothing
            }
        });
        mDDLabel.setOnClickListener(view1 -> {
            // Set the max date so the early date can't be set as later than the due date
            LocalDate minDate = (mEarlyDate == 0) ? LocalDate.now() :
                    LocalDate.ofEpochDay(mEarlyDate);

            LocalDate defaultDay = mDueDate == 0 ? minDate : LocalDate.ofEpochDay(mDueDate);

            // Generate and show the DatePicker
            DialogFragment newFragment = new DatePickerFragment(fakeDdET, getString(R.string.due_date),
                    minDate, null, defaultDay, 0, 0, false);
            newFragment.show(getParentFragmentManager(), "datePicker");
        });

        mRecurButton.setOnClickListener(v -> intentRecur());

        // Add the AddLabelsListener
        mLabelsLabel.setOnClickListener(new AddLabelsListener());

        // Add the PickProjectListener
        mProjectLabel.setOnClickListener(new PickProjectListener());

        // Load data about task onto screen
        if (mID != -1) {
            mNameET.setText(LogicSubsystem.getInstance().getTaskName(mID));
            LocalDate toDisplay = LogicSubsystem.getInstance().getTaskECD(mID);
            mEarlyDate = toDisplay.toEpochDay();
            setText(Task.dateFormat.format(toDisplay), mECDLabel,
                    getString(R.string.early_date_format));
            toDisplay = LogicSubsystem.getInstance().getTaskDD(mID);
            mDueDate = toDisplay.toEpochDay();
            setText(Task.dateFormat.format(toDisplay), mDDLabel, getString(R.string.due_date_format));
            mProject = LogicSubsystem.getInstance().getTaskProject(mID);
            setText(LogicSubsystem.getInstance().getProjectName(mProject, requireContext()),
                    mProjectLabel, getString(R.string.project_replace));
            mLabels = LogicSubsystem.getInstance().getTaskLabels(mID);
            setText(Integer.toString(mLabels.size()), mLabelsLabel, getString(R.string.label_format));
            mTtcET.setText(String.format(getResources().getConfiguration().locale, "%d",
                    LogicSubsystem.getInstance().getTaskTTC(mID)));
            mParents = LogicSubsystem.getInstance().getTaskParents(mID);
            setText(Integer.toString(mParents.size()), mParentsLabel,
                    getString(R.string.parent_tasks_format));
            mPrioritySeekbar.setProgress(LogicSubsystem.getInstance().getTaskPriority(mID));
        }

        if (mListener != null) {
            view.findViewById(R.id.submitButton).setOnClickListener(mListener);
        }

        view.findViewById(R.id.helpButton).setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.add_task_url)));
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
     * Builds a task based on the inputted information.
     *
     * @return true if item is successfully added, false otherwise
     */
    @SuppressWarnings("unused")
    public boolean addItem() {
        // Get the user's input
        String taskName = mNameET.getText().toString();
        LocalDate early = LocalDate.ofEpochDay(mEarlyDate);
        LocalDate due = LocalDate.ofEpochDay(mDueDate);
        String ttc = mTtcET.getText().toString();
        int priority = mPrioritySeekbar.getProgress();

        // Check if eventName is valid
        if (taskName.length() == 0) {
            Toast.makeText(getActivity(), R.string.name_empty_task,
                    Toast.LENGTH_LONG).show();
            return false;
        }

        // Check if ECD is valid
        if (mEarlyDate == 0) {
            Toast.makeText(getActivity(),
                    R.string.ecd_empty_task,
                    Toast.LENGTH_LONG).show();
            return false;
        }

        // Ensure the user entered a dueDate
        if (mDueDate == 0) {
            Toast.makeText(getActivity(),
                    R.string.due_empty_task,
                    Toast.LENGTH_LONG).show();
            return false;
        }

        // Check if length is valid
        int timeToComplete;
        if (ttc.length() == 0) {
            Toast.makeText(getActivity(), R.string.ttc_error_empty_task, Toast.LENGTH_LONG).show();
            return false;
        }
        // Check if user entered length is a number.
        else {
            try {
                timeToComplete = Integer.parseInt(ttc);
            }
            catch (Exception e) {
                Toast.makeText(getActivity(),
                        R.string.ttc_format_event,
                        Toast.LENGTH_LONG).show();
                return false;
            }
        }

        // Add the specified task into the LogicSubsystem
        Long[] arr = new Long[mLabels.size()];
        LogicSubsystem.getInstance().editTask(taskName, early, due, mRecur, timeToComplete, mProject,
                mLabels.toArray(arr), mParents, priority, mID, getContext());

        return true;
    }

    /**
     * Listener for the "add prerequisites" button. Extracted due to it's large size.
     */
    private class AddParentsListener implements View.OnClickListener {
        /**
         * Opens a dialog allowing the user to set parents for the task
         *
         * @param view the button
         */
        @Override
        public void onClick(View view) {
            // Converts the bundled arraylist of task names to a String[] that can be used by
            // the alert dialog
            ArrayList<String> taskNames = LogicSubsystem.getInstance().getTaskNames(getContext());

            showPickerDialog(convertListToArray(taskNames));
        }

        /**
         * Builds and shows a picker dialog based on a list of task names.
         *
         * @param taskNamesArr List of names of tasks
         */
        private void showPickerDialog(String[] taskNamesArr) {
            ArrayList<Integer> selectedItems = new ArrayList<>();
            // Define the dialog used to pick parent tasks
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.parents_button)
                    .setMultiChoiceItems(taskNamesArr, null,
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
                            ((dialogInterface, i) -> {
                                // Edit the parents list
                                mParents = new ArrayList<>();
                                for (int index : selectedItems) {
                                    mParents.add(LogicSubsystem.getInstance().getTaskID(index));
                                }
                                setText(Integer.toString(mParents.size()), mParentsLabel,
                                        getString(R.string.parent_tasks_format));
                            }));

            builder.create();
            builder.show();
        }
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
            showPickerDialog();
        }

        /**
         * Builds and shows a picker dialog based on a list of label names.
         */
        private void showPickerDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.pick_labels)
                    .setAdapter(new LabelChipAdapter<>(requireContext(), R.layout.chip_item),
                            (dialogInterface, i) -> {
                                // Do Nothing
                            })
                            .setPositiveButton(R.string.ok,
                            ((dialogInterface, unused) -> setText(Integer.toString(mLabels.size()), mLabelsLabel,
                                    getString(R.string.labels_format)))).setNeutralButton(getString(R.string.add_label),
                            ((dialogInterface, i) -> {
                                // Open Label Entry dialog
                                LabelEntry labelEntry = new LabelEntry();
                                labelEntry.show(getParentFragmentManager(), "LABEL ENTRY");
                            }));


            AlertDialog diag = builder.create();

            ListView listView = diag.getListView();
            listView.setAdapter(new LabelChipAdapter<>(requireContext(), R.layout.chip_item));

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
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.pick_project)
                    .setAdapter(new ProjectChipAdapter<>(requireContext(), R.layout.chip_item),
                            null)
                    .setPositiveButton(R.string.ok,
                            ((dialogInterface, unused) -> {
                                // Get name of selected project
                                String projectName = LogicSubsystem.getInstance()
                                        .getProjectName(mProject, requireContext());
                                String formatString = getString(R.string.project_replace);

                                setText(projectName, mProjectLabel, formatString);
                            }))
                    .setNeutralButton(getString(R.string.add_project), ((dialogInterface, i) -> {
                        // Open Project Entry dialog
                        ProjectEntry projectEntry = new ProjectEntry();
                        projectEntry.show(getParentFragmentManager(), "PROJECT ENTRY");
                    }));


            AlertDialog diag = builder.create();

            ListView listView = diag.getListView();
            listView.setAdapter(new ProjectChipAdapter<>(requireContext(), R.layout.chip_item,
                    projectNamesArr));

            builder.show();
        }
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
                LayoutInflater inflater = LayoutInflater.from(requireContext());
                convertView = inflater.inflate(R.layout.chip_item, parent, false);
            }

            Chip chip = convertView.findViewById(R.id.chip);
            long projectID = LogicSubsystem.getInstance().getProjectID(position);

            chip.setText(LogicSubsystem.getInstance().getProjectName(projectID, requireContext()));

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

            ImageView finalSelectIndicator = convertView.findViewById(R.id.selectIndicator);
            chip.setOnClickListener(v -> {
                mProject = LogicSubsystem.getInstance().getProjectID(position);
                if (mCurrentSelected != null) {
                    mCurrentSelected.setVisibility(View.INVISIBLE);
                }
                mCurrentSelected = finalSelectIndicator;
                mCurrentSelected.setVisibility(View.VISIBLE);
            });

            if (mProject == LogicSubsystem.getInstance().getProjectID(position)) {
                if (mCurrentSelected != null) {
                    mCurrentSelected.setVisibility(View.INVISIBLE);
                }
                mCurrentSelected = finalSelectIndicator;
                mCurrentSelected.setVisibility(View.VISIBLE);
            }
            else {
                finalSelectIndicator.setVisibility(View.INVISIBLE);
            }

            return convertView;
        }

        @Override
        public int getCount() {
            return LogicSubsystem.getInstance().getProjectNames().size();
        }
    }

    private class LabelChipAdapter<T> extends ArrayAdapter<T> {
        public LabelChipAdapter(Context context, int chip_item) {
            super(context, chip_item);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(requireContext());
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
}