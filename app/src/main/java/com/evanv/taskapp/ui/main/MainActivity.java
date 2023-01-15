package com.evanv.taskapp.ui.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;

import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ViewFlipper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.evanv.taskapp.R;
import com.evanv.taskapp.databinding.ActivityMainBinding;
import com.evanv.taskapp.logic.LogicSubsystem;
import com.evanv.taskapp.ui.FilterActivity;
import com.evanv.taskapp.ui.additem.AddItem;
import com.evanv.taskapp.ui.main.recycler.DayItem;
import com.evanv.taskapp.ui.main.recycler.DayItemAdapter;
import com.evanv.taskapp.ui.projects.ProjectActivity;
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.threeten.bp.LocalDate;

import java.util.ArrayList;
import java.util.List;

import kotlin.Pair;

/**
 * Main Activity for the app. Display's the user's schedule of Tasks/Events, while allowing for
 * Task completion/addition (with the latter done through the use of a separate AddItem activity).
 *
 * @author Evan Voogd
 */
@SuppressWarnings("unused")
public class MainActivity extends AppCompatActivity implements ClickListener {
    // Fields
    @SuppressWarnings("unused")
    private ActivityMainBinding mBinding;          // Binding for the MainActivity
    public static final int ITEM_REQUEST = 1;      // requestCode for task/item entry
    private DayItemAdapter mDayItemAdapter;        // Adapter for recycler showing user commitments
    private ViewFlipper mVF;                       // Swaps between loading screen and recycler
    // Allows data to be pulled from activity
    private ActivityResultLauncher<Intent> mStartForResult;
    private ActivityResultLauncher<Intent> mUpdateUILauncher;
    // Allows us to manually show FAB when task/event completed/deleted.
    LogicSubsystem mLogicSubsystem;                // Subsystem that handles logic for taskapp
    private LocalDate mStartDate;                  // The current date
    private long mEditedID;                        // ID of the currently edited task
    private int mPosition;                         // Position of button press
    private int mDay;                              // Day of button press

    // Key for the extra that stores the type of edit
    public static final String EXTRA_TYPE = "com.evanv.taskapp.ui.main.extras.TYPE";
    // Key for the extra that stores the ID of the item to edit
    public static final String EXTRA_ID = "com.evanv.taskapp.ui.main.extras.ID";

    // Keys into SharedPrefs to store todayTime
    public static final String PREF_FILE = "taskappPrefs"; // File name for sharedPrefs
    public static final String PREF_DAY = "taskappDay";    // Day for todayTime
    public static final String PREF_TIME = "taskappTime";  // Time for todayTime
    public static final String PREF_TIMED_TASK = "taskappTimerTask"; // TaskID for timer
    public static final String PREF_TIMER = "taskappTimerStart"; // Start Date for the timer

    /**
     * Handles activities started for a result, in this case when the AddItem activity returns with
     * a new Event/Task to be added. Parses the data in the BundleExtra AddItem.EXTRA_ITEM into a
     * Task/Event depending on their AddItem.EXTRA_TYPE.
     *
     * @param resultCode RESULT_OK if there were no issues with user input
     * @param data Contains the BundleExtra AddItem.EXTRA_ITEM, with all the data needed to build
     *             the item.
     */
    protected void onActivityResult(int resultCode, @Nullable Intent data) {
        // Show loading screen
        if (resultCode != RESULT_OK) {
            mVF.setDisplayedChild(0);

            if (!mLogicSubsystem.isEmpty()) {
                mVF.setDisplayedChild(1);
            }
            else {
                mVF.setDisplayedChild(2);
            }

            // As the task dependency graph has been updated, we must reoptimize it
            Optimize();

            // Update the recycler
            updateRecycler();

            // Show recycler as Optimize is finished
            mVF.setDisplayedChild(1);
        }
    }

    /**
     * Calls the Optimizer to find an optimal schedule for the user's tasks, given the user's
     * scheduled events.
     */
    private void Optimize() {
        mLogicSubsystem.Optimize();

        updateRecycler();
    }


    /**
     * Runs on the start of the app. Most importantly it loads the user data from the file.
     *
     * @param savedInstanceState Not used.
     */
    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create layout
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        // startDate is our representation for the current date upon the launch of TaskApp.
        mStartDate = LocalDate.now();

        // Get todayTime from shared preferences
        SharedPreferences sp = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        LocalDate todayTimeDate = LocalDate.ofEpochDay(sp.getLong(PREF_DAY, -1L));
        int todayTime = 0;
        if (!todayTimeDate.isBefore(mStartDate)) {
            todayTime = sp.getInt(PREF_TIME, 0);
        }

        mEditedID = -1;

        // Get current timer
        long timedTaskID = sp.getLong(PREF_TIMED_TASK, -1);
        long timerStart = sp.getLong(PREF_TIMER, -1);

