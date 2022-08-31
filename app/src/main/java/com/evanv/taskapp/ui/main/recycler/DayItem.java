package com.evanv.taskapp.ui.main.recycler;

import java.util.List;

/**
 * Representation of a day for the outer recyclerview in the MainActivity. Contains two
 * sub-RecyclerViews, represented by EventItem and TaskItem.
 *
 * @author Evan Voogd
 */
@SuppressWarnings("unused")
public class DayItem {
    private String mDayString;       // String of the day this DayItem represents
    private List<EventItem> mEvents; // List of EventItems to be displayed in recyclerview
    private List<TaskItem> mTasks;   // List of TaskItems to be displayed in recyclerview
    private final int mIndex;        // Index into task/eventSchedule for this day

    /**
     * Returns how many days past today's date this DayItem represents, which can be used as an
     * index into the taskSchedule/eventSchedule ArrayLists for easy completion/deletion of events
     * and tasks without using convoluted recycler index logic.
     *
     * @return index into task/eventSchedule representing this DayItem
     */
    public int getIndex() {
        return mIndex;
    }

    /**
     * Creates a DayItem to be used in MainActivity's recyclerview
     *
     * @param dayString string of the day this DayItem represents
     * @param events list of EventItems to be displayed in recyclerview
     * @param tasks list of TaskItems to be displayed in recyclerview
     * @param index how many days past today's date this DayItem represents
     */
    @SuppressWarnings("unused")
    public DayItem(String dayString, List<EventItem> events, List<TaskItem> tasks, int index) {
        mDayString = dayString;
        mEvents = events;
        mTasks = tasks;
        mIndex = index;
    }

    /**
     * Returns string representing the day this DayItem represents
     *
     * @return a string representing the day this DayItem represents
     */
    public String getDayString() {
        return mDayString;
    }

    /**
     * Sets the string representing the day this DayItem represents
     *
     * @param dayString a string representing the day this DayItem represents
     */
    public void setDayString(String dayString) {
        this.mDayString = dayString;
    }

    /**
     * Returns the List of EventItems to be displayed in recyclerview
     *
     * @return a List of EventItems to be displayed in recyclerview
     */
    public List<EventItem> getEvents() {
        return mEvents;
    }

    /**
     * Sets the List of EventItems to be displayed in recyclerview
     *
     * @param events a List of EventItems to be displayed in recyclerview
     */
    public void setEvents(List<EventItem> events) {
        this.mEvents = events;
    }

    /**
     * Returns the List of TaskItems to be displayed in recyclerview
     *
     * @return a List of TaskItems to be displayed in recyclerview
     */
    public List<TaskItem> getTasks() {
        return mTasks;
    }

    /**
     * Sets the List of TaskItems to be displayed in recyclerview
     *
     * @param tasks a List of TaskItems to be displayed in recyclerview
     */
    public void setTasks(List<TaskItem> tasks) {
        this.mTasks = tasks;
    }
}
