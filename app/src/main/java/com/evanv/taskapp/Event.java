package com.evanv.taskapp;

import java.util.Date;

/**
 * Represents a single event, e.g. something that has a set date/time
 *
 * @author Evan Daniel Voogd
 * @author evanv.com
 */
public final class Event {
    private String name;   // The name of the event to display in the schedule
    private MyTime doDate; // The start time for the event. Called "doDate" with consistency to Task
    private int length;    // How long the event lasts

    /**
     * Returns the name of the event
     *
     * @return The name of the event
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the starting time of the event
     *
     * @return The starting time of the event
     */
    public MyTime getDoDate() {
        return doDate;
    }

    /**
     * Returns the length of the event
     *
     * @return The length of the event
     */
    public int getLength() {
        return length;
    }

    /**
     * Initializes an object representing an event
     *
     * @param event The name of the event
     * @param start The start time for the event
     * @param length The time to complete for the event
     * @return An object representing an event
     */
    public Event(String event, MyTime start, int length) {
        this.name = event;
        this.doDate = start;
        this.length = length;
    }
}