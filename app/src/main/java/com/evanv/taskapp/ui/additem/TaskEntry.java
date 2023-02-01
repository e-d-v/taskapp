package com.evanv.taskapp.ui.additem;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.DialogFragment;

import com.evanv.taskapp.R;
import com.evanv.taskapp.logic.LogicSubsystem;
import com.evanv.taskapp.logic.Task;
import com.evanv.taskapp.ui.FilterActivity;
import com.evanv.taskapp.ui.additem.recur.NoRecurFragment;
import com.evanv.taskapp.ui.additem.recur.RecurActivity;
import com.evanv.taskapp.ui.additem.recur.RecurInput;
import com.evanv.taskapp.ui.additem.recur.DatePickerFragment;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.threeten.bp.LocalDate;
import org.threeten.bp.temporal.ChronoField;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * The fragment that handles data entry for new tasks
 *
 * @author Evan Voogd
 */
public class TaskEntry extends BottomSheetDialogFragment {
    private View mContainer;   // The ViewGroup for the activity, allows easy access to views
    private Bundle mRecur;       // Bundle containing recurrence information
    private long mEarlyDate;     // Holds the user selected early date
    private long mDueDate;       // Holds the user selected due date
    private long mProject;       // Holds the ID of the user selected project
    private long[] mLabels;      // Array of labels added to this task.
    private List<Long> mParents; // Array of selected parents
    private long mID = -1;       // ID of the edited task (or -1 if adding a task)
    private View.OnClickListener mListener; // Listener for Submit Button

    private ActivityResultLauncher<Intent> mLaunchRecur;   // Launcher for the recurrence activity
    private EditText mNameET;
    private ImageButton mECDButton;
    private TextView mECDLabel;
    private ImageButton mDDButton;
    private ImageButton mProjectButton;
    private TextView mDDLabel;
    private TextView mProjectLabel;
    private ImageButton mLabelsButton;
    private TextView mLabelsLabel;
    private EditText mTtcET;
    private ImageButton mPrereqButton;
    private TextView mParentsLabel;
    private SeekBar mPrioritySeekbar;
    private Button mRecurButton;

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

