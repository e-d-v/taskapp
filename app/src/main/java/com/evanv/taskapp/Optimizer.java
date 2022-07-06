package com.evanv.taskapp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.PriorityQueue;

/**
 * The holy grail. A class that calculates a (locally) optimal schedule for task completion
 * dependent on other tasks, their prerequisites, and events.
 *
 * @author Evan Daniel Voogd
 * @author evanv.com
 */
public class Optimizer {
    private MyTime FindTrueEndDate(Task t) {
        MyTime currLateDate = t.getDueDate();

        for (int j = 0; j < t.getChildren().size(); j++) {
            Task child = t.getChildren().get(j);

            if (currLateDate.getDateTime() > child.getDoDate().getDateTime()) {
                currLateDate = child.getDoDate();
            }
        }

        return currLateDate;
    }

    private MyTime FindTrueEarlyDate(Task t) {
        MyTime currEarlyDate = t.getEarlyDate();

        for (int j = 0; j < t.getParents().size(); j++) {
            Task parent = t.getParents().get(j);

            if (currEarlyDate.getDateTime() < parent.getDoDate().getDateTime()) {
                currEarlyDate = parent.getDoDate();
            }
        }

        return currEarlyDate;
    }

    /**
     * Optimally schedules the given tasks
     *
     * @param tasks A list of Tasks to be scheduled
     * @param taskSchedule A list of lists of tasks. taskSchedule[i] refers to the list of tasks scheduled for i days
     *                     past the current day.
     * @param eventSchedule A list of lists of events. eventSchedule[i] refers to the list of events occurring i days
     *                      past the current day
     * @param startDate The current date
     */
    public void Optimize(ArrayList<Task> tasks, ArrayList<ArrayList<Task>> taskSchedule, ArrayList<ArrayList<Event>> eventSchedule, MyTime startDate, int todayTime) {
        taskSchedule.clear();

        MyTime lateDate = startDate;

        // Initializes tasks for optimization, see Javadoc for Task for more detail on why this is necessary.
        for (int i = 0; i < tasks.size(); i++) {
            tasks.get(i).initalizeForOpimization();

            if (tasks.get(i).getDueDate().getDateTime() > lateDate.getDateTime()) {
                lateDate = tasks.get(i).getDueDate();
            }
        }

        // Makes sure that taskSchedule is big enough to hold up to the latest possible date
        int diff = (int) (((lateDate.getDateTime() - startDate.getDateTime()) / 1440));

        for (int i = taskSchedule.size(); i <= diff; i++) {
            taskSchedule.add(new ArrayList<Task>());
        }

        // The idea here is fairly simple. We'll schedule tasks with earlier dueDates first, and then tie break for
        // the number of children (as Tasks with more children should be completed earlier so the children don't clump
        // on the due date), and finally on the early date (so earlier dates are filled up first). We only add tasks
        // with no prerequisites first (as we can schedule them now) and add tasks to the PriorityQueue when all
        // prerequisite tasks have been scheduled
        PriorityQueue<Task> pq = new PriorityQueue<>();

        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);

