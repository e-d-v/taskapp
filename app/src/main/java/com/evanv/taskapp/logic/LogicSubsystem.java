package com.evanv.taskapp.logic;

import static com.evanv.taskapp.logic.Event.clearTime;
import static com.evanv.taskapp.logic.Task.clearDate;
import static com.evanv.taskapp.logic.Task.getDiff;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.evanv.taskapp.R;
import com.evanv.taskapp.db.TaskAppViewModel;
import com.evanv.taskapp.ui.additem.AddItem;
import com.evanv.taskapp.ui.main.MainActivity;
import com.evanv.taskapp.ui.main.recycler.DayItem;
import com.evanv.taskapp.ui.main.recycler.EventItem;
import com.evanv.taskapp.ui.main.recycler.TaskItem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import kotlin.Pair;

/**
 * The subsystem that handles the logic for the app. All items/references are worked with here.
 *
 * @author Evan Voogd
 */
public class LogicSubsystem {
    private final Date mStartDate;              // The current date
    private final MainActivity mMainActivity;   // MainActivity for resources
    // taskSchedule[i] represents the list of tasks for the day i days past startDate
    private final List<List<Task>> mTaskSchedule = new ArrayList<>();
    // eventSchedule[i] represents the list of events for the day i days past startDate
    private final List<List<Event>> mEventSchedule = new ArrayList<>();
    private int mTodayTime;                       // The time spent completing tasks today
    private List<Task> mTasks;                    // List of all tasks for user
    private TaskAppViewModel mTaskAppViewModel;   // ViewModel to interact with Database
    private List<Task> overdueTasks;              // Overdue tasks
    private Task mTimerTask;                      // Task currently being timed.
    private Date mTimer;                          // Start time of current timer
    @Nullable private List<Task> mWorkAheadTasks; // List of tasks currently work ahead.
    private List<Project> mProjects;              // List of current projects.