        mLaunchRecur = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleRecurInput(result.getResultCode(),
                        result.getData()));

        // null signifies that no parents are added.
        mParents = null;

        mLabels = new long[0];
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

        // Add the default recurrence interval (none)
        mRecur = new Bundle();
        mRecur.putString(RecurInput.EXTRA_TYPE, NoRecurFragment.EXTRA_VAL_TYPE);

        mNameET = view.findViewById(R.id.editTextTaskName);
        mECDButton = view.findViewById(R.id.startDateButton);
        mECDLabel = view.findViewById(R.id.startDateLabel);
        mDDButton = view.findViewById(R.id.endDateButton);
        mDDLabel = view.findViewById(R.id.endDateLabel);
        mProjectButton = view.findViewById(R.id.imageButtonProject);
        mProjectLabel = view.findViewById(R.id.projectsLabel);
        mLabelsButton = view.findViewById(R.id.imageButtonLabels);
        mLabelsLabel = view.findViewById(R.id.labelsLabel);
        mTtcET = view.findViewById(R.id.editTextTTC);
        mPrereqButton = view.findViewById(R.id.buttonAddParents);
        mParentsLabel = view.findViewById(R.id.parentsLabel);
        mPrioritySeekbar = view.findViewById(R.id.seekBar);
        mRecurButton = view.findViewById(R.id.recurButton);

        // Sets the onClick behavior to the button to creating a dialog asking what parents the user
        // wants to give the new task
        mPrereqButton.setOnClickListener(new AddParentsListener());

        // Make starting text bold
        String startString = getString(R.string.early_date_default);
        SpannableString startDateText = new SpannableString(startString);
        startDateText.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                0, startString.indexOf('\n'), 0);
        mECDLabel.setText(startDateText);

        String endString = getString(R.string.due_date_default);
        SpannableString endDateText = new SpannableString(endString);
        endDateText.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                0, endString.indexOf('\n'), 0);
        mDDLabel.setText(endDateText);

        String projectString = getString(R.string.project_label);
        SpannableString projectText = new SpannableString(projectString);
        projectText.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                0, projectString.indexOf('\n'), 0);
        mProjectLabel.setText(projectText);

        String labelsString = getString(R.string.label_label);
        SpannableString labelsText = new SpannableString(labelsString);
        labelsText.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                0, labelsString.indexOf('\n'), 0);
        mLabelsLabel.setText(labelsText);

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
        mECDButton.setOnClickListener(view1 -> {
            // Set the max date so the early date can't be set as later than the due date
            LocalDate maxDate = (mDueDate == 0) ? null : LocalDate.ofEpochDay(mDueDate);

            // Generate and show the DatePicker
            DialogFragment newFragment = new DatePickerFragment(fakeEcdEt, getString(R.string.ecd),
                    LocalDate.now(), maxDate, false);
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
        mDDButton.setOnClickListener(view1 -> {
            // Set the max date so the early date can't be set as later than the due date
            LocalDate minDate = (mEarlyDate == 0) ? LocalDate.now() :
                    LocalDate.ofEpochDay(mEarlyDate);

            // Generate and show the DatePicker
            DialogFragment newFragment = new DatePickerFragment(fakeDdET, getString(R.string.due_date),
                    minDate, null, false);
            newFragment.show(getParentFragmentManager(), "datePicker");
        });

        mRecurButton.setOnClickListener(v -> intentRecur());

        // Add the AddLabelsListener
        mLabelsButton.setOnClickListener(new AddLabelsListener());

        // Add the PickProjectListener
        mProjectButton.setOnClickListener(new PickProjectListener());

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
            setText(LogicSubsystem.getInstance().getProjectName(mProject, getContext()), mProjectLabel,
                    getString(R.string.project_replace));
            mLabels = convertLongListToArray(LogicSubsystem.getInstance().getTaskLabels(mID));
            setText(Integer.toString(mLabels.length), mLabelsLabel, getString(R.string.label_format));
            mTtcET.setText(Integer.toString(LogicSubsystem.getInstance().getTaskTTC(mID)));
            mParents = LogicSubsystem.getInstance().getTaskParents(mID);
            setText(Integer.toString(mParents.size()), mParentsLabel,
                    getString(R.string.parent_tasks_format));
            mPrioritySeekbar.setProgress(LogicSubsystem.getInstance().getTaskPriority(mID));
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

    private void setText(String toShow, TextView element, String formatString) {

        // Make starting text bold
        SpannableString dateText = new SpannableString(String.format
                (formatString, toShow));
        dateText.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                0, formatString.indexOf('\n'), 0);

        element.setText(dateText);
    }

    /**
     * Launch a new intent to the RecurActivity, and give it the needed information
     */
    private void intentRecur() {
        // Create a new intent
        Intent intent = new Intent(getActivity(), RecurActivity.class);

        // Get the date information the user has entered
        LocalDate ecd;

        if (mEarlyDate == 0) {
            Toast.makeText(getContext(), "Please enter a start date first.", Toast.LENGTH_LONG).show();
            return;
        }

        ecd = LocalDate.ofEpochDay(mEarlyDate);

        // Get the day in month e.g. "31st"
        intent.putExtra(EventEntry.EXTRA_DAY, EventEntry.getOrdinalDayInMonth(ecd));

        // Get the ordinal day of week e.g. "3rd Monday"
        intent.putExtra(EventEntry.EXTRA_DESC, EventEntry.getOrdinalDayInWeek(requireContext(), ecd));

        // Get the month e.g. "August"
        intent.putExtra(EventEntry.EXTRA_MONTH,
                getResources().getStringArray(R.array.months)[ecd.get(ChronoField.MONTH_OF_YEAR) - 1]);

        // Get the time
        intent.putExtra(EventEntry.EXTRA_TIME, mEarlyDate);

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
        LogicSubsystem.getInstance().editTask(taskName, early, due, mRecur, timeToComplete, mProject,
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

                                setText(Integer.toString(mLabels.length), mLabelsLabel,
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
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.pick_project)
                    .setSingleChoiceItems(projectNamesArr, -1, (dialogInterface, i) ->
                            mProject = LogicSubsystem.getInstance().getProjectID(i))
                    .setPositiveButton(R.string.ok,
                            ((dialogInterface, unused) -> {
                                // Get name of selected project
                                String projectName = LogicSubsystem.getInstance()
                                        .getProjectName(mProject, getContext());
                                String formatString = getString(R.string.project_replace);

                                // Make starting text bold
                                SpannableString projectText = new SpannableString(String.format
                                        (formatString, projectName));
                                projectText.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                                        0, formatString.indexOf('\n'), 0);

                                mProjectLabel.setText(projectText);
                            }));

            builder.create();
            builder.show();
        }
    }
}