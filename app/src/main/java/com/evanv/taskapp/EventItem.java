package com.evanv.taskapp;

/**
 * Class representing an Event for use in the DayItem's recyclerview
 *
 * @author Evan Voogd
 */
public class EventItem {
    private final String mName;     // Name of the event (e.g. "Staff Meeting")
    private final String mTimespan; // Timespan of the event (e.g. "2-4pm")
    private final int mIndex;       // Index into eventSchedule.get(day) for this event

    /**
     * Creates an item representing an Event
     *
     * @param name a string representing the name of the Event
     * @param timespan a string representing the timespan of the Event
     * @param index Index into eventSchedule.get(day) for this event
     */
    @SuppressWarnings("unused")
    public EventItem(String name, String timespan, int index) {
        mName = name;
        mTimespan = timespan;
        mIndex = index;
    }

    /**
     * Returns index of eventSchedule.get(day) this event is stored at.
     *
     * @return index of eventSchedule.get(day) this event is stored at.
     */
    public int getIndex() {
        return mIndex;
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

}
