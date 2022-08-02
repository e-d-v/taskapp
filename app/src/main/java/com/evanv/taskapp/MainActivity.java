package com.evanv.taskapp;

import static com.evanv.taskapp.Event.clearTime;
import static com.evanv.taskapp.Task.clearDate;
import static com.evanv.taskapp.Task.getDiff;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.text.InputType;
import android.util.Log;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.evanv.taskapp.databinding.ActivityMainBinding;
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main Activity for the app. Display's the user's schedule of Tasks/Events, while allowing for
 * Task completion/addition (with the latter done through the use of a separate AddItem activity).
 *
 * @author Evan Voogd
 */
@SuppressWarnings("unused")
public class MainActivity extends AppCompatActivity implements ClickListener {

    @SuppressWarnings("unused")
    private ActivityMainBinding mBinding;      // Binding for the MainActivity
    private Date mStartDate;                 // The current date
    private int mTodayTime;                    // The amount of time spent completing tasks today
    private int mNumEvents;                    // Number of events for user
    public static final int ITEM_REQUEST = 1; // requestCode for task/item entry
    private DayItemAdapter mDayItemAdapter;    // Adapter for recyclerview showing user commitments
    private final ArrayList<Task> mTasks = new ArrayList<>(); // List of all tasks for user
    // taskSchedule[i] represents the list of tasks for the day i days past startDate
    private final ArrayList<ArrayList<Task>> mTaskSchedule = new ArrayList<>();
    // eventSchedule[i] represents the list of events for the day i days past startDate
    private final ArrayList<ArrayList<Event>> mEventSchedule = new ArrayList<>();
    // Key for the extra that stores the list of Task names for the Parent Task Picker Dialog in
    // TaskEntry
    public static final String EXTRA_TASKS = "com.evanv.taskapp.extras.TASKS";
    private ViewFlipper mVF; // Swaps between loading screen and recycler
    // Allows data to be pulled from activity
    private ActivityResultLauncher<Intent> mStartForResult;
    // Allows us to manually show FAB when task/event completed/deleted.

    /**
     * Removes a task from the task dependency graph, while asking the user how long it took to
     * complete and adding that time to todayTime to prevent overscheduling on today's date.
     *
     * @param task The task to be removed from the task dependency graph
     * @param showDialog true if dialog is needed, false if dialog isn't
     * @param refresh true if we should refresh recycler
     */
    private void Complete(Task task, boolean showDialog, boolean refresh) {
        // Show loading screen
        mTasks.remove(task);

        Date doDate = task.getDoDate();

        // Get the number of days past the start date this task is scheduled for, so we can get the
        // index of the taskSchedule member for it's do date.
        int diff = getDiff(doDate, mStartDate);

        if (diff >= 0) {
            mTaskSchedule.get(diff).remove(task);
        }

        // Remove the task from the task dependency graph
        for (int i = 0; i < task.getChildren().size(); i++) {
            task.getChildren().get(i).removeParent(task);
        }
        for (int i = 0; i < task.getParents().size(); i++) {
            task.getParents().get(i).removeChild(task);
        }

        if (showDialog) {
            // Prompt the user to ask how long it took to complete the task, and add this time to
            // todayTime to prevent the user from being overscheduled on today's date.
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(String.format(getString(R.string.complete_dialog_message),
                    task.getName()));
            builder.setTitle(R.string.complete_dialog_title);

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            builder.setView(input);

            builder.setPositiveButton(R.string.complete_task, (dialogInterface, i) -> {
                mTodayTime += Integer.parseInt(input.getText().toString());
                // As the task dependency graph has been updated, we must reoptimize it
                Optimize(refresh);
                // Show recycler
                mVF.setDisplayedChild(1);
            });

            builder.show();
        }
        else {
            // As the task dependency graph has been updated, we must reoptimize it
            Optimize(refresh);
        }
    }

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
        mVF.setDisplayedChild(0);

