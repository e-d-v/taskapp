package com.evanv.taskapp.logic;

import static com.evanv.taskapp.logic.Event.clearTime;
import static com.evanv.taskapp.logic.Task.clearDate;
import static com.evanv.taskapp.logic.Task.getDiff;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.evanv.taskapp.R;
import com.evanv.taskapp.db.TaskAppViewModel;
import com.evanv.taskapp.ui.additem.AddItem;
import com.evanv.taskapp.ui.additem.recur.DailyRecurFragment;
import com.evanv.taskapp.ui.additem.recur.MonthlyRecurFragment;
import com.evanv.taskapp.ui.additem.recur.NoRecurFragment;
import com.evanv.taskapp.ui.additem.recur.RecurActivity;
import com.evanv.taskapp.ui.additem.recur.RecurInput;
import com.evanv.taskapp.ui.additem.recur.WeeklyRecurFragment;
import com.evanv.taskapp.ui.additem.recur.YearlyRecurFragment;
import com.evanv.taskapp.ui.main.MainActivity;
import com.evanv.taskapp.ui.main.recycler.DayItem;
import com.evanv.taskapp.ui.main.recycler.EventItem;
import com.evanv.taskapp.ui.main.recycler.TaskItem;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import kotlin.Pair;

/**
 * The subsystem that handles the logic for the app. All items/references are worked with here.
 *
 * @author Evan Voogd
 */
public class LogicSubsystem {
    private final Date mStartDate;              // The current date
    private int mTodayTime;                     // The time spent completing tasks today
    private List<Task> mTasks;                  // List of all tasks for user
    private TaskAppViewModel mTaskAppViewModel; // ViewModel to interact with Database
    private final MainActivity mMainActivity;   // MainActivity for resources
    private List<Task> overdueTasks;            // Overdue tasks

    // taskSchedule[i] represents the list of tasks for the day i days past startDate
    private final ArrayList<ArrayList<Task>> mTaskSchedule = new ArrayList<>();
    // eventSchedule[i] represents the list of events for the day i days past startDate
    private final ArrayList<ArrayList<Event>> mEventSchedule = new ArrayList<>();

    /**
     * Creates a new LogicSubsystem and loads data from database into internal data structures.
     *
     * @param mainActivity The calling MainActivity. Don't love this from a coupling perspective,
     *                     but due to Android's design, it's necessary to extract resources.
     * @param todayTime The amount of time spent completing tasks so far today.
     */
    public LogicSubsystem(MainActivity mainActivity, int todayTime) {
        this.mMainActivity = mainActivity;
        this.mTodayTime = todayTime;

        // startDate is our representation for the current date upon the launch of TaskApp.
        mStartDate = clearDate(new Date());

        mTodayTime = 0;

        ViewModelStoreOwner vmso = mMainActivity;

        // Populate from database;
        Thread thread = new Thread() {
            public void run() {
                mTaskAppViewModel = new ViewModelProvider(vmso).get(TaskAppViewModel.class);
            }
        };
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Get tasks from database
        mTasks = mTaskAppViewModel.getAllTasks();
        mTasks = (mTasks == null) ? new ArrayList<>() : mTasks;

        overdueTasks = new ArrayList<>(); // Tasks that are overdue.

        // Add tasks to taskSchedule/add parents
        for (Task t : mTasks) {
            // Calculate how many days past today's date this task is scheduled for. Used to
            // index into taskSchedule
            Date doDate = t.getDoDate();
            int index = getDiff(doDate, mStartDate);

            if (t.getEarlyDate().before(mStartDate)) {
                t.setEarlyDate(mStartDate);
                mTaskAppViewModel.update(t);
            }

            // Adds file to taskSchedule if it is scheduled for today or later.
            if (index >= 0) {
                // Make sure taskSchedule is big enough
                for (int i = mTaskSchedule.size(); i <= index; i++) {
                    mTaskSchedule.add(new ArrayList<>());
                }

                // Add to taskSchedule
                mTaskSchedule.get(index).add(t);
            } else {
                overdueTasks.add(t);
            }
        }

        // Add parent/child structure to task lists, as Room DB cannot do this
        for (Task t : mTasks) {
            if (t.getParentArr().size() != 0) {
                for (Task other : mTasks) {
                    if (t.getParentArr().contains(other.getID())) {
                        t.addParent(other);
                        other.addChild(t);
                    }
                }
            }
        }
    }

