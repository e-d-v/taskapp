package com.evanv.taskapp.ui.additem;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.DialogFragment;

import com.evanv.taskapp.R;
import com.evanv.taskapp.logic.LogicSubsystem;
import com.evanv.taskapp.logic.Task;
import com.evanv.taskapp.ui.additem.recur.NoRecurFragment;
import com.evanv.taskapp.ui.additem.recur.RecurActivity;
import com.evanv.taskapp.ui.additem.recur.RecurInput;
import com.evanv.taskapp.ui.additem.recur.DatePickerFragment;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.threeten.bp.LocalDate;
import org.threeten.bp.temporal.ChronoField;

import java.util.ArrayList;
import java.util.List;

/**
 * The fragment that handles data entry for new tasks
 *
 * @author Evan Voogd
 */
public class TaskEntry extends BottomSheetDialogFragment {
    private View mContainer;   // The ViewGroup for the activity, allows easy access to views
    private EditText mEditTextECD;  // The EditText for the earliest completion date
    private SeekBar mSeekBar;       // The Priority SeekBar

    private ArrayAdapter<String> mAdapter; // Adapter for the Project Spinner
    private Spinner mProjectSpinner;       // The project spinner itself.

    private Bundle mRecur;       // Bundle containing recurrence information
    private long[] mLabels;      // Array of labels added to this task.
    private List<Long> mParents; // Array of selected parents
    private long mID = -1;       // ID of the edited task (or -1 if adding a task)
    private View.OnClickListener mListener; // Listener for Submit Button

    private ActivityResultLauncher<Intent> mLaunchRecur;   // Launcher for the recurrence activity
    private ActivityResultLauncher<Intent> mLaunchProject; // Launcher for the project activity

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

