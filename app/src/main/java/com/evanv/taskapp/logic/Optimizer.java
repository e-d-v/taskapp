package com.evanv.taskapp.logic;

import static com.evanv.taskapp.logic.Task.getDiff;

import org.threeten.bp.LocalDate;
import org.threeten.bp.temporal.ChronoField;
import org.threeten.bp.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import kotlin.Pair;

/**
 * The holy grail. A class that calculates a (locally) optimal schedule for task completion
 * dependent on other tasks, their prerequisites, and events.
 *
 * @author Evan Voogd
 */
public class Optimizer {
    /**
     * Finds the date that is the earlier of A) the earliest current do date for a child Task, or
     * B) the actual due date for the given Task, as scheduling later than the earlier of these
     * dates would create problems.
     *
     * @param t The task to find the true end date for
     *
     * @return The latest date this task should be rescheduled for
     */
    private LocalDate findTrueEndDate(Task t) {
        // B) the actual due date for the given task
        LocalDate currLateDate = t.getDueDate();

        // A) the earliest current do date for a child task, if the child task's do date is earlier
        // than B) the actual due date for the given task
        for (int j = 0; j < t.getChildren().size(); j++) {
            Task child = t.getChildren().get(j);

            if (currLateDate.isAfter(child.getWorkingDoDate())) {
                currLateDate = child.getWorkingDoDate();
            }
        }

        return currLateDate;
    }


    /**
     * Finds the date that is the later of A) the earliest current do date for a parent Task, or
     * B) the actual earliest completion date for the given Task, as scheduling earlier than the
     * later of these dates would create problems.
     *
     * @param t The task to find the true early date for
     * @return The earliest date this task should be rescheduled for
     */
    private LocalDate findTrueEarlyDate(Task t) {
        // B) the actual earliest completion date for the given task
        LocalDate currEarlyDate = t.getEarlyDate();

        // A) the latest current do date for a parent task, if the parent task's do date is later
        // than B) the actual earliest completion date for the given task
        for (int j = 0; j < t.getParents().size(); j++) {
            Task parent = t.getParents().get(j);

            if (currEarlyDate.isBefore(parent.getWorkingDoDate())) {
                currEarlyDate = parent.getWorkingDoDate();
            }
        }

        return currEarlyDate;
    }

    /**
     * Schedules task for the date index days past startDate.
     *  @param t The task to be scheduled
     * @param index How many days past startDate to be scheduled
     * @param startDate The Date representing taskSchedule[0]
     * @param taskSchedule ArrayList where taskSchedule[i] is a list of tasks scheduled for i days
*                     past the start date
     * @param time Array of ints where time[i] is the number of minutes scheduled for i days past
     */
    private void schedule(Task t, int index, LocalDate startDate, List<List<Task>> taskSchedule
            , int[] time) {
        taskSchedule.get(index).add(t);
        time[index] += t.getTimeToComplete();
        t.setWorkingDoDate(startDate.plus(index, ChronoUnit.DAYS));
    }

    /**
     * Removes task from taskSchedule/time for the given index.
     *
     * @param t The task to be removed
     * @param index Index into taskSchedule where task is located
     * @param taskSchedule ArrayList where taskSchedule[i] is a list of tasks scheduled for i days
     *                     past the start date
     * @param time Array of ints where time[i] is the number of minutes scheduled for i days past
     *             the start date
     */
    private void remove(Task t, int index, List<List<Task>> taskSchedule, int[] time) {
        taskSchedule.get(index).remove(t);
        time[index] -= t.getTimeToComplete();
    }

