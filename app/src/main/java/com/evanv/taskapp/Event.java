package com.evanv.taskapp;

/**
 * Represents a single event, e.g. something that has a set date/time
 *
 * @author Evan Voogd
 */
@SuppressWarnings("unused")
public final class Event {
    private final String mName;   // The name of the event to display in the schedule
    private final int mLength;    // How long the event lasts
    // The start time for the event. Called "doDate" with consistency to Task
    private final MyTime mDoDate;

    /**
     * Returns the name of the event
     *
     * @return The name of the event
     */
    public String getName() {
        return mName;
    }

    /**
     * Returns the starting time of the event
     *
     * @return The starting time of the event
     */
    public MyTime getDoDate() {
        return mDoDate;
    }

    /**
     * Returns the length of the event
     *
     * @return The length of the event
     */
    public int getLength() {
        return mLength;
    }

    /**
     * Initializes an object representing an event
     *
     * @param event The name of the event
     * @param start The start time for the event
     * @param length The time to complete for the event
     */
    public Event(String event, MyTime start, int length) {
        this.mName = event;
        this.mDoDate = start;
        this.mLength = length;
    }
}