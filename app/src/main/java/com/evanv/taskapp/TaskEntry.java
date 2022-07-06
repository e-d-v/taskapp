package com.evanv.taskapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * The fragment that handles data entry for new tasks
 *
 * @author Evan Voogd
 */
public class TaskEntry extends Fragment implements ItemEntry {

    private ViewGroup mContainer;  // The ViewGroup for the activity, allows easy access to views
    private String currentParents; // The list of parents for the task, returned when fab is clicked

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
        currentParents = "-1";
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
        Button button = (Button) view.findViewById(R.id.buttonAddParents);
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
                ArrayList<String> taskNames = getActivity().getIntent()
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
                                }).setPositiveButton("OK",
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
                                        currentParents = sb.toString();
                                     }
                                });

                builder.create();
                builder.show();
            }
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

        // If any required fields are empty, return null
        if (taskName.equals("") || ecd.equals("") || dueDate.equals("") || ttc.equals("")) {
            return null;
        }

        // Build a Bundle with all the required fields
        Bundle toReturn = new Bundle();
        toReturn.putString(AddItem.EXTRA_TYPE, AddItem.EXTRA_VAL_TASK);
        toReturn.putString(AddItem.EXTRA_NAME, taskName);
        toReturn.putString(AddItem.EXTRA_ECD, ecd);
        toReturn.putString(AddItem.EXTRA_DUE, dueDate);
        toReturn.putString(AddItem.EXTRA_TTC, ttc);
        toReturn.putString(AddItem.EXTRA_PARENTS, currentParents);

        return toReturn;
    }
}