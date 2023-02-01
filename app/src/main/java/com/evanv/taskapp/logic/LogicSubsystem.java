package com.evanv.taskapp.logic;

import static com.evanv.taskapp.logic.Task.getDiff;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.evanv.taskapp.R;
import com.evanv.taskapp.db.TaskAppViewModel;
import com.evanv.taskapp.ui.main.MainActivity;
import com.evanv.taskapp.ui.main.recycler.DayItem;
import com.evanv.taskapp.ui.main.recycler.EventItem;
import com.evanv.taskapp.ui.main.recycler.TaskItem;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kotlin.Pair;

/**
 * The subsystem that handles the logic for the app. All items/references are worked with here.
 * Singleton so information can be found everywhere without constant use of ever-increasingly
 * complex Intents.
 *
 * @author Evan Voogd
 */
public class LogicSubsystem {
    // taskSchedule[i] represents the list of tasks for the day i days past startDate
    private final List<List<Task>> mTaskSchedule = new ArrayList<>();
    // eventSchedule[i] represents the list of events for the day i days past startDate
    private final List<List<Event>> mEventSchedule = new ArrayList<>();
    private final LocalDate mStartDate;           // The current date
    private int mTodayTime;                       // The time spent completing tasks today
    private List<Task> mTasks;                    // List of all tasks for user
    private TaskAppViewModel mTaskAppViewModel;   // ViewModel to interact with Database
    private List<Task> overdueTasks;              // Overdue tasks
    private Task mTimerTask;                      // Task currently being timed.
    private LocalDateTime mTimer;                 // Start time of current timer
    @Nullable private List<Task> mWorkAheadTasks; // List of tasks currently work ahead.
    private final List<Project> mProjects;        // List of current projects.
    private final List<Label> mLabels;            // List of current labels.
    private List<Integer> mUpdatedIndices;        // List of updated indices.

    private static volatile LogicSubsystem INSTANCE; // The singleton of the LogicSubsystem

