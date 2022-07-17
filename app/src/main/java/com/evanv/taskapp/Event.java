package com.evanv.taskapp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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
    private final Date mDoDate;
    // SimpleDateFormat that formats date in the style "08/20/22 08:12 PM"
    public static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MM/dd/yy hh:mm aa");
    // SimpleDateFormat that formats time in the style "08:12 PM"
    public static final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm aa");

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
    public Date getDoDate() {
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
    public Event(String event, Date start, int length) {
        this.mName = event;
        this.mDoDate = clearTime(start);
        this.mLength = length;
    }

    /**
     * Clears second/ms information from Date. Very useful for Events as Events have no information
     * past seconds.
     *
     * @param toClear The date to return without a seconds
     * @return toClear but at the time HOURS:MINUTES:00.0000
     */
    public static Date clearTime(Date toClear) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(toClear);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}