            if (t.getParents().size() == 0) {
                pq.add(t);
            }
        }

        int time[] = new int[taskSchedule.size()];

        for (int i = 0; i < taskSchedule.size() && i < eventSchedule.size(); i++) {
            time[i] = (i == 0) ? todayTime : 0;

            for (int j = 0; j < eventSchedule.get(i).size(); j++) {
                time[i] += eventSchedule.get(i).get(j).getLength();
            }
        }

        // First run of scheduling. Basically assigns a task to the date in it's early-due with lowest current
        // committment
        while (pq.size() != 0) {
            Task t = pq.remove();

            int earlyDateIndex = (int) (((t.getWorkingEarlyDate().getDateTime() - startDate.getDateTime()) / 1440));
            int dueDateIndex = (int) (((t.getDueDate().getDateTime() - startDate.getDateTime()) / 1440));

            int min = 9000;
            int minIndex = -1;

            for (int i = earlyDateIndex; i <= dueDateIndex; i++) {
                if (time[i] < min) {
                    minIndex = i;
                    min = time[i];
                }
            }

            taskSchedule.get(minIndex).add(t);

            time[minIndex] += t.getTimeToComplete();

            t.setDoDate(new MyTime(startDate.getDateTime() + (1440 * minIndex)));

            for (int i = 0; i < t.getWorkingChildren().size(); i++) {
                Task child = t.getWorkingChildren().get(i);

                child.removeWorkingParent(t);

                if (child.getWorkingEarlyDate().getDateTime() < t.getDoDate().getDateTime()) {
                    child.setWorkingEarlyDate(t.getDoDate());
                }

                if (child.getWorkingParents().size() == 0) {
                    pq.add(child);
                }
            }
        }

        int max_iters = 100;
        boolean changed = true;
        int iter = 0;

        // Finds locally optimal schedule by repeatedly checking two things for each task: if there is any day that it
        // can swap to to better spread out time, or if there is any task it can swap with to better spread out time.
        // Repeats until local minimum is found or max_iters is reached (although something is likely seriously wrong if
        // it gets anywhere close to that.
        while (changed && iter++ < max_iters) {
            changed = false;
            for (int i = 0; i < tasks.size(); i++) {
                Task curr = tasks.get(i);

                curr.setWorkingEarlyDate(FindTrueEarlyDate(curr));

                int earlyDateIndex = (int) (((curr.getWorkingEarlyDate().getDateTime() - startDate.getDateTime()) / 1440));
                int doDateIndex = (int) (((curr.getDoDate().getDateTime() - startDate.getDateTime()) / 1440));

                MyTime currLateDate = FindTrueEndDate(curr);

                int lateDateIndex = (int) (((currLateDate.getDateTime() - startDate.getDateTime()) / 1440));

                // Sees if it can find a better date to schedule the task for
                for (int j = earlyDateIndex; j <= lateDateIndex; j++) {
                    int currTime = time[doDateIndex];
                    int thisTime = time[j];
                    int currDiff = Math.abs(currTime - thisTime);
                    currTime -= curr.getTimeToComplete();
                    thisTime += curr.getTimeToComplete();
                    int newDiff = Math.abs(currTime - thisTime);

                    if (newDiff < currDiff) {
                        changed = true;
                        curr.setDoDate(new MyTime(startDate.getDateTime() + (1440 * j)));
                        taskSchedule.get(doDateIndex).remove(curr);
                        taskSchedule.get(j).add(curr);
                        time[doDateIndex] -= curr.getTimeToComplete();
                        time[j] += curr.getTimeToComplete();
                        doDateIndex = j;
                    }
                }

                // Sees if there's a task it can swap with to improve the work distribution
                for (int j = earlyDateIndex; j <= lateDateIndex; j++) {
                    for (int k = 0; k < taskSchedule.get(j).size(); k++) {
                        Task other = taskSchedule.get(j).get(k);

                        if (other.getParents().contains(curr) || other.getChildren().contains(curr)) {
                            continue;
                        }

                        MyTime otherLateDate = FindTrueEndDate(other);

                        if (otherLateDate.getDateTime() < curr.getDoDate().getDateTime() || other.getWorkingEarlyDate().getDateTime() > curr.getDoDate().getDateTime()) {
                            continue;
                        }

                        int currTime = time[doDateIndex];
                        int thisTime = time[j];
                        int currDiff = Math.abs(currTime - thisTime);
                        currTime = currTime - curr.getTimeToComplete() + other.getTimeToComplete();
                        thisTime = thisTime + curr.getTimeToComplete() - other.getTimeToComplete();
                        int newDiff = Math.abs(currTime - thisTime);

                        if (newDiff < currDiff) {
                            changed = true;
                            other.setDoDate(curr.getDoDate());
                            curr.setDoDate(new MyTime(startDate.getDateTime() + (1440 * j)));
                            taskSchedule.get(doDateIndex).remove(curr);
                            taskSchedule.get(doDateIndex).add(other);
                            taskSchedule.get(j).add(curr);
                            taskSchedule.get(j).remove(other);
                            time[doDateIndex] -= curr.getTimeToComplete();
                            time[doDateIndex] += other.getTimeToComplete();
                            time[j] += curr.getTimeToComplete();
                            time[j] -= other.getTimeToComplete();
                            doDateIndex = j;
                        }
                    }
                }
            }
        }
    }
}