    /**
     * Creates a new LogicSubsystem and loads data from database into internal data structures.
     * Only call once.
     *
     *  @param mainActivity The calling MainActivity. Don't love this from a coupling perspective,
     *                     but due to Android's design, it's necessary to extract resources.
     * @param todayTime The amount of time spent completing tasks so far today.
     * @param timedTaskID ID of the task currently being timed.
     * @param timerStart Start time of the current timer.
     */
    public LogicSubsystem(MainActivity mainActivity, int todayTime, long timedTaskID, long timerStart) {
        if (INSTANCE != null) {
            throw new IllegalStateException();
        }

        this.mTodayTime = todayTime;

        // startDate is our representation for the current date upon the launch of TaskApp.
        mStartDate = LocalDate.now();

        // Populate from database;
        Thread thread = new Thread() {
            public void run() {
                mTaskAppViewModel = new ViewModelProvider(mainActivity).get(TaskAppViewModel.class);
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
            mTimer = LocalDateTime.ofEpochSecond(timerStart, 0, ZoneOffset.UTC);
        }

        // Get tasks from database
        mTasks = mTaskAppViewModel.getAllTasks();
        mTasks = (mTasks == null) ? new ArrayList<>() : mTasks;

        overdueTasks = new ArrayList<>(); // Tasks that are overdue.

        // Get projects from database
        mProjects = mTaskAppViewModel.getAllProjects();

        // Get labels from database
        mLabels = mTaskAppViewModel.getAllLabels();

        // Add tasks to taskSchedule/add parents
        for (Task t : mTasks) {
            // Add Project
            t.initializeProject(mProjects);
            t.initializeLabels(mLabels);

            // Calculate how many days past today's date this task is scheduled for. Used to
            // index into taskSchedule
            LocalDate doDate = t.getDoDate();
            int index = getDiff(doDate, mStartDate);

            if (t.getEarlyDate().isBefore(mStartDate)) {
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

        // List of updated indices for the recycler
        mUpdatedIndices = new ArrayList<>();

        INSTANCE = this;
    }

    /**
     * Return the singleton instance of the LogicSubsystem. Contains all data structures for the app.
     *
     * @return The singleton instance of the LogicSubsystem
     */
    public static synchronized LogicSubsystem getInstance() {
        return INSTANCE;
    }

    /**
     * Gets a list of overdue tasks. Only works in the beginning of the app lifecycle, else returns
     * null. This is due to the fact that all overdue tasks need to be handled for the app to begin
     * operation.
     *
     * @param context Give context for resources
     *
     * @return Return list of currently unhandled overdue tasks.
     */
    public String[] getOverdueTasks(Context context) {
        if (overdueTasks == null) {
            return null;
        }

        String[] overdueNames = new String[overdueTasks.size()];

        // Create a list of overdue task names for the dialog
        for (int i = 0; i < overdueNames.length; i++) {
            Task t = overdueTasks.get(i);

            if (t.getDoDate().isEqual(LocalDate.MIN)) {
                mTasks.remove(t);
                mTaskAppViewModel.delete(t);
                continue;
            }

            LocalDate tDate = t.getDueDate();
            overdueNames[i] = String.format(context.getString(R.string.due_when), t.getName(),
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
     * @param context Context for resources
     */
    public void updateOverdueTasks(List<Integer> completedItems, Context context) {
        // As the user has marked these tasks as completed, remove them.
        for (int i = 0; i < completedItems.size(); i++) {
            Complete(overdueTasks.get(completedItems.get(i)), context);
            mTaskAppViewModel.delete(overdueTasks.get(completedItems.get(i)));
        }

        // Change due date for overdue tasks if it has already been passed to today.
        if (overdueTasks.size() != completedItems.size()) {
            for (int i = 0; i < overdueTasks.size(); i++) {
                if (completedItems.contains(i)) {
                    continue;
                }

                Task t = overdueTasks.get(i);

                if (t.getDueDate().isBefore(mStartDate)) {
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
     * @param context Context for resources
     *
     * @return A list of DayItems to be used by MainActivity's recycler.
     */
    public List<DayItem> prepForDisplay(boolean reoptimize, Context context) {
        // Get the event list.
        List<Event> events = mTaskAppViewModel.getAllEvents();

        events = (events == null) ? new ArrayList<>() : events;

        // Add events from the database into the eventSchedule
        if (mEventSchedule.isEmpty()) {
            for (Event e : events) {
                // Calculate how many days past today's date this event is scheduled for. Used to
                // index into eventSchedule
                LocalDateTime doDate = e.getDoDate();
                int doDateIndex = getDiff(doDate, mStartDate);

                // Add the events to the list if they aren't for an earlier date
                if (!doDate.toLocalDate().isBefore(mStartDate)) {
                    for (int j = mEventSchedule.size(); j <= doDateIndex; j++) {
                        mEventSchedule.add(new ArrayList<>());
                    }

                    mEventSchedule.get(doDateIndex).add(e);
                }
            }
        }

        // If tasks were changed, make sure to reoptimize the schedule in case it's necessary
        if (reoptimize) {
            Optimize();
        }

        return DayItemList(context);
    }

    /**
     * Removes a task from the task dependency graph.
     *
     * @param task The task to be removed from the task dependency graph
     * @param context Context for resources
     *
     * @return List of days changed.
     */
    public List<Integer> Complete(Task task, Context context) {
        mTasks.remove(task);

        // Remove task from project
        if (task.getProject() != null) {
            task.getProject().removeTask(task);
        }

        // Remove task from labels
        for (Label l : task.getLabels()) {
            l.removeTask(task);
        }

        LocalDate doDate = task.getDoDate();

        // Get the number of days past the start date this task is scheduled for, so we can get the
        // index of the taskSchedule member for it's do date.
        int diff = getDiff(doDate, mStartDate);

        // If the task is in the internal data structure, remove it.
        if (diff >= 0) {
            mTaskSchedule.get(diff).remove(task);

            mUpdatedIndices.add(diff);
        }

        // Remove the task from the task dependency graph
        for (int i = 0; i < task.getChildren().size(); i++) {
            task.getChildren().get(i).removeParent(task);
            mTaskAppViewModel.update(task.getChildren().get(i));

            mUpdatedIndices.add(getDiff(task.getChildren().get(i).getDoDate(), mStartDate));
        }
        for (int i = 0; i < task.getParents().size(); i++) {
            task.getParents().get(i).removeChild(task);
        }

        if (diff >= 0) {
            DayItemHelper(diff, context);
        }
        return mUpdatedIndices;
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
     * Optimize the user's schedules. Returns a list of pairs representing tasks that were changed -
     * specifically their formerly scheduled index and their newly scheduled index, so all these
     * indices must be updated.
     */
    public void Optimize() {
        Optimizer opt = new Optimizer();
        ArrayList<Task> changedTasks = opt.Optimize
                (mTasks, mTaskSchedule, mEventSchedule, mStartDate, mTodayTime);

        pareDownSchedules();

        updateTasks(changedTasks);
    }

    /**
     * Update the task data structures based on Optimizer's output. Returns a list of pairs
     * representing the tasks that have been changed - specifically their formerly scheduled index
     * and their newly scheduled index, so all these indices must be updated.
     *
     * @param changedTasks List of tasks that have been changed.
     */
    private void updateTasks(List<Task> changedTasks) {
        // Update the task with the new do date, and reflect this change in the database.
        for (Task t : changedTasks) {
            mUpdatedIndices.add(getDiff(t.getDoDate(), mStartDate));
            mUpdatedIndices.add(getDiff(t.getWorkingDoDate(), mStartDate));

            t.setDoDate(t.getWorkingDoDate());
            mTaskAppViewModel.update(t);
        }
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

        int bothLowIndex = Math.max(taskLowIndex, eventLowIndex);

        for (int i = 0; i < mUpdatedIndices.size(); i++) {
            if (mUpdatedIndices.get(i) >= bothLowIndex) {
                mUpdatedIndices.remove(i);
                i--;
            }
        }
    }

    /**
     * Builds a DayItem List representation of a user's tasks/events
     *
     * @param context Context for resources
     *
     * @return a DayItem List representation of a user's tasks/events
     */
    public List<DayItem> DayItemList(Context context) {
        // The list of DayItem's to be displayed in the recycler
        List<DayItem> itemList = new ArrayList<>();

        // Generate a DayItem for the date i days past today's date
        for (int i = 0; i < mTaskSchedule.size() || i < mEventSchedule.size(); i++) {
            itemList.add(DayItemHelper(i, context));
        }

        return itemList;
    }

    /**
     * Get the EventItem and TaskItem lists for the day i days past today's date. Is used as a
     * helper function so when the recycler needs to be refreshed, it only needs to get the DayItem
     * for the updated days, not the entire schedule.
     *
     * @param i How many days past today's date to get the schedule for
     * @param context Context for resources
     *
     * @return A DayItem representing that day's schedule
     */
    public DayItem DayItemHelper(int i, Context context) {
        // Fields for the DayItem
        String dayString;
        List<EventItem> events;
        List<TaskItem> tasks;

        // Date representing the date i days past today's date
        LocalDate curr = mStartDate.plus(i, ChronoUnit.DAYS);

        // Number representing totalTime this date has scheduled.
        int totalTime = getTotalTime(i);

        // Set the fields
        dayString = String.format(context.getString(R.string.schedule_for),
                Task.dateFormat.format(curr), totalTime);
        events = EventItemList(i);
        tasks = i < mTaskSchedule.size() ? TaskItemList(mTaskSchedule.get(i), context) :
                new ArrayList<>();

        // If it is today's date and there are currently no tasks scheduled today, add a
        // "Work Ahead" page to today.
        boolean workAhead = i == 0 && tasks.size() == 0;
        if (workAhead) {
            tasks = new ArrayList<>();

            int numTasks = 0;

            mWorkAheadTasks = new ArrayList<>();

            // For each task, check if it can be completed today. If it can, then add it.
            for (Task t : mTasks) {
                TaskItem temp = TaskItemHelper(t, numTasks, context);

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
        LocalDateTime eventTime = event.getDoDate();
        LocalDateTime endTime = eventTime.plus(event.getLength(), ChronoUnit.MINUTES);

        // Format the event name as Name: StartTime-EndTime
        name = event.getName();
        timespan = Event.timeFormat.format(eventTime) + "-" +
                Event.timeFormat.format(endTime);

        return new EventItem(name, timespan, position);
    }

    /**
     * Builds a TaskItem List representation of a user's tasks on a given day
     *
     * @param taskList The list of tasks to build a TaskItem list out of.
     * @param context Context for resources
     *
     * @return a TaskItem List representation of a user's tasks on a given day
     */
    private List<TaskItem> TaskItemList(List<Task> taskList, Context context) {
        // The list of TaskItems representing the tasks scheduled for the date index days past
        // today's date
        List<TaskItem> itemList = new ArrayList<>();

        Collections.sort(taskList);

        // Add all the tasks scheduled for the given date to itemList
        if (!taskList.isEmpty()) {
            for (int j = 0; j < taskList.size(); j++) {
                // Get the jth task scheduled for the given day.
                Task task = taskList.get(j);

                itemList.add(TaskItemHelper(task, j, context));
            }
        }

        return itemList;
    }

    /**
     * Return a new TaskItem based on a given event.
     *
     * @param task The Task to make a TaskItem based off of
     * @param position The position in the recycler of the task
     * @param context Context for resources
     *
     * @return A TaskItem based off of the given event.
     */
    private TaskItem TaskItemHelper(Task task, int position, Context context) {
        // DayItem's only field
        String name;
        boolean completable;

        // Create the name in the format NAME (TTC minutes to complete)
        name = task.getName() + "\n" +
                String.format(context.getString(R.string.minutes_to_complete),
                        task.getTimeToComplete());

        completable = (task.getEarlyDate().equals(mStartDate))
                && (task.getParents().size() == 0);

        boolean hasTimer = mTimerTask != null && mTimerTask == task;

        int priority = !mStartDate.isBefore(task.getDueDate()) ? 4 : task.getPriority();

        String project = task.getProject() == null ? null : task.getProject().getName();
        int projectColor = task.getProject() == null ? -1 : task.getProject().getColor();

        long ID = task.getID();

        List<String> labels = new ArrayList<>();
        List<Integer> labelColors = new ArrayList<>();

        for (Label l : task.getLabels()) {
            labels.add(l.getName());
            labelColors.add(l.getColor());
        }

        return new TaskItem(name, position, completable, hasTimer, priority, project, projectColor,
                labels, labelColors, ID);
    }

    /**
     * Return a new TaskItem based on a given event.
     *
     * @param ID The ID of the Task to make a TaskItem based off of
     * @param position The position in the recycler of the task
     * @param context Context for resources
     *
     * @return A TaskItem based off of the given event.
     */
    public TaskItem TaskItemHelper(long ID, int position, Context context) {
        for (Task t : mTasks) {
            if (t.getID() == ID) {
                return TaskItemHelper(t, position, context);
            }
        }

        return null;
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
     * @param context The context for resources
     *
     * @return A list of task names for the prerequisite picker in AddItem
     */
    public ArrayList<String> getTaskNames(Context context) {
        ArrayList<String> taskNames = new ArrayList<>();
        for (Task t : mTasks) {
            LocalDate tDate = t.getDueDate();
            taskNames.add(String.format(context.getString(R.string.due_when), t.getName(),
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
     * @param context Context for resources
     *
     */
    public void onButtonClick(int position, int day, int action, Context context) {
        if (action == 0 || action == 1) {
            Task toRemove = mTaskSchedule.get(day).get(position);

            if (day == -1 || mTaskSchedule.get(day).size() <= position) {
                return;
            }

            // If task to remove is currently being timed, cancel the timer.
            if (toRemove == mTimerTask) {
                mTimerTask = null;
                mTimer = null;
            }

            mTaskAppViewModel.delete(toRemove);
            mUpdatedIndices.addAll(Complete(mTaskSchedule.get(day).get(position), context));

            pareDownSchedules();
        }
        // Remove the given event from the schedule and re-optimize.
        if (action == 2) {
            if (day == -1 || mEventSchedule.get(day).size() <= position) {
                return;
            }
            mTaskAppViewModel.delete(mEventSchedule.get(day).get(position));
            mEventSchedule.get(day).remove(position);

            mUpdatedIndices.add(day);

            pareDownSchedules();
        }

    }

    /**
     * Handle button clicks by deleting tasks/events.
     *
     * @param ID the ID of the task to delete.
     * @param action 0 to complete task, 1 to delete task
     * @param context Context for resources
     *
     */
    public void onButtonClick(long ID, int action, Context context) {
        Task toRemove = null;

        for (Task t : mTasks) {
            if (t.getID() == ID) {
                toRemove = t;
                break;
            }
        }

        if (toRemove == null) {
            return;
        }

        int day = getDiff(toRemove.getDoDate(), mStartDate);
        int position = mTaskSchedule.get(day).indexOf(toRemove);

        onButtonClick(position, day, action, context);
    }

    /**
     * Get the number of days currently scheduled in the app's internal data structures
     *
     * @return The number of days tracked in the system
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

        mTimer = LocalDateTime.now();
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

        LocalDateTime currTime = LocalDateTime.now();
        int toReturn = (int) ChronoUnit.MINUTES.between(mTimer, currTime);
        // This ensures we round up to the nearest minute instead of rounding down.
        toReturn += ChronoUnit.SECONDS.between(mTimer, currTime) % 60 != 0 ? 1 : 0;

        return toReturn;
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
        return (mTimer == null) ? -1 : mTimer.toEpochSecond(ZoneOffset.UTC);
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

    /**
     * Return the goals of each project.
     *
     * @return The goal of each project.
     */
    public ArrayList<String> getProjectGoals() {
        ArrayList<String> toReturn = new ArrayList<>();

        for (Project p : mProjects) {
            toReturn.add(p.getGoal());
        }

        return toReturn;
    }

    /**
     * Return the label names for the AddItem screen.
     *
     * @return A list of names of Labels
     */
    public ArrayList<String> getLabelNames() {
        ArrayList<String> toReturn = new ArrayList<>();

        for (Label l : mLabels) {
            toReturn.add(l.getName());
        }

        return toReturn;
    }

    /**
     * Return the colors for each label.
     *
     * @return a list of colors for Label.
     */
    public ArrayList<Integer> getLabelColors() {
        ArrayList<Integer> toReturn = new ArrayList<>();

        for (Label l : mLabels) {
            toReturn.add(l.getColor());
        }

        return toReturn;
    }

    /**
     * Build a list of tasks that meet the given filters. Leave field null or -1 if not needed.
     *
     * @param startDate Earliest due date
     * @param endDate Latest due date
     * @param project Project ID to show
     * @param name Name to search for
     * @param minTime Minimum TTC to show
     * @param maxTime Maximum TTC
     * @param completable Show only completable tasks
     * @param context Context for resources
     *
     * @return A TaskItem list with these parameters.
     */
    public List<TaskItem> filter(LocalDate startDate, LocalDate endDate, long project, String name,
                                 int minTime, int maxTime, boolean completable, List<Long> labels,
                                 int priority, Context context) {
        List<Task> toReturn = new ArrayList<>(mTasks);

        // Remove all tasks that are due before the given start date.
        if (startDate != null) {
            for (int i = 0; i < toReturn.size(); i++) {
                if (toReturn.get(i).getDueDate().isBefore(startDate)) {
                    toReturn.remove(i);
                    i--;
                }
            }
        }

        // Remove all tasks that are due after the given end date.
        if (endDate != null) {
            for (int i = 0; i < toReturn.size(); i++) {
                if (toReturn.get(i).getDueDate().isAfter(endDate)) {
                    toReturn.remove(i);
                    i--;
                }
            }
        }

        // Remove all tasks that are not in the given project
        if (project != -1) {
            for (int i = 0; i < toReturn.size(); i++) {
                Project taskProject = toReturn.get(i).getProject();
                if (taskProject == null || taskProject.getID() != project) {
                    toReturn.remove(i);
                    i--;
                }
            }
        }

        // Remove all tasks that do not contain the searched substring.
        if (name != null) {
            for (int i = 0; i < toReturn.size(); i++) {
                if (!toReturn.get(i).getName().toLowerCase().contains(name.toLowerCase())) {
                    toReturn.remove(i);
                    i--;
                }
            }
        }

        // Remove all tasks that take less time to complete than the given minimum
        if (minTime != -1) {
            for (int i = 0; i < toReturn.size(); i++) {
                if (toReturn.get(i).getTimeToComplete() < minTime) {
                    toReturn.remove(i);
                    i--;
                }
            }
        }

        // Remove all tasks that take more time to complete than the give maximum.
        if (maxTime != -1) {
            for (int i = 0; i < toReturn.size(); i++) {
                if (toReturn.get(i).getTimeToComplete() > maxTime) {
                    toReturn.remove(i);
                    i--;
                }
            }
        }

        // Remove all tasks that are not completable.
        if (completable) {
            for (int i = 0; i < toReturn.size(); i++) {
                Task task = toReturn.get(i);

                // Check if each task can be currently completed.
                if (!task.getEarlyDate().isEqual(mStartDate)
                        || !(task.getParents().size() == 0)) {
                    toReturn.remove(i);
                    i--;
                }
            }
        }

        // Remove all tasks that don't contain all given labels.
        if (labels != null) {
            for (int i = 0; i < toReturn.size(); i++) {
                Task task = toReturn.get(i);

                // Check if task has all chosen labels
                for (long id : labels) {
                    boolean found = false;

                    // Make sure task has chosen label
                    for (Label label : task.getLabels()) {
                        if (label.getID() == id) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        toReturn.remove(i);
                        i--;
                        break;
                    }
                }
            }
        }

        // Remove all tasks that have a lower priority than the given priority.
        if (priority != -1) {
            for (int i = 0; i < toReturn.size(); i++) {
                if (toReturn.get(i).getPriority() < priority) {
                    toReturn.remove(i);
                    i--;
                }
            }
        }

        return TaskItemList(toReturn, context);
    }

    /**
     * Get the ID of the index'th project in the project list.
     *
     * @param index Index into the project list
     *
     * @return index into the project list
     */
    public long getProjectID(int index) {
        return mProjects.get(index).getID();
    }

    /**
     * Get the index of the ID'th project in the project list
     *
     * @param ID ID to lookup
     * @return the index of the project with this ID
     */
    public int getProjectIndex(long ID) {
        for (int i = 0; i < mProjects.size(); i++) {
            if (mProjects.get(i).getID() == ID) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Get the ID of the index'th label in the label list.
     *
     * @param index Index into the label list
     *
     * @return index into the label list
     */
    public long getLabelID(int index) {
        return mLabels.get(index).getID();
    }

    /**
     * Add a new label to the Label list
     *
     * @param name Name of the new label
     * @param color Color of the new label
     */
    public void addLabel(String name, int color) {
        Label toAdd = new Label(name, color);
        mLabels.add(toAdd);
        mTaskAppViewModel.insert(toAdd);
    }

    /**
     * Get the name of a specific project by ID
     *
     * @param ID ID of the project
     * @param context Context for resources
     * @return the Name of the project with the given ID
     */
    public String getProjectName(long ID, Context context) {
        for (Project p : mProjects) {
            if (p.getID() == ID) {
                return p.getName();
            }
        }

        return context.getString(R.string.none_chosen);
    }

    /**
     * Get the name of a specific task by ID
     *
     * @param id ID of the task
     * @return the name of the Task with the given ID
     */
    public String getTaskName(long id) {
        for (Task t : mTasks) {
            if (t.getID() == id) {
                return t.getName();
            }
        }

        return "";
    }

    /**
     * Get the earliest completion date of a specific task by ID
     *
     * @param id ID of the task
     * @return the earliest completion date of the Task with the given ID
     */
    public LocalDate getTaskECD(long id) {
        for (Task t : mTasks) {
            if (t.getID() == id) {
                return t.getEarlyDate();
            }
        }

        return null;
    }

    /**
     * Get the due date of a specific task by ID
     *
     * @param id ID of the task
     * @return the due date of the Task with the given ID
     */
    public LocalDate getTaskDD(long id) {
        for (Task t : mTasks) {
            if (t.getID() == id) {
                return t.getDueDate();
            }
        }

        return null;
    }

    /**
     * Get the time to complete of a specific task by ID
     *
     * @param id ID of the task
     * @return the time to complete of the Task with the given ID
     */
    public int getTaskTTC(long id) {
        for (Task t : mTasks) {
            if (t.getID() == id) {
                return t.getTimeToComplete();
            }
        }

        return 0;
    }

    /**
     * Get the priority of a specific task by ID
     *
     * @param id ID of the task
     * @return the priority of the Task with the given ID
     */
    public int getTaskPriority(long id) {
        for (Task t : mTasks) {
            if (t.getID() == id) {
                return t.getPriority();
            }
        }

        return 0;

    }

    /**
     * Get the project of a specific task by ID
     *
     * @param id ID of the task
     * @return the project of the Task with the given ID
     */
    public long getTaskProject(long id) {
        for (Task t : mTasks) {
            if (t.getID() == id) {
                return t.getProjectID();
            }
        }

        return -1;
    }

    /**
     * Get all labels of a specific task by ID
     *
     * @param id ID of the task
     * @return all labels of the Task with the given ID
     */
    public List<Long> getTaskLabels(long id) {
        List<Long> toReturn = new ArrayList<>();
        for (Task t : mTasks) {
            if (t.getID() == id) {
                for (Label l : t.getLabels()) {
                    toReturn.add(l.getID());
                }
                return toReturn;
            }
        }
        return toReturn;
    }

    /**
     * Get all parents of a specific task by ID
     *
     * @param id ID of the task
     * @return all parents of the Task with the given ID
     */
    public List<Long> getTaskParents(long id) {
        for (Task t : mTasks) {
            if (t.getID() == id) {
                return t.getParentArr();
            }
        }

        return new ArrayList<>();
    }

    /**
     * Get the index in the mTasks list of a specific task by ID
     *
     * @param id ID of the task
     * @return the index in the mTasks list of the Task with the given ID
     */
    public int getTaskIndex(long id) {
        for (int i = 0; i < mTasks.size(); i++) {
            if (mTasks.get(i).getID() == id) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get the ID of a Task in a specific position in the recycler
     *
     * @param mPosition The position in the mDay'th dayViewHolder of the task
     * @param mDay How many days past today's date this task is scheduled for
     * @return The ID of the task in this specific position in the recycler.
     */
    public long getTaskID(int mPosition, int mDay) {
        return mTaskSchedule.get(mDay).get(mPosition).getID();
    }


    /**
     * Get ID of a Task in a specific position in the tasks list
     *
     * @param index Index into the tasks list
     * @return the ID of the given task
     */
    public Long getTaskID(int index) {
        return mTasks.get(index).getID();
    }

    /**
     * Get the name of a specific Event by ID
     *
     * @param id ID of the Event
     * @return the name of the Event with the given ID
     */
    public String getEventName(long id) {
        for (List<Event> day : mEventSchedule) {
            for (Event e : day) {
                if (e.getID() == id) {
                    return e.getName();
                }
            }
        }

        return "";
    }

    /**
     * Get the earliest completion date of a specific Event by ID
     *
     * @param id ID of the Event
     * @return the earliest completion date of the Event with the given ID
     */
    public LocalDateTime getEventECD(long id) {
        for (List<Event> day : mEventSchedule) {
            for (Event e : day) {
                if (e.getID() == id) {
                    return e.getDoDate();
                }
            }
        }

        return null;
    }

    /**
     * Get the time to complete of a specific Event by ID
     *
     * @param id ID of the Event
     * @return the time to complete of the Event with the given ID
     */
    public int getEventTTC(long id) {
        for (List<Event> day : mEventSchedule) {
            for (Event e : day) {
                if (e.getID() == id) {
                    return e.getLength();
                }
            }
        }

        return -1;
    }

    /**
     * Get the ID of an Event in a specific position in the recycler
     *
     * @param mPosition The position in the mDay'th dayViewHolder of the event
     * @param mDay How many days past today's date this event occurs
     * @return The ID of the event in this specific position in the recycler.
     */
    public long getEventID(int mPosition, int mDay) {
        return mEventSchedule.get(mDay).get(mPosition).getID();
    }

    /**
     * Get a list of indices the recycler must update since it last checked.
     *
     * @return a list of indices for the recycler to update.
     */
    public List<Integer> getUpdatedIndices() {
        List<Integer> toReturn = mUpdatedIndices;
        mUpdatedIndices = new ArrayList<>();
        return toReturn;
    }

    /**
     * Create an event based on the given parameters, and update an existing event if necessary.
     *
     * @param name Name of the new event
     * @param start Start time of the new event
     * @param end End time of the new event
     * @param recur Recurrence information of the new event
     * @param id ID of the event if it needs to be updated, -1 otherwise.
     * @param context Context for resources.
     */
    public void editEvent(String name, LocalDateTime start, LocalDateTime end, Bundle recur,
                          long id, Context context) {
        // Make sure ID is only reused once if it's supposed to edit an item.
        boolean first = id != -1;

        // Get information on event recurrence
        RecurrenceParser rp = new RecurrenceParser(context);
        List<LocalDateTime> recurrenceDates = rp.parseBundle(recur, start);

        // Calculate Length of Event
        int ttc = (int) ChronoUnit.MINUTES.between(start, end);

        // Add event based on recurrence information
        for (LocalDateTime d : recurrenceDates) {
            int index = getDiff(d, mStartDate);

            // Make sure there is enough mEventSchedules.
            for (int i = mEventSchedule.size(); i <= index; i++) {
                mEventSchedule.add(new ArrayList<>());
                mUpdatedIndices.add(i);
            }

            Event toAdd = new Event(name, ttc, d);

            // Update the event if necessary
            if (first) {
                toAdd.setID(id);

                for (int i = 0; i < mEventSchedule.get(index).size(); i++) {
                    if (mEventSchedule.get(index).get(i).getID() == id) {
                        mEventSchedule.get(index).set(i, toAdd);
                    }
                }

                mTaskAppViewModel.update(toAdd);

                first = false;
            }
            // If not necessary, insert a new event
            else {
                mEventSchedule.get(index).add(toAdd);
                mTaskAppViewModel.insert(toAdd);
            }

            mUpdatedIndices.add(index);
        }
    }

    /**
     * Create a task based on the given parameters, and update it if necessary
     *
     * @param name The name of the Task
     * @param early The earliest completion date of the task
     * @param due The due date of the Task
     * @param recur The Bundle of recurrence information of the task
     * @param timeToComplete How long the task takes to complete
     * @param project The index of the selected project (0 if no project was selected)
     * @param labelIDs An array of Labels that the user has attached to the task
     * @param parents An array of indices of parents of the added task
     * @param priority The priority of the task
     * @param id the ID of the task if it is to be updated, -1 otherwise
     * @param context Context for resources
     */
    public void editTask(String name, LocalDate early, LocalDate due, Bundle recur,
                         int timeToComplete, long project, long[] labelIDs, List<Long> parents,
                         int priority, long id, Context context) {
        // Parse the recurrence information
        RecurrenceParser rp = new RecurrenceParser(context);
        List<LocalDate> recurrenceDates = rp.parseBundle(recur, early);

        // Get how long the user is given to complete a task.
        int diff = getDiff(due, early);

        // Make sure ID is only reused once if it's supposed to edit an item.
        boolean first = id != -1;

        for (LocalDate d : recurrenceDates) {
            int index = getDiff(d, mStartDate);

            // Make sure there is enough mEventSchedules.
            for (int i = mTaskSchedule.size(); i <= index; i++) {
                mTaskSchedule.add(new ArrayList<>());
                mUpdatedIndices.add(i);
            }

            // Get the day based on the recurrence information
            LocalDate dueDate = d.plus(diff, ChronoUnit.DAYS);

            Task toAdd = new Task(name, d, dueDate, timeToComplete, priority);

            // Add the given project
            if (project != 0) {
                // Add task to selected project
                for (Project p : mProjects) {
                    if (p.getID() == project) {
                        toAdd.setProject(p);
                        p.addTask(toAdd);
                        break;
                    }
                }
            }

            // Add all selected labels to task.
            for (Label label : mLabels) {
                for (long labelID : labelIDs) {
                    if (label.getID() == labelID) {
                        toAdd.addLabel(label);
                        label.addTask(toAdd);
                    }
                }
            }

            // Add given parents
            if (parents != null) {
                for (Long parent : parents) {
                    for (Task t : mTasks) {
                        if (t.getID() == parent && parent != id) {
                            toAdd.addParent(t);
                            t.addChild(toAdd);
                            break;
                        }
                    }
                }
            }

            // Update the task if necessary
            if (first) {
                toAdd.setID(id);

                for (int i = 0; i < mTasks.size(); i++) {
                    if (mTasks.get(i).getID() == id) {
                        Task oldTask = mTasks.get(i);

                        // Replace oldTask with toAdd in the data structures
                        mTasks.set(i, toAdd);
                        int taskScheduleIndex = getDiff(oldTask.getDoDate(), mStartDate);
                        mTaskSchedule.get(taskScheduleIndex).remove(oldTask);
                        mTaskSchedule.get(taskScheduleIndex).add(toAdd);

                        // Add old task location to updated recycler locations
                        mUpdatedIndices.add(taskScheduleIndex);

                        // Replace the parent for each of the remaining children.
                        for (Task child : oldTask.getChildren()) {
                            child.removeParent(oldTask);
                            child.addParent(toAdd);
                            toAdd.addChild(child);
                            mTaskAppViewModel.update(child);
                        }

                        // Remove old task from each parent task, as new parents were already
                        // added earlier.
                        for (Task parent : oldTask.getParents()) {
                            parent.removeChild(oldTask);
                        }
                    }
                }

                mTaskAppViewModel.update(toAdd);

                first = false;
            }
            // Add the task to the DB if not editing
            else {
                mTasks.add(toAdd);
                mTaskAppViewModel.insert(toAdd);
            }
        }
    }

    /**
     * Add a project to the LogicSubsystem
     *
     * @param name Name of the project
     * @param color Color of the project
     * @param goal Goal of the project
     */
    public void addProject(String name, int color, String goal) {
        Project toAdd = new Project(name, color, goal);

        mProjects.add(toAdd);
        mTaskAppViewModel.insert(toAdd);
    }

    /**
     * Get position/day based off of a task's ID
     *
     * @param ID the ID of the task
     * @return a pair where the first item is the position of the task and second is the day.
     */
    public Pair<Integer, Integer> convertDay(long ID) {
        for (Task t : mTasks) {
            if (t.getID() == ID) {
                int day = Task.getDiff(t.getDoDate(), mStartDate);
                int position = mTaskSchedule.get(day).indexOf(t);

                return new Pair<>(position, day);
            }
        }

        return null;
    }
}
