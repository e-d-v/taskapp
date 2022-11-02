package com.evanv.taskapp.ui.additem;

import static com.evanv.taskapp.logic.Task.clearDate;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.evanv.taskapp.R;
import com.evanv.taskapp.logic.Event;
import com.evanv.taskapp.logic.Task;
import com.evanv.taskapp.ui.main.MainActivity;
import com.evanv.taskapp.ui.additem.recur.DatePickerFragment;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * The fragment that handles data entry for new tasks
 *
 * @author Evan Voogd
 */
public class TaskEntry extends Fragment implements ItemEntry {
    private ViewGroup mContainer;  // The ViewGroup for the activity, allows easy access to views
    private String mCurrentParents; // The list of parents for task, returned when fab is clicked

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

        // -1 signifies that the new task has no dependent tasks, as none were entered
        mCurrentParents = "-1";
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
        mContainer = container;

        View view = inflater.inflate(R.layout.fragment_task_entry, container, false);

        // Sets the onClick behavior to the button to creating a dialog asking what parents the user
        // wants to give the new task
        ((Button) view.findViewById(R.id.buttonAddParents)).setOnClickListener
                (new AddParentsListener());

        // Get the EditTexts for dates`
        EditText editTextECD = view.findViewById(R.id.editTextECD);
        EditText editTextDueDate = view.findViewById(R.id.editTextDueDate);

        // Set the onClickListener so clicking the EditTexts opens a Date Picker dialog instead of
        // a keyboard
        editTextECD.setOnClickListener(view1 -> {
            // Set the max date so the early date can't be set as later than the due date
            Date maxDate = null;
            if (!editTextDueDate.getText().toString().equals("")) {
                try {
                    maxDate = Task.dateFormat.parse(editTextDueDate.getText().toString());
                } catch (ParseException e) {
                    Log.e(this.getTag(), e.getMessage());
                }
            }

            // Generate and show the DatePicker
            DialogFragment newFragment = new DatePickerFragment(editTextECD, getString(R.string.ecd),
                    new Date(), maxDate, false);
            newFragment.show(getParentFragmentManager(), "datePicker");
        });
        editTextDueDate.setOnClickListener(view1 -> {
            // Set the min date so the due date can't be before the early date.
            Date minDate = new Date();
            if (!editTextECD.getText().toString().equals("")) {
                try {
                    minDate = Task.dateFormat.parse(editTextECD.getText().toString());
                } catch (ParseException e) {
                    Log.e(this.getTag(), e.getMessage());
                }
            }

            // Generate and show the DatePicker
            DialogFragment newFragment = new DatePickerFragment(editTextDueDate,
                    getString(R.string.due_date), minDate, null, false);
            newFragment.show(getParentFragmentManager(), "datePicker");
        });

        // Initialize the information buttons to help the user understand the fields.
        ((ImageButton) view.findViewById(R.id.ecdInfoButton)).setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.task_ecd_info);
            builder.setTitle(R.string.ecd);
            builder.show();
        });

        ((ImageButton) view.findViewById(R.id.ddInfoButton)).setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.task_dd_info);
            builder.setTitle(R.string.due_date);
            builder.show();
        });

        ((ImageButton) view.findViewById(R.id.ttcInfoButton)).setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.task_ttc_info);
            builder.setTitle(R.string.ttc);
            builder.show();
        });

        // Inflate the layout for this fragment
        return view;
    }

    /**
     * Bundles up the information entered into the fields and, if it's valid, sends it to AddItem,
     * which will in turn send it to MainActivity. If one or more fields are incorrect, it sends
     * null instead.
     *
     * @return If all fields are correct, a Bundle containing the information needed to create an
     *         event, if not, null.
     */
    @SuppressWarnings("unused")
    @Override
    public Bundle getItem() {
        // Get the user's input
        String taskName = ((EditText) mContainer.findViewById(R.id.editTextTaskName)).getText()
                .toString();
        String dueDate = ((EditText) mContainer.findViewById(R.id.editTextDueDate)).getText()
                .toString();
        String ecd = ((EditText) mContainer.findViewById(R.id.editTextECD)).getText().toString();
        String ttc = ((EditText) mContainer.findViewById(R.id.editTextTTC)).getText().toString();

        // Check if eventName is valid
        if (taskName.length() == 0) {
            Toast.makeText(getActivity(), R.string.name_empty_task,
                    Toast.LENGTH_LONG).show();
            return null;
        }

        // Check if ECD is valid
        if (ecd.length() == 0) {
            Toast.makeText(getActivity(),
                    R.string.ecd_empty_task,
                    Toast.LENGTH_LONG).show();
            return null;
        }
        else {
            try {
                Task.dateFormat.parse(ecd);
            } catch (ParseException e) {
                Toast.makeText(getActivity(), R.string.ecd_empty_task, Toast.LENGTH_LONG).show();
                return null;
            }
        }

        // Ensure the user entered a dueDate
        if (dueDate.length() == 0) {
            Toast.makeText(getActivity(),
                    R.string.due_empty_task,
                    Toast.LENGTH_LONG).show();
            return null;
        }
        // Ensure the user entered a valid due date. (Probably) not necessary as the DatePicker
        // should handle this, but kept just in case.
        else {
            try {
                Task.dateFormat.parse(dueDate);
            } catch (ParseException e) {
                Toast.makeText(getActivity(), R.string.due_empty_task, Toast.LENGTH_LONG).show();
                return null;
            }
        }

        // Check if length is valid
        if (ttc.length() == 0) {
            Toast.makeText(getActivity(), R.string.ttc_error_empty_task, Toast.LENGTH_LONG).show();
            return null;
        }
        // Check if user entered length is a number.
        else {
            try {
                Integer.parseInt(ttc);
            }
            catch (Exception e) {
                Toast.makeText(getActivity(),
                        R.string.ttc_format_event,
                        Toast.LENGTH_LONG).show();
                return null;
            }
        }

        // Build a Bundle with all the required fields
        Bundle toReturn = new Bundle();
        toReturn.putString(AddItem.EXTRA_TYPE, AddItem.EXTRA_VAL_TASK);
        toReturn.putString(AddItem.EXTRA_NAME, taskName);
        toReturn.putString(AddItem.EXTRA_ECD, ecd);
        toReturn.putString(AddItem.EXTRA_DUE, dueDate);
        toReturn.putString(AddItem.EXTRA_TTC, ttc);
        toReturn.putString(AddItem.EXTRA_PARENTS, mCurrentParents);

        return toReturn;
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
            ArrayList<String> taskNames = requireActivity().getIntent()
                    .getStringArrayListExtra(MainActivity.EXTRA_TASKS);
            String[] taskNamesArr = new String[taskNames.size()];
            Object[] taskNamesObjs = taskNames.toArray();
            for (int i = 0; i < taskNames.size(); i++) {
                taskNamesArr[i] = (String) taskNamesObjs[i];
            }

            showPickerDialog(taskNamesArr);
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
                                    selectedItems.remove(index);
                                }
                            })).setPositiveButton(R.string.ok,
                            ((dialogInterface, i) -> {
                                // Build a string that has the format n_1, n_2, ..., n_N,
                                // where n_x is an index into the task array of a Task to
                                // be added as a parent for the new Task
                                StringBuilder sb = new StringBuilder();
                                for (int index : selectedItems) {
                                    sb.append(index);
                                    sb.append(",");
                                }
                                mCurrentParents = (selectedItems.size() != 0) ? sb.toString()
                                        : "-1";

                            }));

            builder.create();
            builder.show();
        }
    }
}