    /**
     * Optimally schedules the given tasks
     *
     * @param tasks A list of Tasks to be scheduled
     * @param taskSchedule A list of lists of tasks. taskSchedule[i] refers to the list of tasks
     *                     scheduled for i days past the current day.
     * @param eventSchedule A list of lists of events. eventSchedule[i] refers to the list of events
     *                      occurring i days past the current day
     * @param startDate The current date
     *
     * @return An ArrayList of tasks whose dates were changed.
     */
    public ArrayList<Task> Optimize(List<Task> tasks, List<List<Task>> taskSchedule,
                                    List<List<Event>> eventSchedule, LocalDate startDate,
                                    int todayTime) {
        taskSchedule.clear();

        LocalDate lateDate = startDate;

        // Initializes tasks for optimization, see Javadoc for Task for more detail on why this is
        // necessary.
        for (int i = 0; i < tasks.size(); i++) {
            tasks.get(i).initializeForOptimization();

            if (tasks.get(i).getDueDate().isAfter(lateDate)) {
                lateDate = tasks.get(i).getDueDate();
            }
        }

        // Makes sure that taskSchedule is big enough to hold up to the latest possible date
        int diff = getDiff(lateDate, startDate);

        for (int i = taskSchedule.size(); i <= diff; i++) {
            taskSchedule.add(new ArrayList<>());
        }

        // The idea here is fairly simple. We'll schedule tasks with earlier dueDates first, and
        // then tie break for the number of children (as Tasks with more children should be
        // completed earlier so the children don't clump on the due date), and finally on the early
        // date (so earlier dates are filled up first). We only add tasks with no prerequisites
        // first (as we can schedule them now) and add tasks to the PriorityQueue when all
        // prerequisite tasks have been scheduled
        PriorityQueue<Task> pq = new PriorityQueue<>();
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);