    /**
     * Gets a list of overdue tasks. Only works in the beginning of the app lifecycle, else returns
     * null. This is due to the fact that all overdue tasks need to be handled for the app to begin
     * operation.
     *
     * @return Return list of currently unhandled overdue tasks.
     */
    public String[] getOverdueTasks() {
        if (overdueTasks == null) {
            return null;
        }

        String[] overdueNames = new String[overdueTasks.size()];

        // Create a list of overdue task names for the dialog
        for (int i = 0; i < overdueNames.length; i++) {
            Task t = overdueTasks.get(i);

            if (t.getDoDate().getTime() == 0) {
                mTasks.remove(t);
                mTaskAppViewModel.delete(t);
                continue;
            }

            Date tDate = t.getDueDate();
            overdueNames[i] = String.format(mMainActivity.getString(R.string.due_when), t.getName(),
                    Task.dateFormat.format(tDate));
        }

        return overdueNames;
    }

    /**
     * Handle user input on overdue tasks - mark tasks as either completed, or to be completed
     * today.
     *
     * @param completedItems Indices into the overdue task names array that the user marked as
     *                       completed.
     */
    public void updateOverdueTasks(List<Integer> completedItems) {
        // As the user has marked these tasks as completed, remove them.
        for (int i = 0; i < completedItems.size(); i++) {
            Complete(overdueTasks.get(completedItems.get(i)));
        }

        // Change due date for overdue tasks if it has already been passed to today.
        if (overdueTasks.size() != completedItems.size()) {
            for (int i = 0; i < overdueTasks.size(); i++) {
                if (completedItems.contains(i)) {
                    continue;
                }

                Task t = overdueTasks.get(i);

                if (t.getDueDate().before(mStartDate)) {
                    t.setDueDate(mStartDate);
                }
            }
        }

        overdueTasks = null;
    }

    /**
     * Overdue tasks have been handled, so now we're ready to start the app. Load events from
     * internal database and optimize if necessary.
     *
     * @param reoptimize If there was overdue tasks, reoptimize to account for them.
     *
     * @return A list of DayItems to be used by MainActivity's recycler.
     */
    public List<DayItem> prepForDisplay(boolean reoptimize) {
        // Get the event list.
        List<Event> events = mTaskAppViewModel.getAllEvents();

        events = (events == null) ? new ArrayList<>() : events;

        // Add events from the database into the eventSchedule
        for (Event e : events) {
            // Calculate how many days past today's date this event is scheduled for. Used to
            // index into eventSchedule
            Date doDate = e.getDoDate();
            int doDateIndex = getDiff(doDate, mStartDate);

            // Add the events to the list if they aren't for an earlier date
            if (!doDate.before(mStartDate)) {
                for (int j = mEventSchedule.size(); j <= doDateIndex; j++) {
                    mEventSchedule.add(new ArrayList<>());
                }

                mEventSchedule.get(doDateIndex).add(e);
            }
        }

        // If tasks were changed, make sure to reoptimize the schedule in case it's necessary
        if (reoptimize) {
            Optimize();
        }

        return DayItemList();
    }

    /**
     * Removes a task from the task dependency graph.
     *
     * @param task The task to be removed from the task dependency graph
     */
    public void Complete(Task task) {
        mTasks.remove(task);

        Date doDate = task.getDoDate();

        // Get the number of days past the start date this task is scheduled for, so we can get the
        // index of the taskSchedule member for it's do date.
        int diff = getDiff(doDate, mStartDate);

        // If the task is in the internal data structure, remove it.
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

        DayItemHelper(diff);
    }

    /**
     * Add time to current amount of time spent completing tasks.
     *
     * @param timeToAdd Amount of time to add.
     */
    public void addTodayTime(int timeToAdd) {
        mTodayTime += timeToAdd;
    }

    /**
     * Return true if the app has no tasks/events currently being tracked.
     *
     * @return true if app's internal data structures are empty.
     */
    public boolean isEmpty() {
        return mTaskSchedule.size() == 0 && mEventSchedule.size() == 0;
    }