        mLogicSubsystem = LogicSubsystem.getInstance();

        if (mLogicSubsystem == null) {
            mLogicSubsystem = new LogicSubsystem(this, todayTime, timedTaskID, timerStart);
        }

        // Create activity result handler for AddItem
        mStartForResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> MainActivity.this.onActivityResult(result.getResultCode(),
                        result.getData()));

        // Will eventually return info from projects
        mUpdateUILauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> updateRecycler());

        String[] overdueNames = mLogicSubsystem.getOverdueTasks(this);

        // Prompt the user with a dialog containing overdue tasks so they can mark overdue tasks
        // so taskapp can reoptimize the schedule if some tasks are overdue.
        if (overdueNames.length != 0) {
            // List of indices to tasks that were completed.
            ArrayList<Integer> selectedItems = new ArrayList<>();

            Context context = this;

            // Show a dialog prompting the user to mark tasks that were completed as complete
            android.app.AlertDialog.Builder builder =
                    new android.app.AlertDialog.Builder(this);
            builder.setTitle(R.string.overdue_dialog_title)
                    .setMultiChoiceItems(overdueNames, null,
                            new DialogInterface.OnMultiChoiceClickListener() {

                                /**
                                 * Adds the selected task to the toRemove list, or removes it if the
                                 * task was unselected
                                 *
                                 * @param dialogInterface not used
                                 * @param index           the index into the tasks ArrayList of the
                                 *                        parent
                                 * @param isChecked       true if checked, false if unchecked
                                 */
                                @Override
                                public void onClick(DialogInterface dialogInterface, int index,
                                                    boolean isChecked) {
                                    // If checked, add to list of Tasks to be added as complete
                                    if (isChecked) {
                                        selectedItems.add(index);
                                    }
                                    // If unchecked, remove form list of Tasks to be added as
                                    // complete
                                    else if (selectedItems.contains(index)) {
                                        selectedItems.remove(index);
                                    }
                                }
                            }).setPositiveButton(R.string.overdue_dialog_button,
                            new DialogInterface.OnClickListener() {

                                /**
                                 * Continues starter code when overdue tasks exist
                                 *
                                 * @param di    not used
                                 * @param index not used
                                 */
                                @Override
                                public void onClick(DialogInterface di, int index) {
                                    mLogicSubsystem.updateOverdueTasks(selectedItems, context);

                                    finishProcessing(true);
                                }
                            });

            builder.create();
            builder.show();

        }
        else {
            finishProcessing(false);
        }
    }

    /**
     * Now that the user has handled overdue tasks, we're ready to start the app. This function
     * adds data to the recycler and moves to the working fragment.
     *
     * @param reoptimize If the task list has changed due to overdue tasks, we might have to
     *                   reoptimize it
     */
    private void finishProcessing(boolean reoptimize) {
        List<DayItem> recyclerData = mLogicSubsystem.prepForDisplay(reoptimize, this);

        // Initialize the main recyclerview with data calculated in helper function DayItemList
        RecyclerView dayRecyclerView = findViewById(R.id.main_recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
        mDayItemAdapter = new DayItemAdapter(recyclerData, this, this);
        dayRecyclerView.setAdapter(mDayItemAdapter);
        dayRecyclerView.setLayoutManager(layoutManager);

        // Adds the action bar at the top of the screen
        setSupportActionBar(mBinding.toolbar);

        // When the FAB is clicked, run intentAddItem to open the AddItem Activity
        mBinding.fab.setOnClickListener(view -> intentAddItem(null, -1));

        // Make visible the main content
        mVF = findViewById(R.id.vf);

        if (!mLogicSubsystem.isEmpty()) {
            mVF.setDisplayedChild(1);
        }
        else {
            mVF.setDisplayedChild(2);
        }

    }

    /**
     * Updates todayTime in SharedPreferences
     */
    @Override
    protected void onPause() {
        super.onPause();

        // Update todayTime in SharedPreferences
        SharedPreferences sp = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        edit.putLong(PREF_DAY, mStartDate.toEpochDay());
        edit.putInt(PREF_TIME, mLogicSubsystem.getTodayTime());
        edit.putLong(PREF_TIMED_TASK, mLogicSubsystem.getTimedID());
        edit.putLong(PREF_TIMER, mLogicSubsystem.getTimerStart());

        edit.apply();
    }

    /**
     * Launches the AddItem activity. Must be separate function so FAB handler can call it.
     *
     * @param type Null if adding an item, one of EXTRA_TASK or EXTRA_EVENT if editing
     * @param id ID of the task/event if editing, unused if not
     */
    private void intentAddItem(String type, long id) {
        Intent intent = new Intent(this, AddItem.class);

        // Handles Editing case
        if (type != null) {
            intent.putExtra(EXTRA_TYPE, type);
            intent.putExtra(EXTRA_ID, id);
        }
        else {
            mEditedID = -1;
        }

        mStartForResult.launch(intent);
    }

    /**
     * Adds the items to the three dot menu in the ActionBar. Left to defaults for now.
     *
     * @param menu The menu in the top right of the screen
     * @return always true
     */
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Handles action bar clicks. Left to defaults for now
     *
     * @param item The MenuItem selected
     * @return true in virtually every scenario
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will automatically handle clicks on
        // the Home/Up button, so long as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // Handles the settings menu item being chosen
        if (id == R.id.action_projects) {
            Intent intent = new Intent(this, ProjectActivity.class);

            mUpdateUILauncher.launch(intent);

            return true;
        }
        else if (id == R.id.action_search) {
            Intent intent = new Intent(this, FilterActivity.class);
            mUpdateUILauncher.launch(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Handles one of the buttons being clicked to complete a task. Weird structure where it's
     * called by the DayItemHolder for the day associated with the task, which in turn is called by
     * the TaskItemHolder for the task.
     *
     * @param position The index into the taskSchedule.get(day) ArrayList where the task is stored
     * @param day The date this task is scheduled for is day days past today's date
     */
    @Override
    public void onButtonClick(int position, int day, int action) {
        switch (action) {
            // Normal button press, mark task as complete
            case 0:
                // Show optimizing... screen
                mVF.setDisplayedChild(0);
                mPosition = position;
                mDay = day;
                completeTask();
                break;
            // Options button pressed, set mPosition/mDay
            case 1:
            case 2:
                mPosition = position;
                mDay = day;
                break;
        }
    }

    /**
     * Show a prompt asking the user how much time a task took to complete, and then add that time
     * to todayTime.
     */
    private void ttcPrompt(int newDays, int oldDays) {
        int completionTime = -1;

        // Prompt the user to ask how long it took to complete the task, and add this time to
        // todayTime to prevent the user from being overscheduled on today's date.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.complete_dialog_message));

        builder.setTitle(R.string.complete_dialog_title);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton(R.string.complete_task, (dialogInterface, i) -> {
            mLogicSubsystem.addTodayTime(Integer.parseInt(input.getText().toString()));

            finishButtonPress(newDays, oldDays);
        });

        builder.show();
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

        if (v.getId() == R.id.buttonEventOptions) {
            getMenuInflater().inflate(R.menu.event_options, menu);
        }
        else if (v.getId() == R.id.buttonTaskOptions) {
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
                mVF.setDisplayedChild(0);
                deleteTask();
                break;
            case (R.id.action_edit_task):
                // Show optimizing... screen
                mVF.setDisplayedChild(0);
                editTask();
                break;
            case (R.id.action_time_task):
                timeTask();
                break;
            case (R.id.action_delete_event):
                // Show optimizing... screen
                mVF.setDisplayedChild(0);
                deleteEvent();
                break;
            case (R.id.action_edit_event):
                // Show optimizing... screen
                mVF.setDisplayedChild(0);
                editEvent();
                break;
        }

        return true;
    }

    /**
     * Handles the user choosing to edit an event.
     */
    private void editEvent() {
        mEditedID = mLogicSubsystem.getEventID(mPosition, mDay);

        // Launch an edit intent
        intentAddItem(AddItem.EXTRA_VAL_EVENT, mEditedID);
    }

    /**
     * Handles the user choosing to delete an event.
     */
    private void deleteEvent() {
        int oldDays = mLogicSubsystem.getNumDays();
        mLogicSubsystem.onButtonClick(mPosition, mDay, 2, this);
        int newDays = mLogicSubsystem.getNumDays();

        finishButtonPress(newDays, oldDays);
    }

    /**
     * Handles the user choosing to start a timer on a task.
     */
    private void timeTask() {
        // If it is today's date, check if "Work Ahead" is displayed and then convert position/day
        convertDay();

        int oldTimer = mLogicSubsystem.getTimerDay();

        mLogicSubsystem.timer(mPosition, mDay);

        mDayItemAdapter.mDayItemList.set(mDay, mLogicSubsystem.DayItemHelper(mDay, this));
        mDayItemAdapter.mDayItemList.set(0, mLogicSubsystem.DayItemHelper(0, this));
        mDayItemAdapter.notifyItemChanged(mDay);
        mDayItemAdapter.notifyItemChanged(0);

        if (oldTimer != -1 && oldTimer != mDay) {
            mDayItemAdapter.mDayItemList.set(oldTimer, mLogicSubsystem.DayItemHelper(oldTimer, this));
            mDayItemAdapter.notifyItemChanged(oldTimer);
        }
    }

    /**
     * Handles the user choosing to edit a task.
     */
    private void editTask() {
        // If it is today's date, check if "Work Ahead" is displayed and then convert position/day
        convertDay();

        mEditedID = mLogicSubsystem.getTaskID(mPosition, mDay);

        // Launch an edit intent
        intentAddItem(AddItem.EXTRA_VAL_TASK, mEditedID);
    }

    /**
     * Handles the user choosing to delete a task.
     */
    private void deleteTask() {
        // If it is today's date, check if "Work Ahead" is displayed and then convert position/day
        convertDay();

        int oldDays = mLogicSubsystem.getNumDays();
        mLogicSubsystem.onButtonClick(mPosition, mDay, 1, this);
        int newDays = mLogicSubsystem.getNumDays();

        finishButtonPress(newDays, oldDays);
    }

    /**
     * Handles a user choosing to complete a task
     */
    private void completeTask() {
        // If it is today's date, check if "Work Ahead" is displayed and then convert position/day
        convertDay();

        boolean isTimed = mLogicSubsystem.isTimed(mPosition, mDay);
        int timerVal = -1;

        if (isTimed) {
            timerVal = mLogicSubsystem.getTimer();
        }

        int oldDays = mLogicSubsystem.getNumDays();
        mLogicSubsystem.onButtonClick(mPosition, mDay, 0, this);
        int newDays = mLogicSubsystem.getNumDays();

        if (isTimed) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(String.format(getString(R.string.timer_prompt), timerVal));

            // If user chooses to accept the given time, then add it to the today time and
            // continue.
            int finalTimerVal = timerVal;
            builder.setPositiveButton("OK", (dialogInterface, i) -> {
                mLogicSubsystem.addTodayTime(finalTimerVal);

                finishButtonPress(newDays, oldDays);
            });

            // If user chooses to use a different time, show the normal time to complete
            // dialog.
            builder.setNegativeButton("Manual Time", (dialogInterface, i) ->
                    ttcPrompt(newDays, oldDays));

            builder.show();
            return;
        }

        // User did not have a timer set, so use the normal time to complete dialog.
        ttcPrompt(newDays, oldDays);
    }

    /**
     * Ensures that if the user selected a task that was on the work ahead screen that the activity
     * uses the right indices.
     */
    private void convertDay() {
        if (mDay == 0) {
            Pair<Integer, Integer> convertedDates = mLogicSubsystem.convertDay(mPosition);

            if (convertedDates != null) {
                mPosition = convertedDates.getFirst();
                mDay = convertedDates.getSecond();
            }
        }
    }

    /**
     * Updates the recycler after a button press has been handled.
     *
     * @param newDays How many days are in the recycler now
     * @param oldDays How many days used to be in the recycler.
     */
    private void finishButtonPress(int newDays, int oldDays) {
        while (mDayItemAdapter.mDayItemList.size() > newDays) {
            mDayItemAdapter.mDayItemList.remove(newDays);
        }

        Optimize();
        updateRecycler();

        if (oldDays != newDays) {
            mDayItemAdapter.notifyItemRangeRemoved(newDays, oldDays - newDays);
        }

        // If there are any tasks/events scheduled, show the recycler
        if (!mLogicSubsystem.isEmpty()) {
            mVF.setDisplayedChild(1);
        }
        // If there aren't any, show the "add a task" fragment
        else {
            mVF.setDisplayedChild(2);
        }

        // If the FAB is currently hidden, show the FAB again, to prevent it from being lost as the
        // FAB hides if you scroll down currently, and if we don't do this and the recycler doesn't
        // have enough content to scroll, the FAB will be lost until a restart.
        HideBottomViewOnScrollBehavior<FloatingActionButton> fabBehavior =
                (HideBottomViewOnScrollBehavior<FloatingActionButton>)
                        ((CoordinatorLayout.LayoutParams) mBinding.fab.getLayoutParams())
                                .getBehavior();
        if (fabBehavior != null) {
            fabBehavior.slideUp(mBinding.fab);
        }

        mPosition = mDay = -1;
    }

    /**
     * Update recycler based on changes in other screens.
     */
    private void updateRecycler() {
        List<Integer> updatedIndices = LogicSubsystem.getInstance().getUpdatedIndices();

        for (int index : updatedIndices) {
            if (index >= mDayItemAdapter.getItemCount()) {
                int oldCount = mDayItemAdapter.getItemCount();
                for (int i = oldCount; i <= index; i++) {
                    mDayItemAdapter.mDayItemList.add(mLogicSubsystem.DayItemHelper(i, this));
                }
                mDayItemAdapter.notifyItemRangeInserted(oldCount, index - oldCount + 1);
            }
            else if (index >= 0) {
                mDayItemAdapter.mDayItemList.set(index,
                        LogicSubsystem.getInstance().DayItemHelper(index, this));
                mDayItemAdapter.notifyItemChanged(index);
            }
        }
    }
}