package com.evanv.taskapp.ui.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;

import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ViewFlipper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.ui.text.android.InternalPlatformTextApi;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.evanv.taskapp.R;
import com.evanv.taskapp.databinding.ActivityMainBinding;
import com.evanv.taskapp.logic.LogicSubsystem;
import com.evanv.taskapp.ui.FilterActivity;
import com.evanv.taskapp.ui.LabelsActivity;
import com.evanv.taskapp.ui.TaskListActivity;
import com.evanv.taskapp.ui.additem.EventEntry;
import com.evanv.taskapp.ui.additem.TaskEntry;
import com.evanv.taskapp.ui.main.recycler.DayItem;
import com.evanv.taskapp.ui.main.recycler.DayItemAdapter;
import com.evanv.taskapp.ui.projects.ProjectActivity;
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import org.threeten.bp.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import kotlin.Pair;

@InternalPlatformTextApi /**
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
    private ActivityResultLauncher<Intent> mUpdateUILauncher;
    // Allows us to manually show FAB when task/event completed/deleted.
    LogicSubsystem mLogicSubsystem;                // Subsystem that handles logic for taskapp
    private LocalDate mStartDate;                  // The current date
    private long mEditedID;                        // ID of the currently edited task
    private int mPosition;                         // Position of button press
    private int mDay;                              // Day of button press
    private boolean isFABOpen;                     // Is the fab vertically expanded

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

        // Will eventually return info from projects
        mUpdateUILauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> updateRecycler());

        String[] overdueNames;
        try {
            overdueNames = mLogicSubsystem.getOverdueTasks(this);
        } catch (Exception e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(
                    String.format(getString(R.string.corrupt_error), mLogicSubsystem.getNumFailures()));
            builder.setPositiveButton(R.string.ok, null);
            builder.show();
            overdueNames = mLogicSubsystem.getOverdueTasks(this);
        }

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

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, mBinding.toolbar, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navView = findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(this::onOptionsItemSelected);

        // When fab is clicked, show the two smaller FABs
        mBinding.fab.setOnClickListener(view1 -> {
            mEditedID = -1;
            if (!isFABOpen) {
                isFABOpen = true;
                findViewById(R.id.addEventLayout).animate().translationY(-getResources().getDimension(R.dimen.standard_55));
                findViewById(R.id.addTaskLayout).animate().translationY(-getResources().getDimension(R.dimen.standard_105));
                findViewById(R.id.addTaskLabel).animate().alpha(1);
                findViewById(R.id.addEventLabel).animate().alpha(1);
                findViewById(R.id.addTaskFab).animate().alpha(1);
                findViewById(R.id.addEventFab).animate().alpha(1);
            }
            else {
                closeFAB();
            }
        });

        findViewById(R.id.addEventFab).setOnClickListener(v -> {
            if (isFABOpen) {
                closeFAB();
                addEvent();
            }
        });
        findViewById(R.id.addTaskFab).setOnClickListener(v -> {
            if (isFABOpen) {
                closeFAB();
                addTask();
            }
        });

        // Make visible the main content
        mVF = findViewById(R.id.vf);

        if (!mLogicSubsystem.isEmpty()) {
            mVF.setDisplayedChild(1);
        }
        else {
            mVF.setDisplayedChild(2);
        }

        HideBottomViewOnScrollBehavior<FloatingActionButton> fabBehaviorTask =
                (HideBottomViewOnScrollBehavior<FloatingActionButton>)
                        ((CoordinatorLayout.LayoutParams) mBinding.addTaskLayout.getLayoutParams())
                                .getBehavior();
        if (fabBehaviorTask != null) {
            fabBehaviorTask.addOnScrollStateChangedListener((bottomView, newState) -> closeFAB());
        }
    }

    /**
     * Close FAB from user view
     */
    private void closeFAB() {
        isFABOpen = false;
        findViewById(R.id.addEventLayout).animate().translationY(0);
        findViewById(R.id.addTaskLayout).animate().translationY(0);
        findViewById(R.id.addTaskLabel).animate().alpha(0);
        findViewById(R.id.addEventLabel).animate().alpha(0);
        findViewById(R.id.addTaskFab).animate().alpha(0);
        findViewById(R.id.addEventFab).animate().alpha(0);
    }

    /**
     * Handles the back button being pressed. Allows FAB to be closed on back press.
     */
    @Override
    public void onBackPressed() {
        if (!isFABOpen) {
            super.onBackPressed();
        }
        else {
            closeFAB();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.help_button_menu, menu);
        return super.onCreateOptionsMenu(menu);
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
     * Launch the TaskEntry Bottom Sheet
     */
    private void addEvent() {
        EventEntry frag = new EventEntry();
        frag.setID(mEditedID);
        frag.addSubmitListener(v -> {
            if (frag.addItem()) {
                mEditedID = -1;
                frag.dismiss();
                onActivityResult();
            }
        });
        frag.show(getSupportFragmentManager(), "EVENT");
    }

    /**
     * Launch the EventEntry Bottom Sheet
     */
    private void addTask() {
        TaskEntry frag = new TaskEntry();
        frag.setID(mEditedID);
        frag.addSubmitListener(v -> {
            if (frag.addItem()) {
                mEditedID = -1;
                frag.dismiss();
                onActivityResult();
            }
        });
        frag.show(getSupportFragmentManager(), "TASK");
    }

    /**
     * Optimizes and updates UI after TaskEntry/EventEntry has been called.
     */
    protected void onActivityResult() {
        // As the task dependency graph has been updated, we must reoptimize it
        Runnable toRun = new OptimizeRunnable();

        Thread thread = new Thread(toRun);
        thread.start();
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

        Intent intent;

        switch (item.getItemId()) {
            case (R.id.action_projects):
                intent = new Intent(this, ProjectActivity.class);
                mUpdateUILauncher.launch(intent);
                return true;
            case (R.id.action_search):
                intent = new Intent(this, FilterActivity.class);
                mUpdateUILauncher.launch(intent);
                return true;
            case (R.id.action_labels):
                intent = new Intent(this, LabelsActivity.class);
                mUpdateUILauncher.launch(intent);
                return true;
            case (R.id.action_work_ahead):
                intent = new Intent(this, TaskListActivity.class);
                intent.putExtra(TaskListActivity.EXTRA_COMPLETABLE, true);
                startActivity(intent);
                return true;
            case (R.id.action_help):
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.schedule_url)));
                startActivity(browserIntent);
                return true;
            case (R.id.action_about):
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.about);
                builder.setMessage(R.string.about_message);
                builder.show();
                return true;
            case (R.id.action_settings):
                // TODO: Show settings screen
                return true;
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

            finishButtonPress(newDays);
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
            if (mLogicSubsystem.isTimed(mPosition, mDay)) {
                if (mDay != 0) {
                    getMenuInflater().inflate(R.menu.task_options_timed, menu);
                }
                else {
                    getMenuInflater().inflate(R.menu.task_options_timed_today, menu);
                }
            }
            else {
                if (mDay != 0) {
                    getMenuInflater().inflate(R.menu.task_options, menu);
                }
                else {
                    getMenuInflater().inflate(R.menu.task_options_today, menu);
                }
            }
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
            case (R.id.action_delete_event):
                // Show optimizing... screen
                deleteEvent();
                break;
            case (R.id.action_edit_event):
                // Show optimizing... screen
                editEvent();
                break;
            case (R.id.action_postpone_task):
                postponeTask();
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
        addEvent();
    }

    /**
     * Handles the user choosing to delete an event.
     */
    private void deleteEvent() {
        int oldDays = mLogicSubsystem.getNumDays();
        mLogicSubsystem.onButtonClick(mPosition, mDay, 2, this);
        int newDays = mLogicSubsystem.getNumDays();

        finishButtonPress(newDays);
    }

    /**
     * Handles the user choosing to start a timer on a task.
     */
    private void timeTask() {
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
        mEditedID = mLogicSubsystem.getTaskID(mPosition, mDay);

        // Launch an edit intent
        addTask();
    }

    /**
     * Handles the user choosing to delete a task.
     */
    private void deleteTask() {
        mLogicSubsystem.onButtonClick(mPosition, mDay, 1, this);
        int newDays = mLogicSubsystem.getNumDays();

        finishButtonPress(newDays);
    }

    /**
     * Handles the user choosing to postpone a task.
     */
    private void postponeTask() {
        mLogicSubsystem.postponeTask(mPosition, mDay);
        int newDays = mLogicSubsystem.getNumDays();

        finishButtonPress(newDays);
    }

    /**
     * Handles a user choosing to complete a task
     */
    private void completeTask() {
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

                finishButtonPress(newDays);
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
     * Updates the recycler after a button press has been handled.
     *
     * @param newDays How many days are in the recycler now
     */
    private void finishButtonPress(int newDays) {
        int oldDays = mDayItemAdapter.mDayItemList.size();
        while (mDayItemAdapter.mDayItemList.size() > newDays) {
            mDayItemAdapter.mDayItemList.remove(newDays);
        }

        if (oldDays > newDays) {
            mDayItemAdapter.notifyItemRangeRemoved(newDays, oldDays - newDays);
        }

        Runnable toRun = new OptimizeRunnable();
        Thread thread = new Thread(toRun);
        thread.start();
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

    private class OptimizeRunnable implements Runnable {
        @Override
        public void run() {
            runOnUiThread(() ->mVF.setDisplayedChild(0));
            mLogicSubsystem.Optimize();

            runOnUiThread(() -> {
                updateRecycler();


                // If there are any tasks/events scheduled, show the recycler
                if (!mLogicSubsystem.isEmpty()) {
                    mVF.setDisplayedChild(1);
                }
                // If there aren't any, show the "add a task" fragment
                else {
                    mVF.setDisplayedChild(2);
                }

                // If the FAB is currently hidden, show the FAB again, to prevent it from being lost
                // as the FAB hides if you scroll down currently, and if we don't do this and the
                // recycler doesn't have enough content to scroll, the FAB will be lost until a
                // restart.
                HideBottomViewOnScrollBehavior<FloatingActionButton> fabBehavior =
                        (HideBottomViewOnScrollBehavior<FloatingActionButton>)
                                ((CoordinatorLayout.LayoutParams) mBinding.fab.getLayoutParams())
                                        .getBehavior();
                if (fabBehavior != null) {
                    fabBehavior.slideUp(mBinding.fab);
                }

                mPosition = mDay = -1;
            });
        }
    }
}