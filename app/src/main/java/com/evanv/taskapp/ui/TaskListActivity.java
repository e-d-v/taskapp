package com.evanv.taskapp.ui;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.widget.EditText;

import com.evanv.taskapp.R;
import com.evanv.taskapp.logic.LogicSubsystem;
import com.evanv.taskapp.ui.main.ClickListener;
import com.evanv.taskapp.ui.main.recycler.TaskItem;
import com.evanv.taskapp.ui.main.recycler.TaskItemAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Activity that displays a list of Tasks. Can be created by various screens that need to show
 * a list of tasks, such as a Filter Screen or the Projects Screen.
 *
 * @author Evan Voogd
 */
public class TaskListActivity extends AppCompatActivity implements ClickListener {
    // Key for the value of the earliest due date to show.
    public static final String EXTRA_START_DATE = "com.evanv.taskapp.ui.TaskListActivity.EXTRA_START_DATE";
    // Key for the value of the latest due date to show.
    public static final String EXTRA_END_DATE = "com.evanv.taskapp.ui.TaskListActivity.EXTRA_END_DATE";
    // Key for the value of the project to show.
    public static final String EXTRA_PROJECT = "com.evanv.taskapp.ui.TaskListActivity.EXTRA_PROJECT";
    // Key for the value of the name to show.
    public static final String EXTRA_NAME = "com.evanv.taskapp.ui.TaskListActivity.EXTRA_NAME";
    // Key for the value of the minimum TTC.
    public static final String EXTRA_MIN_TIME = "com.evanv.taskapp.ui.TaskListActivity.EXTRA_MIN_TIME";
    // Key for the value of the maximum TTC.
    public static final String EXTRA_MAX_TIME = "com.evanv.taskapp.ui.TaskListActivity.EXTRA_MAX_TIME";
    // Key for the value of only showing completable tasks.
    public static final String EXTRA_COMPLETABLE = "com.evanv.taskapp.ui.TaskListActivity.EXTRA_COMPLETABLE";
    // Key for the value of the requested labels.
    public static final String EXTRA_LABELS = "com.evanv.taskapp.ui.TaskListActivity.EXTRA_LABELS";
    // Key for the value of the priority.
    public static final String EXTRA_PRIORITY = "com.evanv.taskapp.ui.TaskListActivity.EXTRA_PRIORITY";

    private TaskItemAdapter mAdapter;           // The adapter for the recycler

    private List<Long> IDs;

    /**
     * Creates a TaskListActivity. Bundle requires information including a list of taskNames,
     * a list of projects, a list of project colors, the index of the currently timed task, and a
     * list of priorities.
     *
     * @param savedInstanceState not used
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        Date startDate = new Date(getIntent().getLongExtra(EXTRA_START_DATE, 0));
        startDate = startDate.getTime() == 0 ? null : startDate;
        Date endDate = new Date(getIntent().getLongExtra(EXTRA_END_DATE, 0));
        endDate = endDate.getTime() == 0 ? null : endDate;
        long project = getIntent().getLongExtra(EXTRA_PROJECT, 0);
        String name = getIntent().getStringExtra(EXTRA_NAME);
        int minTime = getIntent().getIntExtra(EXTRA_MIN_TIME, -1);
        int maxTime = getIntent().getIntExtra(EXTRA_MAX_TIME, -1);
        boolean completable = getIntent().getBooleanExtra(EXTRA_COMPLETABLE, false);
        List<Long> labels = convertArrayToList(getIntent().getLongArrayExtra(EXTRA_LABELS));
        int priority = getIntent().getIntExtra(EXTRA_PRIORITY, -1);

        List<TaskItem> taskItemList = LogicSubsystem.getInstance().filter(startDate, endDate,
                project, name, minTime, maxTime, completable, labels, priority, this);

        IDs = new ArrayList<>();
        for (TaskItem item : taskItemList) {
            IDs.add(item.getID());
        }

        RecyclerView recycler = findViewById(R.id.projects_recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mAdapter = new TaskItemAdapter
                (taskItemList, this, -1, null, false, this);
        recycler.setAdapter(mAdapter);
        recycler.setLayoutManager(layoutManager);
    }

    /**
     * Converts an array of Longs to a List of Longs.
     *
     * @param array The array to turn into a List
     *
     * @return A list with the same items as the array
     */
    private List<Long> convertArrayToList(long[] array) {
        List<Long> toReturn = new ArrayList<>();

        if (array == null) {
            return null;
        }

        for (long l : array) {
            toReturn.add(l);
        }

        return toReturn;
    }

    /**
     * Handles button clicks in the Task recycler.
     *
     * @param position Position in the recycler of the task chosen
     * @param day Not used
     * @param action 0: complete task, 1: delete task, 2: delete event, 3 set timer
     */
    @Override
    public void onButtonClick(int position, int day, int action) {
        long id = IDs.get(position);

        // If starting a timer, tell mLogicSubsystem.
        if (action == 3) {
            LogicSubsystem.getInstance().timer(id);

            mAdapter.mTaskItemList.set(position,
                    LogicSubsystem.getInstance().TaskItemHelper(id, position, this));
            mAdapter.notifyItemChanged(position);

            return;
        }

        boolean isTimed = (action == 0) && LogicSubsystem.getInstance().isTimed(id);
        int timerVal = -1;

        if (isTimed) {
            timerVal = LogicSubsystem.getInstance().getTimer();
        }

        List<Integer> changedDates = LogicSubsystem.getInstance().onButtonClick(id, action, this);

        mAdapter.mTaskItemList.remove(position);
        mAdapter.notifyItemRemoved(position);

        // Make sure to always update the first day so "Work Ahead" can be redisplayed.
        changedDates = changedDates == null ? new ArrayList<>() : changedDates;
        changedDates.add(0);

        if (action == 0) {
            if (isTimed) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setMessage(String.format(getString(R.string.timer_prompt), timerVal));

                // If user chooses to accept the given time, then add it to the today time and
                // continue.
                int finalTimerVal = timerVal;
                builder.setPositiveButton("OK", (dialogInterface, i) -> {
                    LogicSubsystem.getInstance().addTodayTime(finalTimerVal);

                    // As the task dependency graph has been updated, we must reoptimize it
                    LogicSubsystem.getInstance().Optimize();
                });

                // If user chooses to use a different time, show the normal time to complete
                // dialog.
                builder.setNegativeButton("Manual Time", (dialogInterface, i) -> ttcPrompt());

                builder.show();
                return;
            }

            // User did not have a timer set, so use the normal time to complete dialog.
            ttcPrompt();
        }
        else if (action == 1) {
            LogicSubsystem.getInstance().Optimize();
        }
    }

    /**
     * Used to use back press when up button is pressed.
     *
     * @param item The item that was pressed
     *
     * @return unused
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return  true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Show a prompt asking the user how much time a task took to complete, and then add that time
     * to todayTime.
     */
    private void ttcPrompt() {
        // Prompt the user to ask how long it took to complete the task, and add this time to
        // todayTime to prevent the user from being overscheduled on today's date.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.complete_dialog_message));

        builder.setTitle(R.string.complete_dialog_title);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton(R.string.complete_task, (dialogInterface, i) -> {
            LogicSubsystem.getInstance().addTodayTime(Integer.parseInt(input.getText().toString()));

            // As the task dependency graph has been updated, we must reoptimize it
            LogicSubsystem.getInstance().Optimize();
        });

        builder.show();
    }
}