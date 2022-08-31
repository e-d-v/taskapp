package com.evanv.taskapp.logic;

import static com.evanv.taskapp.logic.Task.getDiff;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.PriorityQueue;

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
    private Date findTrueEndDate(Task t) {
        // B) the actual due date for the given task
        Date currLateDate = t.getDueDate();

        // A) the earliest current do date for a child task, if the child task's do date is earlier
        // than B) the actual due date for the given task
        for (int j = 0; j < t.getChildren().size(); j++) {
            Task child = t.getChildren().get(j);

            if (currLateDate.after(child.getWorkingDoDate())) {
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
    private Date findTrueEarlyDate(Task t) {
        // B) the actual earliest completion date for the given task
        Date currEarlyDate = t.getEarlyDate();

        // A) the latest current do date for a parent task, if the parent task's do date is later
        // than B) the actual earliest completion date for the given task
        for (int j = 0; j < t.getParents().size(); j++) {
            Task parent = t.getParents().get(j);

            if (currEarlyDate.before(parent.getWorkingDoDate())) {
                currEarlyDate = parent.getWorkingDoDate();
            }
        }

        return currEarlyDate;
    }

    /**
     * Schedules task for the date index days past startDate.
     *
     * @param t The task to be scheduled
     * @param index How many days past startDate to be scheduled
     * @param startDate The Date representing taskSchedule[0]
     * @param taskSchedule ArrayList where taskSchedule[i] is a list of tasks scheduled for i days
     *                     past the start date
     * @param time Array of ints where time[i] is the number of minutes scheduled for i days past
     *             the start date
     */
    private void schedule(Task t, int index, Date startDate, ArrayList<ArrayList<Task>> taskSchedule
            , int[] time) {
        taskSchedule.get(index).add(t);
        time[index] += t.getTimeToComplete();
        Calendar doCal = Calendar.getInstance();
        doCal.setTime(startDate);
        doCal.add(Calendar.DAY_OF_YEAR, index);
        t.setWorkingDoDate(doCal.getTime());
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
    private void remove(Task t, int index, ArrayList<ArrayList<Task>> taskSchedule, int[] time) {
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
    public ArrayList<Task> Optimize(List<Task> tasks, ArrayList<ArrayList<Task>> taskSchedule,
                                    ArrayList<ArrayList<Event>> eventSchedule, Date startDate,
                                    int todayTime) {
        taskSchedule.clear();

        Date lateDate = startDate;

        // Initializes tasks for optimization, see Javadoc for Task for more detail on why this is
        // necessary.
        for (int i = 0; i < tasks.size(); i++) {
            tasks.get(i).initializeForOptimization();

            if (tasks.get(i).getDueDate().after(lateDate)) {
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

        // Initialize the time array, where time[i] is the amount of currently scheduled time for
        // the date i days past today's date. We start with only using events, as they can't be
        // rescheduled, so we should schedule tasks around them
        int[] time = new int[taskSchedule.size()];
        for (int i = 0; i < taskSchedule.size(); i++) {
            time[i] = (i == 0) ? todayTime : 0;

            for (int j = 0; i < eventSchedule.size() && j < eventSchedule.get(i).size(); j++) {
                time[i] += eventSchedule.get(i).get(j).getLength();
            }
        }

        // First run of scheduling. Basically assigns a task to the date between it's earliest
        // completion date and it's due date with the lowest current commitment
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
                if (child.getWorkingEarlyDate().before(t.getWorkingDoDate())) {
                    child.setWorkingEarlyDate(t.getWorkingDoDate());
                }

                // If the child has no unscheduled prerequisites, add it to the priority queue so
                // we can schedule it
                if (child.getWorkingParents().size() == 0) {
                    pq.add(child);
                }
            }
        }

        int max_iters = 100;    // Maximum number of iterations, can be tweaked for performance
        boolean changed = true; // Allows us to stop the loop when a local minima has been found
        int iter = 0;           // How many iterations have been completed

        // Finds locally optimal schedule by repeatedly checking two things for each task: if there
        // is any day that it can swap to to better spread out time, or if there is any task it can
        // swap with to better spread out time. Repeats until local minimum is found or max_iters is
        // reached (although something is likely seriously wrong if it gets anywhere close to that.
        while (changed && iter++ < max_iters) {
            changed = false;

            // Sees if there's a date that is underscheduled it can move to
            for (int i = 0; i < tasks.size(); i++) {
                Task curr = tasks.get(i);

                // Update the earlyDate in case this loop has changed it's parents around
                curr.setWorkingEarlyDate(findTrueEarlyDate(curr));

                // Get the indices into the taskSchedule/tasks
                int earlyDateIndex = getDiff(curr.getWorkingEarlyDate(), startDate);
                int doDateIndex = getDiff(curr.getWorkingDoDate(), startDate);

                // Update the end date in case this loop has changed it's children around
                Date currLateDate = findTrueEndDate(curr);

                // Get the index into tasks/taskSchedule
                int lateDateIndex = getDiff(currLateDate, startDate);

                // Sees if it can find a better date to schedule the task for
                for (int j = earlyDateIndex; j <= lateDateIndex; j++) {
                    int currTime = time[doDateIndex]; // Minutes scheduled for current do date
                    int thisTime = time[j];           // Minutes scheduled for alternative do date
                    // Disparity in minutes between scheduled tasks/events on the currently
                    // scheduled day and the alternative day
                    int currDiff = Math.abs(currTime - thisTime);

                    // Calculate new difference if the task was moved
                    currTime -= curr.getTimeToComplete();
                    thisTime += curr.getTimeToComplete();
                    int newDiff = Math.abs(currTime - thisTime);

                    // If this further optimizes the schedule, implement the task reschedule
                    if (newDiff < currDiff) {
                        changed = true;
                        schedule(curr, j, startDate, taskSchedule, time);
                        remove(curr, doDateIndex, taskSchedule, time);
                        doDateIndex = j;
                    }
                }

                // Sees if there's a task it can swap with to improve the work distribution
                for (int j = earlyDateIndex; j <= lateDateIndex; j++) {
                    for (int k = 0; k < taskSchedule.get(j).size(); k++) {
                        // The task we would potentially swap curr with
                        Task other = taskSchedule.get(j).get(k);

                        // Makes sure that they don't depend on each other, although RealEarlyDate/
                        // RealEndDate should ensure this doesn't happen
                        if (other.getParents().contains(curr) ||
                                other.getChildren().contains(curr)) {
                            continue;
                        }

                        // Makes sure this reschedule wouldn't reschedule the other task too late
                        // or too early for it's parents/children
                        Date otherLateDate = findTrueEndDate(other);
                        if (otherLateDate.before(curr.getWorkingDoDate())) {
                            continue;
                        }

                        // Calculates difference between schedule with/without this change nearly
                        // identically to the first loop, although this one also adds/subtracts
                        // the task to be swapped
                        int currTime = time[doDateIndex];
                        int thisTime = time[j];
                        int currDiff = Math.abs(currTime - thisTime);
                        currTime = currTime - curr.getTimeToComplete() + other.getTimeToComplete();
                        thisTime = thisTime + curr.getTimeToComplete() - other.getTimeToComplete();
                        int newDiff = Math.abs(currTime - thisTime);

                        // Swaps the tasks if it creates a more optimal schedule
                        if (newDiff < currDiff) {
                            changed = true;
                            schedule(curr, j, startDate, taskSchedule, time);
                            schedule(other, doDateIndex, startDate, taskSchedule, time);
                            remove(curr, doDateIndex, taskSchedule, time);
                            remove(other, j, taskSchedule, time);
                            doDateIndex = j;
                        }
                    }
                }
            }
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
}