        // If the request is for AddItem
        if (resultCode == RESULT_OK) {
            // Get the data to build the item
            Bundle result = Objects.requireNonNull(data).getBundleExtra(AddItem.EXTRA_ITEM);
            String type = result.getString(AddItem.EXTRA_TYPE);

            // If the item type is Event
            if (type.equals(AddItem.EXTRA_VAL_EVENT)) {
                // Get the fields from the bundle
                String name = result.getString(AddItem.EXTRA_NAME);
                int ttc = Integer.parseInt(result.getString(AddItem.EXTRA_TTC));
                String startStr = result.getString(AddItem.EXTRA_START);
                Bundle recur = result.getBundle(AddItem.EXTRA_RECUR);

                // Convert the String start time into a MyTime
                Date start;
                Calendar userStart;
                try {
                    start = Event.dateFormat.parse(startStr);
                    userStart = Calendar.getInstance();
                    if (start != null) {
                        userStart.setTime(start);
                    }
                    else {
                        return;
                    }
                }
                catch (Exception e) {
                    System.out.println(e.getMessage());
                    return;
                }

                // Calculate how many days past today's date this event is scheduled (used to
                // index into eventSchedule
                int diff = getDiff(start, mStartDate);

                // Add the new event to the data structure
                if (!recur.get(RecurInput.EXTRA_TYPE).equals(NoRecurFragment.EXTRA_VAL_TYPE)) {
                    boolean until = recur.get(RecurActivity.EXTRA_UNTIL_TYPE)
                            .equals(RecurActivity.EXTRA_VAL_UNTIL);

                    // Initialize until holders. Only the one associated with the user's choice
                    // of recurrence is used.
                    int endIndex = -1;
                    int numTimes = -1;
                    Date endDate = new Date();

                    // Recur until end date
                    if (until) {
                        try {
                            endDate = Task.dateFormat.parse
                                    (recur.getString(RecurActivity.EXTRA_UNTIL));
                            endIndex = getDiff(endDate, mStartDate);
                        } catch (ParseException e) {
                            Log.e("TaskApp.MainActivity", e.getMessage());
                        }
                    }
                    // Recur set number of times
                    else {
                        numTimes = Integer.parseInt(recur.getString(RecurActivity.EXTRA_UNTIL));
                    }

                    String recurType = recur.getString(RecurInput.EXTRA_TYPE);
                    if (DailyRecurFragment.EXTRA_VAL_TYPE.equals(recurType)) {
                        // Number of days between recurrences
                        int interval = recur.getInt(DailyRecurFragment.EXTRA_INTERVAL);

                        // Calculates the number of times recurrence occurs.
                        numTimes = (until) ? ((endIndex - diff) / interval) + 1 : numTimes;

                        // Make sure there is enough mEventSchedules.
                        for (int i = mEventSchedule.size(); i <= diff + (numTimes * interval);
                             i++) {
                            mEventSchedule.add(new ArrayList<>());
                        }

                        // Add the event once every interval days.
                        for (int i = 0; i < (numTimes * interval); i += interval) {
                            // Calculate date to add event on.
                            Calendar recurCal = Calendar.getInstance();
                            recurCal.setTime(start);
                            recurCal.add(Calendar.DAY_OF_YEAR, i);

                            Event toAdd = new Event(name, recurCal.getTime(), ttc);
                            mEventSchedule.get(diff + i).add(toAdd);
                            mNumEvents++;
                        }
                    }
                    else if (WeeklyRecurFragment.EXTRA_VAL_TYPE.equals(recurType)) {
                        // Number of days between recurrences
                        int interval = recur.getInt(WeeklyRecurFragment.EXTRA_INTERVAL);
                        // day[0] = if recurs on sunday, day[6] if recurs on saturday
                        boolean[] days = recur.getBooleanArray(WeeklyRecurFragment.EXTRA_DAYS);

                        for (int i = 1; i <= 7; i++) {
                            if (days[i - 1]) {
                                Calendar recurCal = Calendar.getInstance();
                                recurCal.setTime(start);

                                // Calculates how many days we must add to recurCal to get next
                                // day of week
                                int currIndex = ((i - recurCal.get(Calendar.DAY_OF_WEEK)) + 7)
                                        % 7;

                                // oneLess shows us if this day occurs before the initial day of the
                                // week. This is useful as it stops the days before from being
                                // skewed away from the actual repetition
                                boolean oneLess = (i - recurCal.get(Calendar.DAY_OF_WEEK)) < 0;

                                recurCal.add(Calendar.DAY_OF_YEAR, currIndex);

                                // Gets start index into eventSchedule
                                currIndex += diff;

                                if (until) {
                                    numTimes = Integer.MAX_VALUE;
                                }
                                else {
                                    endDate.setTime(Long.MAX_VALUE);
                                }

                                // This means that our week iterator is off by one.
                                boolean offByOne = oneLess && (interval != 1);

                                for (int j = 0; j < numTimes && !recurCal.getTime().after(endDate);
                                     j++) {
                                    // Add event to schedule
                                    // Make sure an edge case isn't reached. oneLess only matters if
                                    // interval == 1, as the offByOne if clause fixes it otherwise
                                    if (!(j == 0 && offByOne) && !(j == numTimes - 1 && oneLess &&
                                            !offByOne)) {
                                        Event toAdd = new Event(name, recurCal.getTime(), ttc);

                                        for (int k = mEventSchedule.size(); k <= currIndex; k++) {
                                            mEventSchedule.add(new ArrayList<>());
                                        }

                                        mEventSchedule.get(currIndex).add(toAdd);
                                        mNumEvents++;
                                    }

                                    // Calculate next date
                                    recurCal.add(Calendar.WEEK_OF_YEAR, interval);
                                    currIndex += (7 * interval);

                                    // Fix the skew caused by an event recurring at a non-1 interval
                                    if (j == 0 && offByOne) {
                                        recurCal.add(Calendar.WEEK_OF_YEAR, -1);
                                        currIndex -= 7;
                                    }
                                }
                            }
                        }
                    }
                    else if (MonthlyRecurFragment.EXTRA_VAL_TYPE.equals(recurType)) {
                        int interval = recur.getInt(MonthlyRecurFragment.EXTRA_INTERVAL);
                        String intervalType = recur.getString(MonthlyRecurFragment.EXTRA_RECUR_TYPE);

                        Calendar recurCal = Calendar.getInstance();
                        recurCal.setTime(start);

                        int currIndex = diff;

                        if (intervalType.equals(MonthlyRecurFragment.EXTRA_VAL_STATIC)) {
                            if (!until) {
                                Calendar endDateCal = Calendar.getInstance();
                                endDateCal.setTime(start);
                                endDateCal.add(Calendar.MONTH, interval * (numTimes - 1));
                                endDate = endDateCal.getTime();
                                endIndex = getDiff(endDate, mStartDate);
                            }

                            for (int i = mEventSchedule.size(); i <= endIndex; i++) {
                                mEventSchedule.add(new ArrayList<>());
                            }
                            while (!recurCal.getTime().after(endDate)) {
                                // Add event to schedule
                                Event toAdd = new Event(name, recurCal.getTime(), ttc);
                                mEventSchedule.get(currIndex).add(toAdd);
                                mNumEvents++;

                                // Calculate next date
                                recurCal.add(Calendar.MONTH, interval);
                                currIndex = getDiff(recurCal.getTime(), mStartDate);
                            }
                        }
                        if (intervalType.equals(MonthlyRecurFragment.EXTRA_VAL_DYNAMIC)) {
                            int dayOfWeek = recurCal.get(Calendar.DAY_OF_WEEK);
                            int dayOfWeekInMonth = recurCal.get(Calendar.DAY_OF_WEEK_IN_MONTH);

                            // We will repeat until the other clause (date after last day) is
                            // reached
                            if (until) {
                                numTimes = Integer.MAX_VALUE;
                            }
                            else {
                                Calendar endCal = Calendar.getInstance();
                                // 2100 as this is when the display functionality breaks
                                endCal.set(Calendar.YEAR, 2100);

                                endDate = endCal.getTime();
                            }

                            for (int i = 0; i < numTimes && !recurCal.getTime().after(endDate); i++) {
                                // Add event to schedule
                                Event toAdd = new Event(name, recurCal.getTime(), ttc);

                                for (int j = mEventSchedule.size(); j <= currIndex; j++) {
                                    mEventSchedule.add(new ArrayList<>());
                                }

                                mEventSchedule.get(currIndex).add(toAdd);
                                mNumEvents++;

                                // Calculate next date
                                recurCal.add(Calendar.MONTH, interval);
                                recurCal.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                                recurCal.set(Calendar.DAY_OF_WEEK_IN_MONTH, dayOfWeekInMonth);
                                currIndex = getDiff(recurCal.getTime(), mStartDate);
                            }
                        }
                        if (intervalType.equals(MonthlyRecurFragment.EXTRA_VAL_SPECIFIC)) {
                            String[] strs = recur.getString(MonthlyRecurFragment.EXTRA_DAYS)
                                    .split(",");
                            int[] days = new int[strs.length];

                            for (int i = 0; i < strs.length; i++) {
                                days[i] = Integer.parseInt(strs[i]);
                            }

                            Arrays.sort(days);

                            for (int day : days) {
                                recurCal.setTime(start);
                                int daysInMonth = recurCal.getActualMaximum(Calendar.DAY_OF_MONTH);
                                int dayDiff = (day - recurCal.get(Calendar.DAY_OF_MONTH)
                                        + daysInMonth) % daysInMonth;

                                boolean oneLess = (day - recurCal.get(Calendar.DAY_OF_MONTH)) < 0;
                                boolean offByOne = oneLess && (interval != 1);

                                recurCal.add(Calendar.DAY_OF_YEAR, dayDiff);
                                currIndex = getDiff(recurCal.getTime(), mStartDate);

                                // We will repeat until the other clause (date after last day) is
                                // reached
                                if (until) {
                                    numTimes = Integer.MAX_VALUE;
                                }
                                else {
                                    Calendar endCal = Calendar.getInstance();
                                    // 2100 as this is when the display functionality breaks
                                    endCal.set(Calendar.YEAR, 2100);

                                    endDate = endCal.getTime();
                                }

                                for (int i = 0; i < numTimes && !recurCal.getTime().after(endDate);
                                     i++) {
                                    // Add event to schedule
                                    if (!(i == 0 && offByOne) && !(i == numTimes - 1 && oneLess &&
                                            !offByOne)) {
                                        Event toAdd = new Event(name, recurCal.getTime(), ttc);
                                        for (int k = mEventSchedule.size(); k <= currIndex; k++) {
                                            mEventSchedule.add(new ArrayList<>());
                                        }
                                        mEventSchedule.get(currIndex).add(toAdd);
                                        mNumEvents++;
                                    }

                                    // Calculate next date
                                    if (i == 0 && offByOne) {
                                        recurCal.add(Calendar.MONTH, -1);
                                    }
                                    recurCal.add(Calendar.MONTH, interval);
                                    currIndex = getDiff(recurCal.getTime(), mStartDate);
                                }
                            }
                        }
                    }
                    else if (YearlyRecurFragment.EXTRA_VAL_TYPE.equals(recurType)) {
                        // How many years between each recurrence of this event.
                        int interval = recur.getInt(YearlyRecurFragment.EXTRA_INTERVAL);
                        // How the event will recur (the 18th, 3rd monday, 18/21st etc.)
                        String intervalType = recur.getString(YearlyRecurFragment.EXTRA_RECUR_TYPE);
                        // What months to recur on if necessary.
                        boolean[] months = new boolean[12];



                        if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_MULTIPLE_DYNAMIC)
                                || intervalType.equals(YearlyRecurFragment.EXTRA_VAL_MULTIPLE_STATIC)
                                || intervalType.equals(YearlyRecurFragment.EXTRA_VAL_SPECIFIC)) {
                            List<String> monthsStr = Arrays.asList(
                                    recur.getString(YearlyRecurFragment.EXTRA_MONTHS).split(","));

                            months[0] = monthsStr.contains(getString(R.string.jan));
                            months[1] = monthsStr.contains(getString(R.string.feb));
                            months[2] = monthsStr.contains(getString(R.string.mar));
                            months[3] = monthsStr.contains(getString(R.string.apr));
                            months[4] = monthsStr.contains(getString(R.string.may));
                            months[5] = monthsStr.contains(getString(R.string.jun));
                            months[6] = monthsStr.contains(getString(R.string.jul));
                            months[7] = monthsStr.contains(getString(R.string.aug));
                            months[8] = monthsStr.contains(getString(R.string.sep));
                            months[9] = monthsStr.contains(getString(R.string.oct));
                            months[10] = monthsStr.contains(getString(R.string.nov));
                            months[11] = monthsStr.contains(getString(R.string.dec));
                        }

                        Calendar recurCal = Calendar.getInstance();
                        recurCal.setTime(start);

                        int currIndex = diff;

                        if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_STATIC)) {

                            if (!until) {
                                Calendar endDateCal = Calendar.getInstance();
                                endDateCal.setTime(start);
                                endDateCal.add(Calendar.YEAR, interval * (numTimes - 1));
                                endDate = endDateCal.getTime();
                            }

                            for (int i = mEventSchedule.size(); i <= endIndex; i++) {
                                mEventSchedule.add(new ArrayList<>());
                            }

                            while (!recurCal.getTime().after(endDate)) {
                                // Add event to schedule
                                Event toAdd = new Event(name, recurCal.getTime(), ttc);
                                for (int k = mEventSchedule.size(); k <= currIndex; k++) {
                                    mEventSchedule.add(new ArrayList<>());
                                }
                                mEventSchedule.get(currIndex).add(toAdd);
                                mNumEvents++;

                                // Calculate next date
                                recurCal.add(Calendar.YEAR, interval);
                                currIndex = getDiff(recurCal.getTime(), mStartDate);
                            }
                        }
                        if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_DYNAMIC)) {
                            int dayOfWeek = recurCal.get(Calendar.DAY_OF_WEEK);
                            int dayOfWeekInMonth = recurCal.get(Calendar.DAY_OF_WEEK_IN_MONTH);

                            if (until) {
                                numTimes = Integer.MAX_VALUE;
                            }
                            else {
                                endDate.setTime(Long.MAX_VALUE);
                            }

                            for (int i = 0; i < numTimes && !recurCal.getTime().after(endDate); i++) {
                                // Add event to schedule
                                Event toAdd = new Event(name, recurCal.getTime(), ttc);
                                for (int k = mEventSchedule.size(); k <= currIndex; k++) {
                                    mEventSchedule.add(new ArrayList<>());
                                }
                                mEventSchedule.get(currIndex).add(toAdd);
                                mNumEvents++;

                                // Calculate next date
                                recurCal.add(Calendar.YEAR, interval);
                                recurCal.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                                recurCal.set(Calendar.DAY_OF_WEEK_IN_MONTH, dayOfWeekInMonth);
                                currIndex = getDiff(recurCal.getTime(), mStartDate);
                            }
                        }
                        if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_MULTIPLE_STATIC)) {
                            for (int i = 0; i < months.length; i++) {
                                if (months[i]) {
                                    boolean oneLess = (i - recurCal.get(Calendar.MONTH)) < 0;
                                    boolean offByOne = oneLess && (interval != 1);
                                    recurCal = Calendar.getInstance();
                                    recurCal.setTime(start);
                                    recurCal.add(Calendar.MONTH,
                                            (i - recurCal.get(Calendar.MONTH) + 12) % 12);
                                    currIndex = getDiff(recurCal.getTime(), mStartDate);

                                    if (until) {
                                        numTimes = Integer.MAX_VALUE;
                                    }
                                    else {
                                        endDate.setTime(Long.MAX_VALUE);
                                    }

                                    for (int j = 0; j < numTimes && !recurCal.getTime()
                                            .after(endDate); j++) {
                                        // Add event to schedule
                                        if (!(j == 0 && offByOne) && !(j == numTimes - 1 && oneLess &&
                                                !offByOne)) {
                                            Event toAdd = new Event(name, recurCal.getTime(), ttc);
                                            for (int k = mEventSchedule.size(); k <= currIndex; k++) {
                                                mEventSchedule.add(new ArrayList<>());
                                            }
                                            mEventSchedule.get(currIndex).add(toAdd);
                                            mNumEvents++;
                                        }

                                        // Calculate next date
                                        if (j == 0 && offByOne) {
                                            recurCal.add(Calendar.YEAR, -1);
                                        }
                                        recurCal.add(Calendar.YEAR, interval);
                                        currIndex = getDiff(recurCal.getTime(), mStartDate);
                                    }
                                }
                            }
                        }
                        if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_MULTIPLE_DYNAMIC)) {
                            int dayOfWeek = recurCal.get(Calendar.DAY_OF_WEEK);
                            int dayOfWeekInMonth = recurCal.get(Calendar.DAY_OF_WEEK_IN_MONTH);

                            for (int i = 0; i < months.length; i++) {
                                if (months[i]) {
                                    boolean oneLess = (i - recurCal.get(Calendar.MONTH)) < 0;
                                    boolean offByOne = oneLess && (interval != 1);
                                    recurCal = Calendar.getInstance();
                                    recurCal.setTime(start);
                                    recurCal.add(Calendar.MONTH,
                                            (i - recurCal.get(Calendar.MONTH) + 12) % 12);
                                    recurCal.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                                    recurCal.set(Calendar.DAY_OF_WEEK_IN_MONTH, dayOfWeekInMonth);
                                    currIndex = getDiff(recurCal.getTime(), mStartDate);

                                    if (until) {
                                        numTimes = Integer.MAX_VALUE;
                                    }
                                    else {
                                        endDate.setTime(Long.MAX_VALUE);
                                    }

                                    for (int j = 0; j < numTimes && !recurCal.getTime()
                                            .after(endDate); j++) {
                                        // Add event to schedule
                                        if (!(j == 0 && offByOne) && !(j == numTimes - 1 && oneLess
                                                && !offByOne)) {
                                            Event toAdd = new Event(name, recurCal.getTime(), ttc);
                                            for (int k = mEventSchedule.size(); k <= currIndex; k++) {
                                                mEventSchedule.add(new ArrayList<>());
                                            }
                                            mEventSchedule.get(currIndex).add(toAdd);
                                            mNumEvents++;
                                        }

                                        // Calculate next date
                                        if (j == 0 && offByOne) {
                                            recurCal.add(Calendar.YEAR, -1);
                                        }
                                        recurCal.add(Calendar.YEAR, interval);
                                        recurCal.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                                        recurCal.set(Calendar.DAY_OF_WEEK_IN_MONTH, dayOfWeekInMonth);
                                        currIndex = getDiff(recurCal.getTime(), mStartDate);
                                    }
                                }
                            }
                        }
                        if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_SPECIFIC)) {
                            String[] daysStrs = recur.getString(YearlyRecurFragment.EXTRA_DAYS)
                                    .split(",");
                            int[] days = new int[daysStrs.length];

                            for (int i = 0; i < days.length; i++) {
                                days[i] = Integer.parseInt(daysStrs[i]);
                            }

                            for (int i = 0; i < months.length; i++) {
                                if (months[i]) {
                                    boolean oneLess = (i - recurCal.get(Calendar.MONTH)) < 0;
                                    boolean offByOne = oneLess && (interval != 1);
                                    recurCal = Calendar.getInstance();
                                    recurCal.setTime(start);
                                    recurCal.add(Calendar.MONTH,
                                            (i - recurCal.get(Calendar.MONTH) + 12) % 12);

                                    if (until) {
                                        numTimes = Integer.MAX_VALUE;
                                    }
                                    else {
                                        endDate.setTime(Long.MAX_VALUE);
                                    }

                                    for (int j = 0; j < numTimes && !recurCal.getTime()
                                            .after(endDate); j++) {
                                        for (int day : days) {
                                            if (day <= recurCal.getActualMaximum
                                                    (Calendar.DAY_OF_MONTH)) {
                                                recurCal.set(Calendar.DAY_OF_MONTH, day);
                                                currIndex = getDiff(recurCal.getTime(), mStartDate);

                                                if (!recurCal.getTime().before(start) &&
                                                        !recurCal.getTime().after(endDate) &&
                                                        !(j == 0 && offByOne) && !(j == numTimes - 1
                                                        && oneLess && !offByOne)) {
                                                    Event toAdd = new Event(name, recurCal.getTime(),
                                                            ttc);
                                                    for (int k = mEventSchedule.size(); k <=
                                                            currIndex; k++) {
                                                        mEventSchedule.add(new ArrayList<>());
                                                    }
                                                    mEventSchedule.get(currIndex).add(toAdd);
                                                    mNumEvents++;
                                                }
                                            }
                                        }

                                        if (j == 0 && offByOne) {
                                            recurCal.add(Calendar.YEAR, -1);
                                        }
                                        recurCal.add(Calendar.YEAR, interval);
                                    }
                                }
                            }
                        }
                    }
                }
                else {
                    for (int i = mEventSchedule.size(); i <= diff; i++) {
                        mEventSchedule.add(new ArrayList<>());
                    }

                    Event toAdd = new Event(name, start, ttc);
                    mEventSchedule.get(diff).add(toAdd);
                    mNumEvents++;
                }
            }
            // If the item type is Task
            else if (type.equals(AddItem.EXTRA_VAL_TASK)) {
                // Get the fields from the Bundle
                String name = result.getString(AddItem.EXTRA_NAME);
                int timeToComplete = Integer.parseInt(result.getString(AddItem.EXTRA_TTC));
                String ecd = result.getString(AddItem.EXTRA_ECD);
                String dd = result.getString(AddItem.EXTRA_DUE);
                String parents = result.getString(AddItem.EXTRA_PARENTS);

                // Convert the earliest completion date String to a MyTime
                Date early;
                try {
                    early = Task.dateFormat.parse(ecd);
                }
                catch (Exception e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                // Convert the due date String to a MyTime
                Date due;
                try {
                    due = Task.dateFormat.parse(dd);
                }
                catch (Exception e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                Task toAdd = new Task(name, early, due, timeToComplete);

                // The parents string in the Bundle is a String of the format "n1,n2,n3,...nN,"
                // where each nx is an index to a Task in tasks that should be used as a parent
                // for the task to be added.
                String[] parentIndices = parents.split(",");
                if (!parentIndices[0].equals("-1")) {
                    for (String parentIndex : parentIndices) {
                        Task parent = mTasks.get(Integer.parseInt(parentIndex));
                        toAdd.addParent(parent);
                        parent.addChild(toAdd);
                    }
                }

                mTasks.add(toAdd);
            }

            // As the task dependency graph has been updated, we must reoptimize it
            Optimize(true);
            // Show recycler as Optimize is finished
            mVF.setDisplayedChild(1);
        }
        else {
            if (mTaskSchedule.size() != 0 || mEventSchedule.size() != 0) {
                mVF.setDisplayedChild(1);
            }
            else {
                mVF.setDisplayedChild(2);
            }
        }
    }

    /**
     * Calls the Optimizer to find an optimal schedule for the user's tasks, given the user's
     * scheduled events.
     *
     * @param refresh true refreshes recycler, false doesn't allows us to call Optimize() before
     *                recycler initialization
     */
    private void Optimize(boolean refresh) {
        Optimizer opt = new Optimizer();
        opt.Optimize(mTasks, mTaskSchedule, mEventSchedule, mStartDate, mTodayTime);

        // Delete any empty lists at the end of the taskSchedule.
        int taskLowIndex = mTaskSchedule.size();
        for (int i = mTaskSchedule.size() - 1; i >= 0; i--) {
            if (mTaskSchedule.get(i).size() != 0) {
                break;
            }
            else {
                taskLowIndex--;
            }
        }
        if (mTaskSchedule.size() > taskLowIndex) {
            mTaskSchedule.subList(taskLowIndex, mTaskSchedule.size()).clear();
        }

        // Delete any empty lists at the end of the eventSchedule.
        int eventLowIndex = mEventSchedule.size();
        for (int i = mEventSchedule.size() - 1; i >= 0; i--) {
            if (mEventSchedule.get(i).size() != 0) {
                break;
            }
            else {
                eventLowIndex--;
            }
        }
        if (mEventSchedule.size() > eventLowIndex) {
            mEventSchedule.subList(eventLowIndex, mEventSchedule.size()).clear();
        }

        // As the Optimizer may have changed tasks' dates, we must refresh the recycler
        if (refresh) {
            mDayItemAdapter.mDayItemList = DayItemList();
            mDayItemAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Runs on the start of the app. Most importantly it loads the user data from the file.
     *
     * @param savedInstanceState Not used.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create layout
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        // startDate is our representation for the current date upon the launch of TaskApp.
        mStartDate = clearDate(new Date());

        mNumEvents = 0;
        mTodayTime = 0;

        try {
            // Populate from file
            Scanner fileRead = new Scanner(new File(getFilesDir() + "state.tsk"));

            // Get todayTime from the file to make sure the optimizer doesn't overschedule today
            String todTimeString = fileRead.nextLine();
            String[] todTimeSplit = todTimeString.split(": ");
            if (mStartDate.getTime() == Long.parseLong(todTimeSplit[0])) {
                mTodayTime = Integer.parseInt(todTimeSplit[1]);
            }

            String ntString = fileRead.nextLine();

            // Read the task count from the file
            int numTasks = Integer.parseInt(ntString);
            ArrayList<Task> overdueTasks = new ArrayList<>();

            // Read in all the tasks from the file, in the general format:
            // Task Name|earlyDate|dueDate|doDate|timeToComplete|head,parent1,parent2,etc.\n
            for (int i = 0; i < numTasks; i++) {
                String temp = fileRead.nextLine();

                String[] data = temp.split("\\|");

                // Read in values from file
                String name = data[0];
                Date earlyDate = new Date(Long.parseLong(data[1]));
                Date dueDate = new Date(Long.parseLong(data[2]));
                Date doDate = new Date(Long.parseLong(data[3]));
                int timeToComplete = Integer.parseInt(data[4]);
                String parents = data[5];
                String[] parentList = parents.split(",");

                // If earlyDate has past, set today as the earlyDate
                if (earlyDate.before(mStartDate)) {
                    earlyDate = mStartDate;
                }

                // Create the task
                Task toAdd = new Task(name, earlyDate, dueDate, timeToComplete);
                toAdd.setDoDate(doDate);

                // Add parents based on the parent list. Start at 1 as 0 is the head
                // (there for ease of writing)
                for (int j = 1; j < parentList.length; j++) {
                    toAdd.addParent(mTasks.get(Integer.parseInt(parentList[j])));
                    mTasks.get(Integer.parseInt(parentList[j])).addChild(toAdd);
                }

                mTasks.add(toAdd);

                // Calculate how many days past today's date this task is scheduled for. Used to
                // index into taskSchedule
                int doDateIndex = getDiff(toAdd.getDoDate(), mStartDate);

                // Adds file to taskSchedule if it is scheduled for today or later.
                if (doDateIndex >= 0) {
                    for (int j = mTaskSchedule.size(); j <= doDateIndex; j++) {
                        mTaskSchedule.add(new ArrayList<>());
                    }

                    mTaskSchedule.get(doDateIndex).add(toAdd);
                }

                // If a task is overdue, add it to the overdue list so the user can mark it as
                // complete or not.
//                if (doDate.getDateTime() < mStartDate.getDateTime()) {
                if (doDate.before(mStartDate)) {
                    overdueTasks.add(toAdd);
                }
            }

            // Prompt the user with a dialog containing overdue tasks so they can mark overdue tasks
            // so taskapp can reoptimize the schedule if some tasks are overdue.
            if (overdueTasks.size() != 0) {
                String[] overdueNames = new String[overdueTasks.size()];

                // Create a list of overdue task names for the dialog
                for (int i = 0; i < overdueNames.length; i++) {
                    Task t = overdueTasks.get(i);
                    Date tDate = t.getDueDate();
                    overdueNames[i] = String.format(getString(R.string.due_when), t.getName(),
                            Task.dateFormat.format(tDate));
                }

                // List of indices to tasks that were completed.
                ArrayList<Integer> selectedItems = new ArrayList<>();

                // Show a dialog prompting the user to mark tasks that were completed as complete
                android.app.AlertDialog.Builder builder =
                        new android.app.AlertDialog.Builder(this);
                builder.setTitle(R.string.overdue_dialog_title)
                        .setMultiChoiceItems(overdueNames, null,
                                new DialogInterface.OnMultiChoiceClickListener() {

                                    /**
                                     * Adds the selected task to the toRemove list, or removes it if
                                     * the task was unselected
                                     *
                                     * @param dialogInterface not used
                                     * @param index           the index into the tasks ArrayList of
                                     *                        the parent
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
                                        // As the user has marked these tasks as completed, remove
                                        // them. Pass in false as the user completed them on a prior
                                        // day.
                                        for (int i = 0; i < selectedItems.size(); i++) {
                                            Complete(overdueTasks.get(selectedItems.get(i)),
                                                    false, false);
                                        }

                                        // Change due date for overdue tasks if it has already been
                                        // passed to today.
                                        if (overdueTasks.size() != selectedItems.size()) {
                                            for (int i = 0; i < overdueTasks.size(); i++) {
                                                if (selectedItems.contains(i)) {
                                                    continue;
                                                }

                                                Task t = overdueTasks.get(i);

                                                if (t.getDueDate().before(mStartDate)) {
                                                    t.setDueDate(mStartDate);
                                                }
                                            }
                                        }

                                        finishProcessing(fileRead, true);
                                    }
                                });

                builder.create();
                builder.show();

            }
            else {
                finishProcessing(fileRead, false);
            }
        }
        catch (Exception e) {
            Log.d("TaskApp.MainActivity", "Storage file empty/misformatted");
            // Initialize the main recyclerview with data calculated in helper function DayItemList
            RecyclerView dayRecyclerView = findViewById(R.id.main_recyclerview);
            LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
            mDayItemAdapter = new DayItemAdapter(DayItemList(), this);
            dayRecyclerView.setAdapter(mDayItemAdapter);
            dayRecyclerView.setLayoutManager(layoutManager);

            // Adds the action bar at the top of the screen
            setSupportActionBar(mBinding.toolbar);

            // When the FAB is clicked, run intentAddItem to open the AddItem Activity
            mBinding.fab.setOnClickListener(view -> intentAddItem());

            // Make visible the main content
            mVF = findViewById(R.id.vf);
            if (mTaskSchedule.size() != 0 || mEventSchedule.size() != 0) {
                mVF.setDisplayedChild(1);
            }
            else {
                mVF.setDisplayedChild(2);
            }

            mStartForResult = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> MainActivity.this.onActivityResult(result.getResultCode(),
                            result.getData()));
        }
    }

    /**
     * Extremely sketchy, but allows us to wait to finish processing events and displaying the
     * recycler until the user has completed the overdue tasks dialog
     * @param fileRead The scanner used to read the state file
     */
    private void finishProcessing(Scanner fileRead, boolean reoptimize) {
        try {
            // Read the number of events in from the file.
            mNumEvents = fileRead.nextInt();
            fileRead.nextLine();
            // Allows us to decrement numEvents without messing up iteration
            int numEventsCopy = mNumEvents;

            // Read in all the events in the file, with the general format of:
            // Event Name|Time to Complete|Start Time\n
            for (int i = 0; i < numEventsCopy; i++) {
                String temp = fileRead.nextLine();

                String[] data = temp.split("\\|");

                // Load in the fields
                String name = data[0];
                int length = Integer.parseInt(data[1]);
                Date doDate = new Date(Long.parseLong(data[2]));

                Event toAdd = new Event(name, doDate, length);

                // Calculate how many days past today's date this event is scheduled for. Used to
                // index into eventSchedule
                int doDateIndex = getDiff(toAdd.getDoDate(), mStartDate);

                // Add the events to the list if they aren't for an earlier date
                if (!doDate.before(mStartDate)) {
                    for (int j = mEventSchedule.size(); j <= doDateIndex; j++) {
                        mEventSchedule.add(new ArrayList<>());
                    }

                    mEventSchedule.get(doDateIndex).add(toAdd);
                }
                else {
                    mNumEvents--;
                }
            }

            fileRead.close();
        }
        catch (Exception e) {
            Log.d("TaskApp.MainActivity", "Storage file empty/misformatted");
        }

        // If tasks were changed, make sure to reoptimize the schedule in case it's necessary
        if (reoptimize) {
            Optimize(false);
        }

        // Initialize the main recyclerview with data calculated in helper function DayItemList
        RecyclerView dayRecyclerView = findViewById(R.id.main_recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
        mDayItemAdapter = new DayItemAdapter(DayItemList(), this);
        dayRecyclerView.setAdapter(mDayItemAdapter);
        dayRecyclerView.setLayoutManager(layoutManager);

        // Adds the action bar at the top of the screen
        setSupportActionBar(mBinding.toolbar);

        // When the FAB is clicked, run intentAddItem to open the AddItem Activity
        mBinding.fab.setOnClickListener(view -> intentAddItem());

        // Make visible the main content
        mVF = findViewById(R.id.vf);

        if (mTaskSchedule.size() != 0 || mEventSchedule.size() != 0) {
            mVF.setDisplayedChild(1);
        }
        else {
            mVF.setDisplayedChild(2);
        }

        mStartForResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> MainActivity.this.onActivityResult(result.getResultCode(),
                        result.getData()));
    }

    /**
     * Launches the AddItem activity. Must be separate function so FAB handler can call it.
     */
    private void intentAddItem() {
        Intent intent = new Intent(this, AddItem.class);

        // Create an ArrayList of task names so the multiselect dialog can use it to choose parent
        // tasks
        ArrayList<String> taskNames = new ArrayList<>();
        for (Task t : mTasks) {
            Date tDate = t.getDueDate();
            taskNames.add(String.format(getString(R.string.due_when), t.getName(),
                    Task.dateFormat.format(tDate)));
        }

        intent.putStringArrayListExtra(EXTRA_TASKS, taskNames);
        mStartForResult.launch(intent);
    }

    /**
     * Saves internal data structures to file to be rebuilt in onCreate()
     *
     * @param outState unused, required by override
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save data structure to file
        try {
            BufferedWriter out = new BufferedWriter(
                    new FileWriter(getFilesDir() + "state.tsk"));
            // Save todayTime to file to prevent from overscheduling on today's date
            out.write(mStartDate.getTime() + ": " +
                    mTodayTime + "\n");
            out.write(mTasks.size() + "\n");

            // Add all tasks to file, in the structure described in onCreate.
            for (int i = 0; i < mTasks.size(); i++) {
                Task t = mTasks.get(i);
                StringBuilder parents = new StringBuilder("head,");

                for (int j = 0; j < t.getParents().size(); j++) {
                    parents.append(mTasks.indexOf(t.getParents().get(j))).append(",");
                }

                String taskLine = t.getName() + "|" + t.getEarlyDate().getTime() + "|" +
                        t.getDueDate().getTime() + "|" + t.getDoDate().getTime() + "|" +
                        t.getTimeToComplete() + "|" + parents + "\n";

                out.write(taskLine);
            }

            // Write the number of scheduled events to the file.
            out.write(mNumEvents + "\n");

            // Add all events to file, in the structure described in onCreate
            for (int i = 0; i < mEventSchedule.size(); i++) {
                for (int j = 0; j < mEventSchedule.get(i).size(); j++) {
                    Event e = mEventSchedule.get(i).get(j);

                    out.write(e.getName() + "|" +
                            e.getLength() + "|" + e.getDoDate().getTime() +
                            "\n");
                }
            }

            out.close();
        }
        catch (Exception e) {
            Log.e("TaskApp.MainActivity", e.toString());
        }
    }

    /**
     * Builds a DayItem List representation of a user's tasks/events
     *
     * @return a DayItem List representation of a user's tasks/events
     */
    private List<DayItem> DayItemList() {
        // The list of DayItem's to be displayed in the recycler
        List<DayItem> itemList = new ArrayList<>();

        // Generate a DayItem for the date i days past today's date
        for (int i = 0; i < mTaskSchedule.size() || i < mEventSchedule.size(); i++) {
            // Fields for the DayItem
            String dayString;
            List<EventItem> events;
            List<TaskItem> tasks;

            // MyTime representing the date i days past today's date
            Calendar currCal = Calendar.getInstance();
            currCal.add(Calendar.DAY_OF_YEAR, i);
            Date curr = clearDate(currCal.getTime());

            // Number representing totalTime this date has scheduled. If it's today's date, add
            // todayTime to represent the time already completed tasks took.
            int totalTime = (i == 0) ? mTodayTime : 0;

            // Adds the total event time for the day to the total time
            if (i < mEventSchedule.size() && mEventSchedule.get(i).size() > 0) {
                for (int j = 0; j < mEventSchedule.get(i).size(); j++) {
                    Event event = mEventSchedule.get(i).get(j);
                    totalTime += event.getLength();
                }
            }

            // Adds the total task time for the day to the total time
            if (i < mTaskSchedule.size() && mTaskSchedule.get(i).size() > 0) {
                for (int j = 0; j < mTaskSchedule.get(i).size(); j++) {
                    Task task = mTaskSchedule.get(i).get(j);

                    totalTime += task.getTimeToComplete();
                }
            }

            // Set the fields
            dayString = String.format(getString(R.string.schedule_for), Task.dateFormat.format(curr),
                    totalTime);
            events = EventItemList(i);
            tasks = TaskItemList(i);

            itemList.add(new DayItem(dayString, events, tasks, i));
        }

        return itemList;
    }

    /**
     * Builds an EventItem List representation of a user's events on a given day
     *
     * @param index The index into the data structure representing the day
     *
     * @return an EventItem List representation of a user's events on a given day
     */
    private List<EventItem> EventItemList(int index) {
        // The list of EventItems representing the events scheduled for index days past today's date
        List<EventItem> itemList = new ArrayList<>();

        // Add all the events scheduled for the given day to itemList, if any are scheduled
        if (index < mEventSchedule.size() && mEventSchedule.get(index).size() > 0) {
            for (int j = 0; j < mEventSchedule.get(index).size(); j++) {
                // Fields for itemList
                String name;
                String timespan;

                // Get the jth event from the given date
                Event event = mEventSchedule.get(index).get(j);

                // Get the start/end time in MyTime objects
                Date eventTime = event.getDoDate();
                Calendar endCal = Calendar.getInstance();
                endCal.setTime(eventTime);
                endCal.add(Calendar.MINUTE, event.getLength());
                Date endTime = clearTime(endCal.getTime());

                // Format the event name as Name: StartTime-EndTime
                name = event.getName();
                timespan = Event.timeFormat.format(eventTime) + "-" +
                        Event.timeFormat.format(endTime);

                itemList.add(new EventItem(name, timespan, j));
            }
        }
        return itemList;
    }

    /**
     * Builds a TaskItem List representation of a user's tasks on a given day
     *
     * @param index The index into the data structure representing the day
     *
     * @return a TaskItem List representation of a user's tasks on a given day
     */
    private List<TaskItem> TaskItemList(int index) {
        // The list of TaskItems representing the tasks scheduled for the date index days past
        // today's date
        List<TaskItem> itemList = new ArrayList<>();

        // Add all the tasks scheduled for the given date to itemList
        if (index < mTaskSchedule.size() && mTaskSchedule.get(index).size() > 0) {
            for (int j = 0; j < mTaskSchedule.get(index).size(); j++) {
                // DayItem's only field
                String name;

                // Get the jth task scheduled for the given day.
                Task task = mTaskSchedule.get(index).get(j);

                // Create the name in the format NAME (TTC minutes to complete)
//                name = task.getName() + " (" + task.getTimeToComplete() +
//                        " " + getString(R.string.minutes_to_complete) + ")";

                name = task.getName() + "\n" + String.format(getString(R.string.minutes_to_complete),
                        task.getTimeToComplete());

                itemList.add(new TaskItem(name, j));
            }
        }

        return itemList;
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
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
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
        // Remove the given task from the task dependency graph
        mVF.setDisplayedChild(0);
        if (action == 0) {
            if (day == -1 || mTaskSchedule.get(day).size() <= position) {
                mVF.setDisplayedChild(1);
                return;
            }

            Complete(mTaskSchedule.get(day).get(position), true, true);
        }
        // Remove the given task from the task dependency graph without completion time dialog
        if (action == 1) {
            if (day == -1 || mTaskSchedule.get(day).size() <= position) {
                mVF.setDisplayedChild(1);
                return;
            }
            Complete(mTaskSchedule.get(day).get(position), false, true);
        }
        // Remove the given event from the schedule and re-optimize.
        if (action == 2) {
            if (day == -1 || mEventSchedule.get(day).size() <= position) {
                mVF.setDisplayedChild(1);
                return;
            }
            mEventSchedule.get(day).remove(position);
            Optimize(true);
        }

        if (mTaskSchedule.size() != 0 || mEventSchedule.size() != 0) {
            mVF.setDisplayedChild(1);
        }
        else {
            mVF.setDisplayedChild(2);
        }

        //noinspection unchecked
        HideBottomViewOnScrollBehavior<FloatingActionButton> fabBehavior =
                (HideBottomViewOnScrollBehavior<FloatingActionButton>)
                        ((CoordinatorLayout.LayoutParams) mBinding.fab.getLayoutParams())
                                .getBehavior();

        if (fabBehavior != null) {
            fabBehavior.slideUp(mBinding.fab);
        }
    }
}