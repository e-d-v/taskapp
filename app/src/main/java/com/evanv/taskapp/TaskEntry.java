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
 * A simple {@link Fragment} subclass.
 * Use the {@link TaskEntry#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TaskEntry extends Fragment implements ItemEntry {

    private ViewGroup mContainer;
    private String currentParents;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public TaskEntry() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TaskEntry.
     */
    // TODO: Rename and change types and number of parameters
    public static TaskEntry newInstance(String param1, String param2) {
        TaskEntry fragment = new TaskEntry();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        currentParents = "-1";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mContainer = container;

        View view = inflater.inflate(R.layout.fragment_task_entry, container, false);
        Button button = (Button) view.findViewById(R.id.buttonAddParents);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<Integer> selectedItems = new ArrayList<>();
                ArrayList<String> taskNames = getActivity().getIntent()
                        .getStringArrayListExtra(MainActivity.EXTRA_TASKS);

                String[] taskNamesArr = new String[taskNames.size()];
                Object[] taskNamesObjs = taskNames.toArray();

                for (int i = 0; i < taskNames.size(); i++) {
                    taskNamesArr[i] = (String) taskNamesObjs[i];
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.parents_button)
                        .setMultiChoiceItems(taskNamesArr, null,
                                new DialogInterface.OnMultiChoiceClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int index, boolean isChecked) {
                                        if (isChecked) {
                                            selectedItems.add(index);
                                        }
                                        else if (selectedItems.contains(index)) {
                                            selectedItems.remove(index);
                                        }
                                    }
                                }).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
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

    @Override
    public Bundle getItem() {
        EditText editTextTaskName = mContainer.findViewById(R.id.editTextTaskName);
        EditText editTextECD = mContainer.findViewById(R.id.editTextECD);
        EditText editTextDueDate = mContainer.findViewById(R.id.editTextDueDate);
        EditText editTextTTC = mContainer.findViewById(R.id.editTextTTC);

        String taskName = editTextTaskName.getText().toString();
        String ecd = editTextECD.getText().toString();
        String dueDate = editTextDueDate.getText().toString();
        String ttc = editTextTTC.getText().toString();

        if (taskName.equals("") || ecd.equals("") || dueDate.equals("") || ttc.equals("")) {
            return null;
        }

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