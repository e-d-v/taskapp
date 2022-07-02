package com.evanv.taskapp;

/**
 * Class representing an Event for use in the DayItem's recyclerview
 */
public class EventItem {
    private String mName;     // Name of the event (e.g. "Staff Meeting")
    private String mTimespan; // Timespan of the event (e.g. "2-4pm")

    /**
     * Creates an item representing an Event
     *
     * @param name a string representing the name of the Event
     * @param timespan a string representing the timespan of the Event
     */
    public EventItem(String name, String timespan) {
        mName = name;
        mTimespan = timespan;
    }

    /**
     * Returns string representing the name of the Event
     *
     * @return a string representing the name of the Event
     */
    public String getName() {
        return mName;
    }

    /**
     * Returns string representing the timespan of the Event
     *
     * @return a string representing the timespan of the Event
     */
    public String getTimespan() {
        return mTimespan;
    }

    /**
     * Sets the string representing the name of the Event
     *
     * @param mName a string representing the name of the Event
     */
    public void setName(String mName) {
        this.mName = mName;
    }

    /**
     * Sets the string representing the timespan of the Event
     *
     * @param mTimespan a string representing the timespan of the Event
     */
    public void setTimespan(String mTimespan) {
        this.mTimespan = mTimespan;
    }
}
