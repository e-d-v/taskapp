package com.evanv.taskapp.ui;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.ui.text.android.InternalPlatformTextApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.evanv.taskapp.R;
import com.evanv.taskapp.logic.LogicSubsystem;
import com.evanv.taskapp.ui.additem.TaskEntry;
import com.evanv.taskapp.ui.main.ClickListener;
import com.evanv.taskapp.ui.main.MainActivity;
import com.evanv.taskapp.ui.main.recycler.TaskItem;
import com.evanv.taskapp.ui.main.recycler.TaskItemAdapter;

import org.threeten.bp.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import kotlin.Pair;

/**
 * Activity that displays a list of Tasks. Can be created by various screens that need to show
 * a list of tasks, such as a Filter Screen or the Projects Screen.
 *
 * @author Evan Voogd
 */
@InternalPlatformTextApi
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

    private TaskItemAdapter mAdapter; // The adapter for the recycler

    private List<Long> mIDs;          // List of IDs for each task in the task list
    private int mIndex;               // Index into the recycler to update.
    private int mPosition;            // Position in the recycler of the selected task.
    private int mDay;                 // Day of the selected task.
    private long mID;                 // ID of the currently selected task.
    private Thread mOptimizer;        // Holds optimizer thread if currently available.

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

        setSupportActionBar(findViewById(R.id.toolbar));

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        LocalDate startDate = LocalDate.ofEpochDay
                (getIntent().getLongExtra(EXTRA_START_DATE, 0));
        startDate = startDate.toEpochDay() == 0 ? null : startDate;
        LocalDate endDate = LocalDate.ofEpochDay
                (getIntent().getLongExtra(EXTRA_END_DATE, 0));
        endDate = endDate.toEpochDay() == 0 ? null : endDate;
        long project = getIntent().getLongExtra(EXTRA_PROJECT, -1);
        String name = getIntent().getStringExtra(EXTRA_NAME);
        int minTime = getIntent().getIntExtra(EXTRA_MIN_TIME, -1);
        int maxTime = getIntent().getIntExtra(EXTRA_MAX_TIME, -1);
        boolean completable = getIntent().getBooleanExtra(EXTRA_COMPLETABLE, false);
        List<Long> labels = convertArrayToList(getIntent().getLongArrayExtra(EXTRA_LABELS));
        int priority = getIntent().getIntExtra(EXTRA_PRIORITY, -1);

        List<TaskItem> taskItemList = getLogicSubsystem().filter(startDate, endDate,
                project, name, minTime, maxTime, completable, labels, priority, this);

        mIDs = new ArrayList<>();
        for (TaskItem item : taskItemList) {
            mIDs.add(item.getID());
        }

        RecyclerView recycler = findViewById(R.id.projects_recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mAdapter = new TaskItemAdapter
                (taskItemList, this, -1, null, this);
        recycler.setAdapter(mAdapter);
        recycler.setLayoutManager(layoutManager);

        mPosition = -1;
        mID = -1;
        mOptimizer = null;

    }

    /**
     * On activity result update recycler
     */
    private void onActivityResult() {
        mAdapter.mTaskItemList.set(mIndex,
                getLogicSubsystem().TaskItemHelper(mID, mIndex, this));
        mAdapter.notifyItemChanged(mIndex);

        optimize();
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
     * @param action 0 == complete, 1 == options button
     */
    @Override
    public void onButtonClick(int position, int day, int action, long id) {
        mIndex = position;
        mPosition = position;
        mID = mIDs.get(position);

        if (action == 0) {
            completeTask();
        }
    }

    /**
     * Handles a user choosing to complete a task
     */
    private void completeTask() {
        // If it is today's date, check if "Work Ahead" is displayed and then convert position/day
        convertDay();

        boolean isTimed = getLogicSubsystem().isTimed(mPosition, mDay);
        int timerVal = -1;

        if (isTimed) {
            timerVal = getLogicSubsystem().getTimer();
        }

        getLogicSubsystem().onButtonClick(mID, 0, this);
        getLogicSubsystem().getNumDays();

        if (isTimed) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(String.format(getString(R.string.timer_prompt), timerVal));

            // If user chooses to accept the given time, then add it to the today time and
            // continue.
            int finalTimerVal = timerVal;
            builder.setPositiveButton("OK", (dialogInterface, i) -> {
                getLogicSubsystem().addTodayTime(finalTimerVal);

                removeItem();
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
     * Show the context menu when a button/event's option is pressed.
     *
     * @param menu The menu to load info into
     * @param v The button that was pressed
     * @param menuInfo Not used
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (v.getId() == R.id.buttonTaskOptions) {
            getMenuInflater().inflate(R.menu.task_options, menu);
        }
    }

    /**
     * Dispatches menu button clicks to helper functions.
     *
     * @param item The menu item chosen.
     * @return true always
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.action_delete_task):
                // Show optimizing... screen
                deleteTask();
                break;
            case (R.id.action_edit_task):
                // Show optimizing... screen
                editTask();
                break;
            case (R.id.action_time_task):
                timeTask();
                break;
        }

        return true;
    }

    /**
     * Handles the user choosing to start a timer on a task.
     */
    private void timeTask() {
        // If it is today's date, check if "Work Ahead" is displayed and then convert position/day
        convertDay();

        long oldTimer = getLogicSubsystem().getTimedID();

        int index = mIDs.indexOf(mID);

        getLogicSubsystem().timer(mPosition, mDay);

        // Update the recycler at the mPosition
        mAdapter.mTaskItemList.set(index,
                getLogicSubsystem().TaskItemHelper(mID, index, this));
        mAdapter.notifyItemChanged(index);

        // Update the recycler at the oldTimer location
        if (oldTimer != -1) {
            int oldPosition = mIDs.indexOf(oldTimer);
            mAdapter.mTaskItemList.set(oldPosition,
                    getLogicSubsystem().TaskItemHelper(oldTimer, oldPosition, this));
            mAdapter.notifyItemChanged(oldPosition);
        }
    }

    /**
     * Handles the user choosing to edit a task.
     */
    private void editTask() {
        // If it is today's date, check if "Work Ahead" is displayed and then convert position/day
        convertDay();

        long editedID = getLogicSubsystem().getTaskID(mPosition, mDay);

        // Launch an edit dialog
        TaskEntry frag = new TaskEntry();
        frag.setID(editedID);
        frag.addSubmitListener(v -> {
            if (frag.addItem()) {
                frag.dismiss();
                onActivityResult();
            }
        });
        frag.show(getSupportFragmentManager(), "TASK");
    }

    /**
     * Handles the user choosing to delete a task.
     */
    private void deleteTask() {
        // If it is today's date, check if "Work Ahead" is displayed and then convert position/day
        convertDay();

        getLogicSubsystem().onButtonClick(mID, 1, this);

        removeItem();
    }

    /**
     * Ensures that if the user selected a task that was on the work ahead screen that the activity
     * uses the right indices.
     */
    private void convertDay() {
        Pair<Integer, Integer> convertedDates = getLogicSubsystem().convertDay(mID);

        if (convertedDates != null) {
            mPosition = convertedDates.getFirst();
            mDay = convertedDates.getSecond();
        }
    }

    /**
     * Show a prompt asking the user how much time a task took to complete, and then add that time
     * to todayTime.
     */
    private void ttcPrompt() {
        // Prompt the user to ask how long it took to complete the task, and add this time to
        // todayTime to prevent the user from being overscheduled on today's date.
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edittext, null);
        builder.setView(view);
        EditText et = view.findViewById(R.id.editText);
        builder.setTitle(R.string.complete_dialog_title)
                .setMessage(R.string.complete_dialog_message)
                .setPositiveButton("OK", ((dialogInterface, i) -> {
                    if (et.getText().length() != 0) {
                        getLogicSubsystem().addTodayTime(Integer.parseInt(et.getText().toString()));

                        removeItem();
                    }
                }));
        builder.show();}

    /**
     * Remove the currently selected item and update the recycler
     */
    private void removeItem() {
        // Remove item from list
        int index = mIDs.indexOf(mID);
        mAdapter.mTaskItemList.remove(index);
        mAdapter.notifyItemRemoved(index);
        mIDs.remove(index);

        // Update the indices of the recycler so all items know their new positions.
        for (int i = index; i < mAdapter.mTaskItemList.size(); i++) {
            TaskItem curr = mAdapter.mTaskItemList.get(i);
            curr.setIndex(curr.getIndex() - 1);
        }

        optimize();
    }

    /**
     * Optimizes the task list asynchronously
     */
    private void optimize() {
        // Variable is here to ensure that we wait properly for mOptimizer to be null.
        //noinspection UnnecessaryLocalVariable
        Thread newThread = new Thread(() -> {
            getLogicSubsystem().Optimize();
            mOptimizer = null;
        });

        mOptimizer = newThread;

        mOptimizer.start();
    }

    /**
     * Ensures that we don't accidentally leave this activity until we're done optimizing.
     */
    @Override
    public void onBackPressed() {
        if (mOptimizer != null) {
            try {
                mOptimizer.wait();
            } catch (Exception ignored) { }
        }

        super.onBackPressed();
    }

    /**
     * Ensures that we wait to use the LogicSubsystem until we're done optimizing.
     *
     * @return The logic subsystem
     */
    private LogicSubsystem getLogicSubsystem() {
        if (mOptimizer != null) {
            try {
                mOptimizer.wait();
            } catch (Exception ignored) { }
        }

        return LogicSubsystem.getInstance();
    }
}