            if (t.getParents().size() == 0) {
                pq.add(t);
            }
        }

        // Sort eventSchedule by early start time
        for (int i = 0; i < eventSchedule.size(); i++) {
            Collections.sort(eventSchedule.get(i));
        }

        int[] time = new int[taskSchedule.size()];

        for (int i = 0; i < taskSchedule.size(); i++) {
            time[i] = calculateTotalTime(i, eventSchedule);
        }

        if (time.length >= 1) {
            time[0] += todayTime;
        }

        // Assign tasks using a greedy algorithm - for each task assign it to the date between it's
        // earliest completion date and due date with the least current time commitment
        initialAssignment(pq, startDate, taskSchedule, time);

        int max_iters = 100;    // Maximum number of iterations, can be tweaked for performance
        boolean changed = true; // Allows us to stop the loop when a local minima has been found
        int iter = 0;           // How many iterations have been completed

        // Finds locally optimal schedule by repeatedly checking two things for each task: if there
        // is any day that it can swap to to better spread out time, or if there is any task it can
        // swap with to better spread out time. Repeats until local minimum is found or max_iters is
        // reached (although something is likely seriously wrong if it gets anywhere close to that.
        while (changed && iter++ < max_iters) {
            changed = update(tasks, startDate, taskSchedule, time);
        }

        // With the schedule finalized, we will create a list of all the changed do dates. This list
        // is used to update the recycler more efficiently and allow for easy updating in the DB. We
        // don't change the actual doDate here, as we need it in MainActivity to update the Recycler.
        ArrayList<Task> changedTasks = new ArrayList<>();
        for (Task t : tasks) {
            if (!t.getWorkingDoDate().equals(t.getDoDate())) {
                changedTasks.add(t);
            }
        }

        return changedTasks;
    }

    /**
     * Calculates the total event time for a specific day
     *
     * @param day What day to calculate
     * @param eventSchedule Schedule of events where ith element is list of elements scheduled for i
     *                      days past today.
     *
     * @return Total event time for a specific day
     */
    public static int calculateTotalTime(int day, List<List<Event>> eventSchedule) {
        int time = 0;

        for (int j = 0; day < eventSchedule.size() && j < eventSchedule.get(day).size(); j++) {
            Event e = eventSchedule.get(day).get(j);

            // Check if events overlap - if they do, only count each minute once.
            Pair<Integer, Integer> currBounds = getStartEndMinutes(e);
            int startMinute = currBounds.getFirst();
            int endMinute = currBounds.getSecond();

            for (int k = 0; k < j; k++) {
                int otherEndMinute = getStartEndMinutes(eventSchedule.get(day).get(k)).getSecond();

                if (startMinute < otherEndMinute) {
                    startMinute = Integer.min(endMinute, otherEndMinute);
                }
            }

            time += endMinute - startMinute;
        }

        return time;
    }

    /**
     * Get a pair representing the timespan of the event. First item is how many minutes past
     * midnight this event starts, and the second item is how many minutes past midnight this
     * event ends.
     *
     * @param e The event to get the timespan of
     *
     * @return A pair of ints representing the timespan of the event.
     */
    private static Pair<Integer, Integer> getStartEndMinutes(Event e) {
        int startMinute = e.getDoDate().get(ChronoField.MINUTE_OF_DAY);
        int endMinute = startMinute + e.getLength();

        return new Pair<>(startMinute, endMinute);
    }

    /**
     * First run of scheduling. Basically assigns a task to the date between it's earliest
     * completion date and it's due date with the lowest current commitment.
     *  @param pq PriorityQueue of all tasks in the data structure
     * @param startDate Today's date
     * @param taskSchedule Schedule to place tasks in
     * @param time Array where ith entry is the time commitment in minutes i days past today
     */
    private void initialAssignment(PriorityQueue<Task> pq, LocalDate startDate,
                                   List<List<Task>> taskSchedule, int[] time) {
        while (pq.size() != 0) {
            Task t = pq.remove();

            // Get indices into the taskSchedule for the earliest completion date and the due date.
            int earlyDateIndex = getDiff(t.getWorkingEarlyDate(), startDate);
            int dueDateIndex = getDiff(t.getDueDate(), startDate);

            // Find the date with the lowest current commitment in this range
            int min = 9000;
            int minIndex = -1;
            for (int i = earlyDateIndex; i <= dueDateIndex; i++) {
                if (time[i] < min) {
                    minIndex = i;
                    min = time[i];
                }
            }

            // Schedule the task for this date
            schedule(t, minIndex, startDate, taskSchedule, time);

            // Remove it as a dependency for it's children in the working task dependency graph so
            // we can schedule tasks that now have all their prerequisite tasks scheduled
            for (int i = 0; i < t.getWorkingChildren().size(); i++) {
                Task child = t.getWorkingChildren().get(i);

                child.removeWorkingParent(t);

                // Change the workingEarlyDate so tasks aren't scheduled for before their parent(s)
                if (child.getWorkingEarlyDate().isBefore(t.getWorkingDoDate())) {
                    child.setWorkingEarlyDate(t.getWorkingDoDate());
                }

                // If the child has no unscheduled prerequisites, add it to the priority queue so
                // we can schedule it
                if (child.getWorkingParents().size() == 0) {
                    pq.add(child);
                }
            }
        }
    }

    /**
     * Finds local minimum for time variance by attempting to A) move tasks into lesser scheduled
     * days and B) swap with tasks to decrease variance. Iterative algorithm that isn't extremely
     * computationally efficient - but produces extremely good results and performance in testing.
     * This is a single iteration.
     *
     * @param tasks List of tasks in internal data structures
     * @param startDate Today's date
     * @param taskSchedule Schedule of tasks: tS[i] is list of tasks scheduled for i days past today
     * @param time Array of time commitments, where time[i] is for i days past today's date
     *
     * @return true if the update moved a task, false if converged
     */
    private boolean update(List<Task> tasks, LocalDate startDate, List<List<Task>> taskSchedule
            , int[] time) {
        boolean changed = false;

        // Sees if there's a date that is underscheduled it can move to
        for (int i = 0; i < tasks.size(); i++) {
            Task curr = tasks.get(i);

            // Update the earlyDate in case this loop has changed it's parents around
            curr.setWorkingEarlyDate(findTrueEarlyDate(curr));

            // Get the indices into the taskSchedule/tasks
            int earlyDateIndex = getDiff(curr.getWorkingEarlyDate(), startDate);

            // Update the end date in case this loop has changed it's children around
            LocalDate currLateDate = findTrueEndDate(curr);

            // Get the index into tasks/taskSchedule
            int lateDateIndex = getDiff(currLateDate, startDate);

            // Sees if it can find a better date to schedule the task for
            for (int j = earlyDateIndex; j <= lateDateIndex; j++) {
                if (moveDate(time, j, curr, startDate, taskSchedule)) {
                    changed = true;
                }
            }

            // Sees if there's a task it can swap with to improve the work distribution
            for (int j = earlyDateIndex; j <= lateDateIndex; j++) {
                for (int k = 0; k < taskSchedule.get(j).size(); k++) {
                    // The task we would potentially swap curr with
                    Task other = taskSchedule.get(j).get(k);

                    if (swapTasks(curr, other, time, startDate, taskSchedule)) {
                        changed = true;
                    }
                }
            }
        }

        return changed;
    }

    /**
     * See if moving task from doDate to otherDate decreases variance
     *
     * @param time Array of time commitments, where time[i] is for i days past today's date
     * @param otherDateIndex Date to see if swapping to decreases variance
     * @param t Task to attempt to move
     * @param startDate Today's date
     * @param taskSchedule Schedule of tasks: tS[i] is list of tasks scheduled for i days past today
     *
     * @return true if task was moved, false if not
     */
    private boolean moveDate(int[] time, int otherDateIndex, Task t, LocalDate startDate,
                             List<List<Task>> taskSchedule) {
        boolean changed = false;

        int doDateIndex = getDiff(t.getWorkingDoDate(), startDate);

        int currTime = time[doDateIndex];    // Minutes scheduled for current do date
        int thisTime = time[otherDateIndex]; // Minutes scheduled for alternative do date
        // Disparity in minutes between scheduled tasks/events on the currently
        // scheduled day and the alternative day
        int currDiff = Math.abs(currTime - thisTime);

        // Calculate new difference if the task was moved
        currTime -= t.getTimeToComplete();
        thisTime += t.getTimeToComplete();
        int newDiff = Math.abs(currTime - thisTime);

        // If this further optimizes the schedule, implement the task reschedule
        if (newDiff < currDiff) {
            changed = true;
            schedule(t, otherDateIndex, startDate, taskSchedule, time);
            remove(t, doDateIndex, taskSchedule, time);
        }

        return changed;
    }

    /**
     * See if swapping two tasks decreases variance
     *
     * @param t1 First task to attempt swap on
     * @param t2 Second task to attempt swap on
     * @param time Array of time commitments, where time[i] is for i days past today's date
     * @param startDate Today's date
     * @param taskSchedule Schedule of tasks: tS[i] is list of tasks scheduled for i days past today
     *
     * @return true if tasks were swapped, false otherwise
     */
    private boolean swapTasks(Task t1, Task t2, int[] time, LocalDate startDate,
                              List<List<Task>> taskSchedule) {
        boolean changed = false;

        // Get indices
        int doDateIndex = getDiff(t1.getWorkingDoDate(), startDate);
        int otherDateIndex = getDiff(t2.getWorkingDoDate(), startDate);

        // Makes sure that they don't depend on each other, although RealEarlyDate/
        // RealEndDate should ensure this doesn't happen
        if (t2.getParents().contains(t1) ||
                t2.getChildren().contains(t1)) {
            return false;
        }

        // Makes sure this reschedule wouldn't reschedule the other task too late
        // or too early for it's parents/children
        LocalDate otherLateDate = findTrueEndDate(t2);
        if (otherLateDate.isBefore(t1.getWorkingDoDate())) {
            return false;
        }

        LocalDate otherEarlyDate = findTrueEarlyDate(t2);
        if (otherEarlyDate.isAfter(t1.getWorkingDoDate())) {
            return false;
        }

        // Calculates difference between schedule with/without this change nearly
        // identically to the first loop, although this one also adds/subtracts
        // the task to be swapped
        int currTime = time[doDateIndex];
        int thisTime = time[otherDateIndex];
        int currDiff = Math.abs(currTime - thisTime);
        currTime = currTime - t1.getTimeToComplete() + t2.getTimeToComplete();
        thisTime = thisTime + t1.getTimeToComplete() - t2.getTimeToComplete();
        int newDiff = Math.abs(currTime - thisTime);

        // True if the swap "preserves order", essentially makes sure that if task swapping doesn't
        // change time in minutes, it will instead make sure to prioritize tasks by compareTo.
        boolean preservesOrder = (t1.compareTo(t2) < 0) ? doDateIndex > otherDateIndex :
                doDateIndex < otherDateIndex;

        // True if the swap makes less changes to today's schedule than the current scheduling
        int currSame = (doDateIndex == getDiff(t1.getDoDate(), startDate) && doDateIndex == 0 ? 1 : 0) +
                (otherDateIndex == getDiff(t2.getDoDate(), startDate) && otherDateIndex == 0 ? 1 : 0);
        int newSame = (otherDateIndex == getDiff(t1.getDoDate(), startDate) && otherDateIndex == 0 ? 1 : 0) +
                (doDateIndex == getDiff(t2.getDoDate(), startDate) && doDateIndex == 0 ? 1 : 0);
        preservesOrder |= newSame > currSame;

        // Swaps the tasks if it creates a more optimal schedule
        if (newDiff < currDiff || (newDiff == currDiff && preservesOrder)) {
            changed = true;
            schedule(t1, otherDateIndex, startDate, taskSchedule, time);
            schedule(t2, doDateIndex, startDate, taskSchedule, time);
            remove(t1, doDateIndex, taskSchedule, time);
            remove(t2, otherDateIndex, taskSchedule, time);
        }

        return changed;
    }
}