        mLaunchRecur = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleRecurInput(result.getResultCode(),
                        result.getData()));
        mLaunchProject = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> updateProjectSpinner(result.getResultCode(),
                        result.getData()));

        // null signifies that no parents are added.
        mParents = null;

        mLabels = new long[0];
    }

    /**
     * Function that is called when result is received from project input activity.
     *
     * @param resultCode is Activity.RESULT_OK if a project was successfully added.
     * @param data Ignored
     */
    private void updateProjectSpinner(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            // Update the project spinner
            mAdapter.clear();
            mAdapter.add(getString(R.string.project_spinner_default));
            mAdapter.addAll(LogicSubsystem.getInstance().getProjectNames());

            // Select the most recently added item.
            mProjectSpinner.setSelection(mAdapter.getCount() - 1);
        }
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
        mContainer = view;

        // Sets the onClick behavior to the button to creating a dialog asking what parents the user
        // wants to give the new task
        view.findViewById(R.id.buttonAddParents).setOnClickListener
                (new AddParentsListener());

        view.findViewById(R.id.buttonAddLabels).setOnClickListener
                (new AddLabelsListener());

        // Get the EditTexts for dates`
        mEditTextECD = view.findViewById(R.id.editTextECD);
        EditText editTextDueDate = view.findViewById(R.id.editTextDueDate);

        // Add click handler to button
        Button button = view.findViewById(R.id.recurButton);
        button.setOnClickListener(x -> intentRecur());

        // Get the priority seek bar.
        mSeekBar = view.findViewById(R.id.seekBar);

        // Set options in the project spinner
        ArrayList<String> projects = new ArrayList<>();
        projects.add(getString(R.string.project_spinner_default));
        projects.addAll(LogicSubsystem.getInstance().getProjectNames());
        mProjectSpinner = view.findViewById(R.id.projectSpinner);
        mAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, projects);
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mProjectSpinner.setAdapter(mAdapter);

        // Add the default recurrence interval (none)
        mRecur = new Bundle();
        mRecur.putString(RecurInput.EXTRA_TYPE, NoRecurFragment.EXTRA_VAL_TYPE);

        // Set the onClickListener so clicking the EditTexts opens a Date Picker dialog instead of
        // a keyboard
        mEditTextECD.setOnClickListener(view1 -> {
            // Set the max date so the early date can't be set as later than the due date
            LocalDate maxDate = null;
            if (!editTextDueDate.getText().toString().equals("")) {
                maxDate = LocalDate.from(Task.dateFormat.parse(editTextDueDate.getText().toString()));
            }

            // Generate and show the DatePicker
            DialogFragment newFragment = new DatePickerFragment(mEditTextECD, getString(R.string.ecd),
                    LocalDate.now(), maxDate, false);
            newFragment.show(getParentFragmentManager(), "datePicker");
        });
        editTextDueDate.setOnClickListener(view1 -> {
            // Set the min date so the due date can't be before the early date.
            LocalDate minDate = LocalDate.now();
            if (!mEditTextECD.getText().toString().equals("")) {
                minDate = LocalDate.from(Task.dateFormat.parse(mEditTextECD.getText().toString()));
            }

            // Generate and show the DatePicker
            DialogFragment newFragment = new DatePickerFragment(editTextDueDate,
                    getString(R.string.due_date), minDate, null, false);
            newFragment.show(getParentFragmentManager(), "datePicker");
        });

        // Initialize the information buttons to help the user understand the fields.
        view.findViewById(R.id.ecdInfoButton).setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.task_ecd_info);
            builder.setTitle(R.string.ecd);
            builder.show();
        });

        view.findViewById(R.id.ddInfoButton).setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.task_dd_info);
            builder.setTitle(R.string.due_date);
            builder.show();
        });

        view.findViewById(R.id.ttcInfoButton).setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.task_ttc_info);
            builder.setTitle(R.string.ttc);
            builder.show();
        });

        view.findViewById(R.id.priorityInfoButton).setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.priority_info);
            builder.setTitle(R.string.priority);
            builder.show();
        });

        // Set up the add project and add label buttons
        view.findViewById(R.id.addProject).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ProjectEntry.class);
            mLaunchProject.launch(intent);
        });
        view.findViewById(R.id.addLabel).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LabelEntry.class);
            startActivity(intent);
        });

        // Load data about task onto screen
        if (mID != -1) {
            // Set the task name
            EditText etName = view.findViewById(R.id.editTextTaskName);
            etName.setText(LogicSubsystem.getInstance().getTaskName(mID));

            // Set the task ECD
            mEditTextECD.setText(Task.dateFormat.format(LogicSubsystem.getInstance().getTaskECD(mID)));

            // Set the task Due Date
            editTextDueDate.setText(Task.dateFormat.format(LogicSubsystem.getInstance().getTaskDD(mID)));

            // Set the task TTC
            EditText etTTC = view.findViewById(R.id.editTextTTC);
            etTTC.setText(Integer.toString(LogicSubsystem.getInstance().getTaskTTC(mID)));

            // Set the task Priority
            mSeekBar.setProgress(LogicSubsystem.getInstance().getTaskPriority(mID));

            // Set the task Project
            long projectID = LogicSubsystem.getInstance().getTaskProject(mID);
            mProjectSpinner.setSelection(LogicSubsystem.getInstance().getProjectIndex(projectID) + 1);

            // Set the task Labels
            mLabels = convertLongListToArray(LogicSubsystem.getInstance().getTaskLabels(mID));

            // Set the task Parents
            mParents = LogicSubsystem.getInstance().getTaskParents(mID);
        }

        if (mListener != null) {
            view.findViewById(R.id.submitButton).setOnClickListener(mListener);
        }

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

    public void addSubmitListener(View.OnClickListener listener) {
        mListener = listener;
    }

    /**
     * Launch a new intent to the RecurActivity, and give it the needed information
     */
    private void intentRecur() {
        // Create a new intent
        Intent intent = new Intent(getActivity(), RecurActivity.class);

        // Get the date information the user has entered
        long time;
        LocalDate ecd;
        String ecdText = mEditTextECD.getText().toString();

        if (ecdText.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a start date first.", Toast.LENGTH_LONG).show();
            return;
        }

        ecd = LocalDate.from(Task.dateFormat.parse(ecdText));
        time = ecd.toEpochDay();

        // Get the day in month e.g. "31st"
        intent.putExtra(EventEntry.EXTRA_DAY, EventEntry.getOrdinalDayInMonth(ecd));

        // Get the ordinal day of week e.g. "3rd Monday"
        intent.putExtra(EventEntry.EXTRA_DESC, EventEntry.getOrdinalDayInWeek(requireContext(), ecd));

        // Get the month e.g. "August"
        intent.putExtra(EventEntry.EXTRA_MONTH,
                getResources().getStringArray(R.array.months)[ecd.get(ChronoField.MONTH_OF_YEAR) - 1]);

        // Get the time
        intent.putExtra(EventEntry.EXTRA_TIME, time);

        // Launch RecurActivity
        mLaunchRecur.launch(intent);
    }

    /**
     * Builds a task based on the inputted information.
     *
     * @return true if item is successfully added, false otherwise
     */
    @SuppressWarnings("unused")
    public boolean addItem() {
        // Get the user's input
        String taskName = ((EditText) mContainer.findViewById(R.id.editTextTaskName)).getText()
                .toString();
        String dueDate = ((EditText) mContainer.findViewById(R.id.editTextDueDate)).getText()
                .toString();
        String ecd = ((EditText) mContainer.findViewById(R.id.editTextECD)).getText().toString();
        String ttc = ((EditText) mContainer.findViewById(R.id.editTextTTC)).getText().toString();
        int priority = mSeekBar.getProgress();

        // Check if eventName is valid
        if (taskName.length() == 0) {
            Toast.makeText(getActivity(), R.string.name_empty_task,
                    Toast.LENGTH_LONG).show();
            return false;
        }

        // Check if ECD is valid
        LocalDate early;
        if (ecd.length() == 0) {
            Toast.makeText(getActivity(),
                    R.string.ecd_empty_task,
                    Toast.LENGTH_LONG).show();
            return false;
        }
        else {
            early = LocalDate.from(Task.dateFormat.parse(ecd));
        }

        // Ensure the user entered a dueDate
        LocalDate due;
        if (dueDate.length() == 0) {
            Toast.makeText(getActivity(),
                    R.string.due_empty_task,
                    Toast.LENGTH_LONG).show();
            return false;
        }
        // Ensure the user entered a valid due date. (Probably) not necessary as the DatePicker
        // should handle this, but kept just in case.
        else {
            due = LocalDate.from(Task.dateFormat.parse(dueDate));
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

        // Get selected project
        int project = mProjectSpinner.getSelectedItemPosition();

        // Add the specified task into the LogicSubsystem
        LogicSubsystem.getInstance().editTask(taskName, early, due, mRecur, timeToComplete, project,
                mLabels, mParents, priority, mID, getContext());

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
    private long[] convertLongListToArray(List<Long> list) {
        long[] toReturn = new long[list.size()];
        Object[] toReturnObjs = list.toArray();
        for (int i = 0; i < toReturnObjs.length; i++) {
            toReturn[i] = (long) toReturnObjs[i];
        }

        return toReturn;
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
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
                            }));

            builder.create();
            builder.show();
        }
    }
}