    /**
     * Creates a new LogicSubsystem and loads data from database into internal data structures.
     *  @param mainActivity The calling MainActivity. Don't love this from a coupling perspective,
     *                     but due to Android's design, it's necessary to extract resources.
     * @param todayTime The amount of time spent completing tasks so far today.
     * @param timedTaskID ID of the task currently being timed.
     * @param timerStart Start time of the current timer.
     */
    public LogicSubsystem(MainActivity mainActivity, int todayTime, long timedTaskID, long timerStart) {
        this.mMainActivity = mainActivity;
        this.mTodayTime = todayTime;

        // startDate is our representation for the current date upon the launch of TaskApp.
        mStartDate = clearDate(new Date());

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

        // Set the timer
        if (timerStart != -1) {
            mTimer = new Date(timerStart);
        }

        // Get tasks from database
        mTasks = mTaskAppViewModel.getAllTasks();
        mTasks = (mTasks == null) ? new ArrayList<>() : mTasks;

        overdueTasks = new ArrayList<>(); // Tasks that are overdue.

        // Get projects from database
        mProjects = mTaskAppViewModel.getAllProjects();

        // Add tasks to taskSchedule/add parents
        for (Task t : mTasks) {
            // Add Project
            t.initializeProject(mProjects);

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

            // If task is the currently timed task, add it.
            if (t.getID() == timedTaskID) {
                mTimerTask = t;
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
            mTaskAppViewModel.delete(overdueTasks.get(completedItems.get(i)));
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
     * @return List of days changed.
     */
    public List<Integer> Complete(Task task) {
        mTasks.remove(task);

        // Remove task from project
        task.getProject().removeTask(task);

        Date doDate = task.getDoDate();

        List<Integer> toReturn = new ArrayList<>();

        // Get the number of days past the start date this task is scheduled for, so we can get the
        // index of the taskSchedule member for it's do date.
        int diff = getDiff(doDate, mStartDate);

        // If the task is in the internal data structure, remove it.
        if (diff >= 0) {
            mTaskSchedule.get(diff).remove(task);
            toReturn.add(diff);
        }

        // Remove the task from the task dependency graph
        for (int i = 0; i < task.getChildren().size(); i++) {
            task.getChildren().get(i).removeParent(task);

            toReturn.add(getDiff(task.getChildren().get(i).getDoDate(), mStartDate));
        }
        for (int i = 0; i < task.getParents().size(); i++) {
            task.getParents().get(i).removeChild(task);
        }


        if (diff >= 0) {
            DayItemHelper(diff);
        }
        return toReturn;
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
            String startStr = result.getString(AddItem.EXTRA_START);
            String endStr = result.getString(AddItem.EXTRA_END);
            Bundle recur = result.getBundle(AddItem.EXTRA_RECUR);

            // Convert the String start time into a Date
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

            // Convert the String end time into a Date
            Date end;
            Calendar userEnd;
            try {
                end = Event.dateFormat.parse(endStr);
                userEnd = Calendar.getInstance();
                if (end != null) {
                    userEnd.setTime(end);
                }
                else {
                    return null;
                }
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
                return null;
            }

            RecurrenceParser rp = new RecurrenceParser(mMainActivity);
            List<Date> recurrenceDates = rp.parseBundle(recur, start);

            int ttc =(int) ((end.getTime() - start.getTime()) / TimeUnit.MINUTES.toMillis(1));

            for (Date d : recurrenceDates) {
                int index = getDiff(d, mStartDate);

                // Make sure there is enough mEventSchedules.
                for (int i = mEventSchedule.size(); i <= index; i++) {
                    mEventSchedule.add(new ArrayList<>());
                    updatedIndices.add(i);
                }

                Event toAdd = new Event(name, ttc, d);
                mEventSchedule.get(index).add(toAdd);
                mTaskAppViewModel.insert(toAdd);
                updatedIndices.add(index);
            }
        }
        // If the item type is Task
        else if (type.equals(AddItem.EXTRA_VAL_TASK)) {
            // Get the fields from the Bundle
            String name = result.getString(AddItem.EXTRA_NAME);
            int timeToComplete = Integer.parseInt(result.getString(AddItem.EXTRA_END));
            String ecd = result.getString(AddItem.EXTRA_ECD);
            String dd = result.getString(AddItem.EXTRA_DUE);
            String parents = result.getString(AddItem.EXTRA_PARENTS);
            Bundle recur = result.getBundle(AddItem.EXTRA_RECUR);
            int priority = result.getInt(AddItem.EXTRA_PRIORITY);
            int project = result.getInt(AddItem.EXTRA_PROJECT);

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

            RecurrenceParser rp = new RecurrenceParser(mMainActivity);
            List<Date> recurrenceDates = rp.parseBundle(recur, early);

            int diff = getDiff(due, early);

            for (Date d : recurrenceDates) {
                int index = getDiff(d, mStartDate);

                // Make sure there is enough mEventSchedules.
                for (int i = mTaskSchedule.size(); i <= index; i++) {
                    mTaskSchedule.add(new ArrayList<>());
                    updatedIndices.add(i);
                }

                Calendar dDue = Calendar.getInstance();
                dDue.setTime(d);
                dDue.add(Calendar.DAY_OF_YEAR, diff);

                Task toAdd = new Task(name, d, dDue.getTime(), timeToComplete, priority);

                if (project != -1) {
                    toAdd.setProject(mProjects.get(project));
                    mProjects.get(project).addTask(toAdd);
                }

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
                updatedIndices.add(index);
            }
        }

        return updatedIndices;
    }

    /**
     * Optimize the user's schedules. Returns a list of pairs representing tasks that were changed -
     * specifically their formerly scheduled index and their newly scheduled index, so all these
     * indices must be updated.
     *
     * @return Pair of data informing what recycler entries need to update.
     */
    public List<Pair<Integer, Integer>> Optimize() {
        Optimizer opt = new Optimizer();
        ArrayList<Task> changedTasks = opt.Optimize
                (mTasks, mTaskSchedule, mEventSchedule, mStartDate, mTodayTime);

        pareDownSchedules();

        return updateTasks(changedTasks);
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
     * Pare down empty dates in the data structures so there's no extra items in the recycler
     */
    private void pareDownSchedules() {
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
        }
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

        // If it is today's date and there are currently no tasks scheduled today, add a
        // "Work Ahead" page to today.
        boolean workAhead = i == 0 && tasks.size() == 0;
        if (workAhead) {
            tasks = new ArrayList<>();

            int numTasks = 0;

            mWorkAheadTasks = new ArrayList<>();

            // For each task, check if it can be completed today. If it can, then add it.
            for (Task t : mTasks) {
                TaskItem temp = TaskItemHelper(t, numTasks);

                if (temp.isCompletable()) {
                    tasks.add(temp);
                    mWorkAheadTasks.add(t);
                    numTasks++;
                }
            }
        }
        // If Work Ahead is not displayed, do not keep a list.
        else if (i == 0) {
            mWorkAheadTasks = null;
        }

        return new DayItem(dayString, events, tasks, i, workAhead);
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
        totalTime += Optimizer.calculateTotalTime(i, mEventSchedule);

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

        Collections.sort(mTaskSchedule.get(index));

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

        boolean hasTimer = mTimerTask != null && mTimerTask == task;

        int priority = !mStartDate.before(task.getDueDate()) ? 4 : task.getPriority();

        String project = task.getProject().getName();
        int projectColor = task.getProject().getColor();

        return new TaskItem(name, position, completable, hasTimer, priority, project, projectColor);
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
     * @return list of updated dates if ran successfully, false if error occurred.
     */
    public List<Integer> onButtonClick(int position, int day, int action) {
        List<Integer> toReturn = new ArrayList<>();

        if (action == 0 || action == 1) {
            Task toRemove = mTaskSchedule.get(day).get(position);

            if (day == -1 || mTaskSchedule.get(day).size() <= position) {
                return null;
            }

            // If task to remove is currently being timed, cancel the timer.
            if (toRemove == mTimerTask) {
                mTimerTask = null;
                mTimer = null;
            }

            mTaskAppViewModel.delete(toRemove);
            toReturn = Complete(mTaskSchedule.get(day).get(position));
        }
        // Remove the given event from the schedule and re-optimize.
        if (action == 2) {
            if (day == -1 || mEventSchedule.get(day).size() <= position) {
                return null;
            }
            mTaskAppViewModel.delete(mEventSchedule.get(day).get(position));
            mEventSchedule.get(day).remove(position);

            toReturn.add(day);

            pareDownSchedules();
        }

        return toReturn;
    }

    /**
     * Get the number of days currently scheduled in the app's internal data structures
     *
     * @return The number of days currently scheduled in the app's internal data structures
     */
    public int getNumDays() {
        return Integer.max(mEventSchedule.size(), mTaskSchedule.size());
    }

    /**
     * Start or cancel a timer.
     *
     * @param position Position in the day'th list in the taskSchedule list of the task
     * @param day How many days past today's date the task is scheduled for.
     */
    public void timer(int position, int day) {
        Task toTime = mTaskSchedule.get(day).get(position);

        if (mTimerTask == toTime) {
            mTimerTask = null;
            mTimer = null;

            return;
        }

        mTimer = new Date();
        mTimerTask = toTime;
    }

    /**
     * Returns if the selected task is currently being timed.
     *
     * @param position Position in the task recycler for the given day
     * @param day How many days past today's date the task is assigned for
     *
     * @return true if given task is timed, false otherwise.
     */
    public boolean isTimed(int position, int day) {
        return mTaskSchedule.get(day).get(position) == mTimerTask;
    }

    /**
     * Get the amount of time elapsed in the current timer.
     *
     * @return Amount of time elapsed in the current timer, or -1 if no timer is set.
     */
    public int getTimer() {
        if (mTimer == null) {
            return -1;
        }

        Date currTime = new Date();

        // Weird, but this calculates number of minutes task took while rounding up to the nearest
        // minute instead of rounding down.
        return (int) Math.ceil(((double) (currTime.getTime() - mTimer.getTime()))
                / ((double) TimeUnit.MINUTES.toMillis(1)));
    }

    /**
     * Return the ID of the currently timed task.
     *
     * @return ID of the task currently being timed, or -1 if no task is currently being timed.
     */
    public long getTimedID() {
        return (mTimerTask == null) ? -1 : mTimerTask.getID();
    }

    /**
     * Return the long representation of the timer start.
     *
     * @return long representation of the timer Date, or -1 if no timer is active.
     */
    public long getTimerStart() {
        return (mTimer == null) ? -1 : mTimer.getTime();
    }

    /**
     * Get the do date of the currently timed task
     *
     * @return the do date of the currently timed task, or -1 if no task is currently being timed.
     */
    public int getTimerDay() {
        return (mTimerTask == null) ? -1 : getDiff(mTimerTask.getDoDate(), mStartDate);
    }

    /**
     * Must be for today's date. If today's date is currently displaying work ahead, then it returns
     * the position of the task on it's normal date.
     *
     * @param position Position in the work ahead list.
     *
     * @return <Position, Day> pair if Work Ahead is displayed, null otherwise.
     */
    public Pair<Integer, Integer> convertDay(int position) {
        if (mWorkAheadTasks == null) {
            return null;
        }

        Task toRemove = mWorkAheadTasks.get(position);

        int day = getDiff(toRemove.getDoDate(), mStartDate);

        position = mTaskSchedule.get(day).indexOf(toRemove);

        if (position == -1) {
            return null;
        }

        return new Pair<>(position, day);
    }

    /**
     * Return the project names for the AddItem screen.
     *
     * @return A list of names of Projects
     */
    public ArrayList<String> getProjectNames() {
        ArrayList<String> toReturn = new ArrayList<>();

        for (Project p : mProjects) {
            toReturn.add(p.getName());
        }

        return toReturn;
    }

    /**
     * Return the colors for each project.
     *
     * @return a list of colors for Projects.
     */
    public ArrayList<Integer> getProjectColors() {
        ArrayList<Integer> toReturn = new ArrayList<>();

        for (Project p : mProjects) {
            toReturn.add(p.getColor());
        }

        return toReturn;
    }
}
