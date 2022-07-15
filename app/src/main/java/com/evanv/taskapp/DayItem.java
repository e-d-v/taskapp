package com.evanv.taskapp;

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

    /**
     * Creates a DayItem to be used in MainActivity's recyclerview
     *
     * @param dayString string of the day this DayItem represents
     * @param events list of EventItems to be displayed in recyclerview
     * @param tasks list of TaskItems to be displayed in recyclerview
     */
    @SuppressWarnings("unused")
    public DayItem(String dayString, List<EventItem> events, List<TaskItem> tasks) {
        mDayString = dayString;
        mEvents = events;
        mTasks = tasks;
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
     * @param mDayString a string representing the day this DayItem represents
     */
    public void getDayString(String mDayString) {
        this.mDayString = mDayString;
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
     * @param mEvents a List of EventItems to be displayed in recyclerview
     */
    public void setEvents(List<EventItem> mEvents) {
        this.mEvents = mEvents;
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
     * @param mTasks a List of TaskItems to be displayed in recyclerview
     */
    public void setTasks(List<TaskItem> mTasks) {
        this.mTasks = mTasks;
    }
}
