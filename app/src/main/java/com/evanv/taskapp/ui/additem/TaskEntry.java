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
        Button button = view.findViewById(R.id.buttonAddParents);
        button.setOnClickListener(new View.OnClickListener() {
            /**
             * Opens a dialog allowing the user to set parents for the task
             *
             * @param view the button
             */
            @Override
            public void onClick(View view) {
                // Converts the bundled arraylist of task names to a String[] that can be used by
                // the alert dialog
                ArrayList<Integer> selectedItems = new ArrayList<>();
                ArrayList<String> taskNames = requireActivity().getIntent()
                        .getStringArrayListExtra(MainActivity.EXTRA_TASKS);
                String[] taskNamesArr = new String[taskNames.size()];
                Object[] taskNamesObjs = taskNames.toArray();
                for (int i = 0; i < taskNames.size(); i++) {
                    taskNamesArr[i] = (String) taskNamesObjs[i];
                }

                // Define the dialog used to pick parent tasks
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.parents_button)
                        .setMultiChoiceItems(taskNamesArr, null,
                                new DialogInterface.OnMultiChoiceClickListener() {

                                    /**
                                     * Adds the selected task to the parent list, or removes it if
                                     * the task was unselected
                                     *
                                     * @param dialogInterface not used
                                     * @param index the index into the tasks ArrayList of the parent
                                     * @param isChecked true if checked, false if unchecked
                                     */
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int index,
                                                        boolean isChecked) {

                                        // If checked, add to list of Tasks to be added as parents
                                        if (isChecked) {
                                            selectedItems.add(index);
                                        }
                                        // If unchecked, remove form list of Tasks to be added as
                                        // parents
                                        else if (selectedItems.contains(index)) {
                                            selectedItems.remove(index);
                                        }
                                    }
                                }).setPositiveButton(R.string.ok,
                                    new DialogInterface.OnClickListener() {

                                    /**
                                     * When "ok" is clicked, change currentParents to reflect the
                                     * selected tasks
                                     *
                                     * @param dialogInterface not used
                                     * @param i not used
                                     */
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
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

                                     }
                                });

                builder.create();
                builder.show();
            }
        });

        EditText editTextECD = view.findViewById(R.id.editTextECD);
        EditText editTextDueDate = view.findViewById(R.id.editTextDueDate);

        editTextECD.setOnClickListener(view1 -> {
            Date maxDate = null;
            if (!editTextDueDate.getText().toString().equals("")) {
                try {
                    maxDate = Task.dateFormat.parse(editTextDueDate.getText().toString());
                } catch (ParseException e) {
                    Log.e(this.getTag(), e.getMessage());
                }
            }

            DialogFragment newFragment = new DatePickerFragment(editTextECD, getString(R.string.ecd),
                    new Date(), maxDate, false);
            newFragment.show(getParentFragmentManager(), "datePicker");
        });
        editTextDueDate.setOnClickListener(view1 -> {
            Date minDate = new Date();
            if (!editTextECD.getText().toString().equals("")) {
                try {
                    minDate = Task.dateFormat.parse(editTextECD.getText().toString());
                } catch (ParseException e) {
                    Log.e(this.getTag(), e.getMessage());
                }
            }

            DialogFragment newFragment = new DatePickerFragment(editTextDueDate,
                    getString(R.string.due_date), minDate, null, false);
            newFragment.show(getParentFragmentManager(), "datePicker");
        });

        // Initialize the information buttons to help the user understand the fields.
        ImageButton infoECD = view.findViewById(R.id.ecdInfoButton);
        ImageButton infoDD = view.findViewById(R.id.ddInfoButton);
        ImageButton infoTTC = view.findViewById(R.id.ttcInfoButton);
        infoECD.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.task_ecd_info);
            builder.setTitle(R.string.ecd);
            builder.show();
        });
        infoDD.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.task_dd_info);
            builder.setTitle(R.string.due_date);
            builder.show();
        });
        infoTTC.setOnClickListener(v -> {
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
        // Gets all the required EditTexts containing user input
        EditText editTextTaskName = mContainer.findViewById(R.id.editTextTaskName);
        EditText editTextECD = mContainer.findViewById(R.id.editTextECD);
        EditText editTextDueDate = mContainer.findViewById(R.id.editTextDueDate);
        EditText editTextTTC = mContainer.findViewById(R.id.editTextTTC);

        // Get the user's input
        String taskName = editTextTaskName.getText().toString();
        String ecd = editTextECD.getText().toString();
        String dueDate = editTextDueDate.getText().toString();
        String ttc = editTextTTC.getText().toString();

        boolean flag = false; // Allows us to Toast multiple errors at once.

        // Check if eventName is valid
        if (taskName.length() == 0) {
            Toast.makeText(getActivity(), R.string.name_empty_task,
                    Toast.LENGTH_LONG).show();
            flag = true;
        }
        // Check if ECD is valid
        Date earlyDate = null; // Allows us to check if due date is after early date
        if (ecd.length() == 0) {
            Toast.makeText(getActivity(),
                    R.string.ecd_empty_task,
                    Toast.LENGTH_LONG).show();
            flag = true;
        }
        else {
            boolean ecdFlag = false; // true if there is an issue with ecd input

            // Check if ecd follows format MM/DD/YY HH:MM AM/PM
            String[] dateTokens = ecd.split("/");

            if (dateTokens.length == 3) {
                // Check if everything that's supposed to be a number is an integer
                try {
                    int month = Integer.parseInt(dateTokens[0]);
                    int day = Integer.parseInt(dateTokens[1]);
                    int year = Integer.parseInt(dateTokens[2]);

                    // Make sure input makes sense
                    if (month > 12 || month < 1 || day > 31 || day < 1 || year < 0 ) {
                        ecdFlag = true;
                    }

                    // Make sure we're not scheduling an event for before today.
                    Date rightNow = clearDate(new Date());

                    Calendar userCal = Calendar.getInstance();
                    userCal.set(2000+year, month - 1, day);
                    earlyDate = clearDate(userCal.getTime());

                    if (earlyDate.before(rightNow)) {
                        Toast.makeText(getActivity(),
                                R.string.ecd_early_task,
                                Toast.LENGTH_LONG).show();
                        flag = true;
                    }

                }
                catch (Exception e) {
                    ecdFlag = true;
                }
            }
            else {
                ecdFlag = true;
            }

            if (ecdFlag) {
                Toast.makeText(getActivity(),
                        R.string.date_format_task,
                        Toast.LENGTH_LONG).show();
                flag = true;
            }
        }
        if (dueDate.length() == 0) {
            Toast.makeText(getActivity(),
                    R.string.due_empty_task,
                    Toast.LENGTH_LONG).show();
            flag = true;
        }
        else {
            boolean ddFlag= false; // true if there is an issue with ecd input

            // Check if Due Date follows format mm/dd/yy
            String[] dateTokens = dueDate.split("/");

            if (dateTokens.length == 3) {
                // Check if everything that's supposed to be a number is an integer
                try {
                    int month = Integer.parseInt(dateTokens[0]);
                    int day = Integer.parseInt(dateTokens[1]);
                    int year = Integer.parseInt(dateTokens[2]);

                    // Make sure input makes sense
                    if (month > 12 || month < 1 || day > 31 || day < 1 || year < 0 ) {
                        ddFlag = true;
                    }

                    // Make sure we're not scheduling an event for before today.
                    Date rightNow = clearDate(new Date());

                    Calendar userCal = Calendar.getInstance();
                    userCal.set(2000+year, month - 1, day);
                    Date dueDateTime = clearDate(userCal.getTime());

                    if (dueDateTime.before(rightNow)) {
                        Toast.makeText(getActivity(),
                                R.string.ecd_early_task,
                                Toast.LENGTH_LONG).show();
                        flag = true;
                    }
                    else if (earlyDate != null && dueDateTime.before(earlyDate)) {
                        Toast.makeText(getActivity(),
                                R.string.due_before_task,
                                Toast.LENGTH_LONG).show();
                        flag = true;
                    }
                }
                catch (Exception e) {
                    ddFlag = true;
                }
            }
            else {
                ddFlag = true;
            }

            if (ddFlag) {
                Toast.makeText(getActivity(),
                        R.string.date_format_task,
                        Toast.LENGTH_LONG).show();
                flag = true;
            }
        }
        // Check if length is valid
        if (ttc.length() == 0) {
            Toast.makeText(getActivity(), R.string.ttc_error_empty_task, Toast.LENGTH_LONG).show();
            flag = true;
        }
        else {
            try {
                Integer.parseInt(ttc);
            }
            catch (Exception e) {
                Toast.makeText(getActivity(),
                        R.string.ttc_format_event,
                        Toast.LENGTH_LONG).show();
                flag = true;
            }
        }

        // If any required views are empty, return null to signify invalid input
        if (flag) {
            return null;
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

}