    /**
     * TODO: The absolute monstrosity that is addItem. Very flawed - the vast majority of the logic
     * (the recurrence logic) should be a separate class (and has a lot of repeated code), so this
     * iteration will be drastically changed soon.
     *
     * Adds an item to the internal data structure based on the information passed in by the AddItem
     * activity.
     *
     * @param data Information passed in by the AddItem activity.
     *
     * @return Indices whose recycler positions need to be updated.
     */
    public List<Integer> addItem(@Nullable Intent data) {
        // Get the data to build the item
        Bundle result = Objects.requireNonNull(data).getBundleExtra(AddItem.EXTRA_ITEM);
        String type = result.getString(AddItem.EXTRA_TYPE);

        List<Integer> updatedIndices = new ArrayList<>();

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
                    return null;
                }
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
                return null;
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
                        updatedIndices.add(i);
                    }

                    // Add the event once every interval days.
                    for (int i = 0; i < (numTimes * interval); i += interval) {
                        // Calculate date to add event on.
                        Calendar recurCal = Calendar.getInstance();
                        recurCal.setTime(start);
                        recurCal.add(Calendar.DAY_OF_YEAR, i);

                        Event toAdd = new Event(name, ttc, recurCal.getTime());
                        mEventSchedule.get(diff + i).add(toAdd);
                        mTaskAppViewModel.insert(toAdd);
                        updatedIndices.add(diff + i);
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
                                    Event toAdd = new Event(name, ttc, recurCal.getTime());

                                    for (int k = mEventSchedule.size(); k <= currIndex; k++) {
                                        mEventSchedule.add(new ArrayList<>());
                                        updatedIndices.add(k);
                                    }

                                    mEventSchedule.get(currIndex).add(toAdd);
                                    mTaskAppViewModel.insert(toAdd);
                                    updatedIndices.add(currIndex);
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
                // User chose to recur monthly
                else if (MonthlyRecurFragment.EXTRA_VAL_TYPE.equals(recurType)) {
                    // Get recur interval
                    int interval = recur.getInt(MonthlyRecurFragment.EXTRA_INTERVAL);
                    String intervalType = recur.getString(MonthlyRecurFragment.EXTRA_RECUR_TYPE);

                    Calendar recurCal = Calendar.getInstance();
                    recurCal.setTime(start);

                    int currIndex = diff;

                    // User chose to recur on the same day every month
                    if (intervalType.equals(MonthlyRecurFragment.EXTRA_VAL_STATIC)) {
                        // Calculate when to recur until
                        if (!until) {
                            Calendar endDateCal = Calendar.getInstance();
                            endDateCal.setTime(start);
                            endDateCal.add(Calendar.MONTH, interval * (numTimes - 1));
                            endDate = endDateCal.getTime();
                            endIndex = getDiff(endDate, mStartDate);
                        }

                        // Ensure there is enough space in the eventSchedule
                        for (int i = mEventSchedule.size(); i <= endIndex; i++) {
                            mEventSchedule.add(new ArrayList<>());
                            updatedIndices.add(i);
                        }

                        // Add an event at a consistent interval until end date is reached
                        while (!recurCal.getTime().after(endDate)) {
                            // Add event to schedule
                            Event toAdd = new Event(name, ttc, recurCal.getTime());
                            mEventSchedule.get(currIndex).add(toAdd);
                            mTaskAppViewModel.insert(toAdd);
                            updatedIndices.add(currIndex);

                            // Calculate next date
                            recurCal.add(Calendar.MONTH, interval);
                            currIndex = getDiff(recurCal.getTime(), mStartDate);
                        }
                    }
                    // If user chose to recur on the same weekday of every month (e.g. 3rd tuesday)
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
                            Event toAdd = new Event(name, ttc, recurCal.getTime());

                            for (int j = mEventSchedule.size(); j <= currIndex; j++) {
                                mEventSchedule.add(new ArrayList<>());
                                updatedIndices.add(j);
                            }

                            mEventSchedule.get(currIndex).add(toAdd);
                            mTaskAppViewModel.insert(toAdd);
                            updatedIndices.add(currIndex);

                            // Calculate next date
                            recurCal.add(Calendar.MONTH, interval);
                            recurCal.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                            recurCal.set(Calendar.DAY_OF_WEEK_IN_MONTH, dayOfWeekInMonth);
                            currIndex = getDiff(recurCal.getTime(), mStartDate);
                        }
                    }
                    // If user chose to recur on the same dates of every month (e.g. 2nd and 3rd)
                    if (intervalType.equals(MonthlyRecurFragment.EXTRA_VAL_SPECIFIC)) {
                        String[] strs = recur.getString(MonthlyRecurFragment.EXTRA_DAYS)
                                .split(",");
                        // Get all the days from the bundle
                        int[] days = new int[strs.length];

                        for (int i = 0; i < strs.length; i++) {
                            days[i] = Integer.parseInt(strs[i]);
                        }

                        Arrays.sort(days);

                        // For each day, add all of it's occurrences to the taskSchedule
                        for (int day : days) {
                            // Calculate the first day the task occurs on this day of month
                            recurCal.setTime(start);
                            int daysInMonth = recurCal.getActualMaximum(Calendar.DAY_OF_MONTH);
                            int dayDiff = (day - recurCal.get(Calendar.DAY_OF_MONTH)
                                    + daysInMonth) % daysInMonth;

                            // Set these fields to help with edge cases
                            boolean oneLess = (day - recurCal.get(Calendar.DAY_OF_MONTH)) < 0;
                            boolean offByOne = oneLess && (interval != 1);

                            // Calculate the Date based on the first day he task occurs on
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
                                    Event toAdd = new Event(name, ttc, recurCal.getTime());
                                    for (int k = mEventSchedule.size(); k <= currIndex; k++) {
                                        mEventSchedule.add(new ArrayList<>());
                                        updatedIndices.add(currIndex);
                                    }
                                    mEventSchedule.get(currIndex).add(toAdd);
                                    mTaskAppViewModel.insert(toAdd);
                                    updatedIndices.add(currIndex);
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
                // The user chose to recur yearly
                else if (YearlyRecurFragment.EXTRA_VAL_TYPE.equals(recurType)) {
                    // How many years between each recurrence of this event.
                    int interval = recur.getInt(YearlyRecurFragment.EXTRA_INTERVAL);
                    // How the event will recur (the 18th, 3rd monday, 18/21st etc.)
                    String intervalType = recur.getString(YearlyRecurFragment.EXTRA_RECUR_TYPE);
                    // What months to recur on if necessary.
                    boolean[] months = new boolean[12];

                    // If necessary, see which months the user chose to recur on
                    if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_MULTIPLE_DYNAMIC)
                            || intervalType.equals(YearlyRecurFragment.EXTRA_VAL_MULTIPLE_STATIC)
                            || intervalType.equals(YearlyRecurFragment.EXTRA_VAL_SPECIFIC)) {
                        List<String> monthsStr = Arrays.asList(
                                recur.getString(YearlyRecurFragment.EXTRA_MONTHS).split(","));

                        // For each month, see if the user chose to include it
                        months[0] = monthsStr.contains(mMainActivity.getString(R.string.jan));
                        months[1] = monthsStr.contains(mMainActivity.getString(R.string.feb));
                        months[2] = monthsStr.contains(mMainActivity.getString(R.string.mar));
                        months[3] = monthsStr.contains(mMainActivity.getString(R.string.apr));
                        months[4] = monthsStr.contains(mMainActivity.getString(R.string.may));
                        months[5] = monthsStr.contains(mMainActivity.getString(R.string.jun));
                        months[6] = monthsStr.contains(mMainActivity.getString(R.string.jul));
                        months[7] = monthsStr.contains(mMainActivity.getString(R.string.aug));
                        months[8] = monthsStr.contains(mMainActivity.getString(R.string.sep));
                        months[9] = monthsStr.contains(mMainActivity.getString(R.string.oct));
                        months[10] = monthsStr.contains(mMainActivity.getString(R.string.nov));
                        months[11] = monthsStr.contains(mMainActivity.getString(R.string.dec));
                    }

                    // Initialize calendar
                    Calendar recurCal = Calendar.getInstance();
                    recurCal.setTime(start);

                    int currIndex = diff;

                    // If user chose to recur on same month/day
                    if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_STATIC)) {

                        // Calculate end date
                        if (!until) {
                            Calendar endDateCal = Calendar.getInstance();
                            endDateCal.setTime(start);
                            endDateCal.add(Calendar.YEAR, interval * (numTimes - 1));
                            endDate = endDateCal.getTime();
                        }

                        // Ensure enough space in eventSchedule
                        for (int i = mEventSchedule.size(); i <= endIndex; i++) {
                            mEventSchedule.add(new ArrayList<>());
                            updatedIndices.add(i);
                        }

                        while (!recurCal.getTime().after(endDate)) {
                            // Add event to schedule
                            Event toAdd = new Event(name, ttc, recurCal.getTime());
                            for (int k = mEventSchedule.size(); k <= currIndex; k++) {
                                mEventSchedule.add(new ArrayList<>());
                                updatedIndices.add(k);
                            }
                            mEventSchedule.get(currIndex).add(toAdd);
                            mTaskAppViewModel.insert(toAdd);
                            updatedIndices.add(currIndex);

                            // Calculate next date
                            recurCal.add(Calendar.YEAR, interval);
                            currIndex = getDiff(recurCal.getTime(), mStartDate);
                        }
                    }
                    // If user chose to recur on the same month/weekday (e.g. 3rd Mon of Sep)
                    if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_DYNAMIC)) {
                        int dayOfWeek = recurCal.get(Calendar.DAY_OF_WEEK);
                        int dayOfWeekInMonth = recurCal.get(Calendar.DAY_OF_WEEK_IN_MONTH);

                        // Set the null fields to their max values to ensure they're ignored by the
                        // while loop
                        if (until) {
                            numTimes = Integer.MAX_VALUE;
                        }
                        else {
                            endDate.setTime(Long.MAX_VALUE);
                        }

                        for (int i = 0; i < numTimes && !recurCal.getTime().after(endDate); i++) {
                            // Add event to schedule
                            Event toAdd = new Event(name, ttc, recurCal.getTime());
                            for (int k = mEventSchedule.size(); k <= currIndex; k++) {
                                mEventSchedule.add(new ArrayList<>());
                                updatedIndices.add(k);
                            }
                            mEventSchedule.get(currIndex).add(toAdd);
                            mTaskAppViewModel.insert(toAdd);
                            updatedIndices.add(currIndex);

                            // Calculate next date
                            recurCal.add(Calendar.YEAR, interval);
                            recurCal.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                            recurCal.set(Calendar.DAY_OF_WEEK_IN_MONTH, dayOfWeekInMonth);
                            currIndex = getDiff(recurCal.getTime(), mStartDate);
                        }
                    }
                    // If user chose to recur on multiple months on the same day (e.g. Jan/Feb 3rd)
                    if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_MULTIPLE_STATIC)) {
                        // For each month, check if it is recurred on, and if it is, calculate
                        // events
                        for (int i = 0; i < months.length; i++) {
                            if (months[i]) {
                                // Calculate first year event should be scheduled for
                                boolean oneLess = (i - recurCal.get(Calendar.MONTH)) < 0;
                                boolean offByOne = oneLess && (interval != 1);
                                recurCal = Calendar.getInstance();
                                recurCal.setTime(start);
                                recurCal.add(Calendar.MONTH,
                                        (i - recurCal.get(Calendar.MONTH) + 12) % 12);
                                currIndex = getDiff(recurCal.getTime(), mStartDate);

                                // Set the null fields to their max values to ensure they're ignored
                                // by the while loop
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
                                        Event toAdd = new Event(name, ttc, recurCal.getTime());
                                        for (int k = mEventSchedule.size(); k <= currIndex; k++) {
                                            mEventSchedule.add(new ArrayList<>());
                                            updatedIndices.add(k);
                                        }
                                        mEventSchedule.get(currIndex).add(toAdd);
                                        mTaskAppViewModel.insert(toAdd);
                                        updatedIndices.add(currIndex);
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
                    // If the user chose to recur on multiple months on the same weekday (e.g. 3rd
                    // Monday of Jan/Feb)
                    if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_MULTIPLE_DYNAMIC)) {
                        int dayOfWeek = recurCal.get(Calendar.DAY_OF_WEEK);
                        int dayOfWeekInMonth = recurCal.get(Calendar.DAY_OF_WEEK_IN_MONTH);

                        // For each month, if user chose month to have event, schedule event
                        for (int i = 0; i < months.length; i++) {
                            if (months[i]) {
                                // Calculate first occurrence of event on this month
                                boolean oneLess = (i - recurCal.get(Calendar.MONTH)) < 0;
                                boolean offByOne = oneLess && (interval != 1);
                                recurCal = Calendar.getInstance();
                                recurCal.setTime(start);
                                recurCal.add(Calendar.MONTH,
                                        (i - recurCal.get(Calendar.MONTH) + 12) % 12);
                                recurCal.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                                recurCal.set(Calendar.DAY_OF_WEEK_IN_MONTH, dayOfWeekInMonth);
                                currIndex = getDiff(recurCal.getTime(), mStartDate);

                                // Set the null fields to their max values to ensure they're ignored
                                // by the while loop
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
                                        Event toAdd = new Event(name, ttc, recurCal.getTime());
                                        for (int k = mEventSchedule.size(); k <= currIndex; k++) {
                                            mEventSchedule.add(new ArrayList<>());
                                            updatedIndices.add(k);
                                        }
                                        mEventSchedule.get(currIndex).add(toAdd);
                                        mTaskAppViewModel.insert(toAdd);
                                        updatedIndices.add(currIndex);
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
                    // If user chose to recur on specific months/days (e.g. 2nd/3rd of Jan/Feb)
                    if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_SPECIFIC)) {
                        // Get the days to recur on from user input.
                        String[] daysStrs = recur.getString(YearlyRecurFragment.EXTRA_DAYS)
                                .split(",");
                        int[] days = new int[daysStrs.length];

                        for (int i = 0; i < days.length; i++) {
                            days[i] = Integer.parseInt(daysStrs[i]);
                        }

                        // For each month, check if user chose to recur on it, and schedule events
                        // if they did
                        for (int i = 0; i < months.length; i++) {
                            if (months[i]) {
                                // Calculate earliest possible date to start scheduling events for
                                // this month
                                boolean oneLess = (i - recurCal.get(Calendar.MONTH)) < 0;
                                boolean offByOne = oneLess && (interval != 1);
                                recurCal = Calendar.getInstance();
                                recurCal.setTime(start);
                                recurCal.add(Calendar.MONTH,
                                        (i - recurCal.get(Calendar.MONTH) + 12) % 12);

                                // Set the null fields to their max values to ensure they're ignored
                                // by the while loop
                                if (until) {
                                    numTimes = Integer.MAX_VALUE;
                                }
                                else {
                                    endDate.setTime(Long.MAX_VALUE);
                                }

                                // Schedule this month out until the recurrence is set to end.
                                for (int j = 0; j < numTimes && !recurCal.getTime()
                                        .after(endDate); j++) {
                                    for (int day : days) {
                                        // Make sure to schedule not to schedule days in months
                                        // they're greater than the max day (e.g. 2/31)
                                        if (day > recurCal.getActualMaximum
                                                (Calendar.DAY_OF_MONTH)) {
                                            continue;
                                        }

                                        // Set the calendar to this date
                                        recurCal.set(Calendar.DAY_OF_MONTH, day);
                                        currIndex = getDiff(recurCal.getTime(), mStartDate);

                                        // Ensure that we don't schedule events for before today or
                                        // the endDate the user chose
                                        if (recurCal.getTime().before(start) || recurCal.getTime()
                                                .after(endDate) || j == 0 && offByOne ||
                                                j == numTimes - 1 && oneLess && !offByOne) {
                                            continue;
                                        }

                                        // Add the event to the data structure.
                                        Event toAdd = new Event(name, ttc, recurCal.getTime());
                                        for (int k = mEventSchedule.size(); k <= currIndex; k++) {
                                            mEventSchedule.add(new ArrayList<>());
                                            updatedIndices.add(k);
                                        }
                                        mEventSchedule.get(currIndex).add(toAdd);
                                        mTaskAppViewModel.insert(toAdd);
                                        updatedIndices.add(currIndex);
                                    }

                                    // Handle edge case
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
            // If the user chose to not recur
            else {
                // Ensure there is enough space in the eventSchedule
                for (int i = mEventSchedule.size(); i <= diff; i++) {
                    mEventSchedule.add(new ArrayList<>());
                    updatedIndices.add(i);
                }

                // Schedule the event
                Event toAdd = new Event(name, ttc, start);
                mEventSchedule.get(diff).add(toAdd);
                mTaskAppViewModel.insert(toAdd);
                updatedIndices.add(diff);
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
                return null;
            }

            // Convert the due date String to a MyTime
            Date due;
            try {
                due = Task.dateFormat.parse(dd);
            }
            catch (Exception e) {
                return null;
            }

            Task toAdd = new Task(name, early, due, timeToComplete);

            // The parents string in the Bundle is a String of the format "n1,n2,n3,...nN,"
            // where each nx is an index to a Task in tasks that should be used as a parent
            // for the task to be added.
            String[] parentIndices = parents.split(",");
            for (String parentIndex : parentIndices) {
                if (!parentIndex.equals("-1")) {
                    Task parent = mTasks.get(Integer.parseInt(parentIndex));
                    toAdd.addParent(parent);
                    parent.addChild(toAdd);
                }
            }

            // Add the event to the data structure and database
            mTasks.add(toAdd);
            mTaskAppViewModel.insert(toAdd);
        }

        return updatedIndices;
    }

    /**
     * Optimize the user's schedules. Returns a pair of data informing what recycler entries need to
     * update.
     *
     * First item in the pair is the latest day with an event/task scheduled and the number
     * of recycler DayItems. If eventLowIndex < eventScheduleSize, the items in the range between
     * these two numbers need to be removed from the DayItemRecycler.
     *
     * Second item in the pair is a list of pairs representing tasks that were changed -
     * specifically their formerly scheduled index and their newly scheduled index, so all these
     * indices must be updated.
     *
     * @return Pair of data informing what recycler entries need to update.
     */
    public Pair<Pair<Integer, Integer>, List<Pair<Integer, Integer>>> Optimize() {
        Optimizer opt = new Optimizer();
        ArrayList<Task> changedTasks = opt.Optimize
                (mTasks, mTaskSchedule, mEventSchedule, mStartDate, mTodayTime);

        Pair<Integer, Integer> removedRange = pareDownSchedules();
        List<Pair<Integer, Integer>> changedIndices = updateTasks(changedTasks);

        return new Pair<>(removedRange, changedIndices);
    }

    /**
     * Update the task data structures based on Optimizer's output. Returns a list of pairs
     * representing the tasks that have been changed - specifically their formerly scheduled index
     * and their newly scheduled index, so all these indices must be updated.
     *
     * @param changedTasks List of tasks that have been changed.
     *
     * @return A list of pairs representing the tasks that have been changed.
     */
    private List<Pair<Integer, Integer>> updateTasks(List<Task> changedTasks) {
        List<Pair<Integer, Integer>> changedIndices = new ArrayList<>();

        // Update the task with the new do date, and reflect this change in the database.
        for (Task t : changedTasks) {
            int oldIndex = getDiff(t.getDoDate(), mStartDate);
            int newIndex = getDiff(t.getWorkingDoDate(), mStartDate);

            t.setDoDate(t.getWorkingDoDate());
            mTaskAppViewModel.update(t);

            changedIndices.add(new Pair<>(oldIndex, newIndex));
        }

        return changedIndices;
    }

    /**
     * Pare down empty dates in the data structures and return a pair containing the latest day with
     * an event/task scheduled and the number of recycler DayItems. If eventLowIndex <
     * eventScheduleSize, the items in the range between these two numbers need to be removed from
     * the DayItemRecycler.
     *
     * @return A pair of indices representing if the recycler needs to be updated.
     */
    private Pair<Integer, Integer> pareDownSchedules() {
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

        // Delete any empty lists at the end of the event schedule.
        int eventLowIndex = mEventSchedule.size();
        for (int i = mEventSchedule.size() - 1; i >= 0; i--) {
            if (mEventSchedule.get(i).size() != 0) {
                break;
            }
            else {
                eventLowIndex--;
            }
        }
        int eventScheduleSize = mEventSchedule.size();
        if (eventScheduleSize > eventLowIndex) {
            mEventSchedule.subList(eventLowIndex, eventScheduleSize).clear();

            // Remove hanging days from adapter as well
            eventLowIndex = Math.max(eventLowIndex, taskLowIndex);
        }

        return new Pair<>(eventLowIndex, eventScheduleSize);
    }

    /**
     * Builds a DayItem List representation of a user's tasks/events
     *
     * @return a DayItem List representation of a user's tasks/events
     */
    public List<DayItem> DayItemList() {
        // The list of DayItem's to be displayed in the recycler
        List<DayItem> itemList = new ArrayList<>();

        // Generate a DayItem for the date i days past today's date
        for (int i = 0; i < mTaskSchedule.size() || i < mEventSchedule.size(); i++) {
            itemList.add(DayItemHelper(i));
        }

        return itemList;
    }

    /**
     * Get the EventItem and TaskItem lists for the day i days past today's date. Is used as a
     * helper function so when the recycler needs to be refreshed, it only needs to get the DayItem
     * for the updated days, not the entire schedule.
     *
     * @param i How many days past today's date to get the schedule for
     *
     * @return A DayItem representing that day's schedule
     */
    public DayItem DayItemHelper(int i) {
        // Fields for the DayItem
        String dayString;
        List<EventItem> events;
        List<TaskItem> tasks;

        // Date representing the date i days past today's date
        Date curr = xDaysPast(i);

        // Number representing totalTime this date has scheduled.
        int totalTime = getTotalTime(i);

        // Set the fields
        dayString = String.format(mMainActivity.getString(R.string.schedule_for),
                Task.dateFormat.format(curr), totalTime);
        events = EventItemList(i);
        tasks = TaskItemList(i);

        return new DayItem(dayString, events, tasks, i);
    }

    /**
     * Returns a Date x days past today's date.
     *
     * @param x Number of days past today's date
     *
     * @return A Date x days past today's date.
     */
    private Date xDaysPast(int x) {
        // Date representing the date i days past today's date
        Calendar currCal = Calendar.getInstance();
        currCal.add(Calendar.DAY_OF_YEAR, x);

        return clearDate(currCal.getTime());
    }

    /**
     * Get the total amount of time scheduled for a given date.
     *
     * @param i How many days past today's date to get the time commitment for.
     *
     * @return Amount of time scheduled for i days past today.
     */
    private int getTotalTime(int i) {
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

        return totalTime;
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
                // Get the jth event from the given date
                Event event = mEventSchedule.get(index).get(j);

                itemList.add(EventItemHelper(event, j));
            }
        }
        return itemList;
    }

    /**
     * Return a new EventItem based on a given event.
     *
     * @param event The event to make an EventItem based off of
     * @param position The position in the recycler of the event
     *
     * @return An EventItem based off of the given event.
     */
    private EventItem EventItemHelper(Event event, int position) {
        // Fields for itemList
        String name;
        String timespan;

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

        return new EventItem(name, timespan, position);
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
                // Get the jth task scheduled for the given day.
                Task task = mTaskSchedule.get(index).get(j);

                itemList.add(TaskItemHelper(task, j));
            }
        }

        return itemList;
    }

    /**
     * Return a new TaskItem based on a given event.
     *
     * @param task The Task to make a TaskItem based off of
     * @param position The position in the recycler of the task
     *
     * @return A TaskItem based off of the given event.
     */
    private TaskItem TaskItemHelper(Task task, int position) {
        // DayItem's only field
        String name;
        boolean completable;

        // Create the name in the format NAME (TTC minutes to complete)
        name = task.getName() + "\n" +
                String.format(mMainActivity.getString(R.string.minutes_to_complete),
                        task.getTimeToComplete());

        completable = (task.getEarlyDate().equals(mStartDate))
                && (task.getParents().size() == 0);

        return new TaskItem(name, position, completable);
    }

    /**
     * Get the amount of time currently spent completing tasks.
     *
     * @return the amount of time (in minutes) spent completing tasks so far.
     */
    public int getTodayTime() {
        return mTodayTime;
    }

    /**
     * Get a list of task names for the prerequisite picker in AddItem
     *
     * @return A list of task names for the prerequisite picker in AddItem
     */
    public ArrayList<String> getTaskNames() {
        ArrayList<String> taskNames = new ArrayList<>();
        for (Task t : mTasks) {
            Date tDate = t.getDueDate();
            taskNames.add(String.format(mMainActivity.getString(R.string.due_when), t.getName(),
                    Task.dateFormat.format(tDate)));
        }

        return taskNames;
    }

    /**
     * Handle button clicks in the MainActivity by deleting tasks/events.
     *
     * @param position Position in given day of event/task
     * @param day How many days past today's date task/event was scheduled for
     * @param action 0 to complete task, 1 to delete task, 2 to delete event
     *
     * @return true if ran successfully, false if error occurred.
     */
    public boolean onButtonClick(int position, int day, int action) {
        if (action == 0 || action == 1) {
            if (day == -1 || mTaskSchedule.get(day).size() <= position) {
                return false;
            }

            mTaskAppViewModel.delete(mTaskSchedule.get(day).get(position));
            Complete(mTaskSchedule.get(day).get(position));
        }
        // Remove the given event from the schedule and re-optimize.
        if (action == 2) {
            if (day == -1 || mEventSchedule.get(day).size() <= position) {
                return false;
            }
            mTaskAppViewModel.delete(mEventSchedule.get(day).get(position));
            mEventSchedule.get(day).remove(position);
        }

        return true;
